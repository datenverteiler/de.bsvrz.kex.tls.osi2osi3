/*
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2004 by Kappich+Kniß Systemberatung, Aachen
 * Copyright 2006 by Kappich Systemberatung, Aachen
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

import de.bsvrz.kex.tls.osi2osi3.properties.PropertyConsultant;
import de.bsvrz.kex.tls.osi2osi3.properties.PropertyQueryInterface;
import de.bsvrz.sys.funclib.debug.Debug;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

//import javax.comm.CommPortIdentifier;
//import javax.comm.SerialPort;
//import javax.comm.SerialPortEvent;
//import javax.comm.SerialPortEventListener;

/**
 * Klasse zum Senden und Empfangen von TC57-Telegrammen via serieller Schnittstelle.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 10066 $
 */
public class SerialPortControl implements SerialPortEventListener {

	private static final Debug _debug = Debug.getLogger();

	private static final Object OPEN_ACCESS_LOCK = new Object();

	private PropertyConsultant _propertyConsultant = null;

	private SerialPort _port = null;

	private OutputStream _out = null;

	private InputStream _in = null;

	private boolean _rtsOnSend = false;

	private int _rtsPreSendDelay = 0;

	private int _rtsPostSendDelay = 0;

	private int _tap = 0;

	private int _interCharacterTimeout = 0;

	private boolean _waitForCts = false;

	private boolean _waitForDcdDown = false;

	private boolean _pendingOutput = false;

	private boolean _checkDsr = false;

	private long _lastSendTime = 0;

	private int _pendingInput = 0;

	//ReadableByteChannel _inChannel= null;
	private boolean _receiving = false;

	//ByteBuffer _inputBuffer= null;
	private long _lastReadTime = 0;

	private boolean _parityError = false;

	private boolean _overrunError = false;

	private int _receivedCount;

	private byte[] _receiveBuffer;

	private boolean _flushAfterSend = false;

	private long _byteTransmitDurationMicros = 0;   // Einheit Mikrosekunden

	private String _portName;

	private boolean _initialized = false;

	private boolean _waitWhilePendingOutput;


	public SerialPortControl() {
	}

	public synchronized void start(PropertyQueryInterface propertyQueryInterface, String applicationName) throws Exception {
		if(_port != null) throw new IllegalStateException("Port ist bereits gestartet");
		try {
			_propertyConsultant = new PropertyConsultant(propertyQueryInterface);
			_debug.finer("open...");
			_portName = _propertyConsultant.getProperty("seriell.port");
			int openTimeout = 10000;	// Millesekunden
			// folgende Synchronisation ist erforderlich, weil bei der gleichzeitigen Ansprache
			// von mehreren Schnittstellen bei manchen Schnittstellenkarten Probleme entstehen
			synchronized(OPEN_ACCESS_LOCK) {
				Thread.sleep(2000);
				_debug.info(applicationName, "Port suchen: " + _portName);
				CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(_portName);
				_debug.info(applicationName, "Port gefunden: " + _portName);
				_port = (SerialPort)portIdentifier.open(applicationName, openTimeout);
				_debug.info(applicationName, "Port geöffnet: " + _portName);
			}
			_port.setDTR(false);
			String rts = _propertyConsultant.getProperty("seriell.rts").trim().toLowerCase();
			if(rts.equals("immer")) {
				_port.setRTS(true);
				_rtsOnSend = false;
			}
			else if(rts.equals("nie")) {
				_port.setRTS(false);
				_rtsOnSend = false;
			}
			else if(rts.equals("senden")) {
				_port.setRTS(false);
				_rtsOnSend = true;
				_rtsPreSendDelay = _propertyConsultant.getIntProperty("seriell.rtsVorlauf");
				_rtsPostSendDelay = _propertyConsultant.getIntProperty("seriell.rtsNachlauf");
			}
			else {
				throw new IllegalArgumentException("Ungültiger Wert des Parameters seriell.rts");
			}
			String cts = _propertyConsultant.getProperty("seriell.cts").trim().toLowerCase();
			if(cts.equals("ja")) {
				_waitForCts = true;
			}
			else if(cts.equals("nein")) {
				_waitForCts = false;
			}
			else {
				throw new IllegalArgumentException("Ungültiger Wert des Parameters seriell.cts");
			}
			String dcd = _propertyConsultant.getProperty("seriell.dcd").trim().toLowerCase();
			if(dcd.equals("ja")) {
				_waitForDcdDown = true;
			}
			else if(dcd.equals("nein")) {
				_waitForDcdDown = false;
			}
			else {
				throw new IllegalArgumentException("Ungültiger Wert des Parameters seriell.dcd");
			}
			String dsr = _propertyConsultant.getProperty("seriell.dsr").trim().toLowerCase();
			if(dsr.equals("ja")) {
				_checkDsr = true;
			}
			else if(dsr.equals("nein")) {
				_checkDsr = false;
			}
			else {
				throw new IllegalArgumentException("Ungültiger Wert des Parameters seriell.dsr");
			}
			int bps = _propertyConsultant.getIntProperty("seriell.bps");
			int databitsMode;
			int databitsValue = _propertyConsultant.getIntProperty("seriell.bits");
			switch(databitsValue) {
				case 8:
					databitsMode = SerialPort.DATABITS_8;
					break;
				case 7:
					databitsMode = SerialPort.DATABITS_7;
					break;
				case 6:
					databitsMode = SerialPort.DATABITS_6;
					break;
				case 5:
					databitsMode = SerialPort.DATABITS_5;
					break;
				default:
					throw new IllegalArgumentException("Ungültige Anzahl Bits pro Byte in Schnittstelleneinstellungen");
			}
			int stopbitsMode;
			int stopbitsValue = (int)(10 * _propertyConsultant.getDoubleProperty("seriell.stopbits"));
			switch(stopbitsValue) {
				case 10:
					stopbitsMode = SerialPort.STOPBITS_1;
					break;
				case 15:
					stopbitsMode = SerialPort.STOPBITS_1_5;
					break;
				case 20:
					stopbitsMode = SerialPort.STOPBITS_2;
					break;
				default:
					throw new IllegalArgumentException("Ungültige Anzahl Stop-Bits in Schnittstelleneinstellungen");
			}
			String paritySpec = _propertyConsultant.getProperty("seriell.parität").trim().toLowerCase();
			int parity;
			if(paritySpec.equals("gerade")) {
				parity = SerialPort.PARITY_EVEN;
			}
			else if(paritySpec.equals("ungerade")) {
				parity = SerialPort.PARITY_ODD;
			}
			else if(paritySpec.equals("gesetzt")) {
				parity = SerialPort.PARITY_MARK;
			}
			else if(paritySpec.equals("gelöscht")) {
				parity = SerialPort.PARITY_SPACE;
			}
			else if(paritySpec.equals("keine")) {
				parity = SerialPort.PARITY_NONE;
			}
			else {
				throw new IllegalArgumentException("Ungültige Parität " + paritySpec + " gültig sind (gerade|ungerade|gesetzt|gelöscht|keine)");
			}

			_byteTransmitDurationMicros = (10 + databitsValue * 10 + stopbitsValue + (parity != SerialPort.PARITY_NONE ? 10 : 0)) * 100000 / bps;
			_debug.finer("_byteTransmitDurationMicros: " + _byteTransmitDurationMicros + " Mikrosekunden");
			_flushAfterSend = _propertyConsultant.getBooleanProperty("seriell.empfangsPufferNachVersandLöschen");
			_waitWhilePendingOutput = _propertyConsultant.getBooleanProperty("seriell.aufVersandRückmeldungWarten");

			_tap = _propertyConsultant.getIntProperty("primary.Tap");
			_interCharacterTimeout = _propertyConsultant.getIntProperty("seriell.empfangsTimeout");

			_port.setSerialPortParams(bps, databitsMode, stopbitsMode, parity);
			_port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
			_port.setInputBufferSize(1000);
			_port.setOutputBufferSize(1000);
			_port.addEventListener(this);
			_port.notifyOnBreakInterrupt(true);
			_port.notifyOnCarrierDetect(false);
			_port.notifyOnCTS(true);
			_port.notifyOnDataAvailable(true);
			if(_checkDsr) _port.notifyOnDSR(true);
			_port.notifyOnFramingError(true);
			_port.notifyOnOutputEmpty(true);
			_port.notifyOnOverrunError(true);
			_port.notifyOnParityError(_propertyConsultant.getBooleanProperty("seriell.paritätPrüfen"));
			_port.notifyOnOverrunError(_propertyConsultant.getBooleanProperty("seriell.überlaufPrüfen"));
			_port.notifyOnRingIndicator(true);
			_out = _port.getOutputStream();
			_in = _port.getInputStream();
			_receiveBuffer = new byte[_propertyConsultant.getIntProperty("seriell.empfangsPuffer")];
			//_inChannel= Channels.newChannel(_in);
			_port.setDTR(true);
			Thread.sleep(200);
			if(_checkDsr) {
				_debug.finer("waiting for DSR");
				if(!_port.isDSR()) {
					_debug.warning("Kabel am Anschluß " + _portName + " NICHT angeschlossen!");
					while(!_port.isDSR()) wait(100);
					_debug.info("Kabel am Anschluß " + _portName + " ist jetzt angeschlossen.");
				}
			}
			_debug.finer("serial port started");
			_initialized = true;
		}
		catch(Exception e) {
			shutdown();
			throw e;
		}
	}

	public synchronized void shutdown() {
		if(_port != null) {
			_port.close();
			_port = null;
			_propertyConsultant = null;
			_in = null;
			_out = null;
			_receiveBuffer = null;
		}
		try {
			Thread.sleep(1000);
		}
		catch(InterruptedException ignored) {
		}
	}

	static String getEventTypeName(int eventType) {
		switch(eventType) {
			case SerialPortEvent.BI:
				return "Break Interrupt";
			case SerialPortEvent.CD:
				return "Carrier detect";
			case SerialPortEvent.CTS:
				return "Clear to send";
			case SerialPortEvent.DATA_AVAILABLE:
				return "Data available";
			case SerialPortEvent.DSR:
				return "Data set ready";
			case SerialPortEvent.FE:
				return "Framing Error";
			case SerialPortEvent.OE:
				return "Overrun Error";
			case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
				return "Output buffer empty";
			case SerialPortEvent.PE:
				return "Parity error";
			case SerialPortEvent.RI:
				return "Ring indicator";
			default:
				return "unbekannt";
		}
	}

	private synchronized void sendPacket(int waitTimeSinceLastReceive, byte[] packet) throws InterruptedException, IOException {
		try {
			// Warten, bis DCD-Signal abgefallen ist, damit nicht gesendet wird, solange die Gegenstelle noch am Senden ist
			if(_waitForDcdDown) {
				_debug.finer("waiting for DCD off");
				long timeout = System.currentTimeMillis() + 5000;
				while(true) {
					long waitTime = timeout - System.currentTimeMillis();
					if(_checkDsr && !_port.isDSR()) throw new IllegalStateException("Kabel auf Anschluß " + _portName + " wurde abgezogen!");
					if(!_port.isCD()) break;
					if(waitTime <= 0) throw new IllegalStateException("Timeout beim Warten auf Rücksetzen des DCD-Signals vor Versand auf Anschluß " + _portName);
					wait(waitTime);
				}
			}
			// Wartezeit Tw seit dem Empfang des letzten Bytes einhalten
			final long initialWaitTime = _lastReadTime + waitTimeSinceLastReceive - System.currentTimeMillis();
			if(initialWaitTime > 0) {
				_debug.finer("waiting before send: " + initialWaitTime);
				Thread.sleep(initialWaitTime);
			}
			if(_rtsOnSend) {
				_debug.finer("setting rts");
				_port.setRTS(true);
				_debug.finer("pre send delay");
				if(_rtsPreSendDelay > 0) Thread.sleep(_rtsPreSendDelay);
			}
			if(_waitForCts) {
				_debug.finer("waiting for cts");
				long timeout = System.currentTimeMillis() + 1000;
				while(true) {
					long waitTime = timeout - System.currentTimeMillis();
					if(_checkDsr && !_port.isDSR()) throw new IllegalStateException("Kabel auf Anschluß " + _portName + " wurde abgezogen!");
					if(_port.isCTS()) break;
					if(waitTime <= 0) throw new IllegalStateException("CTS-Signal-Timeout auf Anschluß " + _portName);
					wait(waitTime);
				}
			}
			int available = _in.available();
			if(available > 0) {
				_debug.info("skipping " + available + " bytes before send");
				_in.skip(available);
			}
			_parityError = false;
			_overrunError = false;
			_debug.finer(System.currentTimeMillis() + " sending packet ");
			long startSend = System.currentTimeMillis();
			_pendingOutput = true;
			_out.write(packet);

			if(_waitWhilePendingOutput) {
				_debug.finer(System.currentTimeMillis() + " waiting for pending output...");
				long timeout = System.currentTimeMillis() + ((packet.length * _byteTransmitDurationMicros) / 1000) + 500;
				while(true) {
					long waitTime = timeout - System.currentTimeMillis();
					if(_checkDsr && !_port.isDSR()) throw new IllegalStateException("Kabel auf Anschluß " + _portName + " wurde abgezogen!");
					if(!_pendingOutput) break;
					if(waitTime <= 0) throw new IllegalStateException("Timeout beim Warten auf abgeschlossenen Versand: " + _portName);
					wait(waitTime);
				}
				_debug.finer(System.currentTimeMillis() + " waiting for pending output done");
			}
			if(_rtsOnSend) {
				_debug.finer("post send delay");
				long stopSend = startSend + ((packet.length * _byteTransmitDurationMicros) / 1000);
				_debug.finer("start: " + startSend);
				_debug.finer("now:   " + System.currentTimeMillis());
				_debug.finer("stop:  " + stopSend);
				while(true) {
					long waitTime = stopSend - System.currentTimeMillis();
					if(waitTime > 0) {
						if(_checkDsr && !_port.isDSR()) throw new IllegalStateException("Kabel auf Anschluß " + _portName + " wurde abgezogen!");
						wait(waitTime);
					}
					else {
						break;
					}
				}
				_debug.finer("now:   " + System.currentTimeMillis());
				_debug.finer("post send delay");
				if(_rtsPostSendDelay > 0) Thread.sleep(_rtsPostSendDelay);
				_debug.finer("clearing rts");
				_port.setRTS(false);
			}
		}
		finally {
			if(_rtsOnSend) {
				_port.setRTS(false);
			}
		}
		_lastSendTime = System.currentTimeMillis();
		prepareReceive();
	}

	private synchronized void prepareReceive() throws IOException {
		_receiving = true;
		_receivedCount = 0;
		int available = _in.available();
		//_inputBuffer= ByteBuffer.allocate(1000);

		if(_flushAfterSend) {
			if(available > 0) {
				//_inChannel.read(_inputBuffer);
				//_inputBuffer.mark();
				_debug.info("skipping " + available + " bytes after send");
				_in.skip(available);
			}
			_parityError = false;
			_overrunError = false;
		}
		_lastReadTime = 0;
	}

	private synchronized byte[] receivePacket(ByteBuffer buffer, int timeout) throws InterruptedException {
//		long abortTime= 0;
//		boolean withTimeout= false;
//		if(timeout>0) {
//			withTimeout= true;
//			abortTime= _lastSendTime + timeout;
//		}
//		while(_receiving) {
//
//			if(withTimeout) {
//				long now= System.currentTimeMillis();
//				long waitTime= abortTime - now;
//				if(waitTime<=0) _receiving= false;
//				else {
//					if((_lastReadTime> 0) && (_interReadTimeout > 0)) {
//						if((_lastReadTime + _interReadTimeout < now) && (_in.available()<=0)) _receiving= false;
//					wait(waitTime);
//						throw UnsupportedOperationException();
//					}
//
//				}
//				else wait();
//				throw UnsupportedOperationException();
//			}
//		}
//		if(withTimeout) {
//				//wait(
//			throw UnsupportedOperationException();
//		}
////		if(_waitForDCD) {
////			_debug.finer("waiting for dcd");
////			while(!_port.isCD()) {
////				if(_pendingInput>0) {
////					_debug.finer("skipping " + _pendingInput);
////					_in.skip(_pendingInput);
////					_pendingInput= 0;
////				}
////				if(withTimeout) {
////					long waitTime= abortTime - System.currentTimeMillis();
////					if(waitTime<=0) return null;
////					wait(waitTime);
////				}
////				else wait();
////			}
////		}
////		while(true) {
////			if(_pendingInput>0) {
////				_in.read(_pendingInput);
////				_pendingInput= 0;
////			}
////
////			int available= _in.available();
////			if(available>0
////		}

		return null;
	}

	public synchronized void serialEvent(SerialPortEvent serialPortEvent) {
		try {
			if(_receiving) {
				_debug.finest(
						"receiving on " + serialPortEvent.toString() + " " + getEventTypeName(serialPortEvent.getEventType()) + " "
						+ serialPortEvent.getNewValue() + " (" + serialPortEvent.getOldValue() + ")"
				);
				if(serialPortEvent.getEventType() == SerialPortEvent.PE) {
					_parityError = true;
					//_inputBuffer.mark();
					_receiving = false;
					notifyAll();
				}
				else if(serialPortEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
					int available = _in.available();
					if(available > 0) {
						//_inChannel.read(_inputBuffer);
						int readCount = _in.read(_receiveBuffer, _receivedCount, available);
						if(readCount < 0) {
							_receiving = false;
							notifyAll();
						}
						else if(readCount > 0) {
							_receivedCount += readCount;
							if(_lastReadTime == 0) notifyAll();
							_lastReadTime = System.currentTimeMillis();
						}
					}
				}
				else if(serialPortEvent.getEventType() == SerialPortEvent.CD) {
					int available = _in.available();
					if(available > 0) {
						//_inChannel.read(_inputBuffer);
						int readCount = _in.read(_receiveBuffer, _receivedCount, available);
						if(readCount < 0) {
							_receiving = false;
							notifyAll();
						}
						else if(readCount > 0) {
							_receivedCount += readCount;
							if(_lastReadTime == 0) notifyAll();
							_lastReadTime = System.currentTimeMillis();
						}
					}
					if(serialPortEvent.getNewValue()) {
						_debug.fine("DCD hat Eingangspuffer gelöscht");
						//_receivedCount= 0;
					}
					else {
						_debug.fine("DCD hat Empfang beendet");
						_receiving = false;
						notifyAll();
					}
				}
				else if(serialPortEvent.getEventType() == SerialPortEvent.DSR) {
					if(serialPortEvent.getNewValue()) {
						_debug.info("Kabel auf Anschluß " + _portName + " wurde während dem Datenempfang abgezogen!");
						_receiving = false;
						notifyAll();
					}
				}
				else if(serialPortEvent.getEventType() == SerialPortEvent.OE) {
					_overrunError = true;
					_receiving = false;
					notifyAll();
				}
			}
			else {
				_debug.finest(
						serialPortEvent.toString() + " " + getEventTypeName(serialPortEvent.getEventType()) + " " + serialPortEvent.getNewValue() + " ("
						+ serialPortEvent.getOldValue() + ")"
				);
				if(serialPortEvent.getEventType() == SerialPortEvent.OUTPUT_BUFFER_EMPTY) _pendingOutput = false;
				notifyAll();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public byte[] query(int waitTimeSinceLastReceive, byte[] sendBytes, int receiveTimeout) throws InterruptedException, IOException {
		if(!_initialized) {
			String message = "Anschluß " + _portName + " ist nicht korrekt initialisiert.";
			_debug.info(message);
			throw new IllegalStateException(message);
		}
		sendPacket(waitTimeSinceLastReceive, sendBytes);
		return readPacket(receiveTimeout);
	}

	private synchronized byte[] readPacket(int timeout) throws InterruptedException, IOException {
		try {
			long firstCharacterTimeoutTime = _lastSendTime + timeout;
			while(_receiving) {
				long now = System.currentTimeMillis();
				long waitTime;
				if(_lastReadTime == 0) {
					waitTime = firstCharacterTimeoutTime - now;
					if(waitTime <= 0) {
						if(timeout > 0) _debug.info("Antwortüberwachungszeit abgelaufen");
						_receiving = false;
						return null;
					}
				}
				else {
					waitTime = _lastReadTime + _interCharacterTimeout - now;
					if(waitTime <= 0) {
						if(_in.available() > 0) waitTime = _interCharacterTimeout;
						_debug.finer("InterCharacterTimeout abgelaufen");
						_receiving = false;
						break;
					}
				}
				_debug.finer("waiting: " + waitTime);
				wait(waitTime);
			}
			if(_checkDsr && !_port.isDSR()) throw new IllegalStateException("Kabel auf Anschluß " + _portName + " wurde abgezogen!");
			if(_parityError) {
				_debug.info("Paritäts Fehler");
				return null;
			}
			if(_overrunError) {
				_debug.info("Überlauf Fehler");
				return null;
			}
			byte[] result = new byte[_receivedCount];
			System.arraycopy(_receiveBuffer, 0, result, 0, result.length);
			return result;
		}
		finally {
			_receiving = false;
		}
	}

	public synchronized byte[] readBytes(long timeout) throws InterruptedException {
		if(!_receiving) {
			_parityError = false;
			_overrunError = false;
			_receiving = true;
			_receivedCount = 0;
		}
		long firstCharacterTimeoutTime = System.currentTimeMillis() + timeout;
		int halfReceiveBufferSize = _receiveBuffer.length / 2;
		while(_receiving && _receivedCount < halfReceiveBufferSize) {
			long now = System.currentTimeMillis();
			long waitTime;
			if(_receivedCount == 0) {
				waitTime = firstCharacterTimeoutTime - now;
				if(waitTime <= 0) {
					_debug.finer("keine Daten empfangen");
					return new byte[0];
				}
			}
			else {
				waitTime = _lastReadTime + _interCharacterTimeout - now;
				if(waitTime <= 0) {
					_debug.finer("InterCharacterTimeout abgelaufen");
					break;
				}
			}
			_debug.finer("waiting: " + waitTime);
			wait(waitTime);
		}
		if(_checkDsr && !_port.isDSR()) {
			_debug.warning("Kabel auf Anschluß " + _portName + " wurde abgezogen!");
			return null;
		}
		if(_parityError) {
			_debug.warning("Paritäts Fehler");
			return null;
		}
		if(_overrunError) {
			_debug.warning("Überlauf Fehler");
			return null;
		}
		if(_receivedCount == 0) {
			_debug.warning("Fehler beim Empfang");
			return null;
		}
		byte[] result = new byte[_receivedCount];
		System.arraycopy(_receiveBuffer, 0, result, 0, result.length);
		_receivedCount = 0;
		return result;
	}
}
