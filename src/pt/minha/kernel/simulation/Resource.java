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

import java.net.UnknownHostException;
import java.util.LinkedList;

import pt.minha.kernel.log.Logger;
import pt.minha.kernel.log.SimpleLoggerLog4j;
import pt.minha.kernel.util.PropertiesLoader;

public class Resource extends Event {
	private String name;
	private boolean busy, stopped;
	private LinkedList<Event> queue;
	private Event wakeup;

	private static final long GRANULARITY = 1000000000;
	private long free_since;
	private long ti, tu;
	
	private Logger logger;
	private static boolean cpu_log_enabled = false;
	static {
		try {
			PropertiesLoader conf = new PropertiesLoader(Logger.LOG_CONF_FILENAME);
			cpu_log_enabled = Boolean.parseBoolean(conf.getProperty("resource.cpu.log"));
		} catch (Exception e) {
			// log will be disabled
		}
	}
	
	public Resource(Timeline timeline, String host) throws UnknownHostException {
		super(timeline);
		
		this.queue = new LinkedList<Event>();
		this.name = host;
		
		if ( cpu_log_enabled )
			this.logger = new SimpleLoggerLog4j("log/cpu-"+host+".log");
	}
		
	public synchronized void run() {
		wakeup.schedule(0);
		wakeup = null;
		busy = false;
		
		if ( cpu_log_enabled ) {
			free_since = getTimeline().getTime();
			if (tu + ti > GRANULARITY) {
				this.logger.debug(name+" "+getTimeline().getTime()+" "+tu+" "+ti);
				tu=0;
				ti=0;
			}
		}
		
		restart();
	}
	
	private synchronized void restart() {
		if (busy)
			return;
		
		if (queue.isEmpty())
			return;
		
		busy = true;
		
		if ( cpu_log_enabled )
			ti += getTimeline().getTime() - free_since;
		
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
		if ( cpu_log_enabled )
			tu += delta;
		
		this.wakeup = wakeup;
		this.schedule(delta);
	}
	
	public synchronized void stop() throws InterruptedException {
		stopped = true;
		queue.clear();
	}
}
