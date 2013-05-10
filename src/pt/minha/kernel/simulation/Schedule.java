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
import java.util.List;

public class Schedule {
	volatile long simulationTime;
	Usage usage;
	private int idle, procs=1;
	private Processor processors[];
	private long base, fuzzyness = 100000;
	private List<Timeline> timelines = new ArrayList<Timeline>();
	private Timeline nextline;
	
	public Schedule() {
		usage = new Usage(newTimeline(), 1000000000, "simulation", 1, "events/s", 1); 
	}
	
	public Timeline newTimeline() {
		Timeline t = new Timeline(this);
		timelines.add(t);
		return t;
	}

	public long getTime() {
		return base;
	}

	public void returnFromRun() {
		this.simulationTime = 1;
	}
	
	private void updateLimits() {
		base = Long.MAX_VALUE-2*fuzzyness;
		for(Processor p: processors)
			if (p.base < base)
				base = p.base;
		
		long nexttime = Long.MAX_VALUE;
		nextline = null;
		for(Timeline t: timelines) {
			if (!t.busy && t.earliest() < nexttime) { 
				nexttime = t.earliest();
				nextline = t;
			}
		}
	}
	
	synchronized Timeline next(Processor proc, Timeline target) {
		if (target != null)
			target.release();
	
		proc.base = Long.MAX_VALUE-2*fuzzyness;
		idle++;
		
		updateLimits();
		notify();
				
		while((nextline == null && idle<procs) || (nextline != null && nextline.earliest() > base+fuzzyness))
			try {
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		

		idle--;
		
		if (nextline == null || nextline.earliest() >= simulationTime) {
			notifyAll();
			return null;
		}
	
		proc.base = nextline.earliest();
		if (proc.base < base)
			base = proc.base;
		nextline.acquire(base+fuzzyness);
		
		return nextline;
	}

	public boolean run(long limit) {
		if (limit == 0)
			limit = Long.MAX_VALUE-3*fuzzyness;
		simulationTime = limit;

		for(Timeline t: timelines)
			t.release();

		Thread[] threads = new Thread[procs];
		processors = new Processor[procs];
		for(int i = 0; i<threads.length; i++) {
			processors[i] = new Processor(this);
			threads[i] = new Thread(processors[i]);
			threads[i].start();
		}
		
		for(int i = 0; i<threads.length; i++)
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		return nextline!=null;
	}
}