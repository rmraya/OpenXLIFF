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
package com.maxprograms.languages;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;

public class Language implements Comparable<Language>, Serializable {

	private static final long serialVersionUID = -5391793426888923842L;

	private static List<Language> languages;

	private String code;
	private String description;
	private String script;

	public Language(String code, String description) {
		this.code = code;
		this.description = description;
		script = "";
	}

	public String getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return description;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Language) {
			Language l = (Language) obj;
			return code.equals(l.getCode()) && description.equals(l.getDescription());
		}
		return false;
	}

	@Override
	public int compareTo(Language arg0) {
		return description.compareTo(arg0.getDescription());
	}

	public void setSuppressedScript(String value) {
		script = value;
	}

	public String getSuppresedScript() {
		return script;
	}

	public boolean isBiDi() {
		if (code.startsWith("ar") || code.startsWith("fa") || code.startsWith("az") || code.startsWith("ur")
				|| code.startsWith("pa-PK") || code.startsWith("ps") || code.startsWith("prs") || code.startsWith("ug")
				|| code.startsWith("he") || code.startsWith("ji") || code.startsWith("yi")) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return code.hashCode();
	}

	public static List<Language> getCommonLanguages() throws SAXException, IOException, ParserConfigurationException {
		if (languages == null) {
			languages = new ArrayList<>();
			RegistryParser registry = new RegistryParser(Language.class.getResource("language-subtag-registry.txt"));
			SAXBuilder builder = new SAXBuilder();
			Element root = builder.build(Language.class.getResource("languageList.xml")).getRootElement();
			List<Element> children = root.getChildren();
			Iterator<Element> it = children.iterator();
			while (it.hasNext()) {
				Element lang = it.next();
				String code = lang.getAttributeValue("code");
				String description = registry.getTagDescription(code);
				languages.add(new Language(code, description));
			}
			Collections.sort(languages);
		}
		return languages;
	}
}
