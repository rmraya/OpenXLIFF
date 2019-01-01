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

public class TextNode implements XMLNode, Serializable {

	private static final long serialVersionUID = 2837146125080492272L;
	private String text;

	public TextNode(String value) {
		text = value;
	}

	@Override
	public short getNodeType() {
		return XMLNode.TEXT_NODE;
	}

	@Override
	public String toString() {
		return XMLUtils.cleanText(text);
	}

	@Override
	public void writeBytes(FileOutputStream output, Charset charset) throws IOException {
		output.write(XMLUtils.getBytes(XMLUtils.cleanText(text), charset));
	}

	public String getText() {
		return text;
	}

	public void setText(String value) {
		text = value;
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
		if (node.getNodeType() != XMLNode.TEXT_NODE) {
			return false;
		}
		return text.equals(((TextNode) node).getText());
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

}
