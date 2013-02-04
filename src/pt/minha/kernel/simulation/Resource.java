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

import java.net.UnknownHostException;
import java.util.LinkedList;


public class Resource extends Event {
	private boolean busy, stopped;
	private LinkedList<Event> queue;
	private Event wakeup;
	
	private Usage usage;
	
	public Resource(Timeline timeline, String host) throws UnknownHostException {
		super(timeline);
		
		this.queue = new LinkedList<Event>();
		
		usage = new Usage(timeline, 1000000000, "cpu."+host, 1e-7, "%", 0); 
	}
		
	public synchronized void run() {
		wakeup.schedule(0);
		wakeup = null;
		busy = false;
				
		restart();
	}
	
	private synchronized void restart() {
		if (busy)
			return;
		
		if (queue.isEmpty())
			return;
		
		busy = true;
				
		Event event = queue.removeFirst();
		event.schedule(0);
	}

	public synchronized void acquire(Event event) {
		if (stopped)
			return;
		
		queue.add(event);
		restart();
	}

	public synchronized void release(long delta, Event wakeup) {
		usage.using(delta);
		
		this.wakeup = wakeup;
		this.schedule(delta);
	}
	
	public synchronized void stop() throws InterruptedException {
		stopped = true;
		queue.clear();
	}
}
