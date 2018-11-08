/*******************************************************************************
 * Copyright (c) 2003, 2018 Maxprograms.
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

	public String getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}

	private String code;
	private String description;
	private String prefix;

	public Variant(String code, String description, String prefix) {
		this.code = code;
		this.description = description;
		this.prefix = prefix;
	}

	public String getPrefix() {
		return prefix;
	}

	@Override
	public int compareTo(Variant arg0) {
		return description.compareTo(arg0.getDescription());
	}

}
