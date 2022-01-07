/*******************************************************************************
 * Copyright (c) 2022 Maxprograms.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors: Maxprograms - initial API and implementation
 *******************************************************************************/
package com.maxprograms.converters.wpml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import com.maxprograms.converters.Constants;
import com.maxprograms.xml.CData;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.TextNode;
import com.maxprograms.xml.XMLNode;
import com.maxprograms.xml.XMLOutputter;

import org.xml.sax.SAXException;

public class Xliff2Wpml {

    private static Catalog catalog;
    private static Document skeleton;
    private static Map<String, Element> segments;

    private Xliff2Wpml() {
        // do not instantiate this class
        // use run method instead
    }

    public static List<String> run(Map<String, String> params) {
        List<String> result = new ArrayList<>();

        String xliffFile = params.get("xliff");
        String sklFile = params.get("skeleton");
        String outputFile = params.get("backfile");
        try {
            catalog = new Catalog(params.get("catalog"));
            loadXliff(xliffFile);
            loadSkeleton(sklFile);
            recurseSkeleton(skeleton.getRootElement());
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
                outputter.preserveSpace(true);
                outputter.output(skeleton, out);
            }
            result.add(Constants.SUCCESS);
        } catch (IOException | SAXException | ParserConfigurationException | URISyntaxException e) {
            Logger logger = System.getLogger(Xliff2Wpml.class.getName());
            logger.log(Level.ERROR, "Error merging WPML file.", e);
            result.add(Constants.ERROR);
            result.add(e.getMessage());
        }
        return result;
    }

    private static void recurseSkeleton(Element e) throws IOException {
        if ("trans-unit".equals(e.getName())) {
            List<XMLNode> content = new ArrayList<>();
            Element target = e.getChild("target");
            String text = target.getText();
            int index = text.indexOf("%%%");
            while (index != -1) {
                String start = text.substring(0, index);
                content.add(new TextNode(start));
                text = text.substring(index + "%%%".length());
                String code = text.substring(0, text.indexOf("%%%"));
                text = text.substring(code.length() + "%%%".length());
                Element segment = segments.get(code);
                if (segment != null) {
                    if (segment.getAttributeValue("approved").equalsIgnoreCase("yes")) {
                        content.addAll(segment.getChild("target").getContent());
                    } else {
                        content.addAll(segment.getChild("source").getContent());
                    }
                } else {
                    throw new IOException("Missing segment: " + code);
                }
                index = text.indexOf("%%%");
            }
            if (!text.isEmpty()) {
                content.add(new TextNode(text));
            }
            target.setContent(content);
            text = Wpml2Xliff.getText(target);
            target.setContent(new ArrayList<>());
            target.addContent(new CData(text));
        } else {
            List<Element> children = e.getChildren();
            Iterator<Element> it = children.iterator();
            while (it.hasNext()) {
                recurseSkeleton(it.next());
            }
        }
    }

    private static void loadSkeleton(String sklFile) throws SAXException, IOException, ParserConfigurationException {
        SAXBuilder builder = new SAXBuilder();
        builder.setEntityResolver(catalog);
        skeleton = builder.build(sklFile);
    }

    private static void loadXliff(String xliffFile) throws SAXException, IOException, ParserConfigurationException {
        SAXBuilder builder = new SAXBuilder();
        builder.setEntityResolver(catalog);
        Document xliff = builder.build(xliffFile);
        segments = new HashMap<>();
        recurseXliff(xliff.getRootElement());
    }

    private static void recurseXliff(Element e) {
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
}