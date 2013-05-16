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

package pt.minha.kernel.simulation;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Timeline {
	
	private Lock lock = new ReentrantLock(false);
	private NavigableSet<Event> schedule = new ConcurrentSkipListSet<Event>();
	private long doorstop = Long.MAX_VALUE;
		
	private long now = 0;
	private Schedule sched;
	
	Timeline(Schedule sched) {
		this.sched = sched;
	}
	
	/**
	 * Schedule an event. The event time is computed using this timeline's current
	 * time and the delay, and then queued on the event's target timeline.	 * 
	 * @param e event to be scheduled
	 * @param delay delay relative to current simulation time
	 */
	public void schedule(Event e, long delay) {
		if (e.time != -1) {
			assert(e.getTimeline() == this);
			schedule.remove(e);
		}
				
		e.time = now+delay;
		e.getTimeline().schedule.add(e);
	}

	public void returnFromRun() {
		sched.returnFromRun();
	}
	
	public long getTime() {
		return now;
	}
	
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
		
		Iterator<Event> i = schedule.iterator();
		
		while(i.hasNext()) {
			Event next = i.next();
			if (next.time >= sched.lease.get())
				break;
			if (next.time > now)
				now = next.time;
			doorstop = now;
			i.remove();
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
