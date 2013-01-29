/*
 * Minha.pt: middleware testing platform.
 * Copyright (c) 2011-2013, Universidade do Minho.
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
import java.net.SocketAddress;
import java.net.SocketException;

import pt.minha.models.fake.java.nio.channels.DatagramChannel;
import pt.minha.models.global.net.NetworkCalibration;
import pt.minha.models.global.net.UDPSocket;
import pt.minha.models.local.lang.SimulationThread;

public class DatagramSocket {
	protected UDPSocket udp;
	
    private boolean closed = false;
    private DatagramChannel channel;
		
	public DatagramSocket() throws SocketException{
		this(0);
	}
	
	public DatagramSocket(int port) throws SocketException {
		this(new InetSocketAddress(port));
	}

	public DatagramSocket(int port, InetAddress address) throws SocketException {
		this(new InetSocketAddress(address,port));
	}

	public DatagramSocket(SocketAddress address) throws SocketException {
		udp=new UDPSocket(SimulationThread.currentSimulationThread().getHost().getNetwork());
		udp.bind((InetSocketAddress)address);
	}
	
	public DatagramSocket(DatagramChannel channel, UDPSocket udp) {
		this.udp = udp;
		this.channel = channel;
	}

	public void bind(SocketAddress address) throws SocketException {
		udp.bind((InetSocketAddress)address);
	}

	public void send(DatagramPacket packet) throws IOException {
		if (closed)
			throw new SocketException("send on closed socket");
		
		try {
			SimulationThread.stopTime(NetworkCalibration.writeCost*packet.getLength());
			
			udp.send(packet);
			
		} finally  {
			SimulationThread.startTime(0);
		}
	}

	public void receive(DatagramPacket packet) throws IOException {
		if (closed)
			throw new SocketException("receive on closed socket");

		int cost = 0;
		
		try {
			SimulationThread.stopTime(0);
			
			while(!udp.readers.isReady() && !closed) {
				udp.readers.queue(SimulationThread.currentSimulationThread().getWakeup());
				SimulationThread.currentSimulationThread().pause();
			}
			
			if (closed)
				throw new SocketException("receive on closed socket");
			
			DatagramPacket p=udp.receive();
					
			System.arraycopy(p.getData(), 0, packet.getData(), packet.getOffset(), p.getLength());
			packet.setAddress(p.getAddress());
			packet.setPort(p.getPort());
			cost = p.getLength();
		
		} finally {
			SimulationThread.startTime(NetworkCalibration.readCost*cost);
		}
	}

    public void close() {
    	closed = true;
    	udp.shutdown();
    }
        
	public SocketAddress getLocalSocketAddress() {
		return udp.getLocalAddress();
	}
	
	public InetAddress getLocalAddress() {
		return udp.getLocalAddress().getAddress();
	}

	public int getLocalPort() {
		return udp.getLocalAddress().getPort();
	}
	
	public DatagramChannel getChannel() {
		return channel;
	}

	public boolean isClosed() {
		return closed;
	}
}
