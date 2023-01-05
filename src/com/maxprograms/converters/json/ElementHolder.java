/*******************************************************************************
 * Copyright (c) 2023 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/

 package com.maxprograms.converters.json;

import com.maxprograms.xml.Element;

public class ElementHolder {
    Element element;
    String start;
    String end;

    public ElementHolder(Element element, String start, String end) {
        this.element = element;
        this.start = start;
        this.end = end;
    }

    public Element getElement() {
        return element;
    }

    public String getStart() {
        return start;
    }

    public String getEnd() {
        return end;
    }
}
