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

/**
 * Provides an exit out of a managed process. This is the only
 * safe way to invoke methods outside the system, e.g. to
 * communicate with a global overseer. Note that the proxy 
 * is single threaded and at any given time will be in a single
 * timing and mode. 
 */
public interface Exit<T> extends Milestone {

	/**
	 * Set CPU overhead before and after invocation. This can be invoked
	 * from outside or from inside the container. The value persists for
	 * all subsequent invocations. This is used for all invocations. The 
	 * callee can within the invocation override only the second value,
	 * to be incurred after the invocation.
	 * 
	 * @param before CPU overhead in nanoseconds
	 * @param after CPU overhead in nanoseconds
	 * @return the object itself for chaining invocations
	 */
	public Exit<T> overhead(long before, long after);

	/**
	 * Set delay that is incurred by the simulation with the
	 * invocation. This is imposed only on synchronous invocations. The
	 * callee can override this delay within the invocation.
	 * 
	 * @param delay delay in nanoseconds
	 * @return the object itself for chaining invocations
	 */
	public Exit<T> delay(long delay);

	/**
	 * Set invocation mode to asynchronous and return the proxy. The returned
	 * result and possible exceptions will be discarded. Driver code should
	 * thus carefully catch all exceptions, otherwise they will be hidden.
	 * 
	 * @return the proxy set for asynchronous invocations
	 */
	public T report();

	/**
	 * Set the invocation mode to synchronous and return the proxy.
	 * The system will be stopped while the invocation lasts and
	 * the external driver code is free to schedule further asynchronous
	 * entries. Synchronous entries are however not allowed.
	 *  
	 * @return the proxy set for synchronous invocations
	 */
	public T callback();

}