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

package pt.minha.test.model.lang;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;

import mockit.Expectations;
import mockit.Mocked;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import pt.minha.api.Entry;
import pt.minha.api.Exit;
import pt.minha.api.Process;
import pt.minha.api.World;
import pt.minha.api.sim.Global;
import pt.minha.api.sim.Simulation;

public class SynchronizedTest {
	private World world;
	private Process proc;

	@BeforeClass
	public void init() throws Exception {
		world = new Simulation();
		proc = world.createHost().createProcess();
	}
	
	@AfterClass
	public void cleanup() throws Exception {
		world.close();
	}

	@Global
	public static interface Callback {
		public void enter(String name);
		public void leave(String name);
	}
	
	@Global
	public static interface Target {
		public void execute(String name, int delay, Callback cb);
	}
	
	public static class Impl implements Target {
		private static Object o = new Object();
		
		public void execute(String name, int delay, Callback cb) {
			synchronized(o) {
				cb.enter(name);
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				cb.leave(name);
			}
		}
		
		public void illegalWait() throws InterruptedException {
			o.wait();
		}

		public void illegalNotify() throws InterruptedException {
			o.notify();
		}
	}

	public static class ImplArray implements Target {
		private static Object o = new int[0];
		
		public void execute(String name, int delay, Callback cb) {
			synchronized(o) {
				cb.enter(name);
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				cb.leave(name);
			}
		}
	}

	public static class ImplNative implements Target {
		private static Object o = new ArrayList<Integer>();
		
		public void execute(String name, int delay, Callback cb) {
			synchronized(o) {
				cb.enter(name);
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				cb.leave(name);
			}
		}
	}
	
	@Test
	public void simple(@Mocked final Callback cb) throws Exception {
		new Expectations() {
			{
				cb.enter("t1");
				cb.leave("t1");
			}
		};
		
		Entry<Target> en = proc.createEntry(Target.class, Impl.class.getName());
		Exit<Callback> ex = proc.createExit(Callback.class, cb);
		
		en.queue().execute("t1", 1000, ex.report());
		world.run();
	}

	@Test
	public void concurrent(@Mocked final Callback cb) throws Exception {
		new Expectations() {
			{
				cb.enter("t1");
				cb.leave("t1");
				cb.enter("t2");
				cb.leave("t2");
			}
		};
		
		Entry<Target> en1 = proc.createEntry(Target.class, Impl.class.getName());
		Entry<Target> en2 = proc.createEntry(Target.class, Impl.class.getName());
		Exit<Callback> ex = proc.createExit(Callback.class, cb);
		
		en1.at(1, SECONDS).queue().execute("t1", 2000, ex.report());
		en2.at(2, SECONDS).queue().execute("t2", 0, ex.report());
		world.run();
	}

	@Test
	public void simpleOnArray(@Mocked final Callback cb) throws Exception {
		new Expectations() {
			{
				cb.enter("t1");
				cb.leave("t1");
			}
		};
		
		Entry<Target> en = proc.createEntry(Target.class, ImplArray.class.getName());
		Exit<Callback> ex = proc.createExit(Callback.class, cb);
		
		en.queue().execute("t1", 1000, ex.report());
		world.run();
	}
	
	// https://code.google.com/p/minha/issues/detail?id=5
	@Test
	public void simpleOnNative(@Mocked final Callback cb) throws Exception {
		new Expectations() {
			{
				cb.enter("t1");
				cb.leave("t1");
			}
		};
		
		Entry<Target> en = proc.createEntry(Target.class, ImplNative.class.getName());
		Exit<Callback> ex = proc.createExit(Callback.class, cb);
		
		en.queue().execute("t1", 1000, ex.report());
		world.run();
	}

	
	public static class ExceptionImpl implements Runnable {
		private Object o = new Object();
				
		public void run() {
			try {
				o.wait();
			} catch (InterruptedException e) {
				// don't care
			}
		}
	}
	
	// https://code.google.com/p/minha/issues/detail?id=17
	@Test(expectedExceptions={IllegalMonitorStateException.class})
	public void illegalMonitorState(@Mocked final Callback cb) throws Exception {		
		Entry<Runnable> en = proc.createEntry(Runnable.class, ExceptionImpl.class.getName());

		en.call().run();
		world.run();
	}	
}
