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

package pt.minha.models.local.lang;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import pt.minha.kernel.simulation.Event;
import pt.minha.kernel.simulation.Timeline;
import pt.minha.kernel.timer.IntervalTimer;
import pt.minha.kernel.timer.TimerProvider;
import pt.minha.models.local.HostImpl;

public class SimulationThread extends Thread {
	private static IntervalTimer timer = TimerProvider.open();
	
	private pt.minha.models.fake.java.lang.Thread fakeThread;
	private long id;
	private HostImpl host;
	private long time = -1;
	private boolean blocked, rt, dead, started, interruptible, interrupted;
	private boolean uninterruptible;
	
	private WakeUpEvent wakeup;
	private Condition wakeupCond;
	private ReentrantLock lock;
	
	private Runnable runnable;
	
	// fake_join
	private pt.minha.models.fake.java.util.concurrent.locks.ReentrantLock joinLock = new pt.minha.models.fake.java.util.concurrent.locks.ReentrantLock();
	private Condition joinCond = joinLock.newCondition();
	private boolean joinDead = false;
		
	public SimulationThread(boolean rt, HostImpl host, Runnable runnable, pt.minha.models.fake.java.lang.Thread fakeThread) {

		if (host==null)
			host = ((SimulationThread) Thread.currentThread()).host;
		
		setContextClassLoader(this.getClass().getClassLoader());
				
		this.rt = rt;
		this.host = host;
		this.runnable = runnable;
		this.fakeThread = fakeThread;
		this.id=host.getNextThreadId();
		
		this.wakeup = new WakeUpEvent();
		this.lock = new ReentrantLock();
		
		this.wakeupCond = lock.newCondition();
		
		blocked = true;
		setDaemon(true);
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

		getWakeup().schedule(delay);
	}
	
	public static SimulationThread currentSimulationThread() {
		return (SimulationThread)Thread.currentThread();
	}
	
	public Object getFakeThread() {
		return this.fakeThread;
	}
	
	public HostImpl getHost() {
		return host;
	}
	
	public Timeline getTimeline() {
		return host.getCPU().getTimeline();
	}

	public void run() {
		host.addThread(this);
		
		lock.lock();
		while(blocked)
			wakeupCond.awaitUninterruptibly();
		lock.unlock();
		
		try {
			if (rt) startTime(0);
			runnable.run();
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(1);
		} finally {
			joinLock.lock();
			joinDead = true;
			joinCond.signal();
			joinLock.unlock();

			if (rt) stopTime(0);

			lock.lock();
			blocked = true;
			wakeupCond.signal();
			dead = true;
			lock.unlock();
		
			host.removeThread(this);
		}
	}
	
	public void fake_join() throws InterruptedException {
		try {
			joinLock.lock();
			while(!joinDead)
				joinCond.await();
		} finally {
			joinLock.unlock();
		}
	}
	
	/**
	 * This method gets called by the time-line thread. It should be blocked while
	 * a simulation thread is executing on behalf of an event.
	 */
	private void wakeup() {
		lock.lock();
		
		if (dead)
			throw new RuntimeException("Dead thread waking up. They live!");
		
		blocked = false;
		interruptible = false;
		uninterruptible = false;
		wakeupCond.signal();
		
		while(!blocked)
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

		while(blocked)
			wakeupCond.awaitUninterruptibly();

		lock.unlock();
		
		return getInterruptedStatus(clear);
	}
	
	public Event getWakeup() {		
		return wakeup;
	}
	
	private void resync() {
		lock.lock();
		
		blocked = true;
		wakeupCond.signal();

		while(blocked)
			wakeupCond.awaitUninterruptibly();
		
		lock.unlock();
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
		if (time>=0)
			throw new RuntimeException("restarting time");

		host.getCPU().acquire(getWakeup());
		resync();
		
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
		long delta = timer.getTime() - time;
		
		if (time<0)
			throw new RuntimeException("restopping time");
		
		time = -1;
		
		host.getCPU().release(delta+overhead, getWakeup());
		resync();
		
		return getTimeline().getTime();
	}
	
	public boolean idle(long delta, boolean interruptible, boolean clear) {
		if (time>=0)
			throw new RuntimeException("time not stopped");
		
		getWakeup().schedule(delta);
		return pause(interruptible, clear);
	}
	
	private class WakeUpEvent extends Event {
		public WakeUpEvent() {
			super(host.getTimeline());
		}

		public void run() {
			wakeup();
		}
		
		public String toString() {
			return "Wakeup "+SimulationThread.this;
		}
	}
	
	public boolean fake_isAlive() {
		return started && !dead;
	}

	public long fake_getId() {
		return id;
	}
	
	public String toString() {
		return "SimulationThread@"+host.getNetwork().getLocalAddress().getHostAddress()+"["+fakeThread.getName()+"]";
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
