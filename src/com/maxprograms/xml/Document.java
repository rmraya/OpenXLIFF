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

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

public class Document implements XMLNode {

	private String systemId;
	private String publicId;
	private Vector<XMLNode> content;
	private Element root;
	private String internalSubset;
	private Charset encoding;
	private Hashtable<String, String> entities;
	private Vector<String> attributes;

	private static final Logger LOGGER = System.getLogger(Document.class.getName());

	@Override
	public short getNodeType() {
		return XMLNode.DOCUMENT_NODE;
	}

	public Document(String namespaceURI, String qualifiedName, String publicId, String systemId) {
		this.publicId = publicId;
		this.systemId = systemId;
		if (namespaceURI == null) {
			namespaceURI = "";
		}
		content = new Vector<>();
		root = new Element(qualifiedName);
		content.add(root);
		if (!namespaceURI.equals("")) {
			String prefix = root.getPrefix();
			if (prefix != null) {
				root.setAttribute("xmlns:" + prefix, namespaceURI);
			} else {
				root.setAttribute("xmlns", namespaceURI);
			}
		}
	}

	public Document(String namespaceURI, String qualifiedName, String internalSubset) {
		if (namespaceURI == null) {
			namespaceURI = "";
		}
		this.internalSubset = internalSubset;
		content = new Vector<>();
		root = new Element(qualifiedName);
		content.add(root);

		if (!namespaceURI.equals("")) {
			String prefix = root.getPrefix();
			if (prefix != null) {
				root.setAttribute("xmlns:" + prefix, namespaceURI);
			} else {
				root.setAttribute("xmlns", namespaceURI);
			}
		}
	}

	protected Document(String namespaceURI, String qualifiedName, Vector<XMLNode> prolog) {
		if (namespaceURI == null) {
			namespaceURI = "";
		}
		content = new Vector<>();
		if (prolog != null) {
			Iterator<XMLNode> it = prolog.iterator();
			while (it.hasNext()) {
				XMLNode node = it.next();
				switch (node.getNodeType()) {
				case XMLNode.PROCESSING_INSTRUCTION_NODE:
					content.add(new PI(((PI) node).getTarget(), ((PI) node).getData()));
					break;
				case XMLNode.COMMENT_NODE:
					content.add(new Comment(((Comment) node).getText()));
					break;
				default:
					// should never happen
					LOGGER.log(Level.WARNING, "Prolog contains wrong content type.");
				}
			}
		}
		root = new Element(qualifiedName);
		content.add(root);
		if (!namespaceURI.equals("")) {
			String prefix = root.getPrefix();
			if (prefix != null) {
				root.setAttribute("xmlns:" + prefix, namespaceURI);
			} else {
				root.setAttribute("xmlns", namespaceURI);
			}
		}
	}

	public Element getRootElement() {
		return root;
	}

	public void setRootElement(Element e) {
		for (int i = 0; i < content.size(); i++) {
			XMLNode node = content.get(i);
			if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
				root = e;
				content.remove(i);
				content.set(i, e);
			}
		}
	}

	public String getSystemId() {
		return systemId;
	}

	public void setSystemId(String id) {
		systemId = id;
	}

	public String getPublicId() {
		return publicId;
	}

	public void setPublicId(String id) {
		publicId = id;
	}

	public void setDocType(String publicId, String systemId) {
		this.publicId = publicId;
		this.systemId = systemId;
	}

	public String getInternalSubset() {
		return internalSubset;
	}

	@Override
	public String toString() {
		return getRootElement().getName() + "@document";
	}

	public List<PI> getPI() {
		Vector<PI> result = new Vector<>();
		for (int i = 0; i < content.size(); i++) {
			XMLNode n = content.get(i);
			if (n.getNodeType() == XMLNode.PROCESSING_INSTRUCTION_NODE) {
				result.add((PI) n);
			}
		}
		return result;
	}

	public List<PI> getPI(String target) {
		Vector<PI> result = new Vector<>();
		for (int i = 0; i < content.size(); i++) {
			XMLNode n = content.get(i);
			if (n.getNodeType() == XMLNode.PROCESSING_INSTRUCTION_NODE && ((PI) n).getTarget().equals(target)) {
				result.add((PI) n);
			}
		}
		return result;
	}

	public void removePI(String target) {
		for (int i = 0; i < content.size(); i++) {
			XMLNode node = content.get(i);
			if (node.getNodeType() == XMLNode.PROCESSING_INSTRUCTION_NODE && ((PI) node).getTarget().equals(target)) {
				content.remove(node);
			}
		}
	}

	public void removeAllPI() {
		for (int i = 0; i < content.size(); i++) {
			XMLNode node = content.get(i);
			if (node.getNodeType() == XMLNode.PROCESSING_INSTRUCTION_NODE) {
				content.remove(node);
			}
		}
	}

	public void addPI(PI pi) {
		content.add(pi);
	}

	public boolean isDefaultNamespace(String namespaceURI) {
		return root.getAttributeValue("xmlns", "").equals(namespaceURI);
	}

	public void setDefaultNamespace(String namespaceURI) {
		String prfx = lookupPrefix(namespaceURI);
		if (prfx != null) {
			root.removeAttribute("xmlns:" + prfx);
		}
		root.setAttribute("xmlns", namespaceURI);
		root.setPrefix(prfx);
	}

	private String lookupPrefix(String namespaceURI) {
		List<Attribute> atts = root.getAttributes();
		for (int i = 0; i < atts.size(); i++) {
			Attribute a = atts.get(i);
			if (a.getName().startsWith("xmlns:") && a.getValue().equals(namespaceURI)) {
				String[] parts = a.getName().split(":");
				return parts[1];
			}
		}
		return null;
	}

	public Charset getEncoding() {
		if (encoding == null) {
			encoding = StandardCharsets.UTF_8;
		}
		return encoding;
	}

	public void setEncoding(String value) {
		encoding = Charset.forName(value);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		Document doc = (Document) obj;
		return content == doc.getContent();
	}

	public List<XMLNode> getContent() {
		return content;
	}

	public void addContent(XMLNode node) {
		content.add(node);
	}

	protected void setProlog(Vector<XMLNode> prolog) {
		Iterator<XMLNode> it = prolog.iterator();
		while (it.hasNext()) {
			XMLNode node = it.next();
			switch (node.getNodeType()) {
			case XMLNode.PROCESSING_INSTRUCTION_NODE:
				content.add(content.size() - 1, new PI(((PI) node).getTarget(), ((PI) node).getData()));
				break;
			case XMLNode.COMMENT_NODE:
				content.add(content.size() - 1, new Comment(((Comment) node).getText()));
				break;
			default:
				// should never happen
				LOGGER.log(Level.WARNING, "Prolog contains wrong content type.");
			}
		}
	}

	public void setEntities(Hashtable<String, String> table) {
		entities = table;
	}

	public Hashtable<String, String> getEntities() {
		return entities;
	}

	@Override
	public void writeBytes(FileOutputStream output, Charset charset) throws IOException {
		XMLOutputter outputter = new XMLOutputter();
		outputter.setEncoding(charset);
		outputter.output(this, output);
	}

	public void setAttributes(Vector<String> attributes) {
		this.attributes = attributes;
	}

	public Vector<String> getAttributes() {
		return attributes;
	}

	public void setInternalSubset(String value) {
		internalSubset = value;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

}
