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


import de.bsvrz.dav.daf.main.ClientDavInterface;

import java.util.*;

/**
 * Schnittstelle für Protokolle der Sicherungsschicht (OSI 2).
 *
 * @author Kappich Systemberatung
 * @version $Revision: 5114 $
 */
public interface DataLinkLayer {

	/**
	 * Nimmmt die Verbindung zum Datenverteiler entgegen. Diese Methode wird vom OSI-3 Modul nach dem Erzeugen des OSI-2 Moduls durch den jeweiligen Konstruktor
	 * aufgerufen. Eine Implementierung eines Protokollmoduls kann sich bei Bedarf die übergebene Datenverteilerverbindung intern merken, um zu späteren
	 * Zeitpunkten auf die Datenverteiler-Applikationsfunktionen zuzugreifen.
	 *
	 * @param connection Verbindung zum Datenverteiler
	 */
	void setDavConnection(ClientDavInterface connection);

	/**
	 * Bestimmt die OSI2-Adresse bzw. Portnummer des eigenen Kommunikationsendpunkts.
	 *
	 * @return Eigene OSI2-Adresse bzw. Portnummer.
	 */
	int getLocalAddress();

	/**
	 * Setzt die OSI2-Adresse bzw. Portnummer des eigenen Kommunikationsendpunkts.
	 *
	 * @param port Eigene OSI2-Adresse bzw. Portnummer.
	 */
	void setLocalAddress(int port);

	/**
	 * Bestimmt den Wert eines bestimmten Protokoll-Parameters.
	 *
	 * @param name Name des Protokoll-Parameters.
	 *
	 * @return Wert des Protokoll-Parameters.
	 */
	String getProperty(String name);

	/**
	 * Setzt einen neuen Satz von Protokoll-Parametern. Diese dienen auch als Defaultwerte für die Verbindungsparameter.
	 *
	 * @param properties Neue Verbindungsparameter.
	 *
	 * @see Link#getProperty
	 */
	void setProperties(Properties properties);

	/**
	 * Bestimmt die maximale Anzahl von Nutzdatenbytes in einem OSI-2 Paket (Telegramm).
	 *
	 * @return Maximale Anzahl Nutzdatenbytes.
	 */
	int getMaximumDataSize();

	public void addEventListener(DataLinkLayerListener dataLinkLayerListener);

	public void removeEventListener(DataLinkLayerListener dataLinkLayerListener);

	/** Aktiviert die Kommunikation dieses Protokolls. */
	void start();

	/**
	 * Beendet die Kommunikation dieses Protokolls. Alle noch verbundenen Links werden mit der Methode {@link Link#shutdown} terminiert. Dabei wird sichergestellt,
	 * dass Daten, die zuvor an die Methode {@link de.bsvrz.kex.tls.osi2osi3.osi2.api.DataLinkLayer.Link#send} übergeben wurden, auch übertragen werden.
	 */
	void shutdown();

	/** Beendet die Kommunikation dieses Protokolls. Alle noch verbundenen Links werden mit der Methode {@link Link#abort} terminiert. */
	void abort();

	/**
	 * Bestimmt, ob die Kommunikation dieses Protokolls bereits mit dr Methode {@link #start} aktiviert wurde.
	 *
	 * @return <code>true</code>, wenn die Kommunikation dieses Protokolls bereits aktiviert wurde, sonst <code>false</code>.
	 */
	boolean isStarted();

	/**
	 * Erzeugt eine neue logische Verbindung zu einem bestimmten Kommunikationspartner.
	 *
	 * @param remoteAddress OSI-2 Adresse bzw. Portnummer des gewünschten Kommunikationspartners.
	 *
	 * @return Logische Verbindung zum Kommunikationspartner.
	 */
	DataLinkLayer.Link createLink(int remoteAddress);

	/**
	 * Schnittstellenklasse die eine logische Verbindung mit einem Kommunikationspartner darstellt.
	 *
	 * @see DataLinkLayer#createLink
	 */
	interface Link {

		/**
		 * Bestimmt das Kommunikationsprotokoll zu dem diese Verbindung gehört.
		 *
		 * @return Kommunikationsprotokoll dieser Verbindung.
		 */
		DataLinkLayer getDataLinkLayer();

		/**
		 * Bestimmt die OSI2-Adresse bzw. Portnummer des Kommunikationspartners dieser Verbindung.
		 *
		 * @return OSI2-Adresse bzw. Portnummer des Kommunikationspartners.
		 */
		int getRemoteAddress();

		/**
		 * Bestimmt den Wert eines bestimmten Verbindungsparameters. Wenn der gewünschte Verbindungsparameter nicht in den verbindungsspezifischen Parametern
		 * enthalten ist, wird mit der {@link DataLinkLayer#getProperty} Methode des Sicherungsprotokolls ein Defaultwert bestimmt.
		 *
		 * @param name Name des Verbindungsparameters.
		 *
		 * @return Wert des Verbindungsparameters.
		 *
		 * @see #setProperties
		 * @see DataLinkLayer#getProperty
		 */
		String getProperty(String name);

		/**
		 * Setzt einen neuen verbindungsspezifischen Satz von Verbindungsparametern.
		 *
		 * @param properties Neue Verbindungsparameter.
		 */
		void setProperties(Properties properties);

		/** Aktiviert die Kommunikation auf dieser logischen Verbindung. */
		void connect();

		/**
		 * Beendet die Kommunikation auf dieser logischen Verbindung. Vor der Terminierung der Verbindung wird sichergestellt, dass Daten, die zuvor an die Methode
		 * {@link #send} übergeben wurden, auch übertragen werden.
		 */
		void shutdown() throws InterruptedException;

		/**
		 * Abbruch der Kommunikation auf dieser logischen Verbindung. Der Aufruf dieser Methode führt zur sofortigen Terminierung der Verbindung. Daten, die zuvor an
		 * die Methode {@link #send} übergeben wurden und noch nicht übertragen wurden, werden nicht mehr übertragen.
		 */
		void abort() throws InterruptedException;

		/**
		 * Bestimmt den aktuellen Verbindungszustand der Verbindung.
		 *
		 * @return Verbindungszustand der Verbindung.
		 */
		public LinkState getState();

		/**
		 * Übernimmt die übergebenen Nutzdaten in den Sendepuffer. Die übergebenen Nutzdaten werden asynchron in der Reihenfolge der Aufrufe dieser Methode an den
		 * Kommunikationspartner dieser Verbindung übertragen.
		 *
		 * @param bytes    Zu übertragende Nutzdatenbytes.
		 * @param priority Priorität der zu übertragenden Daten
		 */
		void send(byte[] bytes, int priority) throws InterruptedException;
	}
}
