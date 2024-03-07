/*******************************************************************************
 * Copyright (c) 2022 - 2024 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/
package com.maxprograms.converters.sdlppx;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.ParserConfigurationException;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.sdlxliff.Xliff2Sdl;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.XMLOutputter;

import org.xml.sax.SAXException;

public class Xliff2Sdlrpx {

    private static ZipOutputStream out;

    private static String srcLang;
    private static String tgtLang;

    private Xliff2Sdlrpx() {
        // do not instantiate this class
        // use run method instead
    }

    public static List<String> run(Map<String, String> params) {
        List<String> result = new ArrayList<>();
        srcLang = "";
        tgtLang = "";
        Map<String, String> filesMap = new HashMap<>();

        String sklFile = params.get("skeleton");
        String xliffFile = params.get("xliff");
        String catalog = params.get("catalog");
        String outputFile = params.get("backfile");

        try {
            SAXBuilder builder = new SAXBuilder();
            builder.setEntityResolver(new Catalog(catalog));
            Document doc = builder.build(xliffFile);
            Element root = doc.getRootElement();
            List<Element> files = root.getChildren("file");
            Iterator<Element> it = files.iterator();

            XMLOutputter outputter = new XMLOutputter();
            outputter.preserveSpace(true);
            while (it.hasNext()) {
                Element file = it.next();
                Element header = file.getChild("header");
                if (header == null) {
                    throw new SAXException(Messages.getString("Xliff2Sdlrpx.0"));
                }
                Element skl = header.getChild("skl");
                if (skl == null) {
                    throw new SAXException("Missing <skl>");
                }
                List<Element> propGroups = header.getChildren("prop-group");
                if (propGroups == null) {
                    throw new SAXException(Messages.getString("Xliff2Sdlrpx.1"));
                }
                String sdlxliffFile = "";
                for (int i = 0; i < propGroups.size(); i++) {
                    Element propGroup = propGroups.get(i);
                    if (propGroup.getAttributeValue("name").equals("document")) {
                        List<Element> props = propGroup.getChildren("prop");
                        for (int j = 0; j < props.size(); j++) {
                            Element prop = props.get(j);
                            if (prop.getAttributeValue("prop-type").equals("original")) {
                                sdlxliffFile = prop.getText();
                            }
                        }
                    }
                }
                if (sdlxliffFile.isEmpty()) {
                    throw new SAXException(Messages.getString("Xliff2Sdlrpx.2"));
                }
                if (tgtLang.isEmpty()) {
                    tgtLang = file.getAttributeValue("target-language");
                }
                if (srcLang.isEmpty()) {
                    srcLang = file.getAttributeValue("source-language");
                }
                Document d = new Document(null, "xliff", null, null);
                Element r = d.getRootElement();
                r.setAttribute("version", "1.2");
                r.setAttribute("xmlns", "urn:oasis:names:tc:xliff:document:1.2");
                r.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
                r.setAttribute("xsi:schemaLocation",
                        "urn:oasis:names:tc:xliff:document:1.2 xliff-core-1.2-transitional.xsd");
                r.addContent(file);

                File tempXliff = File.createTempFile("tmp", ".xlf");
                try (FileOutputStream outFile = new FileOutputStream(tempXliff)) {
                    outputter.output(d, outFile);
                }
                filesMap.put(sdlxliffFile, tempXliff.getAbsolutePath());
            }

            out = new ZipOutputStream(new FileOutputStream(outputFile));
            try (ZipInputStream in = new ZipInputStream(new FileInputStream(sklFile))) {
                ZipEntry entry = null;
                while ((entry = in.getNextEntry()) != null) {
                    File f = new File(entry.getName());
                    String name = f.getName();
                    String extension = name.substring(name.lastIndexOf('.'));
                    File tmp = File.createTempFile("tmp", extension);
                    try (FileOutputStream output = new FileOutputStream(tmp.getAbsolutePath())) {
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            output.write(buf, 0, len);
                        }
                    }

                    if (name.endsWith(".skl")) {
                        File back = File.createTempFile("tmp", ".sdlxliff");
                        String xliff = entry.getName();
                        xliff = xliff.substring(0, xliff.length() - 4);
                        Map<String, String> map = new HashMap<>();
                        map.put("xliff", filesMap.get(xliff));
                        map.put("skeleton", tmp.getAbsolutePath());
                        map.put("catalog", catalog);
                        map.put("backfile", back.getAbsolutePath());
                        List<String> res = Xliff2Sdl.run(map);
                        if (!res.get(0).equals(Constants.SUCCESS)) {
                            return res;
                        }
                        saveEntry(xliff, back.getAbsolutePath());
                        Files.delete(back.toPath());
                    } else if (name.endsWith(".sdlproj")) {
                        // update project
                        updateProjectFile(tmp);
                        saveEntry(updateProjectName(entry.getName()), tmp.getAbsolutePath());
                    } else {
                        // store as is
                        saveEntry(entry.getName(), tmp.getAbsolutePath());
                    }
                    Files.delete(tmp.toPath());

                }
            }
            out.close();

            Set<String> keySet = filesMap.keySet();
            Iterator<String> kt = keySet.iterator();
            while (kt.hasNext()) {
                String file = filesMap.get(kt.next());
                Files.delete(new File(file).toPath());
            }

            result.add(Constants.SUCCESS);
        } catch (IOException | SAXException | ParserConfigurationException | URISyntaxException e) {
            Logger logger = System.getLogger(Xliff2Sdlrpx.class.getName());
            logger.log(Level.ERROR, Messages.getString("Xliff2Sdlrpx.3"), e);
            result.add(Constants.ERROR);
            result.add(e.getMessage());
        }
        return result;
    }

    private static String updateProjectName(String string) {
        String name = string;
        if (name.endsWith(".sdlproj")) {
            name = name.substring(0, name.length() - ".sdlproj".length());
            int last = name.lastIndexOf('-');
            if (last != -1) {
                name = name.substring(0, last);
                last = name.lastIndexOf('-');
                if (last != -1) {
                    name = name.substring(0, last);
                    Date now = new Date();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'-'HH'h'mm'm'ss's'");
                    return name + '-' + sdf.format(now) + ".sdlproj";
                }
            }
        }
        return string;
    }

    private static void updateProjectFile(File tmp) throws SAXException, IOException, ParserConfigurationException {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(tmp);
        Element root = doc.getRootElement();
        root.setAttribute("PackageType", "ReturnPackage");
        Element termbase = root.getChild("TermbaseConfiguration");
        if (termbase != null) {
            root.removeChild(termbase);
        }
        root.removeChild("TermbaseConfiguration");
        Element tasks = root.getChild("Tasks");
        if (tasks != null) {
            Element automatic = tasks.getChild("AutomaticTask");
            if (automatic != null) {
                tasks.removeChild(automatic);
            }
        }
        recurseProject(root);
        XMLOutputter outputter = new XMLOutputter();
        outputter.preserveSpace(true);
        try (FileOutputStream output = new FileOutputStream(tmp)) {
            outputter.output(doc, output);
        }
    }

    private static void recurseProject(Element node) {
        List<Element> children = node.getChildren();
        Iterator<Element> it = children.iterator();
        while (it.hasNext()) {
            Element child = it.next();
            if (child.getName().equals("Reports")) {
                child.setContent(new ArrayList<>());
            }
            if (child.getName().equals("LanguageFile")) {
                if (child.getAttributeValue("LanguageCode").equalsIgnoreCase(tgtLang)) {
                    Element versions = child.getChild("FileVersions");
                    List<Element> files = versions.getChildren("FileVersion");
                    Iterator<Element> ft = files.iterator();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'0000Z'");
                    while (ft.hasNext()) {
                        Element file = ft.next();
                        Date now = new Date();
                        file.setAttribute("FileTimeStamp", sdf.format(now));
                    }
                } else if (child.getAttributeValue("LanguageCode").equalsIgnoreCase(srcLang)) {
                    // leave source files unchanged
                } else {
                    // remove any other language
                    node.removeChild(child);
                }
            } else {
                recurseProject(child);
            }
        }

    }

    private static void saveEntry(String name, String file) throws IOException {
        ZipEntry content = new ZipEntry(name);
        content.setMethod(ZipEntry.DEFLATED);
        out.putNextEntry(content);
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] array = new byte[1024];
            int len;
            while ((len = input.read(array)) > 0) {
                out.write(array, 0, len);
            }
            out.closeEntry();
        }
    }
}