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

import de.bsvrz.sys.funclib.hexdump.HexDumper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Objekte dieser Klasse beschreiben ein OSI2-Kommunikations-Ereignis. Diese Ereignisse werden i.a. von einer konkreten OSI2-Protokollimplementierung an eine
 * Anwendung bzw. an die nächst höhere Protokollebene übergeben.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 5114 $
 * @see DataLinkLayer#addEventListener
 */
public class DataLinkLayerEvent {

	/** Klasse zur Definition von verschiedenen Ereignistypen. */
	public static class Type {

		/**
		 * Ereignistyp der signalisiert, dass ein Fehler im Protokoll aufgetreten ist. Ereignisse dieses Typs können als zusätzliche Daten eine Zeichenkette (vom Typ
		 * String) mit einer Fehlerbeschreibung enthalten.
		 *
		 * @see DataLinkLayerEvent#getData
		 */
		public static final Type ERROR = new Type("Fehler");

		/**
		 * Ereignistyp der signalisiert, dass im Protokollablauf eine unerwartete Ausnahme aufgetreten ist. Ereignisse dieses Typs können als zusätzliche Daten das
		 * Ausnahmeobjekt (vom Typ Exception) enthalten.
		 *
		 * @see DataLinkLayerEvent#getData
		 */
		public static final Type EXCEPTION = new Type("Ausnahme");

		/** Ereignistyp der signalisiert, dass eine Verbindung hergestellt wurde. */
		public static final Type CONNECTED = new Type("Verbindung hergestellt");

		/** Ereignistyp der signalisiert, dass die Verbindung beendet wurde. */
		public static final Type DISCONNECTED = new Type("Verbindung terminiert");

		/**
		 * Ereignistyp der signalisiert, dass Nutzdaten ordnungsgemäß empfangen wurden. Ereignisse dieses Typs enthalten als zusätzliche Daten die empfangenen
		 * Nutzdaten (vom Typ byte[]).
		 *
		 * @see DataLinkLayerEvent#getData
		 */
		public static final Type DATA_RECEIVED = new Type("Daten empfangen");

		/**
		 * Ereignistyp der signalisiert, dass Nutzdaten, die mit der Methode {@link DataLinkLayer.Link#send} zum Versand übergeben wurden, ordnungsgemäß übertragen
		 * wurden. Ereignisse dieses Typs enthalten als zusätzliche Daten die versendeten Nutzdaten (vom Typ byte[]).
		 *
		 * @see DataLinkLayerEvent#getData
		 */
		public static final Type DATA_SENT = new Type("Daten gesendet");

		;

		/**
		 * Ereignistyp der signalisiert, das Bytes von tieferen Protokollebenen entgegengenommen wurden. Ereignisse dieses Typs enthalten als zusätzliche Daten die
		 * empfangenen Bytes (vom Typ byte[]).
		 *
		 * @see DataLinkLayerEvent#getData
		 */
		public static final Type BYTES_RECEIVED = new Type("Bytes empfangen");

		/**
		 * Ereignistyp der signalisiert, das Bytes an tiefere Protokollebenen zu Versand übergeben wurde. Ereignisse dieses Typs enthalten als zusätzliche Daten die
		 * versendeten Bytes (vom Typ byte[]).
		 *
		 * @see DataLinkLayerEvent#getData
		 */
		public static final Type BYTES_SENT = new Type("Bytes gesendet");

		/**
		 * Ereignistyp der signalisiert, dass das ein Telegramm empfangen wurde. Ereignisse dieses Typs enthalten als zusätzliche Daten eine Zeichenkette (vom Typ
		 * String), die alle Einzelheiten des Telegramms in Textform wiedergibt.
		 *
		 * @see DataLinkLayerEvent#getData
		 */
		public static final Type TELEGRAM_RECEIVED = new Type("Telegramm empfangen");

		/**
		 * Ereignistyp der signalisiert, dass das ein Telegramm versendet wurde. Ereignisse dieses Typs enthalten als zusätzliche Daten eine Zeichenkette (vom Typ
		 * String), die alle Einzelheiten des Telegramms in Textform wiedergibt.
		 *
		 * @see DataLinkLayerEvent#getData
		 */
		public static final Type TELEGRAM_SENT = new Type("Telegramm gesendet");

		/**
		 * Liefert eine textuelle Beschreibung dieses Ereignistyps zurück. Das genaue Format ist nicht festgelegt und kann sich ändern.
		 *
		 * @return Beschreibung dieses Ereignistyps.
		 */
		public String toString() {
			return _name;
		}

		private Type(String name) {
			_name = name;
		}

		private final String _name;
	}

	private final Type _type;

	private final long _time;

	private final DataLinkLayer _dataLinkLayer;

	private final DataLinkLayer.Link _link;

	private final Object _data;

	/**
	 * Erzeugt ein neues Ereignis.
	 *
	 * @param dataLinkLayer Protokoll, auf dass sich dieses Ereignis bezieht.
	 * @param link          Verbindung, auf dass sich dieses Ereignis bezieht oder <code>null</code>, wenn sich das Ereignis nicht auf eine spezielle Verbindung
	 *                      bezieht.
	 * @param type          Typ des neuen Ereignisses.
	 * @param data          Zusätzliche vom Typ abhängige Daten des Ereignisses oder <code>null</code>, wenn keine weiteren Daten vorliegen.
	 */
	public DataLinkLayerEvent(DataLinkLayer dataLinkLayer, DataLinkLayer.Link link, Type type, Object data) {
		_dataLinkLayer = dataLinkLayer;
		_link = link;
		_type = type;
		_time = System.currentTimeMillis();
		_data = data;
	}

	/*
	 * Liefert den Typ des Ereignisses zurück.
	 * @return Typ des Ereignisses.
	 */
	public Type getType() {
		return _type;
	}

	/*
	 * Liefert den Zeitpunkt der Erzeugung des Ereignisses zurück.
	 * @return Zeit in Millisekunden seit 1970.
	 */
	public long getTime() {
		return _time;
	}

	/*
	 * Liefert das diesem Ereignis zugeordnete Protokoll zurück.
	 * @return Zugeordnetes Protokoll.
	 */
	public DataLinkLayer getDataLinkLayer() {
		return _dataLinkLayer;
	}

	/*
	 * Liefert die dem Ereignis zugeordnete Verbindung zurück.
	 * @return Zugeordnete Verbindung oder <code>null</code>, wenn sich das
	 *         Ereignis nicht auf eine spezielle Verbindung bezieht.
	 */
	public DataLinkLayer.Link getLink() {
		return _link;
	}

	/*
	 * Liefert die dem Ereignis zugeordnete zusätzlichen Daten zurück.
	 * @return Zusätzliche Ereignisdaten oder <code>null</code>.
	 */
	public Object getData() {
		return _data;
	}

	/**
	 * Liefert eine textuelle Beschreibung dieses Ereignisses zurück. Das genaue Format ist nicht festgelegt und kann sich ändern.
	 *
	 * @return Beschreibung dieses Ereignisses.
	 */
	public String toString() {
		StringBuffer result = new StringBuffer("Ereignis: ");
		result.append(getTimeString());
		result.append(": ").append(getType()).append(", ");
		DataLinkLayer dataLinkLayer = getDataLinkLayer();
		if(dataLinkLayer == null) {
			result.append("?");
		}
		else {
			result.append(dataLinkLayer);
		}
		DataLinkLayer.Link link = getLink();
		if(link != null) {
			Type type = getType();
			if(type == Type.CONNECTED) {
				result.append(" <+> ");
			}
			//else if(type==Type.CONNECTED_RNR) result.append(" <+< ");
			//else if(type==Type.DISCONNECTED) result.append(" --- ");
			else if(type == Type.DATA_RECEIVED || type == Type.BYTES_RECEIVED || type == Type.TELEGRAM_RECEIVED) {
				result.append(" <-- ");
			}
			else if(type == Type.DATA_SENT || type == Type.BYTES_SENT || type == Type.TELEGRAM_SENT) {
				result.append(" --> ");
			}
			else {
				result.append(" <?> ");
			}
			result.append(link.getRemoteAddress());
		}
		Object data = getData();
		if(data != null) {
			result.append(", Daten: ");
			result.append(System.getProperty("line.separator"));
			if(data instanceof byte[]) {
				byte[] bytes = (byte[])data;
				//result.append(bytes.length).append(" bytes:");
				result.append(HexDumper.toString(bytes));
			}
			else if(data instanceof Exception) {
				Exception e = (Exception)data;
				result.append(e);
			}
			else {
				result.append(data);
			}
		}
		return result.toString();
	}

	private static final DateFormat _dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss,SSS");

	private String getTimeString() {
		synchronized(_dateFormat) {
			return _dateFormat.format(new Date(getTime()));
		}
	}
}
