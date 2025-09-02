/*******************************************************************************
 * Copyright (c) 2018 - 2025 Maxprograms.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors: Maxprograms - initial API and implementation
 *******************************************************************************/
package com.maxprograms.converters.xliff;

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
import com.maxprograms.xml.Attribute;
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
import com.maxprograms.xml.XMLUtils;

public class FromOpenXliff {

    private static Catalog catalog;
    private static Document skeleton;
    private static Map<String, Element> segments;
    private static String tgtLang;
    private static boolean hasTarget;
    private static int auto;
    private static Map<String, String> segmentMetadata;
    private static Map<String, String> filesMetadata;
    private static int fileCounter;

    private FromOpenXliff() {
        // do not instantiate this class
        // use run method instead
    }

    public static List<String> run(Map<String, String> params) {
        List<String> result = new ArrayList<>();
        fileCounter = 0;
        tgtLang = "";
        String xliffFile = params.get("xliff");
        String sklFile = params.get("skeleton");
        segmentMetadata = new Hashtable<>();
        filesMetadata = new Hashtable<>();
        String outputFile = params.get("backfile");
        try {
            catalog = CatalogBuilder.getCatalog(params.get("catalog"));
            loadXliff(xliffFile);
            loadSkeleton(sklFile);
            Element root = skeleton.getRootElement();
            recurseSkeleton(root);
            if (root.getAttributeValue("version").startsWith("1")) {
                restoreAttributes(root);
            }
            File f = new File(outputFile);
            File p = f.getParentFile();
            if (p == null) {
                p = new File(System.getProperty("user.dir"));
            }
            if (Files.notExists(p.toPath())) {
                Files.createDirectories(p.toPath());
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
            if (!segmentMetadata.isEmpty() || !filesMetadata.isEmpty()) {
                restoreMetadata(outputFile);
            }
            result.add(Constants.SUCCESS);
        } catch (IOException | SAXException | ParserConfigurationException | URISyntaxException e) {
            Logger logger = System.getLogger(FromOpenXliff.class.getName());
            logger.log(Level.ERROR, Messages.getString("FromOpenXliff.1"), e);
            result.add(Constants.ERROR);
            result.add(e.getMessage());
        }
        return result;
    }

    private static void recurseSkeleton(Element rootElement)
            throws SAXException, IOException, ParserConfigurationException {
        String version = rootElement.getAttributeValue("version");
        if (version.startsWith("1")) {
            recurse1x(rootElement);
        }
        if (version.startsWith("2")) {
            tgtLang = rootElement.getAttributeValue("trgLang");
            recurse2x(rootElement);
            if (hasTarget && rootElement.getAttributeValue("trgLang").isEmpty()) {
                rootElement.setAttribute("trgLang", tgtLang);
            }
        }
    }

    private static void recurse1x(Element root) throws SAXException, IOException, ParserConfigurationException {
        if ("file".equals(root.getName())) {
            tgtLang = root.getAttributeValue("target-language");
        }
        if ("trans-unit".equals(root.getName()) && !root.getAttributeValue("translate").equals("no")) {
            Element segSource = root.getChild("seg-source");
            if (segSource != null) {
                Element target = root.getChild("target");
                if (target == null) {
                    addtarget(root);
                    target = root.getChild("target");
                }
                if (target.getContent().isEmpty()) {
                    Element t = new Element("target");
                    t.clone(segSource);
                    target.setContent(t.getContent());
                }
                List<XMLNode> content = segSource.getContent();
                Iterator<XMLNode> it = content.iterator();
                while (it.hasNext()) {
                    XMLNode node = it.next();
                    if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
                        Element e = (Element) node;
                        if ("mrk".equals(e.getName()) && "seg".equals(e.getAttributeValue("mtype"))) {
                            String pi = e.getPI(Constants.TOOLID).get(0).getData();
                            Element segment = segments.get(pi);
                            if (segment.getAttributeValue("approved").equals("yes")) {
                                Element mrk = Xliff1xProcessor.locateMrk(target, e.getAttributeValue("mid"));
                                mrk.setContent(segment.getChild("target").getContent());
                                if (!mrk.getChildren().isEmpty()) {
                                    replaceTags(mrk, 1);
                                }
                            }
                            e.removePI(Constants.TOOLID);
                        }
                    }
                }
            } else {
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
                        target.clone(segment.getChild("target"));
                        if (!target.getContent().isEmpty() && ("new".equals(target.getAttributeValue("state"))
                                || "needs-translation".equals(target.getAttributeValue("state")))) {
                            target.setAttribute("state", "translated");
                        }
                        if (!target.getChildren().isEmpty()) {
                            replaceTags(target, 1);
                        }
                        root.setAttribute("approved", "yes");
                    }
                    root.removePI(Constants.TOOLID);
                }
            }
            return;
        }

        List<Element> children = root.getChildren();
        Iterator<Element> it = children.iterator();
        while (it.hasNext()) {
            recurse1x(it.next());
        }
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

    private static void addtarget(Element root) {
        boolean hasSegSource = root.getChild("seg-source") != null;
        List<XMLNode> newContent = new Vector<>();
        List<XMLNode> content = root.getContent();
        Iterator<XMLNode> it = content.iterator();
        while (it.hasNext()) {
            XMLNode node = it.next();
            newContent.add(node);
            if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
                Element e = (Element) node;
                if (!hasSegSource && e.getName().equals("source")) {
                    newContent.add(new TextNode("\n      "));
                    newContent.add(new Element("target"));
                }
                if (hasSegSource && e.getName().equals("seg-source")) {
                    newContent.add(new Element("target"));
                }
            }
        }
        root.setContent(newContent);
    }

    private static void recurse2x(Element root) throws SAXException, IOException, ParserConfigurationException {
        if ("file".equals(root.getName())) {
            String metadata = filesMetadata.get(String.valueOf(fileCounter++));
            if (metadata != null) {
                root.addContent(new PI("customMetadata", metadata));
            }
        }
        if ("unit".equals(root.getName()) && !root.getAttributeValue("translate").equals("no")) {
            List<Element> children = root.getChildren("segment");
            Iterator<Element> it = children.iterator();
            while (it.hasNext()) {
                Element seg = it.next();
                List<PI> list = seg.getPI(Constants.TOOLID);
                if (!list.isEmpty()) {
                    String pi = list.get(0).getData();
                    Element segment = segments.get(pi);
                    String metadata = segmentMetadata.get(pi);
                    if (metadata != null) {
                        root.addContent(new PI("customMetadata", metadata));
                    }
                    if (segment.getAttributeValue("approved").equals("yes")) {
                        Element target = seg.getChild("target");
                        if (target == null) {
                            addtarget(seg);
                            target = seg.getChild("target");
                        }
                        target.setContent(segment.getChild("target").getContent());
                        if (seg.getChild("source").getAttributeValue("xml:space").equals("preserve")) {
                            target.setAttribute("xml:space", "preserve");
                        }
                        if (!target.getChildren().isEmpty()) {
                            replaceTags(target, 2);
                        }
                        seg.setAttribute("state", "final");
                    }
                    if (!hasTarget && segment.getChild("target") != null) {
                        hasTarget = true;
                    }
                    seg.removePI(Constants.TOOLID);
                }
            }
        }
        List<Element> children = root.getChildren();
        Iterator<Element> it = children.iterator();
        while (it.hasNext()) {
            recurse2x(it.next());
        }
    }

    private static void loadXliff(String xliffFile) throws SAXException, IOException, ParserConfigurationException {
        SAXBuilder builder = new SAXBuilder();
        builder.setEntityResolver(catalog);
        Document xliff = builder.build(xliffFile);
        segments = new Hashtable<>();
        recurseXliff(xliff.getRootElement());
    }

    private static void recurseXliff(Element e) throws IOException, SAXException, ParserConfigurationException {
        if ("xliff".equals(e.getName()) && !"1.2".equals(e.getAttributeValue("version"))) {
            throw new IOException(Messages.getString("FromOpenXliff.2"));
        }
        if ("file".equals(e.getName()) && tgtLang.isEmpty()) {
            tgtLang = e.getAttributeValue("target-language");
            List<PI> pids = e.getPI("counter");
            String id = (pids.isEmpty()) ? e.getAttributeValue("original") : pids.get(0).getData();
            List<PI> metadata = e.getPI("customMetadata");
            if (!metadata.isEmpty()) {
                StringBuffer sb = new StringBuffer();
                for (PI pi : metadata) {
                    String meta = pi.getData();
                    sb.append(meta);
                }
                filesMetadata.put(id, sb.toString());
            }
        }
        if ("trans-unit".equals(e.getName())) {
            segments.put(e.getAttributeValue("id"), e);
            List<PI> metadata = e.getPI("customMetadata");
            if (!metadata.isEmpty()) {
                StringBuffer sb = new StringBuffer();
                for (PI pi : metadata) {
                    String meta = pi.getData();
                    sb.append(meta);
                }
                segmentMetadata.put(e.getAttributeValue("id"), sb.toString());
            }
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

    public static void restoreAttributes(Element e) {
        List<Attribute> atts = e.getAttributes();
        Iterator<Attribute> at = atts.iterator();
        Vector<String> change = new Vector<>();
        while (at.hasNext()) {
            Attribute a = at.next();
            if (a.getName().indexOf("__") != -1) {
                change.add(a.getName());
            }
        }
        for (int i = 0; i < change.size(); i++) {
            String name = change.get(i);
            Attribute a = e.getAttribute(name);
            e.setAttribute(name.replace("__", ":"), a.getValue());
            e.removeAttribute(name);
        }
        List<Element> children = e.getChildren();
        Iterator<Element> it = children.iterator();
        while (it.hasNext()) {
            restoreAttributes(it.next());
        }
    }

    private static void restoreMetadata(String outputFile)
            throws SAXException, IOException, ParserConfigurationException {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(outputFile);
        Element root = doc.getRootElement();
        recurseMetadata(root);
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            XMLOutputter outputter = new XMLOutputter();
            Indenter.indent(root, 2);
            outputter.preserveSpace(true);
            outputter.output(doc, out);
        }

    }

    private static void recurseMetadata(Element root) throws SAXException, IOException, ParserConfigurationException {
        if ("file".equals(root.getName()) || "unit".equals(root.getName())) {
            List<PI> pis = root.getPI("customMetadata");
            if (!pis.isEmpty()) {
                List<Element> metaGroups = parseMetadata(pis.get(0).getData());
                Element metadata = root.getChild("mda:metadata");
                if (metadata == null) {
                    metadata = new Element("mda:metadata");
                    List<XMLNode> content = root.getContent();
                    content.add(0, metadata);
                    root.setContent(content);
                }
                metadata.setContent(new Vector<>());
                for (Element group : metaGroups) {
                    if ("file".equals(root.getName())) {
                        String category = group.getAttributeValue("category");
                        if (category.equals("format") || category.equals("tool") || category.equals("PI")
                                || category.equals("sourceFile") || category.equals("document")) {
                            continue; // Skip standard metadata categories
                        }
                    }
                    metadata.addContent(group);
                }
                if (metadata.getChildren().isEmpty()) {
                    root.removeChild("mda:metadata");
                }
                root.removePI("customMetadata");
            }
        }
        if ("unit".equals(root.getName())) {
            return;
        }
        List<Element> children = root.getChildren();
        Iterator<Element> it = children.iterator();
        while (it.hasNext()) {
            recurseMetadata(it.next());
        }
    }

    private static List<Element> parseMetadata(String data)
            throws SAXException, IOException, ParserConfigurationException {
        String source = "<mda:metadata xmlns:mda=\"urn:oasis:names:tc:xliff:metadata:2.0\">" + data + "</mda:metadata>";
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)));
        return doc.getRootElement().getChildren();
    }
}