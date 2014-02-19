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

package pt.minha.api.sim;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import pt.minha.api.Entry;
import pt.minha.api.Exit;
import pt.minha.api.Host;
import pt.minha.api.Main;
import pt.minha.api.Process;
import pt.minha.api.ContainerException;
import pt.minha.kernel.instrument.ClassConfig;
import pt.minha.kernel.instrument.InstrumentationLoader;
import pt.minha.kernel.simulation.Resource;
import pt.minha.models.global.EntryInterface;
import pt.minha.models.global.disk.Storage;
import pt.minha.models.global.net.NetworkStack;
import pt.minha.models.local.MainEntry;

class ProcessImpl implements Process {
	HostImpl host;
	InstrumentationLoader loader;
	EntryInterface impl;

	ProcessImpl(HostImpl h, ClassConfig cc, NetworkStack network, Resource cpu, Storage storage, Properties sysProps) throws ContainerException {
		this.host = h;
		
		// Copy properties to a Map, as java.util.Properties is "moved" and cannot
		// be given to a different class loader.
		Map<Object,Object> props = new HashMap<Object, Object>();
		if (sysProps != null)
			props.putAll(sysProps);
		else
			props.putAll(System.getProperties());

		loader=new InstrumentationLoader(cc);
		try {
			Class<?> clz = loader.loadClass("pt.minha.models.local.SimulationProcess");
			impl = (EntryInterface) clz.getDeclaredConstructor(Host.class, Resource.class, NetworkStack.class, Storage.class, Map.class).newInstance(host, cpu, network, storage, props);
		} catch(Exception e) {
			throw new ContainerException(e);
		}
	}
	
	@Override
	public <T> Entry<T> createEntry(Class<T> intf, String impl) throws IllegalArgumentException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		return new EntryImpl<T>(this, intf, impl);
	}

	@Override
	public Entry<Main> createEntry() throws IllegalArgumentException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		return new EntryImpl<Main>(this, Main.class, MainEntry.class.getName());
	}
	
	@Override
	public <T> Exit<T> createExit(Class<T> intf, T impl) {
		return new ExitImpl<T>(this, intf, impl);
	}
	
	@Override
	public Host getHost() {
		return host;
	}

	@Override
	public void close() throws IOException {
		host.removeProcess(this);
		impl.close();
	}
}
