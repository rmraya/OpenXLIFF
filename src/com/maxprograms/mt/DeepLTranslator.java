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

package com.maxprograms.mt;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
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
			String[] codes = { "bg", "cs", "da", "de", "el", "en", "es", "et", "fi", "fr", "hu", "it", "ja", "lt", "lv",
					"nl", "pl", "pt", "ro", "ru", "sk", "sl", "sv", "zh" };
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
			String[] codes = { "bg", "cs", "da", "de", "el", "en-GB", "en-US", "es", "et", "fi", "fr", "hu", "it", "ja",
					"lt", "lv", "nl", "pl", "pt-BR", "pt-PT", "ro", "ru", "sk", "sl", "sv", "zh" };
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
				return array.getJSONObject(0).getString("text");
			}
			throw new IOException(Messages.getString("DeepLTranslator.2"));
		}
		switch (response.statusCode()) {
			case 400:
				throw new IOException(Messages.getString("DeepLTranslator.3"));
			case 403:
				throw new IOException(Messages.getString("DeepLTranslator.4"));
			case 404:
				throw new IOException(Messages.getString("DeepLTranslator.5"));
			case 413:
				throw new IOException(Messages.getString("DeepLTranslator.6"));
			case 429:
				throw new IOException(Messages.getString("DeepLTranslator.7"));
			case 456:
				throw new IOException(Messages.getString("DeepLTranslator.8"));
			case 503:
				throw new IOException(Messages.getString("DeepLTranslator.9"));
			default:
				MessageFormat mf = new MessageFormat(Messages.getString("DeepLTranslator.10"));
				throw new IOException(mf.format(new String[] { "" + response.statusCode() }));
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DeepLTranslator dl) {
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