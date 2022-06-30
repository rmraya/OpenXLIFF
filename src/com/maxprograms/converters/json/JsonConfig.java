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
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonConfig {

    public static final String SOURCEKEY = "sourceKey";
    public static final String TARGETKEY = "targetKey";
    public static final String IDKEY = "idKey";
    public static final String RESNAMEKEY = "resnameKey";
    public static final String NOTEKEY = "noteKey";
    public static final String REPLICATE = "replicateNotes";

    Map<String, JSONObject> translatableKeys;
    List<String> ignorableKeys;
    List<String> sourceKeys;

    private JsonConfig() {
        translatableKeys = new HashMap<>();
        ignorableKeys = new Vector<>();
        sourceKeys = new Vector<>();
    }

    public static JsonConfig parseFile(String configFile) throws IOException, JSONException {
        JsonConfig config = new JsonConfig();
        StringBuilder sb = new StringBuilder();
        String line = "";
        boolean first = true;
        try (FileReader reader = new FileReader(configFile)) {
            try (BufferedReader buffer = new BufferedReader(reader)) {
                while ((line = buffer.readLine()) != null) {
                    if (!first) {
                        sb.append('\n');
                    }
                    sb.append(line);
                    first = false;
                }
            }
        }
        JSONObject configObject = new JSONObject(sb.toString());
        JSONArray translatableArray = configObject.getJSONArray("translatable");
        for (int i = 0; i < translatableArray.length(); i++) {
            JSONObject translatable = translatableArray.getJSONObject(i);
            if (translatable.has(SOURCEKEY)) {
                String sourceKey = translatable.getString(SOURCEKEY);
                config.translatableKeys.put(sourceKey, translatable);
                config.sourceKeys.add(sourceKey);
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

    public List<String> getSourceKeys() {
        return sourceKeys;
    }

    public List<String> getIgnorableKeys() {
        return ignorableKeys;
    }

    public JSONObject getConfiguration(String sourceKey) {
        return translatableKeys.get(sourceKey);
    }
}
