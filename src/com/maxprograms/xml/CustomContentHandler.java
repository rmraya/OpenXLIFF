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

import java.io.IOException;
import java.util.EmptyStackException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

class CustomContentHandler implements IContentHandler {

	private Document doc;
	private Element current;
	Stack<Element> stack;
	private List<XMLNode> prolog;
	private boolean inDocument;
	private boolean inCDATA = false;
	private StringBuffer cdata;
	private List<String[]> namespaces;
	private Map<String, String> namespacesInUse;
	private Map<String, String> pendingNamespaces;
	private String systemId;
	private String encoding;
	private Catalog catalog;
	private boolean isRelaxNG;
	private Map<String, Map<String, String>> defaultAttributes;

	public CustomContentHandler() {
		doc = null;
		stack = new Stack<>();
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		String string = new String(ch, start, length);
		if (!inCDATA) {
			if (current != null) {
				current.addContent(new TextNode(string));
			} else {
				if (prolog == null) {
					prolog = new Vector<>();
				}
				prolog.add(new TextNode(string));
			}
		} else {
			if (cdata == null) {
				cdata = new StringBuffer();
			}
			cdata.append(string);
		}
	}

	@Override
	public void endDocument() throws SAXException {
		inDocument = false;
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		try {
			if (isRelaxNG) {
				Map<String, String> map = defaultAttributes.get(localName);
				if (map != null) {
					Set<String> keys = map.keySet();
					Iterator<String> it = keys.iterator();
					while (it.hasNext()) {
						String key = it.next();
						if (!current.hasAttribute(key)) {
							current.setAttribute(key, map.get(key));
						}
					}
				}
			}
			current = stack.pop();
		} catch (EmptyStackException es) {
			throw new SAXException("Malformed content found.");
		}
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		namespacesInUse.remove(prefix);
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		if (current != null) {
			current.addContent(new String(ch, start, length));
		} else {
			if (prolog == null) {
				prolog = new Vector<>();
			}
			prolog.add(new TextNode(new String(ch, start, length)));
		}
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		if (target.equals("xml-model")) {
			List<Attribute> atts = getPseudoAttributes(data);
			Iterator<Attribute> it = atts.iterator();
			String href = null;
			String schemaType = null;
			while (it.hasNext()) {
				Attribute a = it.next();
				if ("href".equals(a.getName())) {
					href = a.getValue();
				}
				if ("schematypens".equals(a.getName())) {
					schemaType = a.getValue();
				}
			}
			if (href != null && XMLConstants.RELAXNG_NS_URI.equals(schemaType)) {
				try {
					parseRelaxNG(href);
				} catch (IOException | ParserConfigurationException e) {
					throw new SAXException(e);
				}
			}
		}
		if (current != null) {
			if (!stack.isEmpty()) {
				current.addContent(new PI(target, data));
			} else {
				doc.addContent(new PI(target, data));
			}
		} else {
			if (prolog == null) {
				prolog = new Vector<>();
			}
			PI pi = new PI(target, data);
			prolog.add(pi);
		}
	}

	protected static List<Attribute> getPseudoAttributes(String string) {
		String data = string.trim();
		List<Attribute> result = new Vector<>();
		StringBuilder name = new StringBuilder();
		StringBuilder value = new StringBuilder();
		boolean inName = true;
		boolean inValue = false;
		char delimiter = '\"';
		for (int i = 0; i < data.length(); i++) {
			char c = data.charAt(i);
			if (inName) {
				if (c == '=' || Character.isWhitespace(c)) {
					inName = false;
				} else {
					name.append(c);
				}
			}
			if (inValue) {
				if (c == delimiter) {
					inValue = false;
					result.add(new Attribute(name.toString(), value.toString()));
					name = new StringBuilder();
					value = new StringBuilder();
					continue;
				}
				value.append(c);
			}
			if (!inName && !inValue) {
				if (Character.isWhitespace(c) || c == '=') {
					continue;
				}
				if (c == '\"' || c == '\'') {
					delimiter = c;
					inValue = true;
					continue;
				}
				name.append(c);
				inName = true;
			}
		}
		return result;
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		// do nothing
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
		// do nothing, the entity resolver must support this
	}

	@Override
	public void declaration(String version, String encoding, String standalone) throws SAXException {
		// do nothing
	}

