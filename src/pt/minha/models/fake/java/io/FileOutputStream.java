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

import pt.minha.models.global.disk.Request;
import pt.minha.models.global.disk.RequestType;
import pt.minha.models.global.disk.Storage;
import pt.minha.models.local.lang.SimulationThread;

public class FileOutputStream extends OutputStream {
	private final java.io.FileOutputStream impl;
	private final int streamHashCode;

	
	private final Storage storage;
	
	/**
	 * Main constructor of the class
	 * @param name
	 * @param append
	 * @throws FileNotFoundException
	 */
	public FileOutputStream(String name, boolean append) throws FileNotFoundException {
		String pathAux = SimulationThread.currentSimulationThread().getHost().getName();
		this.createTree(pathAux);
		this.impl = new java.io.FileOutputStream(pathAux + File.separator + name, append);
		this.streamHashCode = this.impl.hashCode();
		this.storage = SimulationThread.currentSimulationThread().getProcess().getStorage();
	}

	/**
	 * Secondary constructor of the class
	 * @param name
	 * @throws FileNotFoundException
	 */
	public FileOutputStream(String name) throws FileNotFoundException {
		this(name, false);
	}
	
	/**
	 * Secondary constructor of the class
	 * @param file
	 * @param append
	 * @throws FileNotFoundException
	 */
	public FileOutputStream(File file, boolean append) throws FileNotFoundException {
		this(file.getAbsolutePath(), append);
	}

	/**
	 * Secondary constructor of the class
	 * @param file
	 * @throws FileNotFoundException
	 */
	public FileOutputStream(File file) throws FileNotFoundException {
        this(file, false);
    }
	
	/**
	 * Main write method
	 * @param b
	 */
	public void write(int b) throws IOException {
		long cost = 0;
		
		try {
			SimulationThread.stopTime(0);

			Request r = new Request(RequestType.WRITE, 4, this.streamHashCode, this.storage);
			
			r.execute();
			
			while((cost = r.getCost()) == 0) {
				this.storage.addBlock(SimulationThread.currentSimulationThread().getWakeup(), this.streamHashCode);
				SimulationThread.currentSimulationThread().pause(false, false);
			}
			
			this.impl.write(b);
		} finally {
			SimulationThread.startTime(cost);					
		}
	}
	
	/**
	 * Main write method
	 * @param b[]
	 * @param off
	 * @param len
	 */
	public void write(byte b[], int off, int len) throws IOException {
		long cost = 0;
		
		try {
			SimulationThread.stopTime(0);

			Request r = new Request(RequestType.WRITE, len, this.streamHashCode, this.storage);
			
			r.execute();
			
			while((cost = r.getCost()) == 0) {
				this.storage.addBlock(SimulationThread.currentSimulationThread().getWakeup(), this.streamHashCode);
				SimulationThread.currentSimulationThread().pause(false, false);
			}
			
			this.impl.write(b, off, len);
		} finally {
			SimulationThread.startTime(cost);					
		}
	}
	
	/**
	 * Secondary write method
	 * @param b 
	 */
	public void write(byte b[]) throws IOException {
        this.write(b, 0, b.length);
    }
	
	public FileDescriptor getFD() throws IOException{
		return new FileDescriptor(this.impl.getFD(), this.streamHashCode, this.storage);
	}
	
	public void close() throws IOException {
		this.impl.close();
	}
	
	/**
	 * Function responsible for creation of the correct path in the tree
	 * @param dir ip of current host
	 */
	private void createTree(String dir){
		java.io.File newDir = new java.io.File(dir);
		if(!newDir.exists()) newDir.mkdir();
	}
}
