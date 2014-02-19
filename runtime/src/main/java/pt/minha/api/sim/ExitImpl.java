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

package pt.minha.api.sim;

import java.lang.reflect.Method;

import pt.minha.api.Exit;
import pt.minha.models.global.ExitHandler;
import pt.minha.models.global.ResultHolder;

@Global
class ExitImpl<T> extends MilestoneImpl implements Exit<T> {
	private T proxy;
	private boolean async, waited, done;
	private long before, after, delay;
	
	ExitImpl(final ProcessImpl proc, Class<T> intf, final T impl) {
				
		this.proxy = proc.impl.createExit(intf, new ExitHandler() {
			
			@Override
			public boolean invoke(Method method, Object[] args, ResultHolder wakeup) {
				proc.host.world.handleInvoke(impl, method, args, wakeup);
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
	
	@Override
	public Exit<T> overhead(long before, long after) {
		this.before = before;
		this.after = after;
		return this;
	}

	@Override
	public Exit<T> delay(long delay) {
		this.delay = delay;
		return this;
	}

	@Override
	public T report() {
		async = true;
		return proxy;
	}
	
	@Override
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
