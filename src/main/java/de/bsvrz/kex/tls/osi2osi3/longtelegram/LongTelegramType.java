/*
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2005 by Kappich+Kniß Systemberatung Aachen (K2S)
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

package de.bsvrz.kex.tls.osi2osi3.longtelegram;

/**
 * Diese Klasse stellt Objekte zur Verfügung, die die verschiedenen Zustände eines Langtelegramms darstellen.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 5114 $
 */
public class LongTelegramType {

	public static final LongTelegramType RESERVERD = new LongTelegramType("Reserviert", 0);

	public static final LongTelegramType SHORT_BLOCK = new LongTelegramType("ShortBlock", 1);

	public static final LongTelegramType START_BIG_BLOCK = new LongTelegramType("StartBigBlock", 2);

	public static final LongTelegramType NEXT_BIG_PIECE = new LongTelegramType("NextBigPiece", 3);

	public static final LongTelegramType END_BIG_BLOCK = new LongTelegramType("EndBigBlock", 4);

	public static final LongTelegramType START_BIGGER_BLOCK = new LongTelegramType("StartBiggerBlock", 5);

	public static final LongTelegramType NEXT_BIGGER_PIECE = new LongTelegramType("NextBiggerPiece", 6);

	public static final LongTelegramType END_BIGGER_BLOCK = new LongTelegramType("EndBiggerBlock", 7);

	public static final LongTelegramType BIG = new LongTelegramType("BIG", 100);

	public static final LongTelegramType BIGGER = new LongTelegramType("BIGGER", 100);


	public static LongTelegramType getInstance(int typeNumber) throws IllegalArgumentException {
		switch(typeNumber) {
			case 0:
				return RESERVERD;
			case 1:
				return SHORT_BLOCK;
			case 2:
				return START_BIG_BLOCK;
			case 3:
				return NEXT_BIG_PIECE;
			case 4:
				return END_BIG_BLOCK;
			case 5:
				return START_BIGGER_BLOCK;
			case 6:
				return NEXT_BIGGER_PIECE;
			case 7:
				return END_BIGGER_BLOCK;
			default:
				throw new IllegalArgumentException("Undefinierter Typ: " + typeNumber);
		}
	}

	/** Name des Types (ShortBlock, StartBigBlock, ....) */
	private final String _typeName;

	/** Zahl, die den Typ darstellt (ShortBlock = 1, ....) */
	private final int _typeNumber;

	private LongTelegramType(String typeName, int typeNumber) {
		_typeName = typeName;
		_typeNumber = typeNumber;
	}

	public int getTypeNumber() {
		return _typeNumber;
	}

	public String getTypeName() {
		return _typeName;
	}

	public String toString() {
		return "Name des Typs: " + getTypeName() + " Nummer des Typs: " + getTypeNumber();
	}
}
