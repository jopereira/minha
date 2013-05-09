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

import java.util.PriorityQueue;

public class Timeline {
	private Usage usage;
	
	private long now = 0;
	private PriorityQueue<Event> events = new PriorityQueue<Event>();
	
	public static double toSeconds(long time) {
		return ((double)time)/1e9;
	}
	
	private long simulationTime;

	public Timeline() {
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
		e.getTimeline().add(e, now+delay);
	}

	public synchronized long getTime() {
		return now;
	}

	synchronized void add(Event event, long time) {
		event.cancel();
		event.time = time;
		events.add(event);
	}

	synchronized void remove(Event event) {
		events.remove(event);
	}
	
	public synchronized void returnFromRun() {
		this.simulationTime = 1;
	}

	public boolean run(long limit) {
		synchronized(this) {
			this.simulationTime = limit;
		}
		
		while(!events.isEmpty()) {
			Event next = null;
			
			synchronized(this) {
				if (simulationTime>0 && now>simulationTime)
					break;
				
				next = events.poll();			
			
				if (next == null)
					break;
				
				if (next.time < now)
					throw new RuntimeException("FATAL: Time going backwards");
				
				now = next.time;			
			}
			next.execute();
			usage.using(1);
		}
		
		return !events.isEmpty();
	}
}
