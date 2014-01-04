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

package pt.minha.models.local;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import pt.minha.api.Main;
import pt.minha.models.local.lang.SimulationThread;

public class MainEntry implements Main {
	@Override
	public void main(String impl, String[] args) throws Throwable {
		// Run main
		Class<?> claz=Class.forName(impl);
		Method m = claz.getMethod("main", new String[0].getClass());
		
		try {
			m.invoke(claz, (Object)args);
		} catch(InvocationTargetException ite) {
			throw ite.getTargetException();
		} finally {
			// For for all remaining non-daemon threads to stop
			pt.minha.models.fake.java.lang.Thread.currentThread().setDaemon(true);
			SimulationThread.currentSimulationThread().getProcess().stop();
		}
	}
}
