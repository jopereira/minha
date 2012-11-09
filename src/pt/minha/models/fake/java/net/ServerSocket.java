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
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;

import pt.minha.api.World;
import pt.minha.kernel.simulation.Event;
import pt.minha.models.global.net.Log;
import pt.minha.models.global.net.Protocol;
import pt.minha.models.global.net.ServerSocketUpcalls;
import pt.minha.models.global.net.SocketUpcalls;
import pt.minha.models.local.HostImpl;
import pt.minha.models.local.lang.SimulationThread;

public class ServerSocket extends AbstractSocket {
	
	private final List<WakeAcceptEvent> incomingAccept = new LinkedList<WakeAcceptEvent>();
	private final List<Event> blockedAccept = new LinkedList<Event>();
	private boolean closed = false;
	private ServerSocketUpcalls upcalls = new Upcalls();
	
	public ServerSocket() throws IOException {
		HostImpl host = SimulationThread.currentSimulationThread().getHost();
		InetSocketAddress isa = host.getHostAvailableInetSocketAddress();
		isa=this.checkSocket(isa);
		this.localSocketAddress = World.network.networkMap.addTCPSocket(isa,upcalls);

	}

	public ServerSocket(int port, int backlog, InetAddress address) throws IOException {
		// FIXME: bind address and backlog not implemented
		this(port);
    }
	
	public ServerSocket(int port) throws IOException {
		HostImpl host = SimulationThread.currentSimulationThread().getHost();
		InetSocketAddress isa = host.getHostAvailableInetSocketAddress(port);
		isa=this.checkSocket(isa);
		this.localSocketAddress = World.network.networkMap.addTCPSocket(isa,upcalls);

    }
	
    public Socket accept() throws IOException {
		if (closed)
			throw new SocketException("accept on closed socket");

		SimulationThread.stopTime(0);
		if (incomingAccept.isEmpty()) {
			blockedAccept.add(SimulationThread.currentSimulationThread().getWakeup());
			SimulationThread.currentSimulationThread().pause();
		}

		WakeAcceptEvent addresses = incomingAccept.remove(0);
		Socket socket = new Socket(addresses.remote, addresses.local);
		// inform client Socket that accept ended
	    socket.connectedSocketKey = World.network.networkMap.SocketScheduleServerSocketAcceptDone(addresses.remote, addresses.local, socket.upcalls, addresses.cli);
		socket.connected = true;
		
		if ( Log.network_tcp_log_enabled )
			Log.TCPdebug("ServerSocket accept: "+socket.connectedSocketKey);
		
		SimulationThread.startTime(0);		
    	return socket;
    }
	
    public void close() throws IOException {
    	if (closed)
    		return;
        closed = true;
        
        World.network.networkMap.removeSocket(Protocol.TCP, this.localSocketAddress);
        
        if ( Log.network_tcp_log_enabled )
        	Log.TCPdebug("ServerSocket close: "+this.localSocketAddress);
	}
    
	private class WakeAcceptEvent extends Event {
		public InetSocketAddress local;
		public InetSocketAddress remote;
		public SocketUpcalls cli;

		public WakeAcceptEvent(InetSocketAddress local, InetSocketAddress remote, SocketUpcalls cli) {
			super(World.timeline);
			this.local = local;
			this.remote = remote;
			this.cli = cli;
		}

		public void run() {
			incomingAccept.add(this);
			if (!blockedAccept.isEmpty())
				blockedAccept.remove(0).schedule(0);
		}
	}

    public String toString() {
        return "ServerSocket[addr=" + this.getLocalAddress() +
                ",localport=" + this.getLocalPort()  + "]";
    }

    private class Upcalls implements ServerSocketUpcalls {
		public void queueConnect(InetSocketAddress local, InetSocketAddress remote, SocketUpcalls cli) {
			new WakeAcceptEvent(local, remote, cli).schedule(100000);
		}
    }
    

}