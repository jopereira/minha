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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.minha.kernel.instrument.ClassConfig;
import pt.minha.kernel.simulation.Timeline;
import pt.minha.models.global.ResultHolder;
import pt.minha.models.global.net.Network;
import pt.minha.models.global.net.NetworkConfig;

/**
 * This is the main entry point for Minha. It provides a method to create
 * simulated hosts and interact with the simulated world as whole.
 */
public class World {
	private static Logger logger = LoggerFactory.getLogger("pt.minha.API");
	
	private ClassConfig cc;
	private NetworkConfig nc;
	private Timeline timeline;
	private Network network;
	private List<Host> hosts = new ArrayList<Host>();
	private List<Invocation> queue = new ArrayList<Invocation>();
	private boolean closed;
	private Thread exitThread;
	private boolean running, blocked;
	private ReentrantLock lock = new ReentrantLock();
	private Condition cond = lock.newCondition();
		
	/**
	 * Create a new simulation container.
	 * @throws Exception
	 */
	public World() throws Exception {		
		
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
		timeline = new Timeline();
		
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
	 * Create a host with automatically assigned IP address.
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
	 * Run the simulation until all events are processed. Warning: If
	 * the simulation has some activity hat never
	 * terminates, this method will never return. The safe way to run a
	 * simulation is to use a timeout or a set of milestones.
	 * 
	 * @return time of latest event processed
	 * @throws InterruptedException
	 */
	public long run() {
		return runNanos(0);
	}

	/**
	 * Run the simulation until all events are processed or the time limit
	 * is reached. This method waits for all outstanding callback events to
	 * be processed.
	 * 
	 * @param timeout run only to the specified time limit
	 * @param unit time units for timeout parameter
	 * @return time of latest event processed
	 * @throws InterruptedException
	 */
	public long run(long timeout, TimeUnit unit) {
		return runNanos(unit.toNanos(timeout));
	}

	
	/**
	 * Run the simulation until all events are processed or the time limit
	 * is reached. This method waits for all outstanding callback events to
	 * be processed.
	 * 
	 * @param nanosTimeout run only to the specified time limit
	 * @return time of latest event processed
	 */
	public long runNanos(long nanosTimeout) {
		acquire(true);
		runSimulation(nanosTimeout);
		long time = timeline.getTime();
		release(true);
		return time;
	}
	
	/**
	 * Run the simulation until all events are processed or all milestones
	 * achieved. This method waits for all outstanding callback events to
	 * be processed.
	 * 
	 * @param milestones goals to be met
	 * @return time of latest event processed
	 */
	public long runAll(Milestone... milestones) {
		acquire(true);
		for(Milestone m: milestones)
			m.setWaited();
		for(Milestone m: milestones) { 
			while(!m.isComplete() && runSimulation(0))
				;
		}
		long time = timeline.getTime();
		release(true);
		return time;
	}
	
	boolean runSimulation(long limit) {
		boolean empty = timeline.run(limit);
		
		lock.lock();
		closed = true;
		cond.signalAll();
		while(exitThread != null)
			cond.awaitUninterruptibly();
		lock.unlock();
		
		return empty;
	}
	
	void acquire(boolean runner) {
		lock.lock();
		if (running && (!blocked || runner))
			throw new ConcurrentModificationException("reentering simulation");
		if (runner) {
			running = true;
			lock.unlock();
		}
	}
	
	void setBlocked(boolean blocked) {
		this.blocked = blocked; 
	}
	
	void release(boolean runner) {
		if (runner) {
			lock.lock();
			running = false;
		}
			
		lock.unlock();
	}
	
	private static class Invocation {
		public Object target;
		public Method method;
		public Object[] args;
		private ResultHolder result;
		
		public Invocation(Object target, Method method, Object[] args, ResultHolder result) {
			this.target = target;
			this.method = method;
			this.args = args;
			this.result = result;
		}		
	};
	
	void handleInvoke(Object target, Method method, Object[] args, ResultHolder result) {
		lock.lock();
		if (exitThread == null) {
			closed = false;
			exitThread = new Thread() {
				public void run() {
					exitThread();
				}
			};
			exitThread.start();
		}

		queue.add(new Invocation(target, method, args, result));
		cond.signalAll();
		lock.unlock();
	}
	
	private void exitThread() {
		lock.lock();
		try {
			while(true) {
				Invocation i = null;
				while(!closed && queue.isEmpty())
					cond.awaitUninterruptibly();
				if (queue.isEmpty())
					return;
				i = queue.remove(0);

				setBlocked(true);
				lock.unlock();
				try {
					i.result.reportReturn(i.method.invoke(i.target, i.args));
				} catch(InvocationTargetException ite) {
					i.result.reportException(ite.getTargetException());
				}
				lock.lock();
				setBlocked(false);					
			}
		} catch (Exception e) {
			logger.error("unexpected exception on exit", e);
		} finally {
			exitThread = null;
			cond.signalAll();
			lock.unlock();
		}
	}
}
