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

package pt.minha.api.sim;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
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

import pt.minha.api.Entry;
import pt.minha.api.Exit;
import pt.minha.api.Host;
import pt.minha.api.Main;
import pt.minha.api.Milestone;
import pt.minha.api.ContainerException;
import pt.minha.api.World;
import pt.minha.kernel.instrument.ClassConfig;
import pt.minha.kernel.simulation.Resource;
import pt.minha.kernel.simulation.Scheduler;
import pt.minha.kernel.simulation.Timeline;
import pt.minha.models.global.ResultHolder;
import pt.minha.models.global.disk.Storage;
import pt.minha.models.global.net.Network;
import pt.minha.models.global.net.NetworkStack;

/**
 * The simulation container. This is the main entry point for Minha.
 * It provides a method to create simulated hosts and interact with
 * the simulated world as whole.
 */
public class Simulation implements World {
	private static Logger logger = LoggerFactory.getLogger("pt.minha.API");
	
	private ClassConfig cc;
	private Calibration nc;
	private Scheduler sched;
	private Network network;
	private List<Host> hosts = new ArrayList<Host>();
	private List<Invocation> queue = new ArrayList<Invocation>();
	private boolean stopping, closed;
	private Thread exitThread;
	private boolean running, blocked;
	private ReentrantLock lock = new ReentrantLock();
	private Condition cond = lock.newCondition();
		
	/**
	 * Create a new simulation container.
	 * @throws Exception
	 */
	public Simulation() throws Exception {		
		
		// load calibration values
		Properties props = new Properties();
		props.load(ClassLoader.getSystemClassLoader().getResourceAsStream("default.calibration.properties"));			
		props = new Properties(props);
		InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("calibration.properties"); 
		if (is!=null)
			props.load(is);
		nc = new Calibration(props);
		
		// load instrumentation properties
		props = new Properties();
		props.load(ClassLoader.getSystemClassLoader().getResourceAsStream("default.instrument.properties"));			
		props = new Properties(props);
		is = ClassLoader.getSystemClassLoader().getResourceAsStream("instrument.properties"); 
		if (is!=null)
			props.load(is);
		cc = new ClassConfig(props);
		
		// load simulation properties
		props = new Properties();
		props.load(ClassLoader.getSystemClassLoader().getResourceAsStream("default.simulation.properties"));			
		props = new Properties(props);
		is = ClassLoader.getSystemClassLoader().getResourceAsStream("simulation.properties"); 
		if (is!=null)
			props.load(is);
		int procs = Integer.parseInt(props.getProperty("processors"));
		if (procs < 0)
			procs = Runtime.getRuntime().availableProcessors()*-procs;		
		int timelines = Integer.parseInt(props.getProperty("timelines"));
		if (timelines < 0)
			timelines = procs*-timelines;		
		long fuzzyness = Long.parseLong(props.getProperty("fuzzyness"));
		if (fuzzyness < 0)
			fuzzyness = (nc.getLineDelay(10)+nc.getLineLatency(10))*-fuzzyness;		
		sched = new Scheduler(procs, timelines, fuzzyness);
		
		logger.info("using up to {} processors, {}ns fuzzyness", procs, fuzzyness);
		
		network = new Network(nc);
	}
	
	/* (non-Javadoc)
	 * @see pt.minha.api.WorldI#createHost(java.lang.String)
	 */
	@Override
	public Host createHost(String ip) throws ContainerException {
		try {
			Timeline timeline = sched.createTimeline();
			NetworkStack ns = new NetworkStack(timeline, ip, network);
			Resource cpu = new Resource(timeline, ns.getLocalAddress().getHostAddress());
			Storage storage = new Storage(timeline, nc, ns.getLocalAddress().getHostAddress());
			HostImpl host = new HostImpl(this, cc, timeline, cpu, ns, storage);
			hosts.add(host);
			return host;
		} catch (UnknownHostException e) {
			throw new ContainerException(e);
		}

	}

	/* (non-Javadoc)
	 * @see pt.minha.api.WorldI#createHost()
	 */
	@Override
	public Host createHost() throws ContainerException {
		return createHost(null);
	}
	
