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
package com.maxprograms.languages;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;

public class LanguageUtils {

	private static List<Language> languages;
	private static List<Language> extendedLanguages;
	private static Set<String> bidiCodes;
	private static RegistryParser registry;

	private LanguageUtils() {
		// do not instantiate
	}

	public static List<Language> getAllLanguages() throws SAXException, IOException, ParserConfigurationException {
		if (registry == null) {
			registry = new RegistryParser(Language.class.getResource("language-subtag-registry.txt"));
		}
		if (extendedLanguages == null) {
			extendedLanguages = new Vector<>();
			bidiCodes = new TreeSet<>();
			SAXBuilder builder = new SAXBuilder();
			Element root = builder.build(Language.class.getResource("extendedLanguageList.xml")).getRootElement();
			List<Element> children = root.getChildren();
			Iterator<Element> it = children.iterator();
			while (it.hasNext()) {
				Element lang = it.next();
				String code = lang.getAttributeValue("code");
				String description = registry.getTagDescription(code);
				extendedLanguages.add(new Language(code, description));
				if ("true".equals(lang.getAttributeValue("bidi"))) {
					bidiCodes.add(code);
				}
			}
			Collections.sort(extendedLanguages);
		}
		return extendedLanguages;
	}

	public static List<Language> getCommonLanguages() throws SAXException, IOException, ParserConfigurationException {
		if (registry == null) {
			registry = new RegistryParser(Language.class.getResource("language-subtag-registry.txt"));
		}
		if (languages == null) {
			languages = new Vector<>();
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

	public static Language getLanguage(String code) throws IOException {
		if (registry == null) {
			registry = new RegistryParser(Language.class.getResource("language-subtag-registry.txt"));
		}
		String description = registry.getTagDescription(code);
		if (description != null) {
			return new Language(code, description);
		}
		return null;
	}

	public static Language languageFromName(String description)
			throws SAXException, IOException, ParserConfigurationException {
		List<Language> list = getAllLanguages();
		Iterator<Language> it = list.iterator();
		while (it.hasNext()) {
			Language l = it.next();
			if (l.getDescription().equals(description)) {
				return l;
			}
		}
		return null;
	}

	public static String normalizeCode(String code) throws IOException {
		if (registry == null) {
			registry = new RegistryParser(Language.class.getResource("language-subtag-registry.txt"));
		}
		return registry.normalizeCode(code);
	}

	public static boolean isBiDi(String code) throws SAXException, IOException, ParserConfigurationException {
		if (bidiCodes == null) {
			getAllLanguages();
		}
		return bidiCodes.contains(code);
	}

	public static boolean isCJK(String code) {
		return code.startsWith("zh") || code.startsWith("ja") || code.startsWith("ko") || code.startsWith("vi")
				|| code.startsWith("ain") || code.startsWith("aib");
	}

	public static String[] getLanguageNames() throws SAXException, IOException, ParserConfigurationException {
		Set<String> set = new TreeSet<>();
		List<Language> list = getCommonLanguages();
		Iterator<Language> it = list.iterator();
		while (it.hasNext()) {
			set.add(it.next().getDescription());
		}
		return set.toArray(new String[set.size()]);
	}
}
