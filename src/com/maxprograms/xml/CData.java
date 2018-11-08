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
import java.io.Serializable;
import java.nio.charset.Charset;

public class CData implements XMLNode, Serializable {

	private static final long serialVersionUID = 610927332260249086L;
	private String value;

	protected CData() {
		value = "";
	}

	protected CData(String data) {
		value = data;
	}

	public String getData() {
		return value;
	}

	@Override
	public String toString() {
		return "<![CDATA[" + value + "]]>";
	}

	@Override
	public short getNodeType() {
		return XMLNode.CDATA_SECTION_NODE;
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
		if (node.getNodeType() != XMLNode.CDATA_SECTION_NODE) {
			return false;
		}
		CData cd = (CData) node;
		return value.equals(cd.getData());
	}

	@Override
	public void writeBytes(FileOutputStream output, Charset charset) throws IOException {
		output.write(XMLUtils.getBytes("<![CDATA[", charset));
		output.write(XMLUtils.getBytes(value, charset));
		output.write(XMLUtils.getBytes("]]>", charset));
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

}
