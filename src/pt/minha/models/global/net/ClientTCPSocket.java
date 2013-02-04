/*
 * Minha.pt: middleware testing platform.
 * Copyright (c) 2011-2013, Universidade do Minho.
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

package pt.minha.models.global.net;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.PriorityQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.minha.kernel.simulation.Event;
import pt.minha.kernel.simulation.Timeline;
import pt.minha.models.global.io.BlockingHelper;
import pt.minha.models.global.io.Buffer;

/**
 * Simulated TCP connection.
 * 
 * @author jop
 */
public class ClientTCPSocket extends AbstractSocket {
	public static final int MTU = 1500;
	public static final int PIPE_BUF = 512;
	public static final int WINDOW_SIZE = 64*1024;
	
	private PriorityQueue<TCPPacket> incoming = new PriorityQueue<TCPPacket>();

	private ClientTCPSocket peer;	
	
	private int seqOut = 0, ackedOut = -1, seqIn = 0, ackedIn = -1;
	private Buffer out = new Buffer(), in = new Buffer();
	
	private boolean synSent;
	private boolean finSent;
	
	public BlockingHelper readers, writers, connectors;
	
	/**
	 * Create a TCP connection at connecting side.
	 */
	public ClientTCPSocket(NetworkStack stack) {
		super(stack);
		
		init();
	}
		
	/**
	 * Initiate a connection.
	 * 
	 * @param addr server address
	 */
	public void connect(InetSocketAddress addr) throws SocketException {		
		if (getLocalAddress()==null)
			bind(null);

		synSent = true;
		sendPacket(new TCPPacket(this, null, seqOut, seqIn, new byte[0], 0), addr);
	}
	
	/**
	 * Create a TCP connection at accepting side.
	 * 
	 * @param syn initial packet received
	 */
	public ClientTCPSocket(NetworkStack stack, InetSocketAddress local, TCPPacket syn) {
		super(stack, local);
		
		init();
		
		synSent = true;
		receivePacket(syn);
	}
	
	private void init() {
		readers = new BlockingHelper() {
			public boolean isReady() {
				return in.getUsed()>0 || in.isClosed();
			}
		};
		writers = new BlockingHelper() {
			public boolean isReady() {
				return out.getFree()>=PIPE_BUF;
			}
		};
		connectors = new BlockingHelper() {
			public boolean isReady() {
				return ackedIn>=0;
			}
		};
	}

	/**
	 * Handle all packets received (except the initial SYN at the server side)
	 * 
	 * @param p packet
	 */
	protected void receivePacket(TCPPacket p) {

		// Is the remote peer reseting the connection?
		if ((p.getControl()&TCPPacket.RST)!=0) {
			incoming.clear();
			finSent = true;
			out.close(false);
			in.close(true);
			readers.wakeup();
			writers.wakeup();
			connectors.wakeup();
			return;
		}

		// Oportunistically handle acknowledgment, even if out of order
		if (p.getAcknowledgement() > ackedOut) {
			ackedOut = p.getAcknowledgement();
			writers.wakeup();
			
			// Established?
			if (peer==null && ackedOut >= 0) {
				peer = p.getSource();
				connectors.wakeup();
			}
		}
	
		// Handle packet processing in sequence
		incoming.add(p);
		handlePacket();		
	}
	
	/**
	 * Send all packets received (except the initial SYN at the client side)
	 * 
	 * @param p packet
	 */
	protected void sendPacket(TCPPacket p) {
		stack.getNetwork().relayTCPData(p);
	}

	/**
	 * Send the initial SYN packet at the client side
	 * 
	 * @param p packet
	 */
	protected void sendPacket(TCPPacket p, InetSocketAddress serverAddress) {
		stack.getNetwork().relayTCPConnect(serverAddress, p);		
	}
	
	public void scheduleRead(final TCPPacket p) {
		new Event(stack.getTimeline()) {
			public void run() {
				receivePacket(p);
			}
		}.schedule(stack.getConfig().getNetworkDelay(p.getSize()));
	}
	
