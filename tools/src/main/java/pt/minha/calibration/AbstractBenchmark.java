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

package pt.minha.calibration;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.net.InetSocketAddress;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Variance;

import pt.minha.api.sim.Global;

/**
 * Base class for calibration benchmarks.
 */
public abstract class AbstractBenchmark implements Benchmark {
	public InetSocketAddress srv;
	public int samples;
	public int payload;

	private transient double[] times;
	private transient double[] sizes;
	private transient int idx, begin;
	private transient long cputime;
	private transient ThreadMXBean mxbean;
	
	public void setParameters(Map<String, Object> p) {
		this.srv = (InetSocketAddress) p.get("server");
		this.samples = (Integer) p.get("samples");
		this.payload = (Integer) p.get("payload");
		
		begin = 0;
		idx = 0;
		sizes = new double[samples];
		times = new double[samples];
	}
	
	protected Result stop(boolean interarrival) {
		double cpu = ((double)(mxbean.getCurrentThreadCpuTime()-cputime))/idx;
		if (interarrival) {
			for(int i = idx-1; i>0; i--)
				times[i]-=times[i-1];
			begin = 1;			
		}
		Mean mean = new Mean();
		double m = mean.evaluate(times, begin, idx-begin);
		Variance var = new Variance();
		double v = var.evaluate(times, m, begin, idx-begin);
		return new Result(m, v, cpu);
	}

	protected void add(double length, double now) {
		sizes[idx] = length;
		times[idx] = now;
		idx++;
	}
	
	protected void start() {
		mxbean = ManagementFactory.getThreadMXBean();
        mxbean.setThreadCpuTimeEnabled(true);
        cputime = mxbean.getCurrentThreadCpuTime();
	}
	
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();

		begin = 0;
		idx = 0;
		sizes = new double[samples];
		times = new double[samples];
	}
	
	protected static boolean readFully(InputStream in, byte[] buffer) throws IOException {
		int length = 0;
		while(length < buffer.length) {
			int n = in.read(buffer, length, buffer.length-length);
			if (n<=0)
				return false;
			length += n;
		}		
		return true;
	}
	
	public String toString() {
		return "probe: "+samples+" samples of "+payload+" bytes";
	}
	
	@Global
	public static class Result implements Serializable {
		public double meanLatency, varLatency;
		public double meanCPU;
		
		public Result(double meanLatency, double varLatency, double meanCPU) {
			super();
			this.meanLatency = meanLatency;
			this.varLatency = varLatency;
			this.meanCPU = meanCPU;
		}

		public String toString() {
			return "latency="+meanLatency+"("+varLatency+") cpu="+meanCPU;
		}
	}
}
