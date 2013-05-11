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

import java.util.ArrayList;
import java.util.Collection;
import java.util.PriorityQueue;

public class Timeline {
	
	long now = 0;
	private long lease;
	private Schedule sched;
	private Collection<Event> outgoing = new ArrayList<Event>();
	
	Usage usage;
	boolean busy;
	PriorityQueue<Event> workingset = new PriorityQueue<Event>(10, new Event.LocalOrder());
	PriorityQueue<Event> schedule = new PriorityQueue<Event>(10, new Event.GlobalOrder());

	Timeline(Schedule sched) {
		this.sched = sched;
		usage = new Usage(this, 1000000000, "simulation", 1, "events/s", 1); 	
	}
	
	/**
	 * Schedule an event. The event time is computed using this timeline's current
	 * time and the delay, and then queued on the event's target timeline.
	 * 
	 * @param e event to be scheduled
	 * @param delay delay relative to current simulation time
	 */
	public void schedule(Event e, long delay) {
		assert(isLocal(e) || !isFuture(e));
		if (isPresent(e))
			workingset.remove(e);
		else if (isOutgoing(e))
			outgoing.remove(e);
		
		e.time = now+delay;
		
		assert(!sched.running || busy);
		if (isLocal(e) && isPresent(e) && busy)
			workingset.add(e);
		else
			outgoing.add(e);
	}

	public long getTime() {
		return now;
	}
	
	public void returnFromRun() {
		sched.returnFromRun();
	}
	
	public static double toSeconds(long time) {
		return ((double)time)/1e9;
	}

	// This is executed in the context of Schedule synchronization,
	// that owns the local schedule queue
	void acquire(long lease) {
		busy = true;
		this.lease = lease;
		Event next = schedule.peek();
		while(next!=null && next.stime < lease) {
			next = schedule.poll();
			
			assert(isFuture(next));
			
			if (next.time != next.stime) {
				next.stime = next.time;
				if (isFuture(next))
					schedule.add(next);
			} else {
				next.stime = -1;
				workingset.add(next);
			}
			
			next = schedule.peek();
		}
	}
	
	// This is executed in the context of the Schedule synchronization,
	// that owns all local schedule queues
	void release() {
		busy = false;
		for(Event e: outgoing) {
			assert(e.time >= 0);
			
			if (isFuture(e))
				e.getTimeline().schedule.remove(e);

			e.stime = e.time;
			
			if (isFuture(e))
				e.getTimeline().schedule.add(e);
		}
		outgoing.clear();
	}
	
	long earliest() {
		return schedule.isEmpty()?Long.MAX_VALUE:schedule.peek().stime;
	}
	
	private boolean isPresent(Event e) {
		return e.time >= 0 && e.getTimeline()==this && e.time < lease; 
	}
	
	private boolean isOutgoing(Event e) {
		return e.time >= 0 && (e.getTimeline()!=this || e.time > lease);
	}
	
	private boolean isFuture(Event e) {
		return e.stime >= 0;
	}
		
	private boolean isLocal(Event e) {
		return e.getTimeline()==this;
	}
}
