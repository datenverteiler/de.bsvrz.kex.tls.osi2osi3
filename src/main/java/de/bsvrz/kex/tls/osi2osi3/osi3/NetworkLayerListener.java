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

package de.bsvrz.kex.tls.osi2osi3.osi3;

/**
 * Interface, dass von der OSI-7 Schicht zur entgegennahme empfangener Telegramme und zur Verarbeitung von Statuswechseln implementiert werden muss.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9349 $
 */
public interface NetworkLayerListener {

	public static final int DEVICE_CONNECTED = 0;

	public static final int DEVICE_DISCONNECTED = 1;

	/**
	 * Diese Methode nimmt ein Telegramm entgegen und reicht es an den entsprechenden Empfänger weiter.
	 *
	 * @param sender       Absender des Telegramms
	 * @param data         Telegramm
	 * @param longTelegram true = Das Telegramm, das übergeben wird, ist ein zusammengebautes Langtelegramm und muss besonders behandelt werden (Anzahl
	 *                     Einzeltelgramme fehlen, Länge Einzeltelegramme fehlt, usw), da es nicht der TLS Definition entspricht false = Die Daten entsprechen der
	 *                     TLS Definition
	 */
	public void dataReceived(int sender, byte[] data, boolean longTelegram);

	public void stateChanged(int device, int state);
}
