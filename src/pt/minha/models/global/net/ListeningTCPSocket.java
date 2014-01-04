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

package pt.minha.models.global.net;

import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;

import pt.minha.models.global.io.BlockingHelper;

public class ListeningTCPSocket extends AbstractSocket {
	
	private int backlog;
	private final List<TCPPacket> incomingAccept = new LinkedList<TCPPacket>();
	public BlockingHelper acceptors;

	public ListeningTCPSocket(NetworkStack stack) {
		super(stack);
		acceptors = new BlockingHelper() {
			public boolean isReady() {
				return !incomingAccept.isEmpty();
			}
		};
	}
	
	public void listen(int backlog) throws SocketException {
		if (backlog<=0) backlog = 1;
		if (backlog>5) backlog = 5;
		this.backlog = backlog;
		stack.addTCPSocket(getLocalAddress(), this);		
	}

	public ClientTCPSocket accept() {
		if (incomingAccept.isEmpty())
			return null;
		return new ClientTCPSocket(stack, getLocalAddress(), incomingAccept.remove(0));
	}
	
	public boolean queueConnect(TCPPacket syn) {
		if (incomingAccept.size()>=backlog)
			return false;
		
		incomingAccept.add(syn);
		acceptors.wakeup();
		return true; 
	}
	
	public void close() {
		acceptors.wakeup();
		
		if (getLocalAddress()!=null)
			stack.removeTCPSocket(getLocalAddress());
	}
}