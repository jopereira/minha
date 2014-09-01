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

package pt.minha.models.fake.java.lang;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;

import pt.minha.models.local.io.SystemStreamsImpl;
import pt.minha.models.local.lang.SimulationThread;

public class System {
	private static Properties sysProps;
	
	static {
		// Now in a class that uses moved, convert to java.util.Properties
		Map<java.lang.Object,java.lang.Object> props = SimulationThread.currentSimulationThread().getProcess().getSystemProperties();
		sysProps = new Properties();
		sysProps.putAll(props);		
	}
	
	public static long nanoTime() {
		try {
			SimulationThread.stopTime(0);
			return SimulationThread.currentSimulationThread().getTimeline().getTime();
		} finally {
			SimulationThread.startTime(0);
		}
	}
	
	public static void gc(){
		java.lang.System.gc();
	}

	public static long currentTimeMillis() {
		return nanoTime()/1000000;
	}
	
	public static Properties getProperties() {
		return sysProps;
	}
	
	public static String getProperty(String prop) {
		return sysProps.getProperty(prop);
	}

	public static String getProperty(String prop, String value) {
		return sysProps.getProperty(prop, value);
	}
	
	public static String setProperty(String prop, String value) {
		return (String) sysProps.setProperty(prop, value);
	}
					
	public static void arraycopy(java.lang.Object a, int b, java.lang.Object c, int d, int e) {
		java.lang.System.arraycopy(a, b, c, d, e);
	}
	
	public static void exit(int status) {
		pt.minha.models.global.Debug.println("Stopping simulation through java.lang.System.exit() whit status "+status);
		// FIXME: This is a workaround.
		while(true)
			SimulationThread.currentSimulationThread().pause(false, false);
	}
	
	public static SecurityManager getSecurityManager() {
		return java.lang.System.getSecurityManager();
	}
	
	public static int identityHashCode(Object obj) {
		return java.lang.System.identityHashCode(obj);
	}
	
	public static final PrintStream out = new PrintStream(new OutputStream() {
		@Override
		public void write(int b) throws IOException {
			SystemStreamsImpl.writeOut(b);
		}		
	});
	public static final PrintStream err = new PrintStream(new OutputStream() {
		@Override
		public void write(int b) throws IOException {
			SystemStreamsImpl.writeErr(b);
		}		
	});
	public static final InputStream in = new InputStream() {
		@Override
		public int read() throws IOException {
			return SystemStreamsImpl.readIn();
		}
	};
}
