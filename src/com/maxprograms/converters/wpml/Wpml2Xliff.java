/*******************************************************************************
 * Copyright (c) 2018 - 2025 Maxprograms.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors: Maxprograms - initial API and implementation
 *******************************************************************************/
package com.maxprograms.converters.wpml;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.Utils;
import com.maxprograms.segmenter.Segmenter;
import com.maxprograms.xml.CData;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.CatalogBuilder;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.Indenter;
import com.maxprograms.xml.PI;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.TextNode;
import com.maxprograms.xml.XMLNode;
import com.maxprograms.xml.XMLOutputter;

public class Wpml2Xliff {

    private static String inputFile;
    private static String skeletonFile;
    private static String sourceLanguage;
    private static String targetLanguage;
    private static int segId = 0;
    private static Segmenter segmenter;
    private static boolean paragraphSegmentation;
    private static Pattern pattern;
    private static Pattern endPattern;

    private Wpml2Xliff() {
        // do not instantiate this class
        // use run method instead
    }

    public static List<String> run(Map<String, String> params) {
        List<String> result = new ArrayList<>();
        inputFile = params.get("source");
        String xliffFile = params.get("xliff");
        skeletonFile = params.get("skeleton");
        sourceLanguage = params.get("srcLang");
        targetLanguage = params.get("tgtLang");
        String srxRules = params.get("srxFile");
        String catalogFile = params.get("catalog");
        String elementSegmentation = params.get("paragraph");
        if (elementSegmentation == null) {
            paragraphSegmentation = false;
        } else {
            if (elementSegmentation.equals("yes")) {
                paragraphSegmentation = true;
            } else {
                paragraphSegmentation = false;
            }
        }
        pattern = Pattern.compile("<[A-Za-z0-9]+([\\s][A-Za-z]+=[\"|\'][^<&>]*[\"|\'])*[\\s]*[/]?>");
        endPattern = Pattern.compile("</[A-Za-z0-9]+>");

        try {
            Catalog catalog = CatalogBuilder.getCatalog(catalogFile);
            if (!paragraphSegmentation) {
                segmenter = new Segmenter(srxRules, sourceLanguage, catalog);
            }
            SAXBuilder builder = new SAXBuilder();
            builder.setEntityResolver(catalog);
            Document doc = builder.build(inputFile);
            Element root = doc.getRootElement();

            Document newDoc = new Document(root.getNamespace(), root.getName(), doc.getPublicId(), doc.getSystemId());
            Element newRoot = newDoc.getRootElement();
            newRoot.addContent(new PI("encoding", doc.getEncoding().name()));

            recurse(root, newRoot);

            try (FileOutputStream out = new FileOutputStream(xliffFile)) {
                Indenter.indent(newRoot, 2);
                XMLOutputter outputter = new XMLOutputter();
                outputter.preserveSpace(true);
                outputter.output(newDoc, out);
            }
            try (FileOutputStream skl = new FileOutputStream(skeletonFile)) {
                XMLOutputter outputter = new XMLOutputter();
                outputter.preserveSpace(true);
                outputter.output(doc, skl);
            }
            result.add(Constants.SUCCESS);
        } catch (IOException | SAXException | ParserConfigurationException | URISyntaxException e) {
            Logger logger = System.getLogger(Wpml2Xliff.class.getName());
            logger.log(Level.ERROR, Messages.getString("Wpml2Xliff.1"), e);
            result.add(Constants.ERROR);
            result.add(e.getMessage());
        }
        return result;
    }

