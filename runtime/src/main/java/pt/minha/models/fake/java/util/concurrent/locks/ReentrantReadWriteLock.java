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

package pt.minha.models.fake.java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

// FIXME: Does not accept concurrent readers
public class ReentrantReadWriteLock implements ReadWriteLock {
	private ReentrantLock lock;

	public ReentrantReadWriteLock() {
		this(false);
	}
	
	public ReentrantReadWriteLock(boolean fair) {
		lock = new ReentrantLock(fair);
	}
	
	@Override
	public ReadLock readLock() {
		return new ReadLock();
	}

	@Override
	public WriteLock writeLock() {
		return new WriteLock();
	}
	
	public class ReadLock implements Lock {

		@Override
		public void lock() {
			lock.lock();
		}

		@Override
		public void lockInterruptibly() throws InterruptedException {
			lock.lockInterruptibly();
		}

		@Override
		public boolean tryLock() {
			return lock.tryLock();
		}

		@Override
		public boolean tryLock(long time, TimeUnit unit)
				throws InterruptedException {
			return lock.tryLock(time, unit);
		}

		@Override
		public void unlock() {
			lock.unlock();			
		}

		@Override
		public Condition newCondition() {
			return lock.newCondition();
		}
	}
	
	public class WriteLock implements Lock {

		@Override
		public void lock() {
			lock.lock();
		}

		@Override
		public void lockInterruptibly() throws InterruptedException {
			lock.lockInterruptibly();
		}

		@Override
		public boolean tryLock() {
			return lock.tryLock();
		}

		@Override
		public boolean tryLock(long time, TimeUnit unit)
				throws InterruptedException {
			return lock.tryLock(time, unit);
		}

		@Override
		public void unlock() {
			lock.unlock();			
		}

		@Override
		public Condition newCondition() {
			return lock.newCondition();
		}
	}
}
