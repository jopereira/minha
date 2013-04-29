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

package pt.minha.models.local;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import pt.minha.kernel.simulation.Resource;
import pt.minha.kernel.simulation.Timeline;
import pt.minha.models.global.EntryHandler;
import pt.minha.models.global.ExitHandler;
import pt.minha.models.global.HostInterface;
import pt.minha.models.global.ResultHolder;
import pt.minha.models.global.net.Network;
import pt.minha.models.global.net.NetworkStack;
import pt.minha.models.local.lang.SimulationThread;


public class HostImpl implements HostInterface {
	// CPU
	private Resource cpu;
	private Set<SimulationThread> threads = new HashSet<SimulationThread>();
	private long threadId = -2; // -1 is "main" thread, 0 is the first user thread
	
	// Networking
	private NetworkStack network;
		
	public HostImpl(Timeline timeline, String host, Network network) throws UnknownHostException, FileNotFoundException {
		this.network = new NetworkStack(timeline, host, network);
		this.cpu = new Resource(timeline, this.network.getLocalAddress().getHostAddress());
	}
	
	public Resource getCPU() {
		return cpu;
	}
	
	public Timeline getTimeline() {
		return cpu.getTimeline();
	}
	
	public NetworkStack getNetwork() {
		return network;
	}

	public long getNextThreadId() {
		threadId++;
		return threadId;
	}
	
	/* The following methods are supposed to be called outside simulation events,
	 * as such, must be synchronized. */
	
	public synchronized void addThread(SimulationThread simulationThread) {
		threads.add(simulationThread);
	}
	
	public synchronized void removeThread(SimulationThread simulationThread) {
		threads.remove(simulationThread);
	}
	
	public void stop() throws InterruptedException {
		while(true) {
			SimulationThread t = null;
			synchronized(this) {
				if (threads.isEmpty())
					break;
				t = threads.iterator().next();
			}
			t.fake_join();
		}
		cpu.stop();
	}

	@Override
	public InetAddress getAddress() {
		return network.getLocalAddress();
	}

	@Override
	public EntryHandler createEntry(String impl) {
		return new Trampoline(this, impl);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T createExit(Class<T> intf, final ExitHandler target) {
		return (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[]{ intf }, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				
				SimulationThread.stopTime(0);
				
				ResultHolder result = new ResultHolder();
				try {
					if (target.invoke(method, args, result))
						return result.getResult();
					return result.getFakeResult(method.getReturnType());
				} finally {
					SimulationThread.startTime(0);
				}
			}
		});
	}	
}
