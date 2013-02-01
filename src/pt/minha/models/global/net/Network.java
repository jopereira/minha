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
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.minha.kernel.simulation.Event;
import pt.minha.kernel.simulation.Timeline;

public class Network {
	private Logger logger = LoggerFactory.getLogger(Network.class);

	private Timeline timeline;
	
	// address generation and registry
	private int ip1 = 10;
	private int ip2 = 0;
	private int ip3 = 0;
	private int ip4 = 0;
	private final Map<InetAddress, NetworkStack> hosts = new HashMap<InetAddress, NetworkStack>();
	private final Map<InetAddress, List<NetworkStack>> multicastSockets = new HashMap<InetAddress, List<NetworkStack>>();
	
	// bandwidth control
	private final long BUFFER = 128*1024;
	private final long BANDWIDTH = NetworkCalibration.networkBandwidth/8; // bytes
	private final long RESOLUTION = 40000; // 40 us;
	private final long DELTA = BANDWIDTH/(1000000000/RESOLUTION);
	private long current_bandwidth = 0;
	private final WakeEvent wakeEvent;
	private boolean wakeEventEnabled = false;
	private final LinkedList<TCPPacket> queue = new LinkedList<TCPPacket>();
		
	public Network(Timeline timeline) {
		this.timeline = timeline;
		this.wakeEvent = new WakeEvent();
		if (logger.isInfoEnabled())
			bandwidthLoggerEvent = new BandwidthLoggerEvent();
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
					tcpPacket.getSource().scheduleRead(new TCPPacket(null, tcpPacket.getSource(), 0, 0, new byte[0], TCPPacket.RST));
				else
					target.handleConnect(serverAddr, tcpPacket);				
			}			
		}.schedule(0);
	}
		
	public void relayTCPData(final TCPPacket p) {
		new Event(timeline) {
			public void run() {
				// delay send
				if  ( (current_bandwidth+p.getSize())>BUFFER || !queue.isEmpty()) {
					
					queue.add(p);
					return;
				}
				
				p.getDestination().scheduleRead(p);
				current_bandwidth += p.getSize();
				if (logger.isInfoEnabled())
					bandwidth_log += p.getSize();
				
				if ( !wakeEventEnabled ) {
					wakeEventEnabled = true;
					wakeEvent.schedule(RESOLUTION);
					if (logger.isInfoEnabled())
						bandwidthLoggerEvent.schedule(BandwidthLoggerEventDELTA);
				}
			}			
		}.schedule(0);
	}

	public void relayUDP(final InetSocketAddress destination, final DatagramPacket p) {
		new Event(timeline) {
			public void run() {
				if ( NetworkCalibration.isLostPacket() )
					return;
				
				// drop packet
				if  ( (current_bandwidth+p.getLength()) > BUFFER )
					return;
				
				NetworkStack stack = hosts.get(destination.getAddress());
				if (stack!=null)
					stack.handleDatagram(destination, p);
				
				current_bandwidth += p.getLength();
				if (logger.isInfoEnabled())
					bandwidth_log += p.getLength();
				
				if ( !wakeEventEnabled ) {
					wakeEventEnabled = true;
					wakeEvent.schedule(RESOLUTION);
					if (logger.isInfoEnabled())
						bandwidthLoggerEvent.schedule(BandwidthLoggerEventDELTA);
				}
			}			
		}.schedule(0);
	}

	public void relayMulticast(final InetSocketAddress source, final DatagramPacket packet) throws SocketException {
		new Event(timeline) {
			public void run() {
				List<NetworkStack> targets = multicastSockets.get(packet.getSocketAddress());
				if ( null == targets )
					return;
				
				// Lost in sender
				if ( NetworkCalibration.isLostPacket() )
					return;
						
				for (NetworkStack target : targets) {
					// Lost in receiver
					if ( NetworkCalibration.isLostPacket() )
						continue;
					
					byte[] data = new byte[packet.getLength()];
					System.arraycopy(packet.getData(), packet.getOffset(), data, 0, data.length);
					try {
						DatagramPacket dp = new DatagramPacket(data, data.length, source);
						target.handleDatagram((InetSocketAddress)dp.getSocketAddress(), dp);
					} catch (SocketException e) {
						// drop packet
					}
				}
			}			
		}.schedule(0);
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
					p.getDestination().scheduleRead(p);
					queue.remove(0);
					current_bandwidth += p.getSize();
					if (logger.isInfoEnabled())
						bandwidth_log += p.getSize();
				}
			}
			
			// schedule next run
			if ( wakeEventEnabled )
				this.schedule(RESOLUTION);
		}
	}

	/*
	 * network logger
	 */
	private long bandwidth_log = 0;
	private final long BandwidthLoggerEventDELTA = 1000000000;
	private BandwidthLoggerEvent bandwidthLoggerEvent;
		
	private class BandwidthLoggerEvent extends Event {

		public BandwidthLoggerEvent() {
			super(timeline);
		}

		public void run() {
			logger.info("net usage:{}:{}", getTimeline().getTime(),bandwidth_log);
			bandwidth_log = 0;
			
			// schedule next run
			if ( wakeEventEnabled )
				this.schedule(BandwidthLoggerEventDELTA);
		}
	}
}