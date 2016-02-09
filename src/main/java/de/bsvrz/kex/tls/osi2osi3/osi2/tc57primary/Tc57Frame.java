/*
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2004 by Kappich+Kniﬂ Systemberatung, Aachen
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

package de.bsvrz.kex.tls.osi2osi3.osi2.tc57primary;


import de.bsvrz.sys.funclib.debug.Debug;

/**
 * Klasse zum Zugriff auf den Inhalt eines TC57-Telegramms.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 5114 $
 */
public class Tc57Frame {

	private static final Debug _debug = Debug.getLogger();

	private final boolean _primaryMessage;

	private final boolean _controlBit5;

	private final boolean _controlBit4;

	private final int _function;

	private final int _address;

	private final byte[] _data;

	Tc57Frame(boolean primaryMessage, boolean controlBit5, boolean controlBit4, int function, int address, byte[] data) {
		if(function < 0 || function > 16) throw new IllegalArgumentException("function sollte zwischen 0 und 15 liegen, ist: " + function);
		if(address < 0 || address > 255) throw new IllegalArgumentException("address sollte zwischen 0 und 255 liegen, ist: " + address);
		if(data != null && data.length > 253) throw new IllegalArgumentException("Anzahl Nutzdatenbytes sollte zwischen 0 und 253 liegen, ist: " + data.length);
		_primaryMessage = primaryMessage;
		_controlBit5 = controlBit5;
		_controlBit4 = controlBit4;
		_function = function;
		_address = address;
		_data = data;
	}

	public boolean isPrimaryMessage() {
		return _primaryMessage;
	}

	public boolean getControlBit5() {
		return _controlBit5;
	}

	public boolean getControlBit4() {
		return _controlBit4;
	}

	public int getFunction() {
		return _function;
	}

	public int getAddress() {
		return _address;
	}

	public byte[] getData() {
		return _data;
	}

	public byte[] getFrameBytes() {
		byte[] data = getData();
		int controlByte = getFunction();
		if(isPrimaryMessage()) controlByte |= 0x40;
		if(getControlBit5()) controlByte |= 0x20;
		if(getControlBit4()) controlByte |= 0x10;
		if(data == null && (controlByte == 0 || controlByte == 9)) {
			//Quittungszeichen
			return new byte[]{(byte)0xe5};
		}
		byte[] frameBytes;
		int controlByteIndex;
		int dataLength = 0;
		int length = 2;
		if(data == null) {
			//Kurztelegramm
			frameBytes = new byte[5];
			frameBytes[0] = 0x10;
			controlByteIndex = 1;
		}
		else {
			//Langtelegramm
			dataLength = data.length;
			length = dataLength + 2;
			frameBytes = new byte[length + 6];
			frameBytes[0] = 0x68;
			frameBytes[1] = (byte)length;
			frameBytes[2] = (byte)length;
			frameBytes[3] = 0x68;
			controlByteIndex = 4;
		}
		int checksum = 0;
		byte b;
		b = (byte)controlByte;
		frameBytes[controlByteIndex] = b;
		checksum += b;
		b = (byte)getAddress();
		frameBytes[controlByteIndex + 1] = b;
		checksum += b;
		for(int i = 0; i < dataLength; ++i) {
			b = data[i];
			frameBytes[controlByteIndex + 2 + i] = b;
			checksum += b;
		}
		frameBytes[controlByteIndex + length] = (byte)checksum;
		frameBytes[controlByteIndex + length + 1] = 0x16;
		return frameBytes;
	}
}
