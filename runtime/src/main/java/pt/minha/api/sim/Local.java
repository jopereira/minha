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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as being loaded by the host local system loader.
 * This is usually not necessary, as it is the default. It can be
 * used to override translation of individual features when 
 * implementing models. This class cannot be referenced by globals but
 * can reference any other local classes directly. Static fields will 
 * be local to each host instance.
 */
@Target(ElementType.TYPE)
@Retention(value=RetentionPolicy.CLASS)
public @interface Local {
	/**
	 * Translate synchronized primitives into simulation. Disabling this
	 * can lead to simulation deadlock.
	 * @return true to enable translation
	 */
	boolean synch() default true;

	/**
	 * Translate references to faked classes. Disabling this allows the
	 * class to reach outside the simulation sand-box, so use with
	 * care.
	 * @return true to enable translation
	 */
	boolean useFakes() default true;

	/**
	 * Translate references to faked classes. Disabling this allows the
	 * class to reach outside the simulation sand-box, so use with
	 * care.
	 * @return true to enable translation
	 */
	boolean useMoved() default true;
}
