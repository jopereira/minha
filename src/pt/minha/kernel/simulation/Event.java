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
	 * Reschedule the event. Can only be called from the event's target
	 * timeline or when the simulation is stopped.
	 * 
	 * @param delay time delay in simulated nanoseconds
	 */
	public void schedule(long delay) {
		timeline.schedule(this, delay);
	}
	
	/**
	 * Schedule the event using a (possibly) different timeline as 
	 * a reference. This should be used whenever there is no
	 * guarantee that the event's target timeline is the same as
	 * the currently running.
	 * 
	 * @param delay time delay in simulated nanoseconds
	 */
	public void scheduleFrom(Timeline base, long delay) {
		base.schedule(this, delay);
	}
	
	void execute() {
		assert(time >= 0);
		
		time = -1;
		run();
	}

	/**
	 * Total order of events, sorted first by time and second by
	 * object id. Assumes that Object.hashCode() is unique, a least
	 * w.h.p. for events found concurrently within the same queue, thus
	 * providing an unique id.
	 */
	@Override
	public int compareTo(Event other) {
		if (time != other.time)
			return Long.signum(time-other.time);
		return Integer.signum(hashCode()-other.hashCode());
	}

	public String toString() {
		return super.toString()+"@["+time+"ns]";
	}
}
