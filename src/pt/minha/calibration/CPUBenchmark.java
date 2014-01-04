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

package pt.minha.calibration;

import java.io.IOException;
import java.lang.management.ManagementFactory;

/**
 * Benchmark for CPU overhead in time virtualization.
 */
public class CPUBenchmark extends AbstractBenchmark {
	
	@Override
	public Object client() throws IOException, InterruptedException {
		double stuff = Math.random();

		for (int i = 0; i < 10; i++) {
			for(int j = 0; j < payload; j++)
				stuff = stuff * 11 / 7;
			System.nanoTime();
			ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
		}

		start();
		for (int i = 0; i < samples; i++) {
			for(int j = 0; j < payload; j++)
				stuff = stuff * 11 / 7;
			add(payload, System.nanoTime());
		}
		Result r = stop(true);
		
		System.out.println("defeat hostspot "+stuff);
			
		return r;
	}

	@Override
	public Object server() throws IOException {
		return null;
	}

	public String toString() {
		return "CPU virtualization "+super.toString();
	}
}
