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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;

import pt.minha.models.global.net.ListeningTCPSocket;
import pt.minha.models.global.net.Log;
import pt.minha.models.local.lang.SimulationThread;

public class ServerSocket {
	
	private boolean closed = false;
	private ListeningTCPSocket tcp; 
	
	public ServerSocket() throws IOException {
		tcp = new ListeningTCPSocket(SimulationThread.currentSimulationThread().getHost().getNetwork());
	}
	
	public ServerSocket(int port) throws IOException {
		this(port, 5, null);
    }

	public ServerSocket(int port, int backlog, InetAddress address) throws IOException {
		this();
		
		tcp.bind(new InetSocketAddress(address, port));
		tcp.listen(backlog);
    }

    public Socket accept() throws IOException {
		if (closed)
			throw new SocketException("socket closed");

		SimulationThread.stopTime(0);
		
		while (!tcp.acceptors.isReady()) {
			tcp.acceptors.queue(SimulationThread.currentSimulationThread().getWakeup());
			SimulationThread.currentSimulationThread().pause();
		}

		Socket socket = new Socket(tcp.accept());
		
		if ( Log.network_tcp_log_enabled )
			Log.TCPdebug("ServerSocket accept: "+socket.getLocalSocketAddress()+" <- "+socket.getRemoteSocketAddress());
		
		SimulationThread.startTime(0);
		
    	return socket;
    }
	
    public void close() throws IOException {
    	if (closed)
    		return;
    	
        closed = true;
        
        tcp.close();
                
        if ( Log.network_tcp_log_enabled )
        	Log.TCPdebug("ServerSocket close: "+getLocalSocketAddress());
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
}