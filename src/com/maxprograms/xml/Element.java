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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

public class Element implements XMLNode, Serializable {

	private static final long serialVersionUID = 3004362075586010085L;

	private String name;
	private List<Attribute> attributes;
	private Vector<XMLNode> content;
	private Hashtable<String, Attribute> attsTable;

	private static final Logger LOGGER = System.getLogger(Element.class.getName());

	public Element() {
		name = "";
		attributes = new Vector<>();
		attsTable = new Hashtable<>();
		content = new Vector<>();
	}

	public Element(String name) {
		this.name = name;
		attributes = new Vector<>();
		attsTable = new Hashtable<>();
		content = new Vector<>();
	}

	public void addContent(XMLNode n) {
		content.add(n);
	}

	public void addContent(Element e) {
		content.add(e);
	}

	public void addContent(Comment c) {
		content.add(c);
	}

	public void addContent(PI pi) {
		content.add(pi);
	}

	public void addContent(CData c) {
		content.add(c);
	}

	public void addContent(String text) {
		content.add(new TextNode(text));
	}

	public void addContent(TextNode text) {
		content.add(text);
	}

	public void clone(Element src) {
		name = src.getName();
		attributes = new Vector<>();
		attsTable = new Hashtable<>();
		List<Attribute> atts = src.getAttributes();
		Iterator<Attribute> it = atts.iterator();
		while (it.hasNext()) {
			Attribute a = it.next();
			Attribute n = new Attribute(a.getName(), a.getValue());
			attributes.add(n);
			attsTable.put(n.getName(), n);
		}
		content = new Vector<>();
		List<XMLNode> cont = src.getContent();
		Iterator<XMLNode> ic = cont.iterator();
		while (ic.hasNext()) {
			XMLNode node = ic.next();
			switch (node.getNodeType()) {
			case XMLNode.TEXT_NODE:
				content.add(new TextNode(((TextNode) node).getText()));
				break;
			case XMLNode.ELEMENT_NODE:
				Element e = new Element();
				e.clone((Element) node);
				content.add(e);
				break;
			case XMLNode.PROCESSING_INSTRUCTION_NODE:
				content.add(new PI(((PI) node).getTarget(), ((PI) node).getData()));
				break;
			case XMLNode.COMMENT_NODE:
				content.add(new Comment(((Comment) node).getText()));
				break;
			case XMLNode.CDATA_SECTION_NODE:
				content.add(new CData(((CData) node).getData()));
				break;
			default:
				// should never happen
				LOGGER.log(Level.WARNING, "Element contains wrong content type.");
			}
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		XMLNode node = (XMLNode) obj;
		if (node.getNodeType() != XMLNode.ELEMENT_NODE) {
			return false;
		}
		Element s = (Element) node;
		if (!name.equals(s.getName())) {
			return false;
		}
		if (attributes.size() != s.getAttributes().size()) {
			return false;
		}
		for (int i = 0; i < attributes.size(); i++) {
			Attribute a = attributes.get(i);
			Attribute b = s.getAttribute(a.getName());
			if (b == null) {
				return false;
			}
			if (!a.equals(b)) {
				return false;
			}
		}
		mergeText();
		s.mergeText();
		if (content.size() != s.getContent().size()) {
			return false;
		}
		List<XMLNode> scontent = s.getContent();
		for (int i = 0; i < content.size(); i++) {
			if (!content.get(i).equals(scontent.get(i))) {
				return false;
			}
		}
		return true;
	}

	private void mergeText() {
		if (content.size() < 2) {
			return;
		}
		Vector<XMLNode> newContent = new Vector<>();
		for (int i = 0; i < content.size(); i++) {
			XMLNode n = content.get(i);
			if (n.getNodeType() == XMLNode.TEXT_NODE && !newContent.isEmpty()) {
				if (newContent.get(newContent.size() - 1).getNodeType() == XMLNode.TEXT_NODE) {
					TextNode t = (TextNode) newContent.get(newContent.size() - 1);
					t.setText(t.getText() + ((TextNode) n).getText());
				} else {
					newContent.add(n);
				}
			} else {
				newContent.add(n);
			}
		}
		content = null;
		content = newContent;
	}

	public Attribute getAttribute(String attributeName) {
		return attsTable.get(attributeName);
	}

	public List<Attribute> getAttributes() {
		return attributes;
	}

	public String getAttributeValue(String attributeName) {
		Attribute a = attsTable.get(attributeName);
		return a != null ? a.getValue() : "";
	}

	public String getAttributeValue(String attributeName, String defaultValue) {
		Attribute a = attsTable.get(attributeName);
		return a != null ? a.getValue() : defaultValue;
	}

	public Element getChild(String tagName) {
		for (int i = 0; i < content.size(); i++) {
			XMLNode node = content.get(i);
			if (node.getNodeType() == XMLNode.ELEMENT_NODE && ((Element) node).getName().equals(tagName)) {
				return (Element) node;
			}
		}
		return null;
	}

	public List<Element> getChildren() {
		List<Element> result = new Vector<>();
		for (int i = 0; i < content.size(); i++) {
			XMLNode node = content.get(i);
			if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
				result.add((Element) node);
			}
		}
		return result;
	}

	public List<Element> getChildren(String tagname) {
		List<Element> result = new Vector<>();
		for (int i = 0; i < content.size(); i++) {
			XMLNode node = content.get(i);
			if (node.getNodeType() == XMLNode.ELEMENT_NODE && ((Element) node).getName().equals(tagname)) {
				result.add((Element) node);
			}
		}
		return result;
	}

	public List<XMLNode> getContent() {
		mergeText();
		return content;
	}

	public String getName() {
		return name;
	}

	public String getLocalName() {
		if (name.indexOf(':') == -1) {
			return name;
		}
		return name.substring(name.indexOf(':'));
	}
	
	public String getNamespace() {
		if (name.indexOf(':') == -1) {
			return "";
		}
		return name.substring(0, name.indexOf(':'));
	}
	
	@Override
	public short getNodeType() {
		return XMLNode.ELEMENT_NODE;
	}

	public String getText() {
		StringBuilder result = new StringBuilder("");
		for (int i = 0; i < content.size(); i++) {
			XMLNode node = content.get(i);
			if (node.getNodeType() == XMLNode.TEXT_NODE) {
				result.append(((TextNode) node).getText());
			}
			if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
				result.append(((Element) node).getText());
			}
		}
		return result.toString();
	}

	public String getTextNormalize() {
		String result = getText();
		result = result.replaceAll("\\s\\s+", " ");
		return result.trim();
	}

	public void removeAttribute(String attributeName) {
		for (int i = 0; i < attributes.size(); i++) {
			Attribute a = attributes.get(i);
			if (a.getName().equals(attributeName)) {
				attributes.remove(a);
				break;
			}
		}
		attsTable.remove(attributeName);
	}

	public void removeChild(String string) {
		for (int i = 0; i < content.size(); i++) {
			XMLNode node = content.get(i);
			if (node.getNodeType() == XMLNode.ELEMENT_NODE && ((Element) node).getName().equals(string)) {
				content.remove(node);
			}
		}
	}

	public void removeChild(Element child) {
		for (int i = 0; i < content.size(); i++) {
			XMLNode node = content.get(i);
			if (node.getNodeType() == XMLNode.ELEMENT_NODE && ((Element) node).equals(child)) {
				content.remove(node);
				return;
			}
		}
	}

	public void setAttribute(String attributeName, String value) {
		if (!attsTable.containsKey(attributeName)) {
			Attribute a = new Attribute(attributeName, value);
			attsTable.put(attributeName, a);
			attributes.add(a);
		} else {
			for (int i = 0; i < attributes.size(); i++) {
				Attribute a = attributes.get(i);
				if (a.getName().equals(attributeName)) {
					a.setValue(value);
					attsTable.put(attributeName, a);
					break;
				}
			}
		}
	}

	public void setContent(List<XMLNode> c) {
		content = new Vector<>();
		for (int i = 0; i < c.size(); i++) {
			content.add(c.get(i));
		}
	}

	public void setText(String text) {
		content = new Vector<>();
		content.add(new TextNode(text));
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder("<" + name);
		for (int i = 0; i < attributes.size(); i++) {
			Attribute a = attributes.get(i);
			result.append(" " + a.toString());
		}
		if (content.isEmpty()) {
			result.append("/>");
			return result.toString();
		}
		result.append(">");
		for (int i = 0; i < content.size(); i++) {
			result.append(content.get(i).toString());
		}
		result.append("</" + name + ">");
		return result.toString();
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

	public void removePI(String string) {
		for (int i = 0; i < content.size(); i++) {
			XMLNode node = content.get(i);
			if (node.getNodeType() == XMLNode.PROCESSING_INSTRUCTION_NODE && ((PI) node).getTarget().equals(string)) {
				content.remove(node);
			}
		}
	}

	public String getPrefix() {
		String[] parts = name.split(":");
		if (parts.length == 2) {
			return parts[0];
		}
		return null;
	}

	public void setPrefix(String prfx) {
		String[] parts = name.split(":");
		if (parts.length == 2) {
			name = prfx + ':' + parts[1];
		} else {
			name = prfx + ":" + name;
		}
	}

	public void setAttributes(List<Attribute> vector) {
		attsTable.clear();
		attributes.clear();
		Iterator<Attribute> it = vector.iterator();
		while (it.hasNext()) {
			Attribute a = it.next();
			setAttribute(a.getName(), a.getValue());
		}
	}

	public void setChildren(List<Element> c) {
		content = new Vector<>();
		for (int i = 0; i < c.size(); i++) {
			content.add(c.get(i));
		}
	}

	@Override
	public void writeBytes(FileOutputStream output, Charset charset) throws IOException {
		output.write(XMLUtils.getBytes("<" + name, charset));
		for (int i = 0; i < attributes.size(); i++) {
			Attribute a = attributes.get(i);
			output.write(XMLUtils.getBytes(" ", charset));
			a.writeBytes(output, charset);
		}
		if (content.isEmpty()) {
			output.write(XMLUtils.getBytes("/>", charset));
			return;
		}
		output.write(XMLUtils.getBytes(">", charset));
		for (int i = 0; i < content.size(); i++) {
			content.get(i).writeBytes(output, charset);
		}
		output.write(XMLUtils.getBytes("</" + name + ">", charset));
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

}
