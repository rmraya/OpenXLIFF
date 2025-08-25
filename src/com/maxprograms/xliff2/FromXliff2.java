/*******************************************************************************
 * Copyright (c) 2018 - 2025 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/
package com.maxprograms.xliff2;

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
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.converters.Constants;
import com.maxprograms.xml.Attribute;
import com.maxprograms.xml.CatalogBuilder;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.Indenter;
import com.maxprograms.xml.PI;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.XMLNode;
import com.maxprograms.xml.XMLOutputter;
import com.maxprograms.xml.XMLUtils;

public class FromXliff2 {

	private static String srcLang;
	private static String trgLang;
	private static int fileCounter;

	private FromXliff2() {
		// do not instantiate this class
		// use run or main methods instead
	}

	public static List<String> run(String sourceFile, String outputFile, String catalog) {
		List<String> result = new ArrayList<>();
		fileCounter = 0;
		try {
			SAXBuilder builder = new SAXBuilder();
			builder.setEntityResolver(CatalogBuilder.getCatalog(catalog));
			Document doc = builder.build(sourceFile);
			Element root = doc.getRootElement();
			if (!root.getAttributeValue("version").startsWith("2.")) {
				result.add(Constants.ERROR);
				result.add(Messages.getString("FromXliff2.1"));
				return result;
			}
			Document xliff12 = new Document(null, "xliff", null, null);
			Element root12 = xliff12.getRootElement();
			recurse(root, root12);
			Indenter.indent(root12, 2);
			XMLOutputter outputter = new XMLOutputter();
			outputter.preserveSpace(true);
			try (FileOutputStream out = new FileOutputStream(new File(outputFile))) {
				out.write(XMLUtils.UTF8BOM);
				outputter.output(xliff12, out);
			}
			result.add(Constants.SUCCESS);
		} catch (SAXException | IOException | ParserConfigurationException | URISyntaxException ex) {
			Logger logger = System.getLogger(FromXliff2.class.getName());
			logger.log(Level.ERROR, Messages.getString("FromXliff2.2"), ex);
			result.add(Constants.ERROR);
			result.add(ex.getMessage());
		}
		return result;
	}

	private static void recurse(Element source, Element target) {
		if (source.getName().equals("xliff")) {
			target.setAttribute("version", "1.2");
			target.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			target.setAttribute("xsi:schemaLocation",
					"urn:oasis:names:tc:xliff:document:1.2 xliff-core-1.2-transitional.xsd");
			target.setAttribute("xmlns", "urn:oasis:names:tc:xliff:document:1.2");
			srcLang = source.getAttributeValue("srcLang");
			trgLang = source.getAttributeValue("trgLang");

			List<PI> piList = source.getPI();
			for (int i = 0; i < piList.size(); i++) {
				PI pi = piList.get(i);
				if ("encoding".equals(pi.getTarget())) {
					String encoding = pi.getData();
					if (!encoding.equalsIgnoreCase(StandardCharsets.UTF_8.name())) {
						target.addContent(new PI("encoding", encoding));
					}
					continue;
				}
				target.addContent(pi);
			}
		}

		if (source.getName().equals("file")) {
			Element file = new Element("file");
			file.addContent(new PI("counter", String.valueOf(fileCounter++)));
			file.setAttribute("original", source.getAttributeValue("original"));
			file.setAttribute("source-language", srcLang);
			if (!trgLang.isEmpty()) {
				file.setAttribute("target-language", trgLang);
			}
			List<Attribute> atts = source.getAttributes();
			Iterator<Attribute> at = atts.iterator();
			while (at.hasNext()) {
				Attribute a = at.next();
				if (a.getName().startsWith("xmlns:")) {
					file.setAttribute(a);
				}
			}
			Element header = new Element("header");
			file.addContent(header);
			Element body = new Element("body");
			file.addContent(body);
			Element skeleton = source.getChild("skeleton");
			if (skeleton != null) {
				Element skl = new Element("skl");
				String href = skeleton.getAttributeValue("href");
				if (!href.isEmpty()) {
					Element external = new Element("external-file");
					external.setAttribute("href", href);
					skl.addContent(external);
				} else {
					Element internal = new Element("internal-file");
					internal.setContent(skeleton.getContent());
					skl.addContent(internal);
				}
				header.addContent(skl);
			}
			Element metadata = source.getChild("mda:metadata");
			if (metadata != null) {
				List<Element> metadataList = metadata.getChildren("mda:metaGroup");
				for (int i = 0; i < metadataList.size(); i++) {
					Element metaGroup = metadataList.get(i);
					String category = metaGroup.getAttributeValue("category");
					if (category.equals("tool")) {
						Element tool = new Element("tool");
						header.addContent(tool);
						List<Element> metaList = metaGroup.getChildren("mda:meta");
						Iterator<Element> it = metaList.iterator();
						while (it.hasNext()) {
							Element meta = it.next();
							tool.setAttribute(meta.getAttributeValue("type"), meta.getText());
						}
						file.setAttribute("tool-id", tool.getAttributeValue("tool-id"));
					} else if (category.equals("PI")) {
						List<Element> metaList = metaGroup.getChildren("mda:meta");
						Iterator<Element> it = metaList.iterator();
						while (it.hasNext()) {
							Element meta = it.next();
							PI pi = new PI(meta.getAttributeValue("type"), meta.getText());
							file.addContent(pi);
						}
					} else if (category.equals("project-data")) {
						List<Element> metaList = metaGroup.getChildren("mda:meta");
						Iterator<Element> it = metaList.iterator();
						while (it.hasNext()) {
							Element meta = it.next();
							String type = meta.getAttributeValue("type");
							if (type.equals("product-name")) {
								file.setAttribute("product-name", meta.getText());
							}
							if (type.equals("project-id")) {
								file.setAttribute("product-version", meta.getText());
							}
							if (type.equals("build-number")) {
								file.setAttribute("build-num", meta.getText());
							}
						}
					} else if (category.equals("format")) {
						Element meta = metaGroup.getChild("mda:meta");
						file.setAttribute("datatype", meta.getText());
					} else {
						// custom metadata
						PI pi = new PI("customMetadata", metaGroup.toString());
						file.addContent(pi);
					}
				}
			}

			target.addContent(file);
			target = body;
		}

		if (source.getName().equals("group")) {
			Element group = new Element("group");
			group.setAttribute("id", source.getAttributeValue("id"));
			Element metadata = source.getChild("mda:metadata");
			if (metadata != null) {
				Element metaGroup = metadata.getChild("mda:metaGroup");
				if (metaGroup != null) {
					List<Element> metaList = metaGroup.getChildren("mda:meta");
					Iterator<Element> it = metaList.iterator();
					while (it.hasNext()) {
						Element meta = it.next();
						if (meta.getAttributeValue("type").equals("ts")) {
							group.setAttribute("ts", meta.getText());
						}
						if (meta.getAttributeValue("type").equals("space") && meta.getText().equals("keep")) {
							group.setAttribute("xml:space", "preserve");
						}
					}
				}
			}
			target.addContent(group);
			target = group;
		}

		if (source.getName().equals("unit")) {
			Element transUnit = new Element("trans-unit");
			transUnit.setAttribute("id", source.getAttributeValue("id"));
			if ("no".equals(source.getAttributeValue("translate"))) {
				transUnit.setAttribute("translate", "no");
			}
			List<Attribute> atts = source.getAttributes();
			Iterator<Attribute> at = atts.iterator();
			while (at.hasNext()) {
				Attribute a = at.next();
				if (a.getName().indexOf(':') != -1 && !a.getName().startsWith("xml:")) {
					transUnit.setAttribute(a);
				}
			}
			target.addContent(transUnit);

			Map<String, String> tags = new HashMap<>();
			Element originalData = source.getChild("originalData");
			if (originalData != null) {
				List<Element> dataList = originalData.getChildren("data");
				Iterator<Element> it = dataList.iterator();
				while (it.hasNext()) {
					Element data = it.next();
					tags.put(data.getAttributeValue("id"), data.getText());
				}
			}

			Map<String, List<String[]>> attributes = new HashMap<>();
			Element metadata = source.getChild("mda:metadata");
			if (metadata != null) {
				List<Element> groups = metadata.getChildren("mda:metaGroup");
				Iterator<Element> it = groups.iterator();
				while (it.hasNext()) {
					Element group = it.next();
					if ("attributes".equals(group.getAttributeValue("category"))) {
						String id = group.getAttributeValue("id");
						List<String[]> list = new ArrayList<>();
						List<Element> metas = group.getChildren("mda:meta");
						for (int i = 0; i < metas.size(); i++) {
							Element meta = metas.get(i);
							list.add(new String[] { meta.getAttributeValue("type"), meta.getText() });
						}
						attributes.put(id, list);
					} else if ("transUnitAttributes".equals(group.getAttributeValue("category"))) {
						List<Element> metas = group.getChildren("mda:meta");
						for (int i = 0; i < metas.size(); i++) {
							Element meta = metas.get(i);
							transUnit.setAttribute(meta.getAttributeValue("type"), meta.getText());
						}
					} else {
						PI pi = new PI("customMetadata", group.toString());
						transUnit.addContent(pi);
					}
				}
			}

			Element joinedSource = new Element("source");
			Element joinedTarget = new Element("target");
			boolean approved = false;
			boolean preserve = false;
			boolean hasTarget = false;

			List<Element> children = source.getChildren();
			Iterator<Element> et = children.iterator();
			while (et.hasNext()) {
				Element child = et.next();
				if (child.getName().equals("segment") || child.getName().equals("ignorable")) {
					Element src = child.getChild("source");
					if (src.getAttributeValue("xml:space", "default").equals("preserve")) {
						preserve = true;
					}
					joinedSource.addContent(src.getContent());
					Element tgt = child.getChild("target");
					if (tgt != null) {
						hasTarget = true;
						joinedTarget.addContent(tgt.getContent());
					}
					if (tgt == null && child.getName().equals("ignorable")) {
						joinedTarget.addContent(src.getContent());
					}
					if (child.getName().equals("segment") && "final".equals(child.getAttributeValue("state"))) {
						approved = true;
					}
				}
			}
			if (approved) {
				transUnit.setAttribute("approved", "yes");
			}
			if (preserve) {
				transUnit.setAttribute("xml:space", "preserve");
			}

			joinedSource = removeComments(joinedSource);
			joinedTarget = removeComments(joinedTarget);

			Element src = new Element("source");
			src.setContent(harvestContent(joinedSource, tags, attributes));
			if (preserve) {
				transUnit.addContent("\n        ");
			}
			transUnit.addContent(src);

			Element tgt = new Element("target");
			if (!joinedTarget.getContent().isEmpty()) {
				tgt.setContent(harvestContent(joinedTarget, tags, attributes));
			}
			if (hasTarget) {
				if (preserve) {
					transUnit.addContent("\n        ");
				}
				transUnit.addContent(tgt);
			}

			Element notes = source.getChild("notes");
			if (notes != null) {
				List<Element> notesList = notes.getChildren("note");
				for (int i = 0; i < notesList.size(); i++) {
					Element note = notesList.get(i);
					Element n = new Element("note");
					String appliesTo = note.getAttributeValue("appliesTo");
					if (!appliesTo.isEmpty()) {
						n.setAttribute("annotates", appliesTo);
					}
					n.addContent(note.getText());
					if (preserve) {
						transUnit.addContent("\n        ");
					}
					transUnit.addContent(n);
				}
			}

			Element matches = source.getChild("mtc:matches");
			if (matches != null) {
				List<Element> matchesList = matches.getChildren("mtc:match");
				for (int i = 0; i < matchesList.size(); i++) {
					Element match = matchesList.get(i);
					Element altTrans = new Element("alt-trans");
					String quality = match.getAttributeValue("matchQuality");
					if (!quality.isEmpty()) {
						try {
							float f = Float.parseFloat(quality);
							int round = Math.round(f);
							altTrans.setAttribute("match-quality", "" + round);
						} catch (NumberFormatException ne) {
							// do nothing
						}
					}
					altTrans.setAttribute("origin", match.getAttributeValue("origin", "unknown"));
					Element matchData = match.getChild("originalData");
					if (matchData != null) {
						List<Element> dataList = matchData.getChildren("data");
						Iterator<Element> it = dataList.iterator();
						while (it.hasNext()) {
							Element data = it.next();
							if (!tags.containsKey(data.getAttributeValue("id"))) {
								tags.put(data.getAttributeValue("id"), data.getText());
							}
						}
					}
					Element matchSrc = match.getChild("source");
					Element s = new Element("source");
					s.setContent(harvestContent(matchSrc, tags, attributes));
					altTrans.addContent(s);
					Element matchTgt = match.getChild("target");
					Element t = new Element("target");
					t.setContent(harvestContent(matchTgt, tags, attributes));
					altTrans.addContent(t);
					if (preserve) {
						transUnit.addContent("\n        ");
					}
					transUnit.addContent(altTrans);
				}
			}
			if (preserve) {
				transUnit.addContent("\n      ");
			}
			Indenter.indent(transUnit, 2);
		}

		List<Element> children = source.getChildren();
		Iterator<Element> it = children.iterator();
		while (it.hasNext()) {
			recurse(it.next(), target);
		}
	}

	public static Element removeComments(Element element) {
		if (!containsComments(element)) {
			return element;
		}
		List<XMLNode> newContent = new Vector<>();
		List<XMLNode> content = element.getContent();
		Iterator<XMLNode> it = content.iterator();
		while (it.hasNext()) {
			XMLNode node = it.next();
			if (node.getNodeType() == XMLNode.TEXT_NODE) {
				newContent.add(node);
			}
			if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
				Element child = (Element) node;
				if (containsComments(child)) {
					List<XMLNode> nodes = child.getContent();
					Iterator<XMLNode> nt = nodes.iterator();
					while (nt.hasNext()) {
						XMLNode n = nt.next();
						if (n.getNodeType() == XMLNode.TEXT_NODE) {
							newContent.add(n);
						}
						if (n.getNodeType() == XMLNode.ELEMENT_NODE) {
							Element clean = removeComments((Element) n);
							newContent.addAll(clean.getContent());
						}
					}
				} else {
					newContent.add(node);
				}
			}
		}
		element.setContent(newContent);
		return element;
	}

	public static boolean containsComments(Element element) {
		if (isComment(element)) {
			return true;
		}
		List<Element> children = element.getChildren();
		Iterator<Element> it = children.iterator();
		while (it.hasNext()) {
			Element child = it.next();
			if (containsComments(child)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isComment(Element element) {
		return ("mrk".equals(element.getName()) && "comment".equals(element.getAttributeValue("type")));
	}

	private static List<XMLNode> harvestContent(Element joinedSource, Map<String, String> tags,
			Map<String, List<String[]>> attributes) {
		List<XMLNode> result = new ArrayList<>();
		List<XMLNode> nodes = joinedSource.getContent();
		Iterator<XMLNode> it = nodes.iterator();
		while (it.hasNext()) {
			XMLNode node = it.next();
			if (node.getNodeType() == XMLNode.TEXT_NODE) {
				result.add(node);
			}
			if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
				result.add(processIniline((Element) node, tags, attributes));
			}
		}
		return result;
	}

	private static Element processIniline(Element tag, Map<String, String> tags,
			Map<String, List<String[]>> attributes) {
		Element result = null;
		if ("ph".equals(tag.getName())) {
			String id = tag.getAttributeValue("id");
			if (id.startsWith("ph")) {
				Element ph = new Element("ph");
				ph.setAttribute("id", id.substring("ph".length()));
				ph.addContent(tags.get(id));
				if (attributes != null && attributes.containsKey(id)) {
					List<String[]> list = attributes.get(id);
					for (int i = 0; i < list.size(); i++) {
						String[] pair = list.get(i);
						ph.setAttribute(pair[0], pair[1]);
					}
				}
				result = ph;
			}
			if (id.startsWith("x")) {
				Element x = new Element("x");
				x.setAttribute("id", id.substring("x".length()));
				if (attributes != null && attributes.containsKey(id)) {
					List<String[]> list = attributes.get(id);
					for (int i = 0; i < list.size(); i++) {
						String[] pair = list.get(i);
						x.setAttribute(pair[0], pair[1]);
					}
				}
				result = x;
			}
			if (id.startsWith("bpt")) {
				Element bpt = new Element("bpt");
				bpt.setAttribute("id", id.substring("bpt".length()));
				bpt.addContent(tags.get(id));
				if (attributes != null && attributes.containsKey(id)) {
					List<String[]> list = attributes.get(id);
					for (int i = 0; i < list.size(); i++) {
						String[] pair = list.get(i);
						bpt.setAttribute(pair[0], pair[1]);
					}
				}
				result = bpt;
			}
			if (id.startsWith("ept")) {
				Element ept = new Element("ept");
				ept.setAttribute("id", id.substring("ept".length()));
				ept.addContent(tags.get(id));
				if (attributes != null && attributes.containsKey(id)) {
					List<String[]> list = attributes.get(id);
					for (int i = 0; i < list.size(); i++) {
						String[] pair = list.get(i);
						ept.setAttribute(pair[0], pair[1]);
					}
				}
				result = ept;
			}
			if (id.startsWith("it")) {
				Element t = new Element("it");
				t.setAttribute("id", id.substring("it".length()));
				t.addContent(tags.get(id));
				if (attributes != null && attributes.containsKey(id)) {
					List<String[]> list = attributes.get(id);
					for (int i = 0; i < list.size(); i++) {
						String[] pair = list.get(i);
						t.setAttribute(pair[0], pair[1]);
					}
				}
				result = t;
			}
			if (id.startsWith("bx")) {
				Element bx = new Element("bx");
				bx.setAttribute("id", id.substring("bx".length()));
				bx.addContent(tags.get(id));
				if (attributes != null && attributes.containsKey(id)) {
					List<String[]> list = attributes.get(id);
					for (int i = 0; i < list.size(); i++) {
						String[] pair = list.get(i);
						bx.setAttribute(pair[0], pair[1]);
					}
				}
				result = bx;
			}
			if (id.startsWith("ex")) {
				Element ex = new Element("ex");
				ex.setAttribute("id", id.substring("ex".length()));
				ex.addContent(tags.get(id));
				if (attributes != null && attributes.containsKey(id)) {
					List<String[]> list = attributes.get(id);
					for (int i = 0; i < list.size(); i++) {
						String[] pair = list.get(i);
						ex.setAttribute(pair[0], pair[1]);
					}
				}
				result = ex;
			}
			if (result == null) {
				// may come from mtc:matches
				result = new Element("ph");
				result.setAttribute("id", id);
				String dataRef = tag.getAttributeValue("dataRef");
				if (tags.containsKey(dataRef)) {
					result.addContent(tags.get(dataRef));
				}
			}
		}
		if ("pc".equals(tag.getName())) {
			String id = tag.getAttributeValue("id");
			Element g = new Element("g");
			g.setAttribute("id", id.substring("g".length()));
			List<XMLNode> newContent = new ArrayList<>();
			List<XMLNode> content = tag.getContent();
			for (int i = 0; i < content.size(); i++) {
				XMLNode n = content.get(i);
				if (n.getNodeType() == XMLNode.TEXT_NODE) {
					newContent.add(n);
				}
				if (n.getNodeType() == XMLNode.ELEMENT_NODE) {
					Element e = processIniline((Element) n, tags, attributes);
					newContent.add(e);
				}
			}
			g.setContent(newContent);
			result = g;
		}
		if ("mrk".equals(tag.getName())) {
			Element mrk = new Element("mrk");
			String id = tag.getAttributeValue("id");
			if (id.startsWith("mrk")) {
				mrk.setAttribute("mid", id.substring("mrk".length()));
			}
			String value = tag.getAttributeValue("value");
			if (!value.isEmpty()) {
				mrk.setAttribute("ts", value);
			}
			if (tag.getAttributeValue("translate", "yes").equals("no")) {
				mrk.setAttribute("mtype", "protected");
			} else {
				String mtype = "x-other";
				String type = tag.getAttributeValue("type");
				if (type.startsWith("oxlf:")) {
					mtype = type.substring(5).replace("_", ":");
				}
				mrk.setAttribute("mtype", mtype);
			}
			List<XMLNode> newContent = new ArrayList<>();
			List<XMLNode> content = tag.getContent();
			for (int i = 0; i < content.size(); i++) {
				XMLNode n = content.get(i);
				if (n.getNodeType() == XMLNode.TEXT_NODE) {
					newContent.add(n);
				}
				if (n.getNodeType() == XMLNode.ELEMENT_NODE) {
					Element e = processIniline((Element) n, tags, attributes);
					newContent.add(e);
				}
			}
			mrk.setContent(newContent);
			result = mrk;
		}
		if ("sc".equals(tag.getName())) {
			result = new Element("bpt");
			String id = tag.getAttributeValue("id");
			String dataRef = tag.getAttributeValue("dataRef");
			result.setAttribute("id", id);
			if (tags.containsKey(dataRef)) {
				result.addContent(tags.get(dataRef));
			}
		}
		if ("ec".equals(tag.getName())) {
			result = new Element("ept");
			String id = tag.getAttributeValue("id");
			String dataRef = tag.getAttributeValue("dataRef");
			result.setAttribute("id", id);
			if (tags.containsKey(dataRef)) {
				result.addContent(tags.get(dataRef));
			}
		}
		return result;
	}
}
