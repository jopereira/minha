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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Event-driven simulation scheduler. This is the main simulation entry
 * point, that includes the simulation main loop.
 */
public class Scheduler implements Runnable {
		
	AtomicLong in = new AtomicLong(), out = new AtomicLong();
	AtomicLong lease = new AtomicLong();
	private volatile long simulationTime;
	private int next = 0;
	
	private long fuzzyness;
	private int procs, numlines;	
	
	public Scheduler(int procs, int numlines, long fuzzyness) {
		this.procs = procs;
		this.numlines = numlines;
		this.fuzzyness = fuzzyness;
	}

	private List<Timeline> timelines = new ArrayList<Timeline>();
	
	/**
	 * Create a new simulation timeline. 
	 * @return the new timeline
	 */
	public Timeline createTimeline() {
		if (numlines<1 || timelines.size()<numlines) {
			next++;
			Timeline t = new Timeline(this);
			timelines.add(t);
			return t;
		} else
			return timelines.get(next++%timelines.size());
	}

	/**
	 * Current simulation time. This should be used only when
	 * the simulation is stopped.
	 * 
	 * @return current simulation time
	 */
	public long getTime() {
		return lease.get();
	}

	/**
	 * Quit the simulation. There might be a short delay before the
	 * simulation is actually stopped, so some events might be run
	 * after this method is invoked. This can safely be invoked from
	 * any timeline or external thread.
	 */
	public void stop() {
		this.simulationTime = 1;
	}

	private void execute() {
		int base = 0;

		while(true) {
			long min = Long.MAX_VALUE;
			long e = out.get();

			int i;
			for(i=0; i<timelines.size(); i++) {
				Timeline t = timelines.get((i+base)%timelines.size());
				
				long l = t.baseline();

				if (l<min)
					min = l;

				if (l<lease.get() && t.run())
					break;
			}
			base = i;
			
			if (min >= simulationTime)
				break;
			
			if (in.get() == e) {
				long prev = lease.get();
				min += fuzzyness;
				while(prev < min && !lease.compareAndSet(prev, min)) 
					prev = lease.get();
			}
		}
	}
	
	/**
	 * Run the simulation until the upper time limit is reached.
	 * The simulation will also stop when no events remain
	 * or {@link #stop()} is called.
	 * 
	 * @param limit maximum simulation time, 0 to run forever 
	 */
	public boolean run(long limit) {
		if (limit == 0)
			limit = Long.MAX_VALUE-fuzzyness;
		simulationTime = limit;
		
		int effprocs = procs;
		if (procs>timelines.size()/2) {
			effprocs = timelines.size()/2;
			if (effprocs == 0) effprocs = 1;
		}

		Thread[] threads = new Thread[procs];
		for(int i = 0; i<threads.length; i++)
			threads[i] = new Thread() {
				public void run() {
					execute();
				}
			};
		for(int i = 0; i<threads.length; i++)
			threads[i].start();
		for(int i = 0; i<threads.length; i++)
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		boolean quiescent = true;
		for(Timeline t: timelines)
			quiescent &= t.isQuiescent();
		return !quiescent;
	}
	
	/**
	 * Run the simulation. The simulation stops when no events remain
	 * or {@link #stop()} is called.
	 */
	public void run() {
		run(0l);
	}
}