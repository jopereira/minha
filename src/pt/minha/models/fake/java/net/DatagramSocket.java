/*
 * Minha.pt: middleware testing platform.
 * Copyright (c) 2011-2012, Universidade do Minho.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package pt.minha.models.fake.java.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;

import pt.minha.api.World;
import pt.minha.kernel.simulation.Event;
import pt.minha.models.global.net.DatagramSocketInterface;
import pt.minha.models.global.net.MulticastSocketMap;
import pt.minha.models.global.net.Network;
import pt.minha.models.global.net.NetworkCalibration;
import pt.minha.models.global.net.Protocol;
import pt.minha.models.local.HostImpl;
import pt.minha.models.local.lang.SimulationThread;

public class DatagramSocket extends AbstractSocket implements DatagramSocketInterface {
	
	private List<DatagramPacket> incoming = new LinkedList<DatagramPacket>();
	private List<Event> blocked = new LinkedList<Event>();
    private long lastRead = 0;
    private boolean closed = false;
	
	public DatagramSocket() throws SocketException{
		HostImpl host = SimulationThread.currentSimulationThread().getHost();
		InetSocketAddress isa = host.getHostAvailableInetSocketAddress();
		this.addSocket(Protocol.UDP, isa, this);
	}
	
	
	public DatagramSocket(int port) throws SocketException {
		HostImpl host = SimulationThread.currentSimulationThread().getHost();
		InetSocketAddress isa = host.getHostAvailableInetSocketAddress(port);
		this.addSocket(Protocol.UDP, isa, this);
	}

	public DatagramSocket(int port, InetAddress address) throws SocketException {
		// FIXME: bind address not implemented
		this(port);
	}

	public void send(DatagramPacket packet) throws IOException {
		if (closed)
			throw new SocketException("send on closed socket");
		
		try {
			SimulationThread.stopTime(NetworkCalibration.writeCost*packet.getLength());
			
			if (packet.getAddress().isMulticastAddress()) {
				MulticastSocketMap.MulticastSocketQueue((InetSocketAddress)this.getLocalSocketAddress(), packet);
			}
			else {
				InetSocketAddress destination = new InetSocketAddress(packet.getAddress(),packet.getPort());
				byte[] data = new byte[packet.getLength()];
				System.arraycopy(packet.getData(), packet.getOffset(), data, 0, data.length);
				DatagramPacket dp = new DatagramPacket(data, data.length, this.getLocalSocketAddress());
				World.network.send(destination, dp);
			}

		} finally  {
			SimulationThread.startTime(0);
		}
	}

	public void receive(DatagramPacket packet) throws IOException {
		if (closed)
			throw new SocketException("receive on closed socket");
	
		SimulationThread.stopTime(0);
		if (incoming.isEmpty()) {
			blocked.add(SimulationThread.currentSimulationThread().getWakeup());
			SimulationThread.currentSimulationThread().pause();
		}

		DatagramPacket p=incoming.remove(0);
		
		// network delay
		if ( NetworkCalibration.networkDelay ){
			long now = SimulationThread.currentSimulationThread().getTimeline().getTime();
			long delay = NetworkCalibration.getNetworkDelay(packet.getLength());
			if ( (now-this.lastRead)>delay )
				SimulationThread.currentSimulationThread().idle(delay);
		}
		this.lastRead = SimulationThread.currentSimulationThread().getTimeline().getTime();
		
		System.arraycopy(p.getData(), 0, packet.getData(), packet.getOffset(), p.getLength());
		packet.setAddress(p.getAddress());
		packet.setPort(p.getPort());
		SimulationThread.startTime(NetworkCalibration.readCost*p.getLength());
	}
	
	@Override
	public void queue(DatagramPacket packet) {
		incoming.add(packet);
		new WakeUpEvent().schedule(0);
	}
	
    public void close(){
    	// FIXME: wake up receivers? synchronization?
    	closed = true;
    	this.removeSocket(Protocol.UDP, (InetSocketAddress)this.getLocalSocketAddress());
    }
    
	private class WakeUpEvent extends Event {
		public WakeUpEvent() {
			super(World.timeline);
		}

		public void run() {
			wakeup();
		}
	}

    private void wakeup() {
		if (!blocked.isEmpty())
			blocked.remove(0).schedule(0);
	}
}