    private static void recurse(Element root, Element newRoot)
            throws SAXException, IOException, ParserConfigurationException {
        newRoot.setAttributes(root.getAttributes());
        if ("file".equals(root.getName())) {
            newRoot.setAttribute("datatype", "x-wpmlxliff");
            newRoot.setAttribute("original", Utils.cleanString(inputFile));
            newRoot.setAttribute("source-language", sourceLanguage);
            if (targetLanguage != null) {
                newRoot.setAttribute("target-language", targetLanguage);
            }
            Element header = new Element("header");
            newRoot.addContent(header);
            Element skl = new Element("skl");
            header.addContent(skl);
            Element externalFile = new Element("external-file");
            externalFile.setAttribute("href", Utils.cleanString(skeletonFile));
            skl.addContent(externalFile);
            Element tool = new Element("tool");
            tool.setAttribute("tool-version", Constants.VERSION + " " + Constants.BUILD);
            tool.setAttribute("tool-id", Constants.TOOLID);
            tool.setAttribute("tool-name", Constants.TOOLNAME);
            header.addContent(tool);
        }

        List<XMLNode> nodes = root.getContent();
        Iterator<XMLNode> it = nodes.iterator();
        while (it.hasNext()) {
            XMLNode node = it.next();
            switch (node.getNodeType()) {
                case XMLNode.ELEMENT_NODE:
                    Element child = (Element) node;
                    if ("header".equals(child.getName()) || "source".equals(child.getName())
                            || "target".equals(child.getName())) {
                        // continue
                    } else if ("trans-unit".equals(child.getName())) {
                        ArrayList<XMLNode> newContent = new ArrayList<>();
                        Element src = new Element("source");
                        src.setContent(child.getChild("source").getContent());
                        removeCdata(src);
                        fixHtmlTags(src);
                        if (!containsText(src)) {
                            continue;
                        }
                        List<List<XMLNode>> array = new ArrayList<>();
                        List<XMLNode> currentContent = new ArrayList<>();
                        List<XMLNode> srcContent = src.getContent();
                        Iterator<XMLNode> st = srcContent.iterator();
                        while (st.hasNext()) {
                            XMLNode n = st.next();
                            if (n.getNodeType() == XMLNode.ELEMENT_NODE) {
                                currentContent.add(n);
                            }
                            if (n.getNodeType() == XMLNode.TEXT_NODE) {
                                String string = ((TextNode) n).getText();
                                StringBuilder builder = new StringBuilder();
                                for (int i = 0; i < string.length(); i++) {
                                    char c = string.charAt(i);
                                    if (c == '\n') {
                                        currentContent.add(new TextNode(builder.toString()));
                                        array.add(currentContent);
                                        List<XMLNode> newLine = new ArrayList<>();
                                        newLine.add(new TextNode("\n"));
                                        array.add(newLine);
                                        currentContent = new ArrayList<>();
                                        builder = new StringBuilder();
                                    } else {
                                        builder.append(c);
                                    }
                                }
                                currentContent.add(new TextNode(builder.toString()));
                            }
                        }
                        if (!currentContent.isEmpty()) {
                            array.add(currentContent);
                        }
                        for (int i = 0; i < array.size(); i++) {
                            Element source = new Element("source");
                            source.setContent(array.get(i));
                            if (!containsText(source)) {
                                newContent.add(new TextNode(getText(source)));
                                continue;
                            }
                            if (paragraphSegmentation) {
                                Element unit = new Element("trans-unit");
                                unit.setAttribute("id", "" + segId);
                                newRoot.addContent(unit);
                                unit.addContent(source);
                                newContent.add(new TextNode("%%%" + segId++ + "%%%"));
                            } else {
                                Element segmented = segmenter.segment(source);
                                List<Element> list = segmented.getChildren("mrk");
                                Iterator<Element> mt = list.iterator();
                                while (mt.hasNext()) {
                                    Element mrk = mt.next();
                                    Element unit = new Element("trans-unit");
                                    unit.setAttribute("id", "" + segId);
                                    newRoot.addContent(unit);
                                    Element segment = new Element("source");
                                    segment.setContent(mrk.getContent());
                                    unit.addContent(segment);
                                    newContent.add(new TextNode("%%%" + segId++ + "%%%"));
                                }
                            }
                        }
                        Element target = child.getChild("target");
                        if (target == null) {
                            target = new Element("target");
                            child.addContent(target);
                        }
                        target.setContent(newContent);
                    } else {
                        Element newChild = new Element(child.getName());
                        newChild.setAttributes(child.getAttributes());
                        newRoot.addContent(newChild);
                        recurse(child, newChild);
                    }
                    break;
                case XMLNode.TEXT_NODE:
                    newRoot.addContent(node);
                    break;
                default:
                    // do nothing
            }
        }

    }

    protected static String getText(Element e) {
        StringBuilder builder = new StringBuilder();
        List<XMLNode> list = e.getContent();
        Iterator<XMLNode> it = list.iterator();
        while (it.hasNext()) {
            XMLNode n = it.next();
            if (n.getNodeType() == XMLNode.TEXT_NODE) {
                builder.append(((TextNode) n).getText());
            }
            if (n.getNodeType() == XMLNode.ELEMENT_NODE) {
                builder.append(getText((Element) n));
            }
        }
        return builder.toString();
    }

    private static boolean containsText(Element child) {
        List<XMLNode> list = child.getContent();
        Iterator<XMLNode> it = list.iterator();
        while (it.hasNext()) {
            XMLNode n = it.next();
            if (n.getNodeType() == XMLNode.TEXT_NODE) {
                String t = ((TextNode) n).getText();
                if (!t.isBlank()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void removeCdata(Element e) {
        List<XMLNode> newContent = new ArrayList<>();
        List<XMLNode> nodes = e.getContent();
        Iterator<XMLNode> it = nodes.iterator();
        while (it.hasNext()) {
            XMLNode node = it.next();
            switch (node.getNodeType()) {
                case XMLNode.ELEMENT_NODE, XMLNode.TEXT_NODE:
                    newContent.add(node);
                    break;
                case XMLNode.CDATA_SECTION_NODE:
                    CData cdata = (CData) node;
                    newContent.add(new TextNode(cdata.getData()));
                    break;
                default:
                    // do nothing
            }
        }
        e.setContent(newContent);
    }

    private static void fixHtmlTags(Element src) {
        int count = 0;
        String e = src.getText();

        Matcher matcher = pattern.matcher(e);
        if (matcher.find()) {
            List<XMLNode> newContent = new ArrayList<>();
            List<XMLNode> content = src.getContent();
            Iterator<XMLNode> it = content.iterator();
            while (it.hasNext()) {
                XMLNode node = it.next();
                if (node.getNodeType() == XMLNode.TEXT_NODE) {
                    TextNode t = (TextNode) node;
                    String text = t.getText();
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
            List<XMLNode> newContent = new ArrayList<>();
            List<XMLNode> content = src.getContent();
            Iterator<XMLNode> it = content.iterator();
            while (it.hasNext()) {
                XMLNode node = it.next();
                if (node.getNodeType() == XMLNode.TEXT_NODE) {
                    TextNode t = (TextNode) node;
                    String text = t.getText();
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

}
