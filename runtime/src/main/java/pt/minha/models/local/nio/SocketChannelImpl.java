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

package pt.minha.models.local.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.NoConnectionPendingException;
import java.nio.channels.SelectionKey;

import pt.minha.models.fake.java.net.Socket;
import pt.minha.models.fake.java.nio.channels.SocketChannel;
import pt.minha.models.fake.java.nio.channels.spi.SelectorProvider;
import pt.minha.models.global.io.BlockingHelper;
import pt.minha.models.global.net.ClientTCPSocket;
import pt.minha.models.local.lang.SimulationThread;

public class SocketChannelImpl extends SocketChannel {
	private Socket socket;
	private ClientTCPSocket tcp;

	public SocketChannelImpl(SelectorProvider provider) {
		this(provider, new ClientTCPSocket(SimulationThread.currentSimulationThread().getProcess().getNetwork()));
	}

	public SocketChannelImpl(SelectorProvider provider, ClientTCPSocket tcp) {
		super(provider);
		this.tcp = tcp;
		this.socket = new Socket(this, tcp);
	}

	@Override
	public Socket socket() {
		return socket;
	}

	@Override
	public boolean isConnected() {
		return tcp.connectors.isReady();
	}

	@Override
	public boolean isConnectionPending() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean connect(SocketAddress endpoint) throws IOException {
		if (socket.isClosed())
			throw new SocketException("socket closed");

		try {
			SimulationThread.stopTime(0);

			tcp.connect((InetSocketAddress) endpoint);
			
			if (!isBlocking())
				return false;

			SimulationThread current = SimulationThread.currentSimulationThread();
			boolean interrupted = current.getInterruptedStatus(false);

			if (!tcp.connectors.isReady() && !interrupted) {
				tcp.connectors.queue(current.getWakeup());
				interrupted = current.pause(true, false);
			}
			
			if (interrupted) {
				close();
				throw new ClosedByInterruptException();
			}
			
			if (!tcp.connectors.isReady())
				throw new SocketException("connection refused");
			
		} finally {
			SimulationThread.startTime(0);
		}

		return false;
	}

	@Override
	public boolean finishConnect() throws IOException {
		if (tcp.isConnecting() == false) throw new NoConnectionPendingException();
		
		// throw exception?
		
		return tcp.connectors.isReady();
	}

	@Override
	public int read(ByteBuffer b) throws IOException {
		if (socket.isClosed())
			throw new SocketException("socket closed");

		long cost = 0;

		SimulationThread.stopTime(0);
		try {
			SimulationThread current = SimulationThread.currentSimulationThread();
			boolean interrupted = current.getInterruptedStatus(false) && isBlocking();

			while (isBlocking() && !tcp.readers.isReady() && !interrupted) {
				tcp.readers.queue(current.getWakeup());
				interrupted = current.pause(true, false);
			}
			
			if (interrupted) {
				close();
				throw new ClosedByInterruptException();
			}
			
			int res = tcp.read(b.array(), b.position(), b.remaining());
			
			b.position(b.position()+res);

			cost = tcp.getNetwork().getConfig().getTCPOverhead(res);
			
			return res;
		} finally {
			if (cost<0) cost = 0;
			tcp.readAt(SimulationThread.currentSimulationThread().getTimeline().getTime()+cost);
			SimulationThread.startTime(cost);					
		}
	}

	@Override
	public int write(ByteBuffer b) throws IOException {
		if (socket.isClosed())
			throw new SocketException("socket closed");

		long cost = 0;
		int total = 0;
		
		long time = SimulationThread.stopTime(0);
		try {
			int res = 0;

			SimulationThread current = SimulationThread.currentSimulationThread();
			boolean interrupted = current.getInterruptedStatus(false) && isBlocking();

			while(b.hasRemaining() && !interrupted) {
				while (isBlocking() && !tcp.writers.isReady() && !interrupted) {
					if (res > 0) {
						total += res;
						res = 0;
						cost = tcp.getNetwork().getConfig().getTCPOverhead(total);
						tcp.uncork();
					}
					tcp.writers.queue(current.getWakeup());
					interrupted = current.pause(true, false);
				}
				
				res += tcp.write(b);
				if (!isBlocking())
					break;
			}

			total += res;			
			tcp.uncork();
		
			cost = tcp.getNetwork().getConfig().getTCPOverhead(total);

			if (interrupted) {
				close();
				throw new ClosedByInterruptException();
			}

			return total;
		} finally {
			if (total>0) tcp.writeAt(time);
			SimulationThread.startTime(cost);					
		}
	}

	@Override
	public long write(ByteBuffer[] b) throws IOException {
		return write(b, 0, b.length);
	}

	@Override
	public long write(ByteBuffer[] b, int offset, int len) throws IOException {
		if (socket.isClosed())
			throw new SocketException("socket closed");

		long cost = 0;
		int total = 0;
			
		long time = SimulationThread.stopTime(0);
		try {
			SimulationThread current = SimulationThread.currentSimulationThread();
			boolean interrupted = current.getInterruptedStatus(false) && isBlocking();

			int res = 0;
			
			for(int i=0;i<len && !interrupted;) {
				while (isBlocking() && !tcp.writers.isReady() && !interrupted) {
					if (res > 0) {
						total += res;
						res = 0;
						cost = tcp.getNetwork().getConfig().getTCPOverhead(total);
						tcp.uncork();
					}
					tcp.writers.queue(current.getWakeup());
					interrupted = current.pause(true, false);
				}

				res += tcp.write(b[offset+i]);
				if (!b[offset+i].hasRemaining())
					i++;
				else if (!isBlocking())
					break;
			}
			total += res;			
			tcp.uncork();
		
			cost = tcp.getNetwork().getConfig().getTCPOverhead(total);
				
			if (interrupted) {
				close();
				throw new ClosedByInterruptException();
			}

			return total;
		} finally {
			if (total>0) tcp.writeAt(time);
			SimulationThread.startTime(cost);					
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
		if (op == SelectionKey.OP_READ)
			return tcp.readers;
		if (op == SelectionKey.OP_WRITE)
			return tcp.writers;
		if (op == SelectionKey.OP_CONNECT)
			return tcp.connectors;
		return null;
	}	
}