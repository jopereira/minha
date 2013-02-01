/*******************************************************************************
 * MINHA - Java Virtualized Environment
 * Copyright (c) 2011, Universidade do Minho
 * All rights reserved.
 * 
 * Contributors:
 *  - Jose Orlando Pereira <jop@di.uminho.pt>
 *  - Nuno Alexandre Carvalho <nuno@di.uminho.pt>
 *  - Joao Paulo Bordalo <jbordalo@di.uminho.pt> 
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
 ******************************************************************************/

package pt.minha.tools;

import pt.minha.api.Host;
import pt.minha.api.World;

public class Runner {
	
	public static void main(String[] args) {
		try {						
			long simulationTime = Long.parseLong(System.getProperty("simulationTime", "0"));

			System.err.println("==== Minha -- middleware testing platform <http://www.minha.pt/>");
			System.err.println("==== Copyright (c) 2011-2013, Universidade do Minho.");
			System.err.println("==== License GPLv2+: GNU GPL version 2 or later <http://gnu.org/licenses/gpl.html>");
			System.err.println("==== This is free software: you are free to change and redistribute it.");
			System.err.println("==== There is NO WARRANTY, to the extent permitted by law.");
			System.err.println("====================================================================================");
			System.err.println("running for: " + simulationTime + " simulated seconds");

			World hv = new World(simulationTime);
			
			CommandLineArgumentsParser cla = new CommandLineArgumentsParser(args);
			
			for (final InstanceArguments argsInstance : cla) {
				for (int i=1; i<=argsInstance.getN(); i++) {
					Host host = hv.createHost(argsInstance.getIP());
					host.launch(argsInstance.getDelay(), argsInstance.getMain(), argsInstance.getArgs());
				}
			}
			
			System.err.println("====================================================================================");
			
			long time=System.nanoTime();
			long stime=hv.run();
			
			System.err.println("====================================================================================");
			System.err.println("simulation finished: "+((double)(System.nanoTime()-time)/1e9)+"s real time / "+(((double)stime)/1e9)+"s simulation time ====");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