	/* (non-Javadoc)
	 * @see pt.minha.api.WorldI#getHosts()
	 */
	@Override
	public Collection<Host> getHosts() {
		return Collections.unmodifiableCollection(hosts);
	}

	/* (non-Javadoc)
	 * @see pt.minha.api.WorldI#createEntries(int)
	 */
	@Override
	public Entry<Main>[] createEntries(int n) throws IllegalArgumentException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ContainerException {
		@SuppressWarnings("unchecked")
		Entry<Main>[] entries = new Entry[n];
		for(int i = 0; i<n; i++)
			entries[i] = createHost().createProcess().createEntry();
		return entries;
	}
	
	/* (non-Javadoc)
	 * @see pt.minha.api.WorldI#createEntries(int, java.lang.Class, java.lang.String)
	 */
	@Override
	public <T> Entry<T>[] createEntries(int n, Class<T> intf, String impl) throws IllegalArgumentException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ContainerException {
		@SuppressWarnings("unchecked")
		Entry<T>[] entries = new Entry[n];
		for(int i = 0; i<n; i++)
			entries[i] = createHost().createProcess().createEntry(intf, impl);
		return entries;
	}	
	
	/* (non-Javadoc)
	 * @see pt.minha.api.WorldI#createExits(pt.minha.api.Entry, java.lang.Class, T1)
	 */
	@Override
	public <T1,T2> Exit<T1>[] createExits(Entry<T2>[] entries, Class<T1> intf, T1 impl) {
		@SuppressWarnings("unchecked")
		Exit<T1>[] exits = new Exit[entries.length];
		for(int i = 0; i<entries.length; i++)
			exits[i] = entries[i].getProcess().createExit(intf, impl);
		return exits;
	}
	
	/* (non-Javadoc)
	 * @see pt.minha.api.WorldI#run()
	 */
	@Override
	public long run() {
		return runNanos(0);
	}

	/* (non-Javadoc)
	 * @see pt.minha.api.WorldI#run(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public long run(long timeout, TimeUnit unit) {
		return runNanos(unit.toNanos(timeout));
	}

	
	/* (non-Javadoc)
	 * @see pt.minha.api.WorldI#runNanos(long)
	 */
	@Override
	public long runNanos(long nanosTimeout) {
		acquire(true);
		runSimulation(nanosTimeout);
		long time = sched.getTime();
		release(true);
		return time;
	}
	
	/* (non-Javadoc)
	 * @see pt.minha.api.WorldI#runAll(pt.minha.api.Milestone)
	 */
	@Override
	public long runAll(Milestone... milestones) throws IllegalArgumentException {
		try {
			acquire(true);
			for (Milestone m : milestones) {
				MilestoneImpl mi = (MilestoneImpl) m;
				if (!mi.isPending())
					throw new IllegalArgumentException();
				mi.setWaited();
			}
			for (Milestone m : milestones) {
				while (!m.isComplete() && runSimulation(0))
					;
			}
			long time = sched.getTime();
			return time;
		} finally {
			release(true);
		}
	}
	
	/**
	 * Get the current calibration parameters.
	 * @return calibration parameters
	 */
	public Calibration getCalibration() {
		return nc;
	}
	
	boolean runSimulation(long limit) {
		if (limit != 0)
			limit += sched.getTime();
		
		boolean hasNext = sched.run(limit);
		
		lock.lock();
		stopping = true;
		cond.signalAll();
		while(exitThread != null)
			cond.awaitUninterruptibly();
		lock.unlock();
		
		return hasNext;
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
			stopping = false;
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
				while(!stopping && queue.isEmpty())
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

	/* (non-Javadoc)
	 * @see pt.minha.api.WorldI#close()
	 */
	@Override
	public void close() throws IOException {
		if (closed)
			return;
		closed = true;
		ArrayList<Host> l = new ArrayList<Host>(hosts);
		for(Host h: l)
			h.close();
	}

	void removeHost(Host host) {
		hosts.remove(host);
	}
}
