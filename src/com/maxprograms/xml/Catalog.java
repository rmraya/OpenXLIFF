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
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;

public class Catalog implements EntityResolver2 {

	private Hashtable<String, Object> catalog;
	private String workDir;
	private String publicId;
	private String systemId;
	private String base = "";
	private CustomContentHandler contentHandler;

	public Catalog(String catalogFile) throws SAXException, IOException, ParserConfigurationException {
		File file = new File(catalogFile);
		if (!file.isAbsolute()) {
			catalogFile = XMLUtils.getAbsolutePath(System.getProperty("user.dir"), catalogFile);
			file = new File(catalogFile);
		}
		workDir = file.getParent();
		if (!workDir.endsWith("\\") && !workDir.endsWith("/")) {
			workDir = workDir + System.getProperty("file.separator");
		}
		String[] catalogs = new String[1];
		catalogs[0] = file.toURI().toURL().toString();

		catalog = new Hashtable<>();
		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(catalogFile);
		Element root = doc.getRootElement();
		recurse(root);
	}

	private void recurse(Element root) throws SAXException, IOException, ParserConfigurationException {
		List<Element> children = root.getChildren();
		Iterator<Element> i = children.iterator();
		while (i.hasNext()) {
			Element child = i.next();
			String currentBase = base;

			if (!child.getAttributeValue("xml:base", "").equals("")) {
				base = child.getAttributeValue("xml:base");
			}

			if (child.getName().equals("system") && !catalog.containsKey(child.getAttributeValue("systemId"))) {
				String uri = makeAbsolute(base + child.getAttributeValue("uri"));
				if (validate(uri)) {
					catalog.put(child.getAttributeValue("systemId"), uri);
				}
			}
			if (child.getName().equals("public")) {
				String publicId1 = child.getAttributeValue("publicId");
				if (publicId1.startsWith("urn:publicid:")) {
					publicId1 = unwrapUrn(publicId1);
				}
				if (!catalog.containsKey(publicId1)) {
					String uri = makeAbsolute(base + child.getAttributeValue("uri"));
					if (validate(uri)) {
						catalog.put(publicId1, uri);
					}
				}
			}
			if (child.getName().equals("uri") && !catalog.containsKey(child.getAttributeValue("name"))) {
				String uri = makeAbsolute(base + child.getAttributeValue("uri"));
				if (validate(uri)) {
					catalog.put(child.getAttributeValue("name"), uri);
				}
			}
			if (child.getName().equals("nextCatalog")) {
				String nextCatalog = child.getAttributeValue("catalog");
				File f = new File(nextCatalog);
				if (!f.isAbsolute()) {
					nextCatalog = XMLUtils.getAbsolutePath(workDir, nextCatalog);
				}
				Catalog cat = new Catalog(nextCatalog);
				Hashtable<String, Object> table = cat.getCatalogue();
				Enumeration<String> keys = table.keys();
				while (keys.hasMoreElements()) {
					String key = keys.nextElement();
					if (!catalog.containsKey(key)) {
						Object value = table.get(key);
						catalog.put(key, value);
					}
				}
			}
			recurse(child);
			base = currentBase;
		}
	}

	private static boolean validate(String uri) {
		File file = new File(uri);
		return file.exists();
	}

	private String makeAbsolute(String file) throws IOException {
		File f = new File(file);
		if (!f.isAbsolute()) {
			file = XMLUtils.getAbsolutePath(workDir, file);
		}
		return file;
	}

	private Hashtable<String, Object> getCatalogue() {
		return catalog;
	}

	@SuppressWarnings("resource")
	@Override
	public InputSource resolveEntity(String publicId1, String systemId1) throws SAXException, IOException {
		this.publicId = publicId1;
		this.systemId = systemId1;

		if (publicId == null && systemId == null) {
			return null;
		}

		if (publicId != null) {
			if (publicId.startsWith("urn:publicid:")) {
				publicId = unwrapUrn(publicId);
			}
			if (catalog.containsKey(publicId)) {
				String location = (String) catalog.get(publicId);
				InputStream input = new FileInputStream(location);
				return new InputSource(input);
			}
		}

		if (systemId != null) {
			if (catalog.containsKey(systemId)) {
				String location = (String) catalog.get(systemId);
				InputStream input = new FileInputStream(location);
				return new InputSource(input);
			}

			Enumeration<String> keys = catalog.keys();
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();
				File f = new File((String) catalog.get(key));
				String location = f.getName();
				if (systemId.endsWith(location)) {
					InputStream input = new FileInputStream((String) catalog.get(key));
					return new InputSource(input);
				}
			}

			// This DTD is not in the catalog,
			// try to find it in the URL reported
			// by the document
			try {
				URL url = new URL(systemId);
				InputStream input = url.openStream();
				return new InputSource(input);
			} catch (Exception e) {
				MessageFormat mf = new MessageFormat("Cannot resolve ''{0}''.");
				throw new IOException(mf.format(new Object[] { systemId }));
			}
		}
		return null;
	}

	private String unwrapUrn(String urn) {
		if (!urn.startsWith("urn:publicid:")) {
			return urn;
		}
		publicId = urn.trim().substring("urn:publicid:".length());
		publicId = publicId.replaceAll("\\+", " ");
		publicId = publicId.replaceAll("\\:", "//");
		publicId = publicId.replaceAll(";", "::");
		publicId = publicId.replaceAll("%2B", "+");
		publicId = publicId.replaceAll("%3A", ":");
		publicId = publicId.replaceAll("%2F", "/");
		publicId = publicId.replaceAll("%3B", ";");
		publicId = publicId.replaceAll("%27", "'");
		publicId = publicId.replaceAll("%3F", "?");
		publicId = publicId.replaceAll("%23", "#");
		publicId = publicId.replaceAll("%25", "%");
		return publicId;
	}

	public File getDTD(String publicId1, String systemId1) throws IOException {
		this.publicId = publicId1;
		this.systemId = systemId1;

		if (publicId == null && systemId == null) {
			return null;
		}

		if (publicId != null) {
			if (publicId.startsWith("urn:publicid:")) {
				publicId = unwrapUrn(publicId);
			}
			if (catalog.containsKey(publicId)) {
				return new File((String) catalog.get(publicId));
			}
		}
		if (systemId != null) {
			if (catalog.containsKey(systemId)) {
				return new File((String) catalog.get(systemId));
			}

			if (catalog.containsKey(systemId)) {
				String location = (String) catalog.get(systemId);
				return new File(location);
			}

			Enumeration<String> keys = catalog.keys();
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();
				String location = key;
				if (location.indexOf('\\') != -1) {
					location = location.substring(location.lastIndexOf('\\'));
				}
				if (location.indexOf('/') != -1) {
					location = location.substring(location.lastIndexOf('/'));
				}
				if (systemId.endsWith(location)) {
					return new File((String) catalog.get(key));
				}
			}

			// This DTD is not in the catalog,
			// try to find it in the URL reported
			// by the document
			try {
				URL url = new URL(systemId);
				File f = new File(url.toString());
				if (f.exists()) {
					return f;
				}
			} catch (Exception e) {
				MessageFormat mf = new MessageFormat("Cannot resolve ''{0}''.");
				throw new IOException(mf.format(new Object[] { systemId }));
			}
		}
		return null;
	}

	@Override
	public InputSource getExternalSubset(String name, String baseURI) throws SAXException, IOException {
		return null;
	}

	@SuppressWarnings("resource")
	@Override
	public InputSource resolveEntity(String name, String publicId1, String baseURI, String systemId1)
			throws SAXException, IOException {
		this.publicId = publicId1;
		this.systemId = systemId1;
		if (publicId != null) {
			if (publicId.startsWith("urn:publicid:")) {
				publicId = unwrapUrn(publicId);
			}
			if (catalog.containsKey(publicId)) {
				String location = (String) catalog.get(publicId);
				InputStream input = new FileInputStream(location);
				return new InputSource(input);
			}
		}
		if (systemId == null) {
			return null;
		}
		if (catalog.containsKey(systemId)) {
			String location = (String) catalog.get(systemId);
			InputStream input = new FileInputStream(location);
			return new InputSource(input);
		}

		// This DTD is not in the catalog,
		// try to find it in the URL reported
		// by the document
		try {
			URL url = new URL(systemId);
			InputStream input = url.openStream();
			return new InputSource(input);
		} catch (Exception e) {
			if (contentHandler != null) {
				String pub = contentHandler.getPublicID();
				if (pub != null && catalog.containsKey(pub)) {
					String parent = (String) catalog.get(pub);
					try {
						File f = new File(parent);
						File g = new File(XMLUtils.getAbsolutePath(f.getParent(), systemId));
						if (g.exists()) {
							InputStream input = new FileInputStream(g);
							return new InputSource(input);
						}
					} catch (Exception ex) {
						// do nothing here, catch below
					}
				} else if (publicId == null) {
					// Get the entity name from the
					// SYSTEM id and check the catalog
					String entity = systemId.replaceAll("\\\\", "/");
					if (entity.indexOf('/') != -1) {
						entity = entity.substring(entity.lastIndexOf('/') + 1);
					}
					if (catalog.containsKey(entity)) {
						InputStream input = new FileInputStream((String) catalog.get(entity));
						return new InputSource(input);
					}

					// try to find it checking file name
					Enumeration<String> keys = catalog.keys();
					while (keys.hasMoreElements()) {
						String key = keys.nextElement();
						File f = new File((String) catalog.get(key));
						String location = f.getName();
						if (systemId.endsWith(location)) {
							InputStream input = new FileInputStream((String) catalog.get(key));
							return new InputSource(input);
						}
					}
				}
			}
			MessageFormat mf = new MessageFormat("Cannot resolve ''{0}''.");
			throw new IOException(mf.format(new Object[] { systemId }));
		}
	}

	protected void setContentHandler(CustomContentHandler handler) {
		contentHandler = handler;
	}

}
