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
 * Klasse, die eine Knotennummer mit einer FG vereint
 * 
 * 
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 6721 $
 *
 */
public class NodeFgPair {
	int _nodenumber;
	int _fg;

	/**
     * @param nodenumber Knotennummer
     * @param fg Funktionsgruppe
     */
    public NodeFgPair(int nodenumber, int fg) {
	    _nodenumber = nodenumber;
	    _fg = fg;
    }
	
	
	/**
 	 * Gibt die Knotennummer des Knotennummer/Funktionsgruppen-Paares zurück.
    * @return Knotennummer
     */
    public int getNodenumber() {
    	return _nodenumber;
    }

	/**
	 * Gibt die Funktionsgruppe des Knotennummer/Funktionsgruppen-Paares zurück.
     * @return  Funktionsgruppe
     */
    public int getFg() {
    	return _fg;
    }


	@Override
    public int hashCode() {
//	    final int prime = 31;
//	    int result = 1;
//	    result = prime * result + _fg;
//	    result = prime * result + _nodenumber;
//	    return result;
	    return _nodenumber * 256 + _fg;
    }


	@Override
    public boolean equals(Object obj) {
	    if(this == obj) return true;
	    if(obj == null) return false;
	    if(getClass() != obj.getClass()) return false;
	    NodeFgPair other = (NodeFgPair)obj;
	    if(_fg != other._fg) return false;
	    if(_nodenumber != other._nodenumber) return false;
	    return true;
    }

    
	
}
