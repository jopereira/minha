package pt.minha.models.local.nio;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;

import pt.minha.models.fake.java.net.DatagramSocket;
import pt.minha.models.fake.java.nio.channels.DatagramChannel;
import pt.minha.models.fake.java.nio.channels.spi.SelectorProvider;
import pt.minha.models.global.io.BlockingHelper;
import pt.minha.models.global.net.NetworkCalibration;
import pt.minha.models.global.net.UDPSocket;
import pt.minha.models.local.lang.SimulationThread;

public class DatagramChannelImpl extends DatagramChannel {
	private DatagramSocket socket;
	private UDPSocket udp;

	protected DatagramChannelImpl(SelectorProvider provider) {
		this(provider, new UDPSocket(SimulationThread.currentSimulationThread().getHost().getNetwork()));
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

		long cost = 0;
		
		try {
			SimulationThread.stopTime(0);

			while (isBlocking() && !udp.readers.isReady()) {
				udp.readers.queue(SimulationThread.currentSimulationThread().getWakeup());
				SimulationThread.currentSimulationThread().pause();
			}
			
			DatagramPacket p = udp.receive();
			
			if (p==null)
				return 0;
			
			int res = dst.remaining();
			
			if (p.getLength()>res)
				res = p.getLength();
			
			dst.put(p.getData(), p.getOffset(), res);
			
			cost = NetworkCalibration.readCost*p.getLength();
			
			return res;
		} finally {
			SimulationThread.startTime(cost);					
		}	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		if (socket.isClosed())
			throw new SocketException("socket closed");

		if (!isConnected())
			throw new NotYetConnectedException();
		
		long cost = 0;
		
		try {
			SimulationThread.stopTime(0);
			
			/* Never blocks. */
			
			byte[] data = new byte[src.remaining()];
			
			udp.send(new DatagramPacket(data, data.length, udp.getRemoteAddress()));
						
			cost = NetworkCalibration.writeCost*data.length;
			
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
