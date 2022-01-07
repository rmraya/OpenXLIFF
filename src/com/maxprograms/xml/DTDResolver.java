/*******************************************************************************
 * Copyright (c) 2022 Maxprograms.
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
import java.net.URI;
import java.net.URISyntaxException;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public class DTDResolver implements EntityResolver {

	@SuppressWarnings("resource")
	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws IOException {
		try {
			URI u = new URI("").resolve(systemId).normalize();
			File file = new File(u.toURL().toString());
			if (file.exists()) {
				return new InputSource(new FileInputStream(file));
			}
		} catch (URISyntaxException e) {
			// ignore
		}
		return null;
	}

}
