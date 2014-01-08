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
import java.lang.management.ManagementFactory;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.Map;

public class UDPOverheadBenchmark extends AbstractBenchmark {
	
	private int usec;

	public void setParameters(Map<String,Object> p) {
		super.setParameters(p);
		if (p.containsKey("usec"))
			this.usec = (Integer) p.get("usec");
	}
	
	private void poison(DatagramSocket s, int v) throws IOException {
		s.setSoTimeout(1000);
		byte[] pill = new byte[1];
		pill[0]=(byte)v;
		for(int i = 0; i<10; i++) {
			DatagramPacket packet = new DatagramPacket(pill, 1, srv);
			s.send(packet);
		    try {
		        s.receive(packet);
		        break;
		    } catch (SocketTimeoutException e) {
		    	s.send(packet);
		    	continue;
		    }
		}
		s.setSoTimeout(0);
	}

	@Override
	public Object client() throws IOException, InterruptedException {
		DatagramSocket s = new DatagramSocket();
		byte[] buffer = new byte[payload];

		for (int i = 0; i < 10; i++) {
			DatagramPacket packet = new DatagramPacket(buffer, payload, srv);
			s.send(packet);
			if (usec>0)
				Thread.sleep(usec);
			System.nanoTime();
			ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
		}

		poison(s, 1);
		
		start();
		
		buffer[0]=2;

		for (int i = 0; i < samples; i++) {
			DatagramPacket packet = new DatagramPacket(buffer, payload, srv);
			s.send(packet);
			if (usec>0)
				Thread.sleep(usec);
			add(payload, System.nanoTime());
		}
		Result r = stop(true);

		poison(s, 3);

		s.close();

		return r;
	}

	@Override
	public Object server() throws IOException {
		DatagramSocket s = new DatagramSocket(srv);

		byte[] buffer = new byte[payload];

		while (true) {
			DatagramPacket packet = new DatagramPacket(buffer, payload, srv);
			s.receive(packet);
			if (packet.getData()[0] == 1) {
				s.send(packet);
				break;
			}
			System.nanoTime();
			ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
		}

		start();
		while (true) {
			DatagramPacket packet = new DatagramPacket(buffer, payload, srv);
			s.receive(packet);
			if (packet.getData()[0] == 1)
				continue;
			if (packet.getData()[0] == 3) {
				s.send(packet);
				break;
			}
			add(payload, System.nanoTime());
		}
		Result r = stop(true);

		s.close();

		return r;
	}

	public String toString() {
		return "UDP flooding "+super.toString();
	}
}
