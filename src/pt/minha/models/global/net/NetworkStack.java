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

package pt.minha.models.global.net;

import java.net.BindException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pt.minha.kernel.simulation.Timeline;
import pt.minha.models.fake.java.net.NetworkInterface;

public class NetworkStack {
	private Network network;
	private Timeline timeline;
	
	private InetAddress localAddress;
	private final List<Integer> usedPorts = new LinkedList<Integer>();
	private int INITIAL_PORT = 10000;
	
	private final String macAddress;
	private final List<NetworkInterface> networkInterfaces = new LinkedList<NetworkInterface>();
	
	public NetworkStack(Timeline timeline, String host, Network network) throws UnknownHostException {
		this.timeline = timeline;
		this.network = network;
		
		if ( host != null )
			this.localAddress = network.addHost(host, this);
		else
			this.localAddress = network.getAvailableInetAddress(this);
		
		this.macAddress = network.generateMACAddress();
		this.networkInterfaces.add(new NetworkInterface(macAddress, localAddress));

	}

	public InetAddress getLocalAddress() {
		return this.localAddress;
	}
		
	private int getAvailablePort() {
		int port;

		do {
			port = this.INITIAL_PORT++;
		}
		while ( this.usedPorts.contains(port) );
		
		if ( port<=0 || port>Integer.MAX_VALUE )
			throw new IllegalArgumentException("port out of range: " + port);

		return port;
	}

	
	private int addPort(int port) {
		if ( 0 == port )
			port = this.getAvailablePort();
		
		if ( this.usedPorts.contains(port) )
			throw new IllegalArgumentException("Port already used: " + port);
		
		this.usedPorts.add(port);
		return port;
	}
	
	
	public InetSocketAddress getHostAvailableInetSocketAddress() {
		return this.getHostAvailableInetSocketAddress(0);
	}

	public InetSocketAddress getHostAvailableInetSocketAddress(int port) {
		return new InetSocketAddress(this.localAddress, this.addPort(port));		
	}

	public String getMACAddress() {
		return this.macAddress;
	}
	
	public List<NetworkInterface> getNetworkInterfaces() {
		return this.networkInterfaces;
	}
	
	public InetSocketAddress checkSocket(InetSocketAddress isa) throws SocketException {
		if ( null == isa )
			throw new IllegalArgumentException("bind: null address");
		if ( isa.getPort() < 0 || isa.getPort()> 0xFFFF )
			throw new IllegalArgumentException("bind: " + isa.getPort());
		if ( null == isa.getAddress() )
    		throw new IllegalArgumentException("bind: null address");
		
		if (isa.getAddress().equals(new InetSocketAddress(0).getAddress())) {
			isa = getHostAvailableInetSocketAddress(isa.getPort());
		}
		else {
			try {
				InetAddress ia = InetAddress.getByName("0.0.0.0");
				if( isa.getAddress().equals(ia) ) {
					isa = getHostAvailableInetSocketAddress(isa.getPort());
				}
			} catch (UnknownHostException e) {
				throw new IllegalArgumentException(e);
			}
		}
		return isa;
	}

	public Network getNetwork() {
		return network;
	}
	
	public Timeline getTimeline() {
		return timeline;
	}
	
	// FIXME: the following should move to networkstack
	
	private final Map<InetSocketAddress, DatagramSocketUpcalls> socketsUDP = new HashMap<InetSocketAddress, DatagramSocketUpcalls>();
	private final Map<InetSocketAddress, ServerSocketUpcalls> socketsTCP = new HashMap<InetSocketAddress, ServerSocketUpcalls>();
	
	public InetSocketAddress addTCPSocket(InetSocketAddress isa, ServerSocketUpcalls ab) throws SocketException {
		if ( socketsTCP.containsKey(isa) )
			throw new BindException(isa.toString() + " Cannot assign requested address on TCP");

		socketsTCP.put(isa, ab);
		return isa;
	}

	public InetSocketAddress addUDPSocket(InetSocketAddress isa, DatagramSocketUpcalls ab) throws SocketException {
		if ( socketsUDP.containsKey(isa) )
			throw new BindException(isa.toString() + " Cannot assign requested address on UDP");

		socketsUDP.put(isa, ab);
		return isa;
	}

	public void removeTCPSocket(InetSocketAddress isa) {
		if ( socketsTCP.containsKey(isa) )
			socketsTCP.remove(isa);
	}
	
	public void removeUDPSocket(InetSocketAddress isa) {
		if ( socketsUDP.containsKey(isa) )
			socketsUDP.remove(isa);
	}
	
	public void DatagramPacketQueue(InetSocketAddress destination, DatagramPacket packet) {
		DatagramSocketUpcalls sgds = socketsUDP.get(destination);
		if ( null!=sgds ) {
			sgds.queue(packet);
		}
	}
	
	public void handleConnect(InetSocketAddress destination, InetSocketAddress source, SocketUpcalls upcalls) {
		ServerSocketUpcalls ssi = socketsTCP.get(destination);
		if (ssi==null)
			upcalls.accepted(null);
		ssi.queueConnect(destination, source, upcalls);
	}
}
