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

/**
 * Abstract finite resource. Usage is known a prior, independent of when the
 * resource actually is available. It cannot be used when usage depends
 * on state that changes while queueing. 
 */
public class SimpleResource {
	private long used;
	private Timeline timeline;
	
	public SimpleResource(Timeline timeline) {
		this.timeline = timeline;
	}
	
	/**
	 * Use the resource. This might lead to arbitrarily long queuing delay
	 * but the event will eventually get scheduled.
	 * 
	 * @param event the event to signal completion
	 * @param usage amount of time used in the resource
	 * @param delay additional delay after the resource has been used
	 */
	public void use(Event event, long usage, long delay) {
		if (used < timeline.getTime())
			used = timeline.getTime();
		used += usage;
		event.schedule(used+delay);
	}

	/**
	 * Use the resource conditionally. The event is scheduled and the
	 * resource used only if it does not exceed the maximum queuing
	 * delay. 
	 * 
	 * @param max maximum queuing delay tolerated
	 * @param event the event to signal completion
	 * @param usage amount of time used in the resource
	 * @param delay additional delay after the resource has been used
	 * @return true when within the delay bound 
	 */
	public boolean useConditionally(long max, Event event, long usage, long delay) {
		if (used < timeline.getTime())
			used = timeline.getTime();
		if (used+usage-timeline.getTime() > max)
			return false;
		used += usage;
		event.schedule(used+delay);
		return true;
	}
}
