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

/**
 * A process running within a host. 
 */
public interface Process extends Closeable {

	/** 
	 * Create an entry point to inject arbitrary invocations within a process.
	 * This avoids the main method and can be used for multiple invocations.
	 * 
	 * @param intf the interface (global) of the class to be created
	 * @param impl the name of the class (translated) to be created within the process
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws SecurityException 
	 * @throws IllegalArgumentException 
	 */
	public <T> Entry<T> createEntry(Class<T> intf, String impl)
			throws IllegalArgumentException, SecurityException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException;

	/** 
	 * Create an entry point start a program within a process.
	 * 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws SecurityException 
	 * @throws IllegalArgumentException 
	 */
	public Entry<Main> createEntry() throws IllegalArgumentException,
			SecurityException, ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException;

	/** 
	 * Create a proxy to receive callbacks from this process. This proxy
	 * is to be handed to target code as a parameter to an entry method.
	 * 
	 * @param intf the interface (global) of the class to be created
	 * @param impl the object implementing the interface handling invocations
	 * @return an exit proxy
	 */
	public <T> Exit<T> createExit(Class<T> intf, T impl);

	/**
	 * Gets the host container.
	 * @return the container for this host
	 */
	public Host getHost();

}