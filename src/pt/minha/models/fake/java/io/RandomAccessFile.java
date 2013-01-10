package pt.minha.models.fake.java.io;

import java.io.FileNotFoundException;
import java.io.IOException;

import pt.minha.models.global.disk.Request;
import pt.minha.models.global.disk.RequestType;
import pt.minha.models.global.disk.Storage;
import pt.minha.models.local.lang.SimulationThread;

public class RandomAccessFile {
	
	private final java.io.RandomAccessFile impl;
	private final int streamHashCode;
	
	private final Storage storage;

	public RandomAccessFile(File file, String mode) throws FileNotFoundException{
		this.impl = new java.io.RandomAccessFile(file.getImpl(), mode);
		this.streamHashCode = this.impl.hashCode();
		this.storage = SimulationThread.currentSimulationThread().getProcess().getStorage();
	}
	
	public void seek(long pos) throws IOException{
		this.impl.seek(pos);
	}
	
	public void write(byte[] b, int off, int len) throws IOException {
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
	
	public void write(byte[] b) throws IOException{
		long cost = 0;
		
		try {
			SimulationThread.stopTime(0);

			Request r = new Request(RequestType.WRITE, b.length, this.streamHashCode, this.storage);
			
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
	
	public void write(int b) throws IOException{
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
	
	public void writeLong(long v) throws IOException{
		long cost = 0;
		
		try {
			SimulationThread.stopTime(0);

			Request r = new Request(RequestType.WRITE, 8, this.streamHashCode, this.storage);
			
			r.execute();
			
			while((cost = r.getCost()) == 0) {
				this.storage.addBlock(SimulationThread.currentSimulationThread().getWakeup(), this.streamHashCode);
				SimulationThread.currentSimulationThread().pause(false, false);
			}
			
			this.impl.writeLong(v);
		} finally {
			SimulationThread.startTime(cost);					
		}
	}
	
	public void writeInt(int v)  throws IOException{
		long cost = 0;
		
		try {
			SimulationThread.stopTime(0);

			Request r = new Request(RequestType.WRITE, 4, this.streamHashCode, this.storage);
			
			r.execute();
			
			while((cost = r.getCost()) == 0) {
				this.storage.addBlock(SimulationThread.currentSimulationThread().getWakeup(), this.streamHashCode);
				SimulationThread.currentSimulationThread().pause(false, false);
			}
			
			this.impl.writeInt(v);
		} finally {
			SimulationThread.startTime(cost);					
		}
	}
	
	public int read() throws IOException{
		long cost = 0;
		
		try {
			SimulationThread.stopTime(0);

			Request r = new Request(RequestType.READ, 4, this.streamHashCode, this.storage);
			
			r.execute();
			
			while((cost = r.getCost()) == 0) {
				this.storage.addBlock(SimulationThread.currentSimulationThread().getWakeup(), this.streamHashCode);
				SimulationThread.currentSimulationThread().pause(false, false);
			}
			
			return this.impl.read();
		} finally {
			SimulationThread.startTime(cost);					
		}
	}
	
	public int read(byte[] b) throws IOException{
		long cost = 0;
		
		try {
			SimulationThread.stopTime(0);

			Request r = new Request(RequestType.READ, b.length, this.streamHashCode, this.storage);
			
			r.execute();
			
			while((cost = r.getCost()) == 0) {
				this.storage.addBlock(SimulationThread.currentSimulationThread().getWakeup(), this.streamHashCode);
				SimulationThread.currentSimulationThread().pause(false, false);
			}
			
			return this.impl.read(b);
		} finally {
			SimulationThread.startTime(cost);					
		}
	}
	
	public int read(byte[] b, int off, int len) throws IOException{
		long cost = 0;
		
		try {
			SimulationThread.stopTime(0);

			Request r = new Request(RequestType.READ, len, this.streamHashCode, this.storage);
			
			r.execute();
			
			while((cost = r.getCost()) == 0) {
				this.storage.addBlock(SimulationThread.currentSimulationThread().getWakeup(), this.streamHashCode);
				SimulationThread.currentSimulationThread().pause(false, false);
			}
			
			return this.impl.read(b, off, len);
		} finally {
			SimulationThread.startTime(cost);					
		}
	}
	
	public void close() throws IOException{
		this.impl.close();
	}
	
}
