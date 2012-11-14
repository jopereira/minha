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

package pt.minha.models.fake.java.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;

import pt.minha.kernel.simulation.Event;
import pt.minha.kernel.simulation.Timeline;
import pt.minha.models.global.net.Log;
import pt.minha.models.global.net.NetworkStack;
import pt.minha.models.global.net.SocketUpcalls;
import pt.minha.models.global.net.TCPPacket;
import pt.minha.models.global.net.TCPPacketAck;
import pt.minha.models.local.lang.SimulationThread;

public class Socket {
	private int doneConnect = 0;
	private List<Event> blockedConnect = new LinkedList<Event>();
	private SocketInputStream in;
	private SocketOutputStream out;	
	private InetSocketAddress remoteSocketAddress;
	final SocketUpcalls upcalls = new Upcalls();
	private InetSocketAddress localSocketAddress;
	
	protected boolean connected = false;
	//protected String connectedSocketKey;
	private boolean closed = false;
	private boolean shutOut = false;
	private boolean shutIn = false;
	SocketUpcalls target;
	NetworkStack stack;
	
	public Socket() throws IOException {
		stack = SimulationThread.currentSimulationThread().getHost().getNetwork();
		
		this.localSocketAddress = stack.getBindAddress(0);

		this.in = new SocketInputStream(this);
		this.out = new SocketOutputStream(this);

		if ( Log.network_tcp_log_enabled )
			Log.TCPdebug("Socket bind: "+this.localSocketAddress);
	}
	
    public Socket(InetAddress address, int port) throws IOException {
    	this();
    	this.connect(new InetSocketAddress(address, port));
    }
 
    public Socket(String address, int port) throws IOException {
    	this(InetAddress.getByName(address), port);
    }
    
	protected Socket(InetSocketAddress local, InetSocketAddress remote) throws IOException {
		/*
		 * Create a "copy" to remote peer.
		 */
		this.localSocketAddress = remote;
		this.remoteSocketAddress = local;
		
		this.in = new SocketInputStream(this);
		this.out = new SocketOutputStream(this);
		stack = SimulationThread.currentSimulationThread().getHost().getNetwork();
	}

    public void connect(SocketAddress endpoint) throws IOException {
		if (closed)
			throw new SocketException("connect on closed socket");
        
        InetSocketAddress epoint = (InetSocketAddress) endpoint;
        this.remoteSocketAddress = epoint;
        
		// wait to ServerSocket.accept() end
		SimulationThread.stopTime(0);
		
		// connect to ServerSocket		
		stack.getNetwork().relayTCPConnect(remoteSocketAddress, (InetSocketAddress)this.getLocalSocketAddress(), upcalls);
		
		if (doneConnect==0) {
			blockedConnect.add(SimulationThread.currentSimulationThread().getWakeup());
			SimulationThread.currentSimulationThread().pause();
		}
		doneConnect--;
		
		if (target==null) {
			SimulationThread.startTime(0);
			throw new ConnectException();
		}
		
	    this.connected = true;
		
		if ( Log.network_tcp_log_enabled )
			Log.TCPdebug("Socket connected: "+this.remoteSocketAddress);
		
		SimulationThread.startTime(0);
    }
    
    public InputStream getInputStream() throws IOException {
        return this.in;
    }
    
    public OutputStream getOutputStream() throws IOException {
        return this.out;
    }

    public InetAddress getInetAddress() {
    	return remoteSocketAddress.getAddress();
    }
    
    public int getPort() {
    	return remoteSocketAddress.getPort();
    }
    
    public SocketAddress getRemoteSocketAddress() {
    	return remoteSocketAddress;
    }
        
    public void shutdownOutput() throws IOException {    	
    	shutOut = true;
    	this.out.close();
    }
    
    public void shutdownInput() throws IOException {
    	shutIn = true;
    	this.in.close();
    }

    public void close() throws IOException {
        if (closed)
            return;    
        this.closed = true;
        
        // close pipes
        if ( null!=this.in && !this.shutIn ) {
        	this.shutIn = true;
        	this.in.closeImpl();
        }
        if ( null!=this.out && !this.shutOut ) {
        	this.shutOut = true;
       		this.out.closeImpl();
        }
                
        if (this.connected) {
	        // We need to remove local key and not remote endpoint key
	        /*int keySeparator = this.connectedSocketKey.lastIndexOf('-');
	        String localConnectedSocketKey = this.connectedSocketKey.substring(0, keySeparator);
	        String connectedSocketKeySuffix = this.connectedSocketKey.substring(keySeparator);
	        if ( connectedSocketKeySuffix.equals("-client") )
	        	localConnectedSocketKey += "-server";
	        else
	        	localConnectedSocketKey += "-client";
	        
	        host.getNetwork().networkMap.removeConnectedSocket(localConnectedSocketKey);
	        
	        if ( Log.network_tcp_log_enabled )
	        	Log.TCPdebug("Socket close: "+this.connectedSocketKey);
	        
	        this.connectedSocketKey = null;*/    	
	        this.connected = false;
        }
    }

	public boolean isClosed() {
		return closed;
	}
    
    public String toString() {
   		return "Socket[addr=" + this.remoteSocketAddress +
    			",localaddr=" + this.getLocalSocketAddress()+"]";
    }
    
	private void wakeConnect(SocketUpcalls scs) {
		doneConnect++;
		this.target = scs;
		if (!blockedConnect.isEmpty())
			blockedConnect.remove(0).schedule(0);
	}
	        
	private class WakeConnectEvent extends Event {
		public SocketUpcalls scs;
		
		public WakeConnectEvent(Timeline timeline, SocketUpcalls scs) {
			super(timeline);
			this.scs = scs;
		}

		public void run() {
			wakeConnect(scs);
		}
	}

	private class Upcalls implements SocketUpcalls {
		public void accepted(SocketUpcalls serverClientSocket) {
			new WakeConnectEvent(stack.getTimeline(), serverClientSocket).schedule(0);
		}
	
		public void scheduleRead(TCPPacket p) {
			in.scheduleRead(p);
		}
		
		public void acknowledge(TCPPacketAck p) {
			out.acknowledge(p);
		}
	}
	
	public SocketAddress getLocalSocketAddress() {
		return this.localSocketAddress;
	}
	
	public InetAddress getLocalAddress() {
		return this.localSocketAddress.getAddress();
	}

	public int getLocalPort() {
		return this.localSocketAddress.getPort();
	}

}
