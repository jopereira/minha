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
import java.util.Random;

import pt.minha.kernel.log.Logger;
import pt.minha.kernel.log.SimpleLoggerLog4j;
import pt.minha.kernel.simulation.Event;
import pt.minha.kernel.simulation.Timeline;
import pt.minha.kernel.util.PropertiesLoader;

public class Network {
	private final long BUFFER = 128*1024;
	private final long BANDWIDTH = NetworkCalibration.networkBandwidth/8; // bytes
	private final long RESOLUTION = 40000; // 40 us;
	private final long DELTA = BANDWIDTH/(1000000000/RESOLUTION);
	private long current_bandwidth = 0;
	
	private final WakeEvent wakeEvent;
	private boolean wakeEventEnabled = false;
	
	private Timeline timeline;
	
	private final LinkedList<TCPPacket> queue = new LinkedList<TCPPacket>();
	
	public Network(Timeline timeline) {
		this.timeline = timeline;
		this.wakeEvent = new WakeEvent();
		try {
			PropertiesLoader conf = new PropertiesLoader(Logger.LOG_CONF_FILENAME);
			bandwidth_log_enabled = Boolean.parseBoolean(conf.getProperty("network.bandwith.log"));
			if ( bandwidth_log_enabled )
				bandwidthLoggerEvent = new BandwidthLoggerEvent();
		} catch (Exception e) {
			// log will be disabled
		}
	}
	
	/*
	 * TCP
	 */
	public void send(TCPPacket p) throws IOException {
		// delay send
		if  ( (current_bandwidth+p.getSize())>BUFFER || !queue.isEmpty()) {
			if ( Log.network_tcp_stream_log_enabled )
				Log.TCPdebug("Network queue: "+p.getSn()+" "+p.getType());
			
			queue.add(p);
			return;
		}
		
		if ( Log.network_tcp_stream_log_enabled )
			Log.TCPdebug("Network send: "+p.getSn()+" "+p.getType());
		p.getDestination().scheduleRead(p);
		current_bandwidth += p.getSize();
		if ( bandwidth_log_enabled )
			bandwidth_log += p.getSize();
		
		if ( !wakeEventEnabled ) {
			wakeEventEnabled = true;
			wakeEvent.schedule(RESOLUTION);
			if ( bandwidth_log_enabled )
				bandwidthLoggerEvent.schedule(BandwidthLoggerEventDELTA);
		}
	}

	public void send(InetSocketAddress destination, DatagramPacket p) {
		if ( NetworkCalibration.isLostPacket() )
			return;
		
		// drop packet
		if  ( (current_bandwidth+p.getLength()) > BUFFER )
			return;
		
		NetworkStack stack = hosts.get(destination.getAddress());
		if (stack!=null)
			stack.handleDatagram(destination, p);
		
		current_bandwidth += p.getLength();
		if ( bandwidth_log_enabled )
			bandwidth_log += p.getLength();
		
		if ( !wakeEventEnabled ) {
			wakeEventEnabled = true;
			wakeEvent.schedule(RESOLUTION);
			if ( bandwidth_log_enabled )
				bandwidthLoggerEvent.schedule(BandwidthLoggerEventDELTA);
		}
	}
	
