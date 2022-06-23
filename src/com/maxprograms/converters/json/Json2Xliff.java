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
import java.io.FileInputStream;
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
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.Utils;
import com.maxprograms.segmenter.Segmenter;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.TextNode;
import com.maxprograms.xml.XMLNode;

public class Json2Xliff {

    private static boolean paragraphSegmentation;
    private static Segmenter segmenter;
    private static int id;
    private static List<Element> segments;
    private static int bomLength = 0;

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
        paragraphSegmentation = "yes".equals(paragraph);
        String initSegmenter = params.get("srxFile");
        String catalog = params.get("catalog");
        String tgtLang = "";
        if (targetLanguage != null) {
            tgtLang = "\" target-language=\"" + targetLanguage;
        }
        try {
            checkBOM(inputFile);
            JSONObject json = loadFile(inputFile, encoding);
            if (!paragraphSegmentation) {
                segmenter = new Segmenter(initSegmenter, sourceLanguage, catalog);
            }
            String configFile = params.get("config");
            if (configFile != null) {
                JsonConfig config = JsonConfig.parseFile(configFile);
                parseJson(json, config);
            } else {
                parseJson(json);
            }

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
                    writeString(out, "  " + segments.get(i).toString() + "\n  ");
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

    private static void checkBOM(String fileName) throws IOException {
        // check the first three bytes of the document
        byte[] array = new byte[3];
        try (FileInputStream inputStream = new FileInputStream(fileName)) {
            if (inputStream.read(array) == -1) {
                throw new IOException((new File(fileName).getName() + " is empty."));
            }
        }
        byte[] feff = { -1, -2 }; // UTF-16BE
        byte[] fffe = { -2, -1 }; // UTF-16LE
        byte[] efbbbf = { -17, -69, -65 }; // UTF-8
        if ((array[0] == feff[0] && array[1] == feff[1]) || (array[0] == fffe[0] && array[1] == fffe[1])
                || (array[0] == efbbbf[0] && array[1] == efbbbf[1])) {
            // there is a BOM
            bomLength = 1;
        }
    }

    private static void writeString(FileOutputStream out, String string) throws IOException {
        out.write(string.getBytes(StandardCharsets.UTF_8));
    }

    protected static JSONObject loadFile(String file, String charset) throws IOException {
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
        return new JSONObject(builder.toString().substring(bomLength));
    }

    private static void parseJson(JSONObject json) {
        Iterator<String> it = json.keys();
        while (it.hasNext()) {
            String key = it.next();
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

    private static void parseJson(JSONObject json, JsonConfig config) {
        List<String> translatableKeys = config.getSourceKeys();
        for (int i = 0; i < translatableKeys.size(); i++) {
            String sourceKey = translatableKeys.get(i);
            if (json.has(sourceKey)) {

                break;
            }
        }
        Iterator<String> it = json.keys();
        while (it.hasNext()) {
            String key = it.next();
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
        segment.setAttribute("xml:space", "preserve");
        segment.addContent("\n    ");
        Element source = new Element("source");
        source.setText(string);
        segment.addContent(source);
        fixHtmlTags(source);
        segment.addContent("\n  ");
        segments.add(segment);
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

    private static void fixHtmlTags(Element src) {
        int count = 0;
        Pattern pattern = Pattern.compile("<[A-Za-z0-9]+([\\s][A-Za-z]+=[\"|\'][^<&>]*[\"|\'])*[\\s]*[/]?>");
        Pattern endPattern = Pattern.compile("</[A-Za-z0-9]+>");

        String e = normalise(src.getText());

        Matcher matcher = pattern.matcher(e);
        if (matcher.find()) {
            List<XMLNode> newContent = new Vector<>();
            List<XMLNode> content = src.getContent();
            Iterator<XMLNode> it = content.iterator();
            while (it.hasNext()) {
                XMLNode node = it.next();
                if (node.getNodeType() == XMLNode.TEXT_NODE) {
                    TextNode t = (TextNode) node;
                    String text = normalise(t.getText());
                    matcher = pattern.matcher(text);
                    if (matcher.find()) {
                        matcher.reset();
                        while (matcher.find()) {
                            int start = matcher.start();
                            int end = matcher.end();

                            String s = text.substring(0, start);
                            newContent.add(new TextNode(s));

                            String tag = text.substring(start, end);
                            Element ph = new Element("ph");
                            ph.setAttribute("id", "" + count++);
                            ph.setText(tag);
                            newContent.add(ph);

                            text = text.substring(end);
                            matcher = pattern.matcher(text);
                        }
                        newContent.add(new TextNode(text));
                    } else {
                        newContent.add(node);
                    }
                } else {
                    newContent.add(node);
                }
            }
            src.setContent(newContent);
        }
        matcher = endPattern.matcher(e);
        if (matcher.find()) {
            List<XMLNode> newContent = new Vector<>();
            List<XMLNode> content = src.getContent();
            Iterator<XMLNode> it = content.iterator();
            while (it.hasNext()) {
                XMLNode node = it.next();
                if (node.getNodeType() == XMLNode.TEXT_NODE) {
                    TextNode t = (TextNode) node;
                    String text = normalise(t.getText());
                    matcher = endPattern.matcher(text);
                    if (matcher.find()) {
                        matcher.reset();
                        while (matcher.find()) {
                            int start = matcher.start();
                            int end = matcher.end();

                            String s = text.substring(0, start);
                            newContent.add(new TextNode(s));

                            String tag = text.substring(start, end);
                            Element ph = new Element("ph");
                            ph.setAttribute("id", "" + count++);
                            ph.setText(tag);
                            newContent.add(ph);

                            text = text.substring(end);
                            matcher = endPattern.matcher(text);
                        }
                        newContent.add(new TextNode(text));
                    } else {
                        newContent.add(node);
                    }
                } else {
                    newContent.add(node);
                }
            }
            src.setContent(newContent);
        }
    }

    private static String normalise(String string) {
        String result = string;
        result = result.replace('\n', ' ');
        result = result.replaceAll("\\s(\\s)+", " ");
        return result;
    }

}
