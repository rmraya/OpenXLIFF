/*******************************************************************************
 * Copyright (c) 2003-2020 Maxprograms.
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
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import com.maxprograms.languages.Language;
import com.maxprograms.languages.LanguageUtils;

import org.json.JSONObject;
import org.xml.sax.SAXException;

public class MyMemoryTranslator implements MTEngine {

    private String apiKey;
    private String srcLang;
    private String tgtLang;

    public MyMemoryTranslator(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String getName() {
        return "MyMemory";
    }

    @Override
    public List<Language> getSourceLanguages()
            throws IOException, InterruptedException, SAXException, ParserConfigurationException {
        return LanguageUtils.getCommonLanguages();
    }

    @Override
    public List<Language> getTargetLanguages()
            throws IOException, InterruptedException, SAXException, ParserConfigurationException {
        return LanguageUtils.getCommonLanguages();
    }

    @Override
    public void setSourceLanguage(String lang) {
        srcLang = lang;
    }

    @Override
    public String getSourceLanguage() {
        return srcLang;
    }

    @Override
    public void setTargetLanguage(String lang) {
        tgtLang = lang;
    }

    @Override
    public String getTargetLanguage() {
        return tgtLang;
    }

    @Override
    public String translate(String source) throws IOException, InterruptedException {
        HttpClient httpclient = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mymemory.translated.net/get?q="
                        + URLEncoder.encode(source, StandardCharsets.UTF_8) + (apiKey.isEmpty() ? "" : "&key=" + apiKey)
                        + "&langpair=" + URLEncoder.encode(srcLang + "|" + tgtLang, StandardCharsets.UTF_8)))
                .build();
        HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String body = response.body();
            if (body != null) {
                JSONObject json = new JSONObject(body);
                JSONObject responseData = json.getJSONObject("responseData");
                return responseData.getString("translatedText");
            }
            throw new IOException("Null response received");
        }
        throw new IOException("Server status code: " + response.statusCode());
    }

    @Override
    public boolean equals(MTEngine obj) {
        if (obj instanceof MyMemoryTranslator) {
            return srcLang.equals(obj.getSourceLanguage()) && tgtLang.equals(obj.getTargetLanguage());
        }
        return false;
    }

}