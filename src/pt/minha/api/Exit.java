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

import java.lang.reflect.Method;

import pt.minha.models.global.ExitHandler;
import pt.minha.models.global.ResultHolder;

/**
 * Provides an exit out of a simulated host. This is the only
 * safe way to invoke methods outside the simulation, e.g. to
 * communicate with a global overseer.
 */
@Global
public class Exit<T> {
	private T proxy;
	private boolean async;
	
	Exit(final Host host, Class<T> intf, final T impl) {
				
		this.proxy = host.impl.createExit(intf, new ExitHandler() {
			
			@Override
			public boolean invoke(Method method, Object[] args, ResultHolder wakeup) {
				host.world.handleInvoke(impl, method, args, wakeup);
				return !async;
			}
		});
	}

	/**
	 * Return the proxy.
	 * 
	 * @return the proxy
	 */
	public T report() {
		async = true;
		return proxy;
	}
	
	/**
	 * Return the proxy.
	 *  
	 * @return the proxy
	 */
	public T callback() {
		async = false;
		return proxy;
	}
}
