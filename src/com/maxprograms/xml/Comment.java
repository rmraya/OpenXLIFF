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

public class Comment implements XMLNode, Serializable {

	private static final long serialVersionUID = 4121792990661005580L;
	private String value;

	protected Comment(String text) {
		value = text;
	}

	public String getText() {
		return value;
	}

	public void setText(String text) {
		value = text;
	}

	@Override
	public String toString() {
		return "<!-- " + value + " -->";
	}

	@Override
	public short getNodeType() {
		return XMLNode.COMMENT_NODE;
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
		if (node.getNodeType() != XMLNode.COMMENT_NODE) {
			return false;
		}
		Comment c = (Comment) node;
		return value.equals(c.getText());
	}

	@Override
	public void writeBytes(FileOutputStream output, Charset charset) throws IOException {
		output.write(XMLUtils.getBytes("<!-- ", charset));
		output.write(XMLUtils.getBytes(value, charset));
		output.write(XMLUtils.getBytes(" -->", charset));
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

}
