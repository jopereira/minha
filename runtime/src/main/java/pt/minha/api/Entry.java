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

import java.util.concurrent.TimeUnit;

/**
 * Provides an entry into a host. It creates an instance
 * of the desired type within the host and then handles invocations
 * in a thread. This class is not thread-safe and should
 * not ever be reentered.  At any given time will be in a single
 * timing and mode.
 */
public interface Entry<T> extends Milestone {

	/**
	 * Set proxy for invocations to be scheduled right away, as
	 * soon as possible.
	 * 
	 * @return the entry itself, for chaining invocations
	 */
	public Entry<T> asap();

	/**
	 * Set proxy for invocations to be scheduled after a delay.
	 *
	 * @param timeout delay before scheduling
	 * @param unit time unit for delay
	 * @return the entry itself, for chaining invocations
	 */
	public Entry<T> after(long timeout, TimeUnit unit);

	/**
	 * Set proxy for invocations to be scheduled after a delay.
	 *
	 * @param nanosTimeout delay before scheduling
	 * @return the entry itself, for chaining invocations
	 */
	public Entry<T> afterNanos(long nanosTimeout);

	/**
	 * Set proxy for invocations to be scheduled at a specific
	 * instant in time. This defaults to asap(), if
	 * the instant is in the past.
	 *
	 * @param schedule delay before scheduling
	 * @param unit time unit for delay
	 * @return the entry itself, for chaining invocations
	 */
	public Entry<T> at(long schedule, TimeUnit unit);

	/**
	 * Set proxy for invocations to be scheduled at a specific
	 * instant in time. This defaults to asap(), if
	 * the instant is in the past.
	 *
	 * @param nanosSchedule delay before scheduling
	 * @return the entry itself, for chaining invocations
	 */
	public Entry<T> atNanos(long nanosSchedule);

	/**
	 * Return the proxy to perform an asynchronous invocation at the desired
	 * time. The result, as well as an exception, can be retrieved using
	 * {@link Entry#getResult()}. Note that this method should always be
	 * executed, even if the application doesn't care about the result or
	 * even if the method is void, otherwise, exceptions in target code
	 * might be hidden. 
	 *  
	 * @return the proxy
	 */
	public T queue();

	/**
	 * Return the proxy to perform a synchronous call at the desired time.
	 *  
	 * @return the proxy
	 */
	public T call();

	/**
	 * Retrieves result from the last asynchronous invocation, if any. This should
	 * always be invoked, in order not to hide exceptions.
	 * 
	 * @return result from the last queued invocation
	 * @throws Throwable exception from the last queued invocation
	 */
	public Object getResult() throws Throwable;

	/**
	 * Gets the containing process.
	 * @return a reference to the process
	 */
	public Process getProcess();
}