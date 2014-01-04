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
	 * @param usage amount of time used in the resource
	 * @return target time for scheduling event
	 */
	public long use(long usage) {
		if (used < timeline.getTime())
			used = timeline.getTime();
		used += usage;
		return used;
	}

	/**
	 * Use the resource conditionally. The resource is used only if
	 * it does not exceed the maximum queuing delay. 
	 * 
	 * @param max maximum queuing delay tolerated
	 * @param usage amount of time used in the resource
	 * @return target time for scheduling event, -1 if queue is full
	 */
	public long useConditionally(long max, long usage) {
		if (used < timeline.getTime())
			used = timeline.getTime();
		if (used+usage-timeline.getTime() > max)
			return -1;
		used += usage;
		return used;
	}
}
