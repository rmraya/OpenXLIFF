/*******************************************************************************
 * Copyright (c) 2022 - 2024 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/
package com.maxprograms.converters.ditamap;

import org.json.JSONObject;

import com.maxprograms.xml.Element;

public class Key implements Comparable<Key> {

	private String name;
	private String href;
	private Element topicmeta;
	private String keyref;
	private String defined;
	private boolean translate;

	public Key(String name, String keyref, String defined) {
		this.name = name;
		this.keyref = keyref;
		this.defined = defined;
	}

	public String toString() {
		JSONObject json = new JSONObject();
		json.put("name", name);
		json.put("href", href);
		json.put("keyref", keyref);
		json.put("defined", defined);
		return json.toString(2);
	}

	public Key(String name, String href, Element topicmeta, String defined, boolean translate) {
		this.name = name;
		this.href = href;
		this.topicmeta = topicmeta;
		this.defined = defined;
		this.translate = translate;
	}

	public String getName() {
		return name;
	}

	public String getHref() {
		return href;
	}

	public String getKeyref() {
		return keyref;
	}

	public Element getTopicmeta() {
		return topicmeta;
	}

	public String getDefined() {
		return defined;
	}

	public boolean isTranslate() {
		return translate;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Key k) {
			return k.name.equals(name) && k.defined.equals(defined);
		}
		return false;
	}

	@Override
	public int compareTo(Key o) {
		if (name.equals(o.getName())) {
			return 0;
		}
		return -1;
	}

	@Override
	public int hashCode() {
		return (name + defined).hashCode();
	}

}
