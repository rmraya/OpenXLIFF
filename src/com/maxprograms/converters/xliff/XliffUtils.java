/*******************************************************************************
 * Copyright (c) 2022 - 2024 Maxprograms.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors: Maxprograms - initial API and implementation
 *******************************************************************************/

package com.maxprograms.converters.xliff;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.maxprograms.xml.Attribute;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.TextNode;
import com.maxprograms.xml.XMLNode;

public class XliffUtils {

    private XliffUtils() {
        // do not instantiate this class
    }

    public static String getHead(Element e) {
        StringBuilder builder = new StringBuilder();
        builder.append('<');
        builder.append(e.getName());
        List<Attribute> atts = e.getAttributes();
        Iterator<Attribute> it = atts.iterator();
        while (it.hasNext()) {
            Attribute a = it.next();
            builder.append(' ');
            builder.append(a.toString());
        }
        builder.append('>');
        return builder.toString();
    }

    public static boolean hasText(Element e) {
        List<XMLNode> nodes = e.getContent();
        Iterator<XMLNode> it = nodes.iterator();
        while (it.hasNext()) {
            XMLNode node = it.next();
            if (node.getNodeType() == XMLNode.TEXT_NODE) {
                TextNode t = (TextNode) node;
                String text = t.getText();
                if (text != null) {
                    for (int i = 0; i < text.length(); i++) {
                        char c = text.charAt(i);
                        if (!(Character.isSpaceChar(c) || c == '\n')) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static JSONObject getXliffLanguages(File file)
            throws SAXException, IOException, ParserConfigurationException, JSONException {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(file);
        Element root = doc.getRootElement();
        if (!"xliff".equals(root.getName())) {
            throw new IOException(Messages.getString("XliffUtils.1"));
        }
        JSONObject result = new JSONObject();
        if (root.getAttributeValue("version").startsWith("1.")) {
            Element firstFile = root.getChild("file");
            result.put("srcLang", firstFile.getAttributeValue("source-language"));
            result.put("tgtLang", firstFile.getAttributeValue("target-language"));
        } else if (root.getAttributeValue("version").startsWith("2.")) {
            result.put("srcLang", root.getAttributeValue("srcLang"));
            result.put("tgtLang", root.getAttributeValue("trgLang"));
        } else {
            throw new IOException(Messages.getString("XliffUtils.2"));
        }
        return result;
    }
}
