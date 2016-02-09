/*
 * Copyright 2009 by Kappich Systemberatung, Aachen
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

import de.bsvrz.kex.tls.osi2osi3.osi3.DestinationUnreachableException;
import de.bsvrz.kex.tls.osi2osi3.osi3.NetworkLayer;
import de.bsvrz.kex.tls.osi2osi3.osi3.NetworkLayerSender;
import de.bsvrz.sys.funclib.debug.Debug;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Klasse zum Verarbeiten der empfangenden und zu versendenden Telegramme.
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 7107 $
 * 
 */
public class TelegramProcessor {
	
	private RedirectionInfo _redirectionInfo = null;
	
	private final NetworkLayerSender _networkLayer;
	
	private static final Debug _debug = Debug.getLogger();
	
	
	public TelegramProcessor(NetworkLayerSender networkLayer) {
		_networkLayer = networkLayer;
	}
	
	/**
	 * Prüfung, ob ein zu versendendes Telegramm auch lokal verarbeitet werden soll und ggf. Weiterleitung an weitere Ziele.
	 * 
	 * @param destination
	 *            Knotennummer, an die das Telegramm versendet werden soll.
	 * @param data
	 *            Bytearray OSI 7 Telegramm Daten.
	 * @return Ob das zu versendene Telegramm auch lokal verarbeitet werden soll.
	 */
	public boolean dataToSend(int destination, byte[] data) {
		// Wenn keine Informationen vorhanden sind, true zurückgeben
		// Entspricht Verhaltem ohne Redirection-Mechanismus
		if(_redirectionInfo == null) return true;
		
		// Telegramm zerlegen
		TelegramStructure telegramStructure = new TelegramStructure(data);

		// Knotennummer
		int nodeNumber = telegramStructure.getNodeNumber();
		
		// OSI7-Telegramm, die die Teile des übergebenen Telegramms enthält, die
		// (auch) lokal behandelt werden sollen.
		TelegramStructure resultLocalTelegram = new TelegramStructure(nodeNumber);

		// Zerlegung in Einzeltelegramme
		List<byte[]> singleTelegrams = telegramStructure.getSingleTelegrams();

		Map<Osi7SingleTelegramConverter, Set<Integer>> sendRedirectionMap = null;
		
		for(int i = 0; i < singleTelegrams.size(); i++) {
			byte[] singleTelegram = singleTelegrams.get(i);
			int fg = telegramStructure.getFgFromSingleTelegram(singleTelegram);
			_debug.fine("Bearbeite Einzeltelegramm von KNR, #, FG;  " + destination +", "+ i +", " + fg);
			
			if(_redirectionInfo.sendTelegramToPrimalTarget(destination, fg)) resultLocalTelegram.addSingleTelegram(singleTelegram);
			
			sendRedirectionMap = _redirectionInfo.getSendRedirectionMap(destination, fg);
			
			if(sendRedirectionMap != null){
				
				printOsi7TelegramConverters2Destinations(sendRedirectionMap);
				
				for(Osi7SingleTelegramConverter osi7TelegramConverter : sendRedirectionMap.keySet()) {
					// Alle Ziele für den Konverter
					Set<Integer> redirectDestinations = sendRedirectionMap.get(osi7TelegramConverter);
	                if(osi7TelegramConverter!=null){
	                	singleTelegram = osi7TelegramConverter.convert(singleTelegram);
	                }
	                // Nach der Konvertierung kann ein leeres Byte-Array zurückgegeben werden
	                // Um Telegramme mit 0 DE-Blöcken zu vermeiden muss das Einzeltelegramm mehr als 5 Bytes haben
	                if (singleTelegram.length>7){
	                
	                	// Wenn das Einzeltelegramm nur einem DE-Block besteht und der Daten-Endgeräte-Kanal (DE) = 255
	                	// gesetzt ist (z.B. Zeitstempel) soll der Versand unterdrückt werden
	                	if(((singleTelegram[4]&0xff)==1) && ((singleTelegram[6]&0xff)==255)){
	                		// Telegramm verwerfen
	                	}
	                	else{
	                		TelegramStructure telegramToSend = new TelegramStructure(nodeNumber);
	                		
	                		telegramToSend.addSingleTelegram(singleTelegram);
	                		for(Integer redirectionDestination : redirectDestinations) {
	                			_debug.fine("Sende an Ziel: " + redirectionDestination);
	                			try {
	                                _networkLayer.sendWithoutRedirection(redirectionDestination, NetworkLayer.PRIORITY_CLASS_1, telegramToSend.getTelegramBytes(), false);
                                }
                                catch(DestinationUnreachableException e) {
                                	_debug.warning("Senden an das Ziel " + redirectionDestination + " fehlgeschlagen!"
                                			, e
                                			);
                                }
	                		}
	                	}
	                }	                
                }
			}
		}

		int resultLocalTelegramSize = resultLocalTelegram._singleTelegrams.size();
		if (resultLocalTelegramSize==0) return false;
		
		// Es sollen weniger Einzeltelegramme auch an das ursprüngliche Ziel gesendet werden als 
		// im übergebenen OSI7-Telegramm erhalten wurden.
		// In diesem Fall erfolgt der Versand des Telegramms hier und es wird false zurückgegeben.
		if(resultLocalTelegramSize < singleTelegrams.size()){
			try {
	            _networkLayer.sendWithoutRedirection(destination, NetworkLayer.PRIORITY_CLASS_1, resultLocalTelegram.getTelegramBytes(), false);
            }
            catch(DestinationUnreachableException e) {
            	_debug.warning("Senden an das Ziel " + destination + " fehlgeschlagen!"
            			, e
         			);
            }
			return false;
		}
		return true;
	}

