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

public class ElementDecl implements XMLNode {

    private String name;
    private String model;

    public ElementDecl(String declaration) {
        int i = "<!ELEMENT".length();
        char c = declaration.charAt(i);
        while (XMLUtils.isXmlSpace(c)) {
            i++;
            c = declaration.charAt(i);
        }
        StringBuilder sb = new StringBuilder();
        while (!XMLUtils.isXmlSpace(c)) {
            sb.append(c);
            i++;
            c = declaration.charAt(i);
        }
        name = sb.toString();
        model = declaration.substring(i, declaration.lastIndexOf('>')).trim();
    }

    public String getName() {
        return name;
    }

    @Override
    public short getNodeType() {
        return XMLNode.ELEMENT_DECL_NODE;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<!ELEMENT ");        
        sb.append(name);
        sb.append(' ');
        sb.append(model);
        sb.append('>');
        return sb.toString();
    }

    @Override
    public void writeBytes(OutputStream output, Charset charset) throws IOException {
        output.write(toString().getBytes(charset));
    }
}
