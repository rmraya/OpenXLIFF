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

public class DTDName implements ContentParticle {

    private String name;
    private int cardinality;

    public DTDName(String name) {
        this.name = name;
        this.cardinality = ContentModel.NONE;
    }

    @Override
    public int getType() {
        return ContentParticle.NAME;
    }

    @Override
    public int getCardinality() {
        return cardinality;
    }

    @Override
    public String toString() {
        switch (cardinality) {
            case ContentModel.NONE:
                return name;
            case ContentModel.OPTIONAL:
                return name + "?";
            case ContentModel.ONEMANY:
                return name + "+";
            case ContentModel.ZEROMANY:
                return name + "*";
            default:
                // ignore
                return "";
        }
    }

    @Override
    public void setCardinality(int cardinality) {
        this.cardinality = cardinality;
    }
}
