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
public class EchoClient {

	public static void main(String[] args) throws Exception {
		DatagramSocket socket = new DatagramSocket();
		InetAddress ia = InetAddress.getByName(args[0]);

		for (int i=0; i<10; i++) {
			byte[] data = (""+i).getBytes();
			DatagramPacket sendPacket = new DatagramPacket(data, data.length, ia, 5000);
			socket.send(sendPacket);

			byte[] buffer = new byte[1500];
			DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
			socket.receive(receivePacket);
			System.out.println("echo: " + new String(buffer));
		}
	}
}
