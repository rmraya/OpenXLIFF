/*******************************************************************************
 * Copyright (c) 2003, 2019 Maxprograms.
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

public class Attribute implements XMLNode, Serializable, Comparable<Attribute> {

	private static final long serialVersionUID = -859299907013846457L;
	private String name;
	private String value;

	protected Attribute() {
		name = "";
		value = "";
	}

	public Attribute(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name + "=\"" + XMLUtils.cleanText(value).replaceAll("\"", "&quot;") + "\"";
	}

	@Override
	public short getNodeType() {
		return XMLNode.ATTRIBUTE_NODE;
	}

	public void setValue(String string) {
		value = string;
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
		if (node.getNodeType() != XMLNode.ATTRIBUTE_NODE) {
			return false;
		}
		Attribute a = (Attribute) node;
		return a.getName().equals(name) && a.getValue().equals(value);
	}

	@Override
	public void writeBytes(FileOutputStream output, Charset charset) throws IOException {
		output.write(
				XMLUtils.getBytes(name + "=\"" + XMLUtils.cleanText(value).replaceAll("\"", "&quot;") + "\"", charset));
	}

	@Override
	public int compareTo(Attribute o) {
		return name.compareTo(o.name);
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	public String getLocalName() {
		if (name.indexOf(':') == -1) {
			return name;
		}
		return name.substring(name.indexOf(':')+1);
	}
	
	public String getNamespace() {
		if (name.indexOf(':') == -1) {
			return "";
		}
		return name.substring(0, name.indexOf(':'));
	}

}
