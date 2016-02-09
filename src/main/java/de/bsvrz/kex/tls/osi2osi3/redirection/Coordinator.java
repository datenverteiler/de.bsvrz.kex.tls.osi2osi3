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

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.ClientReceiverInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.Aspect;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.ConfigurationObject;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.ObjectTimeSpecification;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.config.SystemObjectType;
import de.bsvrz.kex.tls.osi2osi3.osi3.NetworkLayerSender;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.*;

/**
 * Koordinator der OSI2/OSI3 Umleitung. 
 * Diese Klasse koordiniert den OSI2/OSI3 Umleitungsmechanismus.
 * Sie sorgt dafür, dass die Kommunikation zwischen den benötigten Klassen 
 * hergestellt wird und sorgt für die Auswertung der Umleitungsparameter.  
 * 
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 10172 $
 *
 */
public class Coordinator {

	private static final Debug _debug = Debug.getLogger();
	private final TelegramProcessor _telegramProcessor;
	private final ClientDavInterface _daf;	
	private final ConfigurationObject _localDevice;
	private ConfigurationObject _osi3RedirectionDevice=null;
	private final NetworkLayerSender _networkLayer;	
	private TlsModel _tlsModel;
	private WildcardProcessor _wildcardProcessor;
	private DataModel _config;
	
	/**
	 * Konstruktor für einen neuen Coordinator. 
	 * Meldet sich auf den Parameterdatensatz für die OSI3 Weiterleitung an.
	 * 
	 * @param daf  Datenverteilerverbindung.
	 * @param localDevice Device, für das KExTls gestartet wurde (z.B. eine VRZ oder UZ).
	 * @param networkLayer Networklayer, wird für den TelegramProcessor benötigt.
	 */
	public Coordinator(ClientDavInterface daf, ConfigurationObject localDevice, NetworkLayerSender networkLayer) {
		_daf = daf;
		_config = daf.getDataModel();
		_localDevice = localDevice;
		_networkLayer = networkLayer;
		_telegramProcessor = new TelegramProcessor(networkLayer);
		_tlsModel = new TlsModel(_daf.getDataModel());
		_wildcardProcessor = new WildcardProcessor(_tlsModel);
		
		Aspect aspectParamNormative = _config.getAspect("asp.parameterSoll");
		AttributeGroup atgOsi3RedirectionParam = _config.getAttributeGroup("atg.osi3UmleitungsParameter");
		
		AttributeGroup atgOsi3RedirectionProperties = _config.getAttributeGroup("atg.osi3UmleitungsEigenschaften");
		Aspect aspectProperties = _config.getAspect("asp.eigenschaften");
		
		_debug.info("Coordinator für OSI3 Redirection aufgerufen", _localDevice);

		// Achtung, falls die ATG nicht vorhanden sein sollte (SW läuft mit einem "alten" Datenmodell darf kein Fehler 
		// auftauchen, sondern das bisherige Verhalten muss unterstützt werden
		if(atgOsi3RedirectionParam==null){
//			_debug.config("Die SW läuft nicht mit dem erforderlichen Datenmodell.");
//			_debug.config("Bitte den Konfigurationsbereich x übernehmen.");
		}
		else if(localDevice==null){
//			_debug.config("Das Gerät localDevice darf nicht null sein!");
		}
		else{
			// Testen, ob genau ein Objekt vom Typ typ.osi3Umleitung vorhanden ist, das localDevice referenziert
			Collection<SystemObjectType> systemObjectTypes = new ArrayList<SystemObjectType>();
			systemObjectTypes.add(_config.getType("typ.osi3Umleitung"));
			Collection<SystemObject> osi3RedirectionObjects = _config.getObjects(null, systemObjectTypes, ObjectTimeSpecification.valid());
			int count=0;

			for(SystemObject osi3RedirectionObject : osi3RedirectionObjects) {
				Data configurationData = osi3RedirectionObject.getConfigurationData(atgOsi3RedirectionProperties, aspectProperties);
				SystemObject deviceReference = configurationData.getReferenceValue("TlsKnoten").getSystemObject();
				_debug.info("Device Referenz gefunden", deviceReference);
				if (deviceReference.equals(_localDevice)){
					count++;
					_osi3RedirectionDevice=(ConfigurationObject)osi3RedirectionObject;
				}
			}
			if(count>1) _debug.warning("Keine eindeutige Zuordnung für die Ermittlung des Redirectionobjektes möglich!");
			if(count==1 && _osi3RedirectionDevice!=null){
				_debug.info("melde Objekt für die Parametrierung den OSI3 Weiterleitungsparameter an" , _osi3RedirectionDevice);

				DataDescription dataDescription = new DataDescription(atgOsi3RedirectionParam, aspectParamNormative);
				_daf.subscribeReceiver(new Receiver(), _osi3RedirectionDevice, dataDescription, ReceiveOptions.normal(), ReceiverRole.receiver());
			}
		}
	}
	
	/**
	 * Gibt das Device zurück, für das der Parameter OSI3 Weiterleitung ausgewertet wird.
     * @return Konfigurationsobjekt, für das der OSI3 Weiterleitungsparameter ausgewertet wird.
     */
    public ConfigurationObject getOsi3RedirectionDevice() {
    	return _osi3RedirectionDevice;
    }

    /**
     * Gibt den TelegramProcessor zurück.
     * @return TelegramProcessor
     */
	public TelegramProcessor getTelegramProcessor() {
		return _telegramProcessor;
	}
	
	
	/**
	 * Receiver für den Parameterdatensatz für die OSI3 Weiterleitung.
	 * Erzeugt bei jedem neuen Datensatz die Weiterleitungsinformationen und gibt diese 
	 * an den TelegramProcessor weiter. 
	 * 
	 * @author Kappich Systemberatung
	 * @version $Revision: 10172 $
	 *
	 */
	private class Receiver implements ClientReceiverInterface {
		/**
		 * Update Methode.
		 * Immer wenn ein neuer Datensatz erhalten wird, der ungleich null ist,
		 * wird die Erzeugung einer neuen RedirectionInfo initiiert und diese wird 
		 * an den TelegramProcessor weitergegeben.
		 */
		public void update(ResultData[] results) {
			try {
				for(ResultData resultData : results) {
					Data data = resultData.getData();

					_debug.fine("Update für Objekt" + resultData.getObject().getNameOrPidOrId() + " für " +  resultData.getDataDescription());
					_debug.fine("Datensatz erhalten: " + data);

					if(data!=null){
						RedirectionInfo redirectionInfo = _wildcardProcessor.createRedirectionInfo(data);
						_telegramProcessor.setRedirectionInfo(redirectionInfo);
						redirectionInfo.printAllEntries();
					}
					else {
						_telegramProcessor.setRedirectionInfo(null);
					}
				}
			}
			catch(Exception e) {
				_debug.warning("Parameterdatensatz für OSI3-Weiterleitung konnte nicht ausgewertet werden", e);
				
			}
		}
	}

}
