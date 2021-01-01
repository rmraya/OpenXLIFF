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

package com.maxprograms.mt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.maxprograms.languages.Language;
import com.maxprograms.languages.LanguageUtils;

import org.json.JSONArray;
import org.json.JSONObject;

public class AzureTranslator implements MTEngine {

    private static final String BASEURL = "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0";

    private String apiKey;
    private String srcLang;
    private String tgtLang;
    List<Language> languages;

    public AzureTranslator(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String getName() {
        return "Azure Translator Text";
    }

    @Override
    public String getShortName() {
        return "Azure";
    }

    @Override
    public List<Language> getSourceLanguages() throws IOException {
        if (languages == null) {
            getLanguages();
        }
        return languages;
    }

    private void getLanguages() throws IOException {
        URL url = new URL("https://api.cognitive.microsofttranslator.com/languages?api-version=3.0&scope=translation");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        byte[] bytes = new byte[10240];
        String result = null;
        try (InputStream in = conn.getInputStream()) {
            int read = in.read(bytes);
            result = new String(bytes, 0, read);
        }
        conn.disconnect();
        JSONObject json = new JSONObject(result);
        JSONObject translation = json.getJSONObject("translation");
        String[] codes = JSONObject.getNames(translation);
        languages = new ArrayList<>();
        for (int i = 0; i < codes.length; i++) {
            languages.add(LanguageUtils.getLanguage(codes[i]));
        }
    }

    @Override
    public List<Language> getTargetLanguages() throws IOException {
        if (languages == null) {
            getLanguages();
        }
        return languages;
    }

    @Override
    public void setSourceLanguage(String lang) {
        srcLang = lang;
    }

    @Override
    public void setTargetLanguage(String lang) {
        tgtLang = lang;
    }

    @Override
    public String translate(String source) throws IOException {
        URL url = new URL(BASEURL + "&from=" + srcLang + "&to=" + tgtLang + "&textType=plain");

        JSONArray array = new JSONArray();
        JSONObject object = new JSONObject();
        object.put("Text", source);
        array.put(object);

        byte[] bytesOut = array.toString().getBytes(StandardCharsets.UTF_8);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Ocp-Apim-Subscription-Key", apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Content-Length", "" + bytesOut.length);
        conn.setDoInput(true);
        conn.setDoOutput(true);

        try (OutputStream cout = conn.getOutputStream()) {
            cout.write(bytesOut);
            cout.flush();
        }
        byte[] bytes = new byte[10240];
        String result = null;
        try (InputStream in = conn.getInputStream()) {
            int read = in.read(bytes);
            result = new String(bytes, 0, read, StandardCharsets.UTF_8);
        }
        conn.disconnect();
        JSONArray json = new JSONArray(result);
        return json.getJSONObject(0).getJSONArray("translations").getJSONObject(0).getString("text");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AzureTranslator) {
            AzureTranslator az = (AzureTranslator) obj;
            return srcLang.equals(az.getSourceLanguage()) && tgtLang.equals(az.getTargetLanguage());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return AzureTranslator.class.getName().hashCode();
    }
    
    
    @Override
    public String getSourceLanguage() {
        return srcLang;
    }

    @Override
    public String getTargetLanguage() {
        return tgtLang;
    }

}