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

package pt.minha.models.global.net;


public abstract class TCPPacket {
	private final PacketType type;
	private final String key;	// destination 
	private final int sn;		// sequence number
	private final int size;
	
	public TCPPacket(PacketType type, String key, int sn, int size) {
		this.type = type;
		this.key = key;
		this.sn = sn;
		this.size = size;
	}

	public PacketType getType() {
		return this.type;
	}
	
	public String getKey() {
		return this.key;
	}

	public int getSn() {
		return this.sn;
	}
	
	public int getSize() {
		return this.size;
	}
	
	public String toString() {
		return "\n{PACKET: " + this.key 
				+ "; SN: " + this.sn
				+ "; TYPE: " + this.type
				+ "; SIZE: " + this.size
				+ "}";
	}
}
