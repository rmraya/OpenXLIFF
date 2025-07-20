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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.maxprograms.converters.Constants;
import com.maxprograms.xml.Attribute;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.PI;
import com.maxprograms.xml.TextNode;
import com.maxprograms.xml.XMLNode;

public class Xliff1xProcessor {

    private static List<String> namespaces;
    private static int tag;
    private static boolean preserveSpaces = false;

    private Xliff1xProcessor() {
        // do not instantiate this class
        // use static methods instead
    }

    public static void processXliff1x(Element root, List<Element> units) {
        namespaces = new ArrayList<>();
        tag = 1;
        preserveSpaces = false;
        recurse1x(root, units);
    }

    private static void recurse1x(Element root, List<Element> units) {
        if ("xliff".equals(root.getName())) {
            List<Attribute> atts = root.getAttributes();
            Iterator<Attribute> it = atts.iterator();
            while (it.hasNext()) {
                Attribute a = it.next();
                if (a.getName().startsWith("xmlns:")) {
                    String ns = a.getName().substring("xmlns:".length());
                    if (!ns.equals("xml")) {
                        namespaces.add(ns);
                    }
                }
                if (a.getName().startsWith("x-workiva")) {
                    preserveSpaces = true;
                }
            }
            if (!namespaces.isEmpty()) {
                renameAttributes(root);
            }
        }
        if ("trans-unit".equals(root.getName()) && !root.getAttributeValue("translate").equals("no")) {
            Element segSource = root.getChild("seg-source");
            if (segSource != null) {
                List<XMLNode> content = segSource.getContent();
                Iterator<XMLNode> it = content.iterator();
                while (it.hasNext()) {
                    XMLNode node = it.next();
                    if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
                        Element e = (Element) node;
                        if ("mrk".equals(e.getName()) && "seg".equals(e.getAttributeValue("mtype"))) {
                            Element unit = new Element("trans-unit");
                            unit.setAttribute("id", "" + units.size());
                            unit.setAttribute("approved", root.getAttributeValue("approved", "no"));
                            boolean space = root.getAttributeValue("xml:space").equals("preserve");
                            if (space) {
                                unit.setAttribute("xml:space", "preserve");
                            }
                            Element source = new Element("source");
                            tag = 1;
                            source.setContent(getContent1x(e));
                            if (!hasTranslatableText(source)) {
                                return;
                            }
                            unit.addContent(source);
                            Element target = new Element("target");
                            Element tgt = root.getChild("target");
                            if (tgt != null) {
                                tag = 1;
                                Element mrk = locateMrk(tgt, e.getAttributeValue("mid"));
                                if (mrk != null) {
                                    target.setContent(getContent1x(mrk));
                                }
                            }
                            unit.addContent(target);
                            units.add(unit);
                            e.addContent(new PI(Constants.TOOLID, unit.getAttributeValue("id")));
                        }
                    }
                }
            } else {
                Element unit = new Element("trans-unit");
                unit.setAttribute("id", "" + units.size());
                unit.setAttribute("approved", root.getAttributeValue("approved", "no"));
                boolean space = root.getAttributeValue("xml:space").equals("preserve");
                if (space || preserveSpaces) {
                    unit.setAttribute("xml:space", "preserve");
                }

                Element source = new Element("source");
                tag = 1;
                source.setContent(getContent1x(root.getChild("source")));
                if (!hasTranslatableText(source)) {
                    return;
                }
                unit.addContent(source);
                Element target = new Element("target");
                tag = 1;
                target.setContent(getContent1x(root.getChild("target")));
                unit.addContent(target);

                List<Element> matches = root.getChildren("alt-trans");
                Iterator<Element> it = matches.iterator();
                while (it.hasNext()) {
                    Element match = it.next();
                    Element altTrans = new Element("alt-trans");
                    String origin = match.getAttributeValue("origin");
                    if (!origin.isEmpty()) {
                        altTrans.setAttribute("origin", origin);
                    }
                    String quality = match.getAttributeValue("match-quality");
                    if (!quality.isBlank()) {
                        altTrans.setAttribute("match-quality", quality);
                    }
                    Element altSource = new Element("source");
                    tag = 1;
                    altSource.setContent(getContent1x(match.getChild("source")));
                    altTrans.addContent(altSource);
                    Element altTarget = new Element("target");
                    tag = 1;
                    altTarget.setContent(getContent1x(match.getChild("target")));
                    altTrans.addContent(altTarget);
                    if (!altSource.getContent().isEmpty() && !altTarget.getContent().isEmpty()) {
                        unit.addContent(altTrans);
                    }
                }
                List<Element> notes = root.getChildren("note");
                Iterator<Element> noteIt = notes.iterator();
                while (noteIt.hasNext()) {
                    Element note = noteIt.next();
                    Element n = new Element("note");
                    n.setText(note.getText());
                    if (note.hasAttribute("priority")) {
                        try {
                            Integer.parseInt(note.getAttributeValue("priority"));
                            n.setAttribute("priority", note.getAttributeValue("priority"));
                        } catch (NumberFormatException e) {
                            // ignore invalid priority
                        }
                    }
                    if (note.hasAttribute("annotates")) {
                        String value = note.getAttributeValue("annotates");
                        if ("source".equals(value) || "target".equals(value)) {
                            n.setAttribute("annotates", value);
                        }
                    }
                    unit.addContent(n);
                }
                units.add(unit);
                root.addContent(new PI(Constants.TOOLID, unit.getAttributeValue("id")));
            }
            return;
        }
        List<Element> children = root.getChildren();
        Iterator<Element> it = children.iterator();
        while (it.hasNext()) {
            recurse1x(it.next(), units);
        }
    }

    private static boolean hasTranslatableText(Element e) {
        if (("source".equals(e.getName()) || "target".equals(e.getName()) || "mrk".equals(e.getName())
                || "pc".equals(e.getName()) || "sub".equals(e.getName())) && XliffUtils.hasText(e)) {
            return true;
        }
        List<Element> children = e.getChildren();
        Iterator<Element> it = children.iterator();
        while (it.hasNext()) {
            if (hasTranslatableText(it.next())) {
                return true;
            }
        }
        return false;
    }

    public static Element locateMrk(Element target, String mid) {
        List<XMLNode> content = target.getContent();
        Iterator<XMLNode> it = content.iterator();
        while (it.hasNext()) {
            XMLNode node = it.next();
            if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
                Element e = (Element) node;
                if ("mrk".equals(e.getName()) && e.getAttributeValue("mid").equals(mid)) {
                    return e;
                }
            }
        }
        return null;
    }

    private static List<XMLNode> getContent1x(Element child) {
        List<XMLNode> result = new Vector<>();
        if (child != null) {
            List<XMLNode> content = child.getContent();
            Iterator<XMLNode> it = content.iterator();
            while (it.hasNext()) {
                XMLNode node = it.next();
                if (node.getNodeType() == XMLNode.TEXT_NODE) {
                    result.add(node);
                }
                if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
                    Element e = (Element) node;
                    String name = e.getName();
                    if ("g".equals(name)) {
                        Element ph1 = new Element("ph");
                        ph1.setAttribute("id", "" + tag++);
                        ph1.setText(XliffUtils.getHead(e));
                        result.add(ph1);
                        if (e.getChildren().isEmpty()) {
                            result.add(new TextNode(e.getText()));
                        } else {
                            result.addAll(getContent1x(e));
                        }
                        Element ph2 = new Element("ph");
                        ph2.setAttribute("id", "" + tag++);
                        ph2.setText("</g>");
                        result.add(ph2);
                    }
                    if ("x".equals(name) || "bx".equals(name) || "ex".equals(name) || "ph".equals(name)
                            || "bpt".equals(name) || "ept".equals(name) || "it".equals(name)) {
                        Element ph = new Element("ph");
                        ph.setAttribute("id", "" + tag++);
                        ph.setText(e.toString());
                        result.add(ph);
                    }
                    if ("mrk".equals(name)) {
                        // add <mrk> as is
                        result.add(e);
                    }
                }
            }
        }
        return result;
    }

    private static void renameAttributes(Element e) {
        List<Attribute> atts = e.getAttributes();
        Iterator<Attribute> at = atts.iterator();
        Vector<String> change = new Vector<>();
        while (at.hasNext()) {
            Attribute a = at.next();
            if (a.getName().indexOf(":") != -1) {
                String ns = a.getName().substring(0, a.getName().indexOf(":"));
                if (namespaces.contains(ns)) {
                    change.add(a.getName());
                }
            }
        }
        for (int i = 0; i < change.size(); i++) {
            String name = change.get(i);
            Attribute a = e.getAttribute(name);
            e.setAttribute(name.replace(":", "__"), a.getValue());
            e.removeAttribute(name);
        }
        List<Element> children = e.getChildren();
        Iterator<Element> it = children.iterator();
        while (it.hasNext()) {
            renameAttributes(it.next());
        }
    }
}
