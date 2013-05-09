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

public class Timeline {
	
	volatile long now = 0;
	private Schedule sched;
	
	Timeline(Schedule sched) {
		this.sched = sched;
	}
	
	/**
	 * Schedule an event. The event time is computed using this timeline's current
	 * time and the delay, and then queued on the event's target timeline.
	 * 
	 * @param e event to be scheduled
	 * @param delay delay relative to current simulation time
	 */
	public void schedule(Event e, long delay) {
		sched.add(e, getTime()+delay);
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
}
