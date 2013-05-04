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
import pt.minha.models.global.EntryInterface;
import pt.minha.models.global.net.Network;
import pt.minha.models.local.MainEntry;

/**
 * A process running within a simulated host. 
 */
public class Process {
	World world;
	InstrumentationLoader loader;
	EntryInterface impl;

	Process(World w, ClassConfig cc, Timeline timeline, String ip, Network network) throws SimulationException {
		world = w;

		loader=new InstrumentationLoader(cc);
		
		try {
			Class<?> clz = loader.loadClass("pt.minha.models.local.HostImpl");
			impl = (EntryInterface)clz.getDeclaredConstructor(timeline.getClass(), String.class, Network.class).newInstance(timeline, ip, network);
		} catch(Exception e) {
			throw new SimulationException(e);
		}
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
	 * Create an entry point start a program within a host.
	 * 
	 * @param impl the name of the class (translated) to be created within the host
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws SecurityException 
	 * @throws IllegalArgumentException 
	 */
	public Entry<Main> createEntry() throws IllegalArgumentException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		return new Entry<Main>(this, Main.class, MainEntry.class.getName());
	}
	
	/** 
	 * Create a proxy to receive callbacks from this simulation host. This proxy
	 * is to be handled to simulation code as a parameter to an entry method.
	 * 
	 * @param clz the interface (global) of the class to be created
	 * @param impl the object implementing the interface handling invocations
	 * @returns an exit proxy
	 */
	public <T> Exit<T> createExit(Class<T> intf, T impl) {
		return new Exit<T>(this, intf, impl);
	}
	
	// FIXME: the following belong to host, not process

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
