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

import com.maxprograms.languages.Language;
import com.maxprograms.languages.LanguageUtils;

import org.json.JSONArray;
import org.json.JSONObject;

public class YandexTranslator implements MTEngine {

    private String apiKey;
    private String srcLang;
    private String tgtLang;
    private List<String> directions;
    private List<Language> languages;

    public YandexTranslator(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String getName() {
        return "Yandex Translate API";
    }

    @Override
    public String getShortName() {
        return "Yandex";
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
                .uri(URI.create("https://translate.yandex.net/api/v1.5/tr.json/getLangs?key=" + apiKey + "&ui=en"))
                .build();
        HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String body = response.body();
            if (body != null) {
                JSONObject json = new JSONObject(body);
                directions = new ArrayList<>();
                JSONArray dirs = json.getJSONArray("dirs");
                for (int i = 0; i < dirs.length(); i++) {
                    directions.add(dirs.getString(i));
                }
                JSONObject langs = json.getJSONObject("langs");
                String[] codes = JSONObject.getNames(langs);
                languages = new ArrayList<>();
                for (int i = 0; i < codes.length; i++) {
                    languages.add(LanguageUtils.getLanguage(codes[i]));
                }
            }
        }
    }

    public List<String> getDirections() throws IOException, InterruptedException {
        if (directions == null) {
            getLanguages();
        }
        return directions;
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
                .uri(URI.create("https://translate.yandex.net/api/v1.5/tr.json/translate?key=" + apiKey + "&lang="
                        + srcLang + "-" + tgtLang + "&text=" + URLEncoder.encode(source, StandardCharsets.UTF_8)))
                .build();

        HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String body = response.body();
            if (body != null) {
                JSONObject json = new JSONObject(body);
                int code = json.getInt("code");
                if (code == 200) {
                    JSONArray array = json.getJSONArray("text");
                    return array.getString(0);
                }
                if (code == 413) {
                    throw new IOException("Text size exceeds the maximum");
                }
                if (code == 422) {
                    throw new IOException("The text could not be translated");
                }
                if (code == 501) {
                    if (directions == null) {
                        getLanguages();
                    }
                    if (!directions.contains(srcLang + "-" + tgtLang)) {
                        throw new IOException("The specified translation direction is not supported");
                    }
                }
                throw new IOException("Status code: " + code);
            }
            throw new IOException("Null response received");
        }
        throw new IOException("Server status code: " + response.statusCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof YandexTranslator yt) {
            return srcLang.equals(yt.getSourceLanguage()) && tgtLang.equals(yt.getTargetLanguage());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return YandexTranslator.class.getName().hashCode();
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