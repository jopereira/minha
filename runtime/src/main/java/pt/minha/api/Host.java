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
import java.net.InetAddress;
import java.util.Collection;
import java.util.Properties;

/**
 * A host running within the container. A
 * host has a shared CPU and network stacks, with an unique IP, for all
 * processes.
 */
public interface Host extends Closeable {

	/**
	 * Create a host with automatically assigned IP address. System properties
	 * are set from the host.
	 * 
	 * @return a host instance
	 * @throws ContainerException
	 */
	public Process createProcess() throws ContainerException;

	/**
	 * Create a host with automatically assigned IP address and custom
	 * system properties.
	 * 
	 * @param props system properties for the process
	 * @return a host instance
	 * @throws ContainerException
	 */
	public Process createProcess(Properties props) throws ContainerException;

	/**
	 * Get processes in this container.
	 * @return host collection
	 */
	public Collection<Process> getProcesses();

	/**
	 * Get host address. This is the address that the
	 * host will use in the container, when networking with other
	 * hosts.
	 * @return host address
	 */
	public InetAddress getAddress();

	/**
	 * Returns an unique name for this host.
	 * @return a unique identifier of this host
	 */
	public String getName();

	/**
	 * Gets the world container.
	 * @return the container for this host
	 */
	public World getContainer();

}