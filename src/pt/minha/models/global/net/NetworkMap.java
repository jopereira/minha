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
import java.util.Random;

public class NetworkMap {
	private final Random random = new Random();
	private final List<InetAddress> hosts = new LinkedList<InetAddress>();
	private final Map<InetSocketAddress, Object> socketsUDP = new HashMap<InetSocketAddress, Object>();
	private final Map<InetSocketAddress, Object> socketsTCP = new HashMap<InetSocketAddress, Object>();
	private final Map<String, Object> connectedSocketsTCP = new HashMap<String, Object>();

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
	
	
	public InetAddress addHost(String host) throws UnknownHostException {
		if ( host.startsWith("127.") )
			throw new UnknownHostException("Invalid address: " + host);
		
		InetAddress ia = InetAddress.getByName(host);
		if ( hosts.contains(ia) )
			throw new UnknownHostException("Address already used: " + ia.toString());
		
		hosts.add(ia);
		return ia;		
	}
	
		
	public InetAddress getAvailableInetAddress() throws UnknownHostException {
		InetAddress ia;
		do {
			ia = InetAddress.getByName(getAvailableIP());
		}
		while ( hosts.contains(ia) );
		
		hosts.add(ia);
		return ia;
	}
	
	
	public boolean existsHost(InetAddress ia) {
		return hosts.contains(ia);
	}

	
	public InetSocketAddress addSocket(Protocol protocol, InetSocketAddress isa, Object ab) throws SocketException {
		switch (protocol) {
		case UDP:
			if ( socketsUDP.containsKey(isa) )
				throw new BindException(isa.toString() + " Cannot assign requested address on "+protocol);

			socketsUDP.put(isa, ab);
			return isa;
		case TCP:
			if ( socketsTCP.containsKey(isa) )
				throw new BindException(isa.toString() + " Cannot assign requested address on "+protocol);

			socketsTCP.put(isa, ab);
			return isa;
		default:
			throw new BindException(isa.toString() + " Cannot assign requested address on "+protocol);
		}
	}

	
	public boolean existsSocket(Protocol protocol, InetSocketAddress isa) {
		switch (protocol) {
		case UDP:
			return socketsUDP.containsKey(isa);
		case TCP:
			return socketsTCP.containsKey(isa);
		default:
			return false;
		}
	}


	public void removeSocket(Protocol protocol, InetSocketAddress isa) {
		switch (protocol) {
		case UDP:
			if ( socketsUDP.containsKey(isa) )
				socketsUDP.remove(isa);
			break;
		case TCP:
			if ( socketsTCP.containsKey(isa) )
				socketsTCP.remove(isa);
		}
	}
	
	
	protected void DatagramPacketQueue(InetSocketAddress destination, DatagramPacket packet) {
		Object o = socketsUDP.get(destination);
		if ( null!=o && containsInterface(o, DatagramSocketInterface.class) ) {
			DatagramSocketInterface sgds = (DatagramSocketInterface) o;
			sgds.queue(packet);
		}
	}
	
	
	public boolean isServerSocket(InetSocketAddress destination) {
		Object o = socketsTCP.get(destination);
		if ( null != o ) {
			if ( containsInterface(o, ServerSocketInterface.class) )
				return true;
		}

		return false;
	}
	
	
	public String ServerSocketConnect(InetSocketAddress destination, InetSocketAddress source, Object clientSocket) throws IOException {
		Object o = socketsTCP.get(destination);
		if ( null!=o && containsInterface(o, ServerSocketInterface.class) ) {
			ServerSocketInterface ssi = (ServerSocketInterface) o;
			ssi.queueConnect(destination, source);
			
			// register socket between client socket and server socket
			String connectedSocketKeyPrefix = "["+destination.getAddress().getHostAddress()+":"+destination.getPort()+"<-"+source.getAddress().getHostAddress()+":"+source.getPort()+"]";
		    addConnectedSocket(connectedSocketKeyPrefix+"-client", clientSocket);
		    return connectedSocketKeyPrefix+"-server";
		}
		
		throw new IOException("connect: unable to connect to " + destination.toString());
	}

	
	public String SocketScheduleServerSocketAcceptDone(InetSocketAddress destination, InetSocketAddress source, Object serverClientSocket) throws IOException {
		Object o = socketsTCP.get(destination);
		if ( null!=o && containsInterface(o, SocketInterface.class) ) {
			SocketInterface si = (SocketInterface) o;
			si.scheduleServerSocketAcceptDone();
			
			// register socket between server socket and client socket
			String connectedSocketKeyPrefix = "["+source.getAddress().getHostAddress()+":"+source.getPort()+"<-"+destination.getAddress().getHostAddress()+":"+destination.getPort()+"]";
			addConnectedSocket(connectedSocketKeyPrefix+"-server", serverClientSocket);
			return connectedSocketKeyPrefix+"-client";
		}

		throw new IOException("connect: unable to schedule ServerSocket.accept() done on " + destination.toString());
	}

	
	protected void SocketScheduleRead(String key, TCPPacket p) throws IOException {
		Object o = connectedSocketsTCP.get(key);
		if ( null!=o && containsInterface(o, SocketInterface.class) ) {
			SocketInterface si = (SocketInterface) o;
			si.scheduleRead(p);
			return;
		}
		
		/* This should fail silently, as some racing is expected when
		 * both endpoints simultaneously close the socket after exchanging
		 * an agreed amound of data (e.g. chunked encoding). */
	}
	

