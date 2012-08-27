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

package pt.minha.kernel.instrument;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

public class InstrumentationLoader extends ClassLoader {
	
	private ClassConfig cc;
	private Remapper remapAll = new Remapper() {
		@Override
		public String map(String type) {
			ClassConfig.Action act = cc.get(type);
			
			if (act.equals(ClassConfig.Action.fake))
				return ClassConfig.fake_prefix+type;
			else if (act.equals(ClassConfig.Action.moved))
				return ClassConfig.moved_prefix+type;
			else
				return type;
		}
	}; 
	private Remapper remapMoved = new Remapper() {
		@Override
		public String map(String type) {
			ClassConfig.Action act = cc.get(type);
			
			if (act.equals(ClassConfig.Action.moved))
				return ClassConfig.moved_prefix+type;
			else
				return type;
		}
	}; 

	public InstrumentationLoader(ClassConfig cc) {
		this.cc = cc;
	}

	public Class<?> loadClass(String name) throws ClassNotFoundException {
		
		String effname = name.replace('.', '/');
		ClassConfig.Action act = cc.get(effname);

		if (act.equals(ClassConfig.Action.invalid))
			throw new ClassCastException("class marked as invalid");

		/**
		 * Moved and faked classes arrive with the prefixed name. If it
		 * gets here without the prefix, then it should be handled as a
		 * global as the reference comes from a fake, load or global.
		 */
		if (act.equals(ClassConfig.Action.global) ||
			act.equals(ClassConfig.Action.fake) ||
			act.equals(ClassConfig.Action.moved))
			return super.loadClass(name);
					
		Class<?> claz = findLoadedClass(name);
		if (claz!=null)
			return claz;
		
		/**
		 * We recognize moved and faked classes from the prefix that has 
		 * been placed there by the remapper.
		 */
		if (effname.startsWith(ClassConfig.fake_prefix))
			act = ClassConfig.Action.fake;
		else if (effname.startsWith(ClassConfig.moved_prefix)) {
			act = ClassConfig.Action.moved;
			effname = effname.substring(ClassConfig.moved_prefix.length());
		}
		
		try {
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
				// This is most likely wrong, but the orginal method is also wrong, as
				// it relies on loading classes (which escape the sandbox).
				protected String getCommonSuperClass(String type1, String type2) {
					return "java/lang/Object";
				}
			};
			ClassVisitor ca = cw;

			// ------ This is the bytecode re-writting pipeline: -------
			if (!act.equals(ClassConfig.Action.load)) {
				if (act.equals(ClassConfig.Action.fake)) {
					
					// 1. Redirect references to fake.* and moved.* classes
					ca = new RemappingClassAdapter(ca, remapMoved);
					
				} else { // move and translate
					
					// 5. Handle readObject/writeObject in Serializable classes
					ca = new SerializableClassVisitor(ca);

					// 4. Rewrite some special methods
					ca = new MethodRemapperClassVisitor(ca);
				
					// 3. Rewrite MONITORENTER/MONITORLEAVE to methods in fake.j.l.Object 
					ca = new FakeMonitorClassVisitor(ca);
				
					// 2. Rewrite synchronized methods to synchronized blocks
					ca = new SyncToMonitorClassVisitor(ca);

					// 1. Redirect references to fake.* and moved.* classes
					ca = new RemappingClassAdapter(ca, remapAll);

					ca = new JSRInlinerClassVisitor(ca);
				}				
			}
			// ---------------------------------------------------------------------------------------
			
			String resource = effname + ".class";
			InputStream is = getResourceAsStream(resource);
			ClassReader cr = new ClassReader(is);
			cr.accept(ca, ClassReader.SKIP_FRAMES);
			byte[] b2 = cw.toByteArray();
			
			// Enable this to get debugging output:
			//checkAndDumpClass(b2);
			
			return defineClass(name, b2, 0, b2.length);
		} catch (Exception e) {
			throw new ClassNotFoundException(name, e);
		}
	}

	public Class<?> forName(String name) throws ClassNotFoundException {
		return loadClass(remapAll.map(name));
	}
	
	private static void checkAndDumpClass(byte[] buf) {
		try {
			ClassReader cr = new ClassReader(new ByteArrayInputStream(buf));
			cr.accept(new CheckClassAdapter(new TraceClassVisitor(new PrintWriter(System.out))), ClassReader.EXPAND_FRAMES);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
}
