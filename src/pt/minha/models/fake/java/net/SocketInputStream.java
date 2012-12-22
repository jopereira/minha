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


import static pt.minha.models.fake.java.net.SocketOutputStream.BUFFER_SIZE;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import pt.minha.kernel.simulation.Event;
import pt.minha.models.global.net.Log;
import pt.minha.models.global.net.NetworkCalibration;
import pt.minha.models.global.net.TCPPacket;
import pt.minha.models.global.net.TCPPacketAck;
import pt.minha.models.global.net.TCPPacketData;
import pt.minha.models.local.lang.SimulationThread;

class SocketInputStream extends InputStream {
	private final Socket socket;

	private PriorityQueue<TCPPacket> incoming = new PriorityQueue<TCPPacket>();
	private List<Event> incomingBlocked = new LinkedList<Event>();
	
	private final byte[] buffer = new byte[BUFFER_SIZE];
	private int h, t, n = 0;
	
	private boolean closed = false;
	
	private long lastRead = 0;
	private int sn = 0;
	
	SocketInputStream(Socket socket) throws IOException {
		this.socket = socket;
	}
	
	void scheduleRead(TCPPacket p) {
		if ( closed )
			return;
		
		if ( Log.network_tcp_stream_log_enabled )
			Log.TCPdebug("SocketInputStream scheduleRead: "+p.getSn()+" "+p.getType());
		
		incoming.add(p);
		if (!incomingBlocked.isEmpty())
			incomingBlocked.remove(0).schedule(0);
	}
	
	private int readFromIncomingQueue() throws IOException {
		while(!closed && (incoming.isEmpty() || incoming.peek().getSn()!=sn)) {
			incomingBlocked.add(SimulationThread.currentSimulationThread().getWakeup());
			SimulationThread.currentSimulationThread().pause();
		}

		if (closed)
			return -1;
		
		TCPPacket p = incoming.peek();
		
		if ( Log.network_tcp_stream_log_enabled )
			Log.TCPdebug("SocketInputStream read: "+p.getSn()+" "+p.getType());
		
		if ( p.getSn() != this.sn )
			throw new IOException("Wrong SN on "+this.socket.getLocalSocketAddress().toString()+": got: "+p.getSn()+" should be: "+this.sn);
		
		// network delay
		if ( NetworkCalibration.networkDelay ){
			long now = SimulationThread.currentSimulationThread().getTimeline().getTime();
			long delay = NetworkCalibration.getNetworkDelay(p.getSize());
			if ( (now-this.lastRead)>delay )
				SimulationThread.currentSimulationThread().idle(delay);
		}
		
		this.lastRead = SimulationThread.currentSimulationThread().getTimeline().getTime();
		switch ( p.getType() ) {
			case Data:
				// send acknowledge
				TCPPacketAck ack = new TCPPacketAck(this.socket.upcalls, p.getSn(), p.getSize());
				if ( Log.network_tcp_stream_log_enabled )
					Log.TCPdebug("SocketInputStream acknowledge: "+ack.getSn()+" "+ack.getType());

				socket.stack.getNetwork().relayTCPAck(ack);

				byte[] data = ((TCPPacketData)p).getData();
				for (int i=0; i<data.length; i++) {
					if ( this.buffer.length == this.n ) {
						throw new IOException("Buffer overflow! blame nac");
					}
					this.n++;
					this.buffer[this.h] = data[i];
					this.h = (this.h+1)%(BUFFER_SIZE);
				}
				this.sn++;
				return data.length;
			case Close:
				this.sn++;
				return -1;
		}
		
		return 0;
	}

	@Override
	public int read() throws IOException {
		if ( this.socket.isClosed() )
			return -1;
		
		// if true close socket after restart time
		boolean close = false;
		
		try {
			SimulationThread.stopTime(0);

			// if no available data, read from socket
			while ( 0 == this.n ) {
				int r = this.readFromIncomingQueue();
				if ( r<0 ) {
					close = true;
					return r;
				}
			}

			this.n--;
			int b = this.buffer[this.t];
			this.t = (this.t+1)%(BUFFER_SIZE);
			return b;
		} finally {
			SimulationThread.startTime(NetworkCalibration.readCost);
			if (close)
				this.close();
		}
	}
	
	@Override
	public int read(byte b[], int off, int len) throws IOException {
		if ( this.socket.isClosed() )
			return -1;
		
		// if true close socket after restart time
		boolean close = false;
		
		int length = 0;
		try {
			SimulationThread.stopTime(0);

			// if no available data, read from socket
			while ( 0 == this.n ) {
				int r = this.readFromIncomingQueue();
				if ( r<0 ) {
					close = true;
					return r;
				}
			}

			length = Math.min(this.n, len);
			for ( int i=0; i<length; i++ ) {
				this.n--;
				b[off+i] = this.buffer[this.t];
				this.t = (this.t+1)%(BUFFER_SIZE);
			}
			return length;
		} finally {
			SimulationThread.startTime(NetworkCalibration.readCost*length);
			if (close)
				this.close();
		}
	}
	
	public int available() throws IOException {
		return this.n;
    }
	
	public void close() throws IOException {
		this.socket.close();
	}
	
	protected void closeImpl() throws IOException {
		if ( this.closed )
			return;
		
		closed = true;
		while (!incomingBlocked.isEmpty()) {
			incomingBlocked.remove(0).schedule(0);
		}
		
		if ( Log.network_tcp_stream_log_enabled )
			Log.TCPdebug("SocketInputStream close ");
	}

	protected int getSN() {
		return this.sn;
	}
}
