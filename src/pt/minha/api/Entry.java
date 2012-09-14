/*
 * Minha.pt: middleware testing platform.
 * Copyright (c) 2011-2012, Universidade do Minho.
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

import pt.minha.models.global.HostInterface;
import pt.minha.models.local.Trampoline;

/**
 * Provides an entry into a simulated host. It creates an instance
 * of the desired type within the host and then handles invocations
 * in a simulated thread.  
 */
public class Entry<T> {
	private T proxy;
	private Class<?> claz; 
	private Trampoline target;
	private long time;
	
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
	Entry(Host host, Class<T> intf, String impl) throws ClassNotFoundException, IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		this.proxy = (T) Proxy.newProxyInstance(host.loader, new Class<?>[]{ intf }, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (!method.getReturnType().equals(Void.TYPE))
					throw new IllegalArgumentException("method with non-void return type: "+method.getName());

				target.invoke(time, method, args);
				
				return null;
			}
		});
		
		this.claz = host.loader.loadClass(Trampoline.Impl.class.getName());
		this.target = (Trampoline)claz.getDeclaredConstructor(HostInterface.class, String.class).newInstance(host.impl, impl);
	}
	
	/**
	 * Return the proxy to performa an invocation at the desired time.
	 *  
	 * @return the proxy
	 */
	public T at(long time) {
		this.time = time;
		return proxy;
	}
}
