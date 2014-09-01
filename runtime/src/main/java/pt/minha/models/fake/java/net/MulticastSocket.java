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

package pt.minha.models.fake.java.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;

public class MulticastSocket extends DatagramSocket {
	
    public MulticastSocket() throws IOException {
        super();
    }
    
    public MulticastSocket(int port) throws IOException {
        super(port);
    }
    
    public MulticastSocket(SocketAddress address) throws SocketException {
    	super(address);
    }

    public void joinGroup(InetAddress addr) throws IOException {
    	udp.joinGroup(addr);
    }

    public void joinGroup(SocketAddress addr, NetworkInterface intf) throws IOException {
    	joinGroup(((InetSocketAddress)addr).getAddress());
    }

    public void leaveGroup(InetAddress addr) throws IOException {
    	udp.leaveGroup(addr);
    }
    
    public void setTimeToLive(int ttl) throws IOException {
    	// not implemented
    }
    
    public void setInterface(InetAddress addr) throws IOException {
    	// not implemented
    }

    public InetAddress getInterface() throws IOException {
    	return udp.getNetwork().getLocalAddress();
    }
    
    public void setNetworkInterface(NetworkInterface intf) {
    	// TODO
    }
}
