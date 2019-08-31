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
package com.maxprograms.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;

public class Catalog implements EntityResolver2 {

	private Hashtable<String, String> systemCatalog;
	private Hashtable<String, String> publicCatalog;
	private Hashtable<String, String> uriCatalog;
	private Vector<String[]> uriRewrites;
	private Vector<String[]> systemRewrites;
	private String workDir;
	private String base = "";

	public Catalog(String catalogFile)
			throws SAXException, IOException, ParserConfigurationException, URISyntaxException {
		File file = new File(catalogFile);
		if (!file.isAbsolute()) {
			String absolute = XMLUtils.getAbsolutePath(System.getProperty("user.dir"), catalogFile);
			file = new File(absolute);
		}
		workDir = file.getParent();
		if (!workDir.endsWith(File.separator)) {
			workDir = workDir + File.separator;
		}
		String[] catalogs = new String[1];
		catalogs[0] = file.toURI().toURL().toString();

		systemCatalog = new Hashtable<>();
		publicCatalog = new Hashtable<>();
		uriCatalog = new Hashtable<>();
		uriRewrites = new Vector<>();
		systemRewrites = new Vector<>();

		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(catalogFile);
		Element root = doc.getRootElement();
		recurse(root);
	}

	private void recurse(Element root)
			throws SAXException, IOException, ParserConfigurationException, URISyntaxException {
		List<Element> children = root.getChildren();
		Iterator<Element> i = children.iterator();
		while (i.hasNext()) {
			Element child = i.next();
			String currentBase = base;

			if (!child.getAttributeValue("xml:base", "").equals("")) {
				base = child.getAttributeValue("xml:base");
			}

			if (child.getName().equals("system") && !systemCatalog.containsKey(child.getAttributeValue("systemId"))) {
				String uri = makeAbsolute(child.getAttributeValue("uri"));
				if (validate(uri)) {
					systemCatalog.put(child.getAttributeValue("systemId"), uri);
				}
			}
			if (child.getName().equals("public")) {
				String publicId = child.getAttributeValue("publicId");
				if (publicId.startsWith("urn:publicid:")) {
					publicId = unwrapUrn(publicId);
				}
				if (!publicCatalog.containsKey(publicId)) {
					String uri = makeAbsolute(child.getAttributeValue("uri"));
					if (validate(uri)) {
						publicCatalog.put(publicId, uri);
					}
				}
			}
			if (child.getName().equals("uri") && !uriCatalog.containsKey(child.getAttributeValue("name"))) {
				String uri = makeAbsolute(child.getAttributeValue("uri"));
				if (validate(uri)) {
					uriCatalog.put(child.getAttributeValue("name"), uri);
				}
			}
			if (child.getName().equals("nextCatalog")) {
				String nextCatalog = child.getAttributeValue("catalog");
				File f = new File(nextCatalog);
				if (!f.isAbsolute()) {
					nextCatalog = XMLUtils.getAbsolutePath(workDir, nextCatalog);
				}
				Catalog cat = new Catalog(nextCatalog);
				Hashtable<String, String> table = cat.getSystemCatalogue();
				Iterator<String> it = table.keySet().iterator();
				while (it.hasNext()) {
					String key = it.next();
					if (!systemCatalog.containsKey(key)) {
						String value = table.get(key);
						systemCatalog.put(key, value);
					}
				}
				table = cat.getPublicCatalogue();
				it = table.keySet().iterator();
				while (it.hasNext()) {
					String key = it.next();
					if (!publicCatalog.containsKey(key)) {
						String value = table.get(key);
						publicCatalog.put(key, value);
					}
				}
				table = cat.getUriCatalogue();
				it = table.keySet().iterator();
				while (it.hasNext()) {
					String key = it.next();
					if (!uriCatalog.containsKey(key)) {
						String value = table.get(key);
						uriCatalog.put(key, value);
					}
				}
				Vector<String[]> system = cat.getSystemRewrites();
				for (int h = 0; h < system.size(); h++) {
					String[] pair = system.get(h);
					if (!systemRewrites.contains(pair)) {
						systemRewrites.add(pair);
					}
				}
				Vector<String[]> uris = cat.getUriRewrites();
				for (int h = 0; h < uris.size(); h++) {
					String[] pair = uris.get(h);
					if (!uriRewrites.contains(pair)) {
						uriRewrites.add(pair);
					}
				}
			}
			if (child.getName().equals("rewriteSystem")) {
				String uri = makeAbsolute(child.getAttributeValue("rewritePrefix"));
				String[] pair = new String[] { child.getAttributeValue("systemIdStartString"), uri };
				if (!systemRewrites.contains(pair)) {
					systemRewrites.add(pair);
				}
			}
			if (child.getName().equals("rewriteURI")) {
				String uri = makeAbsolute(child.getAttributeValue("rewritePrefix"));
				String[] pair = new String[] { child.getAttributeValue("uriStartString"), uri };
				if (!uriRewrites.contains(pair)) {
					uriRewrites.add(pair);
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

	private String makeAbsolute(String uri) throws URISyntaxException {
		URI b = new URI(base);
		URI u = b.resolve(uri).normalize();
		if (!u.isAbsolute()) {
			URI work = new URI(workDir);
			if (!base.isEmpty()) {
				work = work.resolve(base);
			}
			u = work.resolve(uri).normalize();
		}
		return u.toString();
	}

	private Hashtable<String, String> getSystemCatalogue() {
		return systemCatalog;
	}

	private Hashtable<String, String> getPublicCatalogue() {
		return publicCatalog;
	}

	private Hashtable<String, String> getUriCatalogue() {
		return uriCatalog;
	}

	private Vector<String[]> getSystemRewrites() {
		return systemRewrites;
	}

	private Vector<String[]> getUriRewrites() {
		return uriRewrites;
	}

	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		if (publicId != null) {
			String location = matchPublic(publicId);
			if (location != null) {
				return new InputSource(new FileInputStream(location));
			}
		}
		String location = matchSystem(null, systemId);
		if (location != null) {
			return new InputSource(new FileInputStream(location));
		}
		return null;
	}

	private static String unwrapUrn(String urn) {
		if (!urn.startsWith("urn:publicid:")) {
			return urn;
		}
		String publicId = urn.trim().substring("urn:publicid:".length());
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

	@Override
	public InputSource getExternalSubset(String name, String baseURI) throws SAXException, IOException {
		return null;
	}

	@Override
	public InputSource resolveEntity(String name, String publicId, String baseURI, String systemId)
			throws SAXException, IOException {
		if (publicId != null) {
			String location = matchPublic(publicId);
			if (location != null) {
				return new InputSource(new FileInputStream(location));
			}
		}
		String location = matchSystem(baseURI, systemId);
		if (location != null) {
			return new InputSource(new FileInputStream(location));
		}

		// This DTD is not in the catalog,
		// try to find it in the URL reported
		// by the document
		try {
			URI uri = new URI(baseURI != null ? baseURI : "").resolve(systemId).normalize();
			if (uri.toURL().getProtocol() != null) {
				return new InputSource(uri.toURL().openStream());
			}
			return new InputSource(new FileInputStream(uri.toURL().toString()));
		} catch (IOException | URISyntaxException e) {
			// ignore
		}
		return null;
	}

	public String matchPublic(String publicId) {
		if (publicId != null) {
			if (publicId.startsWith("urn:publicid:")) {
				publicId = unwrapUrn(publicId);
			}
			if (publicCatalog.containsKey(publicId)) {
				return publicCatalog.get(publicId);
			}
		}
		return null;
	}

	public String matchSystem(String baseURI, String systemId) {
		if (systemId != null) {
			for (int i = 0; i < systemRewrites.size(); i++) {
				String[] pair = systemRewrites.get(i);
				if (systemId.startsWith(pair[0])) {
					systemId = pair[1] + systemId.substring(pair[0].length());
				}
			}
			if (systemCatalog.containsKey(systemId)) {
				return systemCatalog.get(systemId);
			}
			// this resource is not in catalog.
			try {
				URI u = new URI(baseURI != null ? baseURI : "").resolve(systemId).normalize();
				File file = new File(u.toURL().toString());
				if (file.exists()) {
					return file.getAbsolutePath();
				}
			} catch (MalformedURLException | URISyntaxException e) {
				// ignore
			}
		}
		return null;
	}

	public String matchURI(String uri) {
		if (uri != null) {
			for (int i = 0; i < uriRewrites.size(); i++) {
				String[] pair = uriRewrites.get(i);
				if (uri.startsWith(pair[0])) {
					uri = pair[1] + uri.substring(pair[0].length());
				}
			}
			if (uriCatalog.containsKey(uri)) {
				return uriCatalog.get(uri);
			}
			try {
				URI u = new URI(uri).normalize();
				return u.toString();
			} catch (URISyntaxException e) {
				// ignore
			}
		}
		return null;
	}

}
