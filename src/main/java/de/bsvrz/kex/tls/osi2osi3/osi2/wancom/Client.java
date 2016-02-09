/*
 * Copyright 2008 by Kappich Systemberatung, Aachen
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2004 by Kappich+Kniß Systemberatung Aachen (K2S)
 * Copyright 2006 by Kappich Systemberatung Aachen
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

package de.bsvrz.kex.tls.osi2osi3.osi2.wancom;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.kex.tls.osi2osi3.osi2.api.AbstractDataLinkLayer;
import de.bsvrz.kex.tls.osi2osi3.osi2.api.DataLinkLayer;
import de.bsvrz.kex.tls.osi2osi3.osi2.api.DataLinkLayerEvent;
import de.bsvrz.kex.tls.osi2osi3.osi2.api.LinkState;
import de.bsvrz.kex.tls.osi2osi3.properties.PropertyConsultant;
import de.bsvrz.kex.tls.osi2osi3.properties.PropertyQueryInterface;
import de.bsvrz.sys.funclib.concurrent.PriorityChannel;
import de.bsvrz.sys.funclib.concurrent.PriorizedObject;
import de.bsvrz.sys.funclib.concurrent.UnboundedQueue;
import de.bsvrz.sys.funclib.debug.Debug;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * Klasse, die als OSI-2 Protokollmodul für den client-seitige Teil einer WanCom-Verbindung eingesetzt werden kann. Zur Verwendung dieses Protokollmoduls ist an
 * dem jeweiligen Anschlußpunkt in der Konfiguration in der Attributgruppe "atg.anschlussPunkt" im Attribut "ProtokollTyp" der Wert
 * "de.bsvrz.kex.tls.osi2osi3.osi2.wancom.Client" einzutragen. Im Parameter "atg.protokollEinstellungenStandard" des Anschlußpunkts werden Defaultswerte für
 * alle Verbindungen an diesem Anschlußpunkt eingestellt. Im Parameter "atg.protokollEinstellungenPrimary" bzw. "atg.protokollEinstellungenSecondary" der dem
 * Anschlußpunkt zugeordneten AnschlußPunktKommunikationsPartner werden individuelle Werte für die Verbindung zum jeweiligen Kommunikationspartner eingestellt.
 * Die Parameterdatensätze können mehrere Einträge enthalten die jeweils aus einem Namen und einem Wert bestehen. Folgende Tabelle enthält die Namen,
 * Defaultwerte und eine Beschreibung der unterstützten Einträge: <table cellpadding="2" cellspacing="2" border="1"> <tr> <th> Name </th> <th> Defaultwert </th>
 * <th> Beschreibung </th> </tr> <tr> <td> wancom.host </td> <td>  </td> <td> Domainname oder IP-Adresse des Kommunikationspartners. </td> </tr> <tr> <td>
 * wancom.port </td> <td> 7100 </td> <td> TCP-Portnummer des WanCom-Servers beim Kommunikationspartner. </td> </tr> <tr> <td> wancom.version </td> <td> 35 </td>
 * <td> Im WanCom-Header übertragene Version des eingesetzten Protokolls. </td> </tr> <tr> <td> wancom.keepAliveTime </td> <td> 20 </td> <td> Zeit in Sekunden
 * zwischen dem Versand von 2 Keep-Alive Telegrammen. </td> </tr> <tr> <td> wancom.keepAliveTimeoutCount </td> <td> 3 </td> <td> Anzahl von in Folge vergangenen
 * keepAliveTime-Intervallen ohne Empfang eines KeepAlive-Telegramms bevor die Verbindung abgebrochen wird. </td> </tr> <tr> <td> wancom.keepAliveType </td>
 * <td> 50 </td> <td> WanCom-Type-Feld in KeepAlive-Telegrammen. </td> </tr> <tr> <td> wancom.tlsType </td> <td> 600 </td> <td> WanCom-Type-Feld in versendeten
 * TLS-Telegrammen. </td> </tr> <tr> <td> wancom.tlsTypeReceive </td> <td> </td> <td> WanCom-Type-Feld in empfangenen TLS-Telegrammen. Dieser Wert muss nur
 * angegeben werden, wenn er sich vom WanCom-Typen zum Versand (wancom.tlsType) unterscheidet. Wenn dieser Wert nicht angegeben wurde, wird der Wert von
 * wancom.tlsType auch zum Empfang verwendet. Wenn der Wert <code>-1</code> angegeben wird, dann werden alle WanCom-Typ-Werte (außer dem Wert für
 * KeepAlive-Telegramme wancom.keepAliveType) akzeptiert. </td> </tr> <tr> <td> wancom.connectRetryDelay </td> <td> 60 </td> <td> Wartezeit in Sekunden, bevor
 * ein fehlgeschlagener Verbindungsversuch wiederholt wird. </td> </tr> <tr> <td> wancom.localAddress </td> <td> </td> <td> Lokale Adresse, die in
 * Wan-Com-Header als Absender eingetragen werden soll. Ein leerer Text, wird automatisch durch die aktuelle lokale Adresse der Wan-Com-Verbindung ersetzt.
 * </td> </tr> </table>
 * <p/>
 *
 * @author Kappich Systemberatung
 * @version $Revision: 10187 $
 */
public class Client extends WanCom implements PropertyQueryInterface {

	private static final Debug _debug;

	static {
		_debug = Debug.getLogger();
		//_debug.setLoggerLevel(Debug.FINEST);
	}

	final Worker _worker = new Worker();

	private final Thread _workThread;

	private final Object _protocolLock = new Object();

	private ProtocolState _protocolState = ProtocolState.CREATED;

	private List<Link> _links = new LinkedList<Link>();


	public String toString() {
		return "WAN-Com-Client(" + getLocalAddress() + ", " + _protocolState + ") ";
	}

	public Client() throws IOException {
		_debug.fine("WanComClient ");
		_workThread = new Thread(_worker, "wancom.Client.Worker");
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

	public static class ActionType {

		public static final ActionType CONNECT_CALLED = new ActionType("CONNECT_CALLED");

		public static final ActionType SHUTDOWN_CALLED = new ActionType("SHUTDOWN_CALLED");

		public static final ActionType ABORT_CALLED = new ActionType("ABORT_CALLED");

		public static final ActionType RELOAD_CALLED = new ActionType("RELOAD_CALLED");

		public static final ActionType SEND_CALLED = new ActionType("SEND_CALLED");

		public static final ActionType RETRY_CONNECT = new ActionType("RETRY_CONNECT");

		public static final ActionType KEEPALIVE_TIMER = new ActionType("KEEPALIVE_TIMER");

		private final String _name; // for debug only

		private ActionType(String name) {
			_name = name;
		}

		public String toString() {
			return _name;
		}
	}

	private class Worker implements Runnable {

		private final Selector _selector;

		private final UnboundedQueue<WorkAction> _workQueue;

		public Worker() throws IOException {
			_selector = Selector.open();
			_workQueue = new UnboundedQueue<WorkAction>();
		}

		public void run() {
			synchronized(_protocolLock) {
				// Warten bis Protokoll gestartet wird
				while(_protocolState == ProtocolState.CREATED) {
					_debug.fine("Warten auf den Start des Protokolls: " + toString());
					try {
						_protocolLock.wait();
					}
					catch(InterruptedException e) {
						e.printStackTrace();
						_debug.error("Fehler im " + toString() + ": " + e);
					}
				}
			}
			// Hier wird testweise 30 Sekunden gewartet, um der TLS-OSI-7 genügend Zeit zur Initialisierung zu geben
			_debug.fine("WanCom: 30 Sekunden warten: " + toString());
			try {
				Thread.sleep(30000);
			}
			catch(InterruptedException e) {
			}
			_debug.fine("WanCom: Beginn der Protokollabarbeitung: " + toString());
			while(true) {
				try {
					final ProtocolState state;
					synchronized(_protocolLock) {
						// Instabile Zwischenzustände werden innerhalb des synchronized Block überprüft,
						//  da eine Zustandsänderung notwendig sein könnte
						if(_protocolState == ProtocolState.STARTING) {
							_protocolState = ProtocolState.STARTED;
						}
						else if(_protocolState == ProtocolState.STOPPING) {
							
						}
						state = _protocolState;
					}

					if(state == ProtocolState.STARTING) {
						// nada: wird beim nächsten Schleifendurchlauf im synchronized Block (oben) behandelt
					}
					else if(state == ProtocolState.STARTED || state == ProtocolState.STOPPING) {
						_debug.finest("Protokoll arbeitet: " + this);

						WorkAction action;
						while(null != (action = _workQueue.poll(0))) {
							action._link.handleAction(action._action, _selector);
						}
						try {
							_debug.finest("Aufruf von select()");
							int count = _selector.select();
							_debug.finest("Rückgabe von select(): " + count);
							Set<SelectionKey> selectedKeys = _selector.selectedKeys();
							for(Iterator<SelectionKey> iterator = selectedKeys.iterator(); iterator.hasNext();) {
								SelectionKey selectionKey = iterator.next();
								iterator.remove();
								Link selectedLink = (Link)selectionKey.attachment();
								selectedLink.handleSelection(selectionKey, _selector);
							}
						}
						catch(IOException e) {
							e.printStackTrace();
						}
					}
					else if(state == ProtocolState.STOPPED) {
						_debug.fine("Protokoll wurde gestoppt: " + this);
						break;
					}
					else {
						_debug.error("ungültiger Zustand: " + state + "; " + this);
					}
				}
				catch(InterruptedException e) {
					_debug.warning("InterruptedException: " + this, e);
				}
				catch(RuntimeException e) {
					_debug.warning("Unerwarteter Fehler: " + e.getLocalizedMessage() + "; " + this, e);
				}
			}
			_debug.warning("Thread wird terminiert: " + this);
		}

		public String toString() {
			return "Worker für " + Client.this.toString();
		}

		public void notify(Link link, ActionType action) {
			_workQueue.put(new WorkAction(link, action));
			_debug.finer("Aufruf von _selector.wakeup()");
			_selector.wakeup();
		}

		class WorkAction {

			public final Link _link;

			public final ActionType _action;

			public WorkAction(Link link, ActionType action) {
				_link = link;
				_action = action;
			}

			public String toString() {
				return "WorkAction(link: " + _link + ", action: " + _action;
			}
		}
	}

	/**
	 * Bestimmt, ob die Kommunikation dieses Protokolls bereits mit der Methode {@link #start} aktiviert wurde.
	 *
	 * @return <code>true</code>, wenn die Kommunikation dieses Protokolls bereits aktiviert wurde, sonst <code>false</code>.
	 */
	public boolean isStarted() {
		synchronized(_protocolLock) {
			return (_protocolState == ProtocolState.STARTING) || (_protocolState == ProtocolState.STARTED);
		}
	}

	public void start() {
		_debug.fine("start(): " + this);
		synchronized(_protocolLock) {
			if(_protocolState != ProtocolState.CREATED) {
				throw new IllegalStateException("Protokoll kann nicht erneut gestartet werden: " + toString());
			}
			int localAddress = getLocalAddress();
			if(localAddress < 1 || localAddress > 254) {
				throw new IllegalStateException("lokale OSI-2 Adresse muss zwischen 1 und 254 liegen, ist: " + localAddress);
			}
			_workThread.setName("wancom.Client.Worker(" + localAddress + ")");
			_workThread.start();
			_protocolState = ProtocolState.STARTING;
			_protocolLock.notifyAll();
		}
	}


	public void shutdown() {
		_debug.fine("shutdown): " + this);
		synchronized(_protocolLock) {
			for(Iterator<Link> iterator = _links.iterator(); iterator.hasNext();) {
				Link link = iterator.next();
				link.shutdown();
			}
			if(_protocolState == ProtocolState.STARTED) _protocolState = ProtocolState.STOPPING;
			if(_protocolState != ProtocolState.STOPPING) _protocolState = ProtocolState.STOPPED;
			_protocolLock.notifyAll();
		}
	}

	public void abort() {
		_debug.fine("abort(): " + this);
		synchronized(_protocolLock) {
			for(Iterator<Link> iterator = _links.iterator(); iterator.hasNext();) {
				Link link = iterator.next();
				link.abort();
			}
			if(_protocolState == ProtocolState.STARTED) _protocolState = ProtocolState.STOPPING;
			if(_protocolState != ProtocolState.STOPPING) _protocolState = ProtocolState.STOPPED;
			_protocolLock.notifyAll();
		}
	}

	public void setProperties(Properties properties) {
		super.setProperties(properties);
		_debug.fine("Neue Einstellungen für: " + toString() + ", properties = " + properties);
		synchronized(_protocolLock) {
			for(Iterator<Link> iterator = _links.iterator(); iterator.hasNext();) {
				Link link = iterator.next();
				link.reload();
			}
		}
	}

	public DataLinkLayer.Link createLink(int remoteAddress) {
		return new Link(remoteAddress);
	}

	private class Link extends AbstractDataLinkLayer.Link implements DataLinkLayer.Link, PropertyQueryInterface {

		private final PriorityChannel _sendChannel;

		private SocketChannel _socketChannel;

		private Properties _properties = null;

		private final PropertyConsultant _propertyConsultant;

		private int _wanComVersion;

		private int _wanComKeepAliveTimeSeconds;

		private int _wanComKeepAliveTimeoutCount;

		private int _wanComConnectRetryDelay = 60;

		private final Timer _timer = new Timer(true);

		private final ByteBuffer _readBuffer = ByteBuffer.allocateDirect(2204);

		private final ByteBuffer _sendBuffer = ByteBuffer.allocateDirect(28 + getMaximumDataSize());

		private int _wanComKeepAliveType;

		/** WanCom-Typ für versendete TLS-Telegramme */
		private int _wanComTlsType;

		/** WanCom-Typ für empfangene TLS-Telegramme, -1 bedeutet, dass beliebige Typen akzeptiert werden */
		private int _wanComTlsTypeReceive;

		private long _lastKeepAliveReceive;

		private int _keepAliveReceiveTimeoutCount;

		private boolean _sendKeepAlive;

		private byte[] _wanComIp8 = new byte[8];

		byte[] _packetOnTheAir = null;

		private boolean _wanComWaitForInitialReceive;

		private boolean _tcpConnectedWaitingForFirstReceive = false;

		private void notifyWorker(ActionType action) {
			_worker.notify(this, action);
		}

		private Link(int remoteAddress) {
			super(remoteAddress);
			_propertyConsultant = new PropertyConsultant(this);
			if(remoteAddress < 1 || remoteAddress > 255) {
				throw new IllegalArgumentException(
						"OSI-2 Adresse muss zwischen 1 und 254 liegen oder den speziellen Wert 255 (Broadcastaddresse) haben, versorgt ist: " + remoteAddress
				);
			}
			_sendChannel = new PriorityChannel(3, 1000);
			_readBuffer.order(ByteOrder.LITTLE_ENDIAN);
			_sendBuffer.order(ByteOrder.LITTLE_ENDIAN);
			_linkState = LinkState.DISCONNECTED;
			synchronized(_protocolLock) {
				for(Iterator<Link> iterator = _links.iterator(); iterator.hasNext();) {
					Link link = iterator.next();
					if(link.getRemoteAddress() == _remoteAddress) {
						throw new IllegalStateException(
								"Es gibt bereits ein Verbindung mit dieser Secondary-Adresse: " + _remoteAddress
						);
					}
				}
				_links.add(this);
			}
		}

		public DataLinkLayer getDataLinkLayer() {
			return Client.this;
		}

		public String getProperty(String name) {
			synchronized(_linkPropertyLock) {
				String value = (_properties == null) ? null : _properties.getProperty(name);
				return (value == null) ? Client.this.getProperty(name) : value;
			}
		}

		public void setProperties(Properties properties) {
			synchronized(_linkLock) {
				synchronized(_linkPropertyLock) {
					_properties = properties;
					if(_linkState == LinkState.CONNECTED || _linkState == LinkState.CONNECTING) reload();
				}
			}
		}

		public void connect() {
			_debug.fine("connect " + this);
			synchronized(_protocolLock) {
				synchronized(_linkLock) {
					if(_linkState == LinkState.CONNECTED || _linkState == LinkState.CONNECTING) return;
					if(_linkState != LinkState.DISCONNECTED) {
						throw new IllegalStateException("Verbindung kann in diesem Zustand nicht aufgebaut werden: " + _linkState);
					}
					_linkState = LinkState.CONNECTING;
				}
				_protocolLock.notifyAll();
			}
			notifyWorker(ActionType.CONNECT_CALLED);
		}

		public void shutdown() {
			_debug.fine("shutdown " + this);
			synchronized(_linkLock) {
				if(_linkState == LinkState.DISCONNECTED || _linkState == LinkState.DISCONNECTING) return;
				_linkState = LinkState.DISCONNECTING;
			}
			try {
				_sendChannel.put(new PriorizedByteArray(null, 2));
			}
			catch(InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			notifyWorker(ActionType.SHUTDOWN_CALLED);
		}

		public void abort() {
			_debug.fine("abort " + this);
			synchronized(_linkLock) {
				if(_linkState == LinkState.DISCONNECTED || _linkState == LinkState.DISCONNECTING) return;
				_linkState = LinkState.DISCONNECTING;
			}
			try {
				_sendChannel.put(new PriorizedByteArray(null, 0));
			}
			catch(InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			notifyWorker(ActionType.ABORT_CALLED);
		}

		public void reload() {
			_debug.fine("reload " + this);
			synchronized(_protocolLock) {
				if(_protocolState == ProtocolState.STARTED || _protocolState == ProtocolState.STARTING) notifyWorker(ActionType.RELOAD_CALLED);
			}
		}

		public void send(byte[] bytes, int priority) throws InterruptedException {
			_debug.finer("Telegramm soll gesendet werden, Priorität: " + priority);
			//_debug.finer("Daten: " + HexDumper.toString(bytes));
			synchronized(_linkLock) {
				if(_linkState != LinkState.CONNECTED) {
					throw new IllegalStateException(
							"Telegramm kann in diesem Verbindungszustand nicht versendet werden: " + _linkState
					);
				}
			}
			_debug.finest("Telegramm wird zum Versand gepuffert, Priorität: " + priority);
			_sendChannel.put(new PriorizedByteArray(bytes, priority));
			notifyWorker(ActionType.SEND_CALLED);
		}


		public void handleAction(ActionType action, Selector selector) {
			_debug.finer("handleAction(" + action + "): " + this);
			if(action == ActionType.CONNECT_CALLED || action == ActionType.RETRY_CONNECT) {
				_debug.fine("Verbindung aufbauen");
				connectSocketChannel(selector);
			}
			else if(action == ActionType.KEEPALIVE_TIMER) {
				synchronized(_linkLock) {
					if(_linkState == LinkState.CONNECTED || (_linkState == LinkState.CONNECTING && _tcpConnectedWaitingForFirstReceive)) {
						if(_linkState == LinkState.CONNECTED) _sendKeepAlive = true;
						if(_lastKeepAliveReceive + _wanComKeepAliveTimeSeconds * 1000L < System.currentTimeMillis()) {
							++_keepAliveReceiveTimeoutCount;
							_debug.info("KeepAlive Timeout, Zähler: " + _keepAliveReceiveTimeoutCount + "; " + this);
							if(_keepAliveReceiveTimeoutCount >= _wanComKeepAliveTimeoutCount) {
								_debug.warning("Verbindung wird neu initialisiert wegen fehlenden KeepAlive Telegrammen: " + this);
								closeChannel();
								return;
							}
						}
						scheduleActionTimer(ActionType.KEEPALIVE_TIMER, _wanComKeepAliveTimeSeconds);
					}
				}
			}
			else if(action == ActionType.SEND_CALLED) {
				// handleAsyncSend() wird auf jeden Fall aufgerufen (s.u.)
			}
			else if(action == ActionType.RELOAD_CALLED) {
				closeChannel(2);
			}
			else if(action == ActionType.ABORT_CALLED) {
				// nichts zu tun
			}
			else if(action == ActionType.SHUTDOWN_CALLED) {
				// nichts zu tun
			}
			else {
				_debug.error("unbekannter ActionType: " + action);
			}
			handleAsyncSend(selector);
		}

		private void handleAsyncSend(Selector selector) {
			if(_socketChannel == null) return;
			try {
				do {
					if(!_sendBuffer.hasRemaining()) {
						if(_sendKeepAlive) {
							_debug.finer("Senden eines KeepAlive-Telegramms");
							_debug.finest(
									"eingetragene lokale IP: " + _wanComIp8[0] + "." + _wanComIp8[1] + "." + _wanComIp8[2] + "." + _wanComIp8[3] + "."
									+ _wanComIp8[4] + "." + _wanComIp8[5] + "." + _wanComIp8[6] + "." + _wanComIp8[7]
							);
							_sendKeepAlive = false;
							_sendBuffer.clear();
							int size = 43;
							_sendBuffer.putInt(_wanComVersion);
							_sendBuffer.putInt(size);
							_sendBuffer.putInt(_wanComKeepAliveType);
							_sendBuffer.putInt(0);
							_sendBuffer.putInt(0);
							_sendBuffer.put(_wanComIp8);
							_sendBuffer.put((byte)9);
							_sendBuffer.put((byte)255);
							_sendBuffer.put((byte)255);
							_sendBuffer.put((byte)0);
							_sendBuffer.put((byte)0);
							_sendBuffer.put((byte)0);
							_sendBuffer.put((byte)1);
							_sendBuffer.put((byte)7);
							_sendBuffer.put((byte)134);
							_sendBuffer.put((byte)2);
							_sendBuffer.put((byte)0);
							_sendBuffer.put((byte)1);
							_sendBuffer.put((byte)2);
							_sendBuffer.put((byte)255);
							_sendBuffer.put((byte)130);
							_sendBuffer.flip();
						}
						else {
							PriorizedByteArray priorizedByteArray = (PriorizedByteArray)_sendChannel.poll(0);
							if(priorizedByteArray != null) {
								final byte[] bytes = priorizedByteArray.getBytes();
								if(bytes == null) {
									closeChannel();
								}
								else {
									_packetOnTheAir = bytes;
									_debug.finer("Senden eines TLS-Telegramms");
									_debug.finest(
											"eingetragene lokale IP: " + _wanComIp8[0] + "." + _wanComIp8[1] + "." + _wanComIp8[2] + "." + _wanComIp8[3] + "."
											+ _wanComIp8[4] + "." + _wanComIp8[5] + "." + _wanComIp8[6] + "." + _wanComIp8[7]
									);
									_sendBuffer.clear();
									int size = 28 + bytes.length;
									_sendBuffer.putInt(_wanComVersion);
									_sendBuffer.putInt(size);
									_sendBuffer.putInt(_wanComTlsType);
									_sendBuffer.putInt(0);
									_sendBuffer.putInt(0);
									_sendBuffer.put(_wanComIp8);
									_sendBuffer.put(bytes);
									_sendBuffer.flip();
								}
							}
						}
					}
					if(_sendBuffer.hasRemaining()) {
						_debug.finest("Sendeversuch für verbleibende " + _sendBuffer.remaining() + " Bytes");
						int sent = _socketChannel.write(_sendBuffer);
						_debug.finest("erfolgreich gesendete Bytes " + sent);
					}
					if(_sendBuffer.hasRemaining()) {
						_debug.finer("Versand wird sobald möglich fortgesetzt");
						_socketChannel.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ, this);
						break;
					}
					else {
						if(_packetOnTheAir != null) {
							byte[] sentPacket = _packetOnTheAir;
							_packetOnTheAir = null;
							notifyEvent(DataLinkLayerEvent.Type.DATA_SENT, sentPacket);
						}
					}
				}
				while(!_sendChannel.isEmpty());
			}
			catch(InterruptedException e) {
				e.printStackTrace();
				_debug.warning("unerwartete Exception: " + e);
			}
			catch(IOException e) {
				_debug.warning("Verbindung wird wegen Fehler beim Senden initialisiert: " + e);
				closeChannel();
			}
		}

		private void closeChannel() {
			closeChannel(_wanComConnectRetryDelay);
		}

		private void closeChannel(int reconnectDelay) {
			synchronized(_linkLock) {
				_tcpConnectedWaitingForFirstReceive = false;
				if(_socketChannel != null) {
					try {
						_socketChannel.close();
					}
					catch(IOException e) {
						_debug.warning("Fehler beim Schließen des SocketChannels: " + e);
					}
					finally {
						_socketChannel = null;
					}
				}
				if(_linkState == LinkState.DISCONNECTING) {
					_linkState = LinkState.DISCONNECTED;
					notifyEvent(DataLinkLayerEvent.Type.DISCONNECTED, null);
				}
				else if(_linkState == LinkState.CONNECTED) {
					_linkState = LinkState.CONNECTING;
					_debug.fine("Nächster Verbundungsversuch in " + reconnectDelay + " Sekunden; " + this);
					scheduleActionTimer(ActionType.RETRY_CONNECT, reconnectDelay);
					notifyEvent(DataLinkLayerEvent.Type.DISCONNECTED, null);
				}
				else if(_linkState == LinkState.CONNECTING) {
					_debug.fine("Nächster Verbundungsversuch in " + reconnectDelay + " Sekunden; " + this);
					scheduleActionTimer(ActionType.RETRY_CONNECT, reconnectDelay);
				}
				else {
					_debug.error("closeChannel: Unmöglicher Zustand: Fehler ohne bestehende Verbindung; " + this);
					_linkState = LinkState.DISCONNECTED;
				}
			}
		}

		public void handleSelection(SelectionKey selectionKey, Selector selector) {
			_debug.finer("handleSelection(" + selectionKey.readyOps() + "/" + selectionKey.interestOps() + "): " + this);
			if(selectionKey.isConnectable()) {
				_debug.fine("Verbindungsaufbau abschließen");
				connectSocketChannel(selector);
			}
			if(!selectionKey.isValid()) return;
			if(selectionKey.isReadable()) {
				_debug.finest("Telegramm-Daten empfangen");
				try {
					_debug.finest("_readBuffer vorm lesen: " + _readBuffer);
					//HexDumper.dumpTo(System.out,_readBuffer.array(),0, _readBuffer.position());
					int got = _socketChannel.read(_readBuffer);
					if(got == -1) {
						_debug.info("Verbindung wurde von der Gegenseite terminiert; " + this);
						closeChannel();
					}
					else {
						_debug.finest("Anzahl gelesener Bytes: " + got);
						//_debug.finer("_readBuffer", _readBuffer);
						//HexDumper.dumpTo(System.out,_readBuffer.array(),0, _readBuffer.position());
						_readBuffer.flip();
						int remaining;
						_debug.finest("_readBuffer: " + _readBuffer);

						while((remaining = _readBuffer.remaining()) >= 28) {
							int telegramPosition = _readBuffer.position();
							int telegramVersion = _readBuffer.getInt(telegramPosition + 0);
							int telegramSize = _readBuffer.getInt(telegramPosition + 4);
							_debug.finest("version: " + telegramVersion);
							_debug.finest("size: " + telegramSize);
							try {
								if(telegramVersion != _wanComVersion) {
									throw new IllegalTelegramException("Falsche WanCom Version: " + telegramVersion);
								}
								if(telegramSize < 28) {
									throw new IllegalTelegramException("Empfangene WanCom-Telegrammgröße ist zu klein: " + telegramSize);
								}
								if(telegramSize > 2204) {
									throw new IllegalTelegramException("Empfangene WanCom-Telegrammgröße ist zu groß: " + telegramSize);
								}
								if(remaining >= telegramSize) {
									int telegramType = _readBuffer.getInt(telegramPosition + 8);
									int telegramDestinationIpCount = _readBuffer.getInt(telegramPosition + 12);
									if(telegramDestinationIpCount < 0 || telegramDestinationIpCount > 16) {
										throw new IllegalTelegramException(
												"Ungültiger Anzahl IP-Adressen im WanCom im Telegramm: " + telegramDestinationIpCount
										);
									}
									int telegramDestinationIpPointer = _readBuffer.getInt(telegramPosition + 16);
									if(telegramDestinationIpPointer < 0 || telegramDestinationIpPointer > telegramDestinationIpCount) {
										throw new IllegalTelegramException(
												"Ungültiger IP-Adress-Zeiger im WanCom im Telegramm: " + telegramDestinationIpCount
										);
									}
									int payloadOffset = 5 * 4 + 8 + telegramDestinationIpCount * 8;
									if(payloadOffset > telegramSize) {
										throw new IllegalTelegramException(
												"Berechneter Start der Nutzdaten liegt ausserhalb des Telegramms: " + payloadOffset
										);
									}
									int payloadSize = telegramSize - payloadOffset;
									if(telegramDestinationIpPointer != telegramDestinationIpCount) {
										_debug.warning("IP-Routing in Wan-Com Telegrammen wird nicht unterstützt");
									}
									else {
										if(_wanComTlsTypeReceive == -1 || telegramType == _wanComKeepAliveType || telegramType == _wanComTlsTypeReceive) {
											final boolean tcpConnectedWaitingForFirstReceive = _tcpConnectedWaitingForFirstReceive;
											_tcpConnectedWaitingForFirstReceive = false;
											if(tcpConnectedWaitingForFirstReceive && _linkState == LinkState.CONNECTING) {
												_linkState = LinkState.CONNECTED;
												notifyEvent(DataLinkLayerEvent.Type.CONNECTED, null);
												_sendKeepAlive = true;
											}
											if(telegramType == _wanComKeepAliveType) {
												_debug.finer("keepAlive Telegramm empfangen; " + this);
												_lastKeepAliveReceive = System.currentTimeMillis();
												_keepAliveReceiveTimeoutCount = 0;
											}
											else {
												// TLS Telegramm verarbeiten
												_debug.finer("TLS Telegramm empfangen; " + this);
												_readBuffer.position(telegramPosition + payloadOffset);
												byte[] payload = new byte[payloadSize];
												_readBuffer.get(payload);
												notifyEvent(DataLinkLayerEvent.Type.DATA_RECEIVED, payload);
											}
										}
										else {
											throw new IllegalTelegramException("Ungültiger WanCom Type im Telegramm: " + telegramType);
										}
									}
									_readBuffer.position(telegramPosition + telegramSize);
								}
								else {
									// Nicht genügend Bytes im Puffer => Warten auf weitere Daten
									break;
								}
							}
							catch(IllegalTelegramException e) {
								e.printStackTrace();
								_debug.error(e.getLocalizedMessage());
								closeChannel();
								return;
							}
						}
						_readBuffer.compact();
					}
				}
				catch(IOException e) {
					e.printStackTrace();
				}
			}
			if(!selectionKey.isValid()) return;
			if(selectionKey.isWritable()) {
				if(!_sendBuffer.hasRemaining()) selectionKey.interestOps(SelectionKey.OP_READ);
			}
			if(!selectionKey.isValid()) return;
			handleAsyncSend(selector);
		}

		private void connectSocketChannel(Selector selector) {
			synchronized(_linkLock) {
				if(_linkState == LinkState.CONNECTING) {
					try {
						final boolean connectFinished;
						if(_socketChannel == null) {
							_keepAliveReceiveTimeoutCount = 0;
							_readBuffer.clear();
							_sendBuffer.clear().flip();
							_packetOnTheAir = null;
							_sendKeepAlive = false;
							final String remoteHost = _propertyConsultant.getProperty("wancom.host");
							final int remotePort = _propertyConsultant.getIntProperty("wancom.port");
							_wanComVersion = _propertyConsultant.getIntProperty("wancom.version");
							_wanComKeepAliveTimeSeconds = _propertyConsultant.getIntProperty("wancom.keepAliveTime");
							_wanComKeepAliveTimeoutCount = _propertyConsultant.getIntProperty("wancom.keepAliveTimeoutCount");
							_wanComKeepAliveType = _propertyConsultant.getIntProperty("wancom.keepAliveType");
							_wanComTlsType = _propertyConsultant.getIntProperty("wancom.tlsType");
							try {
								_wanComTlsTypeReceive = _propertyConsultant.getIntProperty("wancom.tlsTypeReceive");
							}
							catch(Exception e) {
								_wanComTlsTypeReceive = _wanComTlsType;
							}
							_wanComConnectRetryDelay = _propertyConsultant.getIntProperty("wancom.connectRetryDelay");
							_wanComWaitForInitialReceive = _propertyConsultant.getBooleanProperty("wancom.waitForInitialReceive");

							_socketChannel = SocketChannel.open();
							_socketChannel.configureBlocking(false);
							_debug.info("Verbindungsversuch zu " + remoteHost + ":" + remotePort + " wird gestartet; " + this);
							connectFinished = _socketChannel.connect(new InetSocketAddress(remoteHost, remotePort));
						}
						else {
							connectFinished = _socketChannel.finishConnect();
						}
						if(connectFinished) {
							_debug.info("Verbindungsaufbau abgeschlossen; " + this);
							byte[] ip = _socketChannel.socket().getLocalAddress().getAddress();
							final String localAddress = _propertyConsultant.getProperty("wancom.localAddress");
							if(localAddress != null && !localAddress.equals("")) {
								ip = InetAddress.getByName(localAddress).getAddress();
							}
							System.arraycopy(ip, 0, _wanComIp8, 0, Math.min(ip.length, _wanComIp8.length));
							_lastKeepAliveReceive = System.currentTimeMillis();
							_socketChannel.register(selector, SelectionKey.OP_READ, this);
							if(!_wanComWaitForInitialReceive) {
								_tcpConnectedWaitingForFirstReceive = false;
								_linkState = LinkState.CONNECTED;
								notifyEvent(DataLinkLayerEvent.Type.CONNECTED, null);
								_sendKeepAlive = true;
							}
							else {
								_tcpConnectedWaitingForFirstReceive = true;
							}
							scheduleActionTimer(ActionType.KEEPALIVE_TIMER, _wanComKeepAliveTimeSeconds);
						}
						else {
							_debug.info("Verbindungsaufbau ist noch nicht abgeschlossen und wird asynchron durchgeführt; " + this);
							_socketChannel.register(selector, SelectionKey.OP_CONNECT, this);
						}
					}
					catch(Exception e) {
						_debug.warning("Verbindungsversuch hat nicht funktioniert; " + this, e);
						closeChannel();
					}
				}
			}
		}

		private void scheduleActionTimer(final ActionType actionType, int delaySeconds) {
			final TimerTask timerTask = new TimerTask() {
				public void run() {
					notifyWorker(actionType);
				}
			};
			_timer.schedule(timerTask, delaySeconds * 1000L);
		}
	}

	private static class IllegalTelegramException extends Exception {

		public IllegalTelegramException(String message) {
			super(message);
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
}
