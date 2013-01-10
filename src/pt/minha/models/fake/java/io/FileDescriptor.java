package pt.minha.models.fake.java.io;

import java.io.SyncFailedException;

import pt.minha.models.global.disk.Request;
import pt.minha.models.global.disk.RequestType;
import pt.minha.models.global.disk.Storage;
import pt.minha.models.local.lang.SimulationThread;

public class FileDescriptor {
	//FIXME: FileDescriptor final variables in, out, etc
	private final java.io.FileDescriptor impl;
	private final int streamHashCode;
	private final Storage storage;

	public FileDescriptor(java.io.FileDescriptor impl, int id, Storage st){
		this.impl = impl;
		this.streamHashCode = id;
		this.storage = st;
	}
	
	public void sync() throws SyncFailedException{
		while(this.storage.hasStream(this.streamHashCode)){
			this.storage.addBlock(SimulationThread.currentSimulationThread().getWakeup(), this.streamHashCode);
			SimulationThread.currentSimulationThread().pause(false, false);
		}
	}
	
	public boolean valid(){
		return this.impl.valid();
	}
}
