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
import java.util.ArrayList;
import java.util.List;

import com.maxprograms.languages.Language;
import com.maxprograms.languages.LanguageUtils;

import org.json.JSONArray;
import org.json.JSONObject;

public class DeepLTranslator implements MTEngine {

	private String apiKey;
	private String srcLang;
	private String tgtLang;

	private List<Language> srcLanguages;
	private List<Language> tgtLanguages;

	public DeepLTranslator(String apiKey) {
		this.apiKey = apiKey;
	}

	@Override
	public String getName() {
		return "DeepL API";
	}

	@Override
	public String getShortName() {
		return "DeepL";
	}

	@Override
	public List<Language> getSourceLanguages() throws IOException {
		if (srcLanguages == null) {
			srcLanguages = new ArrayList<>();
			String[] codes = { "de", "en", "fr", "it", "ja", "es", "nl", "pl", "pt", "ru", "zh" };
			for (int i = 0; i < codes.length; i++) {
				srcLanguages.add(LanguageUtils.getLanguage(codes[i]));
			}
		}
		return srcLanguages;
	}

	@Override
	public List<Language> getTargetLanguages() throws IOException {
		if (tgtLanguages == null) {
			tgtLanguages = new ArrayList<>();
			String[] codes = { "de", "en", "fr", "it", "ja", "es", "nl", "pl", "pt", "pt-BR", "ru", "zh" };
			for (int i = 0; i < codes.length; i++) {
				tgtLanguages.add(LanguageUtils.getLanguage(codes[i]));
			}
		}
		return tgtLanguages;
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
				.uri(URI.create("https://api.deepl.com/v1/translate?auth_key=" + apiKey + "&text="
						+ URLEncoder.encode(source, StandardCharsets.UTF_8) + "&source_lang=" + srcLang.toUpperCase()
						+ "&target_lang=" + tgtLang.toUpperCase()))
				.build();
		HttpResponse<String> response = httpclient.send(request, BodyHandlers.ofString());

		if (response.statusCode() == 200) {
			String body = response.body();
			if (body != null) {
				JSONObject json = new JSONObject(body);
				JSONArray array = json.getJSONArray("translations");
				String target = array.getJSONObject(0).getString("text");
				return target;
			}
			throw new IOException("Null response received");
		}
		switch (response.statusCode()) {
			case 400:
				throw new IOException("Bad request. Please check error message and your parameters.");
			case 403:
				throw new IOException("Authorization failed. Please supply a valid auth_key parameter.");
			case 404:
				throw new IOException("The requested resource could not be found.");
			case 413:
				throw new IOException("The request size exceeds the limit.");
			case 429:
				throw new IOException("Too many requests. Please wait and resend your request.");
			case 456:
				throw new IOException("Quota exceeded. The character limit has been reached.");
			case 503:
				throw new IOException("Resource currently unavailable. Try again later.");
			default:
				throw new IOException("Server status code: " + response.statusCode());
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DeepLTranslator) {
			DeepLTranslator dl = (DeepLTranslator) obj;
			return srcLang.equals(dl.getSourceLanguage()) && tgtLang.equals(dl.getTargetLanguage());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return DeepLTranslator.class.getName().hashCode();
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