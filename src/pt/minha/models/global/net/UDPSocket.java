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
import java.util.ArrayList;
import java.util.List;

import pt.minha.models.global.io.BlockingHelper;

/**
 * Simulated UDP socket.
 * 
 * @author jop
 */
public class UDPSocket extends AbstractSocket {
	private List<DatagramPacket> incoming = new ArrayList<DatagramPacket>();
	
	public BlockingHelper readers, writers;

	private InetSocketAddress remote;

	public UDPSocket(NetworkStack stack) {
		super(stack);
		readers = new BlockingHelper() {
			public boolean isReady() {
				return !incoming.isEmpty();
			}
		};
		writers = new BlockingHelper() {
			public boolean isReady() {
				/* This is needed to support select() in datagram channels. */
				return true;
			}
		};
	}

	@Override
	public void bind(InetSocketAddress addr) throws SocketException {
		super.bind(addr);
		stack.addUDPSocket(getLocalAddress(), this);
	}

    public void joinGroup(InetAddress addr) throws IOException {
    	stack.getNetwork().addToGroup(addr, stack);
    	stack.addUDPSocket(new InetSocketAddress(addr, getLocalAddress().getPort()), this);
    }

    public void leaveGroup(InetAddress addr) throws IOException {
    	stack.getNetwork().removeFromGroup(addr, stack);
    	stack.removeUDPSocket(new InetSocketAddress(addr, getLocalAddress().getPort()));
    }
	
    public void connect(InetSocketAddress addr) {
    	remote = addr;
    }
    
	public void send(DatagramPacket packet) throws SocketException {
		byte[] data = new byte[packet.getLength()];
		System.arraycopy(packet.getData(), packet.getOffset(), data, 0, data.length);
		DatagramPacket dp = new DatagramPacket(data, data.length, getLocalAddress());
		InetSocketAddress destination = new InetSocketAddress(packet.getAddress(), packet.getPort());
		stack.getNetwork().relayUDP(destination, dp);
	}

	public DatagramPacket receive() {
		return incoming.remove(0);
	}
	
	public void shutdown() {
		stack.removeUDPSocket(getLocalAddress());
	}

	public void queue(DatagramPacket packet) {
		incoming.add(packet);
		readers.wakeup();
	}
	
	public InetSocketAddress getRemoteAddress() {
		return remote;
	}
}
