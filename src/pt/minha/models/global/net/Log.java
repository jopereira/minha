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

import pt.minha.kernel.log.Logger;
import pt.minha.kernel.log.LoggerLog4j;
import pt.minha.kernel.util.PropertiesLoader;

public class Log {
	/*
	 * TCP
	 */
	private static Logger network_tcp_logger;
	public static boolean network_tcp_log_enabled = false;
	public static boolean network_tcp_stream_log_enabled = false;
	static {
		try {
			PropertiesLoader conf = new PropertiesLoader(Logger.LOG_CONF_FILENAME);
			network_tcp_log_enabled = Boolean.parseBoolean(conf.getProperty("network.tcp.log"));
			network_tcp_stream_log_enabled = Boolean.parseBoolean(conf.getProperty("network.tcp.stream.log"));
			if ( network_tcp_log_enabled || network_tcp_stream_log_enabled )
				network_tcp_logger = new LoggerLog4j("log/network-tcp.log");
		} catch (Exception e) {
			// log will be disabled
		}
	}
	
	public static void TCPdebug(String message) {
		network_tcp_logger.debug(message);
	}
}
