/*
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2004 by Kappich+Kniß Systemberatung, Aachen
 * 
 * This file is part of de.bsvrz.kex.tls.osi2osi3.
 * 
 * de.bsvrz.kex.tls.osi2osi3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * de.bsvrz.kex.tls.osi2osi3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.bsvrz.kex.tls.osi2osi3.  If not, see <http://www.gnu.org/licenses/>.

 * Contact Information:
 * Kappich Systemberatung
 * Martin-Luther-Straße 14
 * 52062 Aachen, Germany
 * phone: +49 241 4090 436 
 * mail: <info@kappich.de>
 */

package de.bsvrz.kex.tls.osi2osi3.osi2.tc57primary;

import de.bsvrz.kex.tls.osi2osi3.osi2.api.AbstractDataLinkLayer;

/**
 * Abstrakte Klasse mit Default-Implementierungen für TC57-basierte OSI-2 Module.
 *
 * @author Kappich Systemberatung
 * @version $Revision$
 */
public abstract class AbstractTc57 extends AbstractDataLinkLayer {

	public AbstractTc57() {
	}

	protected String getDefaultProperty(String name) {
		return DefaultProperties.getInstance().getProperty(name);
	}

	public int getMaximumDataSize() {
		return 253;
	}
}
