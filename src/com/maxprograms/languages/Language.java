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

import java.io.Serializable;

public class Language implements Comparable<Language>, Serializable {

	private static final long serialVersionUID = -5391793426888923842L;

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
		return LanguageUtils.isBiDi(code);
	}

	public boolean isCJK() {
		return LanguageUtils.isCJK(code);
	}

	@Override
	public int hashCode() {
		return code.hashCode();
	}
}
