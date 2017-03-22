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
import java.nio.channels.IllegalBlockingModeException;

import pt.minha.models.fake.java.nio.channels.ServerSocketChannel;
import pt.minha.models.global.net.ListeningTCPSocket;
import pt.minha.models.local.lang.SimulationThread;

public class ServerSocket {
	
	private boolean closed = false;
	private ListeningTCPSocket tcp;
	private ServerSocketChannel channel; 
	
	public ServerSocket() throws IOException {
		tcp = new ListeningTCPSocket(SimulationThread.currentSimulationThread().getProcess().getNetwork());
	}
	
	public ServerSocket(int port) throws IOException {
		this(port, ListeningTCPSocket.DEFAULT_BACKLOG, null);
    }

	public ServerSocket(int port, int backlog, InetAddress address) throws IOException {
		this();

		this.bind(new InetSocketAddress(address, port), backlog);
    }
	
	public ServerSocket(ServerSocketChannel channel, ListeningTCPSocket tcp) {
		this.tcp = tcp;
		this.channel = channel;
	}
	
	public ServerSocketChannel getChannel() {
		return channel;
	}

	private void checkBlocking() throws IOException {
		if (channel != null && !channel.isBlocking())
			throw new IllegalBlockingModeException();
	}

	public void bind(SocketAddress address) throws IOException {
		bind(address, ListeningTCPSocket.DEFAULT_BACKLOG);
	}

	public void bind(SocketAddress address, int backlog) throws IOException {
		tcp.bind((InetSocketAddress)address);
		tcp.listen(backlog);
	}

    public Socket accept() throws IOException {
		if (closed)
			throw new SocketException("socket closed");

		SimulationThread.stopTime(0);

		checkBlocking();
		
		while (!tcp.acceptors.isReady()) {
			tcp.acceptors.queue(SimulationThread.currentSimulationThread().getWakeup());
			SimulationThread.currentSimulationThread().pause(false, false);
		}

		Socket socket = new Socket(null, tcp.accept());
		
		SimulationThread.startTime(0);
		
    	return socket;
    }
    
    public void close() throws IOException {
    	if (closed)
    		return;
    	
        closed = true;
        
        tcp.close();
        
        if (channel!=null)
        	channel.close();
	}
    
    public boolean isClosed() {
    	return closed;
    }
	
    public String toString() {
        return "ServerSocket[addr=" + this.getLocalAddress() +
                ",localport=" + this.getLocalPort()  + "]";
    }

	public SocketAddress getLocalSocketAddress() {
		return tcp.getLocalAddress();
	}
	
	public InetAddress getLocalAddress() {
		return tcp.getLocalAddress().getAddress();
	}

	public int getLocalPort() {
		return tcp.getLocalAddress().getPort();
	}
	
	public InetAddress getInetAddress() {
		return tcp.getLocalAddress().getAddress();
	}
	
	public void setPerformancePreferences(int c, int l, int b) {
		// TODO
	}
	
	public void setSoTimeout(int timeout) {
		// TODO
	}
	
	public void setReuseAddress(boolean on) {
		// TODO
	}
	
	public boolean isBound() {
		return tcp.getLocalAddress() != null;
	}
}