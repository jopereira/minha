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

package pt.minha.api;

import java.util.Properties;

import pt.minha.calibration.Linear;

/**
 * Calibration properties to make the simulation mimic some real machine.
 */
public class Calibration {
	public Calibration() {
		reset();
	}

	public Calibration(Properties conf) throws Exception {
		reset();
		load(conf);
	}

	// CPU
	public long getCPUDelay(long value) {
		return cpuScaling.scale(value);
	}

	// Network switch
	public long getLineDelay(int size) {
		long res = (long) (size*(1e9d/(networkBandwidth/8)));
		if (res<=0)
			return 1;
		return res;
	}

	public long getAdditionalDelay(int size) {
		long res = networkLatency.scale(size);
		if (res<=0)
			return 1;
		return res;
	}
	
	// Network stack
	public long getTCPOverhead(int size) {
		return tcpOverhead.scale(size);
	}

	public long getUDPOverhead(int size) {
		return udpOverhead.scale(size);
	}

	public long getMaxDelay() {
		return getLineDelay(udpBuffer);
	}
	
	/**
	 * Reset calibration to defaults (i.e., no calibration).
	 */
	public void reset() {
		cpuScaling = Linear.IDENTITY;
		networkLatency = Linear.UNIT;
		tcpOverhead = Linear.ZERO;
		udpOverhead = Linear.ZERO;
		udpBuffer = 65535;
	}
	
	/**
	 * Load calibration from properties.
	 * @param props calibration properties
	 */
	public void load(Properties props) {
		cpuScaling = new Linear(props.getProperty("cpuScaling", cpuScaling.toString()));
		networkLatency = new Linear(props.getProperty("networkLatency", networkLatency.toString()));
		networkBandwidth = Long.parseLong(props.getProperty("networkBandwidth", Long.toString(networkBandwidth)));
		tcpOverhead = new Linear(props.getProperty("tcpOverhead", tcpOverhead.toString()));
		udpOverhead = new Linear(props.getProperty("udpOverhead", udpOverhead.toString()));
		udpBuffer = Integer.parseInt(props.getProperty("udpBuffer", Integer.toString(udpBuffer)));
	}
	
	/**
	 * Save calibration to properties.
	 * @param props calibration properties
	 */
	public void save(Properties props) {
		props.setProperty("cpuScaling", cpuScaling.toString());
		props.setProperty("networkLatency", networkLatency.toString());
		props.setProperty("networkBandwidth", Long.toString(networkBandwidth));
		props.setProperty("tcpOverhead", tcpOverhead.toString());
		props.setProperty("udpOverhead", udpOverhead.toString());
		props.setProperty("udpBuffer", Integer.toString(udpBuffer));
	}
	
	private Linear cpuScaling;

	private Linear networkLatency;
	private long networkBandwidth;

	private Linear tcpOverhead;
	private Linear udpOverhead;
	private int udpBuffer;
}
