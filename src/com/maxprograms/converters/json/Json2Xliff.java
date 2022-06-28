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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.EncodingResolver;
import com.maxprograms.converters.Utils;
import com.maxprograms.segmenter.Segmenter;
import com.maxprograms.xml.Element;

public class Json2Xliff {

    private static boolean paragraphSegmentation;
    private static Segmenter segmenter;
    private static int id;
    private static List<Element> segments;
    private static Set<String> ids;
    private static int bomLength = 0;

    private Json2Xliff() {
        // do not instantiate this class
        // use run method instead
    }

    public static List<String> run(Map<String, String> params) {
        List<String> result = new ArrayList<>();

        id = 0;
        segments = new ArrayList<>();
        ids = new HashSet<>();

        String inputFile = params.get("source");
        String xliffFile = params.get("xliff");
        String skeletonFile = params.get("skeleton");
        String sourceLanguage = params.get("srcLang");
        String targetLanguage = params.get("tgtLang");
        String encoding = params.get("srcEncoding");
        String paragraph = params.get("paragraph");
        paragraphSegmentation = "yes".equals(paragraph);
        String initSegmenter = params.get("srxFile");
        String catalog = params.get("catalog");
        String tgtLang = "";
        if (targetLanguage != null) {
            tgtLang = "\" target-language=\"" + targetLanguage;
        }
        try {
            bomLength = EncodingResolver.getBOM(inputFile) == null ? 0 : 1;
            Object json = loadFile(inputFile, encoding);
            if (!paragraphSegmentation) {
                segmenter = new Segmenter(initSegmenter, sourceLanguage, catalog);
            }
            String configFile = params.get("config");
            if (configFile != null) {
                JsonConfig config = JsonConfig.parseFile(configFile);
                if (json instanceof JSONObject obj) {
                    parseJson(obj, config);
                } else {
                    parseArray((JSONArray) json, config);
                }
            } else {
                if (json instanceof JSONObject obj) {
                    parseJson(obj);
                } else {
                    parseArray((JSONArray) json);
                }
            }

            if (segments.isEmpty()) {
                result.add(Constants.ERROR);
                result.add("Nothing to translate.");
                return result;
            }

            try (FileOutputStream out = new FileOutputStream(skeletonFile)) {
                if (json instanceof JSONObject obj) {
                    out.write(obj.toString(2).getBytes(StandardCharsets.UTF_8));
                } else {
                    out.write(((JSONArray) json).toString(2).getBytes(StandardCharsets.UTF_8));
                }
            }

            try (FileOutputStream out = new FileOutputStream(xliffFile)) {

                writeString(out, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                writeString(out, "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "xsi:schemaLocation=\"urn:oasis:names:tc:xliff:document:1.2 xliff-core-1.2-transitional.xsd\">\n");

                writeString(out, "<file original=\"" + inputFile + "\" source-language=\"" + sourceLanguage + tgtLang
                        + "\" tool-id=\"" + Constants.TOOLID + "\" datatype=\"x-json\">\n");
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
                    writeString(out, "  " + segments.get(i).toString() + "\n");
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

    protected static Object loadFile(String file, String charset) throws IOException {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        try (FileReader stream = new FileReader(new File(file), Charset.forName(charset))) {
            try (BufferedReader reader = new BufferedReader(stream)) {
                String line = "";
                while ((line = reader.readLine()) != null) {
                    if (!first) {
                        builder.append('\n');
                    }
                    builder.append(line);
                    first = false;
                }
            }
        }
        for (int i = bomLength; i < builder.length(); i++) {
            if (builder.charAt(i) == '[') {
                return new JSONArray(builder.toString().substring(bomLength));
            }
            if (builder.charAt(i) == '{') {
                return new JSONObject(builder.toString().substring(bomLength));
            }
            if (!Character.isSpaceChar(builder.charAt(i))) {
                break;
            }
        }
        throw new IOException("Selected file is not supported");
    }

    private static void parseJson(JSONObject json) {
        Iterator<String> it = json.keys();
        while (it.hasNext()) {
            String key = it.next();
            Object obj = json.get(key);
            if (obj instanceof JSONObject js) {
                parseJson(js);
            } else if (obj instanceof String string) {
                json.put(key, parseText(string));
            } else if (obj instanceof JSONArray array) {
                parseArray(array);
            }
        }
    }

    private static void parseJson(JSONObject json, JsonConfig config) throws IOException {
        List<String> translatableKeys = config.getSourceKeys();
        List<String> ignorable = config.getIgnorableKeys();
        Set<String> parsedKeys = new HashSet<>();
        for (int i = 0; i < translatableKeys.size(); i++) {
            String sourceKey = translatableKeys.get(i);
            if (json.has(sourceKey)) {
                JSONObject configuration = config.getConfiguration(sourceKey);
                if (configuration == null) {
                    throw new IOException("Wrong configuration for source key " + sourceKey);
                }
                ElementHolder sourceHolder = ElementBuilder.buildElement("source", json.getString(sourceKey));
                String targetKey = configuration.has(JsonConfig.TARGETKEY)
                        ? configuration.getString(JsonConfig.TARGETKEY)
                        : "";
                String targetText = json.has(targetKey) ? json.getString(targetKey) : "";
                String idKey = configuration.has(JsonConfig.IDKEY) ? configuration.getString(JsonConfig.IDKEY) : "";
                String idString = "";
                if (json.has(idKey)) {
                    Object obj = json.get(idKey);
                    if (obj instanceof String string) {
                        idString = string;
                    }
                    if (obj instanceof Integer j) {
                        idString = "" + j;
                    }
                }
                if (!idString.isEmpty()) {
                    validateId(idString);
                }
                String resnameKey = configuration.has(JsonConfig.RESNAMEKEY)
                        ? configuration.getString(JsonConfig.RESNAMEKEY)
                        : "";
                String resnameText = json.has(resnameKey) ? json.getString(resnameKey) : "";
                String noteKey = configuration.has(JsonConfig.NOTEKEY) ? configuration.getString(JsonConfig.NOTEKEY)
                        : "";
                List<String> notes = json.has(noteKey) ? harvestNotes(json.get(noteKey))
                        : new ArrayList<>();
                parsedKeys.add(sourceKey);
                if (!targetKey.isEmpty()) {
                    parsedKeys.add(targetKey);
                }
                if (!idKey.isEmpty()) {
                    parsedKeys.add(idKey);
                }
                if (!noteKey.isEmpty()) {
                    parsedKeys.add(noteKey);
                }
                Element transUnit = new Element("trans-unit");
                if (!resnameText.isEmpty()) {
                    transUnit.setAttribute("resname", resnameText);
                }
                transUnit.setAttribute("id", idString.isEmpty() ? "" + id : idString);
                if (ids.contains(transUnit.getAttributeValue("id"))) {
                    // TODO throw new IOException("Duplicated \"id\" specified: " +
                    // transUnit.getAttributeValue("id");
                }
                ids.add(transUnit.getAttributeValue("id"));
                transUnit.addContent("\n    ");
                transUnit.addContent(sourceHolder.getElement());
                if (transUnit.getChild("source").getChildren().isEmpty()) {
                    transUnit.setAttribute("xml:space", "preserve");
                }
                if (targetText.isEmpty()) {
                    json.put(sourceKey, sourceHolder.getStart() + "%%%" +
                            (idString.isEmpty() ? "" + id++ : idString) + "%%%" + sourceHolder.getEnd());
                } else {
                    ElementHolder targetHolder = ElementBuilder.buildElement("target", targetText);
                    transUnit.addContent("\n    ");
                    transUnit.addContent(targetHolder.getElement());
                    json.put(targetKey, targetHolder.getStart() + "%%%" +
                            (idString.isEmpty() ? "" + id++ : idString) + "%%%" + targetHolder.getEnd());
                }
                if (!notes.isEmpty()) {
                    Iterator<String> it = notes.iterator();
                    while (it.hasNext()) {
                        Element note = new Element("note");
                        note.setText(it.next());
                        transUnit.addContent("\n    ");
                        transUnit.addContent(note);
                    }
                }
                transUnit.addContent("\n  ");
                segments.add(transUnit);
            }
        }
        Iterator<String> it = json.keys();
        while (it.hasNext()) {
            String key = it.next();
            if (!parsedKeys.contains(key) && !ignorable.contains(key)) {
                Object object = json.get(key);
                if (object instanceof JSONObject jsobj) {
                    parseJson(jsobj, config);
                } else if (object instanceof String string) {
                    json.put(key, string);
                } else if (object instanceof JSONArray array) {
                    parseArray(array, config);
                }
            }
        }
    }

    private static void validateId(String id) throws IOException {
        String[] nameStart = new String[] { ":", "[A-Z]", "_", "[a-z]", "[\\u00C0-\\u00D6]", "[\\u00D8-\\u00F6]",
                "[\\u00F8-\\u02FF]", "[\\u0370-\\u037D]", "[\\u037F-\\u1FFF]", "[\\u200C-\\u200D]", "[\\u2070-\\u218F]",
                "[\\u2C00-\\u2FEF]", "[\\u3001-\\uD7FF]", "[\\uF900-\\uFDCF]", "[\\uFDF0-\\uFFFD]",
                "[\\u10000-\\uEFFFF]" };
        String[] nameChar = new String[] { ":", "[A-Z]", "_", "[a-z]", "[-]", "[.]", "[0-9]", "\u00B7",
                "[\\u00C0-\\u00D6]", "[\\u00D8-\\u00F6]", "[\\u00F8-\\u02FF]", "[\\u0370-\\u037D]", "[\\u037F-\\u1FFF]",
                "[\\u200C-\\u200D]", "[\\u2070-\\u218F]", "[\\u2C00-\\u2FEF]", "[\\u3001-\\uD7FF]", "[\\uF900-\\uFDCF]",
                "[\\uFDF0-\\uFFFD]", "[\\u10000-\\uEFFFF]", "[\\u0300-\\u036F]", "[\\u203F-\\u2040]" };
        boolean first = false;
        String firstChar = "" + id.charAt(0);
        for (int i = 0; i < nameStart.length; i++) {
            if (firstChar.matches(nameStart[i])) {
                first = true;
                break;
            }
        }
        if (!first) {
            throw new IOException("Invalid initial character for \"id\": " + id.charAt(0));
        }
        for (int i = 1; i < id.length(); i++) {
            boolean rest = false;
            String nextChar = "" + id.charAt(i);
            for (int j = 0; j < nameStart.length; j++) {
                String expr = nameChar[j];
                if (nextChar.matches(expr)) {
                    rest = true;
                    break;
                }
            }
            if (!rest) {
                throw new IOException("Invalid character for \"id\": " + id.charAt(i));
            }
        }
    }

    private static List<String> harvestNotes(Object object) {
        List<String> result = new ArrayList<>();
        if (object instanceof JSONObject json) {
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                result.addAll(harvestNotes(json.get(keys.next())));
            }
        }
        if (object instanceof JSONArray array) {
            for (int i = 0; i < array.length(); i++) {
                result.addAll(harvestNotes(array.get(i)));
            }
        }
        if (object instanceof String string) {
            result.add(string);
        }
        return result;
    }

    private static String parseText(String string) {
        if (!paragraphSegmentation) {
            String[] segs = segmenter.segment(string);
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < segs.length; i++) {
                result.append(addSegment(segs[i]));
            }
            return result.toString();
        }
        return addSegment(string);
    }

    private static String addSegment(String string) {
        Element segment = new Element("trans-unit");
        segment.setAttribute("id", "" + id);
        segment.addContent("\n    ");
        ElementHolder holder = ElementBuilder.buildElement("source", string);
        segment.addContent(holder.getElement());
        segment.addContent("\n  ");
        segments.add(segment);
        return holder.getStart() + "%%%" + id++ + "%%%" + holder.getEnd();
    }

    private static void parseArray(JSONArray array) {
        for (int i = 0; i < array.length(); i++) {
            Object obj = array.get(i);
            if (obj instanceof String string) {
                array.put(i, parseText(string));
            } else if (obj instanceof JSONArray arr) {
                parseArray(arr);
            } else if (obj instanceof JSONObject json) {
                parseJson(json);
            }
        }
    }

    private static void parseArray(JSONArray array, JsonConfig config) throws JSONException, IOException {
        for (int i = 0; i < array.length(); i++) {
            Object obj = array.get(i);
            if (obj instanceof String string) {
                array.put(i, parseText(string));
            } else if (obj instanceof JSONArray arr) {
                parseArray(arr, config);
            } else if (obj instanceof JSONObject json) {
                parseJson(json, config);
            }
        }
    }

}
