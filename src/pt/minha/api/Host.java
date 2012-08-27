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

package pt.minha.api;

import pt.minha.kernel.instrument.ClassConfig;
import pt.minha.kernel.instrument.InstrumentationLoader;
import pt.minha.kernel.simulation.Timeline;
import pt.minha.models.global.HostInterface;

/**
 * A host running within the Minha simulated world. 
 */
public class Host {
	private InstrumentationLoader loader;
	private HostInterface impl;
	private long delay;

	Host(ClassConfig cc, Timeline timeline, long delay, String ip) throws SimulationException {
		this.delay=delay;

		loader=new InstrumentationLoader(cc);
		
		try {
			Class<?> clz = loader.loadClass("pt.minha.models.local.HostImpl");
			impl = (HostInterface)clz.getDeclaredConstructor(timeline.getClass(), String.class).newInstance(timeline, ip);
		} catch(Exception e) {
			throw new SimulationException(e);
		}
	}
	
	/** 
	 * Run an application in this simulated host.
	 * 
	 * @param main name of class with main method
	 * @param args arguments to main method
	 * @throws SimulationException cannot find or launch desired class 
	 */
	public void launch(String main, String[] args) throws SimulationException {
		impl.launch(delay, main, args);
	}
}
