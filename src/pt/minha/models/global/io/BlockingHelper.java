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

package pt.minha.models.global.io;

import java.util.HashSet;
import java.util.Set;

import pt.minha.kernel.simulation.Event;

/**
 * Manages waiting on I/O.
 * 
 * @author jop
 */
public abstract class BlockingHelper {
	private Set<Event> waiting = new HashSet<Event>();
	
	/**
	 * Test if ready for I/O operation.
	 * 
	 * @return true if will not block
	 */
	public abstract boolean isReady();
	
	/**
	 * Shloud be called by derived classes when becoming ready.
	 */
	public void wakeup() {
		for(Event ev: waiting)
			ev.schedule(0);
		waiting.clear();
	}
	
	/**
	 * Queue an event to schedule when ready.
	 * 
	 * @param ev the event
	 */
	public void queue(Event ev) {
		waiting.add(ev);
	}
	
	/**
	 * Cancel a previously queued event.
	 * 
	 * @param ev the event
	 */
	public void cancel(Event ev) {
		waiting.remove(ev);
	}	
}