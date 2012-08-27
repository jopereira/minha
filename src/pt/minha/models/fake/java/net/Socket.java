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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;

import pt.minha.api.World;
import pt.minha.kernel.simulation.Event;
import pt.minha.models.global.net.Log;
import pt.minha.models.global.net.NetworkMap;
import pt.minha.models.global.net.Protocol;
import pt.minha.models.global.net.SocketInterface;
import pt.minha.models.global.net.TCPPacket;
import pt.minha.models.global.net.TCPPacketAck;
import pt.minha.models.local.HostImpl;
import pt.minha.models.local.lang.SimulationThread;

public class Socket extends AbstractSocket implements SocketInterface {
	private int doneConnect = 0;
	private List<Event> blockedConnect = new LinkedList<Event>();
	private SocketInputStream in;
	private SocketOutputStream out;	
	private InetSocketAddress remoteSocketAddress;
	
	protected boolean connected = false;
	protected String connectedSocketKey;
	private boolean closed = false;
	private boolean shutOut = false;
	private boolean shutIn = false;

	public Socket() throws IOException {
		HostImpl host = SimulationThread.currentSimulationThread().getHost();
		InetSocketAddress isa = host.getHostAvailableInetSocketAddress();

		this.addSocket(Protocol.TCP, isa, this);		
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
	}

    public void connect(SocketAddress endpoint) throws IOException {
		if (closed)
			throw new SocketException("connect on closed socket");
        
        InetSocketAddress epoint = (InetSocketAddress) endpoint;
        if ( !this.existsSocket(Protocol.TCP, epoint) )
        	throw new IOException("connect: unable to connect to " + epoint.toString());
        
        if (!(NetworkMap.isServerSocket(epoint)))
        	throw new IOException("connect: unable to connect to " + epoint.toString() + ", is not a ServerSocket");
        
        this.remoteSocketAddress = epoint;
        
		// wait to ServerSocket.accept() end
		SimulationThread.stopTime(0);
		
		// connect to ServerSocket
		this.connectedSocketKey = NetworkMap.ServerSocketConnect(this.remoteSocketAddress, (InetSocketAddress)this.getLocalSocketAddress(), this);
	    this.connected = true;
	    
		if (doneConnect==0) {
			blockedConnect.add(SimulationThread.currentSimulationThread().getWakeup());
			SimulationThread.currentSimulationThread().pause();
		}

		doneConnect--;
		
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
        
        if ( Log.network_tcp_log_enabled ) {
        	try {
        		int snIN = this.in.getSN();
        		int snOUT = this.getRemoteSocketOutputStreamSN();
        		/* snIN+1: if close() method was invoked by me I will not read any Close packet */ 
        		if ( (snIN!=snOUT) && (snIN+1!=snOUT) )
        			Log.TCPdebug("SocketInputStream UnreadPackets: "+this.connectedSocketKey+"; read: "+snIN+"; wrote: "+snOUT);
        	}
        	catch (IOException e) {
        		// ignore
        	}
        }
        
        if (this.connected) {
	        // We need to remove local key and not remote endpoint key
	        int keySeparator = this.connectedSocketKey.lastIndexOf('-');
	        String localConnectedSocketKey = this.connectedSocketKey.substring(0, keySeparator);
	        String connectedSocketKeySuffix = this.connectedSocketKey.substring(keySeparator);
	        if ( connectedSocketKeySuffix.equals("-client") )
	        	localConnectedSocketKey += "-server";
	        else
	        	localConnectedSocketKey += "-client";
	        
	        NetworkMap.removeConnectedSocket(localConnectedSocketKey);
	        if ( !NetworkMap.isServerSocket(this.localSocketAddress) )
	        	NetworkMap.removeSocket(Protocol.TCP, this.localSocketAddress);
	        
	        if ( Log.network_tcp_log_enabled )
	        	Log.TCPdebug("Socket close: "+this.connectedSocketKey);
	        
	        this.connectedSocketKey = null;    	
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
    
	private void wakeConnect() {
		doneConnect++;
		if (!blockedConnect.isEmpty())
			blockedConnect.remove(0).schedule(0);
	}
	
	public void scheduleServerSocketAcceptDone() {
		new WakeConnectEvent().schedule(0);
	}
        
	private class WakeConnectEvent extends Event {
		public WakeConnectEvent() {
			super(World.timeline);
		}

		public void run() {
			wakeConnect();
		}
	}
	
	public void scheduleRead(TCPPacket p) {
		this.in.scheduleRead(p);
	}
	
	public void acknowledge(TCPPacketAck p) {
		this.out.acknowledge(p);
	}

	public int getSocketInputStreamSN() {
		return this.in.getSN();
	}
	
	public int getSocketOutputStreamSN() {
		return this.out.getSN();
	}
	
	public int getRemoteSocketInputStreamSN() throws IOException {
		return NetworkMap.getSocketInputStreamSN(this.connectedSocketKey);
	}
	
	public int getRemoteSocketOutputStreamSN() throws IOException {
		return NetworkMap.getSocketOutputStreamSN(this.connectedSocketKey);
	}
}
