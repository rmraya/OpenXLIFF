/*******************************************************************************
 * Copyright (c) 2003-2019 Maxprograms.
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.xml.Xliff2Xml;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.XMLNode;
import com.maxprograms.xml.XMLOutputter;

import org.xml.sax.SAXException;

public class Xliff2Resx {

	private Xliff2Resx() {
		// do not instantiate this class
		// use run method instead
	}

	public static List<String> run(Map<String, String> params) {
		List<String> result = new ArrayList<>();
		try {
			result = Xliff2Xml.run(params);
			if (!Constants.SUCCESS.equals(result.get(0))) {
				return result;
			}
			String inputFile = params.get("backfile");
			String catalog = params.get("catalog");

			Document xmlResx = openXml(inputFile, catalog);
			Element root = xmlResx.getRootElement();
			List<Element> lstNodes = root.getChildren();
			for (int i = 0; i < lstNodes.size(); i++) {
				Element node = lstNodes.get(i);
				List<XMLNode> lstChilds = node.getContent();
				for (int j = 0; j < lstChilds.size(); j++) {
					if (lstChilds.get(j).getNodeType() == XMLNode.ELEMENT_NODE) {
						Element child = (Element) lstChilds.get(j);
						if (isConvNode(child)) {
							Element newChild = new Element("value");
							newChild.setContent(child.getContent());
							lstChilds.set(j, newChild);
						}
					}
				}
				node.setContent(lstChilds);
			}

			saveXml(xmlResx, inputFile);
			result.add(Constants.SUCCESS);
		} catch (IOException | ParserConfigurationException | SAXException | URISyntaxException e) {
			Logger logger = System.getLogger(Xliff2Resx.class.getName());
			logger.log(Level.ERROR, "Error merging ResX file", e);
			result.add(Constants.ERROR);
			result.add(e.getMessage());
		}
		return result;
	}

	static boolean isConvNode(Element node) {
		return node.getName().equals("translate");
	}

	static Document openXml(String filename, String catalog)
			throws ParserConfigurationException, SAXException, IOException, URISyntaxException {
		SAXBuilder builder = new SAXBuilder();
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
