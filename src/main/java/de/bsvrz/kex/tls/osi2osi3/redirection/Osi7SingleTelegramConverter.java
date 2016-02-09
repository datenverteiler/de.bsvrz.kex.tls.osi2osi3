/*
 * Copyright 2009 by Kappich Systemberatung Aachen
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

/**
 * Interface für Konverter
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 6813 $
 *
 */
public interface Osi7SingleTelegramConverter {
	
	/**
	 * Funktion, die ein übergebenes OSI7 Einzeltelegramm konvertiert und das 
	 * konvertierte Einzeltelegramm zurückgibt.
	 * 
	 * @param  osi7SingleTelegramBytes Übergebenes Einzeltelegramm
	 * @return Konvertiertes Einzeltelegramm
	 */
	byte[] convert(byte[] osi7SingleTelegramBytes);
	
	/**
	 * Setzt den Tls-Knoten, von dem aus die Konvertierung an weitere Ziele umgesetzt wird. 
	 * @param tlsNode
	 */
	void setTlsNode(TlsNode tlsNode);
	/**
	 * Gibt den Tls-Knoten zurück, von dem aus die Konvertierung an weitere Ziele umgesetzt wird.
	 * @return Tlsknoten 
	 */
	TlsNode getTlsNode();
	
}
