/*******************************************************************************
 * Copyright (c) 2003-2019 Maxprograms.
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

public class Variant implements Comparable<Variant> {

	private String code;
	private String description;
	private String prefix;

	public Variant(String code, String description, String prefix) {
		this.code = code;
		this.description = description;
		this.prefix = prefix;
	}

	public String getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}

	public String getPrefix() {
		return prefix;
	}

	@Override
	public int compareTo(Variant arg0) {
		return description.compareTo(arg0.getDescription());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj instanceof Variant) {
			Variant s = (Variant) obj;
			return code.equals(s.getCode()) && description.equals(s.getDescription()) && prefix.equals(s.getPrefix());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return code.hashCode();
	}
}
