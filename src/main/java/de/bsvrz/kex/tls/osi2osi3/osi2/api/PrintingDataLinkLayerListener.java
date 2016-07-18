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

package de.bsvrz.kex.tls.osi2osi3.osi2.api;

/**
 * Beispiel einer Implementierung der Schnittstellenklasse zur Weiterleitung von Kommunikationsereignissen vom Kommunikationsprotokoll der Sicherungsschicht an
 * die Anwendung bzw. die nächst höhere Protokollebene. Diese Implementierung gibt die erhaltenen Ereignisse auf den Standard-Ausgabekanal aus.
 *
 * @author Kappich Systemberatung
 * @version $Revision$
 */
public class PrintingDataLinkLayerListener implements DataLinkLayerListener {

	public void handleDataLinkLayerEvent(DataLinkLayerEvent event) {
		System.out.println("++++++++++++++++++++++++++got DataLinkLayerEvent: " + event);
	}
}
