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
import java.util.HashMap;
import java.util.Map;
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
	
	private Result runReal(Map<String,Object> p) throws Throwable {
		// Reality
		
		logger.info("real: sending {}", p);

		oos.writeObject(p);
		oos.flush();
		Thread.sleep(1000);
		
		logger.info("real: running {}", p);
		
		Command next = (Command) Class.forName((String) p.get("bench")).newInstance();
		next.setParameters(p);

		Result rcli = (Result) next.client();

		logger.info("real: running {} done", next);
		
		Object rsrv = ois.readObject();

		logger.info("client: got reply");
		
		logger.info("real: got {}/{}", rcli, rsrv);
		
		return rcli;
	}
				
	private Result runSimulated(Map<String,Object> p) throws Throwable {
		Command next = (Command) Class.forName((String) p.get("bench")).newInstance();
		next.setParameters(p);

		World world = new World();			
		Entry<Command>[] e = world.createEntries(2, Command.class, (String) p.get("bench"));

		logger.info("simulation: loading {}", next);

		InetSocketAddress srv = new InetSocketAddress(e[1].getProcess().getHost().getAddress(), 20000);
		p.put("server", srv);
		e[0].call().setParameters(p);
		e[1].call().setParameters(p);
		
		logger.info("simulation: running {}", next);

		e[1].queue().server();
		e[0].at(1, TimeUnit.SECONDS).queue().client();			
		world.runAll(e);
		
		Object scli = e[0].getResult();
		Object ssrv = e[1].getResult();

		logger.info("simulation: running {} done", next);
		
		world.close();
		
		logger.info("simulation: got {}/{}", scli, ssrv);

		return (Result) scli;
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
					Map<String,Object> p = (Map<String,Object>) ois.readObject();					
					Command next = (Command) Class.forName((String) p.get("bench")).newInstance();
					next.setParameters(p);
					
					logger.info("server: running {}", p);
					
					Object result = next.server();

					logger.info("server: running {} done", p);

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

			SimpleRegression cpuoh = new SimpleRegression(true);
			for(int i: new int[]{100, 1000, 10000, 20000}) {
				Map<String,Object> p = new HashMap<String, Object>();
				p.put("bench", TimeVirt.class.getName());
				p.put("samples", 50000);
				p.put("payload", i);
				Result r = calib.runReal(p);
				Result s = calib.runSimulated(p);
				cpuoh.addData(s.meanCPU, r.meanCPU);
			}
			
			// Run

			double max = Double.MIN_VALUE;
			SimpleRegression netCPU = new SimpleRegression(true);
			SimpleRegression netCPU_s = new SimpleRegression(true);
			for(int i: new int[]{1, 100, 1000, 4000, 8000, 16000}) {
				Map<String,Object> p = new HashMap<String, Object>();
				p.put("bench", TCPFlood.class.getName());
				p.put("server", new InetSocketAddress(args[0], 20000));
				p.put("samples", 5000);
				p.put("payload", i);
				Result r = calib.runReal(p);
				netCPU.addData(i, r.meanCPU);
				Result s = calib.runSimulated(p);
				netCPU_s.addData(i, s.meanCPU);

				double bw = 8*i*1e9d/r.meanLatency;
				if (bw > max)
					max = bw;

			}

			SimpleRegression rtt = new SimpleRegression(true);
			SimpleRegression rtt_s = new SimpleRegression(true);
			for(int i: new int[]{1, 100, 1000, 4000, 8000, 16000}) {
				Map<String,Object> p = new HashMap<String, Object>();
				p.put("bench", TCPRoundTrip.class.getName());
				p.put("server", new InetSocketAddress(args[0], 20000));
				p.put("samples", 5000);
				p.put("payload", i);
				Result r = calib.runReal(p);
				rtt.addData(i, r.meanLatency);
				Result s = calib.runSimulated(p);
				rtt_s.addData(i, s.meanLatency);
			}
			
			calib.close();

			// Compute results
						
			logger.info("virtualization overhead: {}", new Linear(cpuoh.getIntercept(), cpuoh.getSlope()));
			logger.info("flood: real {} vs {}", new Linear(netCPU.getIntercept(), netCPU.getSlope()), new Linear(netCPU_s.getIntercept(), netCPU_s.getSlope()));
			logger.info("rtt: real {} vs {}", new Linear(rtt.getIntercept(), rtt.getSlope()), new Linear(rtt_s.getIntercept(), rtt_s.getSlope()));
		}
	}
}
