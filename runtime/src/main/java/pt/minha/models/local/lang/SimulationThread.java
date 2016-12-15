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

package pt.minha.models.local.lang;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.minha.api.Host;
import pt.minha.api.sim.Calibration;
import pt.minha.kernel.simulation.Event;
import pt.minha.kernel.simulation.Timeline;
import pt.minha.kernel.timer.IntervalTimer;
import pt.minha.kernel.timer.TimerProvider;
import pt.minha.models.global.SimulationThreadDeath;
import pt.minha.models.local.SimulationProcess;

public class SimulationThread extends Thread implements Closeable {
	private static IntervalTimer timer = TimerProvider.open();
	
	private pt.minha.models.fake.java.lang.Thread fakeThread;
	private long id;
	private SimulationProcess process;
	private Calibration nc;
	private long time;
	private boolean realTime = false;
	private boolean blocked, rt, dead, started, interruptible, interrupted;
	private boolean parked, permit;
	
	private WakeUpEvent wakeup;
	private Condition wakeupCond;
	private ReentrantLock lock;
	
	private Runnable runnable;
	
	public long totalCPU;
	
	private Logger logger;
	
	// fake_join
	private List<Event> waitingOnJoin = new ArrayList<Event>();
		
	public SimulationThread(boolean rt, SimulationProcess host, Runnable runnable, pt.minha.models.fake.java.lang.Thread fakeThread) {

		if (host==null)
			host = ((SimulationThread) Thread.currentThread()).process;
		
		setContextClassLoader(this.getClass().getClassLoader());

		this.rt = rt;
		this.process = host;
		this.nc = process.getNetwork().getConfig();
		this.runnable = runnable;
		this.fakeThread = fakeThread;
		this.id=host.getNextThreadId();
		
		this.wakeup = new WakeUpEvent();
		this.lock = new ReentrantLock();
		
		this.wakeupCond = lock.newCondition();
		
		blocked = true;
		setDaemon(true);
		
		logger = LoggerFactory.getLogger("pt.minha.thread."+host.getNetwork().getLocalAddress().getHostAddress().replace(".", "_")+"."+id);
	}

	/**
	 * Schedule thread start-up. The thread itself will only run in the context of
	 * a simulation event.
	 */
	@Override
	public void start() {
		super.start();

		started = true;
		
		SimulationThread.stopTime(0);
		getWakeup().schedule(0);
		SimulationThread.startTime(0);
	}

	public void simulationStart(long delay) {		
		super.start();

		started = true;
		
		getWakeup().schedule(delay);
	}
	
	public static SimulationThread currentSimulationThread() {
		return (SimulationThread)Thread.currentThread();
	}
	
	public Object getFakeThread() {
		return this.fakeThread;
	}
	
	public SimulationProcess getProcess() {
		return process;
	}
	
	public Host getHost() {
		return process.getHost();
	}

	public Timeline getTimeline() {
		return process.getCPU().getTimeline();
	}

	public void run() {
		process.addThread(this);
		
		lock.lock();
		while(blocked && !dead)
			wakeupCond.awaitUninterruptibly();
		lock.unlock();

		try {
			if (dead)
				return;

			if (rt) startTime(0);
			runnable.run();
		} catch(SimulationThreadDeath d) {
			if (fake_isDaemon())
				logger.info("Daemon thread killed");
			else
				logger.warn("Non-daemon thread killed.");
		} catch (Throwable e) {
			pt.minha.models.fake.java.lang.Thread.UncaughtExceptionHandler handler = fakeThread.getUncaughtExceptionHandler();
			try {
				if (handler != null) {
					handler.uncaughtException(fakeThread, e);
					e = null;
				}
			} catch(Throwable t) {
				e = t;
			}
			if (e != null)
				logger.warn("Uncaught exception", e);
		} finally {

			lock.lock();
			blocked = true;
			wakeupCond.signal();
			dead = true;
			lock.unlock();
		
			process.removeThread(this);

			for(Event e: waitingOnJoin) {
				e.schedule(0);
			}
			waitingOnJoin.clear();
		}
	}
	
	public void fake_join(long timeout) throws InterruptedException {
		fake_join(timeout, 0);
	}

	public void fake_join(long millis, long nanos) throws InterruptedException {

		try {
			long now = SimulationThread.stopTime(0);

			if (dead)
				return;

			SimulationThread current = SimulationThread.currentSimulationThread();

			long timeout = millis*1000000+nanos;
			long target = 0;
			if (timeout > 0) {
				target = now + timeout;
			}

			waitingOnJoin.add(current.getWakeup());
			while(!dead) {
				if (timeout > 0) {
					long delta = target - getTimeline().getTime();
					if (delta <= 0)
						break;
					current.getWakeup().schedule(target);
				}
				if (current.pause(true, false))
					break;
			}

			if (!dead)
				waitingOnJoin.remove(current.getWakeup());

			if (current.getInterruptedStatus(true))
				throw new InterruptedException();

		} finally {
			SimulationThread.startTime(0);
		}
	}
	
	/**
	 * This method gets called by the time-line thread. It should be blocked while
	 * a simulation thread is executing on behalf of an event.
	 */
	private void wakeup() {
		lock.lock();
		
		blocked = false;
		interruptible = false;
		wakeupCond.signal();
		
		while(!blocked && !dead)
			wakeupCond.awaitUninterruptibly();
		
		lock.unlock();
	}

