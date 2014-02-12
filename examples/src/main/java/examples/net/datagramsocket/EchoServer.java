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
import java.net.DatagramSocket;
import java.net.InetAddress;


/*
 * Echo server/client over UDP.
 * 
 * MINHA command line:
 * java -cp lib/asm-all-3.3.1.jar:build/minha.jar sim.Runner examples.net.datagramsocket.EchoServer, -d=1 examples.net.datagramsocket.EchoClient 10.0.0.1
 * 
 */
class Server implements Runnable {
	@Override
	public void run() {
		try {
			DatagramSocket socket = new DatagramSocket(5000);
			byte[] buffer = new byte[1500];

			while(true) {
				DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
				socket.receive(receivePacket);

				InetAddress ia = receivePacket.getAddress();
				int port = receivePacket.getPort();
				DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, ia, port);
				socket.send(sendPacket);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}


public class EchoServer {

	public static void main(String[] args) {
		new Thread(new Server()).start();
	}
}
