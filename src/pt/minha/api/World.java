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

package pt.minha.api;

import java.io.InputStream;
import java.util.Properties;

import pt.minha.kernel.instrument.ClassConfig;
import pt.minha.kernel.simulation.Timeline;
import pt.minha.models.global.net.Network;
import pt.minha.models.global.net.NetworkConfig;

/**
 * This is the main entry point for Minha. It provides a method to create
 * simulated hosts and interact with the simulated world as whole.
 */
public class World {
	private ClassConfig cc;
	private NetworkConfig nc;
	private Timeline timeline;
	private Network network;
	
	public World(long simulationTime) throws Exception {		
		
		// load network calibration values
		Properties props = new Properties();
		props.load(ClassLoader.getSystemClassLoader().getResourceAsStream("default.network.properties"));			
		props = new Properties(props);
		InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("network.properties"); 
		if (is!=null)
			props.load(is);
		nc = new NetworkConfig(props);
		
		// load instrumentation properties
		props = new Properties();
		props.load(ClassLoader.getSystemClassLoader().getResourceAsStream("default.instrument.properties"));			
		props = new Properties(props);
		is = ClassLoader.getSystemClassLoader().getResourceAsStream("instrument.properties"); 
		if (is!=null)
			props.load(is);
		cc = new ClassConfig(props);
		timeline = new Timeline(simulationTime);
		
		network = new Network(timeline, nc);
	}
	
	/**
	 * Create a host.
	 * 
	 * @param ip address for host, null for auto-assignment
	 * @return a host instance
	 * @throws SimulationException
	 */
	public Host createHost(String ip) throws SimulationException {
		return new Host(cc, timeline, ip, network);
	}

	/**
	 * Create a host.
	 * 
	 * @return a host instance
	 * @throws SimulationException
	 */
	public Host createHost() throws SimulationException {
		return createHost(null);
	}
	
	public long run() {
		timeline.run();
		return timeline.getTime();
	}
}
