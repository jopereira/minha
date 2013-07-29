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

package pt.minha.calibration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Linear {
	private static Pattern re = Pattern.compile("([-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?)?(([-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?)[xX])?");
	private double intercept, slope;

	public Linear(String eq) {
		Matcher m = re.matcher(eq);
		if (!m.matches())
			throw new IllegalArgumentException("not a valid configuration: "+eq);
		if (m.group(1)!=null)
			intercept = Double.parseDouble(m.group(1));
		if (m.group(4)!=null)
			slope = Double.parseDouble(m.group(4));
	}
	
	public Linear(double intercept, double slope) {
		this.intercept = intercept;
		this.slope = slope;
	}
	
	public long scale(long value) {
		long result = (long) (slope*value + intercept);
		if (result < 1)
			return 1;
		return result;
	}
	
	public static final Linear ZERO = new Linear(0, 0);
	public static final Linear IDENTITY = new Linear(0, 1);
	
	public String toString() {
		return intercept+(slope>=0?"+":"")+slope+"x";
	}
}