	public void acknowledge(TCPPacketAck p) {
		if ( Log.network_tcp_stream_log_enabled )
			Log.TCPdebug("Network acknowledge: "+p.getSn()+" "+p.getType());
		
		p.getDestination().acknowledge(p);
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
					if ( Log.network_tcp_stream_log_enabled )
						Log.TCPdebug("Network send from queue: "+p.getSn()+" "+p.getType());
					//networkMap.SocketScheduleRead(p.getKey(), p);
					p.getDestination().scheduleRead(p);
					queue.remove(0);
					current_bandwidth += p.getSize();
					if ( bandwidth_log_enabled )
						bandwidth_log += p.getSize();
				}
			}
			
			// schedule next run
			if ( wakeEventEnabled )
				this.schedule(RESOLUTION);
		}
	}

	// multicast address:port -> [Multicast Sockets]
	private final Map<InetSocketAddress, List<DatagramSocketUpcalls>> multicastSockets = new HashMap<InetSocketAddress, List<DatagramSocketUpcalls>>();

	public void addToGroup(InetSocketAddress mcastaddr, DatagramSocketUpcalls ms) throws IOException {
		if ( !multicastSockets.containsKey(mcastaddr) )
			multicastSockets.put(mcastaddr, new LinkedList<DatagramSocketUpcalls>());
		
		List<DatagramSocketUpcalls> sockets = multicastSockets.get(mcastaddr);
    	if ( !sockets.contains(ms) )
    		sockets.add(ms);
	}
	
	
	public void removeFromGroup(InetSocketAddress mcastaddr, DatagramSocketUpcalls ms) throws IOException {
    	if (multicastSockets.containsKey(mcastaddr)) {
    		if ( !multicastSockets.get(mcastaddr).remove(ms) )
    			throw new IOException("MulticastSocket '"+ms+"' is not in group '"+mcastaddr+"'");
    	}
    	else
    		throw new IOException("Multicast group '"+mcastaddr+"' do not exists");
	}
	
		
	// Stub method to send messages between sockets
	public void MulticastSocketQueue(InetSocketAddress source, DatagramPacket packet) throws SocketException {
		List<DatagramSocketUpcalls> targets = multicastSockets.get(packet.getSocketAddress());
		if ( null == targets )
			return;
		
		// Lost in sender
		if ( NetworkCalibration.isLostPacket() )
			return;
				
		for (DatagramSocketUpcalls target : targets) {
			// Lost in receiver
			if ( NetworkCalibration.isLostPacket() )
				continue;
			
			byte[] data = new byte[packet.getLength()];
			System.arraycopy(packet.getData(), packet.getOffset(), data, 0, data.length);
			DatagramPacket dp = new DatagramPacket(data, data.length, source);
			target.queue(dp);			
		}
	}

	public void routeConnect(InetSocketAddress destination, InetSocketAddress source, SocketUpcalls upcalls) {
		NetworkStack target = hosts.get(destination.getAddress());
		if (target==null)
			upcalls.accepted(null);
		target.handleConnect(destination, source, upcalls);
	}
	
	/*
	 * network logger
	 */
	private long bandwidth_log = 0;
	private final long BandwidthLoggerEventDELTA = 1000000000;
	private BandwidthLoggerEvent bandwidthLoggerEvent;
	private boolean bandwidth_log_enabled = false;
	
	
	private class BandwidthLoggerEvent extends Event {
		private Logger logger;

		public BandwidthLoggerEvent() {
			super(timeline);
			this.logger = new SimpleLoggerLog4j("log/network.log");
		}

		public void run() {
			logger.debug(this.getTimeline().getTime()+" "+bandwidth_log);
			bandwidth_log = 0;
			
			// schedule next run
			if ( wakeEventEnabled )
				this.schedule(BandwidthLoggerEventDELTA);
		}
	}
	
	private final Random random = new Random();
	private final Map<InetAddress,NetworkStack> hosts = new HashMap<InetAddress, NetworkStack>();
	
	// IP generation
	private int ip1 = 10;
	private int ip2 = 0;
	private int ip3 = 0;
	private int ip4 = 0;


	public String generateMACAddress() {
		String mac[] = new String[6];
		
		mac[0] = "00";
		mac[1] = "22";
		mac[2] = "62";
		mac[3] = Integer.toHexString(random.nextInt(127));
		if ( 1 == mac[3].length() )
			mac[3] = "0"+mac[3];
		mac[4] = Integer.toHexString(random.nextInt(256));
		if ( 1 == mac[4].length() )
			mac[4] = "0"+mac[4];
		mac[5] = Integer.toHexString(random.nextInt(256));
		if ( 1 == mac[5].length() )
			mac[5] = "0"+mac[5];
		
		return mac[0]+":"+mac[1]+":"+mac[2]+":"+mac[3]+":"+mac[4]+":"+mac[5];
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
}