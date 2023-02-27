/*******************************************************************************
 * Copyright (c) 2023 Maxprograms.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors: Maxprograms - initial API and implementation
 *******************************************************************************/
package com.maxprograms.converters.txlf;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.xliff.XliffUtils;
import com.maxprograms.xml.Attribute;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.Indenter;
import com.maxprograms.xml.PI;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.TextNode;
import com.maxprograms.xml.XMLNode;
import com.maxprograms.xml.XMLOutputter;

public class Txlf2Xliff {

    private static int tag;

    private Txlf2Xliff() {
        // do not instantiate this class
        // use run method instead
    }

    public static List<String> run(Map<String, String> params) {
        List<String> result = new ArrayList<>();
        String inputFile = params.get("source");
        String xliffFile = params.get("xliff");
        String skeletonFile = params.get("skeleton");
        String sourceLanguage = params.get("srcLang");
        String targetLanguage = params.get("tgtLang");
        String catalog = params.get("catalog");
        try {
            SAXBuilder builder = new SAXBuilder();
            builder.setEntityResolver(new Catalog(catalog));
            Document doc = builder.build(inputFile);
            Element root = doc.getRootElement();

            Document newDoc = new Document(null, "xliff", null, null);
            Element newRoot = newDoc.getRootElement();
            newRoot.setAttribute("version", "1.2");
            newRoot.setAttribute("xmlns", "urn:oasis:names:tc:xliff:document:1.2");
            newRoot.addContent(new PI("encoding", doc.getEncoding().name()));

            Element file = new Element("file");
            file.setAttribute("datatype", "x-txlf");
            file.setAttribute("source-language", sourceLanguage);
            if (targetLanguage != null && !targetLanguage.isEmpty()) {
                file.setAttribute("target-language", targetLanguage);
            }
            file.setAttribute("original", inputFile);
            newRoot.addContent(file);

            Element header = new Element("header");
            file.addContent(header);

            Element skl = new Element("skl");
            header.addContent(skl);

            Element externalFile = new Element("external-file");
            externalFile.setAttribute("href", skeletonFile);
            skl.addContent(externalFile);

            Element tool = new Element("tool");
            tool.setAttribute("tool-id", Constants.TOOLID);
            tool.setAttribute("tool-name", Constants.TOOLNAME);
            tool.setAttribute("tool-version", Constants.VERSION);
            header.addContent(tool);

            List<Element> units = new Vector<>();

            if (root.getAttributeValue("version").startsWith("1")) {
                recurse(root, units);
            }
            if (units.isEmpty()) {
                result.add(Constants.ERROR);
                result.add(Messages.getString("Txlf2Xliff.1"));
                return result;
            }
            Element body = new Element("body");
            file.addContent(body);
            for (int i = 0; i < units.size(); i++) {
                body.addContent(units.get(i));
            }
            try (FileOutputStream out = new FileOutputStream(xliffFile)) {
                Indenter.indent(newRoot, 2);
                XMLOutputter outputter = new XMLOutputter();
                outputter.preserveSpace(true);
                outputter.output(newDoc, out);
            }
            try (FileOutputStream skeleton = new FileOutputStream(skeletonFile)) {
                XMLOutputter outputter = new XMLOutputter();
                outputter.preserveSpace(true);
                outputter.output(doc, skeleton);
            }
            result.add(Constants.SUCCESS);
        } catch (IOException | SAXException | ParserConfigurationException | URISyntaxException e) {
            Logger logger = System.getLogger(Txlf2Xliff.class.getName());
            logger.log(Level.ERROR, Messages.getString("Txlf2Xliff.2"), e);
            result.add(Constants.ERROR);
            result.add(e.getMessage());
        }
        return result;
    }

