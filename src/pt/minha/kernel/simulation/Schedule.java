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

public class Schedule {
	volatile long simulationTime;
	Usage usage;
	private long latest;
	
	private PriorityQueue<Event> events = new PriorityQueue<Event>();
	private Processor proc;

	public Schedule() {
		proc = new Processor(this);
		usage = new Usage(newTimeline(), 1000000000, "simulation", 1, "events/s", 1); 
	}
	
	public Timeline newTimeline() {
		return new Timeline(this);
	}

	public long getTime() {
		return latest;
	}

	public void returnFromRun() {
		this.simulationTime = 1;
	}
	
	synchronized void add(Event event, long time) {
		if (event.time != -1)
			events.remove(event);
		event.time = time;
		events.add(event);
	}

	synchronized Event next() {
		Event next = events.peek();
		
		if (next == null)
			return null;
		
		if (next.time > simulationTime)
			return null;
		
		events.poll();

		latest = next.time;
		next.getTimeline().now = next.time;
		next.time = -1;
		
		return next;
	}

	public boolean run(long limit) {
		if (limit == 0)
			limit = Long.MAX_VALUE;
		simulationTime = limit;
		
		proc.run();
		
		return !events.isEmpty();
	}
}