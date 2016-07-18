/*
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2004 by Kappich+Kniß Systemberatung, Aachen
 * 
 * This file is part of de.bsvrz.kex.tls.osi2osi3.
 * 
 * de.bsvrz.kex.tls.osi2osi3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * de.bsvrz.kex.tls.osi2osi3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.bsvrz.kex.tls.osi2osi3.  If not, see <http://www.gnu.org/licenses/>.

 * Contact Information:
 * Kappich Systemberatung
 * Martin-Luther-Straße 14
 * 52062 Aachen, Germany
 * phone: +49 241 4090 436 
 * mail: <info@kappich.de>
 */

package de.bsvrz.kex.tls.osi2osi3.osi2.wancom;


/**
 * Definiert die möglichen Zustände eines Protokolls.
 *
 * @author Kappich Systemberatung
 * @version $Revision$
 */
public final class ProtocolState {

	/** Stabiler Zustand für ein noch nicht gestartetes Protokoll. */
	public static final ProtocolState CREATED = new ProtocolState("Erzeugt");

	/** Übergangszustand für ein startendes Protokoll. */
	public static final ProtocolState STARTING = new ProtocolState("Wird gestartet");

	/** Stabiler Zustand für ein gestartetes Protokoll. */
	public static final ProtocolState STARTED = new ProtocolState("Gestartet");

	/** Übergangszustand für ein zu stoppendes Protokoll. */
	public static final ProtocolState STOPPING = new ProtocolState("Wird gestoppt");

	/** Stabiler Zustand für ein gestopptes Protokoll. */
	public static final ProtocolState STOPPED = new ProtocolState("Gestoppt");

	/** Name des Zustands */
	private final String _name;

	/**
	 * Liefert eine textuelle Beschreibung dieses Zustands zurück. Das genaue Format ist nicht festgelegt und kann sich ändern.
	 *
	 * @return Beschreibung dieses Zustands.
	 */
	public String toString() {
		return _name;
	}

	/**
	 * Nicht öffentlicher Konstruktor der zum Erzeugen der vordefinierten Zustände benutzt wird.
	 *
	 * @param name Name des Zustandes.
	 */
	private ProtocolState(String name) {
		_name = name;
	}
}
