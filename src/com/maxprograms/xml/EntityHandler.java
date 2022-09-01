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

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

public class EntityHandler extends DefaultHandler2 {

	private Map<String, String> entities;
	private List<AttributeDecl> attributeDeclarations;

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

	public Map<String, String> getEntities() {
		return entities;
	}

	@Override
	public void elementDecl(String name, String model) throws SAXException {
		// TODO
	}

	@Override
	public void notationDecl(String name, String publicId, String systemId) throws SAXException {
		// TODO
	}

	@Override
	public void attributeDecl(String element, String attribute, String type, String mode, String value)
			throws SAXException {
		if (attribute.indexOf(':') == -1) {
			return;
		}
		if (attribute.startsWith("xml:")) {
			return;
		}
		if (mode == null || type == null) {
			return;
		}
		if (attributeDeclarations == null) {
			attributeDeclarations = new Vector<>();
		}
		attributeDeclarations.add(new AttributeDecl(element, attribute, type, mode, value));
	}

	public List<AttributeDecl> getAttributes() {
		return attributeDeclarations;
	}
}
