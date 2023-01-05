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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RegistryParser {

	private List<RegistryEntry> entries;
	private Map<String, Language> languages;
	private Map<String, Region> regions;
	private Map<String, Script> scripts;
	private Map<String, Variant> variants;

	private void parseRegistry(URL url) throws IOException {
		try (InputStream input = url.openStream()) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
				String line = "";
				entries = new ArrayList<>();
				StringBuilder buffer = new StringBuilder();
				while ((line = reader.readLine()) != null) {
					if (line.trim().equals("%%")) {
						entries.add(new RegistryEntry(buffer.toString().replace("\n  ", " ")));
						buffer = new StringBuilder();
					} else {
						buffer.append(line);
						buffer.append('\n');
					}
				}
			}
		}
		languages = new HashMap<>();
		regions = new HashMap<>();
		scripts = new HashMap<>();
		variants = new HashMap<>();
		Iterator<RegistryEntry> it = entries.iterator();
		while (it.hasNext()) {
			RegistryEntry entry = it.next();
			String type = entry.getType();
			if (type == null) {
				continue;
			}
			if (type.equals("language")) {
				String description = entry.getDescription();
				if (description.indexOf("Private use") != -1) {
					continue;
				}
				String subtag = entry.getSubtag();
				if (subtag != null) {
					if (description.indexOf('|') != -1) {
						// trim and use only the first name
						description = description.substring(0, description.indexOf('|') - 1);
					}
					if (subtag.equals("el")) {
						// official description is "Modern Greek (1453-)", use a familiar name
						description = "Greek";
					}
					description = description.replaceAll("\\(.*\\)", "");
					Language lang = new Language(subtag, description);
					String suppressedScript = entry.get("Suppress-Script");
					if (suppressedScript != null) {
						lang.setSuppressedScript(suppressedScript);
					}
					languages.put(subtag, lang);
				}
			}
			if (type.equals("region")) {
				String description = entry.getDescription();
				if (description.indexOf("Private use") != -1) {
					continue;
				}
				String subtag = entry.getSubtag();
				if (subtag != null) {
					regions.put(subtag, new Region(subtag, description));
				}
			}
			if (type.equals("script")) {
				String description = entry.getDescription();
				if (description.indexOf("Private use") != -1) {
					continue;
				}
				description = description.replace('(', '[');
				description = description.replace(')', ']');
				String subtag = entry.getSubtag();
				if (subtag != null) {
					scripts.put(subtag, new Script(subtag, description));
				}
			}
			if (type.equals("variant")) {
				String description = entry.getDescription();
				if (description.indexOf("Private use") != -1) {
					continue;
				}
				description = description.replace('(', '[');
				description = description.replace(')', ']');
				String subtag = entry.getSubtag();
				String prefix = entry.get("Prefix");
				if (subtag != null) {
					variants.put(subtag, new Variant(subtag, description, prefix));
				}
			}
		}
	}

	public String getRegistryDate() {
		Iterator<RegistryEntry> it = entries.iterator();
		while (it.hasNext()) {
			RegistryEntry entry = it.next();
			Set<String> set = entry.getTypes();
			if (set.contains("File-Date")) {
				return entry.get("File-Date");
			}
		}
		return null;
	}

	public RegistryParser(URL url) throws IOException {
		parseRegistry(url);
	}

	public RegistryParser() throws IOException {
		URL url = RegistryParser.class.getResource("language-subtag-registry.txt");
		parseRegistry(url);
	}

	public String getTagDescription(String tag) {
		String[] parts = tag.split("-");
		if (parts.length == 1) {
			// language part only
			if (languages.containsKey(tag.toLowerCase())) {
				return languages.get(tag.toLowerCase()).getDescription();
			}
		} else if (parts.length == 2) {
			// contains either script or region
			if (!languages.containsKey(parts[0].toLowerCase())) {
				return "";
			}
			Language lang = languages.get(parts[0].toLowerCase());
			if (parts[1].length() == 2 && regions.containsKey(parts[1].toUpperCase())) {
				// could be a country code
				return lang.getDescription() + " (" + regions.get(parts[1].toUpperCase()).getDescription() + ")";
			}
			if (parts[1].length() == 3 && regions.containsKey(parts[1])) {
				// could be a UN region code
				Region reg = regions.get(parts[1]);
				return lang.getDescription() + " (" + reg.getDescription() + ")";
			}
			if (parts[1].length() == 4) {
				// could have script
				String script = parts[1].substring(0, 1).toUpperCase() + parts[1].substring(1).toLowerCase();
				if (script.equals(lang.getSuppresedScript())) {
					return "";
				}
				if (scripts.containsKey(script)) {
					return lang.getDescription() + " (" + scripts.get(script).getDescription() + ")";
				}
			}
			// try with a variant
			if (variants.containsKey(parts[1].toLowerCase())) {
				Variant variant = variants.get(parts[1].toLowerCase());
				if (variant != null && variant.getPrefix().equals(parts[0].toLowerCase())) {
					// variant is valid for the language code
					return lang.getDescription() + " (" + variant.getDescription() + ")";
				}
			}
		} else if (parts.length == 3) {
			if (!languages.containsKey(parts[0].toLowerCase())) {
				return "";
			}
			Language lang = languages.get(parts[0].toLowerCase());
			if (parts[1].length() == 4) {
				// could be script + region or variant
				String script = parts[1].substring(0, 1).toUpperCase() + parts[1].substring(1).toLowerCase();
				if (script.equals(lang.getSuppresedScript())) {
					return "";
				}
				if (scripts.containsKey(script)) {
					Script scr = scripts.get(script);
					// check if next part is a region or variant
					if (regions.containsKey(parts[2].toUpperCase())) {
						// check if next part is a variant
						Region reg = regions.get(parts[2].toUpperCase());
						return lang.getDescription() + " (" + scr.getDescription() + ", " + reg.getDescription() + ")";
					}
					if (variants.containsKey(parts[2].toLowerCase())) {
						Variant variant = variants.get(parts[2].toLowerCase());
						if (variant != null && variant.getPrefix().equals(parts[0].toLowerCase())) {
							// variant is valid for the language code
							return lang.getDescription() + " (" + scr.getDescription() + ", " + variant.getDescription()
									+ ")";
						}
					}
				}
			} else {
				// could be region + variant
				if ((parts[1].length() == 2 || parts[1].length() == 3) && regions.containsKey(parts[1].toUpperCase())) {
					// could be a region code, check if next part is a variant
					Region reg = regions.get(parts[1].toUpperCase());
					if (variants.containsKey(parts[2].toLowerCase())) {
						Variant variant = variants.get(parts[2].toLowerCase());
						if (variant != null && variant.getPrefix().equals(parts[0].toLowerCase())) {
							// variant is valid for the language code
							return lang.getDescription() + " (" + reg.getDescription() + " - "
									+ variant.getDescription() + ")";
						}
					}
				}
			}
		}
		return "";
	}

	public String normalizeCode(String code) {
		String[] parts = code.split("-");
		if (parts.length == 1) {
			// language part only
			if (languages.containsKey(code.toLowerCase())) {
				return code.toLowerCase();
			}
		} else if (parts.length == 2) {
			// contains either script or region
			if (!languages.containsKey(parts[0].toLowerCase())) {
				return "";
			}
			Language lang = languages.get(parts[0].toLowerCase());
			if (parts[1].length() == 2 && regions.containsKey(parts[1].toUpperCase())) {
				// could be a country code
				return parts[0].toLowerCase() + "-" + parts[1].toUpperCase();
			}
			if (parts[1].length() == 3 && regions.containsKey(parts[1])) {
				// could be a UN region code
				return parts[0].toLowerCase() + "-" + parts[1];
			}
			if (parts[1].length() == 4) {
				// could have script
				String script = parts[1].substring(0, 1).toUpperCase() + parts[1].substring(1).toLowerCase();
				if (script.equals(lang.getSuppresedScript())) {
					return "";
				}
				if (scripts.containsKey(script)) {
					return parts[0].toLowerCase() + "-" + script;
				}
			}
			// try with a variant
			if (variants.containsKey(parts[1].toLowerCase())) {
				Variant variant = variants.get(parts[1].toLowerCase());
				if (variant != null && variant.getPrefix().equals(parts[0].toLowerCase())) {
					// variant is valid for the language code
					return parts[0].toLowerCase() + "-" + variant.getCode();
				}
			}
		} else if (parts.length == 3) {
			if (!languages.containsKey(parts[0].toLowerCase())) {
				return "";
			}
			Language lang = languages.get(parts[0].toLowerCase());
			if (parts[1].length() == 4) {
				// could be script + region or variant
				String script = parts[1].substring(0, 1).toUpperCase() + parts[1].substring(1).toLowerCase();
				if (script.equals(lang.getSuppresedScript())) {
					return "";
				}
				if (scripts.containsKey(script)) {
					Script scr = scripts.get(script);
					// check if next part is a region or variant
					if (regions.containsKey(parts[2].toUpperCase())) {
						// check if next part is a variant
						Region reg = regions.get(parts[2].toUpperCase());
						return lang.getCode() + "-" + scr.getCode() + "-" + reg.getCode();
					}
					if (variants.containsKey(parts[2].toLowerCase())) {
						Variant variant = variants.get(parts[2].toLowerCase());
						if (variant != null && variant.getPrefix().equals(parts[0].toLowerCase())) {
							// variant is valid for the language code
							return lang.getCode() + "-" + scr.getCode() + "-" + variant.getCode();
						}
					}
				}
			} else {
				// could be region + variant
				if ((parts[1].length() == 2 || parts[1].length() == 3) && regions.containsKey(parts[1].toUpperCase())) {
					// could be a region code, check if next part is a variant
					Region reg = regions.get(parts[1].toUpperCase());
					if (variants.containsKey(parts[2].toLowerCase())) {
						Variant variant = variants.get(parts[2].toLowerCase());
						if (variant != null && variant.getPrefix().equals(parts[0].toLowerCase())) {
							// variant is valid for the language code
							return lang.getCode() + "-" + reg.getCode() + "-" + variant.getCode();
						}
					}
				}
			}
		}
		return "";
	}

}
