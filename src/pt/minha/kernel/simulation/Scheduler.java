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

public class Schedule implements Runnable {
		
	AtomicLong in = new AtomicLong(), out = new AtomicLong();
	AtomicLong lease = new AtomicLong();
	volatile long simulationTime;
	
	private long fuzzyness = 1000000;
	private int procs=2;
	
	private List<Timeline> timelines = new ArrayList<Timeline>();
	
	public Timeline newTimeline() {
		Timeline t = new Timeline(this);
		timelines.add(t);
		return t;
	}

	public long getTime() {
		return lease.get();
	}

	public void returnFromRun() {
		this.simulationTime = 1;
	}

	public void run() {
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
	
	public boolean run(long limit) {
		if (limit == 0)
			limit = Long.MAX_VALUE-fuzzyness;
		simulationTime = limit;

		Thread[] threads = new Thread[procs];
		for(int i = 0; i<threads.length; i++)
			threads[i] = new Thread(this);
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
}