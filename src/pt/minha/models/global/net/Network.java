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
import java.net.InetSocketAddress;
import java.util.LinkedList;

import pt.minha.api.World;
import pt.minha.kernel.log.Logger;
import pt.minha.kernel.log.SimpleLoggerLog4j;
import pt.minha.kernel.simulation.Event;
import pt.minha.kernel.util.PropertiesLoader;

public class Network {
	private static final long BUFFER = 128*1024;
	private static final long BANDWIDTH = NetworkCalibration.networkBandwidth/8; // bytes
	private static final long RESOLUTION = 40000; // 40 us;
	private static final long DELTA = BANDWIDTH/(1000000000/RESOLUTION);
	private static long current_bandwidth = 0;
	
	private static final WakeEvent wakeEvent = new WakeEvent();
	private static boolean wakeEventEnabled = false;
	
	private static final LinkedList<TCPPacket> queue = new LinkedList<TCPPacket>();
	
	/*
	 * TCP
	 */
	public static void send(TCPPacket p) throws IOException {
		// delay send
		if  ( (current_bandwidth+p.getSize())>BUFFER || !queue.isEmpty()) {
			if ( Log.network_tcp_stream_log_enabled )
				Log.TCPdebug("Network queue: "+p.getSn()+" to "+p.getKey()+" "+p.getType());
			
			queue.add(p);
			return;
		}
		
		if ( Log.network_tcp_stream_log_enabled )
			Log.TCPdebug("Network send: "+p.getSn()+" to "+p.getKey()+" "+p.getType());
		NetworkMap.SocketScheduleRead(p.getKey(), p);
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

	public static void send(InetSocketAddress destination, DatagramPacket p) {
		if ( NetworkCalibration.isLostPacket() )
			return;
		
		// drop packet
		if  ( (current_bandwidth+p.getLength()) > BUFFER )
			return;
		
		NetworkMap.DatagramPacketQueue(destination, p);
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
	
	public static void acknowledge(TCPPacketAck p) {
		if ( Log.network_tcp_stream_log_enabled )
			Log.TCPdebug("Network acknowledge: "+p.getSn()+" to "+p.getKey()+" "+p.getType());
		
		NetworkMap.SocketAcknowledge(p.getKey(), p);
	}
	
	
	private static class WakeEvent extends Event {
		public WakeEvent() {
			super(World.timeline);
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
					try {
						if ( Log.network_tcp_stream_log_enabled )
							Log.TCPdebug("Network send from queue: "+p.getSn()+" to "+p.getKey()+" "+p.getType());
						NetworkMap.SocketScheduleRead(p.getKey(), p);
						queue.remove(0);
						current_bandwidth += p.getSize();
						if ( bandwidth_log_enabled )
							bandwidth_log += p.getSize();
					}
					catch (IOException e) {
						pt.minha.models.global.Debug.println("Exception sending packets from network queue: "+p.toString());
						e.printStackTrace();
						System.exit(1);
					}
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
	private static long bandwidth_log = 0;
	private static final long BandwidthLoggerEventDELTA = 1000000000;
	private static BandwidthLoggerEvent bandwidthLoggerEvent;
	private static boolean bandwidth_log_enabled = false;
	static {
		try {
			PropertiesLoader conf = new PropertiesLoader(Logger.LOG_CONF_FILENAME);
			bandwidth_log_enabled = Boolean.parseBoolean(conf.getProperty("network.bandwith.log"));
			if ( bandwidth_log_enabled )
				bandwidthLoggerEvent = new BandwidthLoggerEvent();
		} catch (Exception e) {
			// log will be disabled
		}
	}
	
	private static class BandwidthLoggerEvent extends Event {
		private Logger logger;

		public BandwidthLoggerEvent() {
			super(World.timeline);
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
}
