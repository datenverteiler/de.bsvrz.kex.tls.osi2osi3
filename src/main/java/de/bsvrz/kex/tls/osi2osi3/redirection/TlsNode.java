/*
 * Copyright 2008 by Kappich Systemberatung Aachen
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

import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * TlsNode zur Ermittlung aller benötigten Informationen zu einem TLS-Knoten
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 7038 $
 * 
 */
public class TlsNode {
	private static final Debug _debug;
	
	private SystemObject _systemObject;
	
	private int _nodeNumber;
	
	private TlsNode _parentNode;
	
	private Set<Integer> _fgPresent;
	
	/**
	 * Gibt das Systemobjekt zurück, das durch den Knoten repräsentiert wird.
	 * 
	 * @return Systemobjekt zum TLS-Knoten
	 */
	public SystemObject getSystemObject() {
		return _systemObject;
	}
	
	/**
	 * Gibt den Vaterknoten zu dem TLS-Knoten zurück (falls vorhanden).
	 * 
	 * @return Vaterknoten zu dem TLS-Knoten zurück (sonst null)
	 */
	public TlsNode getParentNode() {
		return _parentNode;
	}
	
	private final Collection<TlsNode> _subNodes = new HashSet<TlsNode>();
	
	static {
		_debug = Debug.getLogger();
		//_debug.setLoggerLevel(Debug.FINEST);
	}
	
	/**
	 * Konstruktor
	 * 
	 * @param systemObject
	 *            Das dem TLS-Knoten zugeordnete Gerät
	 */
	public TlsNode(SystemObject systemObject) {
		if(systemObject == null) throw new IllegalArgumentException("Systemobjekt ist null");
		_systemObject = systemObject;
		_fgPresent = new HashSet<Integer>();
	}
	
	/**
	 * Konstruktor
	 * 
	 * @param systemObject
	 *            Das dem TLS-Knoten zugeordnete Gerät
	 * @param nodeNumber
	 *            Knotennummer des TLS-Knotens
	 */
	public TlsNode(SystemObject systemObject, int nodeNumber) {
		if(systemObject == null) throw new IllegalArgumentException("Systemobjekt ist null");
		_systemObject = systemObject;
		_fgPresent = new HashSet<Integer>();
		_nodeNumber = nodeNumber;
		_debug.info("Neuen TlsKnoten angelegt mit Knotennummer " + nodeNumber + ", " + _nodeNumber);
	}
	
	/**
	 * Gibt die Knotennummer des TLS-Knotens zurück
	 * 
	 * @return Knotennummer der TLS-Knotens
	 */
	public int getNodeNumber() {
		return _nodeNumber;
	}
	
	/**
	 * Setzt die Knotennummer des TLS-Knotens.
	 * 
	 * @param nodeNumber
	 */
	void setNodeNumber(int nodeNumber) {
		_nodeNumber = nodeNumber;
	}
	
	/**
	 * Fügt dem TLS-Knoten einen Unterknoten hinzu.
	 * 
	 * @param subNode
	 *            Hinzuzufügender TLS-Knoten
	 */
	void addSubNode(TlsNode subNode) {
		if(subNode == null) throw new IllegalArgumentException("subNode ist null");
		if(subNode == this) throw new IllegalArgumentException("subNode ist der Knoten selbst");
		final boolean changed = _subNodes.add(subNode);
		if(!changed) {
			_debug.warning("Tls-Knoten " + this + " hat bereits den Sub-Knoten " + subNode + " enthalten");
		}
	}
	
	/**
	 * Gibt die Menge der Unterknoten zum TLS-Knoten zurück.
	 * 
	 * @return Menge der Unterknoten zum TLS-Knoten
	 */
	public Collection<TlsNode> getSubNodes() {
		return _subNodes;
	}
	
	/**
	 * Setzt den Vaterknoten zum TLS-Knoten
	 * 
	 * @param parentNode
	 *            Vaterknoten zum TLS-Knoten
	 */
	void setParent(TlsNode parentNode) {
		_parentNode = parentNode;
	}
	
	/**
	 * Umrechnung der Knotennummer in Location und Distance
	 * 
	 * @return Location und Distance
	 */
	public String getLocationDistance() {
		int location = _nodeNumber >>> 8;
		int distance = _nodeNumber & 0xff;
		return location + "-" + distance;
	}
	
	/**
	 * Fügt dem TLS-Knoten eine Funktionsgruppe hinzu. Dies ist nur bei Geräten vom Typ Steuermodul sinnvoll. Hiermit werden alle möglichen Funktionsgruppen der
	 * DE in den zugeordneten EAK bestimmt.
	 * 
	 * @param fg
	 *            Funktionsgruppe, die bei diesem Gerät möglich ist.
	 */
	void addFg(int fg) {
		_fgPresent.add(fg);
	}
	
	/**
	 * Gibt an, ob das Gerät (Steuermodul) mindestens ein DE der entsprechenden Funktionsgruppe enthält.
	 * Wenn der Knoten nicht vom Typ Steuermodul ist, wird true zurückgegeben.
	 * 
	 * @param fg
	 * @return
	 */
	public boolean hasFg(int fg) {
		// Wenn der Knoten nicht vom Typ Steuermodul ist, wird true zurückgegeben.
		if(!this.getSystemObject().isOfType("typ.steuerModul")){
			return true;
		}
		return _fgPresent.contains(fg);
	}
	
	@Override
	public String toString() {
		return "TLS-Knoten: " + getLocationDistance() + " (" + _nodeNumber + ")";
	}
}
