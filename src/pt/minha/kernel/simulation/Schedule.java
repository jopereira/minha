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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Schedule {
	volatile long simulationTime;
	private int idle, procs=2;
	private Processor processors[];
	private long base, now, fuzzyness = 1000000;
	private List<Timeline> timelines = new ArrayList<Timeline>();
	private Iterator<Timeline> next;
	private Timeline nextline;
	volatile boolean running;
	
	public Schedule() {
		for(int i = 0; i<4; i++)
			timelines.add(new Timeline(this));
		next = timelines.iterator();
	}
	
	public Timeline newTimeline() {
		if (!next.hasNext())
			next = timelines.iterator();
		return next.next();
	}

	public long getTime() {
		return now;
	}

	public void returnFromRun() {
		this.simulationTime = 1;
	}
	
	private void updateLimits() {
		base = Long.MAX_VALUE;
		for(Processor p: processors)
			if (p.base <= base)
				base = p.base;
		
		long nexttime = Long.MAX_VALUE;
		nextline = null;
		for(Timeline t: timelines) {
			if (!t.busy && t.earliest() < nexttime) { 
				nexttime = t.earliest();
				nextline = t;
			}
		}
		
		// Do this without overflow:
		// if (nexttime >= base+fuzzyness)
		if (nexttime-fuzzyness >= base)
			nextline = null;
	}
	
	synchronized Timeline next(Processor proc, Timeline target) {
		if (target != null)
			target.release();
	
		proc.base = Long.MAX_VALUE;
		idle++;
		
		updateLimits();
		
		while(nextline == null && idle<procs)
			try {
				wait();
				if (nextline == null)
					updateLimits();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		

		
		if (nextline == null || nextline.earliest() >= simulationTime) {
			notifyAll();
			return null;
		}

		idle--;
		
		assert(!nextline.busy);

		Timeline result = nextline;
		nextline = null;

		proc.base = result.earliest();
		// This is needed when all procs are idle
		if (proc.base < base)
			base = proc.base;
		now = base+fuzzyness;
		
		result.acquire(base+fuzzyness);
		
		notify();
		
		return result;
	}

	public boolean run(long limit) {
		if (limit == 0)
			limit = Long.MAX_VALUE;
		simulationTime = limit;

		for(Timeline t: timelines)
			t.release();
		
		running = true;
		idle = 0;

		Thread[] threads = new Thread[procs];
		processors = new Processor[procs];
		for(int i = 0; i<threads.length; i++) {
			processors[i] = new Processor(this);
			threads[i] = new Thread(processors[i]);
		}
		for(int i = 0; i<threads.length; i++)
			threads[i].start();
		for(int i = 0; i<threads.length; i++)
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		running = false;
		
		return nextline!=null;
	}
}