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
package com.maxprograms.converters.sdlxliff;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.lang.System.Logger.Level;
import java.net.URISyntaxException;
import java.lang.System.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.Utils;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.XMLNode;
import com.maxprograms.xml.XMLOutputter;

public class Sdl2Xliff {

	private static FileOutputStream out;

	private Sdl2Xliff() {
		// do not instantiate this class
		// use run method instead
	}

	public static Vector<String> run(Hashtable<String, String> params) {
		Vector<String> result = new Vector<>();

		try {
			String original = params.get("source");
			String output = params.get("xliff");
			String skeletonFile = params.get("skeleton");
			String sourceLanguage = params.get("srcLang");
			String targetLanguage = params.get("tgtLang");
			String srxRules = params.get("srxFile");
			String catalog = params.get("catalog");
			String tgtLang = "";
			if (targetLanguage != null) {
				tgtLang = "\" target-language=\"" + targetLanguage;
			}

			XliffModel model = new XliffModel(original, srxRules, catalog);
			if (model.wasModified()) {
				File f = File.createTempFile("temp", ".sdlxliff");
				f.deleteOnExit();
				model.save(f.getAbsolutePath());
				original = f.getAbsolutePath();
			}
			model = null;

			SAXBuilder builder = new SAXBuilder();
			builder.setEntityResolver(new Catalog(catalog));
			Document doc = builder.build(original);

			out = new FileOutputStream(output);
			writeStr("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
			writeStr("<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" "
					+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
					+ "xsi:schemaLocation=\"urn:oasis:names:tc:xliff:document:1.2 xliff-core-1.2-transitional.xsd\">\n");
			writeStr("<file datatype=\"x-sdlxliff\" original=\"" + Utils.cleanString(params.get("source"))
					+ "\" tool-id=\"" + Constants.TOOLID + "\" source-language=\"" + sourceLanguage + tgtLang + "\" "
					+ "xmlns:sdl=\"http://sdl.com/FileTypes/SdlXliff/1.0\" " + ">\n");
			writeStr("<?encoding " + doc.getEncoding() + "?>\n");

			writeStr("<header>\n");
			writeStr("<skl>\n");
			writeStr("<external-file href=\"" + Utils.cleanString(skeletonFile) + "\"/>\n");
			writeStr("</skl>\n");
			writeStr("   <tool tool-version=\"" + Constants.VERSION + " " + Constants.BUILD + "\" tool-id=\""
					+ Constants.TOOLID + "\" tool-name=\"" + Constants.TOOLNAME + "\"/>\n");
			writeStr("</header>\n");
			writeStr("<body>\n");

			recurse(doc.getRootElement());

			writeStr("</body>\n");
			writeStr("</file>\n");
			writeStr("</xliff>\n");
			out.close();

			XMLOutputter outputter = new XMLOutputter();
			outputter.preserveSpace(true);
			outputter.setSkipLinefeed(true);
			outputter.writeBOM(true);
			try (FileOutputStream skl = new FileOutputStream(skeletonFile)) {
				outputter.output(doc, skl);
			}
			result.add(Constants.SUCCESS);
		} catch (IOException | SAXException | ParserConfigurationException | URISyntaxException e) {
			Logger logger = System.getLogger(Sdl2Xliff.class.getName());
			logger.log(Level.ERROR, "Error converting SDLXLIFF file", e);
			result.add(Constants.ERROR);
			result.add(e.getMessage());
		}
		return result;
	}

	private static void recurse(Element root) throws IOException {
		if (root.getName().equals("trans-unit")) {
			if (root.getAttributeValue("translate", "yes").equals("no")
					&& !(root.getAttributeValue("sdl:locktype", "").equals("Manual")
							&& containsSrcText(root.getChild("source")))) {
				return;
			}
			Element segSource = root.getChild("seg-source");
			Element target = root.getChild("target");
			if (segSource != null) {
				if (containsText(segSource)) {
					Hashtable<String, Element> targets = new Hashtable<>();
					if (target != null) {
						List<Element> tmarks = getSegments(target);
						Iterator<Element> tt = tmarks.iterator();
						while (tt.hasNext()) {
							Element mrk = tt.next();
							targets.put(mrk.getAttributeValue("mid"), mrk);
						}
					}
					List<Element> mrks = getSegments(segSource);
					if (!mrks.isEmpty()) {
						Iterator<Element> it = mrks.iterator();
						while (it.hasNext()) {
							Element mrk = it.next();
							writeStr("<trans-unit id=\"" + root.getAttributeValue("id") + '|'
									+ mrk.getAttributeValue("mid") + "\" xml:space=\"preserve\">\n");
							// write new source
							writeStr("<source>");
							recurseSource(mrk);
							writeStr("</source>\n");
							if (targets.containsKey(mrk.getAttributeValue("mid"))) {
								// write new target
								Element tmrk = targets.get(mrk.getAttributeValue("mid"));
								writeStr("<target>");
								recurseTarget(tmrk);
								writeStr("</target>\n");
							}
							writeStr("</trans-unit>\n");
						}
					}
				}
			} else {
				// <seg-source> is null
				if (root.getAttributeValue("translate", "yes").equals("no")) {
					writeStr(root.toString());
				} else {
					throw new IOException("Segmentation problem found.");
				}
			}
		} else {
			// not in <trans-unit>
			List<Element> children = root.getChildren();
			Iterator<Element> it = children.iterator();
			while (it.hasNext()) {
				recurse(it.next());
			}
		}
	}

	private static boolean containsSrcText(Element child) {
		List<XMLNode> list = child.getContent();
		Iterator<XMLNode> it = list.iterator();
		while (it.hasNext()) {
			XMLNode n = it.next();
			if (n.getNodeType() == XMLNode.TEXT_NODE) {
				if (!n.toString().trim().isEmpty()) {
					return true;
				}
			} else if (n.getNodeType() == XMLNode.ELEMENT_NODE) {
				Element e = (Element) n;
				if (e.getName().equals("g") && containsSrcText(e)) {
					return true;
				}
			}
		}
		return false;
	}

	private static void recurseTarget(Element mrk) throws IOException {
		List<XMLNode> tnodes = mrk.getContent();
		Iterator<XMLNode> tnt = tnodes.iterator();
		while (tnt.hasNext()) {
			XMLNode n = tnt.next();
			if (n.getNodeType() == XMLNode.TEXT_NODE) {
				writeStr(n.toString());
			}
			if (n.getNodeType() == XMLNode.ELEMENT_NODE) {
				Element e = (Element) n;
				if (e.getName().equals("mrk")) {
					recurseTarget(e);
				} else {
					writeStr(e.toString());
				}
			}
		}

	}

	private static void recurseSource(Element mrk) throws IOException {
		List<XMLNode> nodes = mrk.getContent();
		Iterator<XMLNode> nt = nodes.iterator();
		while (nt.hasNext()) {
			XMLNode n = nt.next();
			if (n.getNodeType() == XMLNode.TEXT_NODE) {
				writeStr(n.toString());
			}
			if (n.getNodeType() == XMLNode.ELEMENT_NODE) {
				Element e = (Element) n;
				if (e.getName().equals("mrk")) {
					recurseSource(e);
				} else {
					writeStr(e.toString());
				}
			}
		}

	}

	private static List<Element> getSegments(Element e) {
		List<Element> result = new Vector<>();
		List<Element> children = e.getChildren();
		Iterator<Element> it = children.iterator();
		while (it.hasNext()) {
			Element child = it.next();
			if (child.getName().equals("mrk") && child.getAttributeValue("mtype", "").equals("seg")) {
				result.add(child);
			} else {
				result.addAll(getSegments(child));
			}
		}
		return result;
	}

	private static boolean containsText(Element source) {
		List<XMLNode> nodes = source.getContent();
		Iterator<XMLNode> it = nodes.iterator();
		while (it.hasNext()) {
			XMLNode n = it.next();
			if (n.getNodeType() == XMLNode.TEXT_NODE && !n.toString().trim().isEmpty()) {
				return true;
			}
			if (n.getNodeType() == XMLNode.ELEMENT_NODE) {
				Element e = (Element) n;
				if (e.getName().equals("mrk") && containsText(e)) {
					return true;
				}
				if (e.getName().equals("g") && containsText(e)) {
					return true;
				}
			}
		}
		return false;
	}

	private static void writeStr(String string) throws IOException {
		out.write(string.getBytes(StandardCharsets.UTF_8));
	}
}