	protected void SocketAcknowledge(String key, TCPPacketAck p) {
		if ( null == key ) {
			if ( Log.network_tcp_stream_log_enabled )
				Log.TCPdebug("Unable to acknowledge: null key");
			pt.minha.models.global.Debug.println("Unable to acknowledge: null key");
			System.exit(1);
		}
		
		Object o = connectedSocketsTCP.get(key);
		if ( null!=o && containsInterface(o, SocketInterface.class) ) {
			SocketInterface si = (SocketInterface) o;
			si.acknowledge(p);
			return;
		}
		
		if ( Log.network_tcp_stream_log_enabled )
			Log.TCPdebug("Unable to acknowledge on " + key);
	}

	
	private void addConnectedSocket(String key, Object o) {
		if ( null == key ) {
			pt.minha.models.global.Debug.println("Unable to addConnectedSocket: null key " + o);
			System.exit(1);
		}
		
		connectedSocketsTCP.put(key, o);
	}


	public void removeConnectedSocket(String key) {
		if ( null == key ) {
			pt.minha.models.global.Debug.println("Unable to removeConnectedSocket: null key");
			System.exit(1);
		}
		
		connectedSocketsTCP.remove(key);
	}

	
	@SuppressWarnings("rawtypes")
	private boolean containsInterface(Object o, Class interfac) {
		for (Class intf : o.getClass().getInterfaces()) {
			if ( intf.equals(interfac) )
				return true;
		}
		
		return false;
	}
	
	
	/* debug methods */
	public int getSocketInputStreamSN(String key) throws IOException {
		Object o = connectedSocketsTCP.get(key);
		if ( null!=o && containsInterface(o, SocketInterface.class) ) {
			SocketInterface si = (SocketInterface) o;
			
			return si.getSocketInputStreamSN();
		}
		
		throw new IOException("Unable to get SocketInputStream sn on " + key);
	}
	
	
	public int getSocketOutputStreamSN(String key) throws IOException {
		Object o = connectedSocketsTCP.get(key);
		if ( null!=o && containsInterface(o, SocketInterface.class) ) {
			SocketInterface si = (SocketInterface) o;
			
			return si.getSocketOutputStreamSN();
		}
		
		throw new IOException("Unable to get SocketOutputStream sn on " + key);
	}

	public void checkForgottenSockets() {
		pt.minha.models.global.Debug.println("checkForgottenSockets: "+this.connectedSocketsTCP);
		for (Map.Entry<String,Object> e : this.connectedSocketsTCP.entrySet()) {
			if ( null!=e.getValue() && containsInterface(e.getValue(), SocketInterface.class) ) {
				SocketInterface si = (SocketInterface) e.getValue();
				
				try {
					int snIN = si.getSocketInputStreamSN();
					int snOUT = si.getRemoteSocketOutputStreamSN();
					/* snIN+1: if close() method was invoked by me I will not read any Close packet */
					if ( (snIN!=snOUT) && (snIN+1!=snOUT) )
						pt.minha.models.global.Debug.println("Forgotten Socket: "+e.getKey()+"; read: "+snIN+"; wrote: "+snOUT);
				}
				catch (IOException t) {
					t.printStackTrace();
				}
				
			}
		}
	}
}
