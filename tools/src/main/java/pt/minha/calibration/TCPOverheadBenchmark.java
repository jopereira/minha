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

package pt.minha.calibration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

public class TCPOverheadBenchmark extends AbstractBenchmark {
	
	private int usec;

	public void setParameters(Map<String,Object> p) {
		super.setParameters(p);
		if (p.containsKey("usec"))
			this.usec = (Integer) p.get("usec");
	}

	@Override
	public Object client() throws IOException, InterruptedException {
		Socket s = new Socket(srv.getAddress(), srv.getPort());
		OutputStream out = s.getOutputStream();
		byte[] buffer = new byte[payload];

		for (int i = 0; i < 10; i++) {
			out.write(buffer);
			if (usec>0)
				Thread.sleep(usec);
			System.nanoTime();
			ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
		}

		start();

		for (int i = 0; i < samples; i++) {
			out.write(buffer);
			if (usec>0)
				Thread.sleep(usec);
			add(payload, System.nanoTime());
		}
		Result r = stop(true);
	
		s.close();

		return r;
	}

	@Override
	public Object server() throws IOException {
		ServerSocket ss = new ServerSocket(srv.getPort());
		Socket s = ss.accept();

		InputStream in = s.getInputStream();
		byte[] buffer = new byte[payload];

		for (int i = 0; i < 10; i++) {
			if (!readFully(in, buffer))
				break;
			System.nanoTime();
			ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
		}

		start();
		for (int i = 0; i < samples; i++) {
			if (!readFully(in, buffer))
				break;
			add(payload, System.nanoTime());
		}
		Result r = stop(true);

		s.close();
		ss.close();

		return r;
	}

	public String toString() {
		return "TCP flooding "+super.toString();
	}
}
