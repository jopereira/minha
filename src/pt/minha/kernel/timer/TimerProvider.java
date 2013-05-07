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

package pt.minha.kernel.timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimerProvider {
	private static Logger logger = LoggerFactory.getLogger("pt.minha.Timer"); 
	
	private static Class<?> clz;
	
	static {
		try {
			clz = Class.forName("pt.minha.kernel.timer.NativeTimer");
			
		} catch(Throwable e) {
			logger.warn("Cannot find native timer. Using unreliable timer.");
		}
	}
	
	public static IntervalTimer open() {
		try {
			if (clz != null)
				return (IntervalTimer) clz.newInstance();
		} catch(Exception e) {
			clz = null;
			logger.warn("Cannot use native timer. Using unreliable timer.", e);
		}
		return new JavaTimer();
	}
}
