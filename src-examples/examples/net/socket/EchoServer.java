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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/*
 * Echo server/client over TCP.
 * 
 * MINHA command line:
 * java -cp lib/asm-all-3.3.1.jar:build/minha.jar sim.Runner examples.net.socket.EchoServer 5000, -d=1 examples.net.socket.EchoClient 10.0.0.1 5000
 * 
 */
class Worker implements Runnable {
	private Socket s;
	
	public Worker(Socket s) {
		this.s = s;
	}
	
	@Override
	public void run() {
		try {
			System.out.println("Worker started: " + this.s.toString());
			
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));

			String line;
			while ((line = in.readLine()) != null) {
				out.write(line);
				out.newLine();
				out.flush();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}


class TCPServer implements Runnable {
	private ServerSocket ss;
	
	public TCPServer(int port, InetAddress bindAddr) throws IOException {
		this.ss = new ServerSocket(port, 0, bindAddr);
	}
	
	@Override
	public void run() {
		try {
			System.out.println(ss.toString() + " waiting connections...");
			while (true) {
				Socket s = ss.accept();
				System.out.println("Server: connection from " + s.toString());
				new Thread(new Worker(s)).start();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

}


public class EchoServer {
	public static void main(String[] args) throws IOException {
		int port = Integer.parseInt(args[0]);
		Thread s = new Thread(new TCPServer(port, null));
		s.start();
	}
}
