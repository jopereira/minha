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

package pt.minha.models.fake.java.lang;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import pt.minha.models.fake.java.util.concurrent.locks.ReentrantLock;

public class Object {
	private transient ReentrantLock lock;
	private transient Condition cond;
	
	private static Object cast(java.lang.Object obj) {
		try {
			if (!(obj instanceof java.lang.Class))
				return (Object) obj;
			java.lang.Class<?> clz = (java.lang.Class<?>) obj;
			Field f = clz.getField("_fake_class");
			return (Object) f.get(clz);
		} catch (Exception e) {
			if (obj.getClass().isArray())
				return stub(obj);
			throw new RuntimeException("Trying to synchronize on a native class "+obj.getClass());
		}
	}
	
	/*
	 * Quick and dirty fiz for brain-dead code that does synchronized(array).
	 * This currently leaks references. It can be solved by reference counting
	 * and keeping references only through waits and synchronized. 
	 */
	private static java.util.Map<java.lang.Object, Object> stubmap = new java.util.HashMap<java.lang.Object, Object>();
	private static Object stub(java.lang.Object obj) {
		Object stub = stubmap.get(obj);
		if (stub==null) {
			stub = new Object();
			stubmap.put(obj, stub);
		}
		return stub;
	}
	
	private void fake_enter() {
		try {
			lock.lock();
		} catch(NullPointerException e) {
			lock = new ReentrantLock();
			cond = lock.newCondition();
			lock.lock();
		}
	}
	public static final void _fake_enter(java.lang.Object _this) {
		cast(_this).fake_enter();
	}
	
	private void fake_leave() {
		lock.unlock();
	}
	public static final void _fake_leave(java.lang.Object _this) {
		cast(_this).fake_leave();
	}
	
	private void fake_wait() throws InterruptedException {
		cond.await();
	}
	public static final void _fake_wait(java.lang.Object _this) throws InterruptedException {
		cast(_this).fake_wait();
	}

	private void fake_wait(long timeout) throws InterruptedException {
		cond.await(timeout, TimeUnit.MILLISECONDS);
	}	
	public static final void _fake_wait(java.lang.Object _this, long timeout) throws InterruptedException {
		cast(_this).fake_wait(timeout);
	}

	private void fake_wait(long timeout, int nanos) throws InterruptedException {
		cond.await(timeout*1000000000+nanos, TimeUnit.NANOSECONDS);
	}
	public static final void _fake_wait(java.lang.Object _this, long timeout, int nanos) throws InterruptedException {
		cast(_this).fake_wait(timeout, nanos);
	}
	
	private void fake_notify() {
		cond.signal();
	}
	public static final void _fake_notify(java.lang.Object _this) {
		cast(_this).fake_notify();
	}

	public void fake_notifyAll() {
		cond.signalAll();
	}
	public static final void _fake_notifyAll(java.lang.Object _this) {
		cast(_this).fake_notifyAll();
	}
}
