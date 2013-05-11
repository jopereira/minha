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

public class Processor implements Runnable {
	private Schedule sched;
	long base;
	
	Processor(Schedule sched) {
		this.sched = sched;
	}
	
	public void run() {
		Timeline target = null;
		
		while(true) {
			target = sched.next(this, target);
			
			if (target == null)
				break;
			
			while(!target.workingset.isEmpty()) {
				Event next = target.workingset.poll();
				if (next.time == -1)
					continue;
				next.execute();
				target.usage.using(1);
			}
		}
	}
}
