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

package de.bsvrz.kex.tls.osi2osi3.osi2.api;

/**
 * Beispiel einer Implementierung der Schnittstellenklasse zur Weiterleitung von Kommunikationsereignissen vom Kommunikationsprotokoll der Sicherungsschicht an
 * die Anwendung bzw. die nächst höhere Protokollebene. Diese Implementierung gibt die erhaltenen Ereignisse auf den Standard-Ausgabekanal aus.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 5114 $
 */
public class PrintingDataLinkLayerListener implements DataLinkLayerListener {

	public void handleDataLinkLayerEvent(DataLinkLayerEvent event) {
		System.out.println("++++++++++++++++++++++++++got DataLinkLayerEvent: " + event);
	}
}
