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

package pt.minha.models.local.nio;

import java.io.IOException;
import java.net.SocketException;
import java.nio.channels.SelectionKey;

import pt.minha.models.fake.java.net.ServerSocket;
import pt.minha.models.fake.java.nio.channels.ServerSocketChannel;
import pt.minha.models.fake.java.nio.channels.SocketChannel;
import pt.minha.models.fake.java.nio.channels.spi.SelectorProvider;
import pt.minha.models.global.io.BlockingHelper;
import pt.minha.models.global.net.ClientTCPSocket;
import pt.minha.models.global.net.ListeningTCPSocket;
import pt.minha.models.local.lang.SimulationThread;

// pondo o channel em non-blocking, posso usar o accept do socket?
// se usar o accept, o socket que obtenho, tem channel?

class ServerSocketChannelImpl extends ServerSocketChannel {
	private ServerSocket socket;
	private ListeningTCPSocket tcp;

	protected ServerSocketChannelImpl(SelectorProvider provider) {
		super(null);
		tcp = new ListeningTCPSocket(SimulationThread.currentSimulationThread().getHost().getNetwork());
		this.socket = new ServerSocket(this, tcp);
	}

	@Override
	public ServerSocket socket() {
		return socket;
	}

	@Override
	public SocketChannel accept() throws IOException {
		if (socket.isClosed())
			throw new SocketException("socket closed");

		try {
			SimulationThread.stopTime(0);
			
			while (isBlocking() && !tcp.acceptors.isReady()) {
				tcp.acceptors.queue(SimulationThread.currentSimulationThread().getWakeup());
				SimulationThread.currentSimulationThread().pause();
			}
	
			ClientTCPSocket ctcp = tcp.accept();
			if (ctcp == null)
				return null;
			
			SocketChannel socket = new SocketChannelImpl(provider(), ctcp);
			
	    	return socket;
		} finally {
			SimulationThread.startTime(0);
		}
	}

	@Override
	protected void implCloseChannel() throws IOException {
		super.implCloseChannel();
		if (socket!=null)
			socket.close();
	}

	@Override
	public BlockingHelper helperFor(int op) {
		if (op == SelectionKey.OP_ACCEPT)
			return tcp.acceptors;
		return null;
	}
}
