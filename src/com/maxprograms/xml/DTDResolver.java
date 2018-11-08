/*******************************************************************************
 * Copyright (c) 2003, 2018 Maxprograms.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DTDResolver implements EntityResolver {

	@SuppressWarnings("resource")
	@Override
	public InputSource resolveEntity(String publicId1, String systemId1) throws SAXException, IOException {
		URL url = new URL(systemId1);
		try {
			return new InputSource(new FileInputStream(new File(url.toURI())));
		} catch (URISyntaxException e) {
			throw new IOException(e.getMessage());
		}
	}

}
