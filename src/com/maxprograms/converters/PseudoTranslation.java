/*******************************************************************************
 * Copyright (c) 2023 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/
package com.maxprograms.converters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.Indenter;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.TextNode;
import com.maxprograms.xml.XMLNode;
import com.maxprograms.xml.XMLOutputter;

public class PseudoTranslation {

    private static Logger logger = System.getLogger(PseudoTranslation.class.getName());
    private static String version;

    public static void main(String[] args) {

        String[] arguments = Utils.fixPath(args);
        String xliff = "";
        String catalog = "";

        for (int i = 0; i < arguments.length; i++) {
            String arg = arguments[i];
            if (arg.equals("-help")) {
                help();
                return;
            }
            if (arg.equals("-xliff") && (i + 1) < arguments.length) {
                xliff = arguments[i + 1];
            }
            if (arg.equals("-catalog") && (i + 1) < arguments.length) {
                catalog = arguments[i + 1];
            }
        }
        if (arguments.length < 2) {
            help();
            return;
        }
        if (catalog.isEmpty()) {
            String home = System.getenv("OpenXLIFF_HOME");
			if (home == null) {
				home = System.getProperty("user.dir");
			}
            File catalogFolder = new File(new File(home), "catalog");
            if (!catalogFolder.exists()) {
                logger.log(Level.ERROR, "'catalog' folder not found.");
                return;
            }
            catalog = new File(catalogFolder, "catalog.xml").getAbsolutePath();
        }
        File catalogFile = new File(catalog);
        if (!catalogFile.exists()) {
            logger.log(Level.ERROR, "Catalog file does not exist.");
            return;
        }

        try {
            pseudoTranslate(xliff, catalog);
        } catch (SAXException | IOException | ParserConfigurationException | URISyntaxException e) {
            logger.log(Level.ERROR, e);
        }
    }

    private static void help() {
        String launcher = "   pseudotranslate.sh ";
        if (System.getProperty("file.separator").equals("\\")) {
            launcher = "   pseudotranslate.bat ";
        }
        String help = "Usage:\n\n" + launcher + """
[-help] -xliff xliffFile [-catalog catalogFile]

Where:

    -help:      (optional) Display this help information and exit
    -xliff:     XLIFF file to pseudo-translate
    -catalog:   (optional) XML catalog to use for processing

""";
        System.out.println(help);
    }

    public static void pseudoTranslate(String xliff, String catalog)
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException {
        SAXBuilder builder = new SAXBuilder();
        builder.setEntityResolver(new Catalog(catalog));
        Document doc = builder.build(xliff);
        Element root = doc.getRootElement();
        if (!"xliff".equals(root.getName())) {
            throw new IOException("Selected file is not an XLIFF document");
        }
        version = root.getAttributeValue("version");
        recurse(root);
        Indenter.indent(root, 2);
        XMLOutputter outputter = new XMLOutputter();
        outputter.preserveSpace(true);
        try (FileOutputStream out = new FileOutputStream(xliff)) {
            outputter.output(doc, out);
        }
    }

    private static void recurse(Element root) throws IOException {
        if (("xliff".equals(root.getName()) && version.startsWith("2.") && root.getAttributeValue("trgLang").isEmpty())
                || ("file".equals(root.getName()) && version.startsWith("1.")
                        && root.getAttributeValue("target-language").isEmpty())) {
            throw new IOException("Missing target language declaration");
        }
        if (("file".equals(root.getName()) || "group".equals(root.getName()) || "trans-unit".equals(root.getName())
                || "unit".equals(root.getName()))
                && "no".equals(root.getAttributeValue("translate"))) {
            return;
        }
        if ("trans-unit".equals(root.getName()) && "yes".equals(root.getAttributeValue("approved"))) {
            return;
        }
        if ("segment".equals(root.getName()) && "final".equals(root.getAttributeValue("state"))) {
            return;
        }
        if (("trans-unit".equals(root.getName()) && root.getChild("seg-source") == null)
                || "segment".equals(root.getName())) {
            Element target = root.getChild("target");
            if (target == null) {
                addTarget(root);
                target = root.getChild("target");
            }
            if (!target.getContent().isEmpty()) {
                return;
            }
            Element source = root.getChild("source");
            target.setContent(translate(source).getContent());
            if ("preserve".equals(source.getAttributeValue("xml:space"))) {
                target.setAttribute("xml:space", "preserve");
            }
            if ("segment".equals(root.getName())) {
                root.setAttribute("state", "translated");
            }
        }
        List<Element> children = root.getChildren();
        Iterator<Element> it = children.iterator();
        while (it.hasNext()) {
            recurse(it.next());
        }
    }

    private static void addTarget(Element root) {
        List<XMLNode> newContent = new Vector<>();
        List<XMLNode> content = root.getContent();
        Iterator<XMLNode> it = content.iterator();
        while (it.hasNext()) {
            XMLNode node = it.next();
            if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
                newContent.add(node);
                if ("source".equals(((Element) node).getName())) {
                    newContent.add(new Element("target"));
                }
            } else {
                newContent.add(node);
            }
        }
        root.setContent(newContent);
    }

    private static Element translate(Element source) {
        Element target = new Element("target");
        List<XMLNode> content = source.getContent();
        Iterator<XMLNode> it = content.iterator();
        while (it.hasNext()) {
            XMLNode node = it.next();
            if (node.getNodeType() == XMLNode.TEXT_NODE) {
                String text = ((TextNode) node).getText();
                target.addContent(replace(text));
            }
            if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
                target.addContent((Element) node);
            }
        }
        return target;
    }

    private static String replace(String text) {
        text = text.replace('a', 'á');
        text = text.replace('e', 'è');
        text = text.replace('i', 'î');
        text = text.replace('o', 'ö');
        text = text.replace('u', 'ú');
        text = text.replace('A', 'Á');
        text = text.replace('E', 'È');
        text = text.replace('I', 'Î');
        text = text.replace('O', 'Ö');
        text = text.replace('U', 'Ú');
        return text;
    }
}
