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

package pt.minha.models.local;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import pt.minha.kernel.simulation.Event;
import pt.minha.kernel.simulation.Timeline;
import pt.minha.models.global.EntryHandler;
import pt.minha.models.global.EntryInterface;
import pt.minha.models.global.ResultHolder;
import pt.minha.models.local.lang.SimulationThread;

public class Trampoline implements EntryHandler, Runnable {		
	private String implName;
	private pt.minha.models.fake.java.lang.Thread thread;
	private List<Invocation> queue = new ArrayList<Trampoline.Invocation>();
	private Event wakeup;
	private SimulationProcess host;
	
	public Trampoline(EntryInterface host, String implName) {
		this.implName = implName;
		this.host = (SimulationProcess) host;
		this.thread = new pt.minha.models.fake.java.lang.Thread((SimulationProcess)host, this);
		thread.simulationStart(0);
	}
	
	@Override
	public void invoke(long time, boolean relative, Method method, Object[] args, ResultHolder result) {
		Timeline timeline = host.getTimeline();
		if (!relative)
			time -= timeline.getTime();
		if (time < 0)
			time = 0;
		new Invocation(host.getTimeline(), method, args, result).schedule(time);
	}

	public class Invocation extends Event {
		public Method method;
		public Object[] args;
		private ResultHolder result;

		public Invocation(Timeline timeline, Method method, Object[] args, ResultHolder result) {
			super(timeline);
			this.method = method;
			this.args = args;
			this.result = result;
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
		try {
			SimulationThread.stopTime(0);
			Object impl = Class.forName(implName).newInstance();
			while(true) {
				if (queue.isEmpty()) {
					wakeup = SimulationThread.currentSimulationThread().getWakeup();
					SimulationThread.currentSimulationThread().pause(false, false);
				}
				
				Invocation i = queue.remove(0);
				
				try {
					SimulationThread.startTime(0);
					Object result = i.method.invoke(impl, i.args);
					SimulationThread.stopTime(0);
					
					i.result.reportReturn(result);
					
				} catch(InvocationTargetException ite) {
					if (!SimulationThread.currentSimulationThread().fake_isAlive())
						return;
					SimulationThread.stopTime(0);
					i.result.reportException(ite.getTargetException());
				}
				
				if (!i.result.isIgnored())
					host.getTimeline().getScheduler().stop();
			}
		} catch (Exception e) {
			throw new RuntimeException("exception entering simulation", e);
		}
	}
}