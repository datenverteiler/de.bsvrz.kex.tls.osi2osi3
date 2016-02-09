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

import de.bsvrz.kex.tls.osi2osi3.osi3.TlsNetworkLayerSetting;

import java.util.*;

/**
 * Defaultparameter für Protokoll- und Verbindungsparameter der TC57-Protokolle
 *
 * @author Kappich Systemberatung
 * @version $Revision: 10066 $
 */
public class DefaultProperties extends Properties {

	/** Einziges Objekt dieser Klasse (Singleton). */
	private static DefaultProperties _defaultProperties = new DefaultProperties();

	/**
	 * Bestimmt das einziges Objekt dieser Klasse (Singleton).
	 *
	 * @return Einziges Objekt dieser Klasse (Singleton).
	 */
	public static DefaultProperties getInstance() {
		return _defaultProperties;
	}

	/* Nicht öffentlicher Konstruktor zum Erzeugen der Defaultparameter */
	private DefaultProperties() {
		boolean kri = true;
		if(kri) {
//			setProperty("seriell.port", "/dev/ttyS0");// Name der seriellen Schnittstelle
			setProperty("seriell.port", "COM1");      // Name der seriellen Schnittstelle
			setProperty("seriell.bps", "9600");       // Übertragungsgeschwindigkeit in Bit pro Sekunde
			setProperty("seriell.bits", "8");         // Bits pro übertragenem Byte
			setProperty("seriell.stopbits", "1");     // Anzahl Stopbits
			setProperty("seriell.parität", "gerade"); // Parität (gerade|ungerade|gesetzt|gelöscht|keine)
			setProperty("seriell.paritätPrüfen", "ja"); // Parität prüfen (ja|nein)
			setProperty("seriell.empfangsPuffer", "1000"); // Größe des Empfangspuffers in Byte
			setProperty("seriell.überlaufPrüfen", "nein"); // Auf Überlauffehler prüfen (ja|nein)
			setProperty("seriell.rts", "immer");     // Soll RTS Signal gesetzt werden (immer|senden|nie)
			setProperty("seriell.rtsVorlauf", "0");  // Sendevorlaufzeit in Millisekunden zwischen Setzen von RTS und Versand
			setProperty("seriell.rtsNachlauf", "0");  // Sendenachlaufzeit in Millisekunden zwischen Versand und Rücksetzen von RTS
			setProperty("seriell.cts", "nein");         // Soll vor dem Senden auf CTS gewartet werden (ja|nein)
			setProperty("seriell.dcd", "nein");         // Soll vor dem Versand abgewartet werden bis DCD nicht mehr gesetzt ist (ja|nein)
			setProperty("seriell.dsr", "ja");         // Soll mit dem DSR Signal der Anschluß von Kabel bzw. Modem geprüft werden (ja|nein)
			setProperty("seriell.empfangsTimeout", "40");  // Zeitlücke zum Erkennen des Telegrammendes in Millisekunden

			// Zur genauen Bestimmung des Endes der Sendernachlaufzeit (siehe "seriell.rtsNachlauf") kann auf eine
			// Rückmeldung nach dem Versand des letzten versendeten Zeichens gewartet werden. Da dies nicht von allen
			// seriellen Schnittstellen unterstützt wird und dadurch das Polling auf dem betroffenen Inselbus blockiert,
			//  kann diese Funktion abgeschaltet werden. Der Zeitpunkt des Telegrammendes wird dann errechnet.
			setProperty("seriell.aufVersandRückmeldungWarten", "ja");

			// Nach dem Versand einschließlich rtsNachlauf wird der Eingangspuffer zusätzlich gelöscht. Dies ist sinnvoll,
			// wenn Echo-Zeichen oder Schmierzeichen beim Umschalten der Senderichtung erkannt werden.
			// Das Timing (rtsNachlauf) muss dann sehr genau eingestellt werden, weil sonst Teile des Antworttelegramms gelöscht werden.
			setProperty("seriell.empfangsPufferNachVersandLöschen", "nein");

			setProperty("tc57.Tw", "40");             // Wartezeit zwischen Empfang und Senden Millisekunden (40 bis 200)
			setProperty("primary.Tap", "1000");        // Antwortüberwachungszeit der Primary (150 bis 400)
			setProperty(
					"primary.wiederholungsAnzahl", "2"
			);  // Mindestanzahl der Telegrammwiederholungen bei Übertragungsfehlern auf einer Verbindung bevor diese neu initialisiert wird
			setProperty(
					"primary.wiederholungsDauer", "30000"
			);  // Mindestdauer in Millisekunden für Telegrammwiederholungen bei Übertragungsfehlern auf einer Verbindung bevor diese neu initialisiert wird.
			setProperty(TlsNetworkLayerSetting.pointerIncrement, "ja"); //Vor dem Versenden wird der Pointer inkrementiert
			setProperty(TlsNetworkLayerSetting.acceptSecondaryAddress, "nein"); //Die Adressen 200-254 zulassen
			setProperty(TlsNetworkLayerSetting.reduceToControlByte, "nein"); //Die Routing-Informationen entfernen
			setProperty(TlsNetworkLayerSetting.reflectOsi3Routing, "nein"); //KRI-Telegramme spiegeln
		}
		else {
//			setProperty("seriell.port", "/dev/ttyS0");// Name der seriellen Schnittstelle
			setProperty("seriell.port", "COM1");      // Name der seriellen Schnittstelle
			setProperty("seriell.bps", "1200");       // Übertragungsgeschwindigkeit in Bit pro Sekunde
			setProperty("seriell.bits", "8");         // Bits pro übertragenem Byte
			setProperty("seriell.stopbits", "1");     // Anzahl Stopbits
			setProperty("seriell.parität", "gerade"); // Parität (gerade|ungerade|gesetzt|gelöscht|keine)
			setProperty("seriell.paritätPrüfen", "nein"); // Parität prüfen (ja|nein)
			setProperty("seriell.empfangsPuffer", "1000"); // Größe des Empfangspuffers in Byte
			setProperty("seriell.überlaufPrüfen", "nein"); // Auf Überlauffehler prüfen (ja|nein)
			setProperty("seriell.rts", "senden");     // Soll RTS Signal gesetzt werden (immer|senden|nie)
			setProperty("seriell.rtsVorlauf", "50");  // Sendevorlaufzeit in Millisekunden zwischen Setzen von RTS und Versand
			setProperty("seriell.rtsNachlauf", "0");  // Sendenachlaufzeit in Millisekunden zwischen Versand und Rücksetzen von RTS
			setProperty("seriell.cts", "ja");         // Soll vor dem Senden auf CTS gewartet werden (ja|nein)
			setProperty("seriell.dcd", "ja");         // Soll vor dem Versand abgewartet werden bis DCD nicht mehr gesetzt ist (ja|nein)
			setProperty("seriell.dsr", "ja");         // Soll bei der Initialisierung auf DSR gewartet werden (ja|nein)
			setProperty("seriell.empfangsTimeout", "150");  // Zeitlücke zum Erkennen des Telegrammendes in Millisekunden

			// Zur genauen Bestimmung des Endes der Sendernachlaufzeit (siehe "seriell.rtsNachlauf") kann auf eine
			// Rückmeldung nach dem Versand des letzten versendeten Zeichens gewartet werden. Da dies nicht von allen
			// seriellen Schnittstellen unterstützt wird und dadurch das Polling auf dem betroffenen Inselbus blockiert,
			//  kann diese Funktion abgeschaltet werden. Der Zeitpunkt des Telegrammendes wird dann errechnet.
			setProperty("seriell.aufVersandRückmeldungWarten", "ja");

			// Nach dem Versand einschließlich rtsNachlauf wird der Eingangspuffer zusätzlich gelöscht. Dies ist sinnvoll,
			// wenn Echo-Zeichen oder Schmierzeichen beim Umschalten der Senderichtung erkannt werden.
			// Das Timing (rtsNachlauf) muss dann sehr genau eingestellt werden, weil sonst Teile des Antworttelegramms gelöscht werden.
			setProperty("seriell.empfangsPufferNachVersandLöschen", "ja");

			setProperty("tc57.Tw", "40");             // Wartezeit zwischen Empfang und Senden Millisekunden (40 bis 200)
			setProperty("primary.Tap", "500");        // Antwortüberwachungszeit der Primary (150 bis 400)
			setProperty(
					"primary.wiederholungsAnzahl", "2"
			);  // Mindestanzahl der Telegrammwiederholungen bei Übertragungsfehlern auf einer Verbindung bevor diese neu initialisiert wird
			setProperty(
					"primary.wiederholungsDauer", "30000"
			);  // Mindestdauer in Millisekunden für Telegrammwiederholungen bei Übertragungsfehlern auf einer Verbindung bevor diese neu initialisiert wird.
			setProperty(TlsNetworkLayerSetting.pointerIncrement, "ja"); //Vor dem Versenden wird der Pointer inkrementiert
			setProperty(TlsNetworkLayerSetting.acceptSecondaryAddress, "nein"); //Die Adressen 200-254 zulassen
			setProperty(TlsNetworkLayerSetting.reduceToControlByte, "nein"); //Die Routing-Informationen entfernen
			setProperty(TlsNetworkLayerSetting.reflectOsi3Routing, "nein"); //KRI-Telegramme spiegeln
		}
	}
}
