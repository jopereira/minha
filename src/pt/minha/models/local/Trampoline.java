/*
 * Minha.pt: middleware testing platform.
 * Copyright (c) 2011-2012, Universidade do Minho.
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

package pt.minha.models.local;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import pt.minha.api.Global;
import pt.minha.kernel.simulation.Event;
import pt.minha.kernel.simulation.Timeline;
import pt.minha.models.global.HostInterface;
import pt.minha.models.local.lang.SimulationThread;

@Global
public interface Trampoline {
	void invoke(long time, Method method, Object[] args);

	public static class Impl implements Trampoline, Runnable {		
		private Object impl;
		private pt.minha.models.fake.java.lang.Thread thread;
		private List<Invocation> queue = new ArrayList<Trampoline.Impl.Invocation>();
		private Event wakeup;
		private HostImpl host;
		
		public Impl(HostInterface host, String impl) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
			this.impl = Class.forName(impl).newInstance();
			this.host = (HostImpl) host;
			this.thread = new pt.minha.models.fake.java.lang.Thread((HostImpl)host, this);
			thread.simulationStart(0);
		}
		
		@Override
		public void invoke(long time, Method method, Object[] args) {
			new Invocation(host.getTimeline(), method, args).schedule(time);
		}
	
		public class Invocation extends Event {
			public Method method;
			public Object[] args;
	
			public Invocation(Timeline timeline, Method method, Object[] args) {
				super(timeline);
				this.method = method;
				this.args = args;
			}
	
			@Override
			public void run() {
				queue.add(this);
				if (wakeup!=null) {
					wakeup.schedule(0);
					wakeup = null;
				}
			}
		}
		
		public void run() {
			SimulationThread.stopTime(0);
			while(true) {
				if (queue.isEmpty()) {
					wakeup = SimulationThread.currentSimulationThread().getWakeup();
					SimulationThread.currentSimulationThread().pause();
				}
				
				Invocation i = queue.remove(0);
				
				try {
					SimulationThread.startTime(0);
					i.method.invoke(impl, i.args);
					SimulationThread.stopTime(0);
				} catch (Exception e) {
					// FIXME: should stop simulation?
					e.printStackTrace();
				}			
			}
		}
	}
}
