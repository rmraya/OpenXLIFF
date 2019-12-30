/*******************************************************************************
 * Copyright (c) 2003-2019 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/
package com.maxprograms.xliff2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import com.maxprograms.converters.Constants;
import com.maxprograms.segmenter.Segmenter;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.Indenter;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.TextNode;
import com.maxprograms.xml.XMLNode;
import com.maxprograms.xml.XMLOutputter;

import org.xml.sax.SAXException;

public class Resegmenter {

    private static Segmenter segmenter;

    public static List<String> run(String xliff, String srx, String srcLang, String catalog) {
        List<String> result = new ArrayList<>();
        try {
            segmenter = new Segmenter(srx, srcLang, catalog);
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(xliff);
            Element root = doc.getRootElement();
            recurse(root);
            try (FileOutputStream out = new FileOutputStream(new File(xliff))) {
                XMLOutputter outputter = new XMLOutputter();
                outputter.preserveSpace(true);
                Indenter.indent(root, 2);
                outputter.output(doc, out);
            }
            result.add(Constants.SUCCESS);
        } catch (SAXException | IOException | ParserConfigurationException | URISyntaxException e) {
            Logger logger = System.getLogger(Resegmenter.class.getName());
            logger.log(Level.ERROR, "Error re-segmenting XLIFF 2.0", e);
            result.add(Constants.ERROR);
            result.add(e.getMessage());
        }
        return result;
    }

    private static void recurse(Element root) throws SAXException, IOException, ParserConfigurationException {
        if ("unit".equals(root.getName())) {
            root.removeAttribute("canResegment");
            Element segment = root.getChild("segment");
            Element source = segment.getChild("source");
            Element segSource = segmenter.segment(source);
            if (segSource.getChildren("mrk").size() > 1) {
                root.removeChild(segment);
                List<XMLNode> content = segSource.getContent();
                Iterator<XMLNode> it = content.iterator();

                while (it.hasNext()) {
                    XMLNode n = it.next();
                    switch (n.getNodeType()) {
                    case XMLNode.TEXT_NODE:
                        TextNode text = (TextNode) n;
                        System.out.println("Text node found: \\" + text.toString() + "\\");
                        break;
                    case XMLNode.ELEMENT_NODE:
                        Element e = (Element) n;
                        if ("mrk".equals(e.getName()) && "seg".equals(e.getAttributeValue("mtype"))
                                && !e.getContent().isEmpty()) {
                            Element newSeg = new Element("segment");
                            newSeg.setAttribute("id", root.getAttributeValue("id") + '-' + e.getAttributeValue("mid"));
                            root.addContent(newSeg);
                            Element newSource = new Element("source");
                            newSource.setAttribute("xml:space", source.getAttributeValue("xml:space", "default"));
                            newSeg.addContent(newSource);
                            newSource.addContent(e.getContent());
                        } else {
                            throw new SAXException("Unexpected element found: " + e.toString());
                        }
                        break;
                    default:
                        // ignore
                    }
                }
            }
        } else {
            List<Element> children = root.getChildren();
            Iterator<Element> it = children.iterator();
            while (it.hasNext()) {
                recurse(it.next());
            }
        }
    }
}