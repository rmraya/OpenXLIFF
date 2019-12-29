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
import java.nio.charset.Charset;

public class CData implements XMLNode {

	private static final long serialVersionUID = 610927332260249086L;
	private String value;

	public CData(String data) {
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
		if (!(obj instanceof CData)) {
			return false;
		}
		CData cd = (CData) obj;
		return value.equals(cd.getData());
	}

	@Override
	public void writeBytes(FileOutputStream output, Charset charset) throws IOException {
		output.write(toString().getBytes(charset));
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

}
