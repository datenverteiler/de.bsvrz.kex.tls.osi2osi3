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

package de.bsvrz.kex.tls.osi2osi3.osi2.tc57primary;


import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.kex.tls.osi2osi3.osi2.api.AbstractDataLinkLayer;
import de.bsvrz.kex.tls.osi2osi3.osi2.api.DataLinkLayer;
import de.bsvrz.kex.tls.osi2osi3.osi2.api.DataLinkLayerEvent;
import de.bsvrz.kex.tls.osi2osi3.osi2.api.LinkState;
import de.bsvrz.kex.tls.osi2osi3.properties.PropertyConsultant;
import de.bsvrz.kex.tls.osi2osi3.properties.PropertyQueryInterface;
import de.bsvrz.sys.funclib.concurrent.PriorityChannel;
import de.bsvrz.sys.funclib.concurrent.PriorizedObject;
import de.bsvrz.sys.funclib.debug.Debug;
import de.bsvrz.sys.funclib.hexdump.HexDumper;

import java.io.IOException;
import java.util.*;

/**
 * OSI-2 Modul, das das TC57-Protokoll nach TLS auf Seite der Primary implementiert.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 10187 $
 */
public class Primary extends AbstractTc57 implements PropertyQueryInterface {

	private static final Debug _debug = Debug.getLogger();

	private final Object _protocolLock = new Object();

	private final Thread _pollingThread;

	private boolean _started = false;

	private boolean _stopped = false;

	private boolean _shuttingDown = false;

	private List _links = new LinkedList();

	private ListIterator _linksPollingIterator = _links.listIterator();

	private SerialPortControl _serialPortControl;

	private Properties _newProperties = null;

	private boolean _reloadProperties = false;

	private final static boolean _offlineTest = false;

	public String toString() {
		return "Primary(" + getLocalAddress() + ")";
	}

	public Primary() {
		_pollingThread = new Thread(new RunnablePrimary(), "PrimaryPollingThread");
		_pollingThread.setPriority(Thread.NORM_PRIORITY + 2);
	}

	/**
	 * Nimmmt die Verbindung zum Datenverteiler entgegen. Diese Methode wird vom OSI-3 Modul nach dem Erzeugen des OSI-2 Moduls durch den jeweiligen Konstruktor
	 * aufgerufen. Eine Implementierung eines Protokollmoduls kann sich bei Bedarf die übergebene Datenverteilerverbindung intern merken, um zu späteren
	 * Zeitpunkten auf die Datenverteiler-Applikationsfunktionen zuzugreifen.
	 *
	 * @param connection Verbindung zum Datenverteiler
	 */
	public void setDavConnection(ClientDavInterface connection) {
		// _connection = connection;
		return;
	}

	/**
	 * Bestimmt, ob die Kommunikation dieses Protokolls bereits mit dr Methode {@link #start} aktiviert wurde.
	 *
	 * @return <code>true</code>, wenn die Kommunikation dieses Protokolls bereits aktiviert wurde, sonst <code>false</code>.
	 */
	public boolean isStarted() {
		return _started;
	}

	public void start() {
		synchronized(_protocolLock) {
			if(isStarted()) throw new IllegalStateException("Protokoll kann nicht erneut gestartet werden");
			_started = true;
			int localAddress = getLocalAddress();
			if(localAddress < 200 || localAddress > 254) {
				throw new IllegalStateException("TC57 Primary Adresse muss zwischen 200 und 254 liegen, ist: " + localAddress);
			}
			_pollingThread.setName("PrimaryPollingThread(" + localAddress + ")");
			_pollingThread.start();
		}
	}


	public void shutdown() {
		synchronized(_protocolLock) {
			_shuttingDown = true;
		}
	}

	public void abort() {
		synchronized(_protocolLock) {
			_stopped = true;
		}
	}

	public void setProperties(Properties properties) {
		_debug.fine("Neue Einstellungen: properties = " + properties);
		checkConnection(_links, properties);
		synchronized(_protocolLock) {
			

			_newProperties = properties;
			_reloadProperties = true;
			_protocolLock.notifyAll();
		}
	}

	private class RunnablePrimary implements Runnable {

		private String _name;

		public String toString() {
			return _name;
		}

		public void run() {
			synchronized(_protocolLock) {
				_name = "RunnablePrimary[" + Thread.currentThread() + "]";


				_serialPortControl = new SerialPortControl();
				while(true) {
					if(_reloadProperties) {
						_debug.fine("neue Einstellungen aktivieren");
						_reloadProperties = false;
						_serialPortControl.shutdown();
						Primary.super.setProperties(_newProperties);
						_serialPortControl = new SerialPortControl();
					}
					try {
						if(!_offlineTest) {
							_serialPortControl.start(Primary.this, _name);

							//Überprüft ob eine Verbindung erlaubt ist. Normalerweise sind die Secondary Adressen von 1-199 oder 255 erlaubt.
							//Nun ist es anhand eines Parameter möglich, diese Bereichsgrenze zu umgehen und die Adressen 1-255 zu zulassen.
							if(!allowConnection(_links)) {
								//Die Verbindung ist nicht zulässig und wird deaktiviert.
								//Danach wird so lange gewartet bis neue Properties vorhanden sind und anschließend wird die Prozedur des Verbindungsaufbau wiederholt
								_serialPortControl.shutdown();
								try {
									_protocolLock.wait();
								}
								catch(InterruptedException ignoredException) {
								}
								continue;
							}
						}
						break;
					}
					catch(Exception e) {
						_debug.error(_name + ": Fehler beim Öffnen der seriellen Schnittstelle: " , e);
						e.printStackTrace();
						try {
							_protocolLock.wait();
						}
						catch(InterruptedException ignoredException) {
						}
						continue;
					}
				}
			}


			_debug.info("===============" + toString() + " polling wird gestartet ==============================================");

			Link lastTriedNotConnectedLink = null;
			int pollCount = 0;
			while(true) {
				_debug.finest("===============" + toString() + " running ==============================================");
				synchronized(_protocolLock) {

					if(_stopped) break;

					if(_reloadProperties) {
						_debug.fine("Serielle Schnittstelle wird gestoppt und mit neuen Einstellungen initialisiert");
						// V24 Kommunikation beenden ...
						_serialPortControl.shutdown();


						//Wird solange wiederholt, bis eine Verbindung steht
						while(true) {
							_reloadProperties = false;
							// ... neue Einstellungen aktivieren ...
							// Hier wird explizit die entsprechende Methode der Basisklasse (AbstractDataLinkLayer) benutzt,
							// um die zwischengespeicherten Einstellungen zu aktivieren.
							Primary.super.setProperties(_newProperties);

							// V24 Kommunikation neu initialisieren
							_serialPortControl = new SerialPortControl();
							try {

								if(!allowConnection(_links)) {
									//Es wird auf neue Properties gewartet
									//Mit dem Continue erfolg eine erneute Überprüfung
									_protocolLock.wait();
									continue;
								}
								else {
									if(!_offlineTest) {
										_serialPortControl.start(Primary.this, _name);
									}
									break;
								}
							}
							catch(Exception e) {
								_debug.error(_name + ": Fehler beim Öffnen der seriellen Schnittstelle: " , e);
								//_stopped= true;
								e.printStackTrace();
							}
						}
					}
				}


				try {
					Link link = null;
					synchronized(_links) {
						if(!_linksPollingIterator.hasNext()) {
							// Alle Verbindungen durch iteriert, von vorne anfangen
							_linksPollingIterator = _links.listIterator();
							pollCount = 0;
						}
						while(_linksPollingIterator.hasNext()) {
							link = (Link)_linksPollingIterator.next();
							//Aktionen, die notwendig sind, bevor der synchronisierte Bereich (_links) verlassen wird
							if(link.synchronizedPollAction()) {
								// Wenn die Verbindung aus der Liste der aktiven Verbindungen gelöscht wurde,
								// dann mit der nächsten weitermachen.
								if(link == lastTriedNotConnectedLink) {
									// Wenn die gelöschte Verbindung die gespeicherte tote war, dann wird die nächste
									// tote Verbindung wieder gepollt
									lastTriedNotConnectedLink = null;
								}
								link = null;
								continue;
							}
							if(lastTriedNotConnectedLink != null && link.getState() != LinkState.CONNECTED) {
								// Wenn im aktuellen Durchlauf bereits eine Tote Verbindung berücksichtigt wurde und
								// die aktuelle tot ist, dann wird die Verbindung übersprungen.
								if(link == lastTriedNotConnectedLink) {
									// Es wurden also einen ganzen Durchlauf Tote Stationen ignoriert, dann soll die
									// nächste Tote Station wieder berücksichtigt werden.
									lastTriedNotConnectedLink = null;
								}
								link = null;
								_debug.finest("polling wird ausgelassen");
								continue;
							}
							if(link.getRemoteAddress() == 0xff && link.getState() == LinkState.CONNECTED && link._sendChannel.isEmpty()) {
								// Wenn die betrachtete Verbindung die initialisierte Broadcastverbindung ist und
								// keine Broadcastdaten zu versenden sind, dann ignoriern
								link = null;
								continue;
							}

							break;
						}
					}
					synchronized(_protocolLock) {
						if(link == null) {
							if(_shuttingDown && _links.size() == 0) {
								_stopped = true;
							}
							else {
								// Wenn keine Verbindung gepollt wurde, dann etwas schlafen
								if(pollCount == 0) {
									Thread.sleep(1000);
								}
							}
							continue;
						}
						if(_shuttingDown && (link.getState() == LinkState.CONNECTED || link.getState() == LinkState.CONNECTING)) {
							link.shutdown();
						}
					}
					// Normale Aktionen außerhalb des synchronisierten Bereichs (_links), damit andere Threads nicht länger
					// als unbedingt notwendig blockiert werden.
					++pollCount;
					link.unsynchronizedPollAction();

					
					Thread.yield();


					if(link.getState() != LinkState.CONNECTED) {
						// Es wurde ein nicht verbundener Link berücksichtigt, also werden alle nicht verbundene Links
						// im folgenden Polldurchlauf ignoriert
						lastTriedNotConnectedLink = link;
					}
				}
				catch(InterruptedException e) {
					e.printStackTrace();  //To change body of catch statement use Options | File Templates.
				}
			}

			if(_serialPortControl != null) _serialPortControl.shutdown();
		}
	}

	private SecondaryFrame querySecondary(PrimaryFrame primaryFrame, PropertyConsultant propertyConsultant, Primary.Link link)
			throws InterruptedException, IOException {
		_debug.fine("sending frame: " + primaryFrame);
		byte[] sendBytes = primaryFrame.getFrameBytes();
		notifyEvent(link, DataLinkLayerEvent.Type.BYTES_SENT, sendBytes);
		_debug.fine("sending frame bytes:\n" + HexDumper.toString(sendBytes));
		byte[] receiveBytes;
		if(_offlineTest) {
			receiveBytes = simulateQueryBytes(primaryFrame);
		}
		else {
			int primaryTap = propertyConsultant.getIntProperty("primary.Tap");
			int tc57Tw = propertyConsultant.getIntProperty("tc57.Tw");
			receiveBytes = _serialPortControl.query(tc57Tw, sendBytes, primaryTap);
		}
		_debug.fine("received frame bytes:\n" + HexDumper.toString(receiveBytes));
		SecondaryFrame answer = SecondaryFrame.parseFrame(3, receiveBytes);
		if(answer != null) notifyEvent(link, DataLinkLayerEvent.Type.BYTES_RECEIVED, answer.getFrameBytes());
		//SecondaryFrame answer= simulateQuery(primaryFrame);
		_debug.fine("received frame: " + answer);
		return answer;
	}

	private SecondaryFrame simulateQuery(PrimaryFrame primaryFrame) {
		if(primaryFrame.getFunction() == PrimaryFrame.RQS) return new SecondaryFrame(false, false, SecondaryFrame.S1, 2, null);
		if(primaryFrame.getFunction() == PrimaryFrame.RES0) return new SecondaryFrame(false, false, SecondaryFrame.S1, 2, null);
		return null;
	}

	private int _simulatedDataCount = 0;

	private byte[] simulateQueryBytes(PrimaryFrame primaryFrame) {

		try {
			Thread.sleep(1000);
		}
		catch(InterruptedException ignoredException) {
		}
		final int address = primaryFrame.getAddress();
		if(address != 6 && address != 2 && address != 1) return null;
		if(primaryFrame.getFunction() == PrimaryFrame.RQS) {
			return new SecondaryFrame(false, false, SecondaryFrame.S1, address, null).getFrameBytes();
		}
		if(primaryFrame.getFunction() == PrimaryFrame.RES0) {
			return new SecondaryFrame(false, false, SecondaryFrame.E5, address, null).getFrameBytes();
		}
		if(primaryFrame.getFunction() == PrimaryFrame.RQD3) {
			byte[] data;
			switch(_simulatedDataCount++) {
				case 1:
					System.out.println("-------------------------------------------------------------------------------");
					data = new byte[]{0, 1, 2, 3, 4};
					return new SecondaryFrame(false, false, SecondaryFrame.D, address, data).getFrameBytes();
				case 3:
					System.out.println("-------------------------------------------------------------------------------");
					data = new byte[]{(byte)0x89, 2, (byte)0xC9, 1, 2, 3, 4};
					return new SecondaryFrame(false, false, SecondaryFrame.D, address, data).getFrameBytes();
				case 6:
					System.out.println("-------------------------------------------------------------------------------");
					data = new byte[]{(byte)0xad, 2, (byte)0xC9, 1, (byte)0xC9, 1, (byte)0xCA, (byte)0xC9, 1, (byte)0xC9, 1, 1, 2, 3, 4};
					return new SecondaryFrame(false, false, SecondaryFrame.D, address, data).getFrameBytes();
				case 9:
					System.out.println("-------------------------------------------------------------------------------");
					data = new byte[]{(byte)0x88, (byte)0xC9, (byte)0x02, 1, 2, 3, 4};
					return new SecondaryFrame(false, false, SecondaryFrame.D, address, data).getFrameBytes();
				case 12:
					System.out.println("-------------------------------------------------------------------------------");
					data = new byte[]{(byte)0xa8, 1, (byte)0xC9, 1, (byte)0xC9, (byte)0xCA, 1, (byte)0xC9, 1, (byte)0xC9, 2, 1, 2, 3, 4};
					return new SecondaryFrame(false, false, SecondaryFrame.D, address, data).getFrameBytes();
				case 300:
					System.out.println("-------------------------------------------------------------------------------");
					data = new byte[]{(byte)0x91, 6, (byte)0xca, (byte)0xca, 2, 1, 2, 3};
					return new SecondaryFrame(false, false, SecondaryFrame.D, address, data).getFrameBytes();
				case 50:
					return new byte[]{0x68, 0x0b, 0x0b, 0x68, 0x08, 0x01, (byte)0x80, 0x07, (byte)0xfe, (byte)0x82, 0x00, 0x01, 0x02, 0x01, 0x11, 0x25, 0x16};
				default:
					return new byte[]{(byte)0xe5};
			}
		}
		if(primaryFrame.getFunction() == PrimaryFrame.D) return new byte[]{(byte)0xe5};
		return null;
	}

	public DataLinkLayer.Link createLink(int remoteAddress) {
		Link link = new Link(remoteAddress);
		return link;
	}

	private class Link extends AbstractDataLinkLayer.Link implements DataLinkLayer.Link, PropertyQueryInterface {

		private final PriorityChannel _sendChannel;

		private Properties _properties = null;

		public PollState _pollState;

		private boolean _fcb;

		private boolean _acd;

		private boolean _dfc;

		private int _retryCount;

		private long _lastGoodReply;

		private PrimaryFrame _queryFrame;

		private final PropertyConsultant _propertyConsultant;

		public String toString() {
			return "TC57-Verbindung von " + getDataLinkLayer() + " nach Secondary(" + getRemoteAddress() + "), Zustand: " + getState();
		}

		private Link(int remoteAddress) {
			super(remoteAddress);
			_propertyConsultant = new PropertyConsultant(this);
			
			_sendChannel = new PriorityChannel(3, 500);
			synchronized(_linkLock) {
				_linkState = LinkState.DISCONNECTED;
				_pollState = PollState.DISCONNECTED;
			}
		}

		public DataLinkLayer getDataLinkLayer() {
			return Primary.this;
		}

		public String getProperty(String name) {
			synchronized(_linkPropertyLock) {
				String value = (_properties == null) ? null : _properties.getProperty(name);
				return (value == null) ? Primary.this.getProperty(name) : value;
			}
		}

		public void setProperties(Properties properties) {
			synchronized(_linkLock) {
				synchronized(_linkPropertyLock) {
					_properties = properties;
				}
			}
		}


		public void connect() {
			synchronized(_links) {
				synchronized(_linkLock) {
					if(_linkState == LinkState.CONNECTED || _linkState == LinkState.CONNECTING) return;
					if(_linkState != LinkState.DISCONNECTED) {
						throw new IllegalStateException("Verbindung kann in diesem Zustand nicht aufgebaut werden: " + _linkState);
					}
					for(Iterator iterator = _links.iterator(); iterator.hasNext(); ) {
						Link link = (Link)iterator.next();
						if(link.getRemoteAddress() == _remoteAddress) {
							throw new IllegalStateException("Es gibt bereits ein Verbindung mit dieser Secondary-Adresse: " + _remoteAddress);
						}
					}
					_linksPollingIterator.add(this);
					_linkState = LinkState.CONNECTING;
				}
			}
		}

		public void shutdown() throws InterruptedException {
			synchronized(_linkLock) {
				if(_linkState == LinkState.DISCONNECTED || _linkState == LinkState.DISCONNECTING) return;
				_linkState = LinkState.DISCONNECTING;
			}
			_sendChannel.put(new PriorizedByteArray(null, 2));
		}

		public void abort() throws InterruptedException {
			synchronized(_linkLock) {
				if(_linkState == LinkState.DISCONNECTED || _linkState == LinkState.DISCONNECTING) return;
				_linkState = LinkState.DISCONNECTING;
			}
			_sendChannel.put(new PriorizedByteArray(null, 0));
		}

		public void send(byte[] bytes, int priority) throws InterruptedException {
			_debug.fine("Telegramm wird zum Versand gepuffert, Priorität: " + priority + ", Daten: " + HexDumper.toString(bytes));
			synchronized(_linkLock) {
				if(_linkState != LinkState.CONNECTED) {
					throw new IllegalStateException("Telegramm kann in diesem Zustand nicht versendet werden: " + _linkState);
				}
			}
			_sendChannel.put(new PriorizedByteArray(bytes, priority));
		}

		/**
		 * Führt Pollaktionen durch, die notwendig sind, bevor der synchronisierte Bereich zum Zugriff auf die Liste mit den zu bearbeitenden Verbindungen (_links)
		 * verlassen wird.
		 *
		 * @return <code>true</code>, wenn eine Aktion durchgeführt wurde und das Polling mit der nächsten Verbindung fortgesetzt werden soll, sonst
		 *         <code>false</code>.
		 */
		private boolean synchronizedPollAction() {
			_debug.fine("----------------------------poll link " + this + ", pollState: " + _pollState + "--------------------------");
			synchronized(_linkLock) {
				if(_pollState == PollState.TO_BE_REMOVED) {
					_linksPollingIterator.remove();
					_pollState = PollState.DISCONNECTED;
					_linkState = LinkState.DISCONNECTED;
					notifyEvent(DataLinkLayerEvent.Type.DISCONNECTED, null);
					return true;
				}
			}
			return false;
		}

		private void unsynchronizedPollAction() throws InterruptedException {
			synchronized(_linkLock) {
				try {
					if(_linkState == LinkState.CONNECTING) {
						if(getRemoteAddress() == 0xff) {
							// Broadcastport
							_pollState = PollState.CONNECTED;
							_linkState = LinkState.CONNECTED;
							_debug.info("Verbindung aufgebaut: " + this);
							notifyEvent(DataLinkLayerEvent.Type.CONNECTED, null);
						}
						else {
							if(_pollState == PollState.INIT_RQS) {
								_debug.fine("RQS Telegramm");
								SecondaryFrame answer = querySecondary(
										new PrimaryFrame(false, false, PrimaryFrame.RQS, _remoteAddress, null), _propertyConsultant, this
								);
								if(answer == null) {
									_debug.fine("keine Antwort");
								}
								else if(answer.getFunction() != SecondaryFrame.S1) {
									_debug.fine("falsche Funktion in Antwort");
								}
								else if(answer.getAddress() != _remoteAddress) {
									_debug.fine("falsche Adresse in Antwort");
								}
								else if(answer.getData() != null) {
									_debug.fine("Antwort enthält fälschlicherweise Daten");
								}
								else {
									_pollState = PollState.INIT_RES0;
								}
							}
							else if(_pollState == PollState.INIT_RES0) {
								_debug.fine("RES0 Telegramm");
								SecondaryFrame answer = querySecondary(
										new PrimaryFrame(false, false, PrimaryFrame.RES0, _remoteAddress, null), _propertyConsultant, this
								);
								if(answer == null) {
									_debug.fine("keine Antwort");
									_pollState = PollState.INIT_RQS;
								}
								else if(answer.getFunction() != SecondaryFrame.ANR1 && answer.getFunction() != SecondaryFrame.E5) {
									_debug.fine("falsche Funktion in Antwort");
									_pollState = PollState.INIT_RQS;
								}
								else if(answer.getFunction() != SecondaryFrame.E5 && answer.getAddress() != _remoteAddress) {
									_debug.fine("falsche Adresse in Antwort");
									_pollState = PollState.INIT_RQS;
								}
								else if(answer.getData() != null) {
									_debug.fine("Antwort enthält fälschlicherweise Daten");
									_pollState = PollState.INIT_RQS;
								}
								else {
									_fcb = true;
									_acd = answer.hasAccessDemand();
									_dfc = answer.hasDataFlowControl();
									_queryFrame = null;
									_retryCount = 0;
									_lastGoodReply = System.currentTimeMillis();
									_pollState = PollState.CONNECTED;
									_linkState = LinkState.CONNECTED;
									_debug.info("Verbindung aufgebaut: " + this);
									notifyEvent(DataLinkLayerEvent.Type.CONNECTED, null);
								}
							}
							else {
								_pollState = PollState.INIT_RQS;
								_sendChannel.clear();
							}
						}
					}
					else if(_linkState == LinkState.CONNECTED || _linkState == LinkState.DISCONNECTING) {
						if(_pollState == PollState.TO_BE_REMOVED) {
							_debug.warning(this + ": TO_BE_REMOVED Zustand sollte von synchronizedPollAction() bearbeitet worden sein");
						}
						else if(_linkState == LinkState.DISCONNECTING && _pollState != PollState.CONNECTED) {
							_pollState = PollState.TO_BE_REMOVED;
						}
						else if(_pollState == PollState.CONNECTED) {
							// Wenn Daten gesendet werden können, dann werden sie auch gesendet, ansonsten wird abgefragt
							// Wenn ACD gesetzt, wird direkt nach dem Versenden bzw. nach Abfrage (nachmal) abgefragt.
							if(_queryFrame == null) {
								// Wenn nicht noch ein altes Telegramm wiederholt werden muß, dann prüfen ob Daten zu Versenden sind
								if(!_dfc) {
									// Wenn Daten gesendet werden dürfen
									PriorizedByteArray priorizedByteArray = (PriorizedByteArray)_sendChannel.poll(0);
									if(priorizedByteArray != null) {
										// Wenn Daten zum Versand vorliegen
										byte[] dataBytes = priorizedByteArray.getBytes();
										if(dataBytes == null) {
											// null-Referenz wird von abort und shutdown zur Signalisierung benutzt
											// => Polling wird eingestellt
											_pollState = PollState.TO_BE_REMOVED;
										}
										else {
											// Datentelegramm wird zum Versand vorbereitet.
											if(_remoteAddress == 0xff) {
												// Broadcast-Adresse
												_queryFrame = new PrimaryFrame(false, false, PrimaryFrame.DNR, _remoteAddress, dataBytes);
											}
											else {
												_queryFrame = new PrimaryFrame(_fcb, true, PrimaryFrame.D, _remoteAddress, dataBytes);
												_fcb = !_fcb;
											}
											_debug.fine("Datentelegramm wird versendet");
										}
									}
								}
							}
							else {
								_debug.info("Telegramm wird wiederholt ");
							}
							if(_queryFrame == null && _remoteAddress != 0xff) {
								// Wenn kein altes Telegramm wiederholt werden muss und keine Daten versendet werden sollen, dann
								// Abfragetelegramm zum Versand vorbereitet.
								_queryFrame = new PrimaryFrame(_fcb, true, PrimaryFrame.RQD3, _remoteAddress, null);
								_fcb = !_fcb;
								_debug.fine("Abfragetelegramm wird versendet");
							}
							if(_queryFrame != null) {
								SecondaryFrame answer = querySecondary(_queryFrame, _propertyConsultant, this);
								if(answer == null) {
									_debug.fine("keine Antwort");
								}
								else if((_queryFrame.getFunction() == PrimaryFrame.D && answer.getFunction() != SecondaryFrame.ANR1
								         && answer.getFunction() != SecondaryFrame.E5) | (_queryFrame.getFunction() != PrimaryFrame.D
								                                                          && answer.getFunction() != SecondaryFrame.D
								                                                          && answer.getFunction() != SecondaryFrame.ANR2
								                                                          && answer.getFunction() != SecondaryFrame.E5)) {
									_debug.warning("falsche Funktion in Antwort, Anfrage war: " + _queryFrame + ", Antwort ist: " + answer);
									answer = null;
								}
								else if(answer.getFunction() != SecondaryFrame.E5 && answer.getAddress() != _remoteAddress) {
									_debug.fine("falsche Adresse in Antwort");
									answer = null;
								}
								else if((answer.getFunction() != SecondaryFrame.D) && (answer.getData() != null)) {
									_debug.fine("Antwort enthält fälschlicherweise Daten");
									answer = null;
								}
								else if((answer.getFunction() == SecondaryFrame.D) && (answer.getData() == null)) {
									_debug.fine("Antwort enthält fälschlicherweise keine Daten");
									answer = null;
								}
								if(_remoteAddress == 0xff) {
									// Bei Broadcasttelegrammen wird keine Antwort erwartet
									_debug.finer("Keine Antwort ist ok");
									notifyEvent(DataLinkLayerEvent.Type.DATA_SENT, _queryFrame.getData());
									_queryFrame = null;
								}
								else {
									if(answer == null) {
										// Keine Antwort oder formaler Aufbau nicht OK
										int minRetryCount = _propertyConsultant.getIntProperty("primary.wiederholungsAnzahl");
										int minRetryDuration = _propertyConsultant.getIntProperty("primary.wiederholungsDauer");
										if(_retryCount >= minRetryCount && _lastGoodReply + minRetryDuration < System.currentTimeMillis()) {
											// Weitere Wiederholungen scheinen zwecklos, => Initialisierung mit RQS
											if(_linkState == LinkState.CONNECTED) {
												_linkState = LinkState.CONNECTING;
												_pollState = PollState.DISCONNECTED;
												notifyEvent(DataLinkLayerEvent.Type.DISCONNECTED, null);
											}
											else {
												_pollState = PollState.TO_BE_REMOVED;
											}
											_debug.info("Secondary tot: " + this);
										}
										else {
											++_retryCount;
											_debug.fine(
													"Wiederholungszähler: " + _retryCount + ", Zeit seit letztem korrektem Empfang: " + (
															System.currentTimeMillis() - _lastGoodReply)
											);
										}
									}
									else {
										_acd = answer.hasAccessDemand();
										_dfc = answer.hasDataFlowControl();
										_retryCount = 0;
										_lastGoodReply = System.currentTimeMillis();
										_debug.finer("Antwort ist ok");
										if(answer.getFunction() == SecondaryFrame.D) {
											notifyEvent(DataLinkLayerEvent.Type.DATA_RECEIVED, answer.getData());
										}
										if(_queryFrame.getFunction() == PrimaryFrame.D) {
											notifyEvent(DataLinkLayerEvent.Type.DATA_SENT, _queryFrame.getData());
										}
										_queryFrame = null;
									}
								}
							}
						}
					}
					else if(_linkState == LinkState.DISCONNECTED) {
						_debug.warning(this + ": Verbindungen im DISCONNECTED Zustand sollten nicht in der Poll-Liste enthalten sein ");
					}
					else {
						_debug.error(this + ": Ungültiger Verbindungszustand von Verbindung: " + _linkState);
					}
					_debug.finest("-----------------------ende poll link " + this + ", pollState: " + _pollState + "--------------------------");
				}
				catch(Exception e) {
					if(getRemoteAddress() == 0xff) {
						if(_queryFrame != null) {
							notifyEvent(DataLinkLayerEvent.Type.DATA_SENT, _queryFrame.getData());
							_queryFrame = null;
						}
					}
					else {
						if(_linkState == LinkState.CONNECTED) {
							_debug.warning(e.getLocalizedMessage());
							if(!(e instanceof IllegalStateException)) e.printStackTrace();
							_linkState = LinkState.CONNECTING;
							_pollState = PollState.DISCONNECTED;
							notifyEvent(DataLinkLayerEvent.Type.DISCONNECTED, null);
						}
						else if(_linkState == LinkState.CONNECTING) {
							_debug.warning(e.getLocalizedMessage());
							_pollState = PollState.INIT_RQS;
						}
						else {
							_pollState = PollState.TO_BE_REMOVED;
						}
					}
					Thread.sleep(1000);
				}
			}
		}
	}

	private static class PriorizedByteArray implements PriorizedObject {

		private final byte[] _bytes;

		private final int _priority;

		public PriorizedByteArray(byte[] bytes, int priority) {
			_bytes = bytes;
			_priority = priority;
		}

		public int getPriorityClass() {
			return _priority;
		}

		public byte[] getBytes() {
			return _bytes;
		}
	}

	/** Definiert die möglichen Unterzustände einer Verbindung. */
	private static class PollState {

		public static final PollState INIT_RQS = new PollState("RQS");

		public static final PollState INIT_RES0 = new PollState("RES0");

		public static final PollState CONNECTED = new PollState("verbunden");

		public static final PollState TO_BE_REMOVED = new PollState("wird aus Poll-Liste entfernt");

		public static final PollState DISCONNECTED = new PollState("unterbrochen");

		/** Name des Verbindungszustands */
		private final String _name;

		/**
		 * Liefert eine textuelle Beschreibung dieses Objekts zurück. Das genaue Format ist nicht festgelegt und kann sich ändern.
		 *
		 * @return Beschreibung dieses Ereignistyps.
		 */
		public String toString() {
			return _name;
		}

		/**
		 * Nicht öffentlicher Konstruktor der zum Erzeugen der vordefinierten Zustände benutzt wird.
		 *
		 * @param name Name des Zustandes.
		 */
		private PollState(String name) {
			_name = name;
		}
	}
}
