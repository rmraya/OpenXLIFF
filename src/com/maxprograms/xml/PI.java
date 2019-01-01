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

public class PI implements XMLNode, Serializable {

	private static final long serialVersionUID = -689697302415200547L;
	private String target;
	private String data;

	public PI(String target, String data) {
		this.target = target;
		this.data = data;
	}

	public String getTarget() {
		return target;
	}

	public String getData() {
		return data;
	}

	public void setData(String value) {
		data = value;
	}

	@Override
	public String toString() {
		return "<?" + target + " " + data + "?>";
	}

	@Override
	public void writeBytes(FileOutputStream output, Charset charset) throws IOException {
		output.write(XMLUtils.getBytes("<?" + target + " ", charset));
		output.write(XMLUtils.getBytes(data, charset));
		output.write(XMLUtils.getBytes("?>", charset));
	}

	@Override
	public short getNodeType() {
		return XMLNode.PROCESSING_INSTRUCTION_NODE;
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
		if (node.getNodeType() != XMLNode.PROCESSING_INSTRUCTION_NODE) {
			return false;
		}
		PI pi = (PI) node;
		return target.equals(pi.getTarget()) && data.equals(pi.getData());
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

}
