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

package pt.minha.kernel.simulation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Usage extends Event {
	private long interval, base, acum;
	private Logger logger;
	private String units;
	private double factor;

	/**
	 * @param timeline reference timeline, used to schdule periodic logging
	 * @param interval logging interval
	 * @param id resource identifier
	 * @param factor multiplicative factor 
	 */
	public Usage(Timeline timeline, long interval, String id, double factor, String units) {
		super(timeline);
		this.interval = interval;
		this.base = -1;
		this.logger = LoggerFactory.getLogger("pt.minha.Usage."+id);
		this.factor = factor;
		this.units = units;
	}

	public void run() {
		if (acum == 0) {
			base = -1;
			return;
		}
		
		long now = getTimeline().getTime();
		double delta = Timeline.toSeconds(now-base);
		double usage = acum / delta * factor;
		logger.info("[ {} s, +{} s ] {} {}", Timeline.toSeconds(base), delta, usage, units);
		
		base = now;
		acum = 0;
		
		schedule(interval);
	}
	
	public void using(long used) {
		if (base<0) {
			schedule(interval);
			base = getTimeline().getTime();
		}
		acum += used;
	}
}