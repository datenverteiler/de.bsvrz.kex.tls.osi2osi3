/*
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2003 by Kappich+Kniß Systemberatung, Aachen
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

package de.bsvrz.kex.tls.osi2osi3.properties;

import java.util.Properties;

/**
 * Klasse zur Konvertierung von Text-Properties in einfache Datentypen
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9602 $
 */
public class PropertyConsultant {

	private final PropertyQueryInterface _queryInterface;

	public PropertyConsultant(PropertyQueryInterface queryInterface) {
		_queryInterface = queryInterface;
	}

	public String getProperty(String key) throws IllegalArgumentException {
		String property = _queryInterface.getProperty(key);
		if(property == null) throw new IllegalArgumentException("Property " + key + " nicht definiert");
		return property;
	}

	public int getIntProperty(String key) throws IllegalArgumentException {
		return Integer.parseInt(getProperty(key));
	}

	public double getDoubleProperty(String key) throws IllegalArgumentException {
		return Double.parseDouble(getProperty(key));
	}

	public boolean getBooleanProperty(String key) throws IllegalArgumentException {
		String property = getProperty(key).trim().toLowerCase();
		if(property.equals("ja")) return true;
		if(property.equals("nein")) return false;
		if(property.equals("true")) return true;
		if(property.equals("false")) return false;
		if(property.equals("wahr")) return true;
		if(property.equals("falsch")) return false;
		if(property.equals("yes")) return true;
		if(property.equals("no")) return false;
		throw new IllegalArgumentException("Ungültiger Parameter " + key + " (" + property + "), erlaubt sind \"ja\" und \"nein\"");
	}
}
