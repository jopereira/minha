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

package pt.minha.kernel.instrument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Translation {
	private boolean global;
	private boolean usingFakes;
	private boolean usingMoved;
	private boolean synch;
	private String name;
	private Logger logger;

	public Translation(String name, ClassConfig.Action action) {
		this.name = name;

		/**
		 * We recognize moved and faked classes from the prefix that has 
		 * been placed there by the remapper.
		 */
		if (name.startsWith(ClassConfig.fake_prefix))
			usingMoved = true;
		else if (name.startsWith(ClassConfig.moved_prefix)) {
			usingMoved = true;
			usingFakes = true;
			synch = true;
			this.name = name.substring(ClassConfig.moved_prefix.length());
		} else if (!action.equals(ClassConfig.Action.load)) {			
			usingMoved = true;
			usingFakes = true;
			synch = true;
		}

		this.logger = LoggerFactory.getLogger("MINHA."+name.replace('/','.'));
	}
	
	public boolean isGlobal() {
		return global;
	}

	public void setGlobal(boolean global) {
		this.global = global;
	}

	public boolean isUsingFakes() {
		return usingFakes;
	}

	public void setUsingFakes(boolean usingFakes) {
		this.usingFakes = usingFakes;
	}

	public boolean isUsingMoved() {
		return usingMoved;
	}

	public void setUsingMoved(boolean usingMoved) {
		this.usingMoved = usingMoved;
	}

	public boolean isSynchronized() {
		return synch;
	}

	public void setSynchronized(boolean synch) {
		this.synch = synch;
	}
	
	public String getFileName() {
		return name+".class";
	}

	public Logger getLogger() { return logger; }
	
	public String toString() {
		if (global)
			return "@Global class "+name;
		else
			return "@Local(synch="+synch+",useFaked="+usingFakes+",useMoved="+usingMoved+") class "+name;
	}
}
