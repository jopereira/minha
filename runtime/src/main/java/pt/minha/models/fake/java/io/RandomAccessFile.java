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

import pt.minha.models.fake.java.io.FileDescriptor.SyncTarget;
import pt.minha.models.fake.java.nio.channels.FileChannel;
import pt.minha.models.global.disk.Storage;
import pt.minha.models.local.lang.SimulationThread;
import pt.minha.models.local.nio.FileChannelImpl;

public class RandomAccessFile {
	
	private java.io.RandomAccessFile impl;
	private Storage storage;
	private long syncTarget;
	private FileChannel channel;

	public RandomAccessFile(File file, String mode) throws FileNotFoundException{
		this.impl = new java.io.RandomAccessFile(file.getImpl(), mode);
		this.storage = SimulationThread.currentSimulationThread().getProcess().getStorage();
		this.channel = new FileChannelImpl();
	}
	
	public void seek(long pos) throws IOException{
		this.impl.seek(pos);
	}
	
	public void write(byte[] b, int off, int len) throws IOException {
		try {
			SimulationThread.stopTime(0);

			this.impl.write(b, off, len);

			writeDelay(len);
		} finally {
			SimulationThread.startTime(storage.getConfig().getIOOverhead(len));
		}
	}
	
	public void write(byte[] b) throws IOException{
		this.write(b, 0, b.length);
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
		
	public void writeLong(long v) throws IOException{
		try {
			SimulationThread.stopTime(0);

			this.impl.writeLong(v);

			writeDelay(8);
		} finally {
			SimulationThread.startTime(storage.getConfig().getIOOverhead(8));
		}
	}
	
	public void writeInt(int v)  throws IOException{
		try {
			SimulationThread.stopTime(0);

			this.impl.writeInt(v);

			writeDelay(4);
		} finally {
			SimulationThread.startTime(storage.getConfig().getIOOverhead(4));	
		}
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
		SyncTarget st = new SyncTarget() {
			public long getSyncTarget() {
				return syncTarget;
			}
		};
		return new FileDescriptor(this.impl.getFD(), st);
	}
	
	public FileChannel getChannel() {
		return channel;
	}
	
	public void close() throws IOException{
		this.impl.close();
	}
	
	private void readDelay(int size) {
		SimulationThread current = SimulationThread.currentSimulationThread();

		long qdelay = storage.scheduleRead(size) - current.getTimeline().getTime();
		
		current.idle(qdelay, false, false);		
	}
	
	private void writeDelay(int size) {
		SimulationThread current = SimulationThread.currentSimulationThread();
		
		syncTarget = storage.scheduleWrite(size);
		
		long qdelay = storage.scheduleCache(syncTarget - current.getTimeline().getTime(), size);

		if (qdelay > 0)
			current.idle(qdelay, false, false);			
	}
}
