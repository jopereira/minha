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

package pt.minha.cmdline;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This class supports arguments like:
 * INSTANCE, [INSTANCE, INSTANCE, ...]
 * 
 * Where each INSTANCE is one the following rules
 * [-d=X] N IP MAIN ARGS
 * [-d=X] N MAIN ARGS
 * [-d=X] IP MAIN ARGS
 * [-d=X] MAIN ARGS
 * [-d=X] MAIN
 * 
 * Where:
 * -d=X: X time in seconds at which the instance will be started
 * N: number of instance replicas
 * IP: numerical address
 * MAIN: main class
 * ARGS: arguments of the main class 
 */
public class CommandLineArgumentsParser implements Iterable<InstanceArguments> {
	private static final String ARG_SEPARATOR = " ";
	private static final String INTANCE_SEPARATOR = ",";

	private final List<InstanceArguments> instancesArguments = new LinkedList<InstanceArguments>();


	public static String arrayToString(String[] a) {
		return arrayToString(a, 0);
	}


	public static String arrayToString(String[] a, int start) {
		StringBuffer result = new StringBuffer();
		if (a.length > start) {
			result.append(a[start]);
			for (int i=start+1; i<a.length; i++) {
				result.append(ARG_SEPARATOR);
				result.append(a[i]);
			}
		}
		return result.toString();
	}


	/**
	 * Method that parses args by instance.
	 * @param args
	 */
	public CommandLineArgumentsParser(String[] args) throws Exception {
		// merge args in a single string
		String argsString = arrayToString(args).trim();
		if (argsString.isEmpty())
			throw new Exception("Empty command line args");

		// split args by INTANCE_SEPARATOR
		String[] instanceString = argsString.split(INTANCE_SEPARATOR);

		for (int i=0; i< instanceString.length; i++ ) {
			String s = instanceString[i].trim();
			this.instancesArguments.add(this.InstanceArgumentsParser(s));
		}
	}


	/**
	 * Method that parses instance arguments.
	 * @param s
	 * @throws Exception 
	 */
	public InstanceArguments InstanceArgumentsParser(String s) throws Exception {
		if (s.isEmpty())
			throw new Exception("Empty instance args");

		// defaults
		long delay = 0;
		int N = 1;
		String IP = null;
		String main = null;
		String[] argsMain = new String[]{};

		String[] args = s.split(ARG_SEPARATOR);

		int i = 0;
		
		// delay
		if (args[i].startsWith("-d=")) {
			try {
				delay = Long.parseLong(args[i].substring(3));
				i++;
			}
			catch (NumberFormatException e) {
				// ignore
			}
		}
		
		// exists number of replicas?
		try {
			N = Integer.parseInt(args[i]);
			if ( N < 1)
				throw new Exception("Invalid zero or negative number of replicas");
			i++;
		}
		catch (NumberFormatException e) {
			// ignore
		}

		// exists IP?
		try {
			InetAddress.getByName(args[i]);
			IP = args[i];
			i++;
		}
		catch (UnknownHostException e) {
			// ignore
		}
		
		// main
		main = args[i];
		i++;
		
		// main args
		if ( args.length > i ) {
			argsMain = arrayToString(args, i).split(ARG_SEPARATOR);
			i += argsMain.length;
		}
		
		return new InstanceArguments(N, IP, delay, main, argsMain);
	}


	@Override
	public Iterator<InstanceArguments> iterator() {
		return this.instancesArguments.iterator();
	}
}


class InstanceArguments {
	private final int N;
	private final String IP;
	private final long delay;
	private final String main;
	private final String[] args;

	protected InstanceArguments(int N, String IP, long delay, String main, String[] args) {
		this.N = N;
		this.IP = IP;
		this.delay = delay;
		this.main = main;
		this.args = args;
	}

	protected int getN() {
		return this.N;
	}

	protected String getIP() {
		return this.IP;
	}

	protected long getDelay() {
		return this.delay;
	}
	
	protected String getMain() {
		return this.main;
	}

	protected String[] getArgs() {
		return this.args;
	}
	
	protected String getArgsString() {
		return CommandLineArgumentsParser.arrayToString(this.args);
	}
	
	public String toString() {
		return "{ N: "+this.N+";\tIP: "+this.IP+";\tdelay: "+this.delay+"s;\tmain: "+this.main+";\targs: "+CommandLineArgumentsParser.arrayToString(this.args)+" }";
	}
}
