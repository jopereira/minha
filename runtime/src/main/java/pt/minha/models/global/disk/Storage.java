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
import pt.minha.kernel.simulation.Timeline;
import pt.minha.models.fake.java.io.File;
import pt.minha.models.local.lang.SimulationThread;

public class Storage {
	private SimpleResource bandwidth;
	private Calibration config;
	private String pathPrefix;

	public Storage(Timeline timeline, Calibration config, String path) {
		this.config = config;
		this.pathPrefix = path;
		this.bandwidth = new SimpleResource(timeline);
	}

	public String translate(String name) {
		java.io.File newDir = new java.io.File(pathPrefix);
		if(!newDir.exists()) newDir.mkdir();
		return pathPrefix + File.separator + name;
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
