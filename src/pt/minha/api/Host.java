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

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;

import pt.minha.kernel.instrument.ClassConfig;
import pt.minha.kernel.instrument.InstrumentationLoader;
import pt.minha.kernel.simulation.Timeline;
import pt.minha.models.global.HostInterface;
import pt.minha.models.global.net.Network;

/**
 * A host running within the Minha simulated world. 
 */
public class Host {
	private World world;
	InstrumentationLoader loader;
	HostInterface impl;

	Host(World w, ClassConfig cc, Timeline timeline, String ip, Network network) throws SimulationException {
		world = w;

		loader=new InstrumentationLoader(cc);
		
		try {
			Class<?> clz = loader.loadClass("pt.minha.models.local.HostImpl");
			impl = (HostInterface)clz.getDeclaredConstructor(timeline.getClass(), String.class, Network.class).newInstance(timeline, ip, network);
		} catch(Exception e) {
			throw new SimulationException(e);
		}
	}
	
	/** 
	 * Run an application in this simulated host.
	 * 
	 * @param main name of class with main method
	 * @param args arguments to main method
	 * @throws SimulationException cannot find or launch desired class 
	 */
	public void launch(long delay, String main, String[] args) throws SimulationException {
		impl.launch(delay, main, args);
	}
	
	/** 
	 * Create an entry point to inject arbitrary invocations within a host.
	 * This avoids the main method and can be used for multiple invocations.
	 * 
	 * @param clz the interface (global) of the class to be created
	 * @param impl the name of the class (translated) to be created within the host
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws SecurityException 
	 * @throws IllegalArgumentException 
	 */
	public <T> Entry<T> createEntry(Class<T> intf, String impl) throws IllegalArgumentException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		return new Entry<T>(this, intf, impl);
	}
	
	/**
	 * Get simulated host address. This is the address that the simulated
	 * host will use in the container, when networking with other simulated
	 * hosts.
	 * @return host address
	 */
	public InetAddress getAddress() {
		return impl.getAddress();
	}
	
	/**
	 * Gets the simulated world container.
	 * @return the simulation world for this host
	 */
	public World getContainer() {
		return world;
	}
}
