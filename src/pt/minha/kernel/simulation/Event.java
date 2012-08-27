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

package pt.minha.kernel.simulation;

public abstract class Event implements Runnable, Comparable<Event> {
	private Timeline timeline;
	long time;

	/**
	 * Create an event on a specific simulation time line.
	 * @param timeline
	 */
	public Event(Timeline timeline) {
		this.timeline = timeline;
		this.time = -1;
	}
	
	/**
	 * Utility method available while executing run().
	 * @return
	 */
	public Timeline getTimeline() {
		return timeline;
	}
		
	/**
	 * Event code. 
	 */
	public abstract void run();
	
	/**
	 * Schedule (or reschedule) event to a new deadline, as a delay on
	 * current time in the associated timeline.
	 * @param delta
	 */
	public void schedule(long delta) {
		if (delta<0)
			throw new RuntimeException("negative delay");
		
		cancel();
		time = timeline.getTime() + delta;
		timeline.add(this);
	}

	/**
	 * Cancel event. Does nothing if event is not scheduled.
	 */
	public void cancel() {
		if (time >= 0)
			timeline.remove(this);
		time = -1;
	}
	
	public int compareTo(Event other) {
		return Long.signum(time-other.time);
	}

	void execute() {
		time = -1;
		run();
	}
	
	public String toString() {
		return super.toString()+" at "+time;
	}
}
