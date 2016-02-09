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

import de.bsvrz.sys.funclib.debug.Debug;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Klasse zur Verwaltung der Weiterleitungsinformationen.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 7046 $
 * 
 */
public class RedirectionInfo {
	/**
	 * Map, wobei der Schlüssel die Knotennummer des Absenders ist. Als Wert wird in einer Struktur festgehalten, ob für eine bestimmte FG das Telegramm an
	 * weitere Zielknoten versendet werden muss und ob weiterhin die lokale Verarbeitung des Telegramms durchgeführt werden muss.
	 */
	Map<NodeFgPair, Map<Osi7SingleTelegramConverter, Set<Integer>>> _receiveEntries;
	/**
	 * Set, in der alle Knoten - Funktionsgruppen - Kombinationen aufgeführt sind,
	 * die nicht "normal" behandelt werden sollen.
	 */
	Set<NodeFgPair> _receiveNoNormalProcessing;
	
	/**
	 * Map, wobei der Schlüssel die Knotennummer des Empfängers ist. Als Wert wird in einer Struktur festgehalten, ob für eine bestimmte FG das Telegramm an
	 * weitere Zielknoten versendet werden muss und ob weiterhin die lokale Verarbeitung des Telegramms durchgeführt werden muss.
	 */
	Map<NodeFgPair, Map<Osi7SingleTelegramConverter, Set<Integer>>> _sendEntries;
	/**
	 * Set, in der alle Knoten - Funktionsgruppen - Kombinationen aufgeführt sind,
	 * die nicht an des ursprüngliche Ziel geschickt werden sollen.
	 */
	Set<NodeFgPair> _sendNotToPrimalTarget;

	private static final Debug _debug = Debug.getLogger();

	
	/**
	 * Konstruktor.
	 */
	public RedirectionInfo() {
		_receiveEntries = new HashMap<NodeFgPair, Map<Osi7SingleTelegramConverter, Set<Integer>>>();
		_receiveNoNormalProcessing = new HashSet<NodeFgPair>();
		_sendEntries = new HashMap<NodeFgPair, Map<Osi7SingleTelegramConverter, Set<Integer>>>();
		_sendNotToPrimalTarget = new HashSet<NodeFgPair>();
	}
	
	/**
	 * Fügt einen Eintrag in die Map zur Behandlung der empfangenen Telegramme hinzu.
	 * 
	 * @param knr
	 *            Knotennummer, für den der Eintrag gilt
	 * @param fg
	 *            Funktionsgruppe, wenn Null, dann zu allen Funktionsgruppen
	 * @param normalProcessing
	 *            Ob auch zum Originalziel gesendet wird (true). Bei false wird dies unterdrückt.
	 * @param destinations
	 *            Ziele, an die gesendet werden soll
	 * @param converter 
	 * 			  OSI7 Telegrammkonverter (falls vorhanden) sonst null.
	 */
	void addReceiveEntry(int knr, int fg, boolean normalProcessing, int[] destinations, Osi7SingleTelegramConverter converter) {
		NodeFgPair nodeFgPair = new NodeFgPair(knr,fg);
		if(!normalProcessing){
			_receiveNoNormalProcessing.add(nodeFgPair);
		}
		addEntry(_receiveEntries, nodeFgPair, destinations, converter);
	}

	/**
 	 * Gibt die Map zur Weiterleitung von empfangenden Telegrammen zurück.
 	 * Wenn keine Map für die vorgegebene Kombination von Knotennummer und FG vorhanden ist, wird null zurückgegeben.
 	 * Schlüssel der Map ist der zu verwendende OSI7-Telegramm-Konverter. Der Schlüssel null besagt,
 	 * dass hier vor der Weiterleitung keine Konvertierung auf OSI7 Ebene erfolgt.
 	 * Als Wert enthält die Map eine Set der Ziele, zu denen das Telegramm weitergeleitet werden soll.
 	 * 
 	 * @param knr
	 *            Knotennummer
	 * @param fg
	 *            Funktionsgruppe
     */
    public Map<Osi7SingleTelegramConverter, Set<Integer>> getReceiveRedirectionMap(int knr, int fg) {
    	NodeFgPair nodeFgPair = new NodeFgPair(knr,fg);
    	Map<Osi7SingleTelegramConverter, Set<Integer>> map = _receiveEntries.get(nodeFgPair);
    	if (map != null)return map;
    	nodeFgPair = new NodeFgPair(knr,255);
    	map = _receiveEntries.get(nodeFgPair);
    	if (map != null)return map;
    	return null;
    }
	
