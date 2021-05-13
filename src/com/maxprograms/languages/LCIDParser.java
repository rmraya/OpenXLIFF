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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class LCIDParser {

    public static final int UNKNOWN = 0x1000;

    private static HashMap<String, Integer> languageMap;

    public LCIDParser() throws IOException {
        URL url = RegistryParser.class.getResource("LCID.txt");
        loadMap(url);
    }

    public int getLCID(String lang) {
        return languageMap.containsKey(lang) ? languageMap.get(lang) : UNKNOWN;
    }

    private static void loadMap(URL url) throws IOException {
        languageMap = new HashMap<>();
        try (InputStream input = url.openStream()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_16LE))) {
                String line = "";
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\t");
                    String hex = parts[0].substring(parts[0].indexOf("x") + 1);
                    languageMap.put(parts[1], Integer.valueOf(hex, 16));
                }
            }
        }
    }
}
