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
package com.maxprograms.converters.ditamap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Scope {

	private Set<String> names;
	private List<Scope> children;
	private Map<String, Key> keys;

	public Scope(String name) {
		names = new TreeSet<>();
		String[] parts = name.split("\\s");
		names.addAll(Arrays.asList(parts));
		children = new ArrayList<>();
		keys = new Hashtable<>();
	}

	public void addScope(Scope scope) {
		children.add(scope);
	}

	public boolean addKey(Key key) {
		if (!keys.containsKey(key.getName())) {
			keys.put(key.getName(), key);
			return true;
		}
		return false;
	}

	public Key getKey(String string) {
		String key = string;
		if (keys.containsKey(key)) {
			Key k = keys.get(key);
			if (k.getHref() != null) {
				return k;
			}
			return getKey(k.getKeyref());
		}
		if (key.indexOf('.') != -1) {
			String scope = key.substring(0, key.indexOf('.'));
			key = key.substring(scope.length() + 1);
			if (is(scope)) {
				if (keys.containsKey(key)) {
					return keys.get(key);
				}
			} else {
				Iterator<Scope> it = children.iterator();
				while (it.hasNext()) {
					Scope child = it.next();
					if (child.is(scope)) {
						return child.getKey(key);
					}
				}
			}
		}
		Iterator<Scope> it = children.iterator();
		while (it.hasNext()) {
			Scope child = it.next();
			Key k = child.getKey(string);
			if (k != null) {
				return k;
			}
		}
		return null;
	}

	private boolean is(String name) {
		return names.contains(name);
	}

	public Map<String, Key> getKeys() {
		Map<String, Key> result = new Hashtable<>();
		Iterator<String> it = names.iterator();
		while (it.hasNext()) {
			String name = it.next();
			String prefix = "";
			if (!name.isEmpty()) {
				prefix = name + ".";
			}
			Set<String> keySet = keys.keySet();
			Iterator<String> kit = keySet.iterator();
			while (kit.hasNext()) {
				String key = kit.next();
				result.put(prefix + key, keys.get(key));
			}
			Iterator<Scope> sc = children.iterator();
			while (sc.hasNext()) {
				Map<String, Key> table = sc.next().getKeys();
				Set<String> set = table.keySet();
				Iterator<String> st = set.iterator();
				while (st.hasNext()) {
					String s = st.next();
					result.put(prefix + s, table.get(s));
				}
			}
		}
		return result;
	}
}
