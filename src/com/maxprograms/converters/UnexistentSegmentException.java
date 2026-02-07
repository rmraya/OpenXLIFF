/*******************************************************************************
 * Copyright (c) 2018 - 2026 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/
package com.maxprograms.converters;

public class UnexistentSegmentException extends Exception {

	private static final long serialVersionUID = 5805312863997725508L;

	public UnexistentSegmentException() {
		super();
	}

	@Override
	public String getLocalizedMessage() {
		return Messages.getString("UnexistentSegmentException.1");
	}

	@Override
	public String getMessage() {
		return Messages.getString("UnexistentSegmentException.1");
	}

	public UnexistentSegmentException(String message) {
		super(message);
	}

}
