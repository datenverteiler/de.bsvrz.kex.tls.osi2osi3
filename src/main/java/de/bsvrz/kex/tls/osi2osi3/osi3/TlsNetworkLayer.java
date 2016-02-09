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

import de.bsvrz.kex.tls.osi2osi3.longtelegram.Osi7LongTelegramRecombine;
import de.bsvrz.kex.tls.osi2osi3.longtelegram.Osi7LongTelegramSegment;
import de.bsvrz.kex.tls.osi2osi3.osi2.api.DataLinkLayer;
import de.bsvrz.kex.tls.osi2osi3.osi2.api.DataLinkLayerEvent;
import de.bsvrz.kex.tls.osi2osi3.osi2.api.DataLinkLayerListener;
import de.bsvrz.kex.tls.osi2osi3.redirection.TelegramProcessor;
import de.bsvrz.sys.funclib.concurrent.PriorityChannel;
import de.bsvrz.sys.funclib.concurrent.PriorizedObject;
import de.bsvrz.sys.funclib.debug.Debug;
import de.bsvrz.sys.funclib.hexdump.HexDumper;

import java.util.*;

/**
 * Implementierung der TLS-OSI-3 Netzwerkebene.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 13027 $
 */
public class TlsNetworkLayer implements NetworkLayer, NetworkLayerSender {

	private static final Debug _debug;

	static {
		_debug = Debug.getLogger();
//		_debug.setLoggerLevel(Debug.WARNING);
	}

	public int _localDeviceAddress = -1;

	private final LinkedList<NetworkLayerListener> _networkLayerListeners = new LinkedList<NetworkLayerListener>();

	private Map _device2RouteMap = new HashMap();

	private Map _device2DirectRoutedDeviceListMap = new HashMap();

	private final AsyncDataLinkLayerEventHandler _asyncDataLinkLayerEventHandler = new AsyncDataLinkLayerEventHandler();

	private final Osi7LongTelegramRecombine _longTelegramCombiner = new Osi7LongTelegramRecombine();

	private Osi7LongTelegramSegment _longTelegramSegmenter = null;

	public TlsNetworkLayer() {
	}

	public void addEventListener(NetworkLayerListener networkLayerListener) {
		synchronized(_networkLayerListeners) {
			_networkLayerListeners.add(networkLayerListener);
		}
	}

	public void removeEventListener(NetworkLayerListener networkLayerListener) {
		synchronized(_networkLayerListeners) {
			_networkLayerListeners.remove(networkLayerListener);
		}
	}

	public int[] getRoutedRemoteDevices(int device) {
		Set routedRemoteDevices = new TreeSet();
		addAllRoutedRemoteDevices(new Integer(device), routedRemoteDevices);
		int[] resultArray = new int[routedRemoteDevices.size()];
		int i = 0;
		for(Iterator iterator = routedRemoteDevices.iterator(); iterator.hasNext(); ) {
			Integer remoteDevice = (Integer)iterator.next();
			resultArray[i++] = remoteDevice.intValue();
		}
		return resultArray;
	}

	private void addAllRoutedRemoteDevices(Integer device, Collection resultCollection) {
		List directRoutedDeviceList = (List)_device2DirectRoutedDeviceListMap.get(device);
		if(directRoutedDeviceList != null) {
			for(Iterator iterator = directRoutedDeviceList.iterator(); iterator.hasNext(); ) {
				Integer remoteDevice = (Integer)iterator.next();
				resultCollection.add(remoteDevice);
				addAllRoutedRemoteDevices(remoteDevice, resultCollection);
			}
		}
	}

	public void sendData(int destination, byte[] data, boolean longTelegram) throws DestinationUnreachableException {
		sendData(destination, PRIORITY_CLASS_1, data, longTelegram);
	}

	/**
	 * Methode zum Versenden von Telegrammen mit vorgegebener Priorität
	 *
	 * @param destination  Knotennummer, an die gesendet werden soll
	 * @param priority     Priorität, unter der das Telegramm versendet werden soll
	 * @param data         Bytearray Telegrammdaten OSI7
	 * @param longTelegram true = Es soll ein Langtelegramm verschickt werden, das nicht der TLS-Norm entspricht
	 *
	 * @throws DestinationUnreachableException
	 *
	 */
	public void sendData(int destination, int priority, byte[] data, boolean longTelegram) throws DestinationUnreachableException {
		_debug.fine("TlsNetworkLayer.send: Soll Daten der Klasse " + priority + " an Knoten " + destination + " senden");
		_debug.finer(HexDumper.toString(data));
		if(priority < 1 || priority > 2) throw new IllegalArgumentException("Ungültige Priorität");
		if(data == null) throw new IllegalArgumentException("Keine Daten");
		boolean normalProcessing = true;
		if(!longTelegram) {
			normalProcessing = _telegramProcessor.dataToSend(destination, data);
		}
		if(normalProcessing) {
			_debug.fine("Das Telegramm soll (auch) an das vorgegebene Ziel gesendet werden (Normalprocessing)");
			sendWithoutRedirection(destination, priority, data, longTelegram);
		}
		else {
			_debug.fine("TlsNetworkLayer.send: Die Daten der Klasse " + priority + " an Knoten " + destination + " wurden unterdrückt");
			_debug.finer(HexDumper.toString(data));
		}
	}

	public void sendWithoutRedirection(int destination, int priority, byte[] data, boolean longTelegram) throws DestinationUnreachableException {
		_debug.fine("TlsNetworkLayer.sendWithoutRedirection: Sende Daten der Klasse " + priority + " an Knoten " + destination);
		_debug.finer(HexDumper.toString(data));
		if(destination == _localDeviceAddress) {
			DataLinkLayerEvent event = new DataLinkLayerEvent(null, null, DataLinkLayerEvent.Type.DATA_RECEIVED, data);
			_asyncDataLinkLayerEventHandler.handleDataLinkLayerEvent(event);
		}
		RouteInfo routeInfo = getRouteInfo(destination);
		if(routeInfo == null) {
			throw new DestinationUnreachableException("Keine Route versorgt für Zielknoten " + destination);
		}
		byte[] tlsOsi3Header = routeInfo._tlsOsi3Header;
		if(tlsOsi3Header == null) {
			throw new DestinationUnreachableException("Routing nicht ermittelbar für Zielknoten " + destination);
		}
		DataLinkLayer.Link link = routeInfo._firstLinkInfo._link;
		if(link == null) {
			throw new DestinationUnreachableException("kein Protokoll für ersten Vermittlungsabschnitt versorgt. Zielknoten " + destination);
		}
		if(!longTelegram) {
			// Es ist kein Langtelegramm, also keine Sonderbehandlung
			sendData(link, priority, tlsOsi3Header, data);
		}
		else {
			// Es soll ein Langtelegramm verschickt werden, das nicht der TLS Norm entspricht

			if(_longTelegramSegmenter == null) {
				// Das Objekt wird erzeugt, wenn ein Langtelegramm verschickt werden soll.
				// Threads und Speicher werden also erst dann angefordert, wenn sie auch wirklich benötigt werden
				_longTelegramSegmenter = new Osi7LongTelegramSegment(_localDeviceAddress, this);
			}

			_longTelegramSegmenter.sendLongData(destination, data, priority);
		}
	}

	private void sendData(DataLinkLayer.Link link, int priority, byte[] tlsOsi3Header, byte[] data) throws DestinationUnreachableException {
		// Es ist kein Langtelegramm oder es wird nur durchgereicht

		byte[] frame;

		if(isReducingToControlByte(link)) {
			frame = new byte[data.length + 1];
			frame[0] = 0;
			System.arraycopy(data, 0, frame, 1, data.length);
		}
		else {
			int frameLength = tlsOsi3Header.length + data.length;
			frame = new byte[frameLength];
			System.arraycopy(tlsOsi3Header, 0, frame, 0, tlsOsi3Header.length);
			System.arraycopy(data, 0, frame, tlsOsi3Header.length, data.length);
		}

		//Prio im ersten Routingbyte eintragen, oberstes Bit wird bei Prioritätsklasse 2 gesetzt
		if(priority == PRIORITY_CLASS_2) frame[0] |= 0x80;
		try {
			link.send(frame, priority);
		}
		catch(UnsupportedOperationException e) {
			throw new DestinationUnreachableException("Versand wird vom OSI-2 Protokoll nicht unterstützt: " + e);
		}
		catch(InterruptedException e) {
			throw new DestinationUnreachableException("Versand wurde unterbrochen: " + e);
		}
		catch(IllegalStateException e) {
			throw new DestinationUnreachableException(e.getLocalizedMessage());
		}
	}

	public void start() {
		_asyncDataLinkLayerEventHandler.start();
	}

	public DataLinkLayerListener getDataLinkLayerListener() {
		return _asyncDataLinkLayerEventHandler;
	}

	/**
	 * Weiterleitung eines empfangenen Langtelegramms an die Osi7 Schicht, falls es vollständig ist.
	 *
	 * @param sender Absender des Telegramms
	 * @param data   Telegramm/Nutzdaten für die Osi7 Schicht
	 */
	private void notifyLongReceive(int sender, byte[] data) {

		// Es wurde ein Langtelegramm, bzw. ein Teil empfangen. Dies wird eingefügt, bzw ein neues Langtelegramm wird erzeugt.
		if(_longTelegramCombiner.telegramReceived(data) == true) {
			// Langtelegramm anfordern, damit das richtige Langtelegramm gefunden werden kann, wird das letzte Teiltelegramm
			// mitgegeben
			final byte[] longTelegram = _longTelegramCombiner.getLongTelegram(data);
			synchronized(_networkLayerListeners) {
				Iterator iterator = _networkLayerListeners.iterator();
				while(iterator.hasNext()) {
					NetworkLayerListener listener = ((NetworkLayerListener)iterator.next());
					try {
						// true, weil dies ein besonderes Langtelegramm ist, das nicht der TLS Definition entspricht
						listener.dataReceived(sender, longTelegram, true);
					}
					catch(Exception e) {
						String nl = System.getProperty("line.separator");
						_debug.error(
								"Ausnahme bei Aufruf von dataReceived des NetworkLayerListeners " + nl + "Listener: " + listener + nl + "Exception: " + e + nl
								+ "Knotennummer Absender: " + sender + nl + "data: " + HexDumper.toString(data) + "Langtelegramm: true"
						);
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Weiterleitung eines empfangenen Telegramms an die Osi7-Schicht.
	 *
	 * @param sender Absender des Telegramms
	 * @param data   Telegramm/Nutzdaten für die Osi7 Schicht
	 */
	private void notifyReceive(int sender, byte[] data) {
		synchronized(_networkLayerListeners) {
			Iterator iterator = _networkLayerListeners.iterator();
			while(iterator.hasNext()) {
				NetworkLayerListener listener = ((NetworkLayerListener)iterator.next());
				try {
					// dataReceived(sender, data) gibt ein byte[] zurück.
					// Wenn leeres byte[] => Keine lokale Verarbeitung
					// Die Mindestgröße des Byte-Arrays sollte >= 9 sein
					// OSI7 Telegramm (Allgemeiner Telegrammkopf (4 Bytes) + Einzeltelegrammkopf (5 Bytes)
					final byte[] normalProcessingBytes = _telegramProcessor.dataReceived(sender, data);
					if(normalProcessingBytes.length >= 9) listener.dataReceived(sender, normalProcessingBytes, false);
				}
				catch(Exception e) {
					String nl = System.getProperty("line.separator");
					_debug.error(
							"Ausnahme bei Aufruf von dataReceived des NetworkLayerListeners " + nl + "Listener: " + listener + nl + "Exception: " + e + nl
							+ "Knotennummer Absender: " + sender + nl + "data: " + HexDumper.toString(data) + "Langtelegramm: false"
					);
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Benachrichtigung der Osi7-Listener über die Änderung eines Verbindungsstatus
	 *
	 * @param device Zustandsänderung von diesem Gerät
	 * @param state  Zustand der Verbindung. 0 steht für verbunden, sonst ist die Verbindung unterbrochen.
	 */
	private void notifyStateChange(int device, int state) {
		_debug.info("Statusänderung: Knoten " + device + ", Status " + (state == 0 ? "VERBUNDEN" : "UNTERBROCHEN"));
		synchronized(_networkLayerListeners) {
			Iterator iterator = _networkLayerListeners.iterator();
			while(iterator.hasNext()) {
				NetworkLayerListener listener = ((NetworkLayerListener)iterator.next());
				try {
					listener.stateChanged(device, state);
				}
				catch(Exception e) {
					String nl = System.getProperty("line.separator");
					_debug.error(
							"Ausnahme bei Aufruf von stateChanged des NetworkLayerListeners " + nl + "Listener: " + listener + nl + "Exception: " + e + nl
							+ "Knotennummer Gerät: " + device + nl + "state: " + state
					);
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Es wird geprüft ob die Option auf ReduzierungAufSteuerbyte aktiviert ist, d.h. der Osi3-Header wird auf das Steuerbyte reduziert (Osi3-Routinginformationen
	 * entfernt) Falls keine Einstellung dafür vorhanden ist, wird das Telegram normal (mit Osi3-Routinginformationen) weitergeleitet.
	 *
	 * @param link Verbindung, von der die Einstellungen bezogen werden
	 *
	 * @return <code>false</code>: Osi3-Adressen werden nicht ignoriert <br><code>true</code>: Osi3-Adressen werden ignoriert und werden bis auf das Steuerbyte
	 *         entfernt.
	 */
	public boolean isReducingToControlByte(final DataLinkLayer.Link link) {
		if(link.getProperty(TlsNetworkLayerSetting.reduceToControlByte) != null) {
			try {
				return TlsNetworkLayerSetting.getBooleanProperty(link.getProperty(TlsNetworkLayerSetting.reduceToControlByte));
			}
			catch(IllegalArgumentException ex) {
				_debug.warning(
						"Der Parameter atg.protokollEinstellungenStandard, " + TlsNetworkLayerSetting.reduceToControlByte
						+ " hat einen ungültigen Wert.\n", ex
				);
			}
		}
		return false;
	}

	/**
	 * Wird aufgerufen, wenn ein Daten-Telegramm von der Osi2 Schnittstelle empfangen wird.
	 * <p/>
	 * Wenn das Telegramm keine Informationen über den Verbindungdstatus beinhaltet, wird das Telegram anhand des Osi3-Header-Information (Steuerbyte und
	 * Routinginformationen) ausgewertet und weitergeleitet.
	 *
	 * @param event Beinhaltet das Telegram und die verwendeten Schnittstellen-Informationen.
	 */
	public void handleDataLinkLayerEvent(DataLinkLayerEvent event) {
		if(event.getType() == DataLinkLayerEvent.Type.DATA_RECEIVED) {
			final byte[] data = (byte[])event.getData();
			try {
				final DataLinkLayer.Link link = event.getLink();
				if(link == null) {
					// Daten wurden nicht über eine OSI-2 Verbindung empfangen, dies kann nur durch die
					// Loopback-Funktion, d.h. Aufruf der sendData-Funktion mit dem lokalen Knoten als
					// Ziel passieren. In diesem Fall ist kein OSI-3 Header in den Daten enthalten.
					_debug.info("Daten via Loopback empfangen, Absender: " + _localDeviceAddress + "\nOSI7: " + HexDumper.toString(data));
					notifyReceive(_localDeviceAddress, data);
					return;
				}
				final int osi3HeaderByte = data[0] & 0xff;
				final int osi3Pointer = osi3HeaderByte & 0x07;
				final int osi3Length = (osi3HeaderByte >>> 3) & 0x07;
				// Wenn oberstes Bit 0 ist, dann wird Prioritätsklasse
				final int osi3PriorityClass = ((osi3HeaderByte & 0x80) == 0) ? PRIORITY_CLASS_1 : PRIORITY_CLASS_2;
				final int dataOffset = 1 + osi3Length * 2;
				if(dataOffset > data.length) throw new Exception("OSI 3 Längenangabe fehlerhaft");
				if(osi3Pointer > osi3Length) throw new Exception("OSI 3 Pointer größer als OSI 3 Länge");
				final byte[] nextLayerData = new byte[data.length - dataOffset];
				System.arraycopy(data, dataOffset, nextLayerData, 0, nextLayerData.length);
				final byte[] osi3HeaderData = new byte[dataOffset];
				System.arraycopy(data, 0, osi3HeaderData, 0, dataOffset);

				final boolean osi3IgnoreReceivedAddress;
				String osi3IgnoreReceivedAddressValue = link.getProperty("osi3.adresseBeiEmpfangIgnorieren");
				if(osi3IgnoreReceivedAddressValue == null) osi3IgnoreReceivedAddressValue = "";
				osi3IgnoreReceivedAddressValue = osi3IgnoreReceivedAddressValue.trim().toLowerCase();
				osi3IgnoreReceivedAddress = osi3IgnoreReceivedAddressValue.equals("ja") || osi3IgnoreReceivedAddressValue.equals("yes")
				                            || osi3IgnoreReceivedAddressValue.equals("true") || osi3IgnoreReceivedAddressValue.equals("wahr");

				if(!osi3IgnoreReceivedAddress && osi3Pointer != 0 && osi3Pointer < osi3Length) {
					// Telegramm muss weiter vermittelt werden

					// Verbindungsinfo zu nächstem Vermittlungsabschnitt bestimmen
					int newOsi3Pointer = osi3Pointer + 1;
					final int portAddress1 = osi3HeaderData[newOsi3Pointer * 2 - 1] & 0xff;
					final int portAddress2 = osi3HeaderData[newOsi3Pointer * 2] & 0xff;
					LinkInfo linkInfo = findLinkInfo(_localDeviceAddress, portAddress1, portAddress2, true);
					if(linkInfo == null) {
						throw new Exception(
								"Keine Verbindung zur OSI-3 Vermittlung über Vermittlungsabschnitt " + newOsi3Pointer + " (" + portAddress1 + "->"
								+ portAddress2 + ")"
						);
					}
					if(linkInfo._link == null) {
						throw new Exception(
								"Keine Kommunikation auf Verbindung zur OSI-3 Vermittlung über Vermittlungsabschnitt " + newOsi3Pointer + " (" + portAddress1
								+ "->" + portAddress2 + ")"
						);
					}
					_debug.info(
							"Telegramm wird weitervermittelt, \nOSI3: " + HexDumper.toString(data, 0, dataOffset) + "\nOSI7: " + HexDumper.toString(
									data, dataOffset, -1
							)
					);
					//Der Einfachheit halber wird hier das ganze Byte incrementiert, betroffen ist aber nur der osi3Pointer in den unteren 3 Bits
					if(!linkInfo._dontIncrementOsi3Pointer) osi3HeaderData[0] = (byte)(osi3HeaderByte + 1);

					//Es wird geprüft ob die Option auf ReduzierungAufSteuerbyte aktiviert ist,
					//d.h. das Telegram wird auf das Steuerbyte reduziert (Osi3 Routinginformationen entfernt)
					//Falls keine Einstellung dafür vorhanden ist, wird das Telegram normal (mit Osi3 Routinginformationen)
					//weitergeleitet
					if(isReducingToControlByte(link)) {
						sendData(linkInfo._link, osi3PriorityClass, new byte[]{0}, nextLayerData);
					}
					else {
						sendData(linkInfo._link, osi3PriorityClass, osi3HeaderData, nextLayerData);
					}
					//ENDE
					//sendData(linkInfo._link, osi3PriorityClass, osi3HeaderData, nextLayerData);


				}
				else if(osi3IgnoreReceivedAddress || osi3Pointer == 0 || osi3Pointer == osi3Length) {
					// Zielknoten des Telegramms ist erreicht
					int senderDevice = 0;
					int lastDevice = -1;
					// Bestimmen der Verbindung über die das Telegramm empfangen wurde, dazu werden statt OSI-3 Adressen
					// OSI-2 Adressen benutzt
					LinkInfo inputLinkInfo = findLinkInfo(
							_localDeviceAddress, event.getDataLinkLayer().getLocalAddress(), event.getLink().getRemoteAddress(), false
					);
					_debug.fine(inputLinkInfo == null ? "Kein weg gefunden." : "Ein Weg gefunden zu " + inputLinkInfo);
					// Bestimmen des Absender-Knotens des Telegramms mithilfe der OSI-2 oder OSI-3 Adressen
					if(osi3IgnoreReceivedAddress || osi3Length == 0) {
						// Bei Null-Routing wird als Absender-Knoten das auf der OSI-2 Verbindung gegenüberliegende Gerät verwendet
						senderDevice = inputLinkInfo._remoteDevice;
					}
					else {
						if(osi3Pointer == 0) {
							//OSI3-Routing beim Empfang spiegeln (optional, bei ANT-KRI ist das Spiegeln implizit enthalten)
							//Mit diesem Schalter muss die Möglichkeit bestehen, die Eigenschaft des ANT-KRI nachzubilden.
							boolean shouldReflectOsi3Routing = false;
							if(link.getProperty(TlsNetworkLayerSetting.reflectOsi3Routing) != null) {
								try {
									shouldReflectOsi3Routing = TlsNetworkLayerSetting.getBooleanProperty(link.getProperty(TlsNetworkLayerSetting.reflectOsi3Routing));
								}
								catch(IllegalArgumentException ex) {
									_debug.warning(
											"Der Parameter atg.protokollEinstellungenPrimary, " + TlsNetworkLayerSetting.reflectOsi3Routing
											+ " hat einen ungültigen Wert.\n" , ex
									);
								}
							}

							if(inputLinkInfo._mirroredReceive || shouldReflectOsi3Routing) {
								// zurück spiegeln der vom ANT-KRI bereits gespiegelten OSI3
								for(int i = osi3HeaderData.length - 1; i > 0; i--) {
									osi3HeaderData[i] = data[osi3HeaderData.length - i];
								}
							}
							else {
								throw new Exception("OSI 3 Pointer ist 0");
							}
						}
						// Ausgehend vom lokalen Gerät wird über die Vermittlungsabschnitte in der
						// OSI-3-Adresse ausgehend vom letzten iteriert. Zum Schluss ergibt sich dadurch die Knotennummer
						// des Absenders.
						senderDevice = _localDeviceAddress;
						for(int i = osi3Length; i > 0; --i) {
							final int portAddress1 = osi3HeaderData[i * 2 - 1] & 0xff;
							final int portAddress2 = osi3HeaderData[i * 2] & 0xff;
							LinkInfo linkInfo;
							do {
								linkInfo = findLinkInfo(senderDevice, portAddress2, portAddress1, true, lastDevice);
								if(linkInfo == null) {
									throw new Exception(
											"Unbekannte Adresse im " + i + ". OSI-3 Vermittlungsabschnitt" + " (von Gerät " + senderDevice + " über Port "
											+ portAddress2 + " nach Adresse " + portAddress1 + ", i=" + i + ")"
									);
								}
								lastDevice = senderDevice;
								senderDevice = linkInfo._remoteDevice;
							}
							while(linkInfo._linkIsTransparent);
						}
					}
					_debug.fine(
							"Daten empfangen, Absender: " + senderDevice, new Object() {
						public String toString() {
							// Die toString-Methode wird nur aufgerufen, wenn das Debug-Level hoch genug eingestellt ist.
							// Die aufwändigen HexDumps werden also nur dann durchgeführt, wenn sie auch benötigt werden.
							return "\nOSI3:\n" + HexDumper.toString(data, 0, dataOffset) + "\nOSI7:\n" + HexDumper.toString(data, dataOffset, -1);
						}
					}
					);
					// Wird ein Langtelegramm verschickt? Langtelegramme werden an einer "0" an Index 3 erkannt (an dieser Stelle
					// stehen bei anderen Telegrammarten die Anzahl Einzeltelegramme)
					if(nextLayerData[3] == 0) {
						// Langtelegramm, das zusammengebaut werden muss
						notifyLongReceive(senderDevice, nextLayerData);
					}
					else {
						// Telegramm kann einfach durchgereicht werden
						notifyReceive(senderDevice, nextLayerData);
					}
				}
			}
			catch(Exception e) {
				_debug.error("Fehler bei der OSI3-Analyse des empfangenen Telegramms: " + event + ", Fehler:" + e);
			}
		}
		else if(event.getType() == DataLinkLayerEvent.Type.CONNECTED) {
			LinkInfo inputLinkInfo = findLinkInfo(_localDeviceAddress, event.getDataLinkLayer().getLocalAddress(), event.getLink().getRemoteAddress(), false);
			notifyStateChange(inputLinkInfo._remoteDevice, NetworkLayerListener.DEVICE_CONNECTED);
		}
		else if(event.getType() == DataLinkLayerEvent.Type.DISCONNECTED) {
			LinkInfo inputLinkInfo = findLinkInfo(_localDeviceAddress, event.getDataLinkLayer().getLocalAddress(), event.getLink().getRemoteAddress(), false);
			notifyStateChange(inputLinkInfo._remoteDevice, NetworkLayerListener.DEVICE_DISCONNECTED);
		}
		else {
			_debug.fine("DataLinkLayer Ereignis: " + event);
		}
	}

	/**
	 * Setzt die Knotennummer des lokalen Geräts.
	 *
	 * @param localDeviceAddress Knotennummer des lokalen Geräts.
	 */
	void setLocalDeviceAddress(int localDeviceAddress) {
		_debug.config("lokale Addresse KNr-" + localDeviceAddress);
		_localDeviceAddress = localDeviceAddress;
	}

	/**
	 * Map, über die sich die von einem Gerät ausgehenden Verbindungen bestimmen lassen. Als Key der Map wird ein Integer-Objekt mit der Knotennummer des Geräts
	 * benutzt. Als Wert ist je Knoten ein List-Objekt mit LinkInfo-Objekten der Verbindungen enthalten.
	 */
	private Map _device2LinksMap = new HashMap();

	private TelegramProcessor _telegramProcessor;

	/**
	 * Sucht eine Verbindung  ausgehend von einem vorgegebenen Gerät über ein bestimmtes Adressenpaar.
	 *
	 * @param device                  Gerät von dem aus gesucht werden soll.
	 * @param portAddress             Port-Adresse am Gerät von dem die gesuchte Verbindung ausgeht.
	 * @param remotePortAdress        Port-Adresse des anderen Geräts an dem die gesuchte Verbindung ankommt.
	 * @param resolveTransparentLinks Flag, das gesetzt sein muss, wenn bei Verbindungen, die auf OSI-3 Ebene transparent sind, (also Verbindungen zu KRI) rekursiv
	 *                                weitergesucht werden soll.
	 *
	 * @return Gewünschtes LinkInfo Objekt oder null, wenn keine entsprechende Verbindung gefunden wurde.
	 */
	private LinkInfo findLinkInfo(int device, int portAddress, int remotePortAdress, boolean resolveTransparentLinks) {
		return findLinkInfo(device, portAddress, remotePortAdress, resolveTransparentLinks, -1);
	}

	/**
	 * Sucht eine Verbindung  ausgehend von einem vorgegebenen Gerät über ein bestimmtes Adressenpaar.
	 *
	 * @param device                  Gerät von dem aus gesucht werden soll.
	 * @param portAddress             Port-Adresse am Gerät von dem die gesuchte Verbindung ausgeht.
	 * @param remotePortAdress        Port-Adresse des anderen Geräts an dem die gesuchte Verbindung ankommt.
	 * @param resolveTransparentLinks Flag, das gesetzt sein muss, wenn bei Verbindungen, die auf OSI-3 Ebene transparent sind, (also Verbindungen zu KRI) rekursiv
	 *                                weitergesucht werden soll.
	 * @param ignoreDevice            Verbindungen zum angegebenen Gerät sollen bei der Suche ignoriert werden, um zyklische rekursive Aufrufe zu unterbinden.
	 *
	 * @return Gewünschtes LinkInfo Objekt oder null, wenn keine entsprechende Verbindung gefunden wurde.
	 */
	private LinkInfo findLinkInfo(int device, int portAddress, int remotePortAdress, boolean resolveTransparentLinks, int ignoreDevice) {
		_debug.fine("findLinkInfo( " + device + ", " + portAddress + ", " + remotePortAdress + ", " + resolveTransparentLinks + " )");
		List deviceLinks = (List)_device2LinksMap.get(new Integer(device));
		if(deviceLinks == null) return null;
		for(Iterator linkIterator = deviceLinks.iterator(); linkIterator.hasNext(); ) {
			LinkInfo linkInfo = (LinkInfo)linkIterator.next();
			if(resolveTransparentLinks && linkInfo._linkIsTransparent) {
				// Es wird nicht das Ergebnis des rekursiven Aufrufs zurückgegeben, weil dann bei transparenten
				// Verbindungen die letzte (und nicht transparente Verbindung) als Ergebnis zurückgegeben würde.
				// Da die Funktion immer die erste Verbindung, die auf dem Weg zum Ziel liegt, zurückgeben soll
				// (auch wenn sie transparent ist) wird der rekursive Aufruf nur benutzt, um abzufragen, ob es
				// das gesuchte Adressenpaar hinter dem transparenten Link gibt.
				if(ignoreDevice != linkInfo._remoteDevice && findLinkInfo(linkInfo._remoteDevice, portAddress, remotePortAdress, true, device) != null) {
					return linkInfo;
				}
			}
			else if(linkInfo._portAddress == portAddress && linkInfo._remotePortAddress == remotePortAdress) {
				return linkInfo;
			}
		}
		return null;
	}

	void addLink(
			int device,
			int portAddress,
			DataLinkLayer linkLayer,
			DataLinkLayer.Link link,
			int remotePortAddress,
			int remoteDevice,
			boolean linkIsTransparent,
			boolean mirroredReceive,
			boolean dontIncrementOsi3Pointer) {

		_debug.config(
				"neue " + (linkIsTransparent ? "transparente " : "") + (linkLayer == null ? "entfernte" : "lokale") + " Verbindung KNr-" + device + "/P-"
				+ portAddress + " <-->" + " P-" + remotePortAddress + "/KNr-" + remoteDevice
		);
		addDeviceLink(device, portAddress, linkLayer, link, remotePortAddress, remoteDevice, linkIsTransparent, mirroredReceive, dontIncrementOsi3Pointer);
		addDeviceLink(remoteDevice, remotePortAddress, linkLayer, link, portAddress, device, linkIsTransparent, false, false);
	}

	private void addDeviceLink(
			int device,
			int portAddress,
			DataLinkLayer linkLayer,
			DataLinkLayer.Link link,
			int remotePortAddress,
			int remoteDevice,
			boolean linkIsTransparent,
			boolean mirroredReceive,
			boolean dontIncrementOsi3Pointer) {
		if(device != _localDeviceAddress) {
			linkLayer = null;
			link = null;
		}
		Integer deviceObject = new Integer(device);
		List deviceLinks = (List)_device2LinksMap.get(deviceObject);
		if(deviceLinks == null) {
			deviceLinks = new ArrayList();
			_device2LinksMap.put(deviceObject, deviceLinks);
		}
		deviceLinks.add(
				new LinkInfo(
						device, portAddress, linkLayer, link, remotePortAddress, remoteDevice, linkIsTransparent, mirroredReceive, dontIncrementOsi3Pointer
				)
		);
	}


	/**
	 * Suchalgorithmus zur Ermittlung aller möglichen Routen zu anderen Geräten. Der Algorithmus arbeitet nicht rekursiv nach unten, sondern etagenweise
	 * (breadth-first-search) um bei einem Netz mit Maschen die jeweils kürzeste Route zu jedem erreichbaren anderen Knoten zu ermitteln. Ausgehend von einer
	 * Liste, die nur den lokalen Knoten enthält werden alle Knoten in spiralförmiger Weise iteriert und jeweils das Routing initialisiert und alle noch nicht
	 * bearbeiteten Geräte, die ausgehend vom jeweils bearbeiteten Gerät eine direkte Verbindung haben werden hinten an die Liste angehangen. Als erstes wird ein
	 * spezielles Routing für das lokale Gerät selbst eintragen, damit man auch Telegramme an sich selbst senden kann.
	 */
	public void completeInitialization() {
		Integer localDevice = new Integer(_localDeviceAddress);
		putRouteInfo(localDevice, new RouteInfo());

		LinkedList spiral = new LinkedList();
		spiral.add(localDevice);
		while(spiral.size() > 0) {
			Integer device = (Integer)spiral.removeFirst();
			RouteInfo route = getRouteInfo(device);
			List links = (List)_device2LinksMap.get(device);
			if(links == null) {
				_debug.warning("keine gültige Verbindung am Gerät mit Knotennummer " + device + " gefunden");
				continue;
			}
			// Schleife über alle Verbindungen am jeweils betrachteten Gerät
			for(Iterator linksIterator = links.iterator(); linksIterator.hasNext(); ) {
				LinkInfo link = (LinkInfo)linksIterator.next();
				Integer remoteDevice = new Integer(link._remoteDevice);
				RouteInfo remoteRoute = getRouteInfo(remoteDevice);
				//Feststellen, ob bereits Route zum Gerät auf neuem Link vorhanden ist.
				// Wenn ja, dann wird die neue Route ignoriert, weil sie nicht kürzer als die bestehende Route ist.
				// Wenn nein, dann wird die neue Route eingetragen und das neue Gerät in spiral eingetragen, damit
				// es in einem späteren Durchlauf der while-Schleife auch die Routings der über das neue Gerät
				// erreichbaren Geräte berücksichtigt werden.
				if(remoteRoute == null) {
					// neue Route eintragen: neue Route = alte route + neue Verbindung;
					remoteRoute = new RouteInfo(route, link);
					putRouteInfo(remoteDevice, remoteRoute);
					//... neues Gerät in spiral aufnehmen
					spiral.addLast(remoteDevice);
					// Geräte, die direkt hinter dem aktuellen Gerät liegen, am aktuellen Gerät vermerken
					putRemoteDevice(device, remoteDevice);
				}
			}
		}
	}

	/**
	 * Speichert zu einem Ausgangsgerät ein direkt an diesem angeschlossenes entferntes  Gerät ab, das über das Gerät erreichbar ist, d.h. das Routing zum
	 * entfernten Gerät wird über das Ausgangsgerät führen.
	 *
	 * @param device       Ausgangsgerät
	 * @param remoteDevice Entferntes Gerät
	 */
	private void putRemoteDevice(Integer device, Integer remoteDevice) {
		List directRoutedDeviceList = (List)_device2DirectRoutedDeviceListMap.get(device);
		if(directRoutedDeviceList == null) {
			directRoutedDeviceList = new LinkedList();
			_device2DirectRoutedDeviceListMap.put(device, directRoutedDeviceList);
		}
		directRoutedDeviceList.add(remoteDevice);
	}

	private void putRouteInfo(Integer device, RouteInfo route) {
		_device2RouteMap.put(device, route);
		_debug.fine("Route zum Gerät " + device + ": " + route);
	}

	private RouteInfo getRouteInfo(Integer device) {
		return (RouteInfo)_device2RouteMap.get(device);
	}

	RouteInfo getRouteInfo(int device) {
		return getRouteInfo(new Integer(device));
	}

	static class RouteInfo {

		private final LinkInfo _firstLinkInfo;

		LinkInfo getFirstLinkInfo() {
			return _firstLinkInfo;
		}
		//		public final DataLinkLayer _localLinkLayer;
//		private DataLinkLayer.Link _localLink;

		/**
		 * Enthält den TLS-OSI-3 Header zum Versand von Telegrammen über die Route. Wird auf <code>null</code> gesetzt, wenn die Route mehr als 7
		 * Vermittlungsabschnitte enthält und vom TLS-OSI-3 Routing nicht mehr verarbeitet werden kann.
		 */
		public final byte[] _tlsOsi3Header;

		public RouteInfo() {
//			_localLinkLayer= null;
			_tlsOsi3Header = null;
			_firstLinkInfo = null;
		}

		public RouteInfo(RouteInfo oldRoute, LinkInfo newLink) {
			if(oldRoute._firstLinkInfo == null) {
				_firstLinkInfo = newLink;
			}
			else {
				_firstLinkInfo = oldRoute._firstLinkInfo;
				if(oldRoute._tlsOsi3Header == null) {
					_tlsOsi3Header = null;
					return;
				}
			}
			int arraySize = 1;
			if(oldRoute._tlsOsi3Header != null) arraySize = oldRoute._tlsOsi3Header.length;
			if(!newLink._linkIsTransparent) arraySize += 2;

			if(arraySize > 15) {
				_tlsOsi3Header = null;
				return;
			}
			else {
				_tlsOsi3Header = new byte[arraySize];
			}
			_tlsOsi3Header[0] = 0;
			if(oldRoute._tlsOsi3Header != null) {
				for(int i = 0; i < oldRoute._tlsOsi3Header.length; i++) {
					_tlsOsi3Header[i] = oldRoute._tlsOsi3Header[i];
				}
			}
			if(!newLink._linkIsTransparent) {
				_tlsOsi3Header[arraySize - 2] = (byte)newLink._portAddress;
				_tlsOsi3Header[arraySize - 1] = (byte)newLink._remotePortAddress;
			}
			int osi3Length = (arraySize - 1) / 2;
			if(osi3Length > 0) {
				int osi3Pointer = 1;

				




				if(_firstLinkInfo._dontIncrementOsi3Pointer) {
					osi3Pointer = 0;
				}
				else if(_firstLinkInfo._linkLayer != null && _firstLinkInfo._linkLayer.getProperty(TlsNetworkLayerSetting.pointerIncrement) != null) {
					try {
						if(!TlsNetworkLayerSetting.getBooleanProperty(_firstLinkInfo._linkLayer.getProperty(TlsNetworkLayerSetting.pointerIncrement))) {
							osi3Pointer = 0;
						}
					}
					catch(IllegalArgumentException ex) {
						_debug.warning(
								"Der Parameter atg.protokollEinstellungenStandard, " + TlsNetworkLayerSetting.pointerIncrement
								+ " ist einen falschen Wert zugeordnet.\n" + ex.getMessage()
						);
					}
				}

				_debug.info("Der Pointer wurde zu beginn " + (osi3Pointer == 0 ? "nicht inkrementiert!" : "inkrementiert"));
				_tlsOsi3Header[0] = (byte)(((osi3Length << 3) & 070) | (osi3Pointer & 007));
			}
		}

		public RouteInfo addLink(LinkInfo newLink) {
			return null;
		}

		public String toString() {
			return " mit  TLS OSI 3 Header: " + HexDumper.toString(_tlsOsi3Header) + "\n" + "über Verbindung " + (_firstLinkInfo == null
			                                                                                                      ? "-"
			                                                                                                      : (_firstLinkInfo._link == null
			                                                                                                         ? "--"
			                                                                                                         : _firstLinkInfo._link.toString()));
		}
	}

	/**
	 * Informationen von Verbindungen zwischen Geräten. Zu beachten ist, das die Verbindungen hier Richtungsabhängig verwaltet werden. Eine bidirektionale
	 * Verbindung zwischen zwei Geräten A und B wird hier durch zwei unabhängige Verbindungsobjekte A->B und B->A repräsentiert.
	 */
	static class LinkInfo {

		/** Knotennummer des Geräts von dem die Verbindung aus geht. */
		public final int _device;

		/** OSI-2 Portnummer des Geräts von dem die Verbindung aus geht. */
		public final long _portAddress;

		/**
		 * LinkLayer (OSI-2) Protokoll über das diese Verbindung realisiert wird. Dieses Feld wird nur verwendet, wenn das _device dem lokalen Gerät entspricht, sonst
		 * null.
		 */
		public final DataLinkLayer _linkLayer;

		/**
		 * LinkLayer Verbindungsobjekt über das diese Verbindung realisiert wird. Dieses Feld wird nur verwendet, wenn das _device dem lokalen Gerät entspricht, sonst
		 * null.
		 */
		public final DataLinkLayer.Link _link;

		/** Knotennummer des Geräts zu dem diese Verbindung hin geht. */
		public final long _remotePortAddress;

		/** OSI-2 Portnummer des Geräts zu dem die Verbindung hin geht. */
		public final int _remoteDevice;

		/**
		 * Flag, das gesetzt ist, wenn die Verbindung nicht im OSI-3-Routing berücksichtigt werden soll. Dies ist zum Beispiel für Verbindungen zwischen Unterzentrale
		 * und Kommunikationsrecher-Inselbus (KRI) der Fall.
		 */
		public final boolean _linkIsTransparent;


		/** Flag, das gesetzt ist, wenn empfangene Telegramme ein gespiegeltes Routing-Feld haben. Dies ist zum Beispiel bei ANT-KRI der Fall. */
		public final boolean _mirroredReceive;


		/**
		 * Flag, das gesetzt ist, wenn das OSI-3-Pointer-Feld mit dem Wert 0 statt dem normalen Wert 1 starten soll und vom KRI incrementiert wird. Dies ist zum
		 * Beispiel bei ANT-KRI der Fall.
		 */
		public final boolean _dontIncrementOsi3Pointer;

		public LinkInfo(
				int device,
				int portAddress,
				DataLinkLayer linkLayer,
				DataLinkLayer.Link link,
				int remotePortAddress,
				int remoteDevice,
				boolean linkIsTransparent,
				boolean mirroredReceive,
				boolean dontIncrementOsi3Pointer) {
			_device = device;
			_portAddress = portAddress;
			_linkLayer = linkLayer;
			_link = link;
			_remotePortAddress = remotePortAddress;
			_remoteDevice = remoteDevice;
			_linkIsTransparent = linkIsTransparent;
			_mirroredReceive = mirroredReceive;
			_dontIncrementOsi3Pointer = dontIncrementOsi3Pointer;
		}
	}

	private static class ChannelObject implements PriorizedObject {

		private final int _priorityClass;

		private final DataLinkLayerEvent _event;

		public ChannelObject(final int priorityClass, final DataLinkLayerEvent event) {
			_priorityClass = priorityClass;
			_event = event;
		}

		public DataLinkLayerEvent getEvent() {
			return _event;
		}

		/**
		 * Liefert die Prioritätsklasse des Objektes zurück. Der Wert 1 entspricht dabei der Klasse mit der höchsten Priorität. Größere Werte kennzeichnen
		 * Prioritätsklassen mit niedrigerer Priorität.
		 *
		 * @return Prioritätsklasse als positive Zahl.
		 */
		public int getPriorityClass() {
			return _priorityClass;
		}
	}

	/**
	 * Klasse zur Entkopplung von OSI2 und OSI7 beim Empfang von Daten. Durch den Einsatz eines eigenen Threads kann während der Verarbeitung von empfangenen Daten
	 * durch die OSI-7 Ebene das Polling auf OSI-2 Ebene fortgesetzt werden. Daten der OSI-2 werden von der Methode {@link
	 * #handleDataLinkLayerEvent(de.bsvrz.kex.tls.osi2osi3.osi2.api.DataLinkLayerEvent)} entgegengenommen und in einer begrenzten Queue gespeichert. Die Queue wird
	 * von einem eigenen Thread abgearbeitet.
	 */
	private class AsyncDataLinkLayerEventHandler implements DataLinkLayerListener {

		private final PriorityChannel _eventChannel;

		private final Thread _asyncThread;

		public AsyncDataLinkLayerEventHandler() {
			
			_eventChannel = new PriorityChannel(1, 2000);
			_asyncThread = new Thread(
					new Runnable() {
						/**
						 * When an object implementing interface <code>Runnable</code> is used to create a thread, starting the thread causes the object's <code>run</code> method
						 * to be called in that separately executing thread.
						 * <p/>
						 * The general contract of the method <code>run</code> is that it may take any action whatsoever.
						 *
						 * @see Thread#run()
						 */
						public void run() {
							while(true) {
								try {
									ChannelObject channelObject = (ChannelObject)_eventChannel.take();
									TlsNetworkLayer.this.handleDataLinkLayerEvent(channelObject.getEvent());
								}
								catch(Exception e) {
									e.printStackTrace();
									_debug.error("Fehler in der asynchronen Ereignisverarbeitung: " + e);
								}
							}
						}
					}
			);
			_asyncThread.setDaemon(true);
		}

		public void start() {
			_asyncThread.start();
		}

		/**
		 * Wird von der Sicherungsschicht aufgerufen, wenn ein Kommunikationsereignis aufgetreten ist, das von der Anwendung bzw. der nächst höheren Protokollebene
		 * ausgewertet werden muss.
		 *
		 * @param event Aufgetretenes Kommunikationsereignis.
		 */
		public void handleDataLinkLayerEvent(DataLinkLayerEvent event) {
			try {
				_eventChannel.put(new ChannelObject(0, event));
			}
			catch(InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}

	public void setTelegramProcessor(TelegramProcessor telegramProcessor) {
		_telegramProcessor = telegramProcessor;
	}
}



