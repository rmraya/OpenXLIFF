/*******************************************************************************
 * Copyright (c)  Maxprograms.
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
import java.io.OutputStream;
import java.nio.charset.Charset;

public class TextNode implements XMLNode {

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
	public void writeBytes(OutputStream output, Charset charset) throws IOException {
		output.write(toString().getBytes(charset));
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
		if (!(obj instanceof TextNode)) {
			return false;
		}
		return text.equals(((TextNode) obj).getText());
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

}
