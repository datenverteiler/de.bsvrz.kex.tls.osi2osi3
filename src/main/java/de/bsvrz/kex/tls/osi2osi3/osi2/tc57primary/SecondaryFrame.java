/*
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2004 by Kappich+Kniß Systemberatung, Aachen
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
import de.bsvrz.sys.funclib.hexdump.HexDumper;

/**
 * Klasse zum Zugriff auf den Inhalt eines Secondary-Telegramms.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 7727 $
 */
public class SecondaryFrame extends Tc57Frame {

	private static final Debug _debug = Debug.getLogger();

	public static final int ANR1 = 0;

	public static final int REJ1 = 1;

	public static final int D = 8;

	public static final int ANR2 = 9;

	public static final int S1 = 11;

	public static final int REJ2 = 10;

	public static final int TD1 = 15;

	/** Spezieller Funktionscode, der bei Empfang eines SingleCharacterFrames eingetragen wird */
	public static final int E5 = 16;

	public SecondaryFrame(boolean accessDemand, boolean dataFlowControl, int function, int address, byte[] data) {
		super(false, accessDemand, dataFlowControl, function, address, data);
	}


	public byte[] getFrameBytes() {
		if(getFunction() == E5) {
			return new byte[]{(byte)0xe5};
		}
		return super.getFrameBytes();
	}

	public static SecondaryFrame parseFrame(int maximumSmartSkipCount, byte[] frameBytes) {
		if(frameBytes == null) {
			_debug.fine("no byte array to build frame");
			return null;
		}
		int startIndex = 0;
		int endIndex = 0;
		if(maximumSmartSkipCount > frameBytes.length) maximumSmartSkipCount = frameBytes.length;
		while(startIndex < maximumSmartSkipCount) {
			int b = frameBytes[startIndex] & 0xff;
			if(b == 0xe5) break;
			if(b == 0x10) {
				if(startIndex + 4 < frameBytes.length) {
					if(frameBytes[startIndex + 4] == 0x16) break;
				}
			}
			if(b == 0x68) {
				if(startIndex + 3 < frameBytes.length) {
					if(frameBytes[startIndex + 1] == frameBytes[startIndex + 2] && frameBytes[startIndex + 3] == 0x68) break;
				}
			}
			++startIndex;
		}
		SecondaryFrame result = null;
		if(startIndex < frameBytes.length) {
			int b = frameBytes[startIndex] & 0xff;
			if(b == 0xe5) {
				//Quittungszeichen
				endIndex = startIndex + 1;
				result = new SecondaryFrame(false, false, E5, 0, null);
			}
			else if(b == 0x10) {
				// Kurztelegramm
				if(startIndex + 4 < frameBytes.length && frameBytes[startIndex + 4] == 0x16 && ((frameBytes[startIndex + 1] & 0x40) == 0)) {
					int controlByte = frameBytes[startIndex + 1] & 0xff;
					int addressByte = frameBytes[startIndex + 2] & 0xff;
					int checkSum = frameBytes[startIndex + 3] & 0xff;
					if(((controlByte + addressByte) & 0xff) == checkSum) {
						endIndex = startIndex + 5;
						result = new SecondaryFrame((controlByte & 0x20) != 0, (controlByte & 0x10) != 0, controlByte & 0x0f, addressByte, null);
					}
				}
			}
			else if(b == 0x68) {
				// Langtelegramm
				if(startIndex + 3 < frameBytes.length && frameBytes[startIndex + 3] == 0x68) {
					int length1 = frameBytes[startIndex + 1] & 0xff;
					int length2 = frameBytes[startIndex + 2] & 0xff;
					if(length1 == length2 && length1 >= 2) {
						if(((startIndex + 5 + length1) < frameBytes.length) && frameBytes[startIndex + 5 + length1] == 0x16 && (
								(frameBytes[startIndex + 4] & 0x40) == 0)) {
							int controlByte = frameBytes[startIndex + 4] & 0xff;
							int addressByte = frameBytes[startIndex + 5] & 0xff;
							int checkSum = frameBytes[startIndex + 4 + length1] & 0xff;
							int calculatedCheckSum = 0;
							for(int i = 0; i < length1; ++i) {
								calculatedCheckSum += (frameBytes[startIndex + 4 + i] & 0xff);
							}
							calculatedCheckSum &= 0xff;
							if(calculatedCheckSum == checkSum) {
								byte[] data = new byte[length1 - 2];
								for(int i = 0; i < data.length; ++i) {
									data[i] = frameBytes[startIndex + 6 + i];
								}
								endIndex = startIndex + 6 + length1;
								result = new SecondaryFrame((controlByte & 0x20) != 0, (controlByte & 0x10) != 0, controlByte & 0x0f, addressByte, data);
							}
						}
					}
				}
			}
		}
		if(result == null) {
			_debug.fine("Ignoring received frameBytes:" + HexDumper.toString(frameBytes));
		}
		else {
			if(startIndex > 0) {
				_debug.fine("Skipping received prefix bytes: " + HexDumper.toString(frameBytes, 0, startIndex));
			}
			if(endIndex < frameBytes.length) {
				_debug.fine("Skipping received suffix bytes: " + HexDumper.toString(frameBytes, endIndex, frameBytes.length - endIndex));
			}
		}
		return result;
	}


	public boolean hasAccessDemand() {
		return getControlBit5();
	}

	public boolean hasDataFlowControl() {
		return getControlBit4();
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("SecondaryFrame ");
		result.append(hasAccessDemand() ? "ACD:1" : "acd:0").append(", ");
		result.append(hasDataFlowControl() ? "DFC:1" : "dfc:0");
		result.append(", function:").append(getFunction()).append(":").append(getFunctionName());
		result.append(", address:").append(getAddress()).append(", ");
		byte[] data = getData();
		if(data == null) {
			result.append("no data");
		}
		else {
			result.append(data.length).append(" data bytes:");
			result.append(System.getProperty("line.separator"));
			result.append(HexDumper.toString(data));
		}
		return result.toString();
	}

	private String getFunctionName() {
		switch(getFunction()) {
			case ANR1:
				return "ANR1";
			case REJ1:
				return "REJ1";
			case D:
				return "D";
			case ANR2:
				return "ANR2";
			case S1:
				return "S1";
			case REJ2:
				return "REJ2";
			case TD1:
				return "TD1";
			case E5:
				return "ANR";
			default:
				return "???";
		}
	}
}

