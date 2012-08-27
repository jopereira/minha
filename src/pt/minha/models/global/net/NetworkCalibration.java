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

package pt.minha.models.global.net;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import pt.minha.kernel.util.PropertiesLoader;

public class NetworkCalibration {
	public static long writeCost;
	public static long readCost;
	public static boolean networkDelay;
	public static long networkBandwidth;

	private static int real_degree;
	private static double real_coef[];
	private static int minha_degree;
	private static double minha_coef[];
	
	private static double packetLoss;
	private static Random random = new Random();

	// read calibration
	public static void load() throws Exception {
		String filename = System.getProperty("networkCalibration");
		networkDelay = false;
		if ( null != filename ) {
			System.err.println("==== Loading network calibration from '" + filename + "'");
			PropertiesLoader conf = new PropertiesLoader(filename);
			writeCost = Long.parseLong(conf.getProperty("network.writeCost"));
			readCost = Long.parseLong(conf.getProperty("network.readCost"));
			networkBandwidth = Long.parseLong(conf.getProperty("network.networkBandwidth"));
			
			real_degree = Integer.parseInt(conf.getProperty("network.delay.real_degree"));
			minha_degree = Integer.parseInt(conf.getProperty("network.delay.minha_degree"));
			real_coef = new double[real_degree+1];
			minha_coef = new double[real_degree+1];

			packetLoss = Double.parseDouble(conf.getProperty("network.packetLoss", "0"));

			String inputStr1 = conf.getProperty("network.delay.real_coef");
			String[] coef1S = inputStr1.split(",");
			if (coef1S.length != real_degree+1)
				throw new IOException("Invalid coefficients data: "+coef1S.length);
			for (int i=0; i<=real_degree; i++)
				real_coef[i] = Double.parseDouble(coef1S[i]);
			
			String inputStr2 = conf.getProperty("network.delay.minha_coef");
			String[] coef2S = inputStr2.split(",");
			if (coef2S.length != minha_degree+1)
				throw new IOException("Invalid coefficients data: "+coef2S.length);
			int j=0;
			for (j=0; j<=minha_degree; j++)
				minha_coef[j] = Double.parseDouble(coef2S[j]);
			for (int i=j; i<=real_degree; i++)
				minha_coef[i] = 0.0;
			
			networkDelay = true;	
		}
		else {
			System.err.println("==== Using network calibration default values");
			writeCost = 0;
			readCost = 0;
			networkBandwidth = 1000000000; // Gigabit
		}
		
		System.err.println("======= write cost:\t\t" + writeCost + "nanoseconds");
		System.err.println("======= read cost\t\t" + readCost + "nanoseconds");
		System.err.println("======= network bandwidth:\t" + networkBandwidth + "bit/s");
		if ( networkDelay ) {
			System.err.println("======= network delay real coeficients:  " + Arrays.toString(real_coef));
			System.err.println("======= network delay minha coeficients: " + Arrays.toString(minha_coef));
		}
		else
			System.err.println("======= network delay:\t\t0");
		
		System.err.println("======= packet loss probability:  " + packetLoss);
	}

	/**
	* Given a \code{payload} returns an accurate weight for \code{payload}
        * 
        * The new weight for \code{payload} is calculated by the difference between 
        * the first polynomial function and the second.
        * The intention is to bring the second function closer to the first.
        * The result is the compensation that was missing in the second function
        * to the \code{payload}. 
        * 
        * @param payload
        * @return
        */
	public static long getNetworkDelay(int payload) {
		double res = 0;
		
		for (int i = real_degree; i >= 0; i--)
			res += (real_coef[i] - minha_coef[i]) * Math.pow(payload, i);
		res = res/2 + ((double)payload)*7.5;
		
		return (long)res;
	}
	
	public static boolean isLostPacket() {
		if (packetLoss>0)
			return random.nextDouble()<packetLoss;
		return false;
	}
}
