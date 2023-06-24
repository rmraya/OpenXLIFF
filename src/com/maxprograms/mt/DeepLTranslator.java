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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.maxprograms.converters.Constants;
import com.maxprograms.languages.Language;
import com.maxprograms.languages.LanguageUtils;

public class DeepLTranslator implements MTEngine {

	private String apiKey;
	private String srcLang;
	private String tgtLang;
	String domain;

	private List<Language> srcLanguages;
	private List<Language> tgtLanguages;

	public DeepLTranslator(String apiKey, boolean proPlan) {
		this.apiKey = apiKey;
		domain = proPlan ? "https://api.deepl.com/v1/translate" : "https://api-free.deepl.com/v2/translate";
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
			String[] codes = { "bg", "cs", "da", "de", "el", "en", "es", "et", "fi", "fr", "hu", "id", "it", "ja", "ko",
					"lt", "lv", "nb", "nl", "pl", "pt", "ro", "ru", "sk", "sl", "sv", "tr", "uk", "zh" };
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
			String[] codes = { "bg", "cs", "da", "de", "el", "en-GB", "en-US", "es", "et", "fi", "fr", "hu", "id", "it",
					"ja", "ko", "lt", "lv", "nb", "nl", "pl", "pt-BR", "pt-PT", "ro", "ru", "sk", "sl", "sv", "tr",
					"uk", "zh" };
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
	public String translate(String source) throws IOException, InterruptedException, JSONException {
		String data = "&text=" + URLEncoder.encode(source, StandardCharsets.UTF_8) + "&source_lang=" +
				srcLang.toUpperCase() + "&target_lang=" + tgtLang.toUpperCase();
		byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
		URL url = new URL(domain);
		HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Authorization", "DeepL-Auth-Key " + apiKey);
		con.setRequestProperty("User-Agent", Constants.TOOLNAME);
		con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		con.setRequestProperty("Content-Length", Integer.toString(bytes.length));
		con.setDoOutput(true);
		try (OutputStream out = con.getOutputStream()) {
			out.write(bytes);
			out.flush();
		}
		int status = con.getResponseCode();
		if (status == 200) {
			StringBuffer content = new StringBuffer();
			try (InputStreamReader inputStream = new InputStreamReader(con.getInputStream())) {
				try (BufferedReader in = new BufferedReader(inputStream)) {
					String inputLine;
					while ((inputLine = in.readLine()) != null) {
						if (!content.isEmpty()) {
							content.append('\n');
						}
						content.append(inputLine);
					}
				}
			}
			JSONObject json = new JSONObject(content.toString());
			JSONArray array = json.getJSONArray("translations");
			return array.getJSONObject(0).getString("text");
		}
		switch (status) {
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
				throw new IOException(mf.format(new String[] { "" + status }));
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DeepLTranslator dl) {
			return srcLang.equals(dl.getSourceLanguage()) && tgtLang.equals(dl.getTargetLanguage())
					&& apiKey.equals(dl.apiKey) && domain.equals(dl.domain);
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