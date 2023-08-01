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
package com.maxprograms.languages;

import java.io.IOException;
import java.text.Collator;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
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
		if (extendedLanguages == null) {
			extendedLanguages = new Vector<>();
			bidiCodes = new TreeSet<>();
			SAXBuilder builder = new SAXBuilder();
			Locale locale = Locale.getDefault();
			String language = locale.getLanguage();
			String resource = "extendedLanguageList_" + language + ".xml";
			if (LanguageUtils.class.getResourceAsStream(resource) == null) {
				resource = "extendedLanguageList_" + language.substring(0, 2) + ".xml";
			}
			if (LanguageUtils.class.getResourceAsStream(resource) == null) {
				resource = "extendedLanguageList.xml";
			}
			Element root = builder.build(LanguageUtils.class.getResource(resource)).getRootElement();
			List<Element> children = root.getChildren();
			Iterator<Element> it = children.iterator();
			while (it.hasNext()) {
				Element lang = it.next();
				String code = lang.getAttributeValue("code");
				String description = lang.getText();
				extendedLanguages.add(new Language(code, description));
				if ("true".equals(lang.getAttributeValue("bidi"))) {
					bidiCodes.add(code);
				}
			}
			Collator collator = Collator.getInstance(Locale.getDefault());
			Collections.sort(extendedLanguages, (l1, l2) -> collator.compare(l1.getDescription(), l2.getDescription()));
		}
		return extendedLanguages;
	}

	public static List<Language> getCommonLanguages() throws SAXException, IOException, ParserConfigurationException {
		if (languages == null) {
			languages = new Vector<>();
			SAXBuilder builder = new SAXBuilder();
			Locale locale = Locale.getDefault();
			String language = locale.getLanguage();
			String resource = "languageList_" + language + ".xml";
			if (LanguageUtils.class.getResourceAsStream(resource) == null) {
				resource = "languageList_" + language.substring(0, 2) + ".xml";
			}
			if (LanguageUtils.class.getResourceAsStream(resource) == null) {
				resource = "languageList.xml";
			}
			Element root = builder.build(LanguageUtils.class.getResource(resource)).getRootElement();
			List<Element> children = root.getChildren();
			Iterator<Element> it = children.iterator();
			while (it.hasNext()) {
				Element lang = it.next();
				String code = lang.getAttributeValue("code");
				String description = lang.getText();
				languages.add(new Language(code, description));
			}
			Collator collator = Collator.getInstance(Locale.getDefault());
			Collections.sort(languages, (l1, l2) -> collator.compare(l1.getDescription(), l2.getDescription()));
		}
		return languages;
	}

	public static Language getLanguage(String code) throws IOException, SAXException, ParserConfigurationException {
		List<Language> list = getAllLanguages();
		Iterator<Language> it = list.iterator();
		while (it.hasNext()) {
			Language l = it.next();
			if (l.getCode().equals(code)) {
				return l;
			}
		}
		if (registry == null) {
			registry = new RegistryParser(LanguageUtils.class.getResource("language-subtag-registry.txt"));
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
			registry = new RegistryParser(LanguageUtils.class.getResource("language-subtag-registry.txt"));
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
		List<Language> list = getCommonLanguages();
		Iterator<Language> it = list.iterator();
		List<String> result = new Vector<>();
		while (it.hasNext()) {
			result.add(it.next().getDescription());
		}
		return result.toArray(new String[result.size()]);
	}
}
