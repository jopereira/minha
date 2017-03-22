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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import pt.minha.models.fake.java.io.FileDescriptor.SyncTarget;
import pt.minha.models.fake.java.nio.channels.FileChannel;
import pt.minha.models.global.disk.Storage;
import pt.minha.models.local.lang.SimulationThread;
import pt.minha.models.local.nio.FileChannelImpl;

public class FileOutputStream extends OutputStream {
	private java.io.FileOutputStream impl;
	private Storage storage;
	private long syncTarget;
	
	public FileOutputStream(String name, boolean append) throws FileNotFoundException {
		this.storage = SimulationThread.currentSimulationThread().getProcess().getStorage();
		this.impl = new java.io.FileOutputStream(storage.translate(name));
	}

	public FileOutputStream(String name) throws FileNotFoundException {
		this(name, false);
	}
	
	public FileOutputStream(File file, boolean append) throws FileNotFoundException {
		this(file.getAbsolutePath(), append);
	}

	public FileOutputStream(File file) throws FileNotFoundException {
        this(file, false);
    }
	
	public void write(int b) throws IOException {
		try {
			SimulationThread.stopTime(0);

			this.impl.write(b);

			writeDelay(1);
		} finally {
			SimulationThread.startTime(storage.getConfig().getIOOverhead(1));	
		}
	}
	
	public FileDescriptor getFD() throws IOException{
		SyncTarget st = new SyncTarget() {
			public long getSyncTarget() {
				return syncTarget;
			}
		};
		return new FileDescriptor(this.impl.getFD(), st);
	}
	
	public void close() throws IOException {
		this.impl.close();
	}
	
	private void writeDelay(int size) {
		SimulationThread current = SimulationThread.currentSimulationThread();
		
		long qdelay = storage.scheduleWrite(size);
		
		syncTarget = qdelay + current.getTimeline().getTime();

		qdelay = storage.scheduleCache(qdelay, size);

		if (qdelay > 0)
			current.idle(qdelay, false, false);			
	}

	public FileChannel getChannel(){
		return new FileChannelImpl(this.impl.getChannel());
	}
}
