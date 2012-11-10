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
	private final Map<InetSocketAddress, DatagramSocketUpcalls> socketsUDP = new HashMap<InetSocketAddress, DatagramSocketUpcalls>();
	private final Map<InetSocketAddress, ServerSocketUpcalls> socketsTCP = new HashMap<InetSocketAddress, ServerSocketUpcalls>();

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
	
	protected void DatagramPacketQueue(InetSocketAddress destination, DatagramPacket packet) {
		DatagramSocketUpcalls sgds = socketsUDP.get(destination);
		if ( null!=sgds ) {
			sgds.queue(packet);
		}
	}
		
	public ServerSocketUpcalls lookupServerSocket(InetSocketAddress destination) throws IOException {
		return socketsTCP.get(destination);
	}	
}
