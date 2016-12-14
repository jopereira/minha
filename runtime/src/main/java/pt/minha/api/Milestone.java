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

package pt.minha.api;

/**
 * A milestone that can be waited on. The system is run
 * until a set of milestones is achieved. Milestones are situations in
 * which system returns control to the driver code: Either the 
 * return from entry invocations or the invocation of exit objects.
 */
public interface Milestone {

	/**
	 * Tests if the milestone has been initiated and is pending.
	 * If true, it will eventually be completed.
	 *
	 * @return true if pending
	 */
	public boolean isPending();

	/**
	 * Tests if the milestone has been completed.
	 * 
	 * @return true if complete
	 */
	public boolean isComplete();

}