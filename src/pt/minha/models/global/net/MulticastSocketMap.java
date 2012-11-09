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
import java.net.SocketException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;



public class MulticastSocketMap {
	// multicast address:port -> [Multicast Sockets]
	private final Map<InetSocketAddress, List<DatagramSocketUpcalls>> multicastSockets = new HashMap<InetSocketAddress, List<DatagramSocketUpcalls>>();

	
	public void add(InetSocketAddress mcastaddr, DatagramSocketUpcalls ms) throws IOException {
		if ( !multicastSockets.containsKey(mcastaddr) )
			multicastSockets.put(mcastaddr, new LinkedList<DatagramSocketUpcalls>());
		
		List<DatagramSocketUpcalls> sockets = multicastSockets.get(mcastaddr);
    	if ( !sockets.contains(ms) )
    		sockets.add(ms);
	}
	
	
	public void remove(InetSocketAddress mcastaddr, DatagramSocketUpcalls ms) throws IOException {
    	if (multicastSockets.containsKey(mcastaddr)) {
    		if ( !multicastSockets.get(mcastaddr).remove(ms) )
    			throw new IOException("MulticastSocket '"+ms+"' is not in group '"+mcastaddr+"'");
    	}
    	else
    		throw new IOException("Multicast group '"+mcastaddr+"' do not exists");
	}
	
	
	public boolean contains(InetSocketAddress mcastaddr) {
		return multicastSockets.containsKey(mcastaddr);
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
}
