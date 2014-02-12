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
import java.io.InputStream;

import pt.minha.models.global.disk.Storage;
import pt.minha.models.local.lang.SimulationThread;

public class FileInputStream extends InputStream {
	private final java.io.FileInputStream impl;	
	private final Storage storage;

	public FileInputStream(String name) throws FileNotFoundException {
		this.storage = SimulationThread.currentSimulationThread().getProcess().getStorage();
		this.impl = new java.io.FileInputStream(storage.translate(name));
    }

	public FileInputStream(File file) throws FileNotFoundException {
		this(file.getAbsolutePath());
	}
	
	public int read() throws IOException {
		try {
			SimulationThread.stopTime(0);

			readDelay(1);		
			
			return this.impl.read();
		} finally {
			SimulationThread.startTime(storage.getConfig().getIOOverhead(1));
		}
	}
	
	public int read(byte[] b) throws IOException {
		return this.read(b, 0, b.length);
	}
	
	public int read(byte[] b, int off, int len) throws IOException{
		try {
			SimulationThread.stopTime(0);

			readDelay(len);
			
			return this.impl.read(b, off, len);
		} finally {
			SimulationThread.startTime(storage.getConfig().getIOOverhead(len));					
		}
	}
	
	public FileDescriptor getFD() throws IOException{
		return new FileDescriptor(this.impl.getFD(), null);
	}
	
	public void close() throws IOException {
		this.impl.close();
	}
	
	private void readDelay(int size) {
		SimulationThread current = SimulationThread.currentSimulationThread();
		
		long qdelay = storage.scheduleRead(size);
		
		current.idle(qdelay, false, false);		
	}
}
