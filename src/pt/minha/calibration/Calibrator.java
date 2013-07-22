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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.minha.api.Entry;
import pt.minha.api.World;

public class Calibrator implements Closeable {
	
	private static Logger logger = LoggerFactory.getLogger("pt.minha.calibration");

	private Socket socket;
	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	
	public Calibrator(String server) throws UnknownHostException, IOException {
		logger.info("client: connecting to {}", server);

		socket = new Socket(server, 12345);			
		oos = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		oos.flush();
		ois = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
	}
	
	private Result runCommand(Command next) throws Throwable {
		// Reality
		logger.info("client: sending {}", next);
		
		oos.writeObject(next);
		oos.flush();
		Thread.sleep(1000);
		
		logger.info("client: running {}", next);

		Result rcli = (Result) next.client();

		logger.info("client: running {} done", next);
		
		Object rsrv = ois.readObject();

		logger.info("client: got reply");
		
		// Simulation
		/*PWorld world = new World();			
		Entry<Command>[] e = world.createEntries(2, Command.class, next.getClass().getName());

		logger.info("simulation: loading {}", next);

		InetSocketAddress srv = new InetSocketAddress(e[1].getProcess().getHost().getAddress(), 20000); 
		e[1].queue().setParameters(srv, 1000, 1);
		e[0].queue().setParameters(srv, 1000, 1);			
		world.runAll(e);

		logger.info("simulation: running {}", next);

		e[1].queue().server();
		e[0].at(1, TimeUnit.SECONDS).queue().client();			
		world.runAll(e);
		
		Object scli = e[0].getResult();
		Object ssrv = e[1].getResult();

		logger.info("simulation: running {} done", next);
		
		world.close();*/

		// Summarize results
		//summarize(rcli, rsrv, scli, srv);
		
		//logger.info("got {}/{} vs {}/{}", rcli, rsrv, scli, ssrv);
		logger.info("got {}/{}", rcli, rsrv);

		return rcli;
	}

	public void close() throws IOException {
		socket.close();
	}
	
	private static void runServer() throws Exception {
		// Reality server
		ServerSocket ss = new ServerSocket(12345);
		
		logger.info("server: started at {}", ss.getLocalSocketAddress());
		
		while(true) {
			Socket s = ss.accept();

			logger.info("server: accepted {}", s.getRemoteSocketAddress());
			
			try {
				ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
				oos.flush();
				ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(s.getInputStream()));
				
				while(!s.isClosed()) {
					Command next = (Command) ois.readObject();
					
					logger.info("server: running {}", next);
					
					Object result = next.server();

					logger.info("server: running {} done", next);

					oos.writeObject(result);
					oos.flush();
				}

				logger.info("server: disconnected {}", s.getRemoteSocketAddress());
			
			} catch(IOException ioe) {
				logger.info("server: disconnected {} on {}", s.getRemoteSocketAddress(), ioe);
			}
		}					
	}
	
	public static void main(String[] args) throws Throwable {
		if (args[0].equals("--server")) {
			runServer();
		} else {
			Calibrator calib = new Calibrator(args[0]);

			// Warm-up (JIT, etc)
			
			{
			Command next = new TCPFlood(0);
			next.setParameters(new InetSocketAddress(args[0], 20000), 5000, 1);
			calib.runCommand(next);
			}

			{
			Command next = new TCPRoundTrip();
			next.setParameters(new InetSocketAddress(args[0], 20000), 5000, 1);
			calib.runCommand(next);
			}
			
			// Run

			double max = Double.MIN_VALUE;
			SimpleRegression netCPU = new SimpleRegression(true);
			for(int i: new int[]{1, 100, 1000, 4000, 8000, 16000}) {
				Command next = new TCPFlood(0);
				next.setParameters(new InetSocketAddress(args[0], 20000), 5000, i);
				Result r = calib.runCommand(next);
				netCPU.addData(i, r.meanCPU);
				double bw = 8*i*1e9d/r.meanLatency;
				if (bw > max)
					max = bw;
			}
			
			SimpleRegression switchOverhead = new SimpleRegression(true);
			for(int i: new int[]{1, 100, 1000, 4000, 8000, 16000}) {
				Command next = new TCPFlood(1);
				next.setParameters(new InetSocketAddress(args[0], 20000), 5000, i);
				Result r = calib.runCommand(next);
				switchOverhead.addData(i, r.meanCPU);
			}
			
			SimpleRegression rtt = new SimpleRegression(true);
			for(int i: new int[]{1, 100, 1000, 4000, 8000, 16000}) {
				Command next = new TCPRoundTrip();
				next.setParameters(new InetSocketAddress(args[0], 20000), 5000, i);
				Result r = calib.runCommand(next);
				rtt.addData(i, r.meanLatency);
			}
			
			// Compute results
			
			logger.info("CPU in net stack: {}+{}*b", netCPU.getIntercept(), netCPU.getSlope());
			logger.info("context switch overhead: {} (ignored: +{}*b)", switchOverhead.getIntercept()-netCPU.getIntercept(), switchOverhead.getSlope()-netCPU.getSlope());
			logger.info("network latency: {}+{}*b", rtt.getIntercept()-2*switchOverhead.getIntercept(), rtt.getSlope()/2-netCPU.getSlope());
			logger.info("network bandwith: {} bps", max);

			calib.close();
		}
	}
}
