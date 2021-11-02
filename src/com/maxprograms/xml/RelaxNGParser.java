/*******************************************************************************
 * Copyright (c)  Maxprograms.
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class RelaxNGParser {

    private Catalog catalog;
    private SAXBuilder builder;
    private String defaultNamespace;
    private String defaultPrefix;
    private Map<String, Element> definitions;
    private Document doc;
    private Element root;
    private List<Element> elements;
    private List<Element> attributes;
    private Map<String, Element> cachedDefinitions;
    private boolean divsRemoved;
    private boolean choicesReduced;
    private boolean optionalRemoved;

    public RelaxNGParser(String file, Catalog catalog) throws SAXException, IOException, ParserConfigurationException {
        this.catalog = catalog;
        builder = new SAXBuilder();
        builder.setEntityResolver(catalog);
        doc = builder.build(file);
        root = doc.getRootElement();
        defaultPrefix = root.getNamespace();
        defaultNamespace = root.getAttributeValue("xmlns");
        if ("http://relaxng.org/ns/structure/1.0".equals(defaultNamespace)) {
            defaultNamespace = "";
        }
        removeForeign(root);
        replaceExternalRef(root);
        replaceIncludes(root);
        nameAttribute(root);
        do {
            divsRemoved = false;
            removeDivs(root);
        } while (divsRemoved);
        do {
            choicesReduced = false;
            reduceChoices(root);
        } while (choicesReduced);
        reduceChoices(root);
        do {
            optionalRemoved = false;
            removeOptional(root);
        } while (optionalRemoved);
        removeAttributes(root);
        removeDefines(root);
    }

    public Map<String, Map<String, String>> getElements() {
        Map<String, Map<String, String>> result = new Hashtable<>();
        definitions = new HashMap<>();
        harvestDefinitions(root);
        elements = new Vector<>();
        harvestElements(root);
        cachedDefinitions = new Hashtable<>();
        for (int i = 0; i < elements.size(); i++) {
            Element element = elements.get(i);
            replaceReferences(element);
            attributes = new Vector<>();
            harvestAttributes(element);
            Map<String, String> map = new Hashtable<>();
            for (int j = 0; j < attributes.size(); j++) {
                Element a = attributes.get(j);
                map.put(a.getChild("name").getText(), getDefaultValue(a));
            }
            result.put(element.getChild("name").getText(), map);
        }
        return result;
    }

    private String getDefaultValue(Element a) {
        List<Attribute> atts = a.getAttributes();
        Iterator<Attribute> it = atts.iterator();
        while (it.hasNext()) {
            Attribute at = it.next();
            if ("defaultValue".equals(at.getLocalName())) {
                return at.getValue();
            }
        }
        return null;
    }

    private void replaceReferences(Element e) {
        List<XMLNode> newContent = new Vector<>();
        List<XMLNode> content = e.getContent();
        Iterator<XMLNode> it = content.iterator();
        while (it.hasNext()) {
            XMLNode node = it.next();
            if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
                Element child = (Element) node;
                if ("ref".equals(child.getName())) {
                    String name = child.getAttributeValue("name");
                    if (definesElement(name)) {
                        continue;
                    }
                    Element define = definitions.get(name);
                    if (define != null) {
                        if (cachedDefinitions.containsKey(name)) {
                            define = cachedDefinitions.get(name);
                        } else {
                            replaceReferences(define);
                            cachedDefinitions.put(name, define);
                        }
                        newContent.add(define);
                    }
                } else {
                    newContent.add(node);
                }
            } else {
                newContent.add(node);
            }
        }
        e.setContent(newContent);
        List<Element> children = e.getChildren();
        Iterator<Element> tt = children.iterator();
        while (tt.hasNext()) {
            Element child = tt.next();
            replaceReferences(child);
        }
    }

    private void nameAttribute(Element e) {
        if ("element".equals(e.getName()) && e.hasAttribute("name")) {
            Element name = new Element("name");
            String value = e.getAttributeValue("name");
            name.setText(value);
            if (e.hasAttribute("ns")) {
                name.setAttribute("ns", e.getAttributeValue("ns"));
                e.removeAttribute("ns");
            }
            e.removeAttribute("name");
            e.getContent().add(0, name);
        }
        if ("attribute".equals(e.getName()) && e.hasAttribute("name")) {
            Element name = new Element("name");
            String value = e.getAttributeValue("name");
            name.setText(value);
            name.setAttribute("ns", e.getAttributeValue("ns"));
            if (e.hasAttribute("ns")) {
                e.removeAttribute("ns");
            }
            e.removeAttribute("name");
            e.getContent().add(0, name);
        }
        List<Element> children = e.getChildren();
        Iterator<Element> it = children.iterator();
        while (it.hasNext()) {
            Element child = it.next();
            nameAttribute(child);
        }
    }

    public Document getDocument() {
        return doc;
    }

    public Element getRootElement() {
        return root;
    }

    private void removeForeign(Element e) throws SAXException, IOException, ParserConfigurationException {
        List<XMLNode> newContent = new Vector<>();
        List<XMLNode> content = e.getContent();
        Iterator<XMLNode> it = content.iterator();
        while (it.hasNext()) {
            XMLNode node = it.next();
            if (node.getNodeType() == XMLNode.TEXT_NODE) {
                newContent.add(node);
            }
            if (node.getNodeType() == XMLNode.PROCESSING_INSTRUCTION_NODE) {
                newContent.add(node);
            }
            if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
                Element child = (Element) node;
                if (!defaultPrefix.equals(child.getNamespace())) {
                    continue;
                }
                if (!defaultNamespace.equals(child.getAttributeValue("xmlns"))) {
                    continue;
                }
                removeForeign(child);
                newContent.add(child);
            }
        }
        e.setContent(newContent);
    }

    private void replaceExternalRef(Element e) throws SAXException, IOException, ParserConfigurationException {
        List<XMLNode> newContent = new Vector<>();
        List<XMLNode> content = e.getContent();
        Iterator<XMLNode> it = content.iterator();
        while (it.hasNext()) {
            XMLNode node = it.next();
            if (node.getNodeType() == XMLNode.TEXT_NODE) {
                TextNode text = (TextNode) node;
                if (!text.toString().isBlank()) {
                    newContent.add(node);
                }
            }
            if (node.getNodeType() == XMLNode.PROCESSING_INSTRUCTION_NODE) {
                newContent.add(node);
            }
            if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
                Element child = (Element) node;
                if ("externalRef".equals(child.getName())) {
                    String system = catalog.matchSystem(null, child.getAttributeValue("href"));
                    if (system != null) {
                        RelaxNGParser parser = new RelaxNGParser(system, catalog);
                        newContent.add(parser.getRootElement());
                    } else {
                        throw new SAXException("Missing " + child.getAttributeValue("href") + " in <externalRef>");
                    }
                    continue;
                }
                replaceIncludes(child);
                newContent.add(child);
            }
        }
        e.setContent(newContent);
    }

    private void replaceIncludes(Element e) throws SAXException, IOException, ParserConfigurationException {
        List<XMLNode> newContent = new Vector<>();
        List<XMLNode> content = e.getContent();
        Iterator<XMLNode> it = content.iterator();
        while (it.hasNext()) {
            XMLNode node = it.next();
            if (node.getNodeType() == XMLNode.TEXT_NODE) {
                TextNode text = (TextNode) node;
                if (!text.toString().isBlank()) {
                    newContent.add(node);
                }
            }
            if (node.getNodeType() == XMLNode.PROCESSING_INSTRUCTION_NODE) {
                newContent.add(node);
            }
            if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
                Element child = (Element) node;
                if ("include".equals(child.getName())) {
                    String system = catalog.matchSystem(null, child.getAttributeValue("href"));
                    if (system != null) {
                        RelaxNGParser parser = new RelaxNGParser(system, catalog);
                        Element div = new Element("div");
                        div.addContent(parser.getRootElement());
                        List<Element> children = child.getChildren();
                        for (int i = 0; i < children.size(); i++) {
                            div.addContent(children.get(i));
                        }
                        newContent.add(div);
                    } else {
                        throw new SAXException("Missing " + child.getAttributeValue("href") + " in <include>");
                    }
                    continue;
                }
                replaceIncludes(child);
                newContent.add(child);
            }
        }
        e.setContent(newContent);
    }

    private void harvestElements(Element e) {
        if ("element".equals(e.getName())) {
            elements.add(e);
        }
        List<Element> children = e.getChildren();
        Iterator<Element> it = children.iterator();
        while (it.hasNext()) {
            harvestElements(it.next());
        }
    }

    private void harvestAttributes(Element e) {
        if ("attribute".equals(e.getName())) {
            attributes.add(e);
        }
        List<Element> children = e.getChildren();
        Iterator<Element> it = children.iterator();
        while (it.hasNext()) {
            harvestAttributes(it.next());
        }
    }

    private void harvestDefinitions(Element e) {
        if ("define".equals(e.getName())) {
            definitions.put(e.getAttributeValue("name"), e);
        }
        List<Element> children = e.getChildren();
        Iterator<Element> it = children.iterator();
        while (it.hasNext()) {
            harvestDefinitions(it.next());
        }
    }

    private void removeDivs(Element e) {
        List<XMLNode> newContent = new Vector<>();
        List<XMLNode> content = e.getContent();
        Iterator<XMLNode> it = content.iterator();
        while (it.hasNext()) {
            XMLNode node = it.next();
            if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
                Element child = (Element) node;
                if ("div".equals(child.getLocalName())) {
                    newContent.addAll(child.getContent());
                    divsRemoved = true;
                } else {
                    newContent.add(node);
                }
            } else {
                newContent.add(node);
            }
        }
        e.setContent(newContent);
        List<Element> children = e.getChildren();
        Iterator<Element> tt = children.iterator();
        while (tt.hasNext()) {
            removeDivs(tt.next());
        }
    }

    private void removeOptional(Element e) {
        List<XMLNode> newContent = new Vector<>();
        List<XMLNode> content = e.getContent();
        Iterator<XMLNode> it = content.iterator();
        while (it.hasNext()) {
            XMLNode node = it.next();
            if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
                Element child = (Element) node;
                if ("optional".equals(child.getLocalName())) {
                    newContent.addAll(child.getContent());
                    optionalRemoved = true;
                } else {
                    newContent.add(node);
                }
            } else {
                newContent.add(node);
            }
        }
        e.setContent(newContent);
        List<Element> children = e.getChildren();
        Iterator<Element> tt = children.iterator();
        while (tt.hasNext()) {
            removeOptional(tt.next());
        }
    }

    private void reduceChoices(Element e) {
        List<XMLNode> newContent = new Vector<>();
        List<XMLNode> content = e.getContent();
        Iterator<XMLNode> it = content.iterator();
        while (it.hasNext()) {
            XMLNode node = it.next();
            if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
                Element child = (Element) node;
                if ("choice".equals(child.getLocalName())) {
                    if (child.getChildren().size() > 2) {
                        Element choice = new Element("choice");
                        choice.addContent(child.getChildren().get(0));
                        choice.addContent(child.getChildren().get(1));
                        child.getChildren().remove(0);
                        child.getChildren().remove(0);
                        child.getChildren().add(0, choice);
                        choicesReduced = true;
                    } else {
                        newContent.add(node);
                    }

                } else {
                    newContent.add(node);
                }
            } else {
                newContent.add(node);
            }
        }
        e.setContent(newContent);
        List<Element> children = e.getChildren();
        Iterator<Element> tt = children.iterator();
        while (tt.hasNext()) {
            reduceChoices(tt.next());
        }
    }

    private void removeAttributes(Element e) {
        List<XMLNode> newContent = new Vector<>();
        List<XMLNode> content = e.getContent();
        Iterator<XMLNode> it = content.iterator();
        while (it.hasNext()) {
            XMLNode node = it.next();
            if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
                Element child = (Element) node;
                if ("attribute".equals(child.getLocalName())) {
                    if (hasDefault(child)) {
                        newContent.add(node);
                    }
                } else {
                    newContent.add(node);
                }
            } else {
                newContent.add(node);
            }
        }
        e.setContent(newContent);
        List<Element> children = e.getChildren();
        Iterator<Element> tt = children.iterator();
        while (tt.hasNext()) {
            removeAttributes(tt.next());
        }
    }

    private boolean hasDefault(Element e) {
        List<Attribute> atts = e.getAttributes();
        Iterator<Attribute> it = atts.iterator();
        while (it.hasNext()) {
            Attribute a = it.next();
            if ("defaultValue".equals(a.getLocalName())) {
                return true;
            }
        }
        return false;
    }

    private void removeDefines(Element e) {
        List<XMLNode> newContent = new Vector<>();
        List<XMLNode> content = e.getContent();
        Iterator<XMLNode> it = content.iterator();
        while (it.hasNext()) {
            XMLNode node = it.next();
            if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
                Element child = (Element) node;
                if ("define".equals(child.getLocalName())) {
                    if (hasContent(child)) {
                        newContent.add(node);
                    }
                } else {
                    newContent.add(node);
                }
            } else {
                newContent.add(node);
            }
        }
        e.setContent(newContent);
        List<Element> children = e.getChildren();
        Iterator<Element> tt = children.iterator();
        while (tt.hasNext()) {
            removeDefines(tt.next());
        }
    }

    private boolean hasContent(Element define) {
        if (define.getChildren().isEmpty()) {
            return false;
        }
        if (define.getChildren().size() == 1 && "empty".equals(define.getChildren().get(0).getName())) {
            return false;
        }
        return true;
    }

    private boolean definesElement(String name) {
        if (definitions.containsKey(name)) {
            Element define = definitions.get(name);
            List<Element> children = define.getChildren();
            for (int i = 0; i < children.size(); i++) {
                Element child = children.get(i);
                if ("element".equals(child.getName())) {
                    return true;
                }
                if ("ref".equals(child.getName())) {
                    String def = child.getAttributeValue("name");
                    if (definesElement(def)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
