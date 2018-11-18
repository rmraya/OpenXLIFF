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

import java.util.EmptyStackException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

class CustomContentHandler implements ContentHandler, LexicalHandler {

	private Document doc;
	private Element current;
	Stack<Element> stack;
	private Vector<XMLNode> prolog;
	private boolean inDocument;
	private boolean inCDATA = false;
	private StringBuffer cdata;
	private Vector<String[]> namespaces;
	private Hashtable<String, String> namespacesInUse;
	private Hashtable<String, String> pendingNamespaces;
	private String systemId;
	private String publicId;
	private String encoding;

	public CustomContentHandler() {
		doc = null;
		stack = new Stack<>();
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (!inCDATA) {
			if (current != null) {
				current.addContent(new String(ch, start, length));
			} else {
				if (prolog == null) {
					prolog = new Vector<>();
				}
				prolog.add(new TextNode(new String(ch, start, length)));
			}
		} else {
			if (cdata == null) {
				cdata = new StringBuffer();
			}
			cdata.append(new String(ch, start, length));
		}
	}

	@Override
	public void endDocument() throws SAXException {
		stack = null;
		inDocument = false;
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		try {
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
			Vector<Attribute> atts = getPseudoAttributes(data);
			Iterator<Attribute> it = atts.iterator();
			while (it.hasNext()) {
				Attribute a = it.next();
				if (a.getName().equals("href")) {
					systemId = a.getValue();
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

	protected static Vector<Attribute> getPseudoAttributes(String data) {
		data = data.trim();
		Vector<Attribute> result = new Vector<>();
		String name = "";
		String value = "";
		boolean inName = true;
		boolean inValue = false;
		char delimiter = '\"';
		for (int i = 0; i < data.length(); i++) {
			char c = data.charAt(i);
			if (inName) {
				if (c == '=' || Character.isWhitespace(c)) {
					inName = false;
				} else {
					name = name + c;
				}
			}
			if (inValue) {
				if (c == delimiter) {
					inValue = false;
					result.add(new Attribute(name, value));
					name = "";
					value = "";
					continue;
				}
				value = value + c;
			}
			if (!inName && !inValue) {
				if (Character.isWhitespace(c)) {
					continue;
				}
				if (c == '=') {
					continue;
				}
				if (c == '\"' || c == '\'') {
					delimiter = c;
					inValue = true;
					continue;
				}
				name = name + c;
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
			if (!prefix.equals("") && !namespacesInUse.containsKey(prefix)) {
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
		if (!prefix.equals("") && inDocument) {
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
	public void startDTD(String name, String publicId1, String systemId1) throws SAXException {
		if (doc == null) {
			systemId = systemId1;
			publicId = publicId1;
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

	public String getPublicID() {
		return publicId;
	}

	public void setEncoding(String value) {
		encoding = value;
	}

	public void setInternalSubset(String internalSubset) {
		doc.setInternalSubset(internalSubset);
	}
}
