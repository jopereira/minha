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

/**
 * Provides an exit out of a simulated host. This is the only
 * safe way to invoke methods outside the simulation, e.g. to
 * communicate with a global overseer.
 */
@Global
public class Exit<T> {
	private T proxy;
	
	@SuppressWarnings("unchecked")
	Exit(final World w, Class<T> intf, final T impl) {
		this.proxy = (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[]{ intf }, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (!method.getReturnType().equals(Void.TYPE))
					throw new IllegalArgumentException("method with non-void return type: "+method.getName());

				w.handleInvoke(impl, method, args);
				return null;
			}
		});
	}

	/**
	 * Return the proxy.
	 *  
	 * @return the proxy
	 */
	public T getProxy() {
		return proxy;
	}
}
