/*
 * Copyright 2009 by Kappich Systemberatung, Aachen
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

package de.bsvrz.kex.tls.osi2osi3.redirection;

import de.bsvrz.sys.funclib.debug.Debug;

import java.util.ArrayList;
import java.util.List;

/**
 * Klasse mit Methoden zur Behandlung von OSI7 Telegrammen.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 7106 $
 * 
 */
public class TelegramStructure {
	
	private static final Debug _debug = Debug.getLogger();
	
	private int _nodeNumber = 0;
	
	List<byte[]> _singleTelegrams;


	
	/**
	 * Konstruktor. Als Parameter wird ein OSI7-Telegramm übergeben.
	 * 
	 * @param osi7bytes
	 *            Bytearray eines OSI7-Telegramm.
	 */
	public TelegramStructure(byte[] osi7bytes) {
		if(osi7bytes == null) {
			_debug.warning("Null als OSI7-Telegramm übergeben", osi7bytes);
		}
		else if(osi7bytes.length < 4) {
			_debug.warning("OSI7-Telegramm mit weniger als 4 Bytes übergeben", osi7bytes);
		}
		else {
			_nodeNumber = (((int)osi7bytes[0]) & 0xff) | ((((int)osi7bytes[1]) & 0xff) << 8) | ((((int)osi7bytes[2]) & 0xff) << 16);
			int count = osi7bytes[3] & 0xff;
			_singleTelegrams = new ArrayList<byte[]>();
			int telegramOffset = 4;
			for(int i = 0; i < count; i++) {
				int length = 1 + (osi7bytes[telegramOffset] & 0xff);
				byte[] singleTelegramBytes = new byte[length];
				System.arraycopy(osi7bytes, telegramOffset, singleTelegramBytes, 0, length);
				_singleTelegrams.add(singleTelegramBytes);
				telegramOffset += length;
			}
		}
	}
	
	/**
	 * Konstruktor. Als Parameter wird die Knotennummer übergeben.
	 * 
	 * @param nodeNumber
	 *            Knotennummer
	 */
	public TelegramStructure(int nodeNumber) {
		_nodeNumber = nodeNumber;
		_singleTelegrams = new ArrayList<byte[]>();
	}
	
	public void addSingleTelegram(byte[] singleTelegramBytes){
		_singleTelegrams.add(singleTelegramBytes);
	}

	/**
	 * Konstruktor. Als Parameter wird die Knotennummer und eine Liste der Einzeltelegramme übergeben.
	 * 
	 * @param nodeNumber
	 *            Knotennummer
	 * @param singleTelegrams
	 *            Liste Bytearrays der Einzeltelegramme
	 */
	public TelegramStructure(int nodeNumber, List<byte[]> singleTelegrams) {
		_nodeNumber = nodeNumber;
		_singleTelegrams = singleTelegrams;
	}

	
	/**
	 * Liefert die Funktionsgruppe des Einzeltelegramms zurück.
	 * 
	 * @param singleTelegram
	 *            Einzeltelegramm
	 * @return FG des Einzeltelegramms
	 */
	public int getFgFromSingleTelegram(byte[] singleTelegram) {
		return singleTelegram[1] & 0xff;
	}
	
	/**
	 * Gibt das OSI7 Telegramm als Byte-Array zurück.
	 * 
	 * @return OSI7 Telegramm als Byte-Array
	 */
	byte[] getTelegramBytes() {
		int size = 3 + 1;
		for(byte[] singleTelegram : _singleTelegrams) {
			size += singleTelegram.length;
		}
		byte[] telegram = new byte[size];
		telegram[0] = (byte)(_nodeNumber & 0xff);
		telegram[1] = (byte)((_nodeNumber >>> 8) & 0xff);
		telegram[2] = (byte)((_nodeNumber >>> 16) & 0xff);
		telegram[3] = (byte)_singleTelegrams.size();
		int destinationOffset = 4;
		for(byte[] singleTelegram : _singleTelegrams) {
			System.arraycopy(singleTelegram, 0, telegram, destinationOffset, singleTelegram.length);
			destinationOffset += singleTelegram.length;
		}
		return telegram;
	}
	
	/**
	 * Gibt die Knotennummer des OSI7-Telegramms zurück.
	 * 
	 * @return Knotennummer
	 */
	public int getNodeNumber() {
		return _nodeNumber;
	}
	
	/**
	 * Gibt die Einzeltelegramme des OSI7 Telegramms zurück.
	 * 
	 * @return Liste mit Bytearrays der Einzeltelegramme
	 */
	public List<byte[]> getSingleTelegrams() {
		return _singleTelegrams;
	}

	/**
     * @return
     */
    public int getNumberOfSingletelegrams() {
    	return _singleTelegrams.size();
    }
	
}
