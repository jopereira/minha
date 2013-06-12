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

package pt.minha.models.global.net;

import java.io.IOException;
import java.util.Properties;

public class NetworkConfig {
	public long writeCost;
	public long readCost;
	public boolean networkDelay;
	public long networkBandwidth;

	private int real_degree;
	private double real_coef[];
	private int minha_degree;
	private double minha_coef[];

	// read calibration
	public NetworkConfig(Properties conf) throws Exception {
		networkDelay = false;

		writeCost = Long.parseLong(conf.getProperty("network.writeCost"));
		readCost = Long.parseLong(conf.getProperty("network.readCost"));
		networkBandwidth = Long.parseLong(conf
				.getProperty("network.networkBandwidth"));

		real_degree = Integer.parseInt(conf
				.getProperty("network.delay.real_degree"));
		minha_degree = Integer.parseInt(conf
				.getProperty("network.delay.minha_degree"));
		real_coef = new double[real_degree + 1];
		minha_coef = new double[real_degree + 1];

		String inputStr1 = conf.getProperty("network.delay.real_coef");
		String[] coef1S = inputStr1.split(",");
		if (coef1S.length != real_degree + 1)
			throw new IOException("Invalid coefficients data: " + coef1S.length);
		for (int i = 0; i <= real_degree; i++)
			real_coef[i] = Double.parseDouble(coef1S[i]);

		String inputStr2 = conf.getProperty("network.delay.minha_coef");
		String[] coef2S = inputStr2.split(",");
		if (coef2S.length != minha_degree + 1)
			throw new IOException("Invalid coefficients data: " + coef2S.length);
		int j = 0;
		for (j = 0; j <= minha_degree; j++)
			minha_coef[j] = Double.parseDouble(coef2S[j]);
		for (int i = j; i <= real_degree; i++)
			minha_coef[i] = 0.0;

		networkDelay = true;
	}

	/**
	 * Given a \code{payload} returns an accurate weight for \code{payload}
	 * 
	 * The new weight for \code{payload} is calculated by the difference between
	 * the first polynomial function and the second. The intention is to bring
	 * the second function closer to the first. The result is the compensation
	 * that was missing in the second function to the \code{payload}.
	 * 
	 * @param payload
	 * @return
	 */
	public long getNetworkOverhead(int payload) {
		if (!networkDelay)
			return 0;

		double res = 0;

		for (int i = real_degree; i >= 0; i--)
			res += (real_coef[i] - minha_coef[i]) * Math.pow(payload, i);
		res = res / 2 + ((double) payload) * 7.5;

		return (long) res;
	}

	public long getLineDelay(int size) {
		return size; // FIXME: Hardcoded 1Gbits network
	}

	public long getMaxDelay(int length) {
		return getLineDelay(65535); // FIXME: Hardcoded 64KB buffer
	}
}
