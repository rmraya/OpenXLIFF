/*******************************************************************************
 * Copyright (c) 2018 - 2026 Maxprograms.
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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.converters.Constants;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.CatalogBuilder;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.XMLNode;
import com.maxprograms.xml.XMLOutputter;
import com.maxprograms.xml.XMLUtils;

public class Sdl2Xliff {

	private static FileOutputStream out;

	private Sdl2Xliff() {
		// do not instantiate this class
		// use run method instead
	}

	public static List<String> run(Map<String, String> params) {
		List<String> result = new ArrayList<>();

		try {
			String original = params.get("source");
			String output = params.get("xliff");
			String skeletonFile = params.get("skeleton");
			String sourceLanguage = params.get("srcLang");
			String targetLanguage = params.get("tgtLang");
			String srxRules = params.get("srxFile");
			String catalogFile = params.get("catalog");
			String tgtLang = "";
			if (targetLanguage != null) {
				tgtLang = "\" target-language=\"" + targetLanguage;
			}

			Catalog catalog = CatalogBuilder.getCatalog(catalogFile);
			XliffModel model = new XliffModel(original, srxRules, catalog);
			if (model.wasModified()) {
				File f = File.createTempFile("temp", ".sdlxliff");
				f.deleteOnExit();
				model.save(f.getAbsolutePath());
				original = f.getAbsolutePath();
			}
			model = null;

			SAXBuilder builder = new SAXBuilder();
			builder.setEntityResolver(catalog);
			Document doc = builder.build(original);

			out = new FileOutputStream(output);
			writeStr("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
			writeStr("<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" "
					+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
					+ "xsi:schemaLocation=\"urn:oasis:names:tc:xliff:document:1.2 xliff-core-1.2-transitional.xsd\">\n");
			writeStr("<file datatype=\"x-sdlxliff\" original=\"" + XMLUtils.cleanText(params.get("source"))
					+ "\" tool-id=\"" + Constants.TOOLID + "\" source-language=\"" + sourceLanguage + tgtLang + "\" "
					+ "xmlns:sdl=\"http://sdl.com/FileTypes/SdlXliff/1.0\" " + ">\n");
			writeStr("<?encoding " + doc.getEncoding() + "?>\n");

			writeStr("<header>\n");
			writeStr("  <skl>\n");
			writeStr("    <external-file href=\"" + XMLUtils.cleanText(skeletonFile) + "\"/>\n");
			writeStr("  </skl>\n");
			writeStr("  <tool tool-version=\"" + Constants.VERSION + " " + Constants.BUILD + "\" tool-id=\""
					+ Constants.TOOLID + "\" tool-name=\"" + Constants.TOOLNAME + "\"/>\n");
			writeStr("</header>\n");
			writeStr("<body>\n");

			acceptTrackedChanges(doc.getRootElement());
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
			logger.log(Level.ERROR, Messages.getString("Sdl2Xliff.1"), e);
			result.add(Constants.ERROR);
			result.add(e.getMessage());
		}
		return result;
	}

	private static void recurse(Element root) throws IOException {
		if (root.getName().equals("trans-unit")) {
			if (root.getAttributeValue("translate", "yes").equals("no")
					&& !(root.getAttributeValue("sdl:locktype").equals("Manual")
							&& containsSrcText(root.getChild("source")))) {
				return;
			}
			Element segSource = root.getChild("seg-source");
			Element target = root.getChild("target");

			Map<String, Boolean> lockedMap = new HashMap<>();
			Map<String, String> statusMap = new HashMap<>();

			Element segDefs = root.getChild("sdl:seg-defs");
			if (segDefs != null) {
				List<Element> children = segDefs.getChildren("sdl:seg");
				Iterator<Element> it = children.iterator();
				while (it.hasNext()) {
					Element sdlSeg = it.next();
					String id = sdlSeg.getAttributeValue("id");
					if ("true".equals(sdlSeg.getAttributeValue("locked", "false"))) {
						lockedMap.put(id, true);
					}
					if ("Draft".equals(sdlSeg.getAttributeValue("conf"))) {
						statusMap.put(id, "new");
					}
					if ("Translated".equals(sdlSeg.getAttributeValue("conf"))) {
						statusMap.put(id, "translated");
					}
					if ("RejectedTranslation".equals(sdlSeg.getAttributeValue("conf"))) {
						statusMap.put(id, "needs-review-translation");
					}
					if ("ApprovedSignOff".equals(sdlSeg.getAttributeValue("conf"))) {
						statusMap.put(id, "signed-off");
					}
				}
			}
			if (segSource != null) {
				if (containsText(segSource)) {
					Map<String, Element> targets = new HashMap<>();
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
							String id = mrk.getAttributeValue("mid");
							String lockedString = "";
							if (lockedMap.containsKey(id)) {
								lockedString = "\" ts=\"locked";
							}
							writeStr("  <trans-unit id=\"" + root.getAttributeValue("id") + ':'
									+ mrk.getAttributeValue("mid") + lockedString + "\" xml:space=\"preserve\">\n");
							// write new source
							writeStr("    <source>");
							recurseSource(mrk);
							writeStr("</source>\n");
							if (targets.containsKey(mrk.getAttributeValue("mid"))) {
								// write new target
								String stateString = "";
								if (statusMap.containsKey(id)) {
									stateString = " state=\"" + statusMap.get(id) + "\"";
								}
								Element tmrk = targets.get(mrk.getAttributeValue("mid"));
								writeStr("    <target " + stateString + ">");
								recurseTarget(tmrk);
								writeStr("</target>\n");
							}
							writeStr("  </trans-unit>\n");
						}
					}
				}
			} else {
				// <seg-source> is null
				if (root.getAttributeValue("translate", "yes").equals("no")) {
					writeStr(root.toString());
				} else {
					throw new IOException(Messages.getString("Sdl2Xliff.2"));
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
		List<Element> result = new ArrayList<>();
		List<Element> children = e.getChildren();
		Iterator<Element> it = children.iterator();
		while (it.hasNext()) {
			Element child = it.next();
			if (child.getName().equals("mrk") && child.getAttributeValue("mtype").equals("seg")) {
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

	private static void acceptTrackedChanges(Element e) {
		List<Element> children = e.getChildren("mrk");
		if (!children.isEmpty()) {
			List<XMLNode> content = e.getContent();
			List<XMLNode> newContent = new ArrayList<>();
			Iterator<XMLNode> it = content.iterator();
			while (it.hasNext()) {
				XMLNode n = it.next();
				if (n.getNodeType() == XMLNode.ELEMENT_NODE) {
					Element child = (Element) n;
					if (child.getName().equals("mrk")) {
						if (child.getAttributeValue("mtype").equals("x-sdl-added")) {
							acceptTrackedChanges(child);
							newContent.addAll(child.getContent());
						} else if (child.getAttributeValue("mtype").equals("x-sdl-deleted")) {
							// do nothing
						} else {
							newContent.add(child);
						}
					} else {
						newContent.add(child);
					}
				} else {
					newContent.add(n);
				}
			}
			e.setContent(newContent);
		}
		children = e.getChildren();
		Iterator<Element> it = children.iterator();
		while (it.hasNext()) {
			acceptTrackedChanges(it.next());
		}
	}
}
