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
public class Exit<T> extends Milestone {
	private T proxy;
	private boolean async, waited, done;
	private long before, after, delay;
	
	Exit(final Host host, Class<T> intf, final T impl) {
				
		this.proxy = host.impl.createExit(intf, new ExitHandler() {
			
			@Override
			public boolean invoke(Method method, Object[] args, ResultHolder wakeup) {
				host.world.handleInvoke(impl, method, args, wakeup);
				return !async;
			}
			
			@Override
			public long getOverheadBefore() {
				return before;
			}

			@Override
			public long getOverheadAfter() {
				return after;
			}

			@Override
			public long getDelay() {
				return delay;
			}

			@Override
			public boolean isMilestone() {
				if (waited) {
					done = true;
					return true;
				}
				return false;
			}
		});
	}
	
	/**
	 * Set CPU overhead before and after invocation. This can be invoked
	 * from outside or from inside the simulation. The value persists for
	 * all subsequent invocations. This is used for all invocations. The 
	 * callee can within the invocation override only the second value,
	 * to be incurred after the invocation.
	 * 
	 * @param before simulated CPU overhead in nanoseconds
	 * @param after simulated CPU overhead in nanoseconds
	 * @return the object itself for chaining invocations
	 */
	public Exit<T> overhead(long before, long after) {
		this.before = before;
		this.after = after;
		return this;
	}

	/**
	 * Set simulated delay that is incurred by the simulation with the
	 * invocation. This is imposed only on synchronous invocations. The
	 * callee can override this delay within the invocation.
	 * 
	 * @param before simulated delay in nanoseconds
	 * @return the object itself for chaining invocations
	 */
	public Exit<T> delay(long delay) {
		this.delay = delay;
		return this;
	}

	/**
	 * Set invocation mode to asynchronous and return the proxy.
	 * 
	 * @return the proxy
	 */
	public T report() {
		async = true;
		return proxy;
	}
	
	/**
	 * Set the invocation mode to synchronous and return the proxy.
	 *  
	 * @return the proxy
	 */
	public T callback() {
		async = false;
		return proxy;
	}

	@Override
	void setWaited() {
		waited = true;
	}

	@Override
	public boolean isComplete() {
		return done;
	}
}
