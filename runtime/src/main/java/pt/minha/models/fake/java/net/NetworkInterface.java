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

package pt.minha.models.fake.java.net;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import pt.minha.models.global.net.NetworkStack;
import pt.minha.models.local.lang.SimulationThread;

public class NetworkInterface {

	private InetAddress ipAddr;
	private byte[] macAddr;

	public NetworkInterface(byte[] macAddr, InetAddress ipAddr) {
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
		NetworkStack net = SimulationThread.currentSimulationThread().getProcess().getNetwork(); 
		aux.add(new NetworkInterface(net.getMACAddress(), net.getLocalAddress()));
		return aux.elements();
	}

	public List<InterfaceAddress> getInterfaceAddresses() {
		NetworkStack net = SimulationThread.currentSimulationThread().getProcess().getNetwork();
		return Collections.singletonList(new InterfaceAddress(net));
	}
	
	public static NetworkInterface getByInetAddress(InetAddress addr) {
		NetworkStack net = SimulationThread.currentSimulationThread().getProcess().getNetwork();
		return new NetworkInterface(net.getMACAddress(), net.getLocalAddress());
	}
	
	public String getName() {
		return "eth0";
	}
	
	public String getDisplayName() {
		return getName();
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

	public boolean isVirtual() { return false; }

	public byte[] getHardwareAddress() {
		return macAddr;
	}
	
	public static NetworkInterface getByName(String name) {
		Enumeration<NetworkInterface> i = getNetworkInterfaces();
		while(i.hasMoreElements()) {
			NetworkInterface next = i.nextElement();
			if (next.getName().equals(name))
				return next;
		}
		return null;
	}
	
	public String toString() {
		String addr = getName()+"/"+macAddr[0];
		for(int i=1;i<macAddr.length;i++)
			addr+=":"+Integer.toHexString(macAddr[i]);
		return addr;
	}
}