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

package de.bsvrz.kex.tls.osi2osi3.osi2.tc57listen;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.kex.tls.osi2osi3.osi2.api.AbstractDataLinkLayer;
import de.bsvrz.kex.tls.osi2osi3.osi2.api.DataLinkLayer;
import de.bsvrz.kex.tls.osi2osi3.osi2.api.DataLinkLayerEvent;
import de.bsvrz.kex.tls.osi2osi3.osi2.api.LinkState;
import de.bsvrz.kex.tls.osi2osi3.osi2.tc57primary.AbstractTc57;
import de.bsvrz.kex.tls.osi2osi3.osi2.tc57primary.PrimaryFrame;
import de.bsvrz.kex.tls.osi2osi3.osi2.tc57primary.SecondaryFrame;
import de.bsvrz.kex.tls.osi2osi3.osi2.tc57primary.SerialPortControl;
import de.bsvrz.kex.tls.osi2osi3.osi2.tc57primary.Tc57Frame;
import de.bsvrz.kex.tls.osi2osi3.properties.PropertyConsultant;
import de.bsvrz.kex.tls.osi2osi3.properties.PropertyQueryInterface;
import de.bsvrz.sys.funclib.debug.Debug;
import de.bsvrz.sys.funclib.hexdump.HexDumper;

import java.io.IOException;
import java.util.*;

/**
 * Dieses OSI-2 Modul kann anstelle eines TC57-Primary-Protokolls verwendet werden, um einen von anderer Stelle aktiv gepollten Inselbus passiv abzuhören und
 * die Daten von den Streckenstationen entgegenzunehmen. Der physische Anschluss kann z.B. über ein spezielles Mithörmodul realisiert werden, das die in beiden
 * Richtungen übertragenen Daten ausgibt.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 10187 $
 */
public class Wiretapper extends AbstractTc57 implements PropertyQueryInterface {

	private static final Debug _debug = Debug.getLogger();

	private final Object _protocolLock = new Object();

	private final Thread _wiretappingThread;

	private boolean _started = false;

	private boolean _stopped = false;

	private boolean _shuttingDown = false;

	private List _links = new LinkedList();

	private ListIterator _linksPollingIterator = _links.listIterator();

	private SerialPortControl _serialPortControl;

	private Properties _newProperties = null;

	private boolean _reloadProperties = false;

	protected String getDefaultProperty(String name) {
		if(name.equals("seriell.bps")) return "1200";
		if(name.equals("seriell.rts")) return "nie";
		if(name.equals("seriell.cts")) return "nein";
		if(name.equals("seriell.dsr")) return "nein";
		if(name.equals("seriell.überlaufPrüfen")) return "ja";
		if(name.equals("seriell.empfangsPuffer")) return "10000";
		if(name.equals("wiretapper.antwortTimeout")) return "5000";
		if(name.equals("wiretapper.anfrageTimeout")) return "120000";
		if(name.equals("seriell.dsr")) return "nein";
		if(name.equals("seriell.paritätPrüfen")) return "nein";
		return super.getDefaultProperty(name);
	}

	public String toString() {
		return "MithörModul(" + getLocalAddress() + ")";
	}

