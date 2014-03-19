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

package pt.minha.test.model.io;

import static org.testng.Assert.assertEquals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.testng.annotations.Test;

import pt.minha.api.Entry;
import pt.minha.api.Host;
import pt.minha.api.Process;
import pt.minha.api.sim.Global;
import pt.minha.api.sim.Simulation;

public class FileStreamsTest {
	
	@Global
	public static interface Target {
		public void write(String file, String content) throws IOException;
		public String read(String file) throws IOException;
	}
	
	public static class Impl implements Target {

		@Override
		public void write(String file, String content) throws IOException {
			DataOutputStream fos = new DataOutputStream(new FileOutputStream(new File(file)));
			fos.writeUTF(content);
			fos.close();
		}

		@Override
		public String read(String file) throws IOException {
			DataInputStream fis = new DataInputStream(new FileInputStream(new File(file)));
			String content = fis.readUTF();			
			return content;
		}
	}
	
	@Test
	public void rw() throws Exception {
		Simulation world = new Simulation();
		
		final Host host = world.createHost();
		Process proc = host.createProcess();

		Entry<Target> en = proc.createEntry(Target.class, Impl.class.getName());
		
		String filename = "test.txt";
		String written = "*CONTENT*"+System.nanoTime()+"*CONTENT*";
		
		en.call().write(filename, written);
		String read = en.call().read(filename);
		
		world.close();
		
		assertEquals(read, written);
	} 
}
