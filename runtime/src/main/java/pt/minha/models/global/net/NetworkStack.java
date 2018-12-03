/*
 * Minha.pt: middleware testing platform.
 * Copyright (c) 2011-2014, Universidade do Minho.
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

import java.io.Closeable;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pt.minha.api.sim.Calibration;
import pt.minha.kernel.simulation.Event;
import pt.minha.kernel.simulation.SimpleResource;
import pt.minha.kernel.simulation.Timeline;

public class NetworkStack implements Closeable {
	private Network network;
	private Timeline timeline;
	
	private InetAddress localAddress, broadcastAddress;
	private final List<Integer> usedTCPPorts = new LinkedList<Integer>();
	private final List<Integer> usedUDPPorts = new LinkedList<Integer>();
	private int INITIAL_PORT = 10000;
	
	private byte[] macAddress;
	private SimpleResource bandwidth;
	
	public NetworkStack(Timeline timeline, String host, Network network) throws UnknownHostException {
		this.timeline = timeline;
		this.network = network;
		
		this.localAddress = network.addHost(host, this);
		this.broadcastAddress = network.getBroadcastAddress();
		this.macAddress = new byte[6];
		macAddress[0] = 2;

		for(int i=0;i<localAddress.getAddress().length;i++)
			macAddress[i+1] = localAddress.getAddress()[i];

		this.bandwidth = new SimpleResource(timeline);
	}

	public InetAddress getLocalAddress() {
		return this.localAddress;
	}

	public byte[] getMACAddress() {
		return this.macAddress;
	}

	public InetAddress getBroadcastAddress() {
		return this.broadcastAddress;
	}

	private int getAvailablePort(List<Integer> usedPorts) {
		boolean reset = false;
		int port = -1;
		
		do {
			if (INITIAL_PORT>=(1<<16)-1) {
				if (reset)
					break;
				INITIAL_PORT = 10000;
				reset = true;
			}

			port = this.INITIAL_PORT++;
		}
		while ( usedPorts.contains(port) );
		
		if ( port <= 0 )
			throw new IllegalArgumentException("port out of range: " + port);

		return port;
	}
	
	public InetSocketAddress getUDPBindAddress(int port) {
		
		if ( port < 0 || port> 0xFFFF )
			throw new IllegalArgumentException("bind: " + port);

		if ( 0 == port )
			port = this.getAvailablePort(usedUDPPorts);
		
		if ( this.usedUDPPorts.contains(port) )
			throw new IllegalArgumentException("Port already used: " + port);
		
		this.usedUDPPorts.add(port);
		
		return new InetSocketAddress(this.localAddress, port);		
	}

	public InetSocketAddress getTCPBindAddress(int port) {
		
		if ( port < 0 || port> 0xFFFF )
			throw new IllegalArgumentException("bind: " + port);

		if ( 0 == port )
			port = this.getAvailablePort(usedTCPPorts);
		
		if ( this.usedTCPPorts.contains(port) )
			throw new IllegalArgumentException("Port already used: " + port);
		
		this.usedTCPPorts.add(port);
		
		return new InetSocketAddress(this.localAddress, port);		
	}

	public Network getNetwork() {
		return network;
	}
	
	public Timeline getTimeline() {
		return timeline;
	}
		
	private final Map<InetSocketAddress, UDPSocket> socketsUDP = new HashMap<InetSocketAddress, UDPSocket>();
	private final Map<InetSocketAddress, ListeningTCPSocket> socketsTCP = new HashMap<InetSocketAddress, ListeningTCPSocket>();
	private final Set<ClientTCPSocket> connTCP = new HashSet<ClientTCPSocket>();
	
	public InetSocketAddress addTCPSocket(InetSocketAddress isa, ListeningTCPSocket ab) throws SocketException {
		if ( socketsTCP.containsKey(isa) )
			throw new BindException(isa.toString() + " Cannot assign requested address on TCP");

		socketsTCP.put(isa, ab);
		return isa;
	}

	public void addTCPConnection(ClientTCPSocket ab) {
		connTCP.add(ab);
	}

	public InetSocketAddress addUDPSocket(InetSocketAddress isa, UDPSocket udpSocket) throws SocketException {
		if ( socketsUDP.containsKey(isa) )
			throw new BindException(isa.toString() + " Cannot assign requested address on UDP");

		socketsUDP.put(isa, udpSocket);
		return isa;
	}

	public void removeTCPSocket(InetSocketAddress isa) {
		usedTCPPorts.remove((Object)isa.getPort());
		socketsTCP.remove(isa);
	}

	public void removeTCPConn(ClientTCPSocket isa) {
		connTCP.remove(isa);
	}
	
	public void removeUDPSocket(InetSocketAddress isa) {
		usedUDPPorts.remove((Object)isa.getPort());
		socketsUDP.remove(isa);
	}
	
	public Event handleDatagram(final InetSocketAddress destination, final DatagramPacket packet) {
		return new Event(timeline) {
			public void run() {
				UDPSocket sgds = socketsUDP.get(destination);
				if (sgds==null && destination.getAddress().equals(broadcastAddress)) {
					InetSocketAddress dest =new InetSocketAddress(localAddress, destination.getPort()); 
					sgds=socketsUDP.get(dest);
				}
				if (sgds!=null)
					sgds.queue(packet);
			}			
		};
	}
	
	public Event handleConnect(final InetSocketAddress destination, final TCPPacket p) {
		return new Event(timeline) {
			public void run() {
				ListeningTCPSocket ssi = socketsTCP.get(destination);
				if (ssi==null)
					ssi = socketsTCP.get(new InetSocketAddress(destination.getPort()));
				if (ssi==null || !ssi.queueConnect(p)) // connection refused
					relayTCPData(new TCPPacket(null, p.getSource(), 0, 0, new byte[0], TCPPacket.RST));
			}
		};
	}
	
	public void relayTCPConnect(final InetSocketAddress serverAddr, final TCPPacket p) {
		long qdelay = bandwidth.use(getConfig().getLineDelay(p.getSize())) + getConfig().getLineLatency(p.getSize()) - getTimeline().getTime();
		
		NetworkStack target = network.getHost(serverAddr.getAddress());
		if (target==null)
			p.getSource().scheduleRead(new TCPPacket(null, p.getSource(), 0, 0, new byte[0], TCPPacket.RST)).schedule(qdelay);
		else
			target.handleConnect(serverAddr, p).scheduleFrom(timeline, qdelay);				
	}
		
	public void relayTCPData(final TCPPacket p) {
		long qdelay = bandwidth.use(getConfig().getLineDelay(p.getSize())) + getConfig().getLineLatency(p.getSize()) - getTimeline().getTime();
		
		p.getDestination().scheduleRead(p).scheduleFrom(timeline, qdelay);
	}

	public void relayUDP(final InetSocketAddress destination, final DatagramPacket p) {
		long time = bandwidth.useConditionally(getConfig().getMaxDelay(), getConfig().getLineDelay(p.getLength()));
		if (time < 0)
			return;
		
		long qdelay = time + getConfig().getLineLatency(p.getLength()) - getTimeline().getTime();
		
		if (destination.getAddress().isMulticastAddress()) {
			Collection<NetworkStack> stacks = network.getMulticastHosts(destination.getAddress());
			if (stacks != null)
				for (NetworkStack stack: stacks)
					stack.handleDatagram(destination, p).scheduleFrom(timeline, qdelay);
		} else if (destination.getAddress().equals(broadcastAddress)) {
			Collection<NetworkStack> stacks = network.getBroadcastHosts();
			for (NetworkStack stack: stacks)
				stack.handleDatagram(destination, p).scheduleFrom(timeline, qdelay);
		} else {
			NetworkStack stack = network.getHost(destination.getAddress());
			if (stack!=null)
				stack.handleDatagram(destination, p).scheduleFrom(timeline, qdelay);
		}
	}

	public Calibration getConfig() {
		return network.config;
	}

	@Override
	public void close() {
		ArrayList<ClientTCPSocket> l = new ArrayList<ClientTCPSocket>(connTCP);
		for(ClientTCPSocket tcp: l) {
			tcp.shutdownInput();
			tcp.shutdownOutput();
		}
	}
}
