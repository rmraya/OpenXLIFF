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

package com.maxprograms.converters.json;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonConfig {

    Map<String, JSONObject> translatableKeys;
    List<String> ignorableKeys;;

    private JsonConfig() {
        translatableKeys = new HashMap<>();
        ignorableKeys = new Vector<>();
    }

    public static JsonConfig parseFile(String configFile) throws FileNotFoundException, IOException, JSONException {
        JsonConfig config = new JsonConfig();
        StringBuilder sb = new StringBuilder();
        String line = "";
        boolean first = true;
        try (FileReader reader = new FileReader(configFile)) {
            try (BufferedReader buffer = new BufferedReader(reader)) {
                while ((line = buffer.readLine()) != null) {
                    if (!first) {
                        sb.append('\n');
                        first = false;
                    }
                    sb.append(line);
                }
            }
        }
        JSONObject configObject = new JSONObject(sb.toString());
        JSONArray translatableArray = configObject.getJSONArray("translatable");
        for (int i = 0; i < translatableArray.length(); i++) {
            JSONObject translatable = translatableArray.getJSONObject(i);
            if (translatable.has("sourceKey")) {
                String sourceKey = translatable.getString("sourceKey");
                config.translatableKeys.put(sourceKey, translatable);
            } else {
                throw new IOException("Missing \"sourceKey\" in configuration object");
            }
        }
        JSONArray ignorableArray = configObject.getJSONArray("ignorable");
        for (int i = 0; i < ignorableArray.length(); i++) {
            config.ignorableKeys.add(ignorableArray.getString(i));
        }
        return config;
    }

    public Set<String> getSourceKeys() {
        return translatableKeys.keySet();
    }

    public List<String> getIgnorableKeys() {
        return ignorableKeys;   
    }
}
