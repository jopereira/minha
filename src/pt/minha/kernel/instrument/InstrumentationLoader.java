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

package pt.minha.kernel.instrument;

import java.io.InputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.RemappingClassAdapter;

public class InstrumentationLoader extends ClassLoader {
	
	private ClassConfig cc;
	
	public InstrumentationLoader(ClassConfig cc) {
		this.cc = cc;
	}

	public Class<?> loadClass(String name) throws ClassNotFoundException {
		
		String effname = name.replace('.', '/');
		ClassConfig.Action act = cc.get(effname);

		if (act.equals(ClassConfig.Action.invalid))
			throw new ClassCastException("class marked as invalid");

		/*
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
		
		/*
		 * Set up translation configuration defaults. This depends on
		 * the class name prefix (i.e. fake or moved), properties file,
		 * and class file annotations.
		 */
		Translation trans = new Translation(effname, act);
		
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
			// (order is: last in, first used)
			
			// Handle readObject/writeObject in Serializable classes
			ca = new SerializableClassVisitor(ca, trans);

			// Rewrite some special methods
			ca = new MethodRemapperClassVisitor(ca, trans);
				
			// Rewrite MONITORENTER/MONITORLEAVE to methods in fake.j.l.Object 
			ca = new FakeMonitorClassVisitor(ca, trans);
				
			// Rewrite synchronized methods to synchronized blocks
			ca = new SyncToMonitorClassVisitor(ca, trans);

			// Redirect references to fake.* and moved.* classes
			ca = new RemappingClassAdapter(ca, new ClassRemapper(cc, trans));
					
			// Prepare for changes done by other stages
			ca = new JSRInlinerClassVisitor(ca);
					
			// Update translation config from annotations (this is the first
			// stage in the pipeline!)
			ca = new AnnotatedClassVisitor(ca, trans); 
			// ---------------------------------------------------------------------------------------
			
			InputStream is = getResourceAsStream(trans.getFileName());
			ClassReader cr = new ClassReader(is);
			cr.accept(ca, ClassReader.SKIP_FRAMES);
			byte[] b2 = cw.toByteArray();
			
			// Enable this to get debugging output:
			//checkAndDumpClass(b2);
			
			if (trans.isGlobal())
				return super.loadClass(name);
			else
				return defineClass(name, b2, 0, b2.length);
			
		} catch (Exception e) {
			throw new ClassNotFoundException(name, e);
		}
	}

	public Class<?> forName(String name) throws ClassNotFoundException {
		Translation trans = new Translation("foobar", ClassConfig.Action.translate);
		ClassRemapper remapAll = new ClassRemapper(cc, trans);
		return loadClass(remapAll.map(name));
	}
	
	/*private static void checkAndDumpClass(byte[] buf) {
		try {
			ClassReader cr = new ClassReader(new ByteArrayInputStream(buf));
			cr.accept(new CheckClassAdapter(new TraceClassVisitor(new PrintWriter(System.out))), ClassReader.EXPAND_FRAMES);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}*/
}
