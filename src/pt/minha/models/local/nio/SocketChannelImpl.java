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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.NoConnectionPendingException;
import java.nio.channels.SelectionKey;

import pt.minha.models.fake.java.net.Socket;
import pt.minha.models.fake.java.nio.channels.SocketChannel;
import pt.minha.models.fake.java.nio.channels.spi.SelectorProvider;
import pt.minha.models.global.io.BlockingHelper;
import pt.minha.models.global.net.ClientTCPSocket;
import pt.minha.models.global.net.NetworkCalibration;
import pt.minha.models.local.lang.SimulationThread;

public class SocketChannelImpl extends SocketChannel {
	private Socket socket;
	private ClientTCPSocket tcp;

	public SocketChannelImpl(SelectorProvider provider) {
		this(provider, new ClientTCPSocket(SimulationThread.currentSimulationThread().getHost().getNetwork()));
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
			
			if (!tcp.connectors.isReady()) {
				tcp.connectors.queue(SimulationThread.currentSimulationThread().getWakeup());
				SimulationThread.currentSimulationThread().pause();
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
		long cost = 0;
		
		try {
			SimulationThread.stopTime(0);

			while (isBlocking() && !tcp.readers.isReady()) {
				tcp.readers.queue(SimulationThread.currentSimulationThread().getWakeup());
				SimulationThread.currentSimulationThread().pause();
			}
			
			int res = tcp.read(b.array(), b.position(), b.remaining());
			
			b.position(b.position()+res);

			cost = NetworkCalibration.readCost*res;
			
			return res;
		} finally {
			SimulationThread.startTime(cost);					
		}
	}

	@Override
	public int write(ByteBuffer b) throws IOException {
		long cost = 0;
		
		try {
			int total = 0, res = 0;
			
			while(b.hasRemaining()) {
				while (isBlocking() && !tcp.writers.isReady()) {
					if (res > 0) {
						total += res;
						res = 0;
						cost = NetworkCalibration.writeCost*total;
						tcp.uncork();
					}
					tcp.writers.queue(SimulationThread.currentSimulationThread().getWakeup());
					SimulationThread.currentSimulationThread().pause();
				}

				res += tcp.write(b);
				if (!isBlocking())
					break;
			}
			total += res;			
			tcp.uncork();
		
			cost += NetworkCalibration.writeCost*total;
				
			return total;
		} finally {
			SimulationThread.startTime(cost);					
		}
	}

	@Override
	public long write(ByteBuffer[] b) throws IOException {
		return write(b, 0, b.length);
	}

	@Override
	public long write(ByteBuffer[] b, int offset, int len) throws IOException {
		long cost = 0;
			
		try {
			SimulationThread.stopTime(0);

			int total = 0, res = 0;
			
			for(int i=0;i<len;) {
				while (isBlocking() && !tcp.writers.isReady()) {
					if (res > 0) {
						total += res;
						res = 0;
						cost = NetworkCalibration.writeCost*total;
						tcp.uncork();
					}
					tcp.writers.queue(SimulationThread.currentSimulationThread().getWakeup());
					SimulationThread.currentSimulationThread().pause();
				}

				res += tcp.write(b[offset+i]);
				if (!b[offset+i].hasRemaining())
					i++;
				else if (!isBlocking())
					break;
			}
			total += res;			
			tcp.uncork();
		
			cost += NetworkCalibration.writeCost*total;
				
			return total;
		} finally {
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