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
import java.net.InetSocketAddress;
import java.net.MulticastSocket;

import mockit.FullVerifications;
import mockit.Mocked;

import org.testng.annotations.Test;

import pt.minha.api.Entry;
import pt.minha.api.Exit;
import pt.minha.api.sim.Global;
import pt.minha.api.sim.Simulation;

public class MulticastTest {
	
	@Global
	public static interface Callback {
		public void receive(DatagramPacket packet, InetSocketAddress address);
		public void exception(Exception e);
	}
	
	@Global
	public static interface Target {
		public void open(InetSocketAddress addr, Callback cb) throws IOException;
		public void send(DatagramPacket packet) throws IOException;
	}
	
	public static class Impl implements Target {
		private MulticastSocket socket;
		private Callback callback;

		public void open(InetSocketAddress addr, Callback cb) throws IOException {
			this.callback = cb;
			this.socket = new MulticastSocket(addr.getPort());
			
			if (addr.getAddress().isMulticastAddress())
				socket.joinGroup(addr.getAddress());
			
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
	}
	
	// https://code.google.com/p/minha/issues/detail?id=6
	@Test
	public void multicast(@Mocked final Callback cb) throws Exception {
		Simulation world = new Simulation();
				
		final int N = 10;
		final Entry<Target>[] en = world.createEntries(N, Target.class, Impl.class.getName());
		final Exit<Callback>[] ex = world.createExits(en, Callback.class, cb);

		final byte[] data = "Message!".getBytes(); 
		
		final InetSocketAddress addr = new InetSocketAddress("224.1.2.3", 12345); 
		final DatagramPacket packet = new DatagramPacket(data, data.length, addr);
		
		for(int i = 0; i<en.length; i++)
			en[i].queue().open(addr, ex[i].report());
		en[0].at(1, SECONDS).queue().send(packet);
		world.runAll(ex);
		
		world.close();

		new FullVerifications() {
			{
				cb.receive(withAny(packet), withAny(addr)); times = N;
				forEachInvocation = new Object() {
					void validate(DatagramPacket p, InetSocketAddress a) {
						// Receiver address
						assertNotNull(a);
						assertEquals(a.getPort(), addr.getPort());
						// Sender address
						assertNotNull(p);
						assertEquals(p.getAddress(), en[0].getProcess().getHost().getAddress());
						assertEquals(p.getPort(), addr.getPort());
						// Data
						assertNotNull(p.getData());
						assertTrue(DatagramTest.checkData(p, data));			
					}
				};
			}
		};		
	}

	@Test
	public void filter(@Mocked final Callback yes, @Mocked final Callback no) throws Exception {
		Simulation world = new Simulation();
		
		final int N = 3;
		final Entry<Target>[] en = world.createEntries(N, Target.class, Impl.class.getName());
		final Exit<Callback>[] ex = world.createExits(en, Callback.class, no);

		final byte[] data = "Message!".getBytes(); 
		
		ex[0] = en[0].getProcess().createExit(Callback.class, yes);
		
		final InetSocketAddress addr = new InetSocketAddress("224.1.2.3", 12345); 
		en[0].queue().open(addr, ex[0].report());
		
		// Same address, different port
		en[1].queue().open(new InetSocketAddress("224.1.2.3", 54321), ex[1].report());
		// No join (but on joined host), same port
		en[1].queue().open(new InetSocketAddress("127.0.0.1", 12345), ex[1].report());
		// Different address, same port
		en[2].queue().open(new InetSocketAddress("224.3.2.1", 12345), ex[2].report());

		final DatagramPacket packet = new DatagramPacket(data, data.length, addr);
		en[0].at(1, SECONDS).queue().send(packet);
		world.run();
		
		world.close();

		new FullVerifications() {
			{
				yes.receive(withAny(packet), new InetSocketAddress("10.0.0.1", 12345));
			}
		};		
	}
}
