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

package pt.minha.test.model.net;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Mocked;

import org.testng.annotations.Test;

import pt.minha.api.Entry;
import pt.minha.api.Exit;
import pt.minha.api.Host;
import pt.minha.api.Process;
import pt.minha.api.World;
import pt.minha.api.sim.Global;
import pt.minha.api.sim.Simulation;

public class DatagramTest {
	
	@Global
	public static interface Callback {
		public void receive(DatagramPacket packet, InetSocketAddress address);
		public void exception(Exception e);
	}
	
	@Global
	public static interface Target {
		public void open(InetSocketAddress addr, Callback cb) throws IOException;
		public void send(DatagramPacket packet) throws IOException;
		public void close();
	}
	
	public static class Impl implements Target {
		private DatagramSocket socket;
		private Callback callback;

		public void open(InetSocketAddress addr, Callback cb) throws IOException {
			this.callback = cb;
			this.socket = new DatagramSocket(addr.getPort());
			
			new Thread() {
				public void run() {
					try {
						while(true) {
							DatagramPacket p = new DatagramPacket(new byte[1024], 1024);
							socket.receive(p);
							callback.receive(p, (InetSocketAddress)socket.getLocalSocketAddress());
						}
					} catch(Exception e) {
						callback.exception(e);
						return;
					}
				}
			}.start();
		}

		public void send(DatagramPacket packet) throws IOException {
			socket.send(packet);
		}
		
		public void close() {
			socket.close();
		}
	}
	
	@Test
	public void loopback(@Mocked final Callback cb) throws Exception {
		Simulation world = new Simulation();
		
		final Host host = world.createHost();
		Process proc = host.createProcess();

		final byte[] data = "Message!".getBytes(); 
		
		Entry<Target> en = proc.createEntry(Target.class, Impl.class.getName());
		Exit<Callback> ex = proc.createExit(Callback.class, cb);

		final InetSocketAddress addr = new InetSocketAddress(host.getAddress(), 12345); 
		final DatagramPacket packet = new DatagramPacket(data, data.length, addr);
		
		en.queue().open(addr, ex.report());
		en.at(1, SECONDS).queue().send(packet);
		world.run();
		
		world.close();

		new FullVerifications() {
			{
				cb.receive(withAny(packet), withAny(addr));
				forEachInvocation = new Object() {
					void validate(DatagramPacket p, InetSocketAddress a) {
						// Receiver address
						assertNotNull(a);
						assertEquals(a.getAddress(), host.getAddress());
						assertEquals(a.getPort(), addr.getPort());
						// Sender address
						assertNotNull(p);
						assertEquals(p.getAddress(), host.getAddress());
						assertEquals(p.getPort(), addr.getPort());
						// Data
						assertNotNull(p.getData());
						assertTrue(checkData(p, data));			
					}
				};
			}
		};		
	} 

	@Test
	public void p2p(@Mocked final Callback cb) throws Exception {
		Simulation world = new Simulation();
		
		final Entry<Target>[] en = world.createEntries(2, Target.class, Impl.class.getName());
		final Exit<Callback>[] ex = world.createExits(en, Callback.class, cb);
		
		final byte[] data = "Message!".getBytes(); 
		
		final InetSocketAddress addr = new InetSocketAddress(en[1].getProcess().getHost().getAddress(), 12345); 
		final DatagramPacket packet = new DatagramPacket(data, data.length, addr);
		
		for(int i = 0; i<en.length; i++)
			en[i].queue().open(addr, ex[i].report());
		en[0].at(1, SECONDS).queue().send(packet);
		world.run();

		world.close();

		new FullVerifications() {
			{
				cb.receive(withAny(packet), withAny(addr));
				forEachInvocation = new Object() {
					void validate(DatagramPacket p, InetSocketAddress a) {
						// Receiver address
						assertNotNull(a);
						assertEquals(a.getAddress(), en[1].getProcess().getHost().getAddress());
						assertEquals(a.getPort(), addr.getPort());
						// Sender address
						assertNotNull(p);
						assertEquals(p.getAddress(), en[0].getProcess().getHost().getAddress());
						assertEquals(p.getPort(), addr.getPort());
						// Data
						assertNotNull(p.getData());
						assertTrue(checkData(p, data));			
					}
				};
			}
		};		
	}

	public static boolean checkData(DatagramPacket packet, byte[] data) {
		if (packet.getLength() != data.length)
			return false;
		for(int i = 0; i<data.length; i++)
			if (data[i]!=packet.getData()[i])
				return false;
		return true;
	}
	
	@Test
	public void wakeOnClose(@Mocked final Callback cb) throws Exception {
		
		new Expectations() {
			{
				//cb.exception(new SocketException("receive on closed socket"));
			}
		};
		
		Simulation world = new Simulation();
		
		final Entry<Target>[] en = world.createEntries(1, Target.class, Impl.class.getName());
		final Exit<Callback>[] ex = world.createExits(en, Callback.class, cb);
		
		final InetSocketAddress addr = new InetSocketAddress(en[0].getProcess().getHost().getAddress(), 12345); 

		en[0].queue().open(addr, ex[0].report());
		en[0].at(5, SECONDS).queue().close();
		world.run(10, SECONDS);

		world.close();
	}

	@Global
	public interface Timeout {
		public void run() throws IOException;
	}
	
	public static class TimeoutImpl implements Timeout {
		@Override
		public void run() throws IOException {
			DatagramSocket ds = new DatagramSocket();
			ds.setSoTimeout(1000);
			DatagramPacket p = new DatagramPacket(new byte[100], 100);
			ds.receive(p);
		}		
	}
	
	@Test(expectedExceptions={SocketTimeoutException.class})
	public void timeout() throws Exception {
		World world = new Simulation();
		
		Entry<Timeout> en = world.createHost().createProcess().createEntry(Timeout.class, TimeoutImpl.class.getName());
		en.call().run();
	}

}
