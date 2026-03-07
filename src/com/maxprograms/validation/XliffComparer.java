/*******************************************************************************
 * Copyright (c) 2018 - 2026 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/

package com.maxprograms.validation;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.converters.Utils;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.CatalogBuilder;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;

public class XliffComparer {

    private static Logger logger = System.getLogger(XliffComparer.class.getName());
    private static int differenceCount = 0;

    public static void main(String[] args) {
        String[] fixedArgs = Utils.fixPath(args);

        String file1 = "";
        String file2 = "";
        String catalog = "";

        for (int i = 0; i < fixedArgs.length; i++) {
            String arg = fixedArgs[i];
            if (arg.equals("-help")) {
                help();
                return;
            }
            if (arg.equals("-file1") && (i + 1) < fixedArgs.length) {
                file1 = fixedArgs[i + 1];
            }
            if (arg.equals("-file2") && (i + 1) < fixedArgs.length) {
                file2 = fixedArgs[i + 1];
            }
            if (arg.equals("-catalog") && (i + 1) < fixedArgs.length) {
                catalog = fixedArgs[i + 1];
            }
        }

        if (file1.isEmpty() || file2.isEmpty()) {
            logger.log(Level.ERROR, "Both -file1 and -file2 arguments are required");
            help();
            return;
        }

        if (catalog.isEmpty()) {
            String catalogPath = System.getenv("OpenXLIFF_HOME");
            if (catalogPath != null) {
                catalog = catalogPath + System.getProperty("file.separator") + "catalog" +
                        System.getProperty("file.separator") + "catalog.xml";
            }
        }

        try {
            Catalog cat = null;
            if (!catalog.isEmpty()) {
                cat = CatalogBuilder.getCatalog(catalog);
            }

            boolean result = compareXliff(file1, file2, cat);
            if (result) {
                System.out.println("Files are semantically equivalent");
                System.out.println("Total segments compared: " + differenceCount);
            } else {
                System.out.println("Files have differences");
                System.exit(1);
            }
        } catch (Exception e) {
            logger.log(Level.ERROR, "Error comparing files", e);
            System.exit(1);
        }
    }

    public static boolean compareXliff(String file1, String file2, Catalog catalog)
            throws SAXException, IOException, ParserConfigurationException {
        
        SAXBuilder builder = new SAXBuilder();
        if (catalog != null) {
            builder.setEntityResolver(catalog);
        }

        Document doc1 = builder.build(file1);
        Document doc2 = builder.build(file2);

        Element root1 = doc1.getRootElement();
        Element root2 = doc2.getRootElement();

        List<Element> files1 = root1.getChildren("file");
        List<Element> files2 = root2.getChildren("file");

        if (files1.size() != files2.size()) {
            System.out.println("Different number of files");
            System.out.println("  File1 has " + files1.size() + " files");
            System.out.println("  File2 has " + files2.size() + " files");
            return false;
        }

        boolean identical = true;
        int totalSegments = 0;

        for (int i = 0; i < files1.size(); i++) {
            Element fileElement1 = files1.get(i);
            Element fileElement2 = files2.get(i);
            
            String original1 = fileElement1.getAttributeValue("original", "");
            String original2 = fileElement2.getAttributeValue("original", "");
            
            List<Element> units1 = getUnits(fileElement1);
            List<Element> units2 = getUnits(fileElement2);
            
            boolean fileIdentical = compareFileUnits(original1, original2, i, units1, units2);
            if (fileIdentical) {
                totalSegments += units1.size();
            } else {
                identical = false;
            }
        }

        differenceCount = totalSegments;
        return identical;
    }

    private static boolean compareFileUnits(String original1, String original2, int fileIndex, 
                                           List<Element> units1, List<Element> units2) {
        if (!original1.equals(original2)) {
            System.out.println("Different original attribute at file position: " + fileIndex);
            System.out.println("  File1 original: " + original1);
            System.out.println("  File2 original: " + original2);
            return false;
        }
        
        if (units1.size() != units2.size()) {
            System.out.println("Different number of units in file: " + original1 + " (position " + fileIndex + ")");
            System.out.println("  File1 has " + units1.size() + " units");
            System.out.println("  File2 has " + units2.size() + " units");
            return false;
        }
        
        boolean identical = true;
        for (int i = 0; i < units1.size(); i++) {
            Element unit1 = units1.get(i);
            Element unit2 = units2.get(i);
            
            if (!unit1.equals(unit2)) {
                String id1 = unit1.getAttributeValue("id", String.valueOf(i));
                String id2 = unit2.getAttributeValue("id", String.valueOf(i));
                System.out.println("Difference in file: " + original1 + " (position " + fileIndex + "), unit position: " + i);
                System.out.println("  File1 unit id: " + id1);
                System.out.println("  File2 unit id: " + id2);
                System.out.println("  File1 unit: " + unit1.toString());
                System.out.println("  File2 unit: " + unit2.toString());
                identical = false;
            }
        }
        
        return identical;
    }

    private static List<Element> getUnits(Element fileElement) {
        // For XLIFF 2.x, get <unit> elements directly
        List<Element> units = fileElement.getChildren("unit");
        if (!units.isEmpty()) {
            return units;
        }
        
        // For XLIFF 1.x, get <trans-unit> elements from <body>
        Element body = fileElement.getChild("body");
        if (body != null) {
            return body.getChildren("trans-unit");
        }
        
        return new java.util.ArrayList<>();
    }

    private static void help() {
        String help = "Usage: XliffComparer -file1 <xliff1> -file2 <xliff2> [-catalog <catalog>]\n\n" +
                "Parameters:\n" +
                "  -file1 <xliff1>     First XLIFF file to compare\n" +
                "  -file2 <xliff2>     Second XLIFF file to compare\n" +
                "  -catalog <catalog>  XML catalog file (optional)\n" +
                "  -help               Display this help information\n\n" +
                "Compares two XLIFF files for semantic equivalence, ignoring:\n" +
                "  - Skeleton file references and paths\n" +
                "  - Whitespace and formatting differences\n" +
                "  - Attribute order\n\n" +
                "Exit codes:\n" +
                "  0 - Files are equivalent\n" +
                "  1 - Files differ or error occurred";
        System.out.println(help);
    }
}
