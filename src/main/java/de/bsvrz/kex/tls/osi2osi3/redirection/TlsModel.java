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

import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.ConfigurationObject;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.config.SystemObjectType;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.*;

/**
 * Klasse TlsModel, die die Informationen zu den Tls-Geräten verwaltet.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 10172 $
 */
public class TlsModel {
	
	/**
	 * Datenmodell, aus dem die Konfigurationsinformationen bezogen werden
	 */
	private final DataModel _configuration;
	
	/**
	 * Map, in der zu dem Schlüssel Systemobjekt die entsprechenden Tls-Knoten vorgehalten werden.
	 */
	private final Map<SystemObject, TlsNode> _tlsDevices;
	
	private static final Debug _debug;
	static {
		_debug = Debug.getLogger();
		//_debug.setLoggerLevel(Debug.FINEST);
	}
	
	/**
	 * Map, die die vorhandenen DE-Typen (PID des DE-Typs) den entsprechenden Funktionsgruppen zuordnet.
	 */
	public Map<String, Integer> _deTyp2FgNr;
	
	/**
	 * Konstruktor. Das übergebene Datenmodell darf nicht null sein. Im Konstruktor werden die Informationen zu den TLS-Geräten aus der Konfiguration gelesen
	 * und in einer eigene Struktur vorgehalten.
	 * 
	 * @param configuration
	 *            Datenmodell
	 */
	public TlsModel(final DataModel configuration) {
		if(configuration == null) throw new IllegalArgumentException("DataModel ist null");
		_configuration = configuration;
		
		_tlsDevices = new HashMap<SystemObject, TlsNode>();
		
		// Aufbau der Map
		_deTyp2FgNr = new HashMap<String, Integer>();
		
		// An dieser Stelle müssen alle der Konfiguration bekannten (Haupt)DE-Typen aufgeführt sein.
		// Eventuell abgeleitete DE-Typen werden automatisch ergänzt.
		setDeTypes2Fg("typ.deLve", 1);
		setDeTypes2Fg("typ.deAxl", 2);
		setDeTypes2Fg("typ.deUfd", 3);
		setDeTypes2Fg("typ.deWzg", 4);
		setDeTypes2Fg("typ.deWww", 5);
		setDeTypes2Fg("typ.deVlt", 6);
		setDeTypes2Fg("typ.deZfr", 9);
        setDeTypes2Fg("typ.deZr",9);
        for (int i=128;i<254;i++){
        String type =  "typ.de" + String.valueOf(i);
            setDeTypes2Fg(type, i);
        }
//		setDeTypes2Fg("typ.de128", 128);
//		setDeTypes2Fg("typ.de253", 253);
		setDeTypes2Fg("typ.deSys", 254);
		
		/**
		 * Liste mit allen der Konfiguration bekannten Konfigurationsobjekte vom Typ typ.gerät
		 */
		final List<SystemObject> devices = _configuration.getType("typ.gerät").getElements();
		
		// Auszuwertende konfigurierende Attributgruppen
		final AttributeGroup atgGerät = _configuration.getAttributeGroup("atg.gerät");
		final AttributeGroup atgAnschlussPunktKommunikationsPartner = _configuration.getAttributeGroup("atg.anschlussPunktKommunikationsPartner");
		
		// Für alle Geräte ein Tls-Knoten Objekt anlegen und in die Map einfügen 
		// Erster Lauf über die Geräte, damit beim nächsten Lauf die zu refernzierenden Objekte bereits vorhanden sind.
		for(SystemObject device : devices) {
			final int nodeNumber = device.getConfigurationData(atgGerät).getItem("KnotenNummer").asScaledValue().intValue();
			_tlsDevices.put(device, new TlsNode(device, nodeNumber));
		}
		
		// Ermittlung der Kinderknoten, des Vaterknotens und der vorhandenen FG (Bei Geräten vom Typ typ.steuerModul)
		// Schleife über alle Geräte (Zeiter Lauf)
		for(SystemObject device : devices) {
			
			final List<SystemObject> connectionPointDevices = ((ConfigurationObject)device).getObjectSet("AnschlussPunkteGerät").getElements();
			
			// Schleife über alle Anschlusspunkte
			for(SystemObject connectionPointDevice : connectionPointDevices) {
				
				// Schleife über alle Anschlusspunkte, die an dem Anschlusspunkt angeschlossen sind
				final List<SystemObject> connectionPointsPartner = ((ConfigurationObject)connectionPointDevice).getObjectSet(
				        "AnschlussPunkteKommunikationsPartner").getElements();
				
				for(SystemObject connectionPointPartner : connectionPointsPartner) {
					// Auswertung, der Referenz
					final Data dataAnschlussPunktKommunikationsPartner = connectionPointPartner.getConfigurationData(atgAnschlussPunktKommunikationsPartner);
					
					if(dataAnschlussPunktKommunikationsPartner != null) {
						final SystemObject partnerDevice = dataAnschlussPunktKommunikationsPartner
						        .getItem("KommunikationsPartner")
						        .asReferenceValue()
						        .getSystemObject();
						//						System.out.println("Füge dem Knoten " + device + " den Subknoten "+ partnerDevice + " hinzu");
						_tlsDevices.get(device).addSubNode(_tlsDevices.get(partnerDevice));
						_tlsDevices.get(partnerDevice).setParent(_tlsDevices.get(device));
					}
				}
			}
			
			// Bei Steuermodulen die DE auswerten
			if(device.getType().getPid().equals("typ.steuerModul")) {
				final List<SystemObject> eaks = ((ConfigurationObject)device).getObjectSet("Eak").getElements();
				for(SystemObject eak : eaks) {
					final List<SystemObject> des = ((ConfigurationObject)eak).getObjectSet("De").getElements();
					for(SystemObject de : des) {
						final String deTyp = de.getType().getPid();
						final Integer fg = _deTyp2FgNr.get(deTyp);
						if(fg == null) {
							_debug.warning("Keine Umsetzung für DE vom Typ " + deTyp + " auf eine FG in TlsNode definiert");
						}
						else {
							_tlsDevices.get(device).addFg(fg);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Funktion, die die Map mit der Zuordnung von DE-Typen zu Funktiongruppen aufbaut. Hierbei werden zu den übergebenen DE-Typen jeweils automatisch alle
	 * eventuell abgeleiteten DE-Typen ermittelt und ebenfalls in die Map aufgenommen.
	 *
	 * @param deType
	 *            PID der DE-Typs
	 * @param fgNumber
	 *            Zugeordnete Funktionsgruppe
	 */
	private void setDeTypes2Fg(String deType, int fgNumber) {
		
		final SystemObjectType type = _configuration.getType(deType);
		if(type != null) {
			_deTyp2FgNr.put(deType, fgNumber);
		
			final List<SystemObjectType> subTypes = type.getSubTypes();
			for(SystemObjectType subType : subTypes) {
				setDeTypes2Fg(subType.getPid(), fgNumber);
			}
		}
	}
	
	/**
	 * Gibt den TLS-Knoten zu dem übergebenen Gerät zurück.
	 * 
	 * @param systemObject
	 * @return TLS-Knoten
	 */
	public TlsNode getTlsNode(final SystemObject systemObject) {
		
		final TlsNode tlsNode = _tlsDevices.get(systemObject);
		if(tlsNode == null) throw new IllegalArgumentException("Das Systemobjekt muss vom Typ Gerät sein: " + systemObject);
		return tlsNode;
	}

	/**
	 * Gibt alle Tls-Knoten zurück, die unterhalb des übergeordneten TlsKnotens (superiorNodeSystemObject)
	 * sind und vom Gerätetyp (typReference) sind. Wenn null für den übergeordneten TlsKnoten übergeben wird,
	 * werden alle Knoten des Gerätetyps bestimmt. Wenn als Gerätetyp null angegeben wird, werden alle gefundenen 
	 * Knoten bestimmt. Wenn der Gerätetyp Steuermodul ist, wird kann die Auswahl 
	 * durch die erforderliche Funktionsgruppe eingeschränkt werden (255 bedeutet alle FG).
	 * 
	 * @param superiorNodeSystemObject
	 * @param typReference
	 * @param forcedFg
	 * @return Collection der gefundenen Tls Knoten
	 */
	public Collection<TlsNode> getTlsNodes(SystemObject superiorNodeSystemObject, SystemObject typReference, int forcedFg) {
		ArrayList<TlsNode> tlsNodes = new ArrayList<TlsNode>();
		SystemObjectType deviceType = null;
		if (typReference!=null) deviceType = _configuration.getType(typReference.getPid());
		// Alle Knoten betrachten
		if(superiorNodeSystemObject==null){
			for(SystemObject systemObject : _tlsDevices.keySet()) {
				TlsNode tlsNode = this.getTlsNode(systemObject);
				if (deviceType==null){
					tlsNodes.add(tlsNode);
				}
				else if (systemObject.isOfType(deviceType)) {
					if (forcedFg==255 || tlsNode.hasFg(forcedFg) ){						
						tlsNodes.add(tlsNode);
					}
				}
			}
		}
		else{
			TlsNode superiorNode = this.getTlsNode(superiorNodeSystemObject);
			Collection<TlsNode> subNodes = superiorNode.getSubNodes();
			for(TlsNode tlsNode : subNodes) {
				if (deviceType==null){
					tlsNodes.add(tlsNode);
				}
				else if (tlsNode.getSystemObject().isOfType(deviceType)) {
					if (forcedFg==255 || tlsNode.hasFg(forcedFg) ){						
						tlsNodes.add(tlsNode);
					}
				}
            }
		}		
		return tlsNodes;
    }

	/**
	 * Gibt den Tls-Knoten zurück, der oberhalb des übergebenen Knotens ist und den vorgegebenen 
	 * Typ aufweist. Wenn kein übergeordneter Knoten dieses Typs vorhanden ist, wird null zurückgegeben.
	 * Als Typen sind nur die Typen zugelassen, die von Gerät abgeleitet sind.
	 * Wenn ein anderer Typ (oder Typ Gerät) angegeben wurde, wird eine Warnung ausgegeben und 
	 * als Rückgabe wird null zurückgegeben.
	 * 
     * @param tlsNode  Betrachteter Tls-Knoten
     * @param typeSuperiorTlsNode Gesuchter (Geräte)Typ des übergeordneten Knotens
     */
    public TlsNode getSuperiorNodeOfType(TlsNode tlsNode, SystemObject typeSuperiorTlsNode) {
    	// Prüfung, ob ein akzeptierter Typ von Gerät angegeben wurde.
    	// Hier sind nur Typen zugelassen, die von Gerät abgeleitet sind.
    	List<SystemObjectType> subTypesOfDevice = _configuration.getType("typ.gerät").getSubTypes();
    	TlsNode result = null;
    	int results = 0;
    	boolean acceptedType = false;
    	for(SystemObjectType systemObjectType : subTypesOfDevice) {
	        if(systemObjectType.equals(typeSuperiorTlsNode)) acceptedType = true;
        }
    	if(!acceptedType){
    		_debug.warning("Nicht unterstützen Typ eines Geräts angegeben.",typeSuperiorTlsNode);
    		return null;
    	}
    	
    	TlsNode superiorNode = tlsNode.getParentNode();
    	while(superiorNode!=null){

    		SystemObject systemObject = superiorNode.getSystemObject();
			SystemObjectType typeOfSuperiorNode = systemObject.getType();
    		if(typeSuperiorTlsNode.equals(typeOfSuperiorNode)){
    			results++;
    			if(result==null){
    				result=getTlsNode(systemObject);
//    				System.out.println("Treffer! " + result + " ist vom Typ " + typeSuperiorTlsNode);
    			}
    			else{
    				_debug.warning("Mehr als einen Knoten gefunden. Ignoriere folgenden Knoten: ",typeSuperiorTlsNode);
    			}
    		}
    		superiorNode = superiorNode.getParentNode();
    	}
    	return result;
    }
	
}
