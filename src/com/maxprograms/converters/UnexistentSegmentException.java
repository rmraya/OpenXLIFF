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
package com.maxprograms.converters;

public class UnexistentSegmentException extends Exception {

	private static final long serialVersionUID = 5805312863997725508L;

	public UnexistentSegmentException() {
		super();
	}

	@Override
	public String getLocalizedMessage() {
		return "Requested segment does not exist in skeleton file.";
	}

	@Override
	public String getMessage() {
		return "Requested segment does not exist in skeleton file.";
	}

	public UnexistentSegmentException(String message) {
		super(message);
	}

}
