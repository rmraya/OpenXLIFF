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
package com.maxprograms.converters.plaintext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.lang.System.Logger.Level;
import java.lang.System.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.TextNode;
import com.maxprograms.xml.XMLNode;

public class Xliff2Text {

	private static String xliffFile;
	private static String encoding;
	private static Hashtable<String, Element> segments;
	private static Catalog catalog;
	private static FileOutputStream output;

	private Xliff2Text() {
		// do not instantiate this class
		// use run method instead
	}

	public static Vector<String> run(Hashtable<String, String> params) {

		Vector<String> result = new Vector<>();

		String sklFile = params.get("skeleton");
		xliffFile = params.get("xliff");
		encoding = params.get("encoding");

		try {
			catalog = new Catalog(params.get("catalog"));
			String outputFile = params.get("backfile");
			File f = new File(outputFile);
			if (!f.getParentFile().exists()) {
				f.getParentFile().mkdirs();
			}
			if (!f.exists()) {
				Files.createFile(Paths.get(f.toURI()));
			}
			output = new FileOutputStream(f);
			loadSegments();
			try (InputStreamReader input = new InputStreamReader(new FileInputStream(sklFile),
					StandardCharsets.UTF_8)) {
				BufferedReader buffer = new BufferedReader(input);
				String line;
				while ((line = buffer.readLine()) != null) {
					line = line + "\n";

					if (line.indexOf("%%%") != -1) {
						//
						// contains translatable text
						//
						int index = line.indexOf("%%%");
						while (index != -1) {
							String start = line.substring(0, index);
							writeString(start);
							line = line.substring(index + 3);
							String code = line.substring(0, line.indexOf("%%%"));
							line = line.substring(line.indexOf("%%%") + 3);
							Element segment = segments.get(code);
							if (segment != null) {
								Element target = segment.getChild("target");
								Element source = segment.getChild("source");
								if (target != null) {
									if (segment.getAttributeValue("approved", "no").equals("yes")) {
										writeString(extractText(target));
									} else {
										writeString(extractText(source));
									}
								} else {
									writeString(extractText(source));
								}
							} else {
								result.add(0, "1");
								result.add(1, "segment " + code + " not found");
								return result;
							}

							index = line.indexOf("%%%");
							if (index == -1) {
								writeString(line);
							}
						} // end while
					} else {
						//
						// non translatable portion
						//
						writeString(line);
					}
				}
			}
			output.close();
			result.add("0");

		} catch (IOException | SAXException | ParserConfigurationException e) {
			Logger logger = System.getLogger(Xliff2Text.class.getName());
			logger.log(Level.ERROR, "Error merging TEXT file", e);
			result.add("1");
			result.add(e.getMessage());
		}
		return result;
	}

	private static String extractText(Element target) {
		String result = "";
		List<XMLNode> content = target.getContent();
		Iterator<XMLNode> i = content.iterator();
		while (i.hasNext()) {
			XMLNode n = i.next();
			if (n.getNodeType() == XMLNode.ELEMENT_NODE) {
				Element e = (Element) n;
				result = result + extractText(e);
			}
			if (n.getNodeType() == XMLNode.TEXT_NODE) {
				result = result + ((TextNode) n).getText();
			}
		}
		return result;
	}

	private static void loadSegments() throws SAXException, IOException, ParserConfigurationException {

		SAXBuilder builder = new SAXBuilder();
		if (catalog != null) {
			builder.setEntityResolver(catalog);
		}

		Document doc = builder.build(xliffFile);
		Element root = doc.getRootElement();
		Element body = root.getChild("file").getChild("body");
		List<Element> units = body.getChildren("trans-unit");
		Iterator<Element> i = units.iterator();

		segments = new Hashtable<>();

		while (i.hasNext()) {
			Element unit = i.next();
			segments.put(unit.getAttributeValue("id"), unit);
		}
	}

	private static void writeString(String string) throws IOException {
		output.write(string.getBytes(encoding));
	}

}
