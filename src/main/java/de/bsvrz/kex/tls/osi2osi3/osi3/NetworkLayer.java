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
 * Interface für die Funktionen der OSI-3 Netzwerkebene.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9349 $
 */
public interface NetworkLayer {

	/** Hohe Priorität bei Verwendung in der Methode {@link #sendData(int,int,byte[],boolean)} */
	static final int PRIORITY_CLASS_1 = 1;

	/** Niedrige Priorität bei Verwendung in der Methode {@link #sendData(int,int,byte[],boolean)} */
	static final int PRIORITY_CLASS_2 = 2;

	/** Startet die Verarbeitung von Telegrammen */
	public void start();

	/**
	 * Sendet ein Telegramm mit hoher Priorität an ein vorgegebenes Zielgerät.
	 *
	 * @param destination  Knotennummer des Geräts an das das Telegramm gesendet werden soll.
	 * @param data         Nutzdaten aus Sicht des NetworkLayers (i.a. ein OSI-7 Telegrammblock).
	 * @param longTelegram true = Langtelegramm, das nicht der TLS Definition entspricht; false = Telegramm, das der TLS Definition entspricht
	 *
	 * @throws DestinationUnreachableException
	 *          Wenn das angegebene Ziel nicht erreichbar ist.
	 */
	public void sendData(int destination, byte[] data, boolean longTelegram) throws DestinationUnreachableException;

	/**
	 * Sendet ein Telegramm mit vorgegebener Priorität an ein angegebenes Zielgerät.
	 *
	 * @param destination  Knotennummer des Geräts an das das Telegramm gesendet werden soll.
	 * @param priority     Prioritätsklasse mit der das Telegramm versendet werden soll. Entweder {@link #PRIORITY_CLASS_1} oder {@link #PRIORITY_CLASS_2}
	 * @param data         Nutzdaten aus Sicht des NetworkLayers (i.a. ein OSI-7 Telegrammblock).
	 * @param longTelegram true = Langtelegramm, das nicht der TLS Definition entspricht; false = Telegramm, das der TLS Definition entspricht
	 *
	 * @throws DestinationUnreachableException
	 *          Wenn das angegebene Ziel nicht erreichbar ist.
	 */
	public void sendData(int destination, int priority, byte[] data, boolean longTelegram) throws DestinationUnreachableException;

	/**
	 * Meldet einen Empfänger für Ereignis der Netzebene an. Dies ist im allgemeinen eine Protokollschicht oberhalb der Netzebene (wie z.B. TLS-OSI-7 Schicht).
	 * Nach der Anmeldung werden allen angemeldeten Empfängern empfangene Telegramme und Statusänderung von Verbindungen mitgeteilt.
	 *
	 * @param networkLayerListener Anzumeldender Empfänger, der die Verarbeitung von empfangenen Telegrammen und Statusänderungen von Verbindungen übernimmt.
	 */
	public void addEventListener(NetworkLayerListener networkLayerListener);

	/**
	 * Meldet einen angemeldeten Empfänger für Ereignisse der Netzebene wieder ab.
	 *
	 * @param networkLayerListener Abzumeldender Empfänger
	 */
	public void removeEventListener(NetworkLayerListener networkLayerListener);

	/**
	 * Bestimmt die Geräte, die über das angegebene Gerät erreichbar sind. Alle Geräte, die bezüglich des OSI-3 Routings direkt oder indirekt hinter dem
	 * angegebenen Gerät liegen, werden ermittelt.
	 *
	 * @param device Gerät zu dem die darüber erreichbaren Geräte ermittelt werden sollen.
	 *
	 * @return Array mit den Knotennummern der Geräte, die hinter dem angegebenen liegen.
	 */
	public int[] getRoutedRemoteDevices(int device);
}
