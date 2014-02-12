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

package pt.minha.kernel.simulation;

import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A simulation timeline is a sequence of related events. Execution of
 * events within the same timeline is strictly totally ordered and 
 * accurately timed, even with parallel execution. Event exection across
 * different timelines is causally ordered and might be subject to
 * spurious delays.
 */
public class Timeline {
	
	private Lock lock = new ReentrantLock(false);
	private NavigableSet<Event> schedule = new ConcurrentSkipListSet<Event>();
	private long doorstop = Long.MAX_VALUE;
		
	private long now = 0;
	private Scheduler sched;
	
	Timeline(Scheduler sched) {
		this.sched = sched;
	}
	
	void schedule(Event e, long delay) {
		if (e.time != -1) {
			assert(e.getTimeline() == this);
			schedule.remove(e);
		}
				
		e.time = now+delay;
		e.getTimeline().schedule.add(e);
	}

	/**
	 * Get a reference to the scheduler that owns this timeline.
	 * 
	 * @return the scheduler
	 */
	public Scheduler getScheduler() {
		return sched;
	}
	
	/**
	 * Current time in this timeline. This is safe only when called
	 * from events in this timeline.
	 * 
	 * @return current simulation time
	 */
	public long getTime() {
		return now;
	}
	
	/**
	 * True if no events are scheduled in this timeline. The simulation
	 * is stopped when all timelines are quiescent.
	 * 
	 * @return true if no events remain
	 */
	public boolean isQuiescent() {
		return schedule.isEmpty();
	}
	
	long baseline() {
		try {
			if (!schedule.isEmpty())
				return schedule.first().time;
		} catch(NoSuchElementException e) {
			// fall through
		}
		return doorstop;
	}
		
	boolean run() {
		
		if (!lock.tryLock())
			return false;
		
		while(!schedule.isEmpty()) {
			Event next = schedule.first();
			if (next.time >= sched.lease.get())
				break;
			if (next.time > now)
				now = next.time;
			doorstop = now;
			schedule.remove(next);
			next.execute();
		}

		sched.in.incrementAndGet();
		doorstop = Long.MAX_VALUE;
		sched.out.incrementAndGet();
		
		lock.unlock();
		
		return true;
	}
	
	public static double toSeconds(long time) {
		return ((double)time)/1e9;
	}
}
