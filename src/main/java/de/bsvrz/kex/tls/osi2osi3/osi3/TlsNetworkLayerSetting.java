/*
 * Copyright 2011 by Kappich Systemberatung Aachen
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
 * Eine Hilfsklasse, für die Osi3 Schicht. Hier sind einstellbare Parameter festgelegt. Des weiteren gibt es eine Methode, die den Wert eines Parameters als
 * Boolean zurück gibt, falls möglich.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 10824 $
 */
public class TlsNetworkLayerSetting {

	/**
	 * Defaultwert ist auf Nein Eingestellt, bei keiner Parametrierung.
	 */
	final static public String reduceToControlByte = "osi3.reduzierungAufSteuerbyte";

	/**
	 * Defaultwert ist auf Nein Eingestellt, bei keiner Parametrierung.
	 */
	final static public String acceptSecondaryAddress = "secondary.adressen200-254Akzeptieren";

	/**
	 * Defaultwert ist auf Ja Eingestellt, bei keiner Parametrierung.
	 */
	final static public String pointerIncrement = "osi3.pointerInkrementieren";

	/**
	 * Defaultwert ist auf Nein Eingestellt, bei keiner Parametrierung.
	 */
	final static public String reflectOsi3Routing = "osi3.adressenSpiegeln";

	/**
	 * eine Methode, die den Wert eines Paramaters als
     * Boolean zurück gibt, falls möglich.
	 * @param value Parameter Wert
	 * @return False : Für nein/false/falsch/no , True für ja/true/wahr/yes
	 * @throws IllegalArgumentException Falls der Boolsche Ausdruck nicht bestimmt werden kann.
	 */
	public static boolean getBooleanProperty(String value) throws IllegalArgumentException {
		value = value.trim().toLowerCase();
		if(value.equals("ja")) return true;
		if(value.equals("nein")) return false;
		if(value.equals("true")) return true;
		if(value.equals("false")) return false;
		if(value.equals("wahr")) return true;
		if(value.equals("falsch")) return false;
		if(value.equals("yes")) return true;
		if(value.equals("no")) return false;
		throw new IllegalArgumentException("Ungültiger Wert (" + value + "), " + getValidBooleanPropertys());
	}

	/**
	 * Ein möglicher Fehlertext bzw. Hinweis, was erlaubt ist, damit ein der Boolscher Ausdruck gültig ist.
	 * @return Hinweistext/Fehlertext
	 */
	static public String getValidBooleanPropertys() {
		return "Gültig sind ja/nein, true/false, wahr/falsch, yes/no . Groß- und Kleinschreibung wird nicht unterschieden.";
	}
}
