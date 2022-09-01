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

public class AttributeDecl implements XMLNode, Comparable<AttributeDecl> {

    String element;
    String attribute;
    String type;
    String mode;
    String value;

    public AttributeDecl(String element, String attribute, String type, String mode, String value) {
        this.element = element;
        this.attribute = attribute;
        this.type = type;
        this.mode = mode;
        this.value = value;
    }

    public String getElementName() {
        return element;
    }

    public String getAttributeName() {
        return attribute;
    }

    public String getMode() {
        return mode;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        if (value != null) {
            return "<!ATTLIST " + element + " " + attribute + " " + type + " " + mode + " \"" + value + "\">";
        }
        return "<!ATTLIST " + element + " " + attribute + " " + type + " " + mode + ">";
    }

    @Override
    public int compareTo(AttributeDecl o) {
        int first = element.compareTo(o.element);
        return first != 0 ? first : attribute.compareTo(o.attribute);
    }

    @Override
    public short getNodeType() {
        return XMLNode.ATTRIBUTE_DECL_NODE;
    }

    @Override
    public void writeBytes(OutputStream output, Charset charset) throws IOException {
        output.write(toString().getBytes(charset));
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AttributeDecl other) {
          return toString().equals(other.toString());
        }
        return false;
    }

}
