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
package com.maxprograms.converters.ts;

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

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.Utils;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.XMLNode;
import com.maxprograms.xml.XMLOutputter;

public class Ts2Xliff {
	private static String sourceLanguage;
	private static String targetLanguage;
	private static String skeletonFile;
	private static String xliffFile;
	private static String inputFile;
	private static int segId;
	private static FileOutputStream output;
	private static Document doc;

	private Ts2Xliff() {
		// do not instantiate this class
		// use run method instead
	}

	public static Vector<String> run(Hashtable<String, String> params) {
		Vector<String> result = new Vector<>();
		segId = 0;
		inputFile = params.get("source");
		xliffFile = params.get("xliff");
		skeletonFile = params.get("skeleton");
		sourceLanguage = params.get("srcLang");
		targetLanguage = params.get("tgtLang");
		String srcEncoding = params.get("srcEncoding");
		String tgtLang = "";
		if (targetLanguage != null) {
			tgtLang = "\" target-language=\"" + targetLanguage;
		}

		try {
			output = new FileOutputStream(xliffFile);
			writeString("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			writeString("<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" "
					+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
					+ "xsi:schemaLocation=\"urn:oasis:names:tc:xliff:document:1.2 xliff-core-1.2-transitional.xsd\">\n");
			writeString("<?encoding " + srcEncoding + "?>\n");

			writeString("<file original=\"" + inputFile + "\" source-language=\"" + sourceLanguage + tgtLang
					+ "\" tool-id=\"" + Constants.TOOLID + "\" datatype=\"x-ts\">\n");
			writeString("<header>\n");
			writeString("   <skl>\n");
			writeString("      <external-file href=\"" + skeletonFile + "\"/>\n");
			writeString("   </skl>\n");
			writeString("   <tool tool-version=\"" + Constants.VERSION + " " + Constants.BUILD + "\" tool-id=\""
					+ Constants.TOOLID + "\" tool-name=\"" + Constants.TOOLNAME + "\"/>\n");
			writeString("</header>\n");
			writeString("<body>\n");

			SAXBuilder builder = new SAXBuilder();
			doc = builder.build(inputFile);
			recurse(doc.getRootElement());

			try (FileOutputStream skl = new FileOutputStream(skeletonFile)) {
				XMLOutputter outputter = new XMLOutputter();
				outputter.preserveSpace(true);
				outputter.output(doc, skl);
			}

			writeString("</body>\n");
			writeString("</file>\n");
			writeString("</xliff>");
			output.close();

			result.add(Constants.SUCCESS);
		} catch (IOException | SAXException | ParserConfigurationException e) {
			Logger logger = System.getLogger(Ts2Xliff.class.getName());
			logger.log(Level.ERROR, "Error converting .ts file", e);
			result.add(Constants.ERROR);
			result.add(e.getMessage());
		}

		return result;
	}

	private static void recurse(Element e) throws IOException {
		if (e.getName().equals("message")) {
			Element source = e.getChild("source");
			Element target = e.getChild("translation");
			target.setAttribute("id", "" + segId);
			String targetText = getTarget(target);
			Element comment = e.getChild("comment");
			String approved = (target.getAttributeValue("type", "").equals("") && !targetText.trim().equals("")) ? "yes" //$NON-NLS-5$
					: "no"; //$NON-NLS-1$
			writeString("<trans-unit id=\"" + segId++ + "\" approved=\"" + approved + "\" xml:space=\"preserve\">\n");
			writeString("<source xml:lang=\"" + sourceLanguage + "\">" + getText(source) + "</source>\n");
			writeString("<target>" + targetText + "</target>\n");
			if (comment != null) {
				writeString("<note>" + getText(comment) + "</note>\n");
			}
			writeString("</trans-unit>\n");
		} else {
			List<Element> children = e.getChildren();
			Iterator<Element> it = children.iterator();
			while (it.hasNext()) {
				recurse(it.next());
			}
		}
	}

	private static String getTarget(Element target) {
		List<Element> numerusforms = target.getChildren("numerusform");
		if (numerusforms.size() > 0) {
			return getText(numerusforms.get(0));
		}
		return getText(target);
	}

	private static String getText(Element e) {
		String result = "";
		int id = 0;
		List<XMLNode> nodes = e.getContent();
		Iterator<XMLNode> it = nodes.iterator();
		while (it.hasNext()) {
			XMLNode n = it.next();
			if (n.getNodeType() == XMLNode.TEXT_NODE) {
				result = result + n.toString();
			}
			if (n.getNodeType() == XMLNode.ELEMENT_NODE) {
				Element by = (Element) n;
				result = result + "<ph id=\"" + id++ + "\">" + Utils.cleanString(by.toString()) + "</ph>";
			}
		}
		return result;
	}

	private static void writeString(String string) throws IOException {
		output.write(string.getBytes(StandardCharsets.UTF_8));
	}

}
