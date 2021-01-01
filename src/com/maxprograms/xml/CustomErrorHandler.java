/*******************************************************************************
 * Copyright (c)  Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/
package com.maxprograms.xml;

import java.lang.System.Logger.Level;
import java.lang.System.Logger;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class CustomErrorHandler implements org.xml.sax.ErrorHandler {

	private static final Logger LOGGER = System.getLogger(CustomErrorHandler.class.getName());

	@Override
	public void warning(SAXParseException exception) throws SAXException {
		LOGGER.log(Level.WARNING,
				exception.getLineNumber() + ":" + exception.getColumnNumber() + " " + exception.getMessage());
	}

	@Override
	public void error(SAXParseException exception) throws SAXException {
		LOGGER.log(Level.ERROR,
				exception.getLineNumber() + ":" + exception.getColumnNumber() + " " + exception.getMessage());
		throw new SAXException("[Error] " + exception.getLineNumber() + ":" + exception.getColumnNumber() + " "
				+ exception.getMessage());
	}

	@Override
	public void fatalError(SAXParseException exception) throws SAXException {
		LOGGER.log(Level.ERROR,
				exception.getLineNumber() + ":" + exception.getColumnNumber() + " " + exception.getMessage());
		throw new SAXException("[Fatal Error] " + exception.getLineNumber() + ":" + exception.getColumnNumber() + " "
				+ exception.getMessage());
	}

}
