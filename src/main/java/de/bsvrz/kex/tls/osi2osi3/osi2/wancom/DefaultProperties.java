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

import java.util.*;

/**
 * Defaultparameter für Protokoll- und Verbindungsparameter der WanCom Protokolle.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 7033 $
 */
class DefaultProperties extends Properties {

	/** Einziges Objekt dieser Klasse (Singleton). */
	private static DefaultProperties _defaultProperties = new DefaultProperties();

	/**
	 * Bestimmt das einziges Objekt dieser Klasse (Singleton).
	 *
	 * @return Einziges Objekt dieser Klasse (Singleton).
	 */
	public static DefaultProperties getInstance() {
		return _defaultProperties;
	}

	/* Nicht öffentlicher Konstruktor zum Erzeugen der Dafaultparameter */
	private DefaultProperties() {

		// In den ersten 4 Bytes eines Telegramms übertragene Version des eingesetzten WanCom Protokolls.
		setProperty("wancom.version", "35");

		// Default TCP-Port des WanCom-Servers.
		setProperty("wancom.port", "7100");

		// Zeit in Sekunden zwischen dem Versand von 2 Keep-Alive Telegrammen.
		setProperty("wancom.keepAliveTime", "20");

		// Anzahl von in Folge vergangenen keepAliveTime-Intervallen ohne Empfang eines KeepAlive-Telegramms bevor die Verbindung abgebrochen wird.
		setProperty("wancom.keepAliveTimeoutCount", "3");

		// WanCom-Type-Feld in KeepAlive-Telegrammen.
		setProperty("wancom.keepAliveType", "50");

		// Das IP-Routing in empfangenen WanCom-Telegrammen ignorieren? Der Wert "nein" führt zu ein Warnung und zum Ignorieren des ganzen Telegramms, falls
		// ein Routing enthalten ist und Zeiger nicht gleich der Länge ist.
		setProperty("wancom.ipRoutingIgnorieren", "nein");

		// WanCom-Type-Feld in TLS-Telegrammen.
		setProperty("wancom.tlsType", "600");

		// WanCom-Type-Feld in empfangenen TLS-Telegrammen. Keine Angabe bedeutet, dass der Wert von wancom.tlsType verwendet wird. -1 bedeutet, dass
		// beliebige Type-Werte (ausser wancom.keepAliveType) als TLS-Telegramm interpretiert werden.
		//setProperty("wancom.tlsTypeReceive", "");

		// Wartezeit in Sekunden, bevor ein fehlgeschlagene Verbindungsversuch wiederholt wird.
		setProperty("wancom.connectRetryDelay", "60");

		// Lokale Adresse, die in Wan-Com-Header als Absender eingetragen werden soll. Ein leerer Text, wird
		// automatisch durch die aktuelle lokale Adresse der Wan-Com-Verbindung ersetzt.
		setProperty("wancom.localAddress", "");

		// Wenn "ja", dann wartet das Protokoll nach dem Aufbau der TCP-Verbindung auf den Empfang eines initialen
		// Telegramms, bevor eine Verbindung als "lebt" gemeldet wird.
		setProperty("wancom.waitForInitialReceive", "nein");
	}
}
