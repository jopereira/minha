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

package pt.minha.models.fake.java.util.concurrent.locks;

import pt.minha.models.local.lang.SimulationThread;

public class LockSupport {	
	public static void park() {
		parkNanos(null, 0l);
	}

	public static void park(Object blocker) {
		parkNanos(blocker, 0l);
	}

	public static void parkNanos(Object blocker, long timeout) {
		SimulationThread.currentSimulationThread().park(blocker, timeout);
	}
	
	public static void unpark(pt.minha.models.fake.java.lang.Thread thread) {
		SimulationThread.currentSimulationThread().unpark(thread);
	}
}
