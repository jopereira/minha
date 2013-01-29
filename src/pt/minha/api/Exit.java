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
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * Create an exit out of a simulated host. This is the only
 * safe way to invoke methods outside the simulation, e.g. to
 * communicate with a global overseer.
 */
@Global
public class Exit<T> {
	private T proxy;
	private T target;
	private List<Invocation> queue = new ArrayList<Invocation>();
	
	/** 
	 * Create a proxy to inject arbitrary invocations within a host.
	 * 
	 * @param host an host instance
	 * @param clz the interface (global) of the class to be created
	 * @param impl the name of the class (translated) to be created within the host
	 * @param time true if the invocation is done in real-time
	 */
	@SuppressWarnings("unchecked")
	public Exit(Class<T> intf, T impl) {
		this.proxy = (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[]{ intf }, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (!method.getReturnType().equals(Void.TYPE))
					throw new IllegalArgumentException("method with non-void return type: "+method.getName());

				handleInvoke(method, args);
				return null;
			}
		});
		this.target = impl;
		new Thread() {
			public void run() {
				globalThread();
			}
		}.start();
	}
	
	private static class Invocation {
		public Method method;
		public Object[] args;
		
		public Invocation(Method method, Object[] args) {
			this.method = method;
			this.args = args;
		}		
	};
	
	private synchronized void handleInvoke(Method method, Object[] args) {
		queue.add(new Invocation(method, args));
		notifyAll();		
	}
	
	private synchronized Invocation next() throws InterruptedException {
		while(queue.isEmpty())
			wait();
		return queue.remove(0);
	}

	/**
	 * Return the proxy.
	 *  
	 * @return the proxy
	 */
	public T getProxy() {
		return proxy;
	}
	
	private void globalThread() {
		while(true) {
			try {
				Invocation i = next();
				
				i.method.invoke(target, i.args);
			} catch (Exception e) {
				// FIXME: should stop simulation?
				e.printStackTrace();
			}
		}
	}
}
