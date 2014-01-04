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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import pt.minha.api.Calibration;

public class Network {
	// address generation and registry
	private int ip1 = 10;
	private int ip2 = 0;
	private int ip3 = 0;
	private int ip4 = 0;
	private final Map<InetAddress, NetworkStack> hosts = new HashMap<InetAddress, NetworkStack>();
	private final Map<InetAddress, List<NetworkStack>> multicastSockets = new HashMap<InetAddress, List<NetworkStack>>();
	
	Calibration config;
	
	public Network(Calibration nc) {
		this.config = nc;
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
	
	public synchronized InetAddress addHost(String host, NetworkStack stack) throws UnknownHostException {
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
	
	public synchronized NetworkStack getHost(InetAddress address) {
		return hosts.get(address);
	}

	public synchronized void addToGroup(InetAddress mcastaddr, NetworkStack ms) throws IOException {
		if ( !multicastSockets.containsKey(mcastaddr) )
			multicastSockets.put(mcastaddr, new CopyOnWriteArrayList<NetworkStack>());
		
		List<NetworkStack> sockets = multicastSockets.get(mcastaddr);
    	if ( !sockets.contains(ms) )
    		sockets.add(ms);
	}
		
	public synchronized void removeFromGroup(InetAddress mcastaddr, NetworkStack ms) throws IOException {
    	if (multicastSockets.containsKey(mcastaddr)) {
    		if ( !multicastSockets.get(mcastaddr).remove(ms) )
    			throw new IOException("MulticastSocket '"+ms+"' is not in group '"+mcastaddr+"'");
    	}
    	else
    		throw new IOException("Multicast group '"+mcastaddr+"' do not exists");
	}
	
	public synchronized List<NetworkStack> getMulticastHosts(InetAddress address) {
		return multicastSockets.get(address);
	}
}