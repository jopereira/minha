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

package pt.minha.api;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import pt.minha.kernel.instrument.ClassConfig;
import pt.minha.kernel.simulation.Timeline;
import pt.minha.models.global.net.Network;
import pt.minha.models.global.net.NetworkConfig;

/**
 * This is the main entry point for Minha. It provides a method to create
 * simulated hosts and interact with the simulated world as whole.
 */
public class World {
	private ClassConfig cc;
	private NetworkConfig nc;
	private Timeline timeline;
	private Network network;
	private List<Host> hosts = new ArrayList<Host>();
	private List<Invocation> queue = new ArrayList<Invocation>();
	private boolean closed;
	private Thread thread;
	
	public World(long simulationTime) throws Exception {		
		
		// load network calibration values
		Properties props = new Properties();
		props.load(ClassLoader.getSystemClassLoader().getResourceAsStream("default.network.properties"));			
		props = new Properties(props);
		InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("network.properties"); 
		if (is!=null)
			props.load(is);
		nc = new NetworkConfig(props);
		
		// load instrumentation properties
		props = new Properties();
		props.load(ClassLoader.getSystemClassLoader().getResourceAsStream("default.instrument.properties"));			
		props = new Properties(props);
		is = ClassLoader.getSystemClassLoader().getResourceAsStream("instrument.properties"); 
		if (is!=null)
			props.load(is);
		cc = new ClassConfig(props);
		timeline = new Timeline(simulationTime);
		
		network = new Network(timeline, nc);
	}
	
	/**
	 * Create a host.
	 * 
	 * @param ip address for host, null for auto-assignment
	 * @return a host instance
	 * @throws SimulationException
	 */
	public Host createHost(String ip) throws SimulationException {
		return new Host(this, cc, timeline, ip, network);
	}

	/**
	 * Create a host.
	 * 
	 * @return a host instance
	 * @throws SimulationException
	 */
	public Host createHost() throws SimulationException {
		return createHost(null);
	}
	
	/**
	 * Get hosts in this simulation container.
	 * @return host collection
	 */
	public Collection<Host> getHosts() {
		return Collections.unmodifiableCollection(hosts);
	}
	
	/**
	 * Run the simulation until all events are processed or maximum set time
	 * is reached. This method waits for all outstanding callback events to
	 * be processed.
	 * 
	 * @return time of latest event processed
	 * @throws InterruptedException
	 */
	public long run() throws InterruptedException {
		timeline.run();
		synchronized(this) {
			closed = true;
			notifyAll();			
		}
		thread.join();
		thread = null;
		return timeline.getTime();
	}
	
	/** 
	 * Create a proxy to receive callbacks form the simulation.
	 * 
	 * @param clz the interface (global) of the class to be created
	 * @param impl the object implementing the interface handling invocations
	 * @returns an exit proxy
	 */
	public <T> Exit<T> createExit(Class<T> intf, T impl) {
		return new Exit<T>(this, intf, impl);
	}
	
	private static class Invocation {
		public Object target;
		public Method method;
		public Object[] args;
		
		public Invocation(Object target, Method method, Object[] args) {
			this.target = target;
			this.method = method;
			this.args = args;
		}		
	};
	
	synchronized void handleInvoke(Object target, Method method, Object[] args) {
		if (thread == null) {
			closed = false;
			thread = new Thread() {
				public void run() {
					globalThread();
				}
			};
			thread.start();
		}

		queue.add(new Invocation(target, method, args));
		notifyAll();		
	}
	
	private synchronized Invocation next() throws InterruptedException {
		while(!closed && queue.isEmpty())
			wait();
		if (queue.isEmpty())
			return null;
		return queue.remove(0);
	}

	private void globalThread() {
		while(true) {
			try {
				Invocation i = next();
			
				if (i == null)
					return;
				
				i.method.invoke(i.target, i.args);
				
			} catch (Exception e) {
				// FIXME: should stop simulation?
				e.printStackTrace();
			}
		}
	}
}
