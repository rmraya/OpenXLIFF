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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.xml.XMLConstants;
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
    private Set<String> visited;
    private boolean divsRemoved;
    private File baseURI;

    public RelaxNGParser(String file, Catalog catalog) throws SAXException, IOException, ParserConfigurationException {
        this.catalog = catalog;
        baseURI = new File(file).getParentFile();
        builder = new SAXBuilder();
        builder.setEntityResolver(catalog);
        doc = builder.build(file);
        root = doc.getRootElement();
        defaultPrefix = root.getNamespace();
        defaultNamespace = root.getAttributeValue("xmlns");
        if (XMLConstants.RELAXNG_NS_URI.equals(defaultNamespace)) {
            defaultNamespace = "";
        }
        removeForeign(root);
        replaceExternalRef(root);
        replaceIncludes(root);
        do {
            divsRemoved = false;
            removeDivs(root);
        } while (divsRemoved);
        nameAttribute(root);
    }

    public Map<String, Map<String, String>> getElements() {
        Map<String, Map<String, String>> result = new Hashtable<>();
        definitions = new HashMap<>();
        harvestDefinitions(root);
        elements = new Vector<>();
        harvestElements(root);
        Iterator<Element> it = elements.iterator();
        while (it.hasNext()) {
            Element e = it.next();
            attributes = new Vector<>();
            visited = new TreeSet<>();
            getAttributes(e);
            Map<String, String> map = new Hashtable<>();
            for (int i = 0; i < attributes.size(); i++) {
                Element attribute = attributes.get(i);
                List<Attribute> atts = attribute.getAttributes();
                for (int j = 0; j < atts.size(); j++) {
                    Attribute a = atts.get(j);
                    if ("defaultValue".equals(a.getLocalName())) {
                        String name = attribute.getChild("name").getText();
                        if (attribute.getChild("name").getText().indexOf(':') != -1 && !name.startsWith("xml:")) {
                            continue;
                        }
                        map.put(name, a.getValue());
                    }
                }
            }
            if (map.size() > 0) {
                result.put(e.getChild("name").getText(), map);
            }
        }
        return result;
    }

    private void getAttributes(Element e) {
        if ("attribute".equals(e.getName())) {
            attributes.add(e);
            return;
        }
        if ("ref".equals(e.getName())) {
            String name = e.getAttributeValue("name");
            if (!visited.contains(name)) {
                visited.add(name);
                Element definition = definitions.get(name);
                if (definition != null) {
                    getAttributes(definition);
                }
            }
            return;
        }
        List<Element> children = e.getChildren();
        Iterator<Element> it = children.iterator();
        while (it.hasNext()) {
            Element child = it.next();
            if ("element".equals(child.getName())) {
                return;
            }
            getAttributes(child);
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

    private Element getRootElement() {
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
                    if (system == null) {
                        File f = new File(baseURI, child.getAttributeValue("href"));
                        if (f.exists()) {
                            system = f.getAbsolutePath();
                        }
                    }
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
                    if (system == null) {
                        File f = new File(baseURI, child.getAttributeValue("href"));
                        if (f.exists()) {
                            system = f.getAbsolutePath();
                        }
                    }
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
        if ("element".equals(e.getName()) && e.getChild("name") != null) {
            elements.add(e);
        }
        List<Element> children = e.getChildren();
        Iterator<Element> it = children.iterator();
        while (it.hasNext()) {
            harvestElements(it.next());
        }
    }

    private void harvestDefinitions(Element e) {
        if ("define".equals(e.getName())) {
            String name = e.getAttributeValue("name");
            if (definitions.containsKey(name)) {
                Element old = definitions.get(name);
                old.addContent(e.getContent());
                definitions.put(name, old);
            } else {
                definitions.put(name, e);
            }
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
}
