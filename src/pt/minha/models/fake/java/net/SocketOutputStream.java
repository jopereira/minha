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
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import pt.minha.api.World;
import pt.minha.kernel.simulation.Event;
import pt.minha.models.global.net.Log;
import pt.minha.models.global.net.NetworkCalibration;
import pt.minha.models.global.net.SocketOutputStreamInterface;
import pt.minha.models.global.net.TCPPacket;
import pt.minha.models.global.net.TCPPacketAck;
import pt.minha.models.global.net.TCPPacketClose;
import pt.minha.models.global.net.TCPPacketData;
import pt.minha.models.local.lang.SimulationThread;

public class SocketOutputStream extends OutputStream implements SocketOutputStreamInterface {
	private final Socket socket;
	
	private List<Integer> outgoing = new LinkedList<Integer>();
	private List<Event> outgoingBlocked = new LinkedList<Event>();
	private int sn = 0;
	
	private static final int TCP_BUFFER_SIZE = 32768; // bytes
	protected static final int BUFFER_SIZE = 2*TCP_BUFFER_SIZE;
	private int transit = 0;
	
	private boolean closed = false;
	
	public SocketOutputStream(Socket socket) {
		this.socket = socket;
	}

	/*
	 * event stuff
	 */
	private class WakeWriteEvent extends Event {
		private int sn;
		
		public WakeWriteEvent(int sn) {
			super(World.timeline);
			this.sn = sn;
		}

		public void run() {
			wakeWrite(this.sn);
		}
	}
	
	private void wakeWrite(int sn) {
		outgoing.add(sn);
		if (!outgoingBlocked.isEmpty())
			outgoingBlocked.remove(0).schedule(0);
	}
	
	public void acknowledge(TCPPacketAck p) {
		this.transit -= p.getAckSize();
		
		if ( Log.network_tcp_stream_log_enabled )
			Log.TCPdebug("SocketOutputStream acknowledge: "+p.getSn());
		
		new WakeWriteEvent(sn).schedule(0);
	}
	/*
	 * /event stuff
	 */
	
	@Override
	public void write(int b) throws IOException {
		if ( this.socket.isClosed() )
			throw new IOException("Socket closed");

		try {
			SimulationThread.stopTime(NetworkCalibration.writeCost);

			byte[] copy = new byte[1];
			copy[0] = new Integer(b).byteValue();
			TCPPacket p = new TCPPacketData(this.socket.connectedSocketKey, this.sn++, copy);
			this.writeImpl(p);
		
		} finally {
			SimulationThread.startTime(0);			
		}
	}
	
	@Override
	public void write(byte b[], int off, int len) throws IOException {
		if ( this.socket.isClosed() )
			throw new IOException("Socket closed");

		try {
			SimulationThread.stopTime(NetworkCalibration.writeCost*len);
		
			if ( len > BUFFER_SIZE ) {
				throw new IOException("Package to big " + len + " bytes on " + 
						this.socket.getLocalSocketAddress() + ". Maximum size is " + 
						BUFFER_SIZE + " bytes.");
			}

			byte[] copy = new byte[len];
			System.arraycopy(b, off, copy, 0, len);
			TCPPacket p = new TCPPacketData(this.socket.connectedSocketKey, this.sn++, copy);
			this.writeImpl(p);
		
		} finally {
			SimulationThread.startTime(0);			
		}
	}

	public synchronized void writeImpl(TCPPacket p) throws IOException {
		// wait for acknowledge
		while ( this.available() < p.getSize() ) {
			outgoingBlocked.add(SimulationThread.currentSimulationThread().getWakeup());
			SimulationThread.currentSimulationThread().pause();

			outgoing.remove(0);
		}

		this.transit += p.getSize();
		
		if ( Log.network_tcp_stream_log_enabled )
			Log.TCPdebug("SocketOutputStream write: "+p.getSn()+" to "+p.getKey()+" "+p.getType());
		
		// Injected packet loss: wait an additional round-trip
		if ( NetworkCalibration.isLostPacket() ) {
			SimulationThread.currentSimulationThread().idle(NetworkCalibration.getNetworkDelay(p.getSize())*2);
		}
		
		World.network.send(p);
	}
	
	private List<Event> networkOutgoingBlocked = new LinkedList<Event>();
	
	private class WakeSendToNetworkEvent extends Event {
		public WakeSendToNetworkEvent() {
			super(World.timeline);
		}

		public void run() {
			if (!networkOutgoingBlocked.isEmpty())
				networkOutgoingBlocked.remove(0).schedule(0);
		}
	}
	
	public void wakeSendToNetwork() {
		new WakeSendToNetworkEvent().schedule(0);
	}
	
	public void close() throws IOException {
		this.socket.close();
	}
	
	protected void closeImpl() throws IOException {
		if ( this.closed )
			return;
		
		closed = true;
		TCPPacket close = new TCPPacketClose(this.socket.connectedSocketKey, this.sn++);
		try {
			SimulationThread.stopTime(0);			
			this.writeImpl(close);
		}
		catch (IOException e) {
			// ignore
		} finally {
			SimulationThread.startTime(0);			
		}
		
		if ( Log.network_tcp_stream_log_enabled )
			Log.TCPdebug("SocketOutputStream close: "+this.socket.connectedSocketKey);
	}
	
	protected int available () {
		return BUFFER_SIZE-this.transit;
	}
	
	protected int getSN() {
		return this.sn;
	}
}