	public Wiretapper() {
		_wiretappingThread = new Thread(new WiretappingWorker(), "WireTapperPollingThread");
		_wiretappingThread.setPriority(Thread.NORM_PRIORITY + 2);
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
			_wiretappingThread.setName("WiretappingThread(" + localAddress + ")");
			_wiretappingThread.start();
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
			// Aktivieren der Properties wird verzögert
			_newProperties = properties;
			_reloadProperties = true;
			_protocolLock.notifyAll();
		}
	}

	private class Buffer {

		byte[] _bytes = new byte[1000];

		int _size = 0;

		public void append(byte[] additionalBytes) {
			int newSize = _size + additionalBytes.length;
			if(newSize > _bytes.length) {
				byte[] newBytes = new byte[newSize];
				System.arraycopy(_bytes, 0, newBytes, 0, _size);
				_bytes = newBytes;
			}
			System.arraycopy(additionalBytes, 0, _bytes, _size, additionalBytes.length);
			_size = newSize;
		}

		Tc57Frame fetchFrame() {
			int startIndex = 0;
			while(startIndex < _size) {
				int b = _bytes[startIndex] & 0xff;
				if(b == 0xe5) break;
				if(b == 0x10) {
					if(startIndex + 4 < _size) {
						if(_bytes[startIndex + 4] == 0x16) break;
					}
					else {
						skip(startIndex, "found possible start of incomplete short telegram");
						return null;
					}
				}
				if(b == 0x68) {
					if(startIndex + 3 < _size) {
						if(_bytes[startIndex + 1] == _bytes[startIndex + 2] && _bytes[startIndex + 3] == 0x68) {
							int len = _bytes[startIndex + 1] & 0xff;
							if(len > 2) {
								if((startIndex + 5 + len) < _size) {
									if(_bytes[startIndex + 5 + len] == 0x16) {
										break;
									}
								}
								else {
									skip(startIndex, "found possible header of incomplete long telegram");
									return null;
								}
							}
						}
					}
					else {
						skip(startIndex, "found possible start of incomplete long telegram");
						return null;
					}
				}
				++startIndex;
			}
			Tc57Frame result = null;
			int endIndex = startIndex;
			if(startIndex < _size) {
				int b = _bytes[startIndex] & 0xff;
				if(b == 0xe5) {
					//Quittungszeichen
					endIndex = startIndex + 1;
					result = new SecondaryFrame(false, false, SecondaryFrame.E5, 0, null);
					// Die Quittung kommt vom Secondary, es wird ein Byte-Array der Größe 1 übergeben (Inhalt: E5)
					notifyEvent(null, DataLinkLayerEvent.Type.BYTES_RECEIVED, new byte[]{_bytes[startIndex]});
				}
				else if(b == 0x10) {
					// Kurztelegramm
					int controlByte = _bytes[startIndex + 1] & 0xff;
					int addressByte = _bytes[startIndex + 2] & 0xff;
					int checkSum = _bytes[startIndex + 3] & 0xff;
					endIndex = startIndex + 5;
					if(((controlByte + addressByte) & 0xff) == checkSum) {
						// Damit dem lauschenden Prozess ein OSI 2 Telegramm geschickt werden kann, wird es an dieser Stelle
						// erzeugt.
						final byte[] osi2Telegram = new byte[5];
						// Ein Kurztelegramm ist 5 Bytes lang
						System.arraycopy(_bytes, startIndex, osi2Telegram, 0, osi2Telegram.length);

						if((controlByte & 0x40) != 0) {
							result = new PrimaryFrame((controlByte & 0x20) != 0, (controlByte & 0x10) != 0, controlByte & 0x0f, addressByte, null);
							notifyEvent(null, DataLinkLayerEvent.Type.BYTES_SENT, osi2Telegram);
						}
						else {
							result = new SecondaryFrame((controlByte & 0x20) != 0, (controlByte & 0x10) != 0, controlByte & 0x0f, addressByte, null);
							notifyEvent(null, DataLinkLayerEvent.Type.BYTES_RECEIVED, osi2Telegram);
						}
					}
				}
				else if(b == 0x68) {
					// Langtelegramm
					int length1 = _bytes[startIndex + 1] & 0xff;
					int controlByte = _bytes[startIndex + 4] & 0xff;
					int addressByte = _bytes[startIndex + 5] & 0xff;
					int checkSum = _bytes[startIndex + 4 + length1] & 0xff;
					int calculatedCheckSum = 0;
					for(int i = 0; i < length1; ++i) {
						calculatedCheckSum += (_bytes[startIndex + 4 + i] & 0xff);
					}
					calculatedCheckSum &= 0xff;
					endIndex = startIndex + 6 + length1;
					if(calculatedCheckSum == checkSum) {
						byte[] data = new byte[length1 - 2];
						System.arraycopy(_bytes, startIndex + 6, data, 0, length1 - 2);

						// Der lauschende Prozess benötigt ein OSI 2 Telegramm mit allen Informationen, dieses
						// wird an dieser Stelle erzeugt.
						// Die Länge besteht aus den ersten 4 Anfangsbytes, Länge des Datenfelds + Steuerbyte,Adressbyte und der Prüfsumme und Ende(0x16)
						final byte[] osi2Telegram = new byte[4 + length1 + 2];
						System.arraycopy(_bytes, startIndex, osi2Telegram, 0, osi2Telegram.length);

						if((controlByte & 0x40) != 0) {
							result = new PrimaryFrame((controlByte & 0x20) != 0, (controlByte & 0x10) != 0, controlByte & 0x0f, addressByte, data);
							notifyEvent(null, DataLinkLayerEvent.Type.BYTES_SENT, osi2Telegram);
						}
						else {
							result = new SecondaryFrame((controlByte & 0x20) != 0, (controlByte & 0x10) != 0, controlByte & 0x0f, addressByte, data);
							notifyEvent(null, DataLinkLayerEvent.Type.BYTES_RECEIVED, osi2Telegram);
						}
					}
				}
				skip(endIndex, "telegram recognized");
			}
			else {
				skip(endIndex, "garbage");
			}
			return result;
		}

		private void skip(int count, String reason) {
			if(count == 0) return;
			final int newSize = _size - count;
			_debug.fine(reason + ", " + newSize + " bytes remaining , skipping processed bytes: \n" + HexDumper.toString(_bytes, 0, count));
			if(newSize <= 0) {
				_size = 0;
			}
			else {
				System.arraycopy(_bytes, count, _bytes, 0, newSize);
				_size = newSize;
			}
		}

		public void skipAll() {
			_debug.warning("skipping all received bytes:\n" + HexDumper.toString(_bytes, 0, _size));
			_size = 0;
		}
	}

	private class WiretappingWorker implements Runnable {

		private String _name;

		private PrimaryFrame _primaryFrame = null;

		private long _primaryFrameTimeout;

		private PropertyConsultant _propertyConsultant = new PropertyConsultant(Wiretapper.this);

		public String toString() {
			return _name;
		}

		public void run() {
			synchronized(_protocolLock) {
				_name = "WiretappingWorker[" + Thread.currentThread() + "]";

				_serialPortControl = new SerialPortControl();
				while(true) {
					if(_reloadProperties) {
						_debug.fine("neue Einstellungen aktivieren");
						_reloadProperties = false;
						_serialPortControl.shutdown();
						Wiretapper.super.setProperties(_newProperties);
						_serialPortControl = new SerialPortControl();
					}
					try {
						_serialPortControl.start(Wiretapper.this, _name);

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

						break;
					}
					catch(Exception e) {
						_debug.error(_name + ": Fehler beim Öffnen der seriellen Schnittstelle: " , e);
						e.printStackTrace();
					}
				}
			}

			_debug.info("===============" + toString() + " wiretapping wird gestartet ==============================================");

			long queryTimeout;
			queryTimeout = _propertyConsultant.getIntProperty("wiretapper.antwortTimeout");
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
							Wiretapper.super.setProperties(_newProperties);

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
									_serialPortControl.start(Wiretapper.this, _name);
									break;
								}
							}
							catch(Exception e) {
								_debug.error(_name + ": Fehler beim Öffnen der seriellen Schnittstelle: " , e);
								//_stopped= true;
								e.printStackTrace();
							}
						}
						queryTimeout = _propertyConsultant.getIntProperty("wiretapper.antwortTimeout");
					}
				}


				try {
					Link link = null;
					synchronized(_links) {
						if(!_linksPollingIterator.hasNext()) {
							// Alle Verbindungen durch iteriert, von vorne anfangen
							_linksPollingIterator = _links.listIterator();
						}
						while(_linksPollingIterator.hasNext()) {
							link = (Link)_linksPollingIterator.next();
							//Aktionen, die notwendig sind, bevor der synchronisierte Bereich (_links) verlassen wird
							if(link.synchronizedPollAction()) {
								// Wenn die Verbindung aus der Liste der aktiven Verbindungen gelöscht wurde,
								// dann mit der nächsten weitermachen.
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
							continue;
						}
						if(_shuttingDown && (link.getState() == LinkState.CONNECTED || link.getState() == LinkState.CONNECTING)) {
							link.shutdown();
						}
					}
					// Normale Aktionen außerhalb des synchronisierten Bereichs (_links), damit andere Threads nicht länger
					// als unbedingt notwendig blockiert werden.

					// Verbindungen nach der Zeit der letzten erfolgreichen Übertragung untersuchen und eventuell auf tot setzen
					link.unsynchronizedPollAction();

					try {
						Tc57Frame frame = interceptTelegram();

						// An dieser Stelle steht ein Telegramm zur Verfügung (wenn es ungleich null ist)
						if(frame == null) {
							if(_primaryFrame != null && System.currentTimeMillis() > _primaryFrameTimeout) {
								handleQuery(_primaryFrame, null);
								_primaryFrame = null;
							}
						}
						else {
							if(frame instanceof PrimaryFrame) {
								if(_primaryFrame != null) handleQuery(_primaryFrame, null);
								_primaryFrame = (PrimaryFrame)frame;
								_primaryFrameTimeout = System.currentTimeMillis() + queryTimeout;
							}
							else if(frame instanceof SecondaryFrame) {
								SecondaryFrame secondaryFrame = (SecondaryFrame)frame;
								if(_primaryFrame != null) {
									handleQuery(_primaryFrame, secondaryFrame);
									_primaryFrame = null;
								}
								else {
									_debug.warning("unerwartete Anwort von Secondary ignoriert: " + secondaryFrame);
								}
							}
						}
					}
					catch(IOException e) {
						// Fehler beim Lesen von der seriellen Schnittstelle
						// Alle Verbindungen auf tot setzen
						synchronized(_links) {
							for(Iterator iterator = _links.iterator(); iterator.hasNext(); ) {
								Link someLink = (Link)iterator.next();
								someLink.handleErrorSituation(e.getMessage());
							}
						}
						_primaryFrame = null;
					}

					Thread.yield();
				}
				catch(InterruptedException e) {
					e.printStackTrace();  //To change body of catch statement use Options | File Templates.
				}
			}
			if(_serialPortControl != null) _serialPortControl.shutdown();
		}

		private void handleQuery(PrimaryFrame primaryFrame, SecondaryFrame secondaryFrame) {
			int secondaryAddress = primaryFrame.getAddress();
			Link linkToSecondary = null;
			synchronized(_links) {
				for(Iterator iterator = _links.iterator(); iterator.hasNext(); ) {
					Link testLink = (Link)iterator.next();
					if(testLink.getRemoteAddress() == secondaryAddress) {
						linkToSecondary = testLink;
						break;
					}
				}
			}
			if(linkToSecondary == null) {
				_debug.warning("Kommunikation mit unbekannter Secondary wurde ignoriert. Anfrage: " + primaryFrame + ", Antwort: " + secondaryFrame);
			}
			else {
				linkToSecondary.handleQuery(primaryFrame, secondaryFrame);
			}
		}

		private Buffer _buffer = new Buffer();

		private Tc57Frame interceptTelegram() throws InterruptedException, IOException {

			Tc57Frame frame = _buffer.fetchFrame();
			if(frame == null) {
				byte[] input = _serialPortControl.readBytes(300);
				if(input == null) {
					_buffer.skipAll();
					throw new IOException("Lesefehler");
				}
				_buffer.append(input);
				frame = _buffer.fetchFrame();
			}
			return frame;
		}
	}

	public DataLinkLayer.Link createLink(int remoteAddress) {
		Link link = new Link(remoteAddress);
		return link;
	}

	private class Link extends AbstractDataLinkLayer.Link implements DataLinkLayer.Link, PropertyQueryInterface {

		private Properties _properties = null;

		private boolean _fcb;

		private long _lastGoodReply;

		private final PropertyConsultant _propertyConsultant;

		private boolean _tolerateRetry = false;

		public String toString() {
			return getDataLinkLayer().toString() + "-(" + getRemoteAddress() + ")-(" + getState() + ")";
		}

		private Link(int remoteAddress) {
			super(remoteAddress);
			_propertyConsultant = new PropertyConsultant(this);
			synchronized(_linkLock) {
				_linkState = LinkState.DISCONNECTED;
			}
		}

		public DataLinkLayer getDataLinkLayer() {
			return Wiretapper.this;
		}

		public String getProperty(String name) {
			synchronized(_linkPropertyLock) {
				String value = (_properties == null) ? null : _properties.getProperty(name);
				return (value == null) ? Wiretapper.this.getProperty(name) : value;
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
						throw new IllegalStateException("Verbindung kann in diesem Zustand nicht aufgebaut werden: " + toString());
					}
					for(Iterator iterator = _links.iterator(); iterator.hasNext(); ) {
						Link link = (Link)iterator.next();
						if(link.getRemoteAddress() == _remoteAddress) {
							throw new IllegalStateException("Es gibt bereits ein Verbindung mit dieser Secondary-Adresse: " + _remoteAddress);
						}
					}
					_linksPollingIterator.add(this);
					_linkState = LinkState.CONNECTING;
					_tolerateRetry = false;
				}
			}
		}

		public void shutdown() throws InterruptedException {
			synchronized(_linkLock) {
				if(_linkState == LinkState.DISCONNECTED || _linkState == LinkState.DISCONNECTING) return;
				_linkState = LinkState.DISCONNECTING;
			}
		}

		public void abort() throws InterruptedException {
			synchronized(_linkLock) {
				if(_linkState == LinkState.DISCONNECTED || _linkState == LinkState.DISCONNECTING) return;
				_linkState = LinkState.DISCONNECTING;
			}
		}

		public void send(byte[] bytes, int priority) throws InterruptedException {
			throw new UnsupportedOperationException("Datenversand wird vom Mithörmodul nicht unterstützt " + toString());
		}

		/**
		 * Führt Pollaktionen durch, die notwendig sind, bevor der synchronisierte Bereich zum Zugriff auf die Liste mit den zu bearbeitenden Verbindungen (_links)
		 * verlassen wird.
		 *
		 * @return <code>true</code>, wenn eine Aktion durchgeführt wurde und das Polling mit der nächsten Verbindung fortgesetzt werden soll, sonst
		 *         <code>false</code>.
		 */
		private boolean synchronizedPollAction() {
			_debug.finer("----------------------------synchronizedPollAction link " + this + "--------------------------");
			synchronized(_linkLock) {
				if(_linkState == LinkState.DISCONNECTING) {
					_linksPollingIterator.remove();
					_linkState = LinkState.DISCONNECTED;
					notifyEvent(DataLinkLayerEvent.Type.DISCONNECTED, null);
					return true;
				}
			}
			return false;
		}

		private void unsynchronizedPollAction() {
			synchronized(_linkLock) {
				if(_linkState == LinkState.CONNECTING) {
					if(getRemoteAddress() == 0xff) {
						// Broadcastport
						_linkState = LinkState.CONNECTED;
						_debug.info("Verbindung aufgebaut: " + this);
						notifyEvent(DataLinkLayerEvent.Type.CONNECTED, null);
					}
				}
				else if(_linkState == LinkState.CONNECTED) {
					int secondaryTimeout = _propertyConsultant.getIntProperty("wiretapper.anfrageTimeout");
					if(System.currentTimeMillis() - _lastGoodReply > secondaryTimeout) {
						_linkState = LinkState.CONNECTING;
						_debug.info(toString() + ": Keine Verbindung mehr wegen Timeout");
						notifyEvent(DataLinkLayerEvent.Type.DISCONNECTED, null);
					}
				}
			}
		}

		public void handleQuery(PrimaryFrame primaryFrame, SecondaryFrame secondaryFrame) {
			_debug.fine(toString() + " handleQuery: Anfrage: " + primaryFrame + ", Antwort: " + secondaryFrame);
			// Wenn keine Antwort empfangen wurde, dann soll im nächsten Zyklus eine Wiederholung akzeptiert werden
			boolean tolerateNextRetry = secondaryFrame == null;
			if(secondaryFrame != null) {
				if(secondaryFrame.getFunction() != SecondaryFrame.E5 && secondaryFrame.getAddress() != primaryFrame.getAddress()) {
					_debug.warning(toString() + ": Anwort von falscher Secondary erhalten, Anfrage: " + primaryFrame + ", Antwort: " + secondaryFrame);
					secondaryFrame = null;
				}
			}
			if(secondaryFrame != null) {
				_lastGoodReply = System.currentTimeMillis();
			}
			synchronized(_linkLock) {
				switch(primaryFrame.getFunction()) {
					case PrimaryFrame.RQS:
						if(_linkState == LinkState.CONNECTED) {
							_linkState = LinkState.CONNECTING;
							_debug.info(toString() + ": Keine Verbindung mehr wegen RQS");
							notifyEvent(DataLinkLayerEvent.Type.DISCONNECTED, null);
						}
						if(secondaryFrame != null) {
							if(secondaryFrame.getFunction() != SecondaryFrame.S1) {
								_debug.warning(this.toString() + ": Falsche Antwort von Secondary, Anfrage: " + primaryFrame + ", Antwort: " + secondaryFrame);
							}
						}
						break;
					case PrimaryFrame.RES0:
						if(_linkState == LinkState.CONNECTED) {
							_linkState = LinkState.CONNECTING;
							_debug.info(toString() + ": Keine Verbindung mehr wegen RES0");
							notifyEvent(DataLinkLayerEvent.Type.DISCONNECTED, null);
						}
						if(secondaryFrame != null) {
							final int secondaryFunction = secondaryFrame.getFunction();
							if(secondaryFunction != SecondaryFrame.ANR1 && secondaryFunction != SecondaryFrame.E5) {
								_debug.warning(toString() + ": Falsche Antwort von Secondary, Anfrage: " + primaryFrame + ", Antwort: " + secondaryFrame);
							}
							else {
								if(_linkState == LinkState.CONNECTING) {
									_linkState = LinkState.CONNECTED;
									_debug.info(toString() + ": Verbindung nach RES0 wieder hergestellt");
									notifyEvent(DataLinkLayerEvent.Type.CONNECTED, null);
								}
							}
						}
						else {
							_debug.warning(toString() + ": Keine Antwort auf Reset, Anfrage: " + primaryFrame + ", Antwort: " + secondaryFrame);
						}
						break;
					case PrimaryFrame.D:
						if(secondaryFrame != null) {
							final int secondaryFunction = secondaryFrame.getFunction();
							if(secondaryFunction != SecondaryFrame.ANR1 && secondaryFunction != SecondaryFrame.E5) {
								_debug.warning(toString() + ": Falsche Antwort von Secondary, Anfrage: " + primaryFrame + ", Antwort: " + secondaryFrame);
							}
							else {
								if(_linkState == LinkState.CONNECTING) {
									_linkState = LinkState.CONNECTED;
									_fcb = primaryFrame.getFrameCountBit();
									_debug.info(toString() + ": Verbindung nach Datenversand wieder hergestellt");
									notifyEvent(DataLinkLayerEvent.Type.CONNECTED, null);
								}
								// Wenn im vorhergehenden Zyklus keine Antwort der Secondary empfangen wurde, dann soll eine Wiederholung akzeptiert werden
								if(_fcb == primaryFrame.getFrameCountBit() || _tolerateRetry) {
									notifyEvent(DataLinkLayerEvent.Type.DATA_SENT, primaryFrame.getData());
								}
								else {
									_debug.warning(
											toString() + ": Telegrammwiederholung wurde ignoriert, Anfrage: " + primaryFrame + ", Antwort: " + secondaryFrame
									);
								}
							}
						}
						else {
							_debug.warning(toString() + ": Keine Antwort auf Datenversand, Anfrage: " + primaryFrame + ", Antwort: " + secondaryFrame);
						}
						break;
					case PrimaryFrame.DNR:
						if(_linkState == LinkState.CONNECTING) {
							_linkState = LinkState.CONNECTED;
							_fcb = primaryFrame.getFrameCountBit();
							_debug.info(toString() + ": Verbindung nach Datenversand wieder hergestellt");
							notifyEvent(DataLinkLayerEvent.Type.CONNECTED, null);
						}
						// Es wurde keine Antwort erwartet, deshalb sollen in diesem Ausnahmefall der Ausnahme doch keine Wiederholungen akzeptiert werden, außer wenn erneut ein DNR empfangen wird.
						tolerateNextRetry = false;
						notifyEvent(DataLinkLayerEvent.Type.DATA_SENT, primaryFrame.getData());
						break;
					case PrimaryFrame.RQD1:
					case PrimaryFrame.RQD2:
					case PrimaryFrame.RQD3:
						if(secondaryFrame != null) {
							final int secondaryFunction = secondaryFrame.getFunction();
							if(secondaryFunction != SecondaryFrame.D && secondaryFunction != SecondaryFrame.ANR2 && secondaryFunction != SecondaryFrame.E5) {
								_debug.warning(toString() + ": Falsche Antwort von Secondary, Anfrage: " + primaryFrame + ", Antwort: " + secondaryFrame);
							}
							else {
								if(_linkState == LinkState.CONNECTING) {
									_linkState = LinkState.CONNECTED;
									_fcb = primaryFrame.getFrameCountBit();
									_debug.info(toString() + ": Verbindung nach Datenabruf wieder hergestellt");
									notifyEvent(DataLinkLayerEvent.Type.CONNECTED, null);
								}
								// Wenn im vorhergehenden Zyklus keine Antwort der Secondary empfangen wurde, dann soll eine Wiederholung akzeptiert werden
								if(_fcb == primaryFrame.getFrameCountBit() || _tolerateRetry) {
									if(secondaryFunction == SecondaryFrame.D) {
										notifyEvent(DataLinkLayerEvent.Type.DATA_RECEIVED, secondaryFrame.getData());
									}
								}
								else {
									_debug.warning(
											toString() + ": Telegrammwiederholung wurde ignoriert, Anfrage: " + primaryFrame + ", Antwort: " + secondaryFrame
									);
								}
							}
						}
						else {
							_debug.warning(toString() + ": Keine Antwort auf Datenabruf, Anfrage: " + primaryFrame + ", Antwort: " + secondaryFrame);
						}
						break;
					case PrimaryFrame.RES1:
					case PrimaryFrame.RQT:
					case PrimaryFrame.TD2:
					default:
						_debug.warning(toString() + "Unerwarteter Datenaustausch, Anfrage: " + primaryFrame + ", Antwort: " + secondaryFrame);
						break;
				}
				if(_linkState == LinkState.CONNECTED) {
					_fcb = !primaryFrame.getFrameCountBit();
				}
				// Wenn keine Antwort der Secondary empfangen wurde, dann im nächsten Zyklus eine Wiederholung akzeptiert werden
				_tolerateRetry = tolerateNextRetry;
			}
		}

		public void handleErrorSituation(String message) {
			synchronized(_linkLock) {
				if(_linkState == LinkState.CONNECTED) {
					_linkState = LinkState.CONNECTING;
					_debug.info(toString() + ": Keine Verbindung mehr wegen RES0");
					notifyEvent(DataLinkLayerEvent.Type.DISCONNECTED, null);
				}
			}
		}
	}
}
