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
package examples.net.datagramsocket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;


/*
 * UDP Multicast example.
 * 
 * MINHA command line:
 * java -cp lib/asm-all-3.3.1.jar:build/minha.jar sim.Runner examples.net.datagramsocket.MulticastA, -d=1 examples.net.datagramsocket.MulticastB
 * 
 */
class B implements Runnable {
	@Override
	public void run() {
		try {
			String msg = "Hello";
			InetAddress group = InetAddress.getByName("228.5.6.7");
			MulticastSocket s = new MulticastSocket(6789);
			s.joinGroup(group);

			DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(),
					group, 6789);
			s.send(hi);
			System.out.println(s.getLocalAddress().toString() + " sent: " + msg);

			// get their responses!
			byte[] buf = new byte[1000];
			DatagramPacket recv = new DatagramPacket(buf, buf.length);
			s.receive(recv);
			System.out.println(s.getLocalAddress().toString() + " received: " + new String(recv.getData()));
			
			// OK, I'm done talking - leave the group...
			s.leaveGroup(group);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}


public class MulticastB {

	public static void main(String[] args) throws Exception {
		new Thread(new B()).start();
	}
}
