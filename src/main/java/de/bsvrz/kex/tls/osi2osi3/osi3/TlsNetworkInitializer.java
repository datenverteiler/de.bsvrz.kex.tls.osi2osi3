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

package de.bsvrz.kex.tls.osi2osi3.osi3;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.ClientReceiverInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.Aspect;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.ConfigurationException;
import de.bsvrz.dav.daf.main.config.ConfigurationObject;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.ObjectSet;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.config.SystemObjectType;
import de.bsvrz.kex.tls.osi2osi3.osi2.api.DataLinkLayer;
import de.bsvrz.kex.tls.osi2osi3.osi2.api.LinkState;
import de.bsvrz.kex.tls.osi2osi3.redirection.Coordinator;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Dient zur Initialisierung der OSI-2-Protokolle und der OSI-3 Routing-Tabellen.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 9602 $ kern
 */
public class TlsNetworkInitializer {

	private static final Debug _debug;

	static {
		_debug = Debug.getLogger();
//		_debug.setLoggerLevel(Debug.WARNING);
	}

	private final ClientDavInterface _daf;

	private final DataModel _config;


	public TlsNetworkInitializer(ClientDavInterface daf, ConfigurationObject localDevice, TlsNetworkLayer networkLayer) {
		try {
			_daf = daf;
			_config = _daf.getDataModel();
			final SystemObjectType typeKri = _config.getType("typ.kri");
			final SystemObjectType typeAntKri = _config.getType("typ.antKri");

			AttributeGroup devicePropertiesAtg = _config.getAttributeGroup("atg.gerät");
			AttributeGroup portPropertiesAtg = _config.getAttributeGroup("atg.anschlussPunkt");
			AttributeGroup remotePortPropertiesAtg = _config.getAttributeGroup("atg.anschlussPunktKommunikationsPartner");

			// Knotennummer des lokalen Geräts bestimmen und setzen
			Data localDeviceProperties = localDevice.getConfigurationData(devicePropertiesAtg);
			int localDeviceAddress = localDeviceProperties.getScaledValue("KnotenNummer").intValue();
			networkLayer.setLocalDeviceAddress(localDeviceAddress);

			//Schleife über alle Geräte
			//   Schleife über alle AnschlussPunkte des lokalen Geräts
			//      Schleife über alle AnschlussPunktKommunikationsPartner
			//         Bei lokalen Verbindungen OSI2-Protokoll initialisieren und dem networkLayer übergeben
			//         Nicht-lokale Verbindungen an networkLayer zwecks Routing übergeben,
			List allDevices = _config.getType("typ.gerät").getElements();

			// folgende Zeile fragt die Geräteeigenschaften von allen Geräten mit einer einzigen
			// Konfigurationsanfrage ab. Die Ergebnisse werden zwischengespeichert.
			// Dito für alle
			_config.getConfigurationData(allDevices, devicePropertiesAtg);

			List allPorts = _config.getType("typ.anschlussPunkt").getElements();
			_config.getConfigurationData(allPorts, portPropertiesAtg);

			List allRemotePorts = _config.getType("typ.anschlussPunktKommunikationsPartner").getElements();
			_config.getConfigurationData(allRemotePorts, remotePortPropertiesAtg);

			for(Iterator deviceIterator = allDevices.iterator(); deviceIterator.hasNext(); ) {
				ConfigurationObject device = (ConfigurationObject)deviceIterator.next();
				// Knotennummer des entfernten Geräts bestimmen
				Data deviceProperties = device.getConfigurationData(devicePropertiesAtg);
				_debug.config("Device: " + device.getNameOrPidOrId());
				if(deviceProperties == null) {
					_debug.error("Am Objekt " + device.getNameOrPidOrId() + " fehlt der Datensatz der atg.gerät mit der Knotennummer");
					throw new IllegalStateException("Am Objekt " + device.getNameOrPidOrId() + " fehlt der Datensatz der atg.gerät mit der Knotennummer");
				}
				int deviceAddress = deviceProperties.getScaledValue("KnotenNummer").intValue();
				ObjectSet devicePortSet = device.getObjectSet("AnschlussPunkteGerät");
				if(devicePortSet != null) {
					for(Iterator devicePortIterator = devicePortSet.getElements().iterator(); devicePortIterator.hasNext(); ) {
						ConfigurationObject port = (ConfigurationObject)devicePortIterator.next();
						Data portProperties = port.getConfigurationData(portPropertiesAtg);
						long portAddress = portProperties.getScaledValue("PortNummer").longValue();
						if((portAddress > 254) || (portAddress < 1)) {
							_debug.error("Ungültige PortNummer " + portAddress + " am Anschlusspunkt " + port);
							// fehlerhafter Anschlusspunkt wir ignoriert
							// Mit nächstem Anschlusspunkt weitermachen
							continue;
						}
						ObjectSet remotePortSet = port.getObjectSet("AnschlussPunkteKommunikationsPartner");
						if(remotePortSet != null) {
							DataLinkLayer linkLayer = null;
							if(device.equals(localDevice)) {
								String portProtocol = portProperties.getTextValue("ProtokollTyp").getText();
								if(portProtocol.equals("")) {
									_debug.warning("Anschlusspunkt '" + port + "' wird ignoriert, da kein Kommunikationsprotokoll angegeben wurde");
									continue;
								}
								else {
									portProtocol = convertOldClassname(portProtocol);
									try {
										linkLayer = (DataLinkLayer)Class.forName(portProtocol).newInstance();
										linkLayer.setDavConnection(daf);
										linkLayer.setLocalAddress((int)portAddress);
										linkLayer.addEventListener(networkLayer.getDataLinkLayerListener());
										subscribeProtocolSettingsParameter(port, linkLayer, ProtocolSettingsParameterReceiver.SettingsType.STANDARD);
									}
									catch(Exception e) {
										_debug.error(
												"Initialisierung des Kommunikationsprotokolls '" + portProtocol + "' " + "am Anschlusspunkt '" + port + "' "
												+ "fehlgeschlagen: " + e
										);
										// fehlerhafter Anschlusspunkt wird ignoriert
										// Mit nächstem Anschlusspunkt weitermachen
										continue;
									}
								}
							}
							for(Iterator remotePortIterator = remotePortSet.getElements().iterator(); remotePortIterator.hasNext(); ) {
								ConfigurationObject remotePort = (ConfigurationObject)remotePortIterator.next();
								long t0 = System.currentTimeMillis();
								Data remotePortProperties = remotePort.getConfigurationData(remotePortPropertiesAtg);
								long t1 = System.currentTimeMillis();
								_debug.finest("Dauer der Abfrage von Konfigurationsdaten: " + (t1 - t0));
								long remotePortAddress = remotePortProperties.getUnscaledValue("PortNummer").longValue();
								SystemObject remoteDevice = remotePortProperties.getReferenceValue("KommunikationsPartner").getSystemObject();
								// Knotennummer des entfernten Geräts bestimmen
								Data remoteDeviceProperties = remoteDevice.getConfigurationData(devicePropertiesAtg);
								int remoteDeviceAddress = remoteDeviceProperties.getScaledValue("KnotenNummer").intValue();
								final boolean isLinkTransparent = (typeKri != null) && remoteDevice.isOfType(typeKri);
								final boolean mirroredReceive = (typeAntKri != null) && remoteDevice.isOfType(typeAntKri);
								final boolean dontIncrementOsi3Pointer = (typeAntKri != null) && remoteDevice.isOfType(typeAntKri);
								DataLinkLayer upLinkLayer = null;
								if(remoteDevice.equals(localDevice)) {
									//Bei Verbindungen zum lokalen Gerät entsprechendes Protokoll initialisieren
									String portProtocol = remotePortProperties.getTextValue("ProtokollTyp").getText();
									if(portProtocol.equals("")) {
										_debug.warning(
												"Die Verbindung zum Gerät '" + device + "' am " + "AnschlusspunktKommunikationspartner '" + remotePort + "' "
												+ "wird ignoriert, da kein Kommunikationsprotokoll angegeben wurde"
										);
										continue;
									}
									else {
										portProtocol = convertOldClassname(portProtocol);
										_debug.info("Initialisierung des Kommunikationsprotokolls : " + portProtocol);
										try {
											upLinkLayer = (DataLinkLayer)Class.forName(portProtocol).newInstance();
											upLinkLayer.setDavConnection(daf);
											upLinkLayer.setLocalAddress((int)remotePortAddress);
											upLinkLayer.addEventListener(networkLayer.getDataLinkLayerListener());
											subscribeProtocolSettingsParameter(port, upLinkLayer, ProtocolSettingsParameterReceiver.SettingsType.STANDARD);
										}
										catch(Exception e) {
											_debug.error(
													"Initialisierung des Kommunikationsprotokolls '" + portProtocol + "' " + "für die Verbindung zum Gerät '"
													+ device + "' " + "am AnschlusspunktKommunikationspartner '" + remotePort + "' " + "fehlgeschlagen: " + e
											);
											// fehlerhafter AnschlusspunktKommunikationspartner wird ignoriert
											// Mit nächstem AnschlusspunktKommunikationspartner weitermachen
											continue;
										}
									}
								}
								try {
									DataLinkLayer.Link link = null;
									if(linkLayer != null) {
										link = linkLayer.createLink((int)remotePortAddress);
										subscribeProtocolSettingsParameter(remotePort, link, ProtocolSettingsParameterReceiver.SettingsType.PRIMARY);
									}
									else if(upLinkLayer != null) {
										link = upLinkLayer.createLink((int)portAddress);
										subscribeProtocolSettingsParameter(remotePort, link, ProtocolSettingsParameterReceiver.SettingsType.SECONDARY);
									}
									networkLayer.addLink(
											deviceAddress,
											(int)portAddress,
											linkLayer,
											link,
											(int)remotePortAddress,
											remoteDeviceAddress,
											isLinkTransparent,
											mirroredReceive,
											dontIncrementOsi3Pointer
									);
								}
								catch(Exception e) {
									_debug.error(
											"Initialisierung der Verbindung KNr-" + deviceAddress + "/P-" + portAddress + " -->" + " P-" + remotePortAddress
											+ "/KNr-" + remoteDeviceAddress + " fehlgeschlagen" + e
									);
								}
							}
						}
					}
				}
			}
			networkLayer.completeInitialization();
			Coordinator redirectionCoordinator = new Coordinator(daf, localDevice, networkLayer);

			networkLayer.setTelegramProcessor(redirectionCoordinator.getTelegramProcessor());
		}
		catch(ConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Diese Methode ersetzt die früher (vor der Umstellung der Packagestruktur) verwendeten Klassennamen für OSI-2 Module durch die neueren Namen.
	 *
	 * @param portProtocol Evtl. zu ersetzende Klassenname
	 *
	 * @return Neuer Klassenname, falls der übergebene Klassenname ersetzt werden muss; übergebene Klassenname falls keine Ersetzung durchgeführt wurde.
	 */
	private String convertOldClassname(final String portProtocol) {
		String convertedName = null;
		if(portProtocol.equals("kex.tls.osi2.wancom.Client")) {
			convertedName = "de.bsvrz.kex.tls.osi2osi3.osi2.wancom.Client";
		}
		else if(portProtocol.equals("kex.tls.osi2.tc57.Primary")) {
			convertedName = "de.bsvrz.kex.tls.osi2osi3.osi2.tc57primary.Primary";
		}
		else if(portProtocol.equals("kex.tls.osi2.tc57.Wiretapper")) {
			convertedName = "de.bsvrz.kex.tls.osi2osi3.osi2.tc57listen.Wiretapper";
		}
		if(convertedName != null) {
			_debug.info("Protokollname '" + portProtocol + "' wurde ersetzt durch neuen Namen '" + convertedName + "'");
			return convertedName;
		}
		else {
			return portProtocol;
		}
	}

	private void subscribeProtocolSettingsParameter(
			ConfigurationObject port, Object linkOrLinkLayer, final ProtocolSettingsParameterReceiver.SettingsType settingsType) {
		ProtocolSettingsParameterReceiver receiver = new ProtocolSettingsParameterReceiver(_daf, port, linkOrLinkLayer, settingsType);
		receiver.subscribe();
	}

	/** Klasse zur An- und Abmeldung sowie für den Empfang von Parameter-Datensätzen für OSI-2-Protokolle und Verbindungen. */
	private static class ProtocolSettingsParameterReceiver implements ClientReceiverInterface {

		public enum SettingsType {
			STANDARD,
			PRIMARY,
			SECONDARY
		}

		private final ClientDavInterface _connection;

		private final ConfigurationObject _port;

		private final Object _linkOrLinkLayer;

		private final SettingsType _settingsType;

		/**
		 * Aktualisierungsmethode, die nach Empfang eines Parameter-Datensatzes von den Datenverteiler-Applikationsfunktionen aufgerufen wird.
		 * <p/>
		 * Der Datensatz wird in ein Properties-Objekt konvertiert und mit der Methode {@link DataLinkLayer#setProperties(java.util.Properties)} an das Protokoll
		 * übergeben. Nach dem Empfang des ersten Datensatzes für ein Protokoll wird das Protokoll gestartet.
		 *
		 * @param results Feld mit den empfangenen Ergebnisdatensätzen.
		 */
		public void update(ResultData results[]) {
			Properties properties = new Properties();
			//Wenn mehrere Aktualisierungen gleichzeitig empfangen werden, dann wird nur die letzte ausgewertet
			Data data = results[results.length - 1].getData();
			// Bei leerem Datensatz wird ein leeres Properties-Objekt verwendet.
			if(data != null) {
				Data.Array settingsArray = data.getItem("ProtokollEinstellung").asArray();
				for(int i = 0; i < settingsArray.getLength(); i++) {
					Data arrayEntry = settingsArray.getItem(i);
					String name = arrayEntry.getTextValue("Name").getValueText();
					String value = arrayEntry.getTextValue("Wert").getValueText();
					properties.setProperty(name, value);
				}
			}
			if(_linkOrLinkLayer instanceof DataLinkLayer) {
				DataLinkLayer dataLinkLayer = (DataLinkLayer)_linkOrLinkLayer;
				dataLinkLayer.setProperties(properties);
				if(!dataLinkLayer.isStarted()) dataLinkLayer.start();
			}
			else if(_linkOrLinkLayer instanceof DataLinkLayer.Link) {
				DataLinkLayer.Link link = (DataLinkLayer.Link)_linkOrLinkLayer;
				link.setProperties(properties);
				if(link.getState() == LinkState.DISCONNECTED) link.connect();
			}
		}

		/**
		 * Erzeugt ein neues Objekt der Klasse.
		 *
		 * @param connection      Datenverteiler-Verbindung.
		 * @param port            Anschlußpunkt an dem die Parameter des Protokolls verwaltet werden.
		 * @param linkOrLinkLayer OSI-2-Protokollinstanz bzw. OSI-2-Verbindung, die bei Änderungen informiert werden soll.
		 */
		public ProtocolSettingsParameterReceiver(ClientDavInterface connection, ConfigurationObject port, Object linkOrLinkLayer, SettingsType settingsType) {
			_connection = connection;
			_port = port;
			_linkOrLinkLayer = linkOrLinkLayer;
			_settingsType = settingsType;
		}

		/** Anmelden des Parameterdatensatzes beim Datenverteiler. */

		public void subscribe() {
			try {
				DataModel configuration = _connection.getDataModel();
				final AttributeGroup atg;
				switch(_settingsType) {
					case PRIMARY:
						atg = configuration.getAttributeGroup("atg.protokollEinstellungenPrimary");
						break;
					case SECONDARY:
						atg = configuration.getAttributeGroup("atg.protokollEinstellungenSecondary");
						break;
					case STANDARD:
						atg = configuration.getAttributeGroup("atg.protokollEinstellungenStandard");
						break;
					default:
						atg = null;
				}
				Aspect aspect = configuration.getAspect("asp.parameterSoll");
				DataDescription dataDescription = new DataDescription(atg, aspect);
				_connection.subscribeReceiver(this, _port, dataDescription, ReceiveOptions.normal(), ReceiverRole.receiver());
			}
			catch(ConfigurationException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

		/** Abmelden des Parameterdatensatzes beim Datenverteiler. */
		public void unSubscribe() {
			try {
				DataModel configuration = _connection.getDataModel();
				AttributeGroup atg;
				if(_linkOrLinkLayer instanceof DataLinkLayer) {
					atg = configuration.getAttributeGroup("atg.protokollEinstellungenStandard");
				}
				else {
					atg = configuration.getAttributeGroup("atg.protokollEinstellungenPrimary");
				}
				Aspect aspect = configuration.getAspect("asp.parameterSoll");
				DataDescription dataDescription = new DataDescription(atg, aspect);
				_connection.unsubscribeReceiver(this, _port, dataDescription);
			}
			catch(ConfigurationException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}
}
