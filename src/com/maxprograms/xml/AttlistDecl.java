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
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class AttlistDecl implements XMLNode {

    private String listName;
    private List<AttributeDecl> attributes;

    public AttlistDecl(String declaration) {
        attributes = new Vector<>();
        int i = "<!ATTLIST".length();
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
        listName = sb.toString();
        // TODO this.parseAttributes(declaration.substring(i).trim());
    }

    private void parseAttributes(String declaration) {
        int i = 0;
        char c = declaration.charAt(i);
        while (XMLUtils.isXmlSpace(c)) {
            i++;
            c = declaration.charAt(i);
        }
        StringBuilder nameBuilder = new StringBuilder();
        while (!XMLUtils.isXmlSpace(c)) {
            nameBuilder.append(c);
            i++;
            c = declaration.charAt(i);
        }
        String name = nameBuilder.toString();
        while (XMLUtils.isXmlSpace(c)) {
            i++;
            c = declaration.charAt(i);
        }
        StringBuilder typeBuilder = new StringBuilder();
        if (c == '(') {
            // it is an ennumeration
            while (c != ')') {
                typeBuilder.append(c);
                i++;
                c = declaration.charAt(i);
            }
            typeBuilder.append(')');
        } else {
            while (!XMLUtils.isXmlSpace(c) || c == '(') {
                typeBuilder.append(c);
                i++;
                c = declaration.charAt(i);
            }
        }
        String type = typeBuilder.toString();
    }

    public String getListName() {
        return listName;
    }

    public List<AttributeDecl> getAttributes() {
        return attributes;
    }

    @Override
    public short getNodeType() {
        return XMLNode.ATTRIBUTE_DECL_NODE;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<!ATTLIST");
        Iterator<AttributeDecl> it = attributes.iterator();
        while (it.hasNext()) {
            sb.append("\n  ");
            sb.append(it.next().toString());
        }
        sb.append("\n>\n");
        return sb.toString();
    }

    @Override
    public void writeBytes(OutputStream output, Charset charset) throws IOException {
        output.write(toString().getBytes(charset));
    }
}
