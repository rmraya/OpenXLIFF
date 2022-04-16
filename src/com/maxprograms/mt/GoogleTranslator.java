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

package com.maxprograms.mt;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.maxprograms.languages.Language;
import com.maxprograms.languages.LanguageUtils;

import org.json.JSONArray;
import org.json.JSONObject;

public class GoogleTranslator implements MTEngine {

    private String apiKey;
    private String srcLang;
    private String tgtLang;
    private boolean neural;
    List<Language> languages;

    public GoogleTranslator(String apiKey, boolean neural) {
        this.apiKey = apiKey;
        this.neural = neural;
    }

    @Override
    public String getName() {
        return "Google Cloud Translation";
    }

    @Override
    public String getShortName() {
        return "Google";
    }

    @Override
    public List<Language> getSourceLanguages() throws IOException, InterruptedException {
        if (languages == null) {
            getLanguages();
        }
        return languages;
    }

    @Override
    public List<Language> getTargetLanguages() throws IOException, InterruptedException {
        if (languages == null) {
            getLanguages();
        }
        return languages;
    }

    private void getLanguages() throws IOException, InterruptedException {
        HttpClient httpclient = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://translation.googleapis.com/language/translate/v2/languages?key=" + apiKey
                        + "&model=" + (neural ? "nmt" : "base")))
                .build();
        HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String body = response.body();
            if (body != null) {
                JSONObject json = new JSONObject(body);
                JSONObject data = json.getJSONObject("data");
                JSONArray langArray = data.getJSONArray("languages");
                languages = new ArrayList<>();
                for (int i = 0; i < langArray.length(); i++) {
                    JSONObject lang = langArray.getJSONObject(i);
                    languages.add(LanguageUtils.getLanguage(lang.getString("language")));
                }
            }
        }
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
    public String translate(String source) throws IOException, InterruptedException {
        HttpClient httpclient = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/language/translate/v2?key=" + apiKey + "&q="
                        + URLEncoder.encode(source, StandardCharsets.UTF_8) + "&source=" + srcLang + "&target="
                        + tgtLang + "&model=" + (neural ? "nmt" : "base")))
                .build();
        HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String body = response.body();
            if (body != null) {
                JSONObject json = new JSONObject(body);
                JSONObject data = json.getJSONObject("data");
                JSONArray array = data.getJSONArray("translations");
                return removeEntities(array.getJSONObject(0).getString("translatedText"));
            }
            throw new IOException("Null response received");
        }
        throw new IOException("Server status code: " + response.statusCode());
    }

    private static String removeEntities(String string) {
        String result = string;
        Pattern p = Pattern.compile("\\&\\#[0-9]+\\;");
        Matcher m = p.matcher(result);
        while (m.find()) {
            int from = m.start();
            int to = m.end();
            String start = result.substring(0, from);
            String entity = result.substring(from + 2, to - 1);
            String rest = result.substring(to);
            result = start + new String(Character.toChars(Integer.parseInt(entity))) + rest;
            m = p.matcher(result);
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GoogleTranslator gt) {
            return srcLang.equals(gt.getSourceLanguage()) && tgtLang.equals(gt.getTargetLanguage());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return GoogleTranslator.class.getName().hashCode();
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