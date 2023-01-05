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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.xliff.FromOpenXliff;
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
import com.maxprograms.xml.XMLUtils;

public class Xliff2Txlf {

    private static Catalog catalog;
    private static Document skeleton;
    private static Map<String, Element> segments;
    private static String tgtLang;
    private static int auto;
    private static String phaseName;

    private Xliff2Txlf() {
        // do not instantiate this class
        // use run method instead
    }

    public static List<String> run(Map<String, String> params) {
        List<String> result = new ArrayList<>();
        tgtLang = "";
        phaseName = "";
        String xliffFile = params.get("xliff");
        String sklFile = params.get("skeleton");
        String outputFile = params.get("backfile");
        try {
            catalog = new Catalog(params.get("catalog"));
            loadXliff(xliffFile);
            loadSkeleton(sklFile);
            Element root = skeleton.getRootElement();
            recurseSkeleton(root);
            FromOpenXliff.restoreAttributes(root);
            File f = new File(outputFile);
            File p = f.getParentFile();
            if (p == null) {
                p = new File(System.getProperty("user.dir"));
            }
            if (!p.exists()) {
                p.mkdirs();
            }
            if (!f.exists()) {
                Files.createFile(Paths.get(f.toURI()));
            }
            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                XMLOutputter outputter = new XMLOutputter();
                Indenter.indent(root, 2);
                outputter.preserveSpace(true);
                outputter.output(skeleton, out);
            }
            result.add(Constants.SUCCESS);
        } catch (IOException | SAXException | ParserConfigurationException | URISyntaxException e) {
            Logger logger = System.getLogger(Xliff2Txlf.class.getName());
            logger.log(Level.ERROR, "Error merging XLIFF file.", e);
            result.add(Constants.ERROR);
            result.add(e.getMessage());
        }
        return result;
    }

    private static void loadXliff(String xliffFile) throws SAXException, IOException, ParserConfigurationException {
        SAXBuilder builder = new SAXBuilder();
        builder.setEntityResolver(catalog);
        Document xliff = builder.build(xliffFile);
        segments = new Hashtable<>();
        recurseXliff(xliff.getRootElement());
    }

    private static void recurseXliff(Element e) throws IOException {
        if ("file".equals(e.getName()) && tgtLang.isEmpty()) {
            tgtLang = e.getAttributeValue("target-language");
        }
        if ("trans-unit".equals(e.getName())) {
            segments.put(e.getAttributeValue("id"), e);
        } else {
            List<Element> children = e.getChildren();
            Iterator<Element> it = children.iterator();
            while (it.hasNext()) {
                recurseXliff(it.next());
            }
        }
    }

    private static void loadSkeleton(String sklFile) throws SAXException, IOException, ParserConfigurationException {
        SAXBuilder builder = new SAXBuilder();
        builder.setEntityResolver(catalog);
        skeleton = builder.build(sklFile);
    }

    private static void recurseSkeleton(Element root) throws SAXException, IOException, ParserConfigurationException {
        if ("file".equals(root.getName())) {
            tgtLang = root.getAttributeValue("target-language");
        }
        if ("phase".equals(root.getName())) {
            phaseName = root.getAttributeValue("phase-name");
        }
        if ("trans-unit".equals(root.getName()) && !root.getAttributeValue("translate").equals("no")) {
            List<PI> instructions = root.getPI(Constants.TOOLID);
            if (!instructions.isEmpty()) {
                String pi = instructions.get(0).getData();
                Element segment = segments.get(pi);
                if (segment.getAttributeValue("approved").equals("yes")) {
                    Element target = root.getChild("target");
                    if (target == null) {
                        addtarget(root);
                        target = root.getChild("target");
                    }
                    Element oldTarget = new Element("target");
                    oldTarget.setContent(target.getContent());
                    Element translation = segment.getChild("target");
                    target.setContent(translation.getContent());
                    if (!target.getContent().isEmpty() && ("new".equals(target.getAttributeValue("state"))
                            || "needs-translation".equals(target.getAttributeValue("state")))) {
                        target.setAttribute("state", "translated");
                    }
                    if (!target.getChildren().isEmpty()) {
                        replaceTags(target, 1);
                    }
                    if (!oldTarget.getContent().isEmpty() && !target.getContent().equals(oldTarget.getContent())) {
                        Element altTrans = new Element("alt-trans");
                        altTrans.setAttribute("alttranstype", "previous-version");
                        String editStatus = root.getAttributeValue("gs4tr__editStatus");
                        if (!editStatus.isEmpty()) {
                            altTrans.setAttribute("gs4tr:editStatus", editStatus);
                        }
                        if (!phaseName.isEmpty()) {
                            altTrans.setAttribute("phase-name", phaseName);
                        }
                        List<Attribute> atts = oldTarget.getAttributes();
                        for (int i = 0; i < atts.size(); i++) {
                            Attribute a = atts.get(i);
                            if (a.getName().startsWith("gs4tr__")) {
                                altTrans.setAttribute(a);
                            }
                        }
                        oldTarget.setAttributes(new ArrayList<>());
                        altTrans.addContent(oldTarget);
                        root.addContent("\n");
                        root.addContent(altTrans);
                    }
                    root.setAttribute("gs4tr__editStatus", "modified");
                    root.setAttribute("approved", "yes");
                }
                root.removePI(Constants.TOOLID);
            }
            return;
        }
        List<Element> children = root.getChildren();
        Iterator<Element> it = children.iterator();
        while (it.hasNext()) {
            recurseSkeleton(it.next());
        }
    }

    private static void addtarget(Element root) {
        List<XMLNode> newContent = new Vector<>();
        List<XMLNode> content = root.getContent();
        Iterator<XMLNode> it = content.iterator();
        while (it.hasNext()) {
            XMLNode node = it.next();
            newContent.add(node);
            if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
                Element e = (Element) node;
                if (e.getName().equals("source")) {
                    newContent.add(new TextNode("\n      "));
                    newContent.add(new Element("target"));
                }
            }
        }
        root.setContent(newContent);
    }

    private static void replaceTags(Element target, int version)
            throws SAXException, IOException, ParserConfigurationException {
        StringBuilder sb = new StringBuilder();
        sb.append("<target>");
        List<XMLNode> content = target.getContent();
        auto = 1;
        Iterator<XMLNode> it = content.iterator();
        while (it.hasNext()) {
            XMLNode node = it.next();
            if (node.getNodeType() == XMLNode.TEXT_NODE) {
                TextNode text = (TextNode) node;
                sb.append(XMLUtils.cleanText(text.getText()));
            }
            if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
                Element e = (Element) node;
                if ("mrk".equals(e.getName())) {
                    if (version == 1) {
                        sb.append(e.toString());
                    } else {
                        Element mrk = processMrk(e);
                        sb.append(mrk.toString());
                    }
                } else {
                    sb.append(e.getText());
                }
            }
        }
        sb.append("</target>");
        SAXBuilder builder = new SAXBuilder();
        String string = sb.toString();
        Document d = builder.build(new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8)));
        target.setContent(d.getRootElement().getContent());
    }

    private static Element processMrk(Element e) {
        Element mrk = new Element("mrk");
        mrk.setAttribute("id", e.hasAttribute("mid") ? e.getAttributeValue("mid") : ("auto" + auto++));
        if (e.hasAttribute("ts")) {
            mrk.setAttribute("value", e.getAttributeValue("ts"));
        }
        String mtype = e.getAttributeValue("mtype");
        if ("protected".equals(mtype)) {
            mrk.setAttribute("translate", "no");
        }
        if (Arrays.asList("generic", "comment", "term").contains(mtype)) {
            mrk.setAttribute("type", mtype);
        } else {
            mrk.setAttribute("type", "oxlf:" + mtype.replace(":", "_"));
        }
        List<XMLNode> newContent = new Vector<>();
        List<XMLNode> content = e.getContent();
        Iterator<XMLNode> it = content.iterator();
        while (it.hasNext()) {
            XMLNode node = it.next();
            if (node.getNodeType() == XMLNode.TEXT_NODE) {
                newContent.add(node);
            }
            if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
                Element child = (Element) node;
                if ("mrk".equals(child.getName())) {
                    Element processed = processMrk(child);
                    newContent.add(processed);
                } else {
                    String text = child.getText();
                    newContent.add(new TextNode(text));
                }
            }
        }
        mrk.setContent(newContent);
        return mrk;
    }
}
