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
package com.maxprograms.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.SAXException;

import com.maxprograms.converters.Utils;

public class DTDParser {

    private static Logger logger = System.getLogger(DTDParser.class.getName());

    private Map<String, ElementDecl> elementDeclMap;
    private Map<String, AttlistDecl> attributeListMap;
    private Map<String, EntityDecl> entitiesMap;
    private Map<String, NotationDecl> notationsMap;

    private boolean debug = false;

    public DTDParser() {
        elementDeclMap = new HashMap<>();
        attributeListMap = new HashMap<>();
        entitiesMap = new HashMap<>();
        notationsMap = new HashMap<>();
    }

    public Grammar parse(File file) throws SAXException, IOException {
        String source = readFile(file);
        int pointer = 0;
        while (pointer < source.length()) {
            if (lookingAt("%", source, pointer)) {
                // Parameter-entity references
                int index = source.indexOf(";", pointer);
                if (index == -1) {
                    throw new SAXException("Malformed entity reference");
                }
                String entityName = source.substring(pointer + "%".length(), index);
                if (!entitiesMap.containsKey(entityName)) {
                    throw new SAXException("Referenced undeclared entity " + entityName);
                }
                EntityDecl entity = entitiesMap.get(entityName);
                String module = entity.getValue();
                if (module == null || module.isBlank()) {
                    throw new IOException("Error parsing referenced entity %" + entityName + ";");
                }
                String path = Utils.getAbsolutePath(file.getParentFile().getAbsolutePath(), module);
                File mod = new File(path);
                if (mod.exists()) {
                    parse(mod);
                } else {
                    if (debug) {
                        logger.log(Level.WARNING, "Module \"" + mod.getAbsolutePath() + "\" not found");
                    }
                }
                pointer += "%".length() + entityName.length() + ";".length();
            }
            if (lookingAt("<!ELEMENT", source, pointer)) {
                int index = source.indexOf(">", pointer);
                if (index == -1) {
                    throw new SAXException("Malformed element declaration");
                }
                String elementText = source.substring(pointer, index + ">".length());
                ElementDecl elementDecl = new ElementDecl(elementText);
                elementDeclMap.put(elementDecl.getName(), elementDecl);
                pointer += elementText.length();
                continue;
            }
            if (lookingAt("<!ATTLIST", source, pointer)) {
                int index = source.indexOf(">", pointer);
                if (index == -1) {
                    throw new SAXException("Malformed attribute declaration");
                }
                String attListText = source.substring(pointer, index + ">".length());
                AttlistDecl attList = new AttlistDecl(attListText);
                attributeListMap.put(attList.getListName(), attList);
                pointer += attListText.length();
                continue;
            }
            if (lookingAt("<!ENTITY", source, pointer)) {
                int index = source.indexOf(">", pointer);
                if (index == -1) {
                    throw new SAXException("Malformed entity declaration");
                }
                String entityDeclText = source.substring(pointer, index + ">".length());
                EntityDecl entityDecl = new EntityDecl(entityDeclText);
                if (!entitiesMap.containsKey(entityDecl.getName())) {
                    entitiesMap.put(entityDecl.getName(), entityDecl);
                } else {
                    if (debug) {
                        logger.log(Level.WARNING, "Duplicated entity declaration " + entityDecl);
                    }
                }
                pointer += entityDeclText.length();
                continue;
            }
            if (lookingAt("<!NOTATION", source, pointer)) {
                int index = source.indexOf(">", pointer);
                if (index == -1) {
                    throw new SAXException("Malformed notation declaration");
                }
                String notationDeclText = source.substring(pointer, index + ">".length());
                NotationDecl notation = new NotationDecl(notationDeclText);
                if (!notationsMap.containsKey(notation.getName())) {
                    notationsMap.put(notation.getName(), notation);
                }
                pointer += notationDeclText.length();
                continue;
            }
            if (lookingAt("<?", source, pointer)) {
                int index = source.indexOf("?>", pointer);
                if (index == -1) {
                    throw new SAXException("Malformed processing instruction");
                }
                String piText = source.substring(pointer, index + "?>".length());
                // ignore processing instructions
                pointer += piText.length();
                continue;
            }
            if (lookingAt("<!--", source, pointer)) {
                int index = source.indexOf("-->", pointer);
                if (index == -1) {
                    throw new SAXException("Malformed comment");
                }
                String commentText = source.substring(pointer, index);
                // ignore comments
                pointer += commentText.length() + "-->".length();
                continue;
            }
            if (lookingAt("]]>", source, pointer)) {
                pointer += +"]]>".length();
            }
            if (lookingAt("<![", source, pointer)) {
                int end = source.indexOf("]]>", pointer);
                String section = source.substring(pointer, end + "]]>".length());
                int open = count("<![", section);
                int close = count("]]>", section);
                while (open != close) {
                    end = source.indexOf("]]>", end + 1);
                    section = source.substring(pointer, end + "]]>".length());
                    open = count("<![", section);
                    close = count("]]>", section);
                }
                String type = getSectionType(section);
                if ("INCLUDE".equals(type)) {
                    int sectionStart = source.indexOf("[", pointer + "<![".length());
                    if (sectionStart == -1) {
                        throw new SAXException("Malformed conditional section");
                    }
                    String skip = source.substring(pointer, sectionStart + "[".length());
                    pointer += skip.length();
                } else if ("IGNORE".equals(type)) {
                    pointer += section.length();
                    continue;
                } else {
                    throw new SAXException("Malformed conditional section");
                }
            }
            if (pointer < source.length()) {
                char c = source.charAt(pointer);
                if (XMLUtils.isXmlSpace(c)) {
                    pointer++;
                    continue;
                }
                logger.log(Level.ERROR, "Text before: " + source.substring(pointer - 20, pointer));
                logger.log(Level.ERROR, "Text after:  " + source.substring(pointer, pointer + 20));
                throw new SAXException("Error parsing DTD " + file.getAbsolutePath());
            }
        }
        return new Grammar(elementDeclMap, attributeListMap, entitiesMap, notationsMap);
    }

    private int count(String target, String section) {
        int count = 0;
        int index = section.indexOf(target);
        while (index != -1) {
            count++;
            index = section.indexOf(target, index + 1);
        }
        return count;
    }

    private String getSectionType(String section) throws SAXException {
        int i = "<![".length();
        for (; i < section.length(); i++) {
            char c = section.charAt(i);
            if (!XMLUtils.isXmlSpace(c)) {
                break;
            }
        }
        StringBuilder sb = new StringBuilder();
        for (; i < section.length(); i++) {
            char c = section.charAt(i);
            if (XMLUtils.isXmlSpace(c) || c == '[') {
                break;
            }
            sb.append(c);
        }
        String type = sb.toString();
        while ((type.startsWith("%") || type.startsWith("&")) && type.endsWith(";")) {
            String entityName = type.substring(1, type.length() - 1);
            if (!entitiesMap.containsKey(entityName)) {
                throw new SAXException("Referenced undeclared entity " + entityName);
            }
            EntityDecl entity = entitiesMap.get(entityName);
            type = entity.getValue();
        }
        return type;
    }

    private boolean lookingAt(String search, String source, int start) {
        int length = search.length();
        if (length + start > source.length()) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (source.charAt(start + i) != search.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private String readFile(File file) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (FileReader reader = new FileReader(file)) {
            try (BufferedReader buffer = new BufferedReader(reader)) {
                String line = "";
                while ((line = buffer.readLine()) != null) {
                    if (!builder.isEmpty()) {
                        builder.append('\n');
                    }
                    builder.append(line);
                }
            }
        }
        return builder.toString();
    }

    public static void main(String[] args) {
        try {
            DTDParser parser = new DTDParser();
            parser.parse(new File("/Users/rmraya/Documents/GitHub/OpenXLIFF/catalog/docbook4.3/docbook.dtd"));
        } catch (SAXException | IOException e) {
            e.printStackTrace();
        }
    }
}
