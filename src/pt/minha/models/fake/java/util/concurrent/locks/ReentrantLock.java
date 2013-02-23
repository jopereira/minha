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

package pt.minha.models.fake.java.util.concurrent.locks;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import pt.minha.kernel.simulation.Event;
import pt.minha.models.local.lang.SimulationThread;

public class ReentrantLock implements Lock {
	private List<Event> waitingOnLock = new ArrayList<Event>();
	private int busy;
	private SimulationThread holder;

	@Override
	public void lock() {
		if (busy!=0) {
			SimulationThread current = SimulationThread.currentSimulationThread(); 

			if (holder == current) {
				busy++;
				return;
			}
			
			try {
				SimulationThread.stopTime(0);
			
				while(holder!=null) {
					waitingOnLock.add(current.getWakeup());
					current.pause();
				}
				holder=current;
				busy++;
	
			} finally {
				SimulationThread.startTime(0);
			}
		} else {
			holder=SimulationThread.currentSimulationThread();
			busy++;			
		}
	}

	@Override
	public void unlock() {
		
		busy--;
		if (busy>0)
			return;

		holder=null;
		if (waitingOnLock.isEmpty())
			return;
			
		try {
			SimulationThread.stopTime(0);

			if (!waitingOnLock.isEmpty())
				waitingOnLock.remove(0).schedule(0);
		
		} finally {
			SimulationThread.startTime(0);			
		}
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {
		if (busy!=0) {
			SimulationThread current = SimulationThread.currentSimulationThread(); 

			if (holder == current) {
				busy++;
				return;
			}
			
			try {
				SimulationThread.stopTime(0);
			
				boolean interrupted = current.getInterruptedStatus(true);
				while(!interrupted && holder!=null) {
					waitingOnLock.add(current.getWakeup());
					interrupted = current.pauseInterruptibly(true,true);
				}
				if (interrupted) {
					waitingOnLock.remove(current.getWakeup());
					throw new InterruptedException();
				}
				
				holder=current;
				busy++;
	
			} finally {
				SimulationThread.startTime(0);
			}
		} else {
			holder=SimulationThread.currentSimulationThread();
			busy++;			
		}
	}

	@Override
	public boolean tryLock() {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not supported method");
		//return false;
	}

	@Override
	public boolean tryLock(long time, TimeUnit unit)
			throws InterruptedException {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not supported method");
		//return false;
	}

	@Override
	public Condition newCondition() {
		return new ConditionImpl();
	}

	private class ConditionImpl implements Condition {
		private List<Event> waitingOnCond = new ArrayList<Event>();

		@Override
		public void await() throws InterruptedException {
			awaitNanos(0);
		}

		@Override
		public void awaitUninterruptibly() {
			awaitNanosUnint(0);
		}

		private long awaitNanosUnint(long nanosTimeout) {
			long before=0, after=0;
			
			try {
				SimulationThread.stopTime(0);
				
				SimulationThread current = SimulationThread.currentSimulationThread(); 
				int storedBusy = busy;
	
				before = current.getTimeline().getTime();
				
				// unlock
				holder=null;
				busy=0;
				if (!waitingOnLock.isEmpty())
					waitingOnLock.remove(0).schedule(0);
	
				// sleep
				waitingOnCond.add(current.getWakeup());
				if (nanosTimeout>0) current.getWakeup().schedule(nanosTimeout);
				current.pause();
				
				// cleanup
				waitingOnCond.remove(current.getWakeup());
	
				//  lock
				while(holder!=null) {
					waitingOnLock.add(current.getWakeup());
					current.pause();
				}
				
				// wakeup
				holder=current;
				busy=storedBusy;
	
				after = current.getTimeline().getTime();
				
			} finally {
				SimulationThread.startTime(0);
			}			
			return nanosTimeout-(after-before);
		}

		@Override
		public void signal() {
			try {
				SimulationThread.stopTime(0);
			
				if (!waitingOnCond.isEmpty())
					waitingOnCond.remove(0).schedule(0);
			
			} finally {
				SimulationThread.startTime(0);			
			}
		}

		@Override
		public void signalAll() {
			try {
				SimulationThread.stopTime(0);
			
				for (Event e: waitingOnCond)
					e.schedule(0);
				
				waitingOnCond.clear();
			
			} finally {
				SimulationThread.startTime(0);			
			}
		}
		
		@Override
		public long awaitNanos(long nanosTimeout) throws InterruptedException {
			long before=0, after=0;
			
			try {
				SimulationThread.stopTime(0);
				
				SimulationThread current = SimulationThread.currentSimulationThread(); 
				int storedBusy = busy;
	
				before = current.getTimeline().getTime();
				
				if (current.getInterruptedStatus(true))
					throw new InterruptedException();
				
				// unlock
				holder=null;
				busy=0;
				if (!waitingOnLock.isEmpty())
					waitingOnLock.remove(0).schedule(0);
	
				// sleep
				waitingOnCond.add(current.getWakeup());
				if (nanosTimeout>0) current.getWakeup().schedule(nanosTimeout);
				current.pauseInterruptibly(true,false);
				
				// cleanup
				waitingOnCond.remove(current.getWakeup());
	
				//  lock
				while(holder!=null) {
					waitingOnLock.add(current.getWakeup());
					current.pause();
				}
				
				// wakeup
				holder=current;
				busy=storedBusy;
	
				after = current.getTimeline().getTime();
				
				if (current.getInterruptedStatus(true))
					throw new InterruptedException();
				
			} finally {
				SimulationThread.startTime(0);
			}			
			return nanosTimeout-(after-before);
		}

		@Override
		public boolean await(long time, TimeUnit unit) throws InterruptedException {
			return awaitNanos(unit.toNanos(time)) > 0;
		}

		@Override
		public boolean awaitUntil(Date deadline) throws InterruptedException {
			throw new RuntimeException("not implemented");
		}
	}
}
