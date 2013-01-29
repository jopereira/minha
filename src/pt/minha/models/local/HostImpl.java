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

package pt.minha.models.local;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import pt.minha.api.SimulationException;
import pt.minha.kernel.simulation.Resource;
import pt.minha.kernel.simulation.Timeline;
import pt.minha.models.global.HostInterface;
import pt.minha.models.global.net.Network;
import pt.minha.models.global.net.NetworkStack;
import pt.minha.models.local.lang.SimulationThread;


public class HostImpl implements HostInterface {
	// CPU
	private Resource cpu;
	private Set<SimulationThread> threads = new HashSet<SimulationThread>();
	
	// Networking
	private NetworkStack network;
		
	public HostImpl(Timeline timeline, String host, Network network) throws UnknownHostException, FileNotFoundException {
		this.cpu = new Resource(timeline, host);
		this.network = new NetworkStack(timeline, host, network);
	}
	
	public Resource getCPU() {
		return cpu;
	}
	
	public Timeline getTimeline() {
		return cpu.getTimeline();
	}
	
	public NetworkStack getNetwork() {
		return network;
	}
	
	/* The following methods are supposed to be called outside simulation events,
	 * as such, must be synchronized. */
	
	public synchronized void addThread(SimulationThread simulationThread) {
		threads.add(simulationThread);
	}
	
	public synchronized void removeThread(SimulationThread simulationThread) {
		threads.remove(simulationThread);
	}
	
	public void stop() throws InterruptedException {
		while(true) {
			SimulationThread t = null;
			synchronized(this) {
				if (threads.isEmpty())
					break;
				t = threads.iterator().next();
			}
			t.fake_join();
		}
		cpu.stop();
	}

	@Override
	public void launch(long delay, final String main, final String[] args) throws SimulationException {
		new pt.minha.models.fake.java.lang.Thread(this, new Runnable() {
			public void run() {
				try {
					// Run main
					Class<?> claz=Class.forName(main);
					claz.getMethod("main", new String[0].getClass()).invoke(claz, (Object)args);
					
					// For for all remaining non-daemon threads to stop
					pt.minha.models.fake.java.lang.Thread.currentThread().setDaemon(true);
					stop();
            	} catch (InvocationTargetException e) {
        			e.getTargetException().printStackTrace();
                    System.exit(1);
            	} catch (Exception e) {
            		e.printStackTrace();
            		System.exit(1);
            	}
			}
		}).simulationStart(delay*1000000000);
		
	}	
}
