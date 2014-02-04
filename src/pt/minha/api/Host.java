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

package pt.minha.api;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import pt.minha.kernel.instrument.ClassConfig;
import pt.minha.kernel.simulation.Resource;
import pt.minha.kernel.simulation.Timeline;
import pt.minha.models.global.net.Network;
import pt.minha.models.global.net.NetworkStack;

/**
 * A simulated host running within the simulation container. A simulated
 * host has a shared CPU and network stacks, with an unique IP, for all
 * processes.
 */
public class Host implements Closeable {
	private List<Process> procs = new ArrayList<Process>();
	World world;
	private NetworkStack network;
	private Resource cpu;
	private ClassConfig cc;
	private boolean closed;
	
	Host(World world, ClassConfig cc, Timeline timeline, String ip, Network network) throws SimulationException{
		this.world = world;
		this.cc = cc;
		try {
			this.network = new NetworkStack(timeline, ip, network);
			this.cpu = new Resource(timeline, this.network.getLocalAddress().getHostAddress());
		} catch (UnknownHostException e) {
			throw new SimulationException(e);
		}
	}
	
	/**
	 * Create a host with automatically assigned IP address. System properties
	 * are set from the host.
	 * 
	 * @return a host instance
	 * @throws SimulationException
	 */
	public Process createProcess() throws SimulationException {
		Process proc = new Process(this, cc, network, cpu, null);
		procs.add(proc);
		return proc;
	}
	
	/**
	 * Create a host with automatically assigned IP address and custom
	 * system properties.
	 * 
	 * @param props system properties for the simulated process
	 * @return a host instance
	 * @throws SimulationException
	 */
	public Process createProcess(Properties props) throws SimulationException {
		Process proc = new Process(this, cc, network, cpu, props);
		procs.add(proc);
		return proc;
	}
	
	/**
	 * Get processes in this simulation container.
	 * @return host collection
	 */
	public Collection<Process> getProcesses() {
		return Collections.unmodifiableCollection(procs);
	}
	
	/**
	 * Get simulated host address. This is the address that the simulated
	 * host will use in the container, when networking with other simulated
	 * hosts.
	 * @return host address
	 */
	public InetAddress getAddress() {
		return network.getLocalAddress();
	}
	
	/**
	 * Returns an unique name for this host.
	 * @return a unique identifier of this host
	 */
	public String getName() {
		return getAddress().getHostAddress();
	}
	
	/**
	 * Gets the simulated world container.
	 * @return the simulation world for this host
	 */
	public World getContainer() {
		return world;
	}

	@Override
	public void close() throws IOException {
		if (closed)
			return;
		closed = true;
		ArrayList<Process> l = new ArrayList<Process>(procs);
		for(Process p: l)
			p.close();
		world.removeHost(this);
	}

	void removeProcess(Process process) {
		procs.remove(process);
	}
}
