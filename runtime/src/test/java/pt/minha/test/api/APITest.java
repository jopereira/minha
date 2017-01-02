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

package pt.minha.test.api;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testng.Assert.assertEquals;

import java.lang.reflect.UndeclaredThrowableException;

import mockit.Expectations;
import mockit.Mocked;

import org.testng.annotations.Test;

import pt.minha.api.Entry;
import pt.minha.api.Exit;
import pt.minha.api.Host;
import pt.minha.api.Process;
import pt.minha.api.World;
import pt.minha.api.sim.Global;
import pt.minha.api.sim.Simulation;
import pt.minha.api.sim.SimulationDeadlockException;
import pt.minha.models.local.lang.SimulationThread;

public class APITest {

	@Global
	public static interface Callback {
		public int exit(int v);
	}
	
	@Global
	public static interface Target {
		public int entry(Exit<Callback> cb);
		public int entry(int v);
		public int entry(int v, Exit<Callback> cb);
		public int entry();
	}
	
	public static class Impl implements Target {
		public int entry(Exit<Callback> cb) {
			SimulationThread.stopTime(0);
			SimulationThread.currentSimulationThread().getProcess().getTimeline().getScheduler().stop();
			SimulationThread.startTime(0);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			cb.report().exit(1);
			return 1;
		}

		@Override
		public int entry(int v) {
			return v+1;
		}

		@Override
		public int entry(int v, Exit<Callback> cb) {
			cb.report().exit(v);
			return cb.callback().exit(v);
		}
	
		public int entry() {
			throw new NullPointerException("expected!");
		}
	}

	@Test
	public void empty() throws Exception {
		Simulation world = new Simulation();
		world.createHost();

		world.run();
		
		world.close();

	}
	
	// https://code.google.com/p/minha/issues/detail?id=7
	@Test
	public void entryOnly() throws Exception {
		World world = new Simulation();
		Host host = world.createHost();
		Process proc = host.createProcess();
		
		Entry<Target> en = proc.createEntry(Target.class, Impl.class.getName());

		int r = en.call().entry(100);
		
		assertEquals(r, 101);
		
		world.close();
	}
	
	@Test
	public void entryAndExit(@Mocked final Callback cb) throws Exception {
		World world = new Simulation();
		Host host = world.createHost();
		Process proc = host.createProcess();

		new Expectations() {
			{
				cb.exit(1); result = 2;
				cb.exit(1); result = 3;

				cb.exit(10); result = 20;
				cb.exit(10); result = 30;

				cb.exit(100); result = 200;
				cb.exit(100); result = 300;
			}
		};
		
		Entry<Target> en = proc.createEntry(Target.class, Impl.class.getName());
		Exit<Callback> ex = proc.createExit(Callback.class, cb);

		en.at(2, SECONDS).queue().entry(10, ex);
		en.at(1, SECONDS).queue().entry(1, ex);
		int r = en.at(3, SECONDS).call().entry(100, ex);
		
		assertEquals(r, 300);
		
		world.close();
	}
	
	@Test
	public void lazyEntry(@Mocked final Callback cb) throws Exception {
		World world = new Simulation();
		Host host = world.createHost();
		Process proc = host.createProcess();

		new Expectations() {
			{
				cb.exit(10); result = 20;
				cb.exit(10); result = 30;
			}
		};
		
		Entry<Target> en = proc.createEntry(Target.class, Impl.class.getName());
		Exit<Callback> ex = proc.createExit(Callback.class, cb);

		// Not expected to run
		en.at(100, SECONDS).queue().entry(1, ex);
		
		int r = en.at(1, SECONDS).call().entry(10, ex);
		
		assertEquals(r, 30);

		world.close();
	}
	
	@Test(expectedExceptions={SimulationDeadlockException.class})
	public void interrupted(@Mocked final Callback cb) throws Exception {
		World world = new Simulation();
		Host host = world.createHost();
		Process proc = host.createProcess();
	
		new Expectations() {
			{
			}
		};
		
		Entry<Target> en = proc.createEntry(Target.class, Impl.class.getName());
		Exit<Callback> ex = proc.createExit(Callback.class, cb);

		en.call().entry(ex);
		
		world.close();
	}
	
	@Test
	public void runAll(@Mocked final Callback cb1, @Mocked final Callback cb2) throws Throwable {
		World world = new Simulation();
		Host host1 = world.createHost();
		Process proc1 = host1.createProcess();

		Host host2 = world.createHost();
		Process proc2 = host2.createProcess();

		new Expectations() {
			{
				cb1.exit(1); result = 2;
				cb1.exit(1); result = 3;

				cb2.exit(10); result = 20;
				cb2.exit(10); result = 30;
			}
		};
		
		Entry<Target> en1 = proc1.createEntry(Target.class, Impl.class.getName());
		Exit<Callback> ex1 = proc1.createExit(Callback.class, cb1);

		Entry<Target> en2 = proc2.createEntry(Target.class, Impl.class.getName());
		Exit<Callback> ex2 = proc2.createExit(Callback.class, cb2);
		
		en1.queue().entry(1, ex1);
		en2.at(10, SECONDS).queue().entry(10, ex2);
		
		world.runAll(en1, en2);
		
		assertEquals(en1.getResult(), 3);
		assertEquals(en2.getResult(), 30);

		world.close();
	}

	// https://code.google.com/p/minha/issues/detail?id=8
	@Test(expectedExceptions={NullPointerException.class})
	public void asyncException(@Mocked final Callback cb) throws Throwable {
		World world = new Simulation();
		Host host = world.createHost();
		Process proc = host.createProcess();

		new Expectations() {
			{
			}
		};
		
		Entry<Target> en = proc.createEntry(Target.class, Impl.class.getName());

		en.queue().entry();
				
		world.run();
		
		en.getResult();
		
		world.close();
	}
}
