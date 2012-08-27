/*
 * Minha.pt: middleware testing platform.
 * Copyright (c) 2011-2012, Universidade do Minho.
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

package pt.minha.models.global.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class NetworkMap {
	private static final NetworkMapInstance impl = new NetworkMapInstance();
	
	public static String generateMACAddress() {
		return impl.generateMACAddress();
	}
	
	public static InetAddress addHost(String host) throws UnknownHostException {
		return impl.addHost(host);
	}
	
	public static InetAddress getAvailableInetAddress() throws UnknownHostException {
		return impl.getAvailableInetAddress();
	}
	
	public static boolean existsHost(InetAddress ia) {
		return impl.existsHost(ia);
	}
	
	public static InetSocketAddress addSocket(Protocol protocol, InetSocketAddress isa, Object ab) throws SocketException {
		return impl.addSocket(protocol, isa, ab);
	}
	
	public static boolean existsSocket(Protocol protocol, InetSocketAddress isa) {
		return impl.existsSocket(protocol, isa);
	}
	
	public static void removeSocket(Protocol protocol, InetSocketAddress isa) {
		impl.removeSocket(protocol, isa);
	}
	
	protected static void DatagramPacketQueue(InetSocketAddress destination, DatagramPacket packet) {
		impl.DatagramPacketQueue(destination, packet);
	}
	
	public static boolean isServerSocket(InetSocketAddress destination) {
		return impl.isServerSocket(destination);
	}
	
	public static String ServerSocketConnect(InetSocketAddress destination, InetSocketAddress source, Object clientSocket) throws IOException {
		return impl.ServerSocketConnect(destination, source, clientSocket);
	}
	
	public static String SocketScheduleServerSocketAcceptDone(InetSocketAddress destination, InetSocketAddress source, Object serverClientSocket) throws IOException {
		return impl.SocketScheduleServerSocketAcceptDone(destination, source, serverClientSocket);
	}
	
	protected static void SocketScheduleRead(String key, TCPPacket p) throws IOException {
		impl.SocketScheduleRead(key, p);
	}
	
	protected static void SocketAcknowledge(String key, TCPPacketAck p) {
		impl.SocketAcknowledge(key, p);
	}
	
	public static void removeConnectedSocket(String key) {
		impl.removeConnectedSocket(key);
	}

	/* debug methods */
	public static int getSocketInputStreamSN(String key) throws IOException {
		return impl.getSocketInputStreamSN(key);
	}
	
	public static int getSocketOutputStreamSN(String key) throws IOException {
		return impl.getSocketOutputStreamSN(key);
	}
	
	public static void checkForgottenSockets() {
		impl.checkForgottenSockets();
	}
}
