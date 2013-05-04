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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

import pt.minha.models.global.EntryHandler;
import pt.minha.models.global.ResultHolder;

/**
 * Provides an entry into a simulated host. It creates an instance
 * of the desired type within the host and then handles invocations
 * in a simulated thread. This class is not thread-safe and should
 * not ever be reentered.  At any given time will be in a single
 * timing and mode.
 */
public class Entry<T> extends Milestone {
	private T proxy;
	private EntryHandler target;
	private ResultHolder last;
	
	private long time;
	private boolean async, relative;
	private Process proc;
	
	/** 
	 * Create a proxy to inject arbitrary invocations within a host.
	 * 
	 * @param proc an host instance
	 * @param clz the interface (global) of the class to be created
	 * @param impl the name of the class (translated) to be created within the host
	 * @param time true if the invocation is done in real-time
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws SecurityException 
	 * @throws IllegalArgumentException 
	 */
	@SuppressWarnings("unchecked")
	Entry(Process process, Class<T> intf, String impl) throws ClassNotFoundException, IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		this.proc = process;
		this.proxy = (T) Proxy.newProxyInstance(proc.loader, new Class<?>[]{ intf }, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				
				proc.host.world.acquire(!async);
				
				// Queue invocation
				ResultHolder result = new ResultHolder(method);
					
				target.invoke(time, relative, method, args, result);
					
				if (async) {
					if (last != null)
						last.setIgnored();
					
					last = result;
					
					proc.host.world.release(false);
					
					return result.getFakeResult();
				}
				proc.host.world.runSimulation(0);
				
				proc.host.world.release(true);
				
				if (result.isComplete())
					return result.getResult();
				else {
					result.getFakeResult();
					throw new InterruptedException();
				}
			}

		});
		
		this.target = proc.impl.createEntry(impl);
	}
	
	/**
	 * Set proxy for invocations to be scheduled right away, as
	 * soon as possible.
	 * 
	 * @return the entry itself, for chaining invocations
	 */
	public Entry<T> asap() {
		return afterNanos(0l);
	}

	/**
	 * Set proxy for invocations to be scheduled after a delay.
	 *
	 * @param timeout simulation delay before scheduling
	 * @param unit time unit for simulation delay
	 * @return the entry itself, for chaining invocations
	 */
	public Entry<T> after(long timeout, TimeUnit unit) {
		return afterNanos(unit.toNanos(timeout));
	}
	
	/**
	 * Set proxy for invocations to be scheduled after a delay.
	 *
	 * @param nanosTimeout simulation delay before scheduling
	 * @return the entry itself, for chaining invocations
	 */
	public Entry<T> afterNanos(long nanosTimeout) {
		this.relative = true;
		this.time = nanosTimeout;
		return this;
	}

	/**
	 * Set proxy for invocations to be scheduled at a specific
	 * instant in simulation time. This defaults to asap(), if
	 * the instant is in the past.
	 *
	 * @param schedule simulation delay before scheduling
	 * @param unit time unit for simulation delay
	 * @return the entry itself, for chaining invocations
	 */
	public Entry<T> at(long schedule, TimeUnit unit) {
		return atNanos(unit.toNanos(schedule));
	}
	
	/**
	 * Set proxy for invocations to be scheduled at a specific
	 * instant in simulation time. This defaults to asap(), if
	 * the instant is in the past.
	 *
	 * @param nanosSchedule simulation delay before scheduling
	 * @return the entry itself, for chaining invocations
	 */
	public Entry<T> atNanos(long nanosSchedule) {
		this.relative = false;
		this.time = nanosSchedule;
		return this;
	}

	/**
	 * Return the proxy to perform an asynchronous invocation at the desired
	 * time. The result, as well as an exception, can be retrieved using
	 * {@link Entry#getResult()}. Note that this method should always be
	 * executed, even if the application doesn't care about the result or
	 * even if the method is void, otherwise, exceptions in simulation code
	 * might be hidden. 
	 *  
	 * @return the proxy
	 */
	public T queue() {
		this.async = true;
		return proxy;
	}
	
	/**
	 * Return the proxy to perform a synchronous call at the desired time.
	 *  
	 * @return the proxy
	 */
	public T call() {
		this.async = false;
		return proxy;
	}
	
	/**
	 * Retrieves result from the last asynchronous invocation, if any. This should
	 * always be invoked, in order not to hide exceptions.
	 * 
	 * @return result from the last queued invocation
	 * @throws Throwable exception from the last queued invocation
	 */
	public Object getResult() throws Throwable {
		ResultHolder r = last;
		last = null;
		return r.getResult();
	}
	
	/**
	 * Gets the containing simulation process.
	 * @return a reference to the simulation process
	 */
	public Process getProcess() {
		return proc;
	}

	@Override
	void setWaited() {
		last.setSync();
	}

	@Override
	public boolean isComplete() {
		return last.isComplete();
	}
}