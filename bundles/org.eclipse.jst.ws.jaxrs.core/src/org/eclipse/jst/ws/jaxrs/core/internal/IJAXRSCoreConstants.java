/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 * yyyymmdd bug      Email and other contact information
 * -------- -------- -----------------------------------------------------------
 * 20091013   291954 ericdp@ca.ibm.com - Eric D. Peters, JAX-RS: Implement JAX-RS Facet
 *******************************************************************************/
package org.eclipse.jst.ws.jaxrs.core.internal;

/**
 * JAXRS Core framework constants
 * 
 * <p>
 * <b>Provisional API - subject to change</b>
 * </p>
 * 
 * 
 */
public final class IJAXRSCoreConstants {
	/**
	 * The global id for the JAXRS facet 
	 */
	public static final String JAXRS_FACET_ID = "jst.jaxrs"; //$NON-NLS-1$
	/**
	 * The facet version for a JAX-RS 1.0 project 
	 */
	public final static String FACET_VERSION_1_0 = "1.0"; //$NON-NLS-1$
	/**
	 * The constant id for a JAXRS 1.0 project
	 */
	public final static String JAXRS_VERSION_1_0 = FACET_VERSION_1_0;

	private IJAXRSCoreConstants() {
		// no instantiation
	}
}