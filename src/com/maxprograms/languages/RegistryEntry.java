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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RegistryEntry {

	private Map<String, String> table;

	public RegistryEntry(String entry) {
		table = new HashMap<>();
		parseEntry(entry);
	}

	private void parseEntry(String entry) {
		String[] lines = entry.split("\n");
		for (int i = 0; i < lines.length; i++) {
			String type = lines[i].substring(0, lines[i].indexOf(':')).trim();
			String value = lines[i].substring(lines[i].indexOf(':') + 1).trim();
			if (!table.containsKey(type)) {
				table.put(type, value);
			} else {
				table.put(type, table.get(type) + " | " + value);
			}
		}
	}

	public Set<String> getTypes() {
		return table.keySet();
	}

	public String get(String string) {
		return table.get(string);
	}

	public String getType() {
		if (table.containsKey("Type")) {
			return table.get("Type");
		}
		return null;
	}

	public String getDescription() {
		if (table.containsKey("Description")) {
			return table.get("Description");
		}
		return null;
	}

	public String getSubtag() {
		if (table.containsKey("Subtag")) {
			return table.get("Subtag");
		}
		return null;
	}
}
