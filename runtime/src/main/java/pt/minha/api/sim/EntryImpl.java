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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

import pt.minha.api.Entry;
import pt.minha.api.Process;
import pt.minha.models.global.EntryHandler;
import pt.minha.models.global.ResultHolder;

class EntryImpl<T> extends MilestoneImpl implements Entry<T> {
	private T proxy;
	private EntryHandler target;
	private ResultHolder last;
	
	private long time;
	private boolean async, relative;
	private ProcessImpl proc;
	
	@SuppressWarnings("unchecked")
	EntryImpl(ProcessImpl process, Class<T> intf, String impl) throws ClassNotFoundException, IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
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
					throw new SimulationDeadlockException();
				}
			}

		});
		
		this.target = proc.impl.createEntry(impl);
	}
	
	@Override
	public Entry<T> asap() {
		return afterNanos(0l);
	}

	@Override
	public Entry<T> after(long timeout, TimeUnit unit) {
		return afterNanos(unit.toNanos(timeout));
	}
	
	@Override
	public Entry<T> afterNanos(long nanosTimeout) {
		this.relative = true;
		this.time = nanosTimeout;
		return this;
	}

	@Override
	public Entry<T> at(long schedule, TimeUnit unit) {
		return atNanos(unit.toNanos(schedule));
	}
	
	@Override
	public Entry<T> atNanos(long nanosSchedule) {
		this.relative = false;
		this.time = nanosSchedule;
		return this;
	}

	@Override
	public T queue() {
		this.async = true;
		return proxy;
	}
	
	@Override
	public T call() {
		this.async = false;
		return proxy;
	}
	
	@Override
	public Object getResult() throws Throwable {
		ResultHolder r = last;
		last = null;
		return r.getResult();
	}
	
	@Override
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

	@Override
	public boolean isPending() {
		return last != null;
	}
}