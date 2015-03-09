package pt.minha.models.local.nio;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;

import pt.minha.models.fake.java.net.DatagramSocket;
import pt.minha.models.fake.java.nio.channels.DatagramChannel;
import pt.minha.models.fake.java.nio.channels.spi.SelectorProvider;
import pt.minha.models.global.io.BlockingHelper;
import pt.minha.models.global.net.UDPSocket;
import pt.minha.models.local.lang.SimulationThread;

public class DatagramChannelImpl extends DatagramChannel {
	private DatagramSocket socket;
	private UDPSocket udp;

	protected DatagramChannelImpl(SelectorProvider provider) {
		this(provider, new UDPSocket(SimulationThread.currentSimulationThread().getProcess().getNetwork()));
	}

	protected DatagramChannelImpl(SelectorProvider provider, UDPSocket udp) {
		super(provider);
		this.udp = udp;
		this.socket = new DatagramSocket(this, udp);
	}

	public DatagramChannel connect(SocketAddress address) {
		udp.connect((InetSocketAddress)address);
		return this;
	}

	public DatagramChannel disconnect() {
		udp.connect(null);
		return this;
	}
	
	public boolean isConnected() {
		return udp.getRemoteAddress()!=null;
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		if (socket.isClosed())
			throw new SocketException("socket closed");

		if (!isConnected())
			throw new NotYetConnectedException();

		int res = dst.remaining();
		
		receive(dst);
		
		return res-dst.remaining();
	}
	
	@Override
	public SocketAddress receive(ByteBuffer dst) throws IOException {
		if (socket.isClosed())
			throw new SocketException("socket closed");
		
		long cost = 0;
		
		try {
			SimulationThread.stopTime(0);

			SimulationThread current = SimulationThread.currentSimulationThread();
			boolean interrupted = current.getInterruptedStatus(false) && isBlocking();
			
			while (isBlocking() && !udp.readers.isReady() && !interrupted) {
				udp.readers.queue(current.getWakeup());
				interrupted = current.pause(true, false);
			}

			if (interrupted) {
				close();
				throw new ClosedByInterruptException();
			}
			
			DatagramPacket p = udp.receive();
			
			if (p==null)
				return null;
			
			int res = dst.remaining();
			
			if (p.getLength()<res)
				res = p.getLength();

			dst.put(p.getData(), p.getOffset(), res);
			
			cost = udp.getNetwork().getConfig().getUDPOverhead(p.getLength());

			return p.getSocketAddress();
		} finally {
			SimulationThread.startTime(cost);					
		}
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		if (socket.isClosed())
			throw new SocketException("socket closed");

		if (!isConnected())
			throw new NotYetConnectedException();
		
		return send(src, udp.getRemoteAddress());
	}
		
	@Override
	public int send(ByteBuffer src, SocketAddress target) throws IOException {
		if (socket.isClosed())
			throw new SocketException("socket closed");
				
		long cost = 0;
		
		try {
			SimulationThread.stopTime(0);
			
			SimulationThread current = SimulationThread.currentSimulationThread();
			boolean interrupted = current.getInterruptedStatus(false) && isBlocking();

			/* Never blocks. */
			
			if (interrupted) {
				close();
				throw new ClosedByInterruptException();
			}

			byte[] data = new byte[src.remaining()];
			
			src.get(data);
			
			udp.send(new DatagramPacket(data, data.length, target));
						
			cost = udp.getNetwork().getConfig().getUDPOverhead(data.length);
			
			return data.length;
		} finally {
			SimulationThread.startTime(cost);					
		}
	}

	@Override
	public DatagramSocket socket() {
		return socket;
	}

	@Override
	public BlockingHelper helperFor(int op) {
		if (op == SelectionKey.OP_READ)
			return udp.readers;
		if (op == SelectionKey.OP_WRITE)
			return udp.writers;
		return null;
	}
}