	/**
	 * Debugausgabe, die die Zielknotennummern zu den Konvertern ausgibt.
     * @param sendRedirectionMap
     */
    private void printOsi7TelegramConverters2Destinations(Map<Osi7SingleTelegramConverter, Set<Integer>> sendRedirectionMap) {
	    Set<Osi7SingleTelegramConverter> keySet = sendRedirectionMap.keySet();
	    for(Osi7SingleTelegramConverter osi7TelegramConverter : keySet) {
	    	// Alle Ziele für den Konverter
	    	Set<Integer> redirectDestinations = sendRedirectionMap.get(osi7TelegramConverter);
	    	String rDest = redirectDestinations.toString();
	    	String converterClass = " Kein Konverter ";
	    	if(osi7TelegramConverter!=null) converterClass=osi7TelegramConverter.getClass().getSimpleName();
	    	_debug.fine("Konverter: " + converterClass + " Ziele: " + rDest);
	    }
    }
	
	/**
	 * Prüfung, ob ein empfangenes Telegramm auch lokal verarbeitet werden soll.
	 * Gibt ein Bytearray zurück, das die lokal zu verarbeitenden Bytes enthält.
	 * Dieses Array ist leer, falls keine lokale Verarbeitung erfolgen soll.
	 * 
	 * @param sender
	 *            Knotennummer, von der das Telegramm geschickt wurde.
	 * @param data
	 *            Bytearray OSI 7 Daten.
	 * @return Ob das empfangene Telegramm auch lokal verarbeitet werden soll.
	 */
	public byte[] dataReceived(int sender, byte[] data) {
		// Wenn keine Informationen vorhanden sind, true zurückgeben
		// Entspricht Verhaltem ohne Redirection-Mechanismus
		if(_redirectionInfo == null) return data;
		
		// Telegramm zerlegen
		TelegramStructure telegramStructure = new TelegramStructure(data);
		// Knotennummer
		int nodeNumber = telegramStructure.getNodeNumber();
		
		// OSI7-Telegramm, die die Teile des übergebenen Telegramms enthält, die
		// (auch) lokal behandelt werden sollen.
		
		TelegramStructure resultLocalTelegram = new TelegramStructure(nodeNumber);
		

		// Zerlegung in Einzeltelegramme
		List<byte[]> singleTelegrams = telegramStructure.getSingleTelegrams();

		Map<Osi7SingleTelegramConverter, Set<Integer>> receiveRedirectionMap = null;
		
		for(int i = 0; i < singleTelegrams.size(); i++) {
			byte[] singleTelegram = singleTelegrams.get(i);
			int fg = telegramStructure.getFgFromSingleTelegram(singleTelegram);
			_debug.fine("Bearbeite Einzeltelegramm von KNR, #, FG;  " + sender +", "+ i +", " + fg);
			
			if(_redirectionInfo.receivedTelegramNormalProcessing(sender, fg)) resultLocalTelegram.addSingleTelegram(singleTelegram);
			
			receiveRedirectionMap = _redirectionInfo.getReceiveRedirectionMap(sender, fg);
			
			if(receiveRedirectionMap != null){
				
				printOsi7TelegramConverters2Destinations(receiveRedirectionMap);
				
				for(Osi7SingleTelegramConverter osi7TelegramConverter : receiveRedirectionMap.keySet()) {
					// Alle Ziele für den Konverter
					Set<Integer> redirectDestinations = receiveRedirectionMap.get(osi7TelegramConverter);
	                if(osi7TelegramConverter!=null){
	                	singleTelegram = osi7TelegramConverter.convert(singleTelegram);
	                }
	                // Nach der Konvertierung kann ein leeres Byte-Array zurückgegeben werden
	                // Um Telegramme mit 0 DE-Blöcken zu vermeiden muss das Einzeltelegramm mehr als 5 Bytes haben
	                if (singleTelegram.length>7){
	                	// Wenn das Einzeltelegramm nur einem DE-Block besteht und der Daten-Endgeräte-Kanal (DE) = 255
	                	// gesetzt ist (z.B. Zeitstempel) soll der Versand unterdrückt werden
	                	if(((singleTelegram[4]&0xff)==1) && ((singleTelegram[6]&0xff)==255)){
	                		// Telegramm verwerfen
	                	}
	                	else{
	                		TelegramStructure telegramToSend = new TelegramStructure(nodeNumber);
	                		
	                		telegramToSend.addSingleTelegram(singleTelegram);
	                		for(Integer redirectionDestination : redirectDestinations) {
	                			_debug.fine("Sende an Ziel: " + redirectionDestination);
	                			try {
	                                _networkLayer.sendWithoutRedirection(redirectionDestination, NetworkLayer.PRIORITY_CLASS_1, telegramToSend.getTelegramBytes(), false);
                                }
                                catch(DestinationUnreachableException e) {
                                	_debug.warning("Senden an das Ziel " + redirectionDestination + " fehlgeschlagen!"
                                			, e
                                			);
                                }
	                		}
	                	}
	                }	                
                }
			}
		}
		
		// Wenn kein Einzeltelegramm enthalten ist, leeres Byte-Array zurückgeben.
		if (resultLocalTelegram.getNumberOfSingletelegrams()==0) return new byte[0];
		return resultLocalTelegram.getTelegramBytes();
	}
	
	/**
	 * Setzt die RedirectionInfo.
	 * @param redirectionInfo
	 */
	public void setRedirectionInfo(RedirectionInfo redirectionInfo) {
		_redirectionInfo = redirectionInfo;
	}
	
}
