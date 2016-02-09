/*
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2004 by Kappich+Kniß Systemberatung Aachen (K2S)
 * 
 * This file is part of de.bsvrz.kex.tls.osi2osi3.
 * 
 * de.bsvrz.kex.tls.osi2osi3 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * de.bsvrz.kex.tls.osi2osi3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.bsvrz.kex.tls.osi2osi3; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.bsvrz.kex.tls.osi2osi3.osi2.wancom;

import de.bsvrz.kex.tls.osi2osi3.osi2.api.AbstractDataLinkLayer;

import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * Abstrakte Klasse, welche die für Client und Server identischen Funktionen des WanCom Protokolls enthält.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 5114 $
 */
abstract class WanCom extends AbstractDataLinkLayer {

	public WanCom() {
	}

	protected String getDefaultProperty(String name) {
		return DefaultProperties.getInstance().getProperty(name);
	}

	public int getMaximumDataSize() {
		return 253;
	}


	protected byte[] createKeepAliveTelegramBytes(int version, int type, InetAddress localAddress) {
		int size = 43;
		ByteBuffer buffer = ByteBuffer.allocate(43);
		buffer.putInt(version);
		buffer.putInt(size);
		buffer.putInt(type);
		buffer.putInt(0);
		buffer.putInt(0);
		byte[] ip = localAddress.getAddress();
		byte[] ip8 = new byte[8];
		System.arraycopy(ip, 0, ip8, 0, Math.min(ip.length, ip8.length));
		buffer.put(ip8);
		buffer.put((byte)9);
		buffer.put((byte)255);
		buffer.put((byte)255);
		buffer.put((byte)0);
		buffer.put((byte)0);
		buffer.put((byte)0);
		buffer.put((byte)1);
		buffer.put((byte)7);
		buffer.put((byte)134);
		buffer.put((byte)2);
		buffer.put((byte)0);
		buffer.put((byte)1);
		buffer.put((byte)2);
		buffer.put((byte)255);
		buffer.put((byte)130);
		return buffer.array();
	}
}