	public void fake_interrupt() {
		interrupted = true;
		try {
			lock.lock();
			if (interruptible)
				wakeup();
		} finally {
			lock.unlock();
		}
	}

	public boolean getInterruptedStatus(boolean clear) {
		boolean r = interrupted;
		if (clear)
			interrupted = false;
		return r;
	}

	/**
	 * Generic method to suspend the thread. Typically, the threads
	 * wake-up event will have been scheduled in the future or have
	 * been placed in some queue from where it will be scheduled.
	 * 
	 * This method also provides support for thread interruption.
	 * Note however that it doesn't check, on entry, whether the
	 * thread is already interrupted. If no blocking is desired
	 * in such situation, it should be checked before entering
	 * pause.
	 * 
	 * @param interruptible makes the thread wake-up on interrupt
	 * @param clear if true, will clear interrupted state on exit
	 * @return interrupted state
	 */
	public boolean pause(boolean interruptible, boolean clear) {
		lock.lock();
		
		blocked = true;
		this.interruptible = interruptible;
		wakeupCond.signal();

		while(blocked && !dead)
			wakeupCond.awaitUninterruptibly();
		
		lock.unlock();

		if (dead)
			throw new SimulationThreadDeath();
		
		return getInterruptedStatus(clear);
	}
	
	public Event getWakeup() {		
		return wakeup;
	}
	
	private void resync() {
		lock.lock();
		
		blocked = true;
		wakeupCond.signal();

		while(blocked && !dead)
			wakeupCond.awaitUninterruptibly();
		
		lock.unlock();

		if (dead)
			throw new SimulationThreadDeath();
	}

	/**
	 * Acquire CPU resource and start executing real code. Acquiring the CPU
	 * might advance the simulation time, thus blocking the calling thread.
	 * @throws InterruptedException 
	 */
	public static void startTime(long overhead) {
		((SimulationThread) currentThread()).doStartTime(overhead);
	}
	
	private void doStartTime(long overhead) {
		if (realTime)
			throw new RuntimeException("restarting time");

		process.getCPU().acquire(getWakeup());
		resync();
		
		realTime = true;
		time = timer.getTime() - overhead;
	}
	
	/**
	 * Stop executing real code and free CPU resource. Stopping real code will advance
	 * the simulation time, thus blocking the calling thread.
	 * @throws InterruptedException 
	 */
	public static long stopTime(long overhead) {
		return ((SimulationThread) currentThread()).doStopTime(overhead);
	}
	
	private long doStopTime(long overhead) {
		long delta = nc.getCPUDelay(timer.getTime() - time) + overhead;
		
		if (delta<0)
			delta = 1;
		
		if (!realTime)
			throw new RuntimeException("restopping time");
		
		realTime = false;
		
		totalCPU += delta;
		process.getCPU().release(delta, getWakeup());
		resync();
		
		return getTimeline().getTime();
	}
	
	public boolean idle(long delta, boolean interruptible, boolean clear) {
		if (realTime)
			throw new RuntimeException("time not stopped");
		
		getWakeup().schedule(delta);
		return pause(interruptible, clear);
	}
	
	private class WakeUpEvent extends Event {
		public WakeUpEvent() {
			super(process.getTimeline());
		}

		public void run() {
			wakeup();
		}
		
		public String toString() {
			return super.toString()+"->"+SimulationThread.this;
		}
	}
	
	public boolean fake_isAlive() {
		return started && !dead;
	}

	public long fake_getId() {
		return id;
	}
	
	public boolean fake_isDaemon() {
		return fakeThread.isDaemon();
	}
	
	public StackTraceElement[] fake_getStackTrace() {
		return this.getStackTrace();
	}
	
	public void park(Object blocker, long nanos) {
		if (permit)
			permit = false;
		else {
			fakeThread.parkBlocker = blocker;
			stopTime(0);
			if (nanos>0)
				getWakeup().schedule(nanos);
			parked = true;
			pause(true, false);
			parked = false;
			startTime(0);
		}
	}
	
	public void unpark(pt.minha.models.fake.java.lang.Thread target) {
		SimulationThread st = target.getSimulationThread();
		if (st.parked) {
			stopTime(0);
			st.getWakeup().schedule(0);
			startTime(0);
		} else
			st.permit = true;
	}
	
	public String toString() {
		return "SimulationThread@"+process.getNetwork().getLocalAddress().getHostAddress()+"["+fakeThread.getName()+"]";
	}

	@Override
	public void close() throws IOException {
		lock.lock();
		dead = true;
		wakeupCond.signalAll();
		lock.unlock();
	}
	
	/*
	 public void m() {
	 	realCode();
	 	SimulationThread.stopTime(0);
	 	
	 	// One event starts here
	 	simStuff();
	 	Event e=getWakeup();
	 	e.schedule(someTime);
	 	simStuff();
	 	pause(); // ends here
	 	
	 	// One event starts here
	 	simStuff();
	 	Event e=getWakeup();
	 	e.schedule(someTime);
	 	simStuff();
	 	pause(); // ends here

		// Another event
		simStuff();

	 	SimulationThread.startTime(0);
	 	realCode();
	 	
	 }
	 */
}
