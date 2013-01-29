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

package pt.minha.models.fake.java.net;

import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Vector;

import pt.minha.models.global.net.Network;
import pt.minha.models.global.net.NetworkStack;
import pt.minha.models.local.lang.SimulationThread;

public class NetworkInterface {

	private InetAddress ipAddr;
	private String macAddr;

	public NetworkInterface(String macAddr, InetAddress ipAddr) {
		this.macAddr = macAddr;
		this.ipAddr = ipAddr;
	}
	
	public Enumeration<InetAddress> getInetAddresses() {
		Vector<InetAddress> aux = new Vector<InetAddress>();
		aux.add(ipAddr);
		return aux.elements();
	}
	
	public static Enumeration<NetworkInterface> getNetworkInterfaces() {
		Vector<NetworkInterface> aux = new Vector<NetworkInterface>();
		NetworkStack net = SimulationThread.currentSimulationThread().getHost().getNetwork(); 
		aux.add(new NetworkInterface(net.getMACAddress(), net.getLocalAddress()));
		return aux.elements();
	}
	
	public String getName() {
		return "eth0";
	}
	
	public boolean supportsMulticast() {
		return true;
	}
	
	public boolean isUp() {
		return true;
	}
	
	public boolean isLoopback() {
		return false;
	}
	
	public String toString() {
		return getName()+"/"+macAddr;
	}
}