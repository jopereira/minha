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

package pt.minha.kernel.simulation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// ****************************
// FIXME: This is currently disabled as it doesn't work with the parallel simulation kernel.
// ****************************

public class Usage extends Event {
	private long interval, base, acum, rtbase;
	private Logger logger;
	private String units;
	private float factor;
	private int threshold;

	/**
	 * @param timeline reference timeline, used to schdule periodic logging
	 * @param interval logging interval
	 * @param id resource identifier
	 * @param factor multiplicative factor 
	 * @param threshold minimum events that cause reporting
	 */
	public Usage(Timeline timeline, long interval, String id, float factor, String units, int threshold) {
		super(timeline);
		this.interval = interval;
		this.base = -1;
		this.logger = LoggerFactory.getLogger("pt.minha.Usage."+id);
		this.factor = factor;
		this.units = units;
		this.threshold = threshold;
	}

	public void run() {
		if (acum <= threshold) {
			base = -1;
			acum = 0;
			return;
		}
		
		long now = getTimeline().getTime();
		float delta = (float)Timeline.toSeconds(now-base);
		float usage = acum / delta * factor;
		long rtnow = System.nanoTime();
		float rtdelta = (float)Timeline.toSeconds(rtnow-rtbase);
		float rtusage = acum / rtdelta * factor;
		int ratio = (int)(100*rtdelta/delta);
		logger.info("[ {} s, +{} s ] {} {} (RT: {}% - {} {})", Timeline.toSeconds(base), delta, usage, units, ratio, rtusage, units);
		
		base = now;
		acum = 0;
		rtbase = rtnow;
		
		//schedule(interval);
	}
	
	public void using(long used) {
		acum += used;
		if (base<0 && acum > threshold) {
			//schedule(interval);
			base = getTimeline().getTime();
			rtbase = System.nanoTime();
		}
	}
}