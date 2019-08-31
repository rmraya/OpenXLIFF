/*******************************************************************************
 * Copyright (c) 2003-2019 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/

package com.maxprograms.validation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URISyntaxException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.XMLOutputter;

public class Xliff11 {

	private static Logger LOGGER = System.getLogger(Xliff11.class.getName());
	private String reason = "";

	public boolean validate(Document document, String catalog) {
		Element root = document.getRootElement();
		if (root.getAttributeValue("xmlns").isEmpty()) {
			root.setAttribute("xmlns", "urn:oasis:names:tc:xliff:document:1.1");
			root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			root.setAttribute("xsi:schemaLocation", "urn:oasis:names:tc:xliff:document:1.1 xliff-core-1.1.xsd");
			File temp;
			try {
				temp = File.createTempFile("temp", ".xlf");
				temp.deleteOnExit();
				XMLOutputter outputter = new XMLOutputter();
				try (FileOutputStream output = new FileOutputStream(temp)) {
					outputter.output(document, output);
				}
			} catch (IOException e) {
				LOGGER.log(Level.ERROR, e);
				reason = "Error adding namespace declaration";
				return false;
			}
			try {
				SAXBuilder builder = new SAXBuilder();
				builder.setValidating(true);
				builder.setEntityResolver(new Catalog(catalog));
				builder.build(temp);
			} catch (IOException | SAXException | ParserConfigurationException | URISyntaxException e) {
				LOGGER.log(Level.ERROR, e);
				reason = e.getMessage();
				return false;
			}
		}
		return true;
	}

	public String getReason() {
		return reason;
	}

}