    private static void recurse(Element root, List<Element> units) {
        if ("xliff".equals(root.getName())) {
            renameAttributes(root);
        }
        if ("trans-unit".equals(root.getName()) && !root.getAttributeValue("translate").equals("no")) {
            Element unit = new Element("trans-unit");
            unit.setAttribute("id", "" + units.size());
            unit.setAttribute("approved", root.getAttributeValue("approved", "no"));
            boolean space = root.getAttributeValue("xml:space").equals("preserve");
            if (space) {
                unit.setAttribute("xml:space", "preserve");
            }
            Element source = new Element("source");
            tag = 1;
            source.setContent(getContent(root.getChild("source")));
            if (!XliffUtils.hasText(source)) {
                return;
            }
            unit.addContent(source);
            Element target = new Element("target");
            tag = 1;
            Element originalTarget = root.getChild("target");
            if (originalTarget != null) {
                if (originalTarget.hasAttribute("state")) {
                    target.setAttribute(originalTarget.getAttribute("state"));
                }
                if (originalTarget.hasAttribute("state-qualifier")
                        && !originalTarget.getAttributeValue("state-qualifier").startsWith("x-")) {
                    target.setAttribute(originalTarget.getAttribute("state-qualifier"));
                }
                target.setContent(originalTarget.getContent());
            }

            unit.addContent(target);

            units.add(unit);
            root.addContent(new PI(Constants.TOOLID, unit.getAttributeValue("id")));
            return;
        }
        List<Element> children = root.getChildren();
        Iterator<Element> it = children.iterator();
        while (it.hasNext()) {
            recurse(it.next(), units);
        }
    }

    private static void renameAttributes(Element e) {
        List<Attribute> atts = e.getAttributes();
        Iterator<Attribute> at = atts.iterator();
        Vector<String> change = new Vector<>();
        while (at.hasNext()) {
            Attribute a = at.next();
            if (a.getName().indexOf(":") != -1) {
                String ns = a.getName().substring(0, a.getName().indexOf(":"));
                if ("gs4tr".equals(ns)) {
                    change.add(a.getName());
                }
            }
        }
        for (int i = 0; i < change.size(); i++) {
            String name = change.get(i);
            Attribute a = e.getAttribute(name);
            e.setAttribute(name.replace(":", "__"), a.getValue());
            e.removeAttribute(name);
        }
        List<Element> children = e.getChildren();
        Iterator<Element> it = children.iterator();
        while (it.hasNext()) {
            renameAttributes(it.next());
        }
    }

    private static List<XMLNode> getContent(Element child) {
        List<XMLNode> result = new Vector<>();
        if (child != null) {
            List<XMLNode> content = child.getContent();
            Iterator<XMLNode> it = content.iterator();
            while (it.hasNext()) {
                XMLNode node = it.next();
                if (node.getNodeType() == XMLNode.TEXT_NODE) {
                    result.add(node);
                }
                if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
                    Element e = (Element) node;
                    String name = e.getName();
                    if ("g".equals(name)) {
                        Element ph1 = new Element("ph");
                        ph1.setAttribute("id", "" + tag++);
                        ph1.setText(XliffUtils.getHead(e));
                        result.add(ph1);
                        if (e.getChildren().isEmpty()) {
                            result.add(new TextNode(e.getText()));
                        } else {
                            result.addAll(getContent(e));
                        }
                        Element ph2 = new Element("ph");
                        ph2.setAttribute("id", "" + tag++);
                        ph2.setText("</g>");
                        result.add(ph2);
                    }
                    if ("x".equals(name) || "bx".equals(name) || "ex".equals(name) || "ph".equals(name)
                            || "bpt".equals(name) || "ept".equals(name) || "it".equals(name)) {
                        Element ph = new Element("ph");
                        ph.setAttribute("id", "" + tag++);
                        ph.setText(e.toString());
                        result.add(ph);
                    }
                    if ("mrk".equals(name)) {
                        // add <mrk> as is
                        result.add(e);
                    }
                }
            }
        }
        return result;
    }
}
