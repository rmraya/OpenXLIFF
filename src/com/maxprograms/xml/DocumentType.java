/*******************************************************************************
 * Copyright (c) 2022 Maxprograms.
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
import java.util.List;
import java.util.Vector;

public class DocumentType implements XMLNode {

    private String name;
    private String publicId;
    private String systemId;
    private List<XMLNode> internalSubset;

    public DocumentType(String name, String publicId, String systemId) {
        this.name = name;
        this.publicId = publicId;
        this.systemId = systemId;
    }

    @Override
    public short getNodeType() {
        return XMLNode.DOCUMENT_TYPE_NODE;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<!DOCTYPE ");
        sb.append(name);
        if (publicId != null && systemId != null) {
            sb.append(" PUBLIC \"");
            sb.append(publicId);
            sb.append("\" SYSTEM \"");
            sb.append(systemId);
            sb.append('\"');
        } else if (systemId != null) {
            sb.append('[');
            for (int i=0 ; i<internalSubset.size() ; i++) {
                sb.append(internalSubset.get(i).toString());
            }
            sb.append(']');
        }
        if (internalSubset != null) {

        }
        sb.append('>');
        return sb.toString();
    }

    @Override
    public void writeBytes(OutputStream output, Charset charset) throws IOException {
        // TODO Auto-generated method stub

    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public void setInternalSubset(List<XMLNode> internalSubset) {
        this.internalSubset = internalSubset;
    }

    public void setInternalSubset(String subset) {
        this.internalSubset = new Vector<>();
        parseInternalSubset(subset);
    }

    private void parseInternalSubset(String subset) {
        // TODO
    }
}
