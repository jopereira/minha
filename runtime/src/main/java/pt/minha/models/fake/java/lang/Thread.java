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

package pt.minha.models.fake.java.lang;

import pt.minha.models.local.SimulationProcess;
import pt.minha.models.local.lang.SimulationThread;

public class Thread extends Object implements Runnable {
	private final SimulationThread simulationThread;
	private String name;
	private int priority = NORM_PRIORITY;
	private ThreadGroup group;
	private boolean daemon;
	private UncaughtExceptionHandler handler;
	
	public volatile java.lang.Object parkBlocker;
	
	private static void checkSimulation(boolean sim) {
		if (sim != (java.lang.Thread.currentThread() instanceof SimulationThread))
			throw new RuntimeException("Trying to "+(sim?"leave":"enter")+"sandbox.");
	}
	
	public Thread() {
		this(null, null, null);
	}
	
	public Thread(Runnable runnable) {
		this(null, runnable, null);
	}

	public Thread(String name) {
		this(null, null, name);
	}

	public Thread(Runnable runnable, String name) {
		this(null, runnable, name);
	}

	public Thread(ThreadGroup group, Runnable runnable, String name, long stackSize) {
		this(group, runnable, name);
	}

	public Thread(ThreadGroup group, Runnable runnable) {
		this(group, runnable, null);
	}

	public Thread(ThreadGroup group, Runnable runnable, String name) {
		checkSimulation(true);
		
		if (group==null)
			this.group = Thread.currentThread().getThreadGroup();
		if (runnable==null)
			runnable = this;
		
		this.simulationThread = new SimulationThread(true, null, runnable, this);
		
		if (name!=null)
			this.setName(name);
		else
			this.setName("Thread-"+simulationThread.fake_getId());
	}
	
	public Thread(SimulationProcess host, Runnable runnable) {
		checkSimulation(false);
		this.simulationThread = new SimulationThread(true, host, runnable, this);
		this.setName("main");
	}

	public final static int MIN_PRIORITY = java.lang.Thread.MIN_PRIORITY;
	public final static int NORM_PRIORITY = java.lang.Thread.NORM_PRIORITY;
	public final static int MAX_PRIORITY = java.lang.Thread.MAX_PRIORITY;
		
	public static Thread currentThread() {
		return (Thread)SimulationThread.currentSimulationThread().getFakeThread();
	}
		
	public static void sleep(long millis) throws InterruptedException {
		sleep(millis, 0);
	}

	public static void sleep(long millis, int nanos) throws InterruptedException {
		try {
			SimulationThread.stopTime(0);
			if (SimulationThread.currentSimulationThread().idle(millis*1000000+nanos, true, true))
				throw new InterruptedException();
		} finally {
			SimulationThread.startTime(0);
		}
    }
	
	public static void yield() {
		SimulationThread.stopTime(0);
		SimulationThread.startTime(0);
	}
    
    protected Object clone() throws CloneNotSupportedException {
    	throw new CloneNotSupportedException();
    }
    
    public void start() {
    	checkSimulation(true);
    	this.simulationThread.start();
    }
    
    public void interrupt() {
    	this.simulationThread.fake_interrupt();
    }

    public boolean isInterrupted() {
    	return this.simulationThread.getInterruptedStatus(false);    	
    }
    
    public static boolean interrupted() {
		return SimulationThread.currentSimulationThread().getInterruptedStatus(true);    	
    }
    
    public void join() throws InterruptedException {
    	this.simulationThread.fake_join(0);
    }

    public void join(long timeout) throws InterruptedException {
    	this.simulationThread.fake_join(0*1000000);
    }

    public void join(long millis, long nanos) throws InterruptedException {
    	this.simulationThread.fake_join(0*1000000+nanos);
    }

    public final void setDaemon(boolean on) {
    	daemon = on;
    }

    public boolean isDaemon() {
    	return daemon;
    }
    
    public ClassLoader getContextClassLoader() {
    	return SimulationThread.currentThread().getContextClassLoader();
    }
    
    public long getId() {
    	return SimulationThread.currentSimulationThread().fake_getId();
    }
    
    public java.lang.Thread.State getState() {
    	return SimulationThread.currentThread().getState();
    }

    public StackTraceElement[] getStackTrace() {
    	return simulationThread.fake_getStackTrace();
    }
    
    public final void setName(String name) {
    	this.name = name;
		simulationThread.setName(name+"@"+simulationThread.getProcess().getNetwork().getLocalAddress().getHostAddress());
    }
    
    public final String getName() {
    	return this.name;
    }
    
    public final void setPriority(int newPriority) {
    	this.priority = newPriority;
    }
    
    public final int getPriority() {
        return priority;
    }
    
    public final boolean isAlive() {
    	return this.simulationThread.fake_isAlive();
    }
        
    public String toString() {
    	return "Thread["+name+"]";
    }
    
    public void run() {}
    
    public static void dumpStack() {
    	java.lang.Thread.dumpStack();
    }
    
    public ThreadGroup getThreadGroup() {
    	return group;
    }

    public SimulationThread getSimulationThread() {
    	return simulationThread;
    }
    
    public void simulationStart() {
    	this.simulationStart(0);
    }
    
    public void simulationStart(long delay) {
    	checkSimulation(false);
    	simulationThread.simulationStart(delay);
    }
    
    public void setUncaughtExceptionHandler(UncaughtExceptionHandler handler) {
    	this.handler = handler;
    }
    
    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
    	return this.handler;
    }
    
    public static interface UncaughtExceptionHandler {
    	public void uncaughtException(Thread thread, Throwable throwable);
    }
}
