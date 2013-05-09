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

package pt.minha.models.global.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pt.minha.kernel.simulation.Event;
import pt.minha.kernel.simulation.Timeline;
import pt.minha.kernel.simulation.Usage;

public class Network {
	private Usage usage;

	private Timeline timeline;
	
	// address generation and registry
	private int ip1 = 10;
	private int ip2 = 0;
	private int ip3 = 0;
	private int ip4 = 0;
	private final Map<InetAddress, NetworkStack> hosts = new HashMap<InetAddress, NetworkStack>();
	private final Map<InetAddress, List<NetworkStack>> multicastSockets = new HashMap<InetAddress, List<NetworkStack>>();
	
	// bandwidth control
	NetworkConfig config;
	private final long BUFFER = 128*1024;
	private final long BANDWIDTH; // bytes
	private final long RESOLUTION = 40000; // 40 us;
	private final long DELTA;
	private long current_bandwidth = 0;
	private final WakeEvent wakeEvent;
	private boolean wakeEventEnabled = false;
	private final LinkedList<TCPPacket> queue = new LinkedList<TCPPacket>();
		
	public Network(Timeline timeline, NetworkConfig config) {
		this.timeline = timeline;
		this.wakeEvent = new WakeEvent();
		usage = new Usage(timeline, 1000000000, "network", 1, "bytes/s", 0);
		this.config = config;
		
		BANDWIDTH = config.networkBandwidth/8; // bytes
		DELTA = BANDWIDTH/(1000000000/RESOLUTION);
	}
		
	private String getAvailableIP() throws UnknownHostException {
		if ( ip4 < 254 ) {
			ip4++; 
		}
		else if (ip3 < 254 ) {
			ip4 = 1;
			ip3++;
		}
		else if (ip2 < 254 ) {
			ip4 = 1;
			ip3 = 1;
			ip2++;
		}
		else {
			throw new UnknownHostException("No more addresses available");
		}
		
		return ip1+"."+ip2+"."+ip3+"."+ip4;
	}
	
	public InetAddress addHost(String host, NetworkStack stack) throws UnknownHostException {
		InetAddress ia;
		if (host==null)
			do {
				ia = InetAddress.getByName(getAvailableIP());
			}
			while ( hosts.containsKey(ia) );
		else {
			if ( host.startsWith("127.") )
				throw new UnknownHostException("Invalid address: " + host);
			
			ia = InetAddress.getByName(host);
			
			if ( hosts.containsKey(ia) )
				throw new UnknownHostException("Address already used: " + ia.toString());
		}
		hosts.put(ia, stack);
		return ia;		
	}		

	public void addToGroup(InetAddress mcastaddr, NetworkStack ms) throws IOException {
		if ( !multicastSockets.containsKey(mcastaddr) )
			multicastSockets.put(mcastaddr, new LinkedList<NetworkStack>());
		
		List<NetworkStack> sockets = multicastSockets.get(mcastaddr);
    	if ( !sockets.contains(ms) )
    		sockets.add(ms);
	}
		
	public void removeFromGroup(InetAddress mcastaddr, NetworkStack ms) throws IOException {
    	if (multicastSockets.containsKey(mcastaddr)) {
    		if ( !multicastSockets.get(mcastaddr).remove(ms) )
    			throw new IOException("MulticastSocket '"+ms+"' is not in group '"+mcastaddr+"'");
    	}
    	else
    		throw new IOException("Multicast group '"+mcastaddr+"' do not exists");
	}
	
	/*
	 * TCP
	 */
	public void relayTCPConnect(final InetSocketAddress serverAddr, final TCPPacket tcpPacket) {
		new Event(timeline) {
			public void run() {
				NetworkStack target = hosts.get(serverAddr.getAddress());
				if (target==null)
					tcpPacket.getSource().scheduleRead(new TCPPacket(null, tcpPacket.getSource(), 0, 0, new byte[0], TCPPacket.RST)).scheduleFrom(timeline, 0);
				else
					target.handleConnect(serverAddr, tcpPacket).scheduleFrom(timeline, 0);				
			}			
		}.scheduleFrom(tcpPacket.getSource().getNetwork().getTimeline(), 0);
	}
		
	public Event relayTCPData(final TCPPacket p) {
		return new Event(timeline) {
			public void run() {
				// delay send
				if  ( (current_bandwidth+p.getSize())>BUFFER || !queue.isEmpty()) {
					
					queue.add(p);
					return;
				}
				
				p.getDestination().scheduleRead(p).scheduleFrom(timeline, config.getNetworkDelay(p.getSize()));
				current_bandwidth += p.getSize();
				usage.using(p.getSize());
				
				if ( !wakeEventEnabled ) {
					wakeEventEnabled = true;
					wakeEvent.schedule(RESOLUTION);
				}
			}			
		};
	}

	public Event relayUDP(final InetSocketAddress destination, final DatagramPacket p) {
		return new Event(timeline) {
			public void run() {
				if ( config.isLostPacket() )
					return;
				
				// drop packet
				if  ((current_bandwidth+p.getLength()) > BUFFER)
					return;
				
				if (destination.getAddress().isMulticastAddress()) {
					List<NetworkStack> stacks = multicastSockets.get(destination.getAddress());
					if (stacks != null)
						for (NetworkStack stack: stacks)
							stack.handleDatagram(destination, p).scheduleFrom(timeline, 0);
				
				} else {
					NetworkStack stack = hosts.get(destination.getAddress());
					if (stack!=null)
						stack.handleDatagram(destination, p).scheduleFrom(timeline, 0);
				}
				
				current_bandwidth += p.getLength();
				usage.using(p.getLength());
				
				if ( !wakeEventEnabled ) {
					wakeEventEnabled = true;
					wakeEvent.schedule(RESOLUTION);
				}
			}			
		};
	}

	private class WakeEvent extends Event {
		public WakeEvent() {
			super(timeline);
		}

		public void run() {
			if ( 0==current_bandwidth && 0==queue.size())
				wakeEventEnabled = false;
			else if ( current_bandwidth >= DELTA )
				current_bandwidth -= DELTA;
			else if ( current_bandwidth > 0 )
				current_bandwidth = 0;
			
			while ( queue.size()>0 ) {
				TCPPacket p = queue.get(0);
				if ( (current_bandwidth+p.getSize()) > BUFFER )
					break;
				else {
					p.getDestination().scheduleRead(p).scheduleFrom(timeline, config.getNetworkDelay(p.getSize()));
					queue.remove(0);
					current_bandwidth += p.getSize();
					usage.using(p.getSize());
				}
			}
			
			// schedule next run
			if ( wakeEventEnabled )
				this.schedule(RESOLUTION);
		}
	}
}