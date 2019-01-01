/*******************************************************************************
 * Copyright (c) 2003, 2019 Maxprograms.
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

import java.util.Hashtable;
import java.util.Vector;

import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

public class EntityHandler extends DefaultHandler2 {

	private Hashtable<String, String> entities;
	private Vector<String> attributes;

	public EntityHandler() {
		super();
		entities = new Hashtable<>();
	}

	@Override
	public void internalEntityDecl(String name, String value) throws SAXException {
		super.internalEntityDecl(name, value);
		if (!name.startsWith("%")) {
			entities.put(name, value);
		}
	}

	public Hashtable<String, String> getEntities() {
		return entities;
	}

	@Override
	public void attributeDecl(String eName, String aName, String type, String mode, String value) throws SAXException {
		if (aName.indexOf(':') == -1) {
			return;
		}
		if (aName.startsWith("xml:") || aName.startsWith("xmlns:")) {
			return;
		}
		if (mode == null || type == null) {
			return;
		}
		if (attributes == null) {
			attributes = new Vector<>();
		}
		if (value != null) {
			attributes.add("<!ATTLIST " + eName + " " + aName + " " + type + " " + mode + " " + value + ">"); 
		} else {
			attributes.add("<!ATTLIST " + eName + " " + aName + " " + type + " " + mode + ">"); 
		}

	}

	public Vector<String> getAttributes() {
		return attributes;
	}
}
