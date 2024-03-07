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
package com.maxprograms.converters.resx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.xml.Xml2Xliff;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.XMLNode;
import com.maxprograms.xml.XMLOutputter;

import org.xml.sax.SAXException;

public class Resx2Xliff {

	private Resx2Xliff() {
		// do not instantiate this class
		// use run method instead
	}

	public static List<String> run(Map<String, String> params) {
		List<String> result = new ArrayList<>();
		try {
			String inputFile = params.get("source");
			String catalog = params.get("catalog");

			Document xmlResx = openXml(inputFile, catalog);

			Element root = xmlResx.getRootElement();
			List<Element> lstNodes = root.getChildren();
			for (int i = 0; i < lstNodes.size(); i++) {
				Element node = lstNodes.get(i);
				if (node.getName().equals("data") && isTrans(node) && !isSkipCommand(node)) {
					List<XMLNode> lstChilds = node.getContent();
					for (int j = 0; j < lstChilds.size(); j++) {
						XMLNode childNode = lstChilds.get(j);
						if (childNode.getNodeType() == XMLNode.ELEMENT_NODE) {
							Element eChild = (Element) childNode;
							if (eChild.getName().equals("value")) {
								Element newChild = new Element("translate");
								newChild.setContent(eChild.getContent());
								lstChilds.set(j, newChild);
							}
						}
					}
					node.setContent(lstChilds);
				}
			}
			String original = inputFile;
			File tempFile = File.createTempFile("tempResx", ".xml");
			inputFile = tempFile.getAbsolutePath();
			saveXml(xmlResx, inputFile);
			params.put("source", inputFile);
			params.put("resx", "yes");
			result = Xml2Xliff.run(params);
			Files.delete(Paths.get(tempFile.toURI()));
			if (Constants.SUCCESS.equals(result.get(0))) {
				setOriginal(params.get("xliff"), original);
			}
		} catch (IOException | ParserConfigurationException | SAXException | URISyntaxException e) {
			Logger logger = System.getLogger(Resx2Xliff.class.getName());
			logger.log(Level.ERROR, Messages.getString("Resx2Xliff.1"), e);
			result.add(Constants.ERROR);
			result.add(e.getMessage());
		}
		return result;
	}

	private static void setOriginal(String xliff, String original)
			throws IOException, SAXException, ParserConfigurationException {
		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(xliff);
		doc.getRootElement().getChild("file").setAttribute("original", original);
		saveXml(doc, xliff);
	}

	/*
	 * Each data row contains a name, and value. The row also contains a type or
	 * mimetype. Type corresponds to a .NET class that support text/value
	 * conversion. Classes that don't support this are serialized and stored with
	 * the mimetype set.
	 */

	static boolean isTrans(Element node) {
		if (node.getAttributeValue("name").startsWith(">>")) {
			return false;
		}
		if (node.getAttribute("mimetype") != null && !node.getAttributeValue("mimetype").trim().isEmpty()) {
			return false;
		}
		return !(node.getAttribute("type") != null && !node.getAttributeValue("type").trim().isEmpty());
	}

	static boolean isSkipCommand(Element node) {
		// Search for the _skip command in comment tag
		List<Element> children = node.getChildren("comment");
		for (int i = 0; i < children.size(); i++) {
			if (children.get(i).getText().equalsIgnoreCase("_skip")) {
				return true;
			}
		}
		return false;
	}

	static Document openXml(String filename, String catalog)
			throws ParserConfigurationException, SAXException, IOException, URISyntaxException {
		SAXBuilder builder = new SAXBuilder();
		builder.setValidating(false);
		builder.setEntityResolver(new Catalog(catalog));
		return builder.build(filename);
	}

	// Save the xml to a file
	static void saveXml(Document xmlDoc, String xmlFile) throws IOException {
		XMLOutputter outputter = new XMLOutputter();
		outputter.preserveSpace(true);
		try (FileOutputStream soutput = new FileOutputStream(xmlFile)) {
			outputter.output(xmlDoc, soutput);
		}
	}

}
