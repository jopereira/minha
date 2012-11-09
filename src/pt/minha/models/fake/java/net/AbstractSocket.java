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

package pt.minha.models.fake.java.net;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import pt.minha.api.World;
import pt.minha.models.global.net.Protocol;
import pt.minha.models.local.HostImpl;
import pt.minha.models.local.lang.SimulationThread;

public abstract class AbstractSocket {
	protected InetSocketAddress localSocketAddress;
	
	protected void addSocket(Protocol protocol, InetSocketAddress isa, AbstractSocket ab) throws SocketException {
		if ( null == isa )
			throw new IllegalArgumentException("bind: null address");
		if ( isa.getPort() < 0 || isa.getPort()> 0xFFFF )
			throw new IllegalArgumentException("bind: " + isa.getPort());
		if ( null == isa.getAddress() )
    		throw new IllegalArgumentException("bind: null address");
		
		if (isa.getAddress().equals(new InetSocketAddress(0).getAddress())) {
			HostImpl host = SimulationThread.currentSimulationThread().getHost();
			isa = host.getHostAvailableInetSocketAddress(isa.getPort());
		}
		else {
			try {
				InetAddress ia = InetAddress.getByName("0.0.0.0");
				if( isa.getAddress().equals(ia) ) {
					HostImpl host = SimulationThread.currentSimulationThread().getHost();
					isa = host.getHostAvailableInetSocketAddress(isa.getPort());
				}
			} catch (UnknownHostException e) {
				throw new IllegalArgumentException(e);
			}
		}
		
		this.localSocketAddress = World.networkMap.addSocket(protocol, isa, ab);
	}
	
	protected boolean existsSocket(Protocol protocol, InetSocketAddress isa) {
		if ( isa.getAddress().isLoopbackAddress() ) {
			HostImpl host = SimulationThread.currentSimulationThread().getHost();
			isa = new InetSocketAddress(host.getLocalAddress(), isa.getPort());
		}
		
		return World.networkMap.existsSocket(protocol, isa);
	}
	
	protected void removeSocket(Protocol protocol, InetSocketAddress isa) {
		World.networkMap.removeSocket(protocol, isa);
	}

	public SocketAddress getLocalSocketAddress() {
		return this.localSocketAddress;
	}
	
	public InetAddress getLocalAddress() {
		return this.localSocketAddress.getAddress();
	}

	public int getLocalPort() {
		return this.localSocketAddress.getPort();
	}
}
