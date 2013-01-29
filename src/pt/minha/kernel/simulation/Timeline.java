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

public class Timeline implements Runnable {
	private long now = 0;
	private PriorityQueue<Event> events = new PriorityQueue<Event>();
	
	private long simulationTime;

	public Timeline(long simulationTime) {
		this.simulationTime = simulationTime*1000000000;
	}
	
	public void schedule(Event e) {
		if (e.time < now)
			throw new RuntimeException("scheduling backwards");
		events.add(e);
	}

	public long getTime() {
		return now;
	}

	void add(Event event) {
		events.add(event);
	}

	void remove(Event event) {
		events.remove(event);
	}
	
	public void exit(int status) {
		this.simulationTime = 1;
	}

	@Override
	public void run() {
		while(!events.isEmpty()) {
			if (simulationTime>0 && now>simulationTime)
				break;
			
			Event next = events.poll();
			
			if (next == null)
				break;
			
			if (next.time < now)
				throw new RuntimeException("FATAL: Time going backwards");
			
			now = next.time;			
			next.execute();
		}
	}
}