	private void handlePacket() {

		// Wait until next in sequence is available
		if (incoming.isEmpty() || incoming.peek().getSequence() != seqIn || incoming.peek().getSize() > in.getFree())
			return;
		
		TCPPacket p = incoming.poll();

		// Should I reset the connection?
		if (p.getSize()>0 && finSent && in.isClosed()) {
			sendPacket(new TCPPacket(this, peer, seqOut, seqIn, new byte[0], TCPPacket.RST));			
			return;
		}

		// Handle data
		in.push(p.getData(), 0, p.getSize());
		seqIn += p.getSize();
		
		// Handle connection closing
		if ((p.getControl()&TCPPacket.FIN)!=0)
			in.close(true);

		if (readers.isReady())
			readers.wakeup();

		uncork();
	}
	
	public void uncork() {
		// Prepare data
		int op = out.getUsed();
				
		if (op > MTU)
			op = MTU;

		// Flow control
		if (op >= WINDOW_SIZE-(seqOut-ackedOut))
			op = WINDOW_SIZE-(seqOut-ackedOut);
		
		byte[] data = new byte[op];
		out.pop(data,  0,  op);

		// Prepare connection close
		int flags = 0;
		if (out.isClosed() && op==0 && !finSent) {
			flags = TCPPacket.FIN;
			finSent = true;
		}
		
		writers.wakeup();
		
		// Is there anything new to send?
		if (seqIn == ackedIn && op == 0 && flags == 0)
			return;

		// Send the packet and record sequence numbers
		sendPacket(new TCPPacket(this, peer, seqOut, seqIn, data, flags));

		seqOut += op;
		ackedIn = seqIn;
	}
		
	/**
	 * Get remote socket address after connected.
	 * 
	 * @return 
	 */
	public InetSocketAddress getRemoteAddress() {
		return peer==null?null:peer.getLocalAddress();
	}

	/**
	 * Send data.
	 * 
	 * @param b data array
	 * @param off offset in the array
	 * @param len number of bytes to send
	 * @return number of bytes written
	 * @throws SocketException in case of broken pipe condition
	 */
	public int write(byte b[], int off, int len) throws SocketException {
		
		if (out.isClosed())
			throw new SocketException("broken pipe");
		
		if (len <= PIPE_BUF && out.getFree() <= len)
			return 0;

		int op = out.push(b, off, len);
		
		return op;
	}
	
	/**
	 * Send data.
	 * 
	 * @param b data array
	 * @return number of bytes written
	 * @throws SocketException in case of broken pipe condition
	 */
	public int write(ByteBuffer b) throws SocketException {
		if (out.isClosed())
			throw new SocketException("broken pipe");
		
		if (b.remaining() <= PIPE_BUF && out.getFree() <= b.remaining())
			return 0;

		int op = out.push(b);
		
		return op;
	}

	/**
	 * Receive data.
	 * 
	 * @param b data array
	 * @param off offset into array
	 * @param len maximum amout to read
	 * @return number of bytes read, -1 for EOF
	 */
	public int read(byte b[], int off, int len) {

		int op = in.pop(b, off, len);
		
		handlePacket();
		
		return op;
	}
	
	/**
	 * Close outgoing stream.
	 */
	public void shutdownOutput() {
		if (out.isClosing())
			return;

		out.close(true);
		
		uncork();
	}

	/**
	 * Close incoming stream.
	 */
	public void shutdownInput() {
		if (in.isClosing())
			return;
		
		in.close(false);
	}	
	
	public boolean isConnecting() {
		return synSent;
	}
	
	public String toString() {
		return "["+getLocalAddress().getAddress().getHostAddress()+"-"+getRemoteAddress().getAddress().getHostAddress()+"] "+
				"s("+seqOut+" "+ackedOut+" "+out.isClosing()+" "+finSent+") "+
				"r("+seqIn+" "+ackedIn+" "+in.isClosing()+") ";
	}
	
	private static Logger logger = LoggerFactory.getLogger("pt.minha.TCP");

	public void readAt(long l) {
		logger.info("[ {} s ] {}:{}-{}:{} recv to {} bytes", Timeline.toSeconds(l), getLocalAddress().getAddress().getHostAddress(), getLocalAddress().getPort(), getRemoteAddress().getAddress().getHostAddress(), getRemoteAddress().getPort(), seqIn-in.getUsed());		
	}
	public void writeAt(long l) {
		logger.info("[ {} s ] {}:{}-{}:{} sent to {} bytes", Timeline.toSeconds(l), getLocalAddress().getAddress().getHostAddress(), getLocalAddress().getPort(), getRemoteAddress().getAddress().getHostAddress(), getRemoteAddress().getPort(), seqOut+out.getUsed());
	}
}