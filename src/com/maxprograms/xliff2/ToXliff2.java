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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.Utils;
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

public class ToXliff2 {

	private static Element root2;
	private static int fileId;
	private static int mrkCount;

	private static List<String> preserveAttributes = Arrays.asList("reformat", "datatype", "ts", "phase-name",
			"restype", "resname", "extradata", "help-id", "menu", "menu-option", "menu-name", "coord", "font",
			"css-style", "style", "exstyle", "extype", "maxbytes", "minbytes", "size-unit", "maxheight", "minheight",
			"maxwidth", "minwidth", "charclass");

	private ToXliff2() {
		// do not instantiate this class
		// use run or main methods instead
	}

	public static List<String> run(File xliffFile, String catalog, String version) {
		return run(xliffFile.getAbsolutePath(), xliffFile.getAbsolutePath(), catalog, version);
	}

	public static List<String> run(String sourceFile, String outputFile, String catalog, String version) {
		List<String> result = new ArrayList<>();
		if (!Arrays.asList("2.0", "2.1", "2.2").contains(version)) {
			result.add(Constants.ERROR);
			result.add(Messages.getString("ToXliff2.1"));
			return result;
		}
		fileId = 1;
		try {
			SAXBuilder builder = new SAXBuilder();
			builder.setEntityResolver(CatalogBuilder.getCatalog(catalog));
			Document doc = builder.build(sourceFile);
			Element root = doc.getRootElement();
			if (!root.getAttributeValue("version", "1.2").equals("1.2")) {
				result.add(Constants.ERROR);
				result.add(Messages.getString("ToXliff2.1"));
				return result;
			}
			Document xliff2 = new Document(null, "xliff", null, null);
			root2 = xliff2.getRootElement();
			recurse(root, root2);
			root2.setAttribute("version", version);
			if (version.equals("2.2")) {
				root2.setAttribute("xmlns", "urn:oasis:names:tc:xliff:document:2.2");
			}
			Indenter.indent(root2, 2);
			XMLOutputter outputter = new XMLOutputter();
			outputter.preserveSpace(true);
			try (FileOutputStream out = new FileOutputStream(new File(outputFile))) {
				out.write(XMLUtils.UTF8BOM);
				outputter.output(xliff2, out);
			}
			result.add(Constants.SUCCESS);
		} catch (SAXException | IOException | ParserConfigurationException | URISyntaxException ex) {
			Logger logger = System.getLogger(ToXliff2.class.getName());
			logger.log(Level.ERROR, Messages.getString("ToXliff2.2"));
			result.add(Constants.ERROR);
			result.add(ex.getMessage());
		}
		return result;
	}

	private static void recurse(Element source, Element target)
			throws SAXException, IOException, ParserConfigurationException {
		if (source.getName().equals("xliff")) {
			target.setAttribute("xmlns", "urn:oasis:names:tc:xliff:document:2.0");
			target.setAttribute("xmlns:mda", "urn:oasis:names:tc:xliff:metadata:2.0");

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
			String fileSrcLang = source.getAttributeValue("source-language");
			root2.setAttribute("srcLang", fileSrcLang);
			String fileTgtLang = source.getAttributeValue("target-language");
			if (!fileTgtLang.isEmpty()) {
				root2.setAttribute("trgLang", fileTgtLang);
			}
			Element file = new Element("file");
			file.setAttribute("id", "" + fileId++);
			file.setAttribute("original", source.getAttributeValue("original"));

			List<Attribute> atts = source.getAttributes();
			Iterator<Attribute> at = atts.iterator();
			while (at.hasNext()) {
				Attribute a = at.next();
				if (a.getName().startsWith("xmlns:")) {
					file.setAttribute(a);
				}
			}

			Element fileMetadata = new Element("mda:metadata");
			Element typeGroup = new Element("mda:metaGroup");
			typeGroup.setAttribute("category", "format");
			Element typeMeta = new Element("mda:meta");
			typeMeta.setAttribute("type", "datatype");
			typeMeta.addContent(source.getAttributeValue("datatype"));
			typeGroup.addContent(typeMeta);
			fileMetadata.addContent(typeGroup);

			if (source.hasAttribute("ts")) {
				try {
					JSONObject attributes = new JSONObject(source.getAttributeValue("ts"));
					if (attributes.has("id")) {
						Element idMeta = new Element("mda:meta");
						idMeta.setAttribute("type", "id");
						idMeta.addContent(attributes.getString("id"));
						typeGroup.addContent(idMeta);
					}
				} catch (JSONException ex) {
					// ignore TS that is not in JSON format
				}
			}

			if (!source.getAttributeValue("product-name").isEmpty()
					|| !source.getAttributeValue("product-version").isEmpty()
					|| !source.getAttributeValue("build-num").isEmpty()) {
				Element productGroup = new Element("mda:metaGroup");
				productGroup.setAttribute("category", "project-data");
				Element productName = new Element("mda:meta");
				productName.setAttribute("type", "product-name");
				productName.setText(source.getAttributeValue("product-name"));
				productGroup.addContent(productName);
				Element productVersion = new Element("mda:meta");
				productVersion.setAttribute("type", "project-id");
				productVersion.setText(source.getAttributeValue("product-version"));
				productGroup.addContent(productVersion);
				Element buildNumber = new Element("mda:meta");
				buildNumber.setAttribute("type", "build-number");
				buildNumber.setText(source.getAttributeValue("build-num"));
				productGroup.addContent(buildNumber);
				fileMetadata.addContent(productGroup);
			}

			Element header = source.getChild("header");
			if (header != null) {
				Element skl = header.getChild("skl");
				if (skl != null) {
					Element skeleton = new Element("skeleton");
					Element external = skl.getChild("external-file");
					if (external != null) {
						skeleton.setAttribute("href", external.getAttributeValue("href"));
					} else {
						Element internal = skl.getChild("internal-file");
						skeleton.setContent(internal.getContent());
					}
					file.addContent(skeleton);
				}

				List<Element> propGroups = header.getChildren("prop-group");
				for (int i = 0; i < propGroups.size(); i++) {
					Element sgroup = propGroups.get(i);
					Element tgroup = new Element("mda:metaGroup");
					tgroup.setAttribute("category", sgroup.getAttributeValue("name"));
					List<Element> props = sgroup.getChildren("prop");
					Iterator<Element> it = props.iterator();
					while (it.hasNext()) {
						Element prop = it.next();
						Element meta = new Element("mda:meta");
						meta.setAttribute("type", prop.getAttributeValue("prop-type"));
						meta.addContent(prop.getText());
						tgroup.addContent(meta);
					}
					if (!tgroup.getChildren().isEmpty()) {
						fileMetadata.addContent(tgroup);
					}
				}

				Element tool = header.getChild("tool");
				if (tool != null) {
					Element toolGroup = new Element("mda:metaGroup");
					toolGroup.setAttribute("category", "tool");
					String toolId = tool.getAttributeValue("tool-id");
					if (!toolId.isEmpty()) {
						Element meta = new Element("mda:meta");
						meta.setAttribute("type", "tool-id");
						meta.addContent(toolId);
						toolGroup.addContent(meta);
						if (toolId.equals("Fluenta")) {
							Element fluentaGroup = new Element("mda:metaGroup");
							fluentaGroup.setAttribute("category", "project-data");
							Element name = new Element("mda:meta");
							name.setAttribute("type", "project-name");
							name.addContent(source.getAttributeValue("product-name"));
							fluentaGroup.addContent(name);
							Element project = new Element("mda:meta");
							project.setAttribute("type", "project-id");
							project.addContent(source.getAttributeValue("product-version"));
							fluentaGroup.addContent(project);
							Element build = new Element("mda:meta");
							build.setAttribute("type", "build-number");
							build.addContent(source.getAttributeValue("build-num"));
							fluentaGroup.addContent(build);
							fileMetadata.addContent(fluentaGroup);
						}
					}
					String toolName = tool.getAttributeValue("tool-name");
					if (!toolName.isEmpty()) {
						Element meta = new Element("mda:meta");
						meta.setAttribute("type", "tool-name");
						meta.addContent(toolName);
						toolGroup.addContent(meta);
					}
					String toolCompany = tool.getAttributeValue("tool-company");
					if (!toolCompany.isEmpty()) {
						Element meta = new Element("mda:meta");
						meta.setAttribute("type", "tool-company");
						meta.addContent(toolCompany);
						toolGroup.addContent(meta);
					}
					String toolVersion = tool.getAttributeValue("tool-version");
					if (!toolVersion.isEmpty()) {
						Element meta = new Element("mda:meta");
						meta.setAttribute("type", "tool-version");
						meta.addContent(toolVersion);
						toolGroup.addContent(meta);
					}
					if (!toolGroup.getChildren().isEmpty()) {
						fileMetadata.addContent(toolGroup);
					}
				}
			}

			List<PI> pis = source.getPI();
			if (!pis.isEmpty()) {
				Element piGroup = new Element("mda:metaGroup");
				piGroup.setAttribute("category", "PI");
				Iterator<PI> pit = pis.iterator();
				while (pit.hasNext()) {
					PI pi = pit.next();
					if ("metadata".equals(pi.getTarget())) {
						Element metadata = Utils.toElement(pi.getData().replace("mda:", "mda_"));
						List<Element> metaGroup = metadata.getChildren("mda_metaGroup");
						for (Element childGroup : metaGroup) {
							Element group = new Element("mda:metaGroup");
							group.setAttributes(childGroup.getAttributes());
							List<Element> metas = childGroup.getChildren("mda_meta");
							for (Element metaChild : metas) {
								Element meta = new Element("mda:meta");
								meta.setAttributes(metaChild.getAttributes());
								meta.setText(metaChild.getText());
								group.addContent(meta);
							}
							group.setAttribute("category", "metadata");
							if (!hasGroup(fileMetadata, group)) {
								fileMetadata.addContent(group);
							}
						}
					} else {
						Element meta = new Element("mda:meta");
						meta.setAttribute("type", pi.getTarget());
						meta.addContent(pi.getData());
						piGroup.addContent(meta);
					}
				}
				if (!piGroup.getChildren().isEmpty()) {
					fileMetadata.addContent(piGroup);
				}
			}

			if (!fileMetadata.getChildren().isEmpty()) {
				file.addContent(fileMetadata);
			}

			target.addContent(file);
			target = file;
		}

		if (source.getName().equals("group")) {
			Element group = new Element("group");
			group.setAttribute("id", source.getAttributeValue("id"));
			String ts = source.getAttributeValue("ts");
			if (!ts.isEmpty()) {
				Element metadata = new Element("mda:metadata");
				Element metaGroup = new Element("mda:metaGroup");
				metadata.addContent(metaGroup);
				Element meta = new Element("mda:meta");
				meta.setAttribute("type", "ts");
				meta.addContent(ts);
				metaGroup.addContent(meta);
				group.addContent(metadata);
				if (source.getAttributeValue("xml:space").equals("preserve")) {
					Element space = new Element("mda:meta");
					space.setAttribute("type", "space");
					space.addContent("keep");
					metaGroup.addContent(space);
				}
			}
			target.addContent(group);
			target = group;
		}

		if (source.getName().equals("trans-unit")) {
			Element unit = new Element("unit");
			unit.setAttribute("id", source.getAttributeValue("id"));
			if (source.getAttributeValue("translate", "yes").equals("no")) {
				unit.setAttribute("translate", "no");
			}
			List<Attribute> atts = source.getAttributes();
			Iterator<Attribute> at = atts.iterator();
			List<Attribute> otherAttributes = new ArrayList<>();
			while (at.hasNext()) {
				Attribute a = at.next();
				if (a.getName().indexOf(':') != -1 && !a.getName().startsWith("xml:")) {
					unit.setAttribute(a);
				} else if (preserveAttributes.contains(a.getName())
						&& !("ts".equals(a.getName()) && "locked".equals(a.getValue()))) {
					otherAttributes.add(a);
				}
			}
			if (!otherAttributes.isEmpty()) {
				Element metadata = new Element("mda:metadata");
				unit.addContent(metadata);
				Element metaGroup = new Element("mda:metaGroup");
				metaGroup.setAttribute("category", "transUnitAttributes");
				metadata.addContent(metaGroup);
				Iterator<Attribute> ats = otherAttributes.iterator();
				while (ats.hasNext()) {
					Attribute a = ats.next();
					Element meta = new Element("mda:meta");
					meta.setAttribute("type", a.getName());
					meta.setText(a.getValue());
					metaGroup.addContent(meta);
				}
			}
			List<PI> pis = source.getPI();
			if (!pis.isEmpty()) {
				Iterator<PI> pit = pis.iterator();
				while (pit.hasNext()) {
					PI pi = pit.next();
					if ("metadata".equals(pi.getTarget())) {
						Element unitMetadata = unit.getChild("mda:metadata");
						if (unitMetadata == null) {
							unitMetadata = new Element("mda:metadata");
							unit.addContent(unitMetadata);
						}
						Element metadata = Utils.toElement(pi.getData().replace("mda:", "mda_"));
						List<Element> metaGroup = metadata.getChildren("mda_metaGroup");
						for (Element childGroup : metaGroup) {
							Element group = new Element("mda:metaGroup");
							group.setAttributes(childGroup.getAttributes());
							List<Element> metas = childGroup.getChildren("mda_meta");
							for (Element metaChild : metas) {
								Element meta = new Element("mda:meta");
								meta.setAttributes(metaChild.getAttributes());
								meta.setText(metaChild.getText());
								group.addContent(meta);
							}
							unitMetadata.addContent(group);
						}
					}
				}
			}
			target.addContent(unit);

			List<Element> sourceNotes = new ArrayList<>();
			List<Element> targetNotes = new ArrayList<>();
			List<Element> notesList = source.getChildren("note");
			if (!notesList.isEmpty()) {
				Element notes = new Element("notes");
				unit.addContent(notes);
				for (int i = 0; i < notesList.size(); i++) {
					Element note = notesList.get(i);
					Element n = new Element("note");
					if (note.hasAttribute("priority")) {
						n.setAttribute("priority", note.getAttributeValue("priority"));
					}
					n.setAttribute("id", "n" + (i + 1));
					notes.addContent(n);
					n.addContent(note.getText());
					if ("source".equals(note.getAttributeValue("annotates", "target"))) {
						sourceNotes.add(n);
					} else {
						targetNotes.add(n);
					}
				}
			}

			Element tagAttributes = unit.getChild("mda:metadata");
			if (tagAttributes == null) {
				tagAttributes = new Element("mda:metadata");
				unit.addContent(tagAttributes);
			}
			tagAttributes.setAttribute("id", unit.getAttributeValue("id"));
			Element originalData = new Element("originalData");
			unit.addContent(originalData);

			Element src = source.getChild("source");
			harvestInline(originalData, tagAttributes, src);

			Element segment = new Element("segment");
			segment.setAttribute("id", source.getAttributeValue("id"));
			unit.addContent(segment);
			if (source.getAttributeValue("approved", "no").equals("yes")) {
				segment.setAttribute("state", "final");
			}
			Element src2 = new Element("source");
			if (source.getAttributeValue("xml:space", "default").equals("preserve")) {
				src2.setAttribute("xml:space", "preserve");
			}
			mrkCount = 1;
			src2.setContent(harvestContent(src, tagAttributes));
			if (!sourceNotes.isEmpty()) {
				for (int i = 0; i < sourceNotes.size(); i++) {
					Element note = sourceNotes.get(i);
					Element mrk = new Element("mrk");
					mrk.setAttribute("id", "sn" + i);
					mrk.setAttribute("type", "comment");
					mrk.setAttribute("ref", "#n=" + note.getAttributeValue("id"));
					mrk.setContent(src2.getContent());
					List<XMLNode> content = new ArrayList<>();
					content.add(mrk);
					src2.setContent(content);
				}
			}
			segment.addContent(src2);

			Element tgt = source.getChild("target");
			boolean hasTarget = true;
			if (tgt == null) {
				tgt = new Element("target");
				hasTarget = false;
			}
			harvestInline(originalData, tagAttributes, tgt);
			Element tgt2 = new Element("target");
			if ("preserve".equals(source.getAttributeValue("xml:space"))) {
				tgt2.setAttribute("xml:space", "preserve");
			}
			mrkCount = 1;
			tgt2.setContent(harvestContent(tgt, tagAttributes));
			if (!targetNotes.isEmpty()) {
				for (int i = 0; i < targetNotes.size(); i++) {
					Element note = targetNotes.get(i);
					Element mrk = new Element("mrk");
					mrk.setAttribute("id", "tn" + i);
					mrk.setAttribute("type", "comment");
					mrk.setAttribute("ref", "#n=" + note.getAttributeValue("id"));
					mrk.setContent(tgt2.getContent());
					List<XMLNode> content = new ArrayList<>();
					content.add(mrk);
					tgt2.setContent(content);
				}
			}
			if (!tgt2.getContent().isEmpty()) {
				if (!"final".equals(segment.getAttributeValue("state", "initial"))) {
					segment.setAttribute("state", "translated");
				}
			} else {
				segment.setAttribute("state", "initial");
			}
			if (hasTarget) {
				segment.addContent(tgt2);
			}
			if ("locked".equals(source.getAttributeValue("ts"))) {
				segment.setAttribute("subState", "openxliff:locked");
			}
			List<Element> matches = source.getChildren("alt-trans");
			if (!matches.isEmpty()) {
				root2.setAttribute("xmlns:mtc", "urn:oasis:names:tc:xliff:matches:2.0");
				Element mtc = new Element("mtc:matches");
				unit.getContent().add(0, mtc);
				for (int i = 0; i < matches.size(); i++) {
					Element altTrans = matches.get(i);
					Element match = new Element("mtc:match");
					match.setAttribute("ref", "#" + source.getAttributeValue("id"));
					String matchQuality = altTrans.getAttributeValue("match-quality");
					if (!matchQuality.isEmpty()) {
						try {
							Float quality = Float.parseFloat(matchQuality);
							match.setAttribute("matchQuality", "" + quality);
						} catch (NumberFormatException nf) {
							// ignore
						}
					}
					String origin = altTrans.getAttributeValue("origin");
					if (!origin.isEmpty()) {
						match.setAttribute("origin", origin);
					}
					Element tsrc = new Element("source");
					mrkCount = 1;
					tsrc.setContent(harvestContent(altTrans.getChild("source"), null));
					match.addContent(tsrc);
					Element ttgt = new Element("target");
					mrkCount = 1;
					ttgt.setContent(harvestContent(altTrans.getChild("target"), null));
					match.addContent(ttgt);
					mtc.addContent(match);
				}
			}
			if (originalData.getChildren("data").isEmpty()) {
				unit.removeChild(originalData);
			}
			if (tagAttributes.getChildren("mda:metaGroup").isEmpty()) {
				unit.removeChild(tagAttributes);
			}
		}

		List<Element> children = source.getChildren();
		Iterator<Element> it = children.iterator();
		while (it.hasNext()) {
			recurse(it.next(), target);
		}
	}

	private static boolean hasGroup(Element metadata, Element group) {
		if (group.getAttributeValue("category").isEmpty()) {
			return false;
		}
		List<Element> groups = metadata.getChildren("mda:metaGroup");
		for (Element g : groups) {
			if (g.getAttributeValue("category").equals(group.getAttributeValue("category"))) {
				return true;
			}
		}
		return false;
	}

	private static void harvestInline(Element originalData, Element tagAttributes, Element tag) {
		if ("ph".equals(tag.getName())) {
			String id = "ph" + tag.getAttributeValue("id");
			if (!containsTag(originalData, id)) {
				Element data = new Element("data");
				data.setAttribute("id", id);
				data.setContent(tag.getContent());
				originalData.addContent(data);
				storeAttributes(tagAttributes, tag, id);
			}
			return;
		}
		if ("bpt".equals(tag.getName())) {
			String id = "bpt" + tag.getAttributeValue("id");
			if (!containsTag(originalData, id)) {
				Element data = new Element("data");
				data.setAttribute("id", id);
				data.setContent(tag.getContent());
				originalData.addContent(data);
				storeAttributes(tagAttributes, tag, id);
			}
			return;
		}
		if ("ept".equals(tag.getName())) {
			String id = "ept" + tag.getAttributeValue("id");
			if (!containsTag(originalData, id)) {
				Element data = new Element("data");
				data.setAttribute("id", id);
				data.setContent(tag.getContent());
				originalData.addContent(data);
				storeAttributes(tagAttributes, tag, id);
			}
			return;
		}
		if ("it".equals(tag.getName())) {
			String id = "it" + tag.getAttributeValue("id");
			if (!containsTag(originalData, id)) {
				Element data = new Element("data");
				data.setAttribute("id", id);
				data.setContent(tag.getContent());
				originalData.addContent(data);
				storeAttributes(tagAttributes, tag, id);
			}
			return;
		}
		if ("x".equals(tag.getName())) {
			String id = "x" + tag.getAttributeValue("id");
			storeAttributes(tagAttributes, tag, id);
			return;
		}
		List<Element> children = tag.getChildren();
		Iterator<Element> it = children.iterator();
		while (it.hasNext()) {
			harvestInline(originalData, tagAttributes, it.next());
		}
	}

	private static boolean containsTag(Element originalData, String id) {
		List<Element> tags = originalData.getChildren("data");
		Iterator<Element> it = tags.iterator();
		while (it.hasNext()) {
			Element tag = it.next();
			if (tag.getAttributeValue("id").equals(id)) {
				return true;
			}
		}
		return false;
	}

	private static void storeAttributes(Element tagAttributes, Element tag, String id) {
		if (tagAttributes == null) {
			return;
		}
		List<Attribute> atts = tag.getAttributes();
		if (atts.size() > 1) {
			Element group = new Element("mda:metaGroup");
			group.setAttribute("category", "attributes");
			group.setAttribute("id", id);
			tagAttributes.addContent(group);
			Iterator<Attribute> it = atts.iterator();
			while (it.hasNext()) {
				Attribute a = it.next();
				if (!"id".equals(a.getName())) {
					Element meta = new Element("mda:meta");
					meta.setAttribute("type", a.getName());
					meta.setText(a.getValue());
					group.addContent(meta);
				}
			}
		}
	}

	private static List<XMLNode> harvestContent(Element e, Element tagAttributes) throws SAXException, IOException {
		if ("sub".equals(e.getName())) {
			throw new SAXException(Messages.getString("ToXliff2.3"));
		}
		List<XMLNode> result = new ArrayList<>();
		if ("ph".equals(e.getName())) {
			Element ph = new Element("ph");
			String id = "ph" + e.getAttributeValue("id");
			ph.setAttribute("id", id);
			result.add(ph);
			return result;
		}
		if ("bpt".equals(e.getName())) {
			Element ph = new Element("ph");
			String id = "bpt" + e.getAttributeValue("id");
			ph.setAttribute("id", id);
			result.add(ph);
			return result;
		}
		if ("ept".equals(e.getName())) {
			Element ph = new Element("ph");
			String id = "ept" + e.getAttributeValue("id");
			ph.setAttribute("id", id);
			result.add(ph);
			return result;
		}
		if ("it".equals(e.getName())) {
			Element ph = new Element("ph");
			String id = "it" + e.getAttributeValue("id");
			ph.setAttribute("id", id);
			result.add(ph);
			return result;
		}
		if ("bx".equals(e.getName())) {
			Element ph = new Element("ph");
			String id = "bx" + e.getAttributeValue("id");
			ph.setAttribute("id", id);
			storeAttributes(tagAttributes, e, id);
			result.add(ph);
			return result;
		}
		if ("ex".equals(e.getName())) {
			Element ph = new Element("ph");
			String id = "ex" + e.getAttributeValue("id");
			ph.setAttribute("id", id);
			storeAttributes(tagAttributes, e, id);
			result.add(ph);
			return result;
		}
		if ("mrk".equals(e.getName())) {
			Element mrk = new Element("mrk");
			String id = e.hasAttribute("mid") ? "mrk" + e.getAttributeValue("mid") : "auto" + mrkCount++;
			mrk.setAttribute("id", id);
			String mtype = e.getAttributeValue("mtype");
			if (mtype.isEmpty()) {
				MessageFormat mf = new MessageFormat(Messages.getString("ToXliff2.4"));
				throw new IOException(mf.format(new String[] { e.toString() }));
			}
			if (mtype.equals("protected")) {
				mrk.setAttribute("translate", "no");
			}
			if (Arrays.asList("generic", "comment", "term").contains(mtype)) {
				mrk.setAttribute("type", mtype);
			} else {
				mrk.setAttribute("type", "oxlf:" + mtype.replace(":", "_"));
			}
			String ts = e.getAttributeValue("ts");
			if (!ts.isEmpty()) {
				mrk.setAttribute("value", ts);
			}
			if (e.hasAttribute("comment")) {
				storeAttributes(tagAttributes, e, id);
			}
			List<XMLNode> newContent = new ArrayList<>();
			List<XMLNode> content = e.getContent();
			Iterator<XMLNode> it = content.iterator();
			while (it.hasNext()) {
				XMLNode node = it.next();
				if (node.getNodeType() == XMLNode.TEXT_NODE) {
					newContent.add(node);
				}
				if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
					newContent.addAll(harvestContent((Element) node, tagAttributes));
				}
			}
			mrk.setContent(newContent);
			result.add(mrk);
			return result;
		}
		if ("g".equals(e.getName())) {
			Element pc = new Element("pc");
			String id = "g" + e.getAttributeValue("id");
			storeAttributes(tagAttributes, e, id);
			pc.setAttribute("id", id);
			List<XMLNode> newContent = new ArrayList<>();
			List<XMLNode> content = e.getContent();
			Iterator<XMLNode> it = content.iterator();
			while (it.hasNext()) {
				XMLNode node = it.next();
				if (node.getNodeType() == XMLNode.TEXT_NODE) {
					newContent.add(node);
				}
				if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
					newContent.addAll(harvestContent((Element) node, tagAttributes));
				}
			}
			pc.setContent(newContent);
			result.add(pc);
			return result;
		}
		if ("x".equals(e.getName())) {
			Element ph = new Element("ph");
			String id = "x" + e.getAttributeValue("id");
			ph.setAttribute("id", id);
			result.add(ph);
			return result;
		}
		List<XMLNode> content = e.getContent();
		Iterator<XMLNode> it = content.iterator();
		while (it.hasNext()) {
			XMLNode node = it.next();
			if (node.getNodeType() == XMLNode.TEXT_NODE) {
				result.add(node);
			}
			if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
				Element tag = (Element) node;
				result.addAll(harvestContent(tag, tagAttributes));
			}
		}
		return result;
	}
}
