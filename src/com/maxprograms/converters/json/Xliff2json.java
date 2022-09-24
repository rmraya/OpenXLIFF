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

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.maxprograms.converters.Constants;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.PI;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.TextNode;
import com.maxprograms.xml.XMLNode;

public class Xliff2json {

    private static Map<String, Element> segments;
    private static String encoding;
    private static boolean escaped;
    private static List<String[]> entities;

    private Xliff2json() {
        // do not instantiate this class
        // use run method instead
    }

    public static List<String> run(Map<String, String> params) {
        List<String> result = new ArrayList<>();
        String sklFile = params.get("skeleton");
        String xliffFile = params.get("xliff");
        String catalogFile = params.get("catalog");
        String outputFile = params.get("backfile");

        try {
            Catalog catalog = new Catalog(catalogFile);
            loadSegments(xliffFile, catalog);
            Object json = Json2Xliff.loadFile(sklFile, encoding);
            if (json instanceof JSONObject obj) {
                parseJson(obj);
            } else {
                parseArray((JSONArray) json);
            }

            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                if (json instanceof JSONObject obj) {
                    out.write(obj.toString(2).getBytes(StandardCharsets.UTF_8));
                } else {
                    out.write(((JSONArray) json).toString(2).getBytes(StandardCharsets.UTF_8));
                }
            }

            result.add(Constants.SUCCESS);
        } catch (IOException | SAXException | ParserConfigurationException | URISyntaxException e) {
            Logger logger = System.getLogger(Xliff2json.class.getName());
            logger.log(Level.ERROR, "Error merging file.", e);
            result.add(Constants.ERROR);
            result.add(e.getMessage());
        }
        return result;
    }

    private static void loadSegments(String xliffFile, Catalog catalog)
            throws SAXException, IOException, ParserConfigurationException {
        SAXBuilder builder = new SAXBuilder();
        builder.setEntityResolver(catalog);

        Document doc = builder.build(xliffFile);
        Element root = doc.getRootElement();
        Element file = root.getChild("file");

        escaped = !file.getPI("escaped").isEmpty();
        if (escaped) {
            entities = Json2Xliff.loadEntities(catalog);
            entities.add(0, new String[] { "&amp;", "&" });
            entities.add(new String[] { "&lt;", "<" });
        }
        List<PI> encodings = file.getPI("encoding");
        if (encodings.isEmpty()) {
            throw new IOException("Missing encoding");
        }
        encoding = encodings.get(0).getData();
        Element body = file.getChild("body");
        List<Element> units = body.getChildren("trans-unit");
        Iterator<Element> i = units.iterator();

        segments = new HashMap<>();

        while (i.hasNext()) {
            Element unit = i.next();
            segments.put(unit.getAttributeValue("id"), unit);
        }
    }

    private static void parseJson(JSONObject json) throws IOException {
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

    private static String parseText(String line) throws IOException {
        int index = line.indexOf("%%%");
        while (index != -1) {
            String code = line.substring(index + 3, line.indexOf("%%%", index + 1));
            Element segment = segments.get(code);
            if (segment != null) {
                Element target = segment.getChild("target");
                Element source = segment.getChild("source");
                if (target != null) {
                    if ("yes".equals(segment.getAttributeValue("approved"))) {
                        line = line.replace("%%%" + code + "%%%", extractText(target));
                    } else {
                        line = line.replace("%%%" + code + "%%%", extractText(source));
                    }
                } else {
                    line = line.replace("%%%" + code + "%%%", extractText(source));
                }
            } else {
                throw new IOException("Segment " + code + " not found");
            }
            index = line.indexOf("%%%");
        }
        return line;
    }

    private static void parseArray(JSONArray array) throws IOException {
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

    private static String extractText(Element element) {
        StringBuilder result = new StringBuilder();
        List<XMLNode> content = element.getContent();
        Iterator<XMLNode> i = content.iterator();
        while (i.hasNext()) {
            XMLNode n = i.next();
            if (n.getNodeType() == XMLNode.ELEMENT_NODE) {
                Element e = (Element) n;
                result.append(extractText(e));
            }
            if (n.getNodeType() == XMLNode.TEXT_NODE) {
                if ("ph".equals(element.getName())) {
                    result.append(((TextNode) n).getText());
                } else {
                    if (escaped) {
                        result.append(replaceEntities(((TextNode) n).getText()));
                    } else {
                        result.append(((TextNode) n).getText());
                    }
                }
            }
        }
        return result.toString();
    }

    private static String replaceEntities(String string) {
        if (string.isEmpty()) {
            return string;
        }
        String result = string;
        for (int i = 0; i < entities.size(); i++) {
            String[] entry = entities.get(i);
            String entity = entry[0];
            String character = entry[1];
            int index = result.indexOf(character);
            while (index != -1) {
                String start = result.substring(0, index);
                String end = result.substring(index + character.length());
                result = start + entity + end;
                index = result.indexOf(character);
            }
        }
        return result;
    }
}
