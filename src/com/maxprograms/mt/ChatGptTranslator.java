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
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.maxprograms.languages.Language;
import com.maxprograms.languages.LanguageUtils;

public class ChatGptTranslator implements MTEngine {

    public static final String DAVINCI = "text-davinci-003";
    public static final String CURIE = "text-curie-001";
    public static final String BABBAGE = "text-babbage-001";
    public static final String ADA = "text-ada-001";

    private String apiKey;
    private String model;
    private String srcLang;
    private String tgtLang;

    public ChatGptTranslator(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String getName() {
        return "ChatGPT API";
    }

    @Override
    public String getShortName() {
        return "ChatGPT";
    }

    @Override
    public List<Language> getSourceLanguages()
            throws SAXException, IOException, ParserConfigurationException {
        return LanguageUtils.getAllLanguages();
    }

    @Override
    public List<Language> getTargetLanguages()
            throws SAXException, IOException, ParserConfigurationException {
        return LanguageUtils.getAllLanguages();
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
        MessageFormat mf = new MessageFormat(
                "Translate the text enclosed on triple quotes from \"{0}\" to \"{1}\": \"\"\"{2}\"\"\"");
        JSONObject json = new JSONObject();
        json.put("model", model);
        json.put("prompt", mf.format(new String[] { srcLang, tgtLang, source }));
        json.put("max_tokens", 300);
        json.put("temperature", 0.7);
        json.put("top_p", 1);
        json.put("frequency_penalty", 0);
        json.put("presence_penalty", 0);
        String data = json.toString();
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);

        URL url = new URL("https://api.openai.com/v1/completions");
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Authorization", "Bearer " + apiKey);
        con.setRequestProperty("Content-Length", Integer.toString(bytes.length));
        con.setDoOutput(true);
        try (OutputStream out = con.getOutputStream()) {
            out.write(bytes);
            out.flush();
        }

        int status = con.getResponseCode();
        if (status == 200) {
            StringBuffer content = new StringBuffer();
            try (InputStreamReader inptStream = new InputStreamReader(con.getInputStream())) {
                try (BufferedReader in = new BufferedReader(inptStream)) {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        if (!content.isEmpty()) {
                            content.append('\n');
                        }
                        content.append(inputLine);
                    }
                }
            }
            JSONObject result = new JSONObject(content.toString());
            JSONArray array = result.getJSONArray("choices");
            String translation = array.getJSONObject(0).getString("text");
            if (translation.startsWith("\n\n")) {
                translation = translation.substring(2);
            }
            if (translation.startsWith("\"\"\"") && translation.endsWith("\"\"\"")) {
                translation = translation.substring(3, translation.length() - 3);
            }
            return translation;
        }
        con.disconnect();
        MessageFormat mf2 = new MessageFormat(Messages.getString("ChatGptTranslator.1"));
        throw new IOException(mf2.format(new String[] { "" + status }));
    }
}
