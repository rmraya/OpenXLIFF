/*******************************************************************************
 * Copyright (c) 2018 - 2025 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/
package com.maxprograms.converters.qti;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.Join;
import com.maxprograms.converters.Utils;
import com.maxprograms.converters.sdlppx.Sdlppx2Xliff;
import com.maxprograms.converters.sdlxliff.Sdl2Xliff;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.Indenter;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.XMLOutputter;

public class Qtip2Xliff {

    private static String inputFile;
    private static String skeleton;
    private static ZipOutputStream out;

    public static List<String> run(Map<String, String> params) {

        List<String> result = new ArrayList<>();

        inputFile = params.get("source");
        String xliff = params.get("xliff");
        skeleton = params.get("skeleton");
        String sourceLanguage = params.get("srcLang");
        String targetLanguage = params.get("tgtLang");
        if (targetLanguage == null) {
            targetLanguage = "";
        }
        try {
            out = new ZipOutputStream(new FileOutputStream(skeleton));
            List<String> xliffList = new ArrayList<>();
            try (ZipInputStream in = new ZipInputStream(new FileInputStream(inputFile))) {
                ZipEntry entry = null;
                while ((entry = in.getNextEntry()) != null) {
                    String entryName = entry.getName();
                    String parent = "";
                    String name = entryName;
                    if (entryName.indexOf("\\") != -1) {
                        parent = entryName.substring(0, entryName.indexOf("\\"));
                        name = entryName.substring(entryName.indexOf("\\") + 1);
                    }
                    if (entryName.indexOf("/") != -1) {
                        parent = entryName.substring(0, entryName.indexOf("/"));
                        name = entryName.substring(entryName.indexOf("/") + 1);
                    }
                    if (name.toLowerCase().endsWith(".xml")) {
                        File tmp = File.createTempFile(name.substring(0, name.lastIndexOf('.')), ".xml");
                        try (FileOutputStream output = new FileOutputStream(tmp.getAbsolutePath())) {
                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = in.read(buf)) > 0) {
                                output.write(buf, 0, len);
                            }
                        }

                        Map<String, String> table = new HashMap<>();
                        table.put("source", tmp.getAbsolutePath());
                        table.put("xliff", tmp.getAbsolutePath() + ".xlf");
                        table.put("skeleton", tmp.getAbsolutePath() + ".skl");
                        table.put("catalog", params.get("catalog"));
                        table.put("srcEncoding", params.get("srcEncoding"));
                        table.put("paragraph", params.get("paragraph"));
                        table.put("srxFile", params.get("srxFile"));
                        table.put("format", params.get("format"));
                        table.put("srcLang", sourceLanguage);
                        table.put("tgtLang", targetLanguage);
                        List<String> res = Sdl2Xliff.run(table);
                        if (Constants.SUCCESS.equals(res.get(0))) {
                            updateXliff(tmp.getAbsolutePath() + ".xlf", entry.getName());
                            ZipEntry content = new ZipEntry(entry.getName() + ".skl");
                            content.setMethod(ZipEntry.DEFLATED);
                            out.putNextEntry(content);
                            try (FileInputStream input = new FileInputStream(tmp.getAbsolutePath() + ".skl")) {
                                byte[] array = new byte[1024];
                                int len;
                                while ((len = input.read(array)) > 0) {
                                    out.write(array, 0, len);
                                }
                                out.closeEntry();
                            }
                            File skl = new File(tmp.getAbsolutePath() + ".skl");
                            Files.delete(skl.toPath());
                            File xlf = new File(tmp.getAbsolutePath() + ".xlf");
                            xliffList.add(xlf.getAbsolutePath());
                        } else {
                            saveEntry(entry, tmp.getAbsolutePath());
                        }
                        Files.delete(tmp.toPath());
                    } else {
                        // preserve untranslatable files
                        File tmp = File.createTempFile("zip", ".tmp");
                        try (FileOutputStream output = new FileOutputStream(tmp.getAbsolutePath())) {
                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = in.read(buf)) > 0) {
                                output.write(buf, 0, len);
                            }
                        }
                        saveEntry(entry, tmp.getAbsolutePath());
                        Files.delete(tmp.toPath());
                    }
                }

            }
            out.close();

            // generate final XLIFF

            Join.join(xliffList, xliff);

            for (int i = 0; i < xliffList.size(); i++) {
                File xlf = new File(xliffList.get(i));
                Files.delete(Paths.get(xlf.toURI()));
            }

            result.add(Constants.SUCCESS);
        } catch (IOException | SAXException | ParserConfigurationException e) {
            Logger logger = System.getLogger(Sdlppx2Xliff.class.getName());
            logger.log(Level.ERROR, Messages.getString("Qtip2Xliff.2"), e);
            result.add(Constants.ERROR);
            result.add(e.getMessage());
        }
        return result;
    }

    private static void saveEntry(ZipEntry entry, String name) throws IOException {
        ZipEntry content = new ZipEntry(entry.getName());
        content.setMethod(ZipEntry.DEFLATED);
        out.putNextEntry(content);
        try (FileInputStream input = new FileInputStream(name)) {
            byte[] array = new byte[1024];
            int len;
            while ((len = input.read(array)) > 0) {
                out.write(array, 0, len);
            }
            out.closeEntry();
        }
    }

    private static void updateXliff(String xliff, String original)
            throws SAXException, IOException, ParserConfigurationException {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(xliff);
        Element root = doc.getRootElement();
        Element file = root.getChild("file");
        file.setAttribute("datatype", "x-qtipackage");
        file.setAttribute("original", Utils.cleanString(inputFile));
        Element header = file.getChild("header");
        Element propGroup = new Element("prop-group");
        propGroup.setAttribute("name", "document");
        Element prop = new Element("prop");
        prop.setAttribute("prop-type", "original");
        prop.setText(original);
        propGroup.addContent(prop);
        header.addContent(propGroup);

        Element ext = header.getChild("skl").getChild("external-file");
        ext.setAttribute("href", Utils.cleanString(skeleton));

        XMLOutputter outputter = new XMLOutputter();
        outputter.preserveSpace(true);
        Indenter.indent(root, 2);
        try (FileOutputStream output = new FileOutputStream(xliff)) {
            outputter.output(doc, output);
        }
    }
}
