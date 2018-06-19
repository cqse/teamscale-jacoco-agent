/*******************************************************************************
 * Copyright (c) 2009, 2016 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Brock Janiczak -initial API and implementation
 *    
 *******************************************************************************/
package eu.cqse.teamscale.jacoco.report;

import org.jacoco.report.internal.xml.XMLDocument;
import org.jacoco.report.internal.xml.XMLElement;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Report formatter that creates a single XML file for a coverage session
 */
public class SessionAwareXMLFormatter {

	private static final String SYSTEM = "testwise-coverage.dtd";

	/**
	 * Creates a new visitor to write a report to the given stream.
	 * 
	 * @param output
	 *            output stream to write the report to
	 * @return visitor to emit the report data to
	 * @throws IOException
	 *             in case of problems with the output stream
	 */
	public SessionAwareVisitor createVisitor(final OutputStream output)  throws IOException {
		final XMLElement root = new XMLDocument("report", null, SYSTEM,
				"UTF-8", true, output);
		return new SessionAwareVisitor(root);
	}
}
