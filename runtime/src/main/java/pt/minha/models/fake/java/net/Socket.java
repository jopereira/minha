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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.IllegalBlockingModeException;

import pt.minha.models.fake.java.nio.channels.SocketChannel;
import pt.minha.models.global.net.ClientTCPSocket;
import pt.minha.models.local.lang.SimulationThread;

public class Socket {
	
	private ClientTCPSocket tcp;
	private boolean closed;
	private InputStream in;
	private OutputStream out;
	private SocketChannel channel;

	public Socket() throws IOException {
		tcp = new ClientTCPSocket(SimulationThread.currentSimulationThread().getProcess().getNetwork());
	}
	
    public Socket(InetAddress address, int port) throws IOException {
    	this();
    	this.connect(new InetSocketAddress(address, port));
    }
 
    public Socket(String address, int port) throws IOException {
    	this(InetAddress.getByName(address), port);
    }
    
	public Socket(SocketChannel channel, ClientTCPSocket tcp) {
		this.tcp = tcp;
		this.channel = channel;
		if (channel == null)
			createStreams();
	}
	
	public SocketChannel getChannel() {
		return channel;
	}
	
	private void checkBlocking() throws IOException {
		if (channel != null && !channel.isBlocking())
			throw new IllegalBlockingModeException();
	}

	private void createStreams() {
		in = new InputStream() {
			public int read(byte[] b, int off, int len) throws IOException {
				long cost = 0;
				
				SimulationThread.stopTime(0);
				try {
					
					checkBlocking();
					
					while (!tcp.readers.isReady()) {
						tcp.readers.queue(SimulationThread.currentSimulationThread().getWakeup());
						SimulationThread.currentSimulationThread().pause(false, false);
					}
					
					int res = tcp.read(b, off, len); 

					cost = tcp.getNetwork().getConfig().getTCPOverhead(len);
					
					return res;
				} finally {
					if (cost<0) cost = 0;
					tcp.readAt(SimulationThread.currentSimulationThread().getTimeline().getTime()+cost);
					
					SimulationThread.startTime(cost);					
				}
			}

			public int read() throws IOException {
				byte[] buf = new byte[1];
				int res = read(buf, 0, 1);
				if (res<0)
					return res;
				else
					return ((int)buf[0])&0xff;
			}

			public void close() throws IOException {
				super.close();
				Socket.this.close();
			}
		};
		
		out = new OutputStream() {
			public void write(byte[] b, int off, int len) throws IOException {
				long cost = 0;
				
				long time = SimulationThread.stopTime(0);
				try {
					
					checkBlocking();

					int total = 0, res = 0;
					
					while(total+res < len) {
						while (!tcp.writers.isReady()) {
							if (res > 0) {
								total += res;
								res = 0;
								cost = tcp.getNetwork().getConfig().getTCPOverhead(total);
								tcp.uncork();
							}
							tcp.writers.queue(SimulationThread.currentSimulationThread().getWakeup());
							SimulationThread.currentSimulationThread().pause(false, false);
						}
						
						res += tcp.write(b, off+total+res, len-(total+res));
					}
					total += res;			
					tcp.uncork();
					
					cost = tcp.getNetwork().getConfig().getTCPOverhead(total);
				} finally {
					tcp.writeAt(time);
					
					SimulationThread.startTime(cost);					
				}
			}

			public void close() throws IOException {
				super.close();
				Socket.this.close();
			}

			public void write(int b) throws IOException {
				write(new byte[]{(byte)b});
			}
		};
	}

	public void bind(SocketAddress address) throws IOException {
		tcp.bind((InetSocketAddress)address);
	}


	public void connect(SocketAddress endpoint, int timeout) throws IOException {
		if (endpoint == null)
			throw new IllegalArgumentException("connect: The address can't be null");

		if (timeout < 0)
			throw new IllegalArgumentException("connect: timeout can't be negative");

		if (closed)
			throw new SocketException("Socket is closed");

		if (!(endpoint instanceof InetSocketAddress))
			throw new IllegalArgumentException("Unsupported address type");

		//TODO: implement timeout logic
		connect(endpoint);

	}

    public void connect(SocketAddress endpoint) throws IOException {
		if (closed)
			throw new SocketException("socket closed");

		try {
			SimulationThread.stopTime(0);

			checkBlocking();
			
			tcp.connect((InetSocketAddress) endpoint);
			
			if (!tcp.connectors.isReady()) {
				tcp.connectors.queue(SimulationThread.currentSimulationThread().getWakeup());
				SimulationThread.currentSimulationThread().pause(false, false);
			}
			
			if (!tcp.connectors.isReady())
				throw new SocketException("connection refused");
        				
			createStreams();
			
		} finally {
			SimulationThread.startTime(0);
		}
    }
    
    public OutputStream getOutputStream() throws IOException {
    	if (closed)
    		throw new SocketException("socket closed");
    	
        return out;
    }

    public InputStream getInputStream() throws IOException {
    	if (closed)
    		throw new SocketException("socket closed");

    	return in;
    }
        
    public void shutdownOutput() throws IOException {    	
    	tcp.shutdownOutput();
    }
    
    public void shutdownInput() throws IOException {
    	tcp.shutdownInput();
    }

    public void close() throws IOException {
        if (closed)
            return;   
        
        this.closed = true;
        
        shutdownInput();
        shutdownOutput();
        
        if (channel!=null)
        	channel.close();
    }
    
	public boolean isClosed() {
		return closed;
	}
	
	public boolean isConnected() {
		return tcp.connectors.isReady();
	}
    
    public SocketAddress getRemoteSocketAddress() {
    	return tcp.getRemoteAddress();
    }
    
    public InetAddress getInetAddress() {
    	return tcp.getRemoteAddress().getAddress();
    }
    
    public int getPort() {
    	return tcp.getRemoteAddress().getPort();
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
	
	public int getReceiveBufferSize() throws SocketException {
		return 65535;
	}

	public void setReceiveBufferSize(int size) throws SocketException {
		// not implemented
	}

	public int getSendBufferSize() throws SocketException {
		return 65535;
	}

	public void setSendBufferSize(int size) throws SocketException {
		// not implemented
	}
	
	public void setSoTimeout(int timeout) {
		// TODO
	}

	public void setTcpNoDelay(boolean noDelay) {
		// TODO
	}

	public void setKeepAlive(boolean keepAlive) {
		// TODO
	}

	public void setSoLinger(boolean linger, int timeout) {
		// TODO
	}

	public void setReuseAddress(boolean reuseAddress) {
		// TODO
	}

    public String toString() {
   		return "Socket[addr=" + this.getRemoteSocketAddress() +
    			",localaddr=" + this.getLocalSocketAddress()+"]";
    }
}