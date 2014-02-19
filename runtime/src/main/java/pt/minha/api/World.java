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
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import pt.minha.api.sim.Simulation;

/**
 * Interface to a distributed system container. To run a simulated distributed
 * system use the {@link Simulation} container implementation.
 */
public interface World extends Closeable {

	/**
	 * Create a host.
	 * 
	 * @param ip address for host, null for auto-assignment
	 * @return a host instance
	 * @throws ContainerException
	 */
	public Host createHost(String ip) throws ContainerException;

	/**
	 * Create a host with automatically assigned IP address.
	 * 
	 * @return a host instance
	 * @throws ContainerException
	 */
	public Host createHost() throws ContainerException;

	/**
	 * Get processes in this container.
	 * @return host collection
	 */
	public Collection<Host> getHosts();

	/**
	 * Utility method to quickly initialize a distributed system.
	 * This creates several hosts, each with a single process, each with 
	 * a single entry for a main method.
	 *  
	 * @param n number of hosts/processes to create.
	 * @return entry objects that can invoke main methods
	 */
	public Entry<Main>[] createEntries(int n) throws IllegalArgumentException,
			SecurityException, ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, ContainerException;

	/**
	 * Utility method to quickly initialize a distributed system.
	 * This creates several hosts, each with a single process, each with 
	 * a single entry for a user defined interface and implementation.
	 *  
	 * @param n number of hosts/processes to create.
	 * @return entry objects that can invoke the given interface
	 */
	public <T> Entry<T>[] createEntries(int n, Class<T> intf, String impl)
			throws IllegalArgumentException, SecurityException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, ContainerException;

	/**
	 * Utility method to quickly initialize exit proxies for a distributed system. 
	 * An exit proxy will be created for each process, all pointing
	 * to the same implementation.
	 * 
	 * @param entries entries in the processes where exits will be created
	 * @param intf interface for the proxy
	 * @param impl implementation object outside the container
	 * @return exit objects that can invoke the given interface
	 */
	public <T1, T2> Exit<T1>[] createExits(Entry<T2>[] entries, Class<T1> intf,
			T1 impl);

	/**
	 * Run the system until all events are processed. Warning: If
	 * the system has some activity that never
	 * terminates, this method will never return. The safe way to run a
	 * system is to use a timeout or a set of milestones.
	 * 
	 * @return time of latest event processed
	 * @throws InterruptedException
	 */
	public long run();

	/**
	 * Run the system until all events are processed or the time limit
	 * is reached. This method waits for all outstanding callback events to
	 * be processed.
	 * 
	 * @param timeout run only to the specified time limit
	 * @param unit time units for timeout parameter
	 * @return time of latest event processed
	 * @throws InterruptedException
	 */
	public long run(long timeout, TimeUnit unit);

	/**
	 * Run the system until all events are processed or the time limit
	 * is reached. This method waits for all outstanding callback events to
	 * be processed.
	 * 
	 * @param nanosTimeout run only to the specified time limit
	 * @return time of latest event processed
	 */
	public long runNanos(long nanosTimeout);

	/**
	 * Run the system until all events are processed or all milestones
	 * achieved. This method waits for all outstanding callback events to
	 * be processed.
	 * 
	 * @param milestones goals to be met
	 * @return time of latest event processed
	 */
	public long runAll(Milestone... milestones);
}