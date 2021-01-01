/*******************************************************************************
 * Copyright (c)  Maxprograms.
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

public class Script implements Comparable<Script> {

	private String code;
	private String description;

	public Script(String code, String description) {
		this.code = code;
		this.description = description;
	}

	public String getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public int compareTo(Script arg0) {
		return description.compareTo(arg0.getDescription());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj instanceof Script) {
			Script s = (Script) obj;
			return code.equals(s.getCode()) && description.equals(s.getDescription());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return code.hashCode();
	}
}
