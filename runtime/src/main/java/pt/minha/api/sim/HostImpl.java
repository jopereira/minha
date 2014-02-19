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

package pt.minha.api.sim;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import pt.minha.api.Host;
import pt.minha.api.Process;
import pt.minha.api.ContainerException;
import pt.minha.api.World;
import pt.minha.kernel.instrument.ClassConfig;
import pt.minha.kernel.simulation.Resource;
import pt.minha.kernel.simulation.Timeline;
import pt.minha.models.global.disk.Storage;
import pt.minha.models.global.net.Network;
import pt.minha.models.global.net.NetworkStack;

class HostImpl implements Host {
	private List<Process> procs = new ArrayList<Process>();
	Simulation world;
	private NetworkStack network;
	private Resource cpu;
	private Storage storage;
	private ClassConfig cc;
	private boolean closed;
	
	HostImpl(Simulation world, ClassConfig cc, Timeline timeline, String ip, Network network, Storage storage) throws ContainerException{
		this.world = world;
		this.cc = cc;
		try {
			this.network = new NetworkStack(timeline, ip, network);
			this.cpu = new Resource(timeline, this.network.getLocalAddress().getHostAddress());
			this.storage = storage;
		} catch (UnknownHostException e) {
			throw new ContainerException(e);
		}
	}
	
	@Override
	public Process createProcess() throws ContainerException {
		ProcessImpl proc = new ProcessImpl(this, cc, network, cpu, storage, null);
		procs.add(proc);
		return proc;
	}
	
	@Override
	public Process createProcess(Properties props) throws ContainerException {
		ProcessImpl proc = new ProcessImpl(this, cc, network, cpu, storage, props);
		procs.add(proc);
		return proc;
	}
	
	@Override
	public Collection<Process> getProcesses() {
		return Collections.unmodifiableCollection(procs);
	}
	
	@Override
	public InetAddress getAddress() {
		return network.getLocalAddress();
	}
	
	@Override
	public String getName() {
		return getAddress().getHostAddress();
	}
	
	@Override
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
