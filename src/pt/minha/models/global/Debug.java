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

package pt.minha.models.global;

public class Debug {
	public static final void println(String arg) {
		System.out.println("sim.debug("+Thread.currentThread()+"): "+arg);
		System.out.flush();
	}
	
	public static final long realNanoTime() {
		return System.nanoTime();
	}
	
	public static final void dumpThreadStackTrace() {
		System.err.println();
		System.err.println("########################################################");
		java.util.Map<Thread,StackTraceElement[]> tst = Thread.getAllStackTraces();
		for (Thread t : tst.keySet()) {
			StackTraceElement[] st = tst.get(t);
			System.err.println(t.toString());
			for (int i=0; i<st.length; i++)
				System.err.println("\t"+st[i].toString());
			System.err.println();
		}
		System.err.println("Number of threads: "+tst.size());
		System.err.println("########################################################");
		System.err.println();
	}
}