	@Override
	public void startDocument() throws SAXException {
		inDocument = true;
		namespacesInUse = new Hashtable<>();
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (doc == null) {
			doc = new Document(uri, qName, prolog);
			if (encoding != null) {
				doc.setEncoding(encoding);
			}
			prolog = null;
		}
		if (current == null) {
			current = doc.getRootElement();
			if (prolog != null) {
				doc.setProlog(prolog);
				prolog = null;
			}
			if (namespaces != null) {
				Iterator<String[]> it = namespaces.iterator();
				while (it.hasNext()) {
					String[] pair = it.next();
					current.setAttribute("xmlns:" + pair[0], pair[1]);
					namespacesInUse.put(pair[0], pair[1]);
				}
				namespaces = null;
			}
			stack.push(current);
		} else {
			Element child = new Element(qName);
			String prefix = getPrefixPart(qName);
			if (!prefix.isEmpty() && !namespacesInUse.containsKey(prefix)) {
				if (pendingNamespaces == null) {
					pendingNamespaces = new Hashtable<>();
				}
				if (pendingNamespaces.containsKey(prefix)) {
					namespacesInUse.put(prefix, pendingNamespaces.get(prefix));
					child.setAttribute("xmlns:" + prefix, pendingNamespaces.get(prefix));
					pendingNamespaces.remove(prefix);
				}
			}
			current.addContent(child);
			stack.push(current);
			current = child;
		}
		for (int i = 0; i < atts.getLength(); i++) {
			String u = atts.getURI(i);
			String name = atts.getQName(i);
			if (u.equals("http://www.w3.org/XML/1998/namespace") && !name.startsWith("xml:")) {
				name = "xml:" + name;
			}
			current.setAttribute(name, atts.getValue(i));
		}
	}

	private static String getPrefixPart(String qName) {
		int index = qName.indexOf(':');
		return (index >= 0) ? qName.substring(0, index) : "";
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		if (!prefix.isEmpty() && inDocument) {
			if (current != null) {
				if (namespacesInUse == null) {
					namespacesInUse = new Hashtable<>();
				}
				if (!namespacesInUse.containsKey(prefix)) {
					if (pendingNamespaces == null) {
						pendingNamespaces = new Hashtable<>();
					}
					pendingNamespaces.put(prefix, uri);
				}
			} else {
				if (doc == null) {
					if (namespaces == null) {
						namespaces = new Vector<>();
					}
					String[] pair = { prefix, uri };
					namespaces.add(pair);
				}
			}
		}
	}

	@Override
	public Document getDocument() {
		return doc;
	}

	@Override
	public void comment(char[] ch, int start, int length) throws SAXException {
		if (!inDocument) {
			return;
		}
		if (current == null) {
			if (prolog == null) {
				prolog = new Vector<>();
			}
			prolog.add(new Comment(new String(ch, start, length)));
		} else {
			if (!stack.isEmpty()) {
				current.addContent(new Comment(new String(ch, start, length)));
			} else {
				doc.addContent(new Comment(new String(ch, start, length)));
			}
		}
	}

	@Override
	public void endCDATA() throws SAXException {
		inCDATA = false;
		if (current != null) {
			current.addContent(new CData(cdata.toString()));
		} else {
			doc.addContent(new CData(cdata.toString()));
		}
		cdata = null;
	}

	@Override
	public void endDTD() throws SAXException {
		inDocument = true;
	}

	@Override
	public void endEntity(String arg0) throws SAXException {
		// do nothing, let the EntityResolver handle this
	}

	@Override
	public void startCDATA() throws SAXException {
		inCDATA = true;
		if (cdata == null) {
			cdata = new StringBuffer();
		}
	}

	@Override
	public void startDTD(String name, String publicId, String systemId1) throws SAXException {
		if (doc == null) {
			this.systemId = systemId1;
			if (catalog != null && publicId != null) {
				catalog.parseDTD(publicId);				
			}
			doc = new Document(null, name, publicId, systemId);
			if (encoding != null) {
				doc.setEncoding(encoding);
			}
		}
		inDocument = false;
	}

	@Override
	public void startEntity(String arg0) throws SAXException {
		// do nothing, let the EntityResolver handle this
	}

	public String getSystemID() {
		return systemId;
	}

	public void setEncoding(String value) {
		encoding = value;
	}

	public void setInternalSubset(String internalSubset) {
		doc.setInternalSubset(internalSubset);
	}

	@Override
	public void setCatalog(Catalog catalog) {
		this.catalog = catalog;
	}

	private void parseRelaxNG(String href) throws SAXException, IOException, ParserConfigurationException {
		if (catalog != null) {
			String system = catalog.matchSystem(null, href);
			if (system != null) {
				RelaxNGParser relaxngParser = new RelaxNGParser(system, catalog);
				defaultAttributes = relaxngParser.getElements();
				isRelaxNG = true;
			}
		}
	}
}
