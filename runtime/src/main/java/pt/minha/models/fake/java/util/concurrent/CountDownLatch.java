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

package pt.minha.models.fake.java.util.concurrent;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import pt.minha.models.fake.java.util.concurrent.locks.ReentrantLock;

//FIXME: Should be moved, if we had AbstractQueuedSynchronizer
public class CountDownLatch {
	private Lock lock;
	private Condition cond;
	private int count;

	public CountDownLatch(int count) {
		this.lock = new ReentrantLock();
		this.cond = lock.newCondition();
		this.count = count;
	}
	
	public void await() throws InterruptedException {
		try {
			lock.lock();
			while(count>0)
				cond.await();
		} finally {
			lock.unlock();
		}
	}
	
	public void countDown() {
		try {
			lock.lock();
			if (count>0)
				count--;
			if (count == 0)
				cond.signalAll();
		} finally {
			lock.unlock();
		}
	}
}
