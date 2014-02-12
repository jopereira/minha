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
package examples.net.socket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/*
 * Echo server/client over TCP.
 * 
 * MINHA command line:
 * java -cp lib/asm-all-3.3.1.jar:build/minha.jar sim.Runner examples.net.socket.EchoServer 5000, -d=1 examples.net.socket.EchoClient 10.0.0.1 5000
 * 
 */
public class EchoClient {
	public static void main(String[] args) throws IOException {
		String serverIP = args[0];
		int serverPort = Integer.parseInt(args[1]);
		
		Socket s = new Socket(serverIP, serverPort);
		System.out.println("Client: " + s.toString());
		
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
		BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
		
		for (int i=0; i<10; i++) {
			out.write(""+i);
			out.newLine();
			out.flush();
			String line = in.readLine();
			System.out.println("Client "+s.getLocalAddress()+" echo: " + line);
		}
		s.close();
	}
}
