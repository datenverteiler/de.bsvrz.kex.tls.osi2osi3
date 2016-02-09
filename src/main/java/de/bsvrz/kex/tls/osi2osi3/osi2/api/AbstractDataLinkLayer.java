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

package de.bsvrz.kex.tls.osi2osi3.osi2.api;

import de.bsvrz.kex.tls.osi2osi3.osi3.TlsNetworkLayerSetting;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.*;

/**
 * Klasse mit Default-Implementierungen einzelner Methoden der Schnittstelle für Protokolle der Sicherungsschicht (OSI 2).
 *
 * @author Kappich Systemberatung
 * @version $Revision: 10187 $
 */
public abstract class AbstractDataLinkLayer implements DataLinkLayer {

	private final LinkedList<DataLinkLayerListener> _dataLinkLayerListeners = new LinkedList<DataLinkLayerListener>();

	private int _localAddress = 0;

	private Properties _properties = null;

	private final Object _propertiesSync = new Object();

	private final Debug _debug = Debug.getLogger();

	public AbstractDataLinkLayer() {

		_properties = new Properties();
	}

	abstract protected String getDefaultProperty(String name);

	public String getProperty(String name) {
		synchronized(_propertiesSync) {
			String value = (_properties != null) ? _properties.getProperty(name) : null;
			return (value != null) ? value : getDefaultProperty(name);
		}
	}

	public void setProperties(Properties properties) {
		synchronized(_propertiesSync) {
			_properties = properties;
		}
	}

	public final void addEventListener(DataLinkLayerListener dataLinkLayerListener) {
		synchronized(_dataLinkLayerListeners) {
			_dataLinkLayerListeners.add(dataLinkLayerListener);
		}
	}

	public final void removeEventListener(DataLinkLayerListener dataLinkLayerListener) {
		synchronized(_dataLinkLayerListeners) {
			_dataLinkLayerListeners.remove(dataLinkLayerListener);
		}
	}

	protected final void notifyEvent(DataLinkLayerEvent event) {
		synchronized(_dataLinkLayerListeners) {
			Iterator iterator = _dataLinkLayerListeners.iterator();
			while(iterator.hasNext()) {
				((DataLinkLayerListener)iterator.next()).handleDataLinkLayerEvent(event);
			}
		}
	}

	protected final void notifyEvent(DataLinkLayer.Link link, DataLinkLayerEvent.Type type, Object data) {
		notifyEvent(new DataLinkLayerEvent(this, link, type, data));
	}

	public void setLocalAddress(int port) {
		_localAddress = port;
	}

	public int getLocalAddress() {
		return _localAddress;
	}


	/**
	 * Überprüft, ob die Verbindung zulässig ist. Die Secondary Adresse 1-199 und 255 ist immer zulässig, die Adressen 200-254 sind nur zulässig wenn Parameter
	 * "secondary.adressen200-254Akzeptieren" gesetzt ist. Sollte die Verbindung nicht zulässig sein, wird eine Debug Ausgabe erzeugt.
	 *
	 * @param links      Liste mit allen Links/Verbindungen
	 * @param properties Neue Properties
	 */
	protected void checkConnection(final List<Link> links, final Properties properties) {
		if(!allowConnection(links, properties)) {
			_debug.warning(
					"Secondary Adressen dürfen nur im Bereich von 1-199 liegen." + " \nLokaler Port des Anschlusspunkts: " + getLocalAddress()
					+ "\nDeswegen wird kein Datenaustausch akzeptiert! Setzen bzw. ändern Sie den Parameter atg.protokollEinstellungenStandard, "
					+ TlsNetworkLayerSetting.acceptSecondaryAddress + " auf Ja, um den Datenaustausch zu zulassen."
			);
		}
	}

	/**
	 * Überprüft ob eine Verbindung erlaubt ist. Normalerweise sind die Secondary-Adressen von 1-199 und 255 erlaubt. Mithilfe des Parameters
	 * "secondary.adressen200-254Akzeptieren" können auch die Adressen 1-255 zugelassen werden.
	 *
	 * @param links Liste mit allen Verbindungen
	 *
	 * @return <code>true</code>: Die Verbindung darf aufgebaut werden.<br><code>false</code>: Es darf kein Telegram Austausch stattfinden.
	 */
	protected boolean allowConnection(final List<Link> links) {
		Properties properties = new Properties();
		properties.setProperty(TlsNetworkLayerSetting.acceptSecondaryAddress, getProperty(TlsNetworkLayerSetting.acceptSecondaryAddress));
		return allowConnection(links, properties);
	}

	/**
	 * Überprüft ob eine Verbindung erlaubt ist. Normalerweise sind die Secondary-Adressen von 1-199 und 255 erlaubt. Mithilfe des Parameters
	 * "secondary.adressen200-254Akzeptieren" können auch die Adressen 1-255 zugelassen werden.
	 *
	 * @param links      Liste mit allen Verbindungen
	 * @param properties Neue Einstellungen
	 *
	 * @return <code>true</code>: Die Verbindung darf aufgebaut werden.<br><code>false</code>: Es darf kein Telegram Austausch stattfinden.
	 */
	protected boolean allowConnection(final List<Link> links, final Properties properties) {
		final boolean osi2AllowSecondaryAddress;
		String osi2AllowSecondaryAddressValue = properties.getProperty(TlsNetworkLayerSetting.acceptSecondaryAddress);
		if(osi2AllowSecondaryAddressValue == null) osi2AllowSecondaryAddressValue = "";
		osi2AllowSecondaryAddressValue = osi2AllowSecondaryAddressValue.trim().toLowerCase();
		osi2AllowSecondaryAddress =
				osi2AllowSecondaryAddressValue.equals("ja") || osi2AllowSecondaryAddressValue.equals("yes") || osi2AllowSecondaryAddressValue.equals("true")
				|| osi2AllowSecondaryAddressValue.equals("wahr");

		for(Link link : links) {
			if(link.getRemoteAddress() > 199 && link.getRemoteAddress() < 255) {
				return osi2AllowSecondaryAddress;
			}
		}
		return true;
	}

	public abstract static class Link implements DataLinkLayer.Link {

		protected final int _remoteAddress;

		protected final Object _linkLock = new Object();

		protected final Object _linkPropertyLock = new Object();

		protected LinkState _linkState;

		public Link(int remoteAddress) {
			_remoteAddress = remoteAddress;
		}

		public String toString() {
			return "OSI2-Verbindung von " + getDataLinkLayer() + " nach Kommunikationspartner(" + getRemoteAddress() + "), Zustand: " + getState();
		}

		public int getRemoteAddress() {
			return _remoteAddress;
		}

		public LinkState getState() {
			synchronized(_linkLock) {
				return _linkState;
			}
		}

		protected final void notifyEvent(DataLinkLayerEvent.Type type, Object data) {
			((AbstractDataLinkLayer)getDataLinkLayer()).notifyEvent(this, type, data);
		}
	}
}
