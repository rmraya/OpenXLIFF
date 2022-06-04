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
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.Utils;
import com.maxprograms.segmenter.Segmenter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.SAXException;

public class Json2Xliff {

    private static boolean paragraphSegmentation;
    private static Segmenter segmenter;
    private static int id;
    private static List<String> segments;

    private Json2Xliff() {
        // do not instantiate this class
        // use run method instead
    }

    public static List<String> run(Map<String, String> params) {
        List<String> result = new ArrayList<>();

        id = 0;
        segments = new ArrayList<>();

        String inputFile = params.get("source");
        String xliffFile = params.get("xliff");
        String skeletonFile = params.get("skeleton");
        String sourceLanguage = params.get("srcLang");
        String targetLanguage = params.get("tgtLang");
        String encoding = params.get("srcEncoding");
        String paragraph = params.get("paragraph");
        String initSegmenter = params.get("srxFile");
        String catalog = params.get("catalog");
        String tgtLang = "";
        if (targetLanguage != null) {
            tgtLang = "\" target-language=\"" + targetLanguage;
        }

        if (paragraph == null) {
            paragraphSegmentation = false;
        } else {
            paragraphSegmentation = paragraph.equals("yes");
        }

        try {
            if (!paragraphSegmentation) {
                segmenter = new Segmenter(initSegmenter, sourceLanguage, catalog);
            }

            JSONObject json = loadFile(inputFile, encoding);
            parseJson(json);

            if (segments.isEmpty()) {
                result.add(Constants.ERROR);
                result.add("Nothing to translate.");
                return result;
            }

            try (FileOutputStream out = new FileOutputStream(skeletonFile)) {
                out.write(json.toString(4).getBytes(StandardCharsets.UTF_8));
            }

            try (FileOutputStream out = new FileOutputStream(xliffFile)) {

                writeString(out, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                writeString(out, "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "xsi:schemaLocation=\"urn:oasis:names:tc:xliff:document:1.2 xliff-core-1.2-transitional.xsd\">\n");

                writeString(out, "<file original=\"" + inputFile + "\" source-language=\"" + sourceLanguage + tgtLang
                        + "\" tool-id=\"" + Constants.TOOLID + "\" datatype=\"json\">\n");
                writeString(out, "<header>\n");
                writeString(out, "   <skl>\n");
                writeString(out, "      <external-file href=\"" + Utils.cleanString(skeletonFile) + "\"/>\n");
                writeString(out, "   </skl>\n");
                writeString(out, "   <tool tool-version=\"" + Constants.VERSION + " " + Constants.BUILD
                        + "\" tool-id=\"" + Constants.TOOLID + "\" tool-name=\"" + Constants.TOOLNAME + "\"/>\n");
                writeString(out, "</header>\n");
                writeString(out, "<?encoding " + encoding + "?>\n");
                writeString(out, "<body>\n");

                for (int i = 0; i < segments.size(); i++) {
                    writeString(out,
                            "   <trans-unit id=\"" + i + "\" xml:space=\"preserve\" approved=\"no\">\n"
                                    + "      <source>" + Utils.cleanString(segments.get(i))
                                    + "</source>\n   </trans-unit>\n");
                }

                writeString(out, "</body>\n");
                writeString(out, "</file>\n");
                writeString(out, "</xliff>");
            }

            result.add(Constants.SUCCESS);
        } catch (IOException | SAXException | ParserConfigurationException | URISyntaxException e) {
            Logger logger = System.getLogger(Json2Xliff.class.getName());
            logger.log(Level.ERROR, "Error converting JSON file.", e);
            result.add(Constants.ERROR);
            result.add(e.getMessage());
        }
        return result;
    }

    private static void writeString(FileOutputStream out, String string) throws IOException {
        out.write(string.getBytes(StandardCharsets.UTF_8));
    }

    protected static JSONObject loadFile(String file, String charset) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (FileReader stream = new FileReader(new File(file), Charset.forName(charset))) {
            try (BufferedReader reader = new BufferedReader(stream)) {
                String line = "";
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                    builder.append('\n');
                }
            }
        }
        return new JSONObject(builder.toString());
    }

    private static void parseJson(JSONObject json) {
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object obj = json.get(key);
            if (obj instanceof JSONObject) {
                parseJson(json.getJSONObject(key));
            } else if (obj instanceof String) {
                json.put(key, parseText(json.getString(key)));
            } else if (obj instanceof JSONArray) {
                parseArray(json.getJSONArray(key));
            }
        }
    }

    private static String parseText(String string) {
        if (!paragraphSegmentation) {
            String[] segs = segmenter.segment(string);
            String result = "";
            for (int i = 0; i < segs.length; i++) {
                segments.add(segs[i]);
                result = result + "%%%" + id++ + "%%%";
            }
            return result;
        }
        segments.add(string);
        return "%%%" + id++ + "%%%";
    }

    private static void parseArray(JSONArray array) {
        for (int i = 0; i < array.length(); i++) {
            Object obj = array.get(i);
            if (obj instanceof String) {
                array.put(i, parseText(array.getString(i)));
            } else if (obj instanceof JSONArray) {
                parseArray(array.getJSONArray(i));
            } else if (obj instanceof JSONObject) {
                parseJson(array.getJSONObject(i));
            }
        }
    }
}