	/**
 	 * Gibt für ein Paar von Knotennummer des Senders und Funktionsgruppe an, ob Telegramme dieser Kombination
 	 * auch lokal verarbeitet werden sollen (Rückgabe true)
 	 * 
 	 * @param knr
	 *            Knotennummer
	 * @param fg
	 *            Funktionsgruppe
	 * @return true, wenn das ankommende Telegramm auch lokal verarbeitet werden soll.
     */
    public boolean receivedTelegramNormalProcessing(int knr, int fg) {
    	return normalProcessing(_receiveNoNormalProcessing, knr, fg);
    }

    /**
     * Prüfung, ob für ein Set von Knoten-Funktionsgruppen Paaren bei einer bestimmten
     * Kombination die "normale Behandlung" durchgeführt werden soll oder nicht.
     * 
     * @param nonNormalProcessing Set der Ausnahmen (die nicht normal behandelt werden sollen)
     * @param knr Knotennummer
     * @param fg  Funktionsgruppe
     * @return Ob die "normale Behandlung" durchgeführt werden soll oder nicht.
     */
    boolean normalProcessing(Set<NodeFgPair> nonNormalProcessing, int knr, int fg){
    	NodeFgPair nodeFgPair = new NodeFgPair(knr,fg);
    	if(nonNormalProcessing.contains(nodeFgPair)) return false;
    	nodeFgPair = new NodeFgPair(knr,255);
    	if(nonNormalProcessing.contains(nodeFgPair)) return false;   	
    	return true;
    }
    
	
	/**
	 * Fügt einen Eintrag in die Map zur Behandlung der zu sendenden Telegramme hinzu.
	 * 
	 * @param knr
	 *            Knotennummer, für den der Eintrag gilt
	 * @param fg
	 *            Funktionsgruppe, wenn Null, dann zu allen Funktionsgruppen
	 * @param normalProcessing
	 *            Ob auch zum Originalziel gesendet wird (true). Bei false wird dies unterdrückt.
	 * @param destinations
	 *            Ziele, an die gesendet werden soll
	 * @param converter 
	 * 			  Telegrammkonverter auf OSI7-Ebene. Wenn keine Konvertierung erfolgen soll wird null angegeben.
	 */
	public void addSendEntry(Integer knr, Integer fg, boolean normalProcessing, int[] destinations, Osi7SingleTelegramConverter converter) {
		NodeFgPair nodeFgPair = new NodeFgPair(knr,fg);
		if(!normalProcessing){
			_sendNotToPrimalTarget.add(nodeFgPair);
		}
		addEntry(_sendEntries, nodeFgPair, destinations, converter);

	}	

	/**
 	 * Gibt die Map zur Weiterleitung von zu sendenen Telegrammen zurück.
 	 * Wenn keine Map für die vorgegebene Kombination von Knotennummer und FG vorhanden ist, wird null zurückgegeben.
 	 * Schlüssel der Map ist der zu verwendende OSI7-Telegramm-Konverter. Der Schlüssel null besagt,
 	 * dass hier vor der Weiterleitung keine Konvertierung auf OSI7 Ebene erfolgt.
 	 * Als Wert enthält die Map eine Set der Ziele, zu denen das Telegramm weitergeleitet werden soll.
 	 * 
 	 * @param knr
	 *            Knotennummer
	 * @param fg
	 *            Funktionsgruppe
     */
    public Map<Osi7SingleTelegramConverter, Set<Integer>> getSendRedirectionMap(int knr, int fg) {
    	NodeFgPair nodeFgPair = new NodeFgPair(knr,fg);
    	Map<Osi7SingleTelegramConverter, Set<Integer>> map = _sendEntries.get(nodeFgPair);
    	if (map != null)return map;
    	nodeFgPair = new NodeFgPair(knr,255);
    	map = _sendEntries.get(nodeFgPair);
    	if (map != null)return map;
    	return null;
  }
	
	/**
 	 * Gibt für ein Paar von Knotennummer des Senders und Funktionsgruppe an, ob Telegramme dieser Kombination
 	 * auch lokal verarbeitet werden sollen (Rückgabe true)
 	 * 
 	 * @param knr
	 *            Knotennummer
	 * @param fg
	 *            Funktionsgruppe
	 * @return true, wenn das ankommende Telegramm auch lokal verarbeitet werden soll.
     */
    public boolean sendTelegramToPrimalTarget(int knr, int fg) {
    	return normalProcessing(_sendNotToPrimalTarget, knr, fg);
    }
	
	
	/**
	 * Ergänzt einen neuen Eintrag in der übergebenen Map.
	 * @param entries Map, die ergänzt werden soll.
	 * @param nodeFgPair	Knoten-Funktionsgruppen Paar.
	 * @param destinations  Zielknoten, an die weitergeleitet werden soll.
	 * @param converter		OSI7-Konverter, der verwendet werden soll (bei null keiner).
	 */
	private void addEntry(
            Map<NodeFgPair, Map<Osi7SingleTelegramConverter, Set<Integer>>> entries,
            NodeFgPair nodeFgPair,
            int[] destinations,
            Osi7SingleTelegramConverter converter) {

		Map<Osi7SingleTelegramConverter, Set<Integer>> converterMap = entries.get(nodeFgPair);
		
		if(converterMap == null) {
			converterMap = new HashMap<Osi7SingleTelegramConverter, Set<Integer>>();
			entries.put(nodeFgPair, converterMap);
		}
		
		Set<Integer> setDestinations= converterMap.get(converter);
		if(setDestinations==null){
			setDestinations = new HashSet<Integer>();
			converterMap.put(converter, setDestinations);
		}
		for(int i = 0; i < destinations.length; i++) {
			setDestinations.add(destinations[i]);
        }
    }
	
	
	/**
	 * Gibt alle gespeicherten Einträge aus.
	 * 
	 */
	public void printAllEntries() {
		_debug.fine("Gespeicherte Empfangseinträge:");		
		printEntries(_receiveEntries,_receiveNoNormalProcessing);
		_debug.fine("Gespeicherte Sendeeinträge:");		
		printEntries(_sendEntries,_sendNotToPrimalTarget);
	}

	/**
	 * Ausgabe der Informationen zu den Empfangs oder Sendeinträgen.
	 * 
	 * @param entries 
	 * 				Map, in der für die Knotennummer/Funktionsgruppenpaare Maps für die
	 * 				Maps von Telegrammkonvertern und Zielen, an die Telegramme weitergeleitet
	 *              werden sollen, aufgeführt sind
	 * @param noNormalProcessing
	 * 				Set, in dem die Knotennummer/Funktionsgruppenpaare aufgeführt sind, für die
	 *              keine normale Behandlung erfolgen soll.
	 */
	private void printEntries(Map<NodeFgPair, Map<Osi7SingleTelegramConverter, Set<Integer>>> entries, Set<NodeFgPair> noNormalProcessing) {
		
		Set<NodeFgPair> nodeFgPairs = entries.keySet();
		
	    for(NodeFgPair nodeFgPair : nodeFgPairs) {
	        int knr = nodeFgPair.getNodenumber();
	        int fg = nodeFgPair.getFg();
	        Map<Osi7SingleTelegramConverter, Set<Integer>> map = entries.get(nodeFgPair);
	        String normalProcessing = " (ja) ";
	        
	        if(noNormalProcessing.contains(nodeFgPair)) normalProcessing = " (nein) ";
	        
	        Set<Osi7SingleTelegramConverter> telegramConverters = map.keySet();
	        for(Osi7SingleTelegramConverter osi7TelegramConverter : telegramConverters) {
	        	String converter = " (ohne Konverter)";
	        	if(osi7TelegramConverter!=null){
	        		TlsNode tlsNode = osi7TelegramConverter.getTlsNode();
	        		String locationDistance = " (-)";
	        		if(tlsNode!=null){
	        			locationDistance = " ( "+ tlsNode.getLocationDistance()+")";
	        		}					
	        		converter = " (Konverter: " + osi7TelegramConverter.getClass().getName() + locationDistance+ ")";
	        		
	        	}
	            Set<Integer> destinations = map.get(osi7TelegramConverter);
	            
	            _debug.fine("Knotennummer: " + knr + " (" + getLocationDistance(knr) + ") Funktionsgruppe: " + fg + 
	            				   " lokal verabeiten " + normalProcessing +
	            				   " Ziele " + printDestinations(destinations) +  converter);
            }
        }
    }
	
	/**
	 * Gibt ein Set von Knotennummern als String zurück.
	 * @param destinations Knotennummern
	 * @return String mit den Knotennummern 
	 */
	private String printDestinations(Set<Integer> destinations){
		String result = " (";
		for(Integer destination : destinations) {
			result += destination + " (" + getLocationDistance(destination) + ") ";
        }
		result += ") ";
		return result;
	}
	
	/**
	 * Umrechnung der Knotennummer in Location und Distance
	 * 
	 * @return Location und Distance
	 */
	private String getLocationDistance(int nodeNumber) {
		int location = nodeNumber >>> 8;
		int distance = nodeNumber & 0xff;
		return location + "-" + distance;
	}	
}
