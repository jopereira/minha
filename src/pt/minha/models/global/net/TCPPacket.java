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

package pt.minha.models.global.net;

public class TCPPacket implements Comparable<TCPPacket> {
	private final ClientTCPSocket source, destination;
	private final int seq, ack, control;
	private final byte[] data;
	
	public final static int RST=1, FIN=2; 
	
	public TCPPacket(ClientTCPSocket src, ClientTCPSocket dest, int seq, int ack, byte[] data, int control) {
		this.source = src;
		this.destination = dest;
		this.seq = seq;
		this.ack = ack;
		this.data = data;
		this.control = control;
	}
	
	public ClientTCPSocket getSource() {
		return source;
	}
	
	public ClientTCPSocket getDestination() {
		return destination;
	}

	public int getSequence() {
		return seq;
	}

	public int getAcknowledgement() {
		return ack;
	}
	
	public int getSize() {
		return data.length;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public int getControl() {
		return control;
	}
	
	public int compareTo(TCPPacket o) {
		return seq-o.seq;
	}
	
	public String toString() {
		return "{PACKET: "  
				+ "; SEQ: " + seq
				+ "; ACK: " + ack
				+ "; SIZE: " + getSize()
				+ "; CONTROL: " + control
				+ "}";
	}
}