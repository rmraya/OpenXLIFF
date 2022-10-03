/*******************************************************************************
 * Copyright (c) 2022 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/
package com.maxprograms.xml;

public interface ContentParticle {

    public static final int PCDATA = 0;
    public static final int NAME = 1;
    public static final int SEQUENCE = 2;
    public static final int CHOICE = 3;

    public int getType();
    public void setCardinality(int cardinality);
    public int getCardinality();
    @Override
	public String toString();
}
