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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class SAXBuilder {

	private EntityResolver resolver = null;
	private ErrorHandler errorHandler = null;
	private boolean validating;
	private boolean preserveAttributes = false;
	private IContentHandler contentHandler;

	public SAXBuilder() {
		validating = false;
	}

	public SAXBuilder(boolean validating) {
		this.validating = validating;
	}

	public Document build(String filename) throws SAXException, IOException, ParserConfigurationException {
		return build(new File(filename).toURI().toURL());
	}

	public Document build(URI uri) throws SAXException, IOException, ParserConfigurationException {
		return build(uri.toURL());
	}

	public Document build(File file) throws SAXException, IOException, ParserConfigurationException {
		return build(file.toURI().toURL());
	}

	public Document build(ByteArrayInputStream stream) throws SAXException, IOException, ParserConfigurationException {
		XMLReader parser = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
		parser.setFeature("http://xml.org/sax/features/namespaces", true);
		if (validating) {
			parser.setFeature("http://xml.org/sax/features/validation", true);
			parser.setFeature("http://apache.org/xml/features/validation/schema", true);
			parser.setFeature("http://apache.org/xml/features/validation/dynamic", true);
		}
		if (contentHandler == null) {
			contentHandler = new CustomContentHandler();
		}
		parser.setContentHandler(contentHandler);
		if (resolver == null) {
			resolver = new DTDResolver();
		}
		parser.setEntityResolver(resolver);
		if (errorHandler != null) {
			parser.setErrorHandler(errorHandler);
		} else {
			parser.setErrorHandler(new CustomErrorHandler());
		}
		parser.setProperty("http://xml.org/sax/properties/lexical-handler", contentHandler);

		EntityHandler declhandler = new EntityHandler();
		parser.setProperty("http://xml.org/sax/properties/declaration-handler", declhandler);

		parser.parse(new InputSource(stream));
		Document doc = contentHandler.getDocument();

		Hashtable<String, String> entities = declhandler.getEntities();
		if (entities.size() > 0) {
			doc.setEntities(entities);
		}

		return doc;
	}

	public void setContentHandler(IContentHandler handler) {
		contentHandler = handler;
	}

	public void setEntityResolver(EntityResolver res) {
		resolver = res;
	}

	public void setErrorHandler(ErrorHandler handler) {
		errorHandler = handler;
	}

	public void setValidating(boolean value) {
		validating = value;
	}

	public Document build(URL url) throws SAXException, IOException, ParserConfigurationException {
		XMLReader parser = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
		parser.setFeature("http://xml.org/sax/features/namespaces", true);
		if (validating) {
			parser.setFeature("http://xml.org/sax/features/validation", true);
			parser.setFeature("http://apache.org/xml/features/validation/schema", true);
			parser.setFeature("http://apache.org/xml/features/validation/dynamic", true);
		}
		if (contentHandler == null) {
			contentHandler = new CustomContentHandler();
		}
		parser.setContentHandler(contentHandler);
		if (resolver == null) {
			resolver = new DTDResolver();
		}
		parser.setEntityResolver(resolver);
		if (errorHandler != null) {
			parser.setErrorHandler(errorHandler);
		} else {
			parser.setErrorHandler(new CustomErrorHandler());
		}
		parser.setProperty("http://xml.org/sax/properties/lexical-handler", contentHandler);
		parser.setFeature("http://xml.org/sax/features/namespaces", true);
		parser.setFeature("http://xml.org/sax/features/namespace-prefixes", true);

		EntityHandler declhandler = new EntityHandler();
		parser.setProperty("http://xml.org/sax/properties/declaration-handler", declhandler);

		parser.parse(new InputSource(url.openStream()));
		Document doc = contentHandler.getDocument();

		Hashtable<String, String> entities = declhandler.getEntities();
		if (entities.size() > 0) {
			doc.setEntities(entities);
		}
		Vector<String> attributes = declhandler.getAttributes();
		if (attributes != null && preserveAttributes && hasCustomAttributes(url, doc.getEncoding())) {
			doc.setAttributes(attributes);
		}
		return doc;
	}

	private static boolean hasCustomAttributes(URL url, Charset charset) throws IOException {
		byte[] array = new byte[2048];
		try (InputStream source = url.openStream()) {
			source.read(array, 0, 2048);
		}
		String string = new String(array, charset);
		return string.indexOf("<!ATTLIST") != -1;
	}

	public void preserveCustomAttributes(boolean value) {
		preserveAttributes = value;
	}

}
