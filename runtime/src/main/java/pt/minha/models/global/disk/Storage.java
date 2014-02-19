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

package pt.minha.models.global.disk;

import pt.minha.api.sim.Calibration;
import pt.minha.kernel.simulation.SimpleResource;
import pt.minha.models.fake.java.io.File;
import pt.minha.models.local.lang.SimulationThread;

public class Storage {
	private SimpleResource bandwidth;
	private Calibration config;

	public Storage(Calibration config) {
		this.config = config;
	}

	public String translate(String name) {
		String pathAux = SimulationThread.currentSimulationThread().getHost().getName();
		java.io.File newDir = new java.io.File(pathAux);
		if(!newDir.exists()) newDir.mkdir();
		return pathAux + File.separator + name;
	}
	
	public long scheduleRead(int size) {
		return bandwidth.use(getConfig().getLineDelay(size)) + getConfig().getLineLatency(size);
	}
	
	public long scheduleWrite(int size) {
		return bandwidth.use(getConfig().getLineDelay(size));		
	}
	
	public long scheduleCache(long delta, int size) {
		if (delta < config.getMaxCachedDelay())
			delta = 0;
		else
			delta -= config.getMaxCachedDelay();
		return delta + getConfig().getLineLatency(size);
	}
	
	public Calibration getConfig() {
		return config;
	}
}
