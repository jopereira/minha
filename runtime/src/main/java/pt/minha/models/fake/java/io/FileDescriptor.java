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

package pt.minha.models.fake.java.io;

import java.io.SyncFailedException;

import pt.minha.models.local.lang.SimulationThread;

public class FileDescriptor {
	private java.io.FileDescriptor impl;
	private SyncTarget st;

	interface SyncTarget {
		public long getSyncTarget();
	}
	
	public FileDescriptor() {
		this.impl = new java.io.FileDescriptor();
		this.st = null;
	}
	
	FileDescriptor(java.io.FileDescriptor impl, SyncTarget st){
		this.impl = impl;
		this.st = st;
	}
	
	public void sync() throws SyncFailedException {
		try {
			SimulationThread.stopTime(0);
			if (st == null)
				return;
			SimulationThread current = SimulationThread.currentSimulationThread();
			long syncDelay = st.getSyncTarget() - current.getTimeline().getTime();
			if (syncDelay > 0)
				if (current.idle(syncDelay, false, false))
					throw new SyncFailedException("thread interrupted");
		} finally {
			SimulationThread.startTime(0);
		}
	}
	
	public boolean valid(){
		try {
			SimulationThread.stopTime(0);
			return this.impl.valid();
		} finally {
			SimulationThread.startTime(0);
		}
	}
}
