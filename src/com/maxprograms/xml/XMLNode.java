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
import java.io.Serializable;
import java.nio.charset.Charset;

public interface XMLNode extends Serializable {

	public final short DOCUMENT_NODE = 0;
	public final short ELEMENT_NODE = 1;
	public final short ATTRIBUTE_NODE = 2;
	public final short CDATA_SECTION_NODE = 3;
	public final short COMMENT_NODE = 4;
	public final short PROCESSING_INSTRUCTION_NODE = 5;
	public final short TEXT_NODE = 6;

	public short getNodeType();

	@Override
	public String toString();

	@Override
	public boolean equals(Object node);

	public void writeBytes(OutputStream output, Charset charset) throws IOException;

}
