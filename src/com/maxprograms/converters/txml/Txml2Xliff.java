/*******************************************************************************
 * Copyright (c) 2003, 2019 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/
package com.maxprograms.converters.txml;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.lang.System.Logger.Level;
import java.lang.System.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.converters.Utils;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.XMLNode;
import com.maxprograms.xml.XMLOutputter;

public class Txml2Xliff {

	private static String inputFile;
	private static String xliffFile;
	private static String skeletonFile;
	private static String sourceLanguage;
	private static String targetLanguage;
	private static String catalog;
	private static FileOutputStream output;
	private static int tagId;
	private static int segNum;
	private static Document doc;
	private static String srcEncoding;

	private Txml2Xliff() {
		// do not instantiate this class
		// use run method instead
	}

	public static Vector<String> run(Hashtable<String, String> params) {
		Vector<String> result = new Vector<String>();
		inputFile = params.get("source");
		xliffFile = params.get("xliff");
		skeletonFile = params.get("skeleton");
		sourceLanguage = params.get("srcLang");
		targetLanguage = params.get("tgtLang");
		catalog = params.get("catalog");
		srcEncoding = params.get("srcEncoding");

		try {
			SAXBuilder builder = new SAXBuilder();
			builder.setEntityResolver(new Catalog(catalog));
			doc = builder.build(inputFile);
			Element root = doc.getRootElement();

			output = new FileOutputStream(xliffFile);
			writeHeader(inputFile, skeletonFile);
			recurse(root);
			writeEnd();
			output.close();

			try (FileOutputStream skl = new FileOutputStream(skeletonFile)) {
				XMLOutputter outputter = new XMLOutputter();
				outputter.output(doc, skl);
			}

			result.add("0");
		} catch (IOException | SAXException | ParserConfigurationException e) {
			Logger logger = System.getLogger(Txml2Xliff.class.getName());
			logger.log(Level.ERROR, "Error converting TXML file", e);
			result.add("1");
			if (e.getMessage() != null) {
				result.add(e.getMessage());
			} else {
				result.add("Unknown error");
			}
		}
		return result;
	}

	private static void writeHeader(String source, String skeleton) throws IOException {
		String tgtLang = "";
		if (targetLanguage != null) {
			tgtLang = "\" target-language=\"" + targetLanguage;
		}

		writeStr("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
		writeStr(
				"<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"urn:oasis:names:tc:xliff:document:1.2 xliff-core-1.2-transitional.xsd\">\n");
		writeStr("<?encoding " + srcEncoding + "?>\n");
		writeStr("<file datatype=\"x-txml\" original=\"" + Utils.cleanString(source) + "\" source-language=\""
				+ sourceLanguage + tgtLang + "\">\n");
		writeStr("<header>\n");
		writeStr("  <skl>\n");
		writeStr("   <external-file href=\"" + Utils.cleanString(skeleton) + "\"/>\n");
		writeStr("  </skl>\n");
		writeStr("</header>\n");
		writeStr("<body>\n");
	}

	private static void recurse(Element root) throws IOException {
		List<Element> children = root.getChildren();
		Iterator<Element> it = children.iterator();
		while (it.hasNext()) {
			Element child = it.next();
			if (child.getName().equals("segment") || child.getName().equals("localizable")) {
				parseSegment(child);
			} else {
				recurse(child);
			}
		}
	}

	private static void parseSegment(Element segment) throws IOException {
		segNum++;
		tagId = 0;
		writeStr("<trans-unit id=\"" + segNum + "\">\n");
		Element src = segment.getChild("source");
		Element tgt = segment.getChild("target");
		writeStr(parseElement(src) + "\n");
		if (tgt == null) {
			tgt = new Element("target");
			segment.addContent(tgt);
		}
		tagId = 0;
		writeStr(parseElement(tgt) + "\n");
		Element comments = segment.getChild("comments");
		if (comments != null) {
			parseComments(comments);
		}
		writeStr("</trans-unit>\n");
	}

	private static String parseElement(Element ele) {
		StringBuilder result = new StringBuilder();
		result.append("<" + ele.getName() + ">");
		List<XMLNode> content = ele.getContent();
		Iterator<XMLNode> it = content.iterator();
		while (it.hasNext()) {
			XMLNode o = it.next();
			if (o.getNodeType() == XMLNode.TEXT_NODE) {
				result.append(o.toString());
			}
			if (o.getNodeType() == XMLNode.ELEMENT_NODE) {
				Element tag = (Element) o;
				result.append(parseTag(tag));
			}
		}
		result.append("</" + ele.getName() + ">");
		if (ele.getName().equals("target")) {
			ele.setContent(new Vector<XMLNode>());
			ele.setText("%%%" + segNum + "%%%");
		}
		return result.toString();
	}

	private static String parseTag(Element tag) {
		tagId++;
		return "<ph id=\"" + tagId + "\">" + Utils.cleanString(tag.toString()) + "</ph>";
	}

	private static void parseComments(Element comments) throws IOException {
		List<Element> list = comments.getChildren("comment");
		Iterator<Element> it = list.iterator();
		while (it.hasNext()) {
			Element comment = it.next();
			writeStr("<note>");
			writeStr(Utils.cleanString(comment.getText()));
			writeStr("</note>\n");
		}
	}

	private static void writeEnd() throws IOException {
		writeStr("</body>\n");
		writeStr("</file>\n");
		writeStr("</xliff>\n");
	}

	private static void writeStr(String string) throws IOException {
		output.write(string.getBytes(StandardCharsets.UTF_8));
	}

}
