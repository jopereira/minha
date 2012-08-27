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

package pt.minha.kernel.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class LoggerPrintStream implements Logger {
	private PrintStream out;
	
	public LoggerPrintStream(String filename) {
		String dirname = new File(filename).getParent();
		if ( null != dirname ) {
			File dir = new File(dirname);
			if ( !dir.isDirectory() ) {
				if ( !new File(dirname).mkdirs() ) {
					pt.minha.models.global.Debug.println("Unable to create folder '"+dirname+"' to log file '"+filename+"'");
					System.exit(1);
				}
			}
		}

		try {
			this.out = new PrintStream(new FileOutputStream(filename), true);
		} catch (FileNotFoundException e) {
			pt.minha.models.global.Debug.println("Unable to log file '"+filename+"'");
			System.exit(1);
		}
	}
	
	public void debug(String message) {
		this.out.println(message);
	}
	
	public void close() {
		this.out.close();
	}
}
