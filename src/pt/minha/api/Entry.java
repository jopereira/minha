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

import pt.minha.models.global.EntryHandler;
import pt.minha.models.global.ResultHolder;

/**
 * Provides an entry into a simulated host. It creates an instance
 * of the desired type within the host and then handles invocations
 * in a simulated thread.  
 */
public class Entry<T> {
	private T proxy;
	private EntryHandler target;
	
	private long time;
	private boolean async;
	
	/** 
	 * Create a proxy to inject arbitrary invocations within a host.
	 * 
	 * @param host an host instance
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
	Entry(final Host host, Class<T> intf, String impl) throws ClassNotFoundException, IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		this.proxy = (T) Proxy.newProxyInstance(host.loader, new Class<?>[]{ intf }, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				
				host.world.acquire(!async);
				
				// Queue invocation
				ResultHolder result = new ResultHolder();
					
				target.invoke(time, method, args, result);
					
				if (async) {
					host.world.release(false);
					
					return result.getFakeResult(method.getReturnType());
				}
				host.world.runSimulation();
				
				host.world.release(true);

				return result.getResult();
			}

		});
		
		this.target = host.impl.createEntry(impl);
	}

	/**
	 * Return the proxy to perform an asynchronous invocation at the desired
	 * time. This can only be used on methods returning void and corresponds
	 * to scheduling a simulation event at the specified time.
	 *  
	 * @param time
	 * @return the proxy
	 */
	public T at(long time) {
		this.async = true;
		this.time = time;
		return proxy;
	}

	/**
	 * Return the proxy to perform a symchronous call at the desired time.
	 *  
	 * @return the proxy
	 */
	public T call(long time) {
		this.async = false;
		this.time = time;
		return proxy;
	}
}
