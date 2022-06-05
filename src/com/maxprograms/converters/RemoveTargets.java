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
package com.maxprograms.converters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.Indenter;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.XMLOutputter;

public class RemoveTargets {

    private static Logger logger = System.getLogger(RemoveTargets.class.getName());

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
            File catalogFolder = new File(new File(System.getProperty("user.dir")), "catalog");
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
            removeTargets(xliff, catalog);
        } catch (SAXException | IOException | ParserConfigurationException | URISyntaxException e) {
            logger.log(Level.ERROR, e);
        }
    }

    private static void removeTargets(String xliff, String catalog)
            throws IOException, SAXException, ParserConfigurationException, URISyntaxException {
        SAXBuilder builder = new SAXBuilder();
        builder.setEntityResolver(new Catalog(catalog));
        Document doc = builder.build(xliff);
        Element root = doc.getRootElement();
        recurse(root);
        Indenter.indent(root, 2);
        XMLOutputter outputter = new XMLOutputter();
        outputter.preserveSpace(true);
        try (FileOutputStream out = new FileOutputStream(xliff)) {
            outputter.output(doc, out);
        }
    }

    private static void recurse(Element root) {
        if (("file".equals(root.getName()) || "group".equals(root.getName()) || "trans-unit".equals(root.getName())
                || "unit".equals(root.getName()))
                && "no".equals(root.getAttributeValue("translate"))) {
            return;
        }
        if ("trans-unit".equals(root.getName()) || "segment".equals(root.getName())) {
            Element target = root.getChild("target");
            if (target != null) {
                root.removeChild("target");
                if ("segment".equals(root.getName()) && root.hasAttribute("state")) {
                    root.setAttribute("state", "initial");
                }
                if ("trans-unit".equals(root.getName()) && root.hasAttribute("approved")) {
                    root.removeAttribute("approved");
                }
            }
        }
        List<Element> children = root.getChildren();
        Iterator<Element> it = children.iterator();
        while (it.hasNext()) {
            recurse(it.next());
        }
    }

    private static void help() {
        String launcher = "   removetargets.sh ";
        if (System.getProperty("file.separator").equals("\\")) {
            launcher = "   removetargets.bat ";
        }
        String help = "Usage:\n\n" + launcher
                + "[-help] -xliff xliffFile [-catalog catalogFile]\n\n"
                + "Where:\n\n"
                + "   -help:      (optional) Display this help information and exit\n"
                + "   -xliff:     XLIFF file to process\n"
                + "   -catalog:   (optional) XML catalog to use for processing\n";
        System.out.println(help);
    }

}
