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
package com.maxprograms.languages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class OfficeParser {
    
    private Map<String, String> languageMap;
    
    public OfficeParser() throws IOException {
        languageMap = new HashMap<>();        
        URL url = RegistryParser.class.getResource("Office.txt");
        loadMap(url);
    }

    public String getLCID(String lang) {
        return languageMap.containsKey(lang) ? languageMap.get(lang) : "";
    }

    public boolean isSupported(String lang) {
        return !getLCID(lang).isEmpty(); 
    }

    private void loadMap(URL url) throws IOException {
        try (InputStream input = url.openStream()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_16LE))) {
                String line = "";
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\t");
                    languageMap.put(parts[1], parts[0]);
                }
            }
        }
    }
}
