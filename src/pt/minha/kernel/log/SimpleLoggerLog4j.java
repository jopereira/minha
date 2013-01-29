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

package pt.minha.kernel.log;

import java.io.IOException;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class SimpleLoggerLog4j implements pt.minha.kernel.log.Logger {
	private final Logger logger;
	
	public SimpleLoggerLog4j(String filename) {
		this.logger = Logger.getLogger(filename);
		this.logger.setAdditivity(false);
		this.logger.setLevel(Level.DEBUG);
		Layout layout = new PatternLayout("%m\n");
		try {
			this.logger.addAppender(new FileAppender(layout, filename, false));
		} catch (IOException e) {
			pt.minha.models.global.Debug.println("Unable to create log file '"+filename+"'");
			System.exit(1);
		}
	}
	
	public void debug(String message) {
		this.logger.debug(message);
	}
	
	public void close() {
	}
}
