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

public class SkipException extends Exception {

	private Throwable cause;

	private static final long serialVersionUID = -7962180724167896228L;

	public SkipException(String cause) {
		this.cause = new Throwable(cause);
	}

	@Override
	public synchronized Throwable getCause() {
		return cause;
	}
}
