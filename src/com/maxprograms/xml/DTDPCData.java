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

public class DTDPCData implements ContentParticle {

    @Override
    public int getType() {
        return ContentParticle.PCDATA;
    }

    @Override
    public void setCardinality(int cardinality) {
        // do nothing        
    }

    @Override
    public int getCardinality() {
        return ContentModel.NONE;
    }
    
    @Override
    public String toString() {
        return "#PCDATA";
    }
}
