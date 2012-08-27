/*
 * Minha.pt: middleware testing platform.
 * Copyright (c) 2011-2012, Universidade do Minho.
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

import pt.minha.models.local.HostImpl;
import pt.minha.models.local.lang.SimulationThread;

public class Thread extends Object implements Runnable {
	private final SimulationThread simulationThread;
	private String name;
	private int priority = NORM_PRIORITY;
	private ThreadGroup group;
	private boolean daemon;
	
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

	public Thread(Runnable runnable, String name) {
		this(null, runnable, name);
	}

	public Thread(ThreadGroup group, Runnable runnable, String name) {
		checkSimulation(true);
		if (name!=null)
			this.setName(name);
		if (group==null)
			this.group = Thread.currentThread().getThreadGroup();
		if (runnable==null)
			runnable = this;
		this.simulationThread = new SimulationThread(true, null, runnable, this);		
	}
	
	public Thread(ThreadGroup group, Runnable runnable, String name, long stackSize) {
		this(group, runnable, name);
	}
	
	public Thread(HostImpl host, Runnable runnable) {
		checkSimulation(false);
		this.simulationThread = new SimulationThread(true, host, runnable, this);
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
		SimulationThread.stopTime(0);
		SimulationThread.currentSimulationThread().idle(millis*1000000+nanos);
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
    	this.simulationThread.interrupt();
    }
    
    public void join() throws InterruptedException {
    	this.simulationThread.fake_join();
    }
    
    public final void setDaemon(boolean on) {
    	daemon = on;
    	if (on)
    		simulationThread.getHost().removeThread(simulationThread);
    	else
    		simulationThread.getHost().addThread(simulationThread);
    }

    public boolean isDaemon() {
    	return daemon;
    }
    
    public ClassLoader getContextClassLoader() {
    	return SimulationThread.currentThread().getContextClassLoader();
    }
    
    public long getId() {
    	return SimulationThread.currentThread().getId();
    }
    
    public java.lang.Thread.State getState() {
    	return SimulationThread.currentThread().getState();
    }
    
    public final void setName(String name) {
    	this.name = name;
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
    
    public static final boolean interrupted() {
    	// FIXME Implement thread interruption
    	return false;
    }
    
    public String toString() {
    	return name+"-"+simulationThread.toString();
    }
    
    public void run() {}
    
    public static void dumpStack() {
    	java.lang.Thread.dumpStack();
    }
    
    public ThreadGroup getThreadGroup() {
    	return group;
    }

    public void simulationStart() {
    	this.simulationStart(0);
    }
    
    public void simulationStart(long delay) {
    	checkSimulation(false);
    	simulationThread.simulationStart(delay);
    }
}
