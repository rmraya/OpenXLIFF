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
import java.text.MessageFormat;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.FileFormats;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.SAXBuilder;

public class QtiCheck {

    private static final Logger logger = System.getLogger(QtiCheck.class.getName());

    private QtiCheck() {
        // Do not instantiate this class
    }

    public static List<String> validateFile(String inputFile, Catalog catalog) {

        List<String> result = new Vector<>();
        try {
            SAXBuilder builder = new SAXBuilder();
            builder.setEntityResolver(catalog);
            builder.setValidating(true);
            builder.build(inputFile);
            result.add(Constants.SUCCESS);
        } catch (Exception e) {
            MessageFormat mf = new MessageFormat(Messages.getString("QtiCheck.1"));
            logger.log(Level.ERROR, mf.format(new String[]{inputFile, e.getMessage()}), e);
            result.add(Constants.ERROR);
            result.add(e.getMessage());
        }
        return result;
    }

    public static List<String> validatePackage(String packageFile, Catalog catalog) {
        List<String> result = new Vector<>();
        boolean hasManifest = false;
        try {
            File folder = new File(new File(System.getProperty("java.io.tmpdir")), "" + System.currentTimeMillis());
            Files.createDirectories(folder.toPath());
            try (ZipInputStream in = new ZipInputStream(new FileInputStream(packageFile))) {
                ZipEntry entry = null;
                while ((entry = in.getNextEntry()) != null) {
                    String entryName = entry.getName();
                    String name = entryName;
                    if (entryName.indexOf("\\") != -1) {
                        name = entryName.replace("\\", "/");
                    }
                    File file = new File(folder, name);
                    if ("imsmanifest.xml".equals(file.getName().toLowerCase())) {
                        hasManifest = true;
                    }
                    if (entry.isDirectory()) {
                        Files.createDirectories(file.toPath());
                    } else {
                        Files.createDirectories(file.getParentFile().toPath());
                        try (FileOutputStream output = new FileOutputStream(file)) {
                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = in.read(buf)) > 0) {
                                output.write(buf, 0, len);
                            }
                        }
                    }
                    String format = FileFormats.detectFormat(file.getAbsolutePath());
                    if (FileFormats.QTI.equals(format) || FileFormats.XML.equals(format)) {
                        result = validateFile(file.getAbsolutePath(), catalog);
                        if (!Constants.SUCCESS.equals(result.get(0))) {
                            break;
                        }
                    }
                }
            }
            deleteFiles(folder);
        } catch (Exception e) {
            logger.log(Level.ERROR, e);
            result.add(Constants.ERROR);
            result.add(e.getMessage());
        }
        if (result.isEmpty()) {
            result.add(Constants.SUCCESS);
        }
        if (Constants.SUCCESS.equals(result.get(0)) && !hasManifest) {
            result.clear();
            result.add(Constants.ERROR);
            result.add(Messages.getString("QtiCheck.0"));
        }

        return result;
    }

    private static void deleteFiles(File f) throws IOException {
        if (f.isDirectory()) {
            File[] list = f.listFiles();
            for (File file : list) {
                deleteFiles(file);
            }
        } else {
            Files.delete(f.toPath());
        }
    }
}
