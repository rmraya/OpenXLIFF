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
package com.maxprograms.converters.ditamap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.lang.System.Logger.Level;
import java.lang.System.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.StringConverter;
import com.maxprograms.converters.Utils;
import com.maxprograms.converters.xml.Xliff2Xml;
import com.maxprograms.xml.CData;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.Indenter;
import com.maxprograms.xml.PI;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.TextNode;
import com.maxprograms.xml.XMLNode;
import com.maxprograms.xml.XMLOutputter;

public class Xliff2DitaMap {

	private static Hashtable<String, String[]> filesTable;

	public static Vector<String> run(Hashtable<String, String> params) {
		Vector<String> result = new Vector<>();
		String xliffFile = "";
		Xliff2DitaMap instance = new Xliff2DitaMap();
		try {
			xliffFile = params.get("xliff");
			String outputFile = params.get("backfile");
			filesTable = new Hashtable<>();
			String catalog = params.get("catalog");
			SAXBuilder builder = new SAXBuilder();
			builder.preserveCustomAttributes(true);
			builder.setEntityResolver(new Catalog(catalog));
			Document doc = builder.build(xliffFile);
			Element root = doc.getRootElement();
			List<Element> files = root.getChildren("file");
			Iterator<Element> it = files.iterator();
			while (it.hasNext()) {
				saveFile(it.next(), xliffFile);
			}
			String tgtlang = files.get(0).getAttributeValue("target-language",
					files.get(0).getAttributeValue("source-language"));
			Enumeration<String> keys = filesTable.keys();
			XMLOutputter outputter = new XMLOutputter();
			outputter.preserveSpace(true);
			while (keys.hasMoreElements()) {
				String topicFile = keys.nextElement();
				String[] values = filesTable.get(topicFile);
				Hashtable<String, String> params2 = new Hashtable<>();
				params2.put("xliff", values[0]);
				params2.put("skeleton", values[1]);
				File folder = new File(outputFile);
				File p = folder.getParentFile();
				if (p == null) {
					p = new File(System.getProperty("user.dir"));
				}
				if (!p.exists()) {
					p.mkdirs();
				}
				params2.put("backfile", outputFile);
				params2.put("encoding", params.get("encoding"));
				params2.put("catalog", params.get("catalog"));
				params2.put("dita_based", "yes");
				Vector<String> res = Xliff2Xml.run(params2);
				if (!Constants.SUCCESS.equals(res.get(0))) {
					return res;
				}

				doc = builder.build(outputFile);
				Element r = doc.getRootElement();

				List<PI> ish = doc.getPI("ish");
				String id = r.getAttributeValue("id", "");

				if (!ish.isEmpty() || id.startsWith("GUID-")) {
					restoreGUID(r);
				}

				r.setAttribute("xml:lang", tgtlang);
				Indenter.indent(r, 2);
				instance.cleanConref(r);
				try (FileOutputStream out = new FileOutputStream(outputFile)) {
					outputter.output(doc, out);
				}
				File f = new File(values[0]);
				Files.delete(Paths.get(f.toURI()));
			}

			result.add(Constants.SUCCESS);
		} catch (IOException | SAXException | ParserConfigurationException e) {
			Logger logger = System.getLogger(Xliff2DitaMap.class.getName());
			logger.log(Level.ERROR, "Error merging DITA Map", e);
			result.add(Constants.ERROR);
			result.add(e.getMessage());
		}
		return result;
	}

	private static void restoreGUID(Element r) {
		String href = r.getAttributeValue("href", "");
		if (!href.isEmpty()) {
			int index = href.indexOf("GUID");
			if (index != -1) {
				int end = href.indexOf('=', index + 1);
				if (end != -1) {
					String guid = href.substring(index, end);
					r.setAttribute("href", guid);
				}
			}
		}
		String conref = r.getAttributeValue("conref", "");
		int index = conref.indexOf('=');
		if (!conref.isEmpty() && conref.startsWith("GUID-") && index != -1) {
			String guid = conref.substring(0, index);
			index = conref.indexOf('#');
			if (index != -1) {
				String rest = conref.substring(index);
				r.setAttribute("conref", guid + rest);
			} else {
				r.setAttribute("conref", guid);
			}
		}
		r.removeAttribute("class");
		r.removeAttribute("ditaarch:DITAArchVersion");
		r.removeAttribute("domains");
		List<Element> children = r.getChildren();
		for (int i = 0; i < children.size(); i++) {
			restoreGUID(children.get(i));
		}
	}

	private void cleanConref(Element e) {
		if (!e.getAttributeValue("fluentaIgnore").isEmpty()) {
			e.removeAttribute("fluentaIgnore");
		}
		if (!e.getAttributeValue("keyref", "").equals("")
				&& e.getAttributeValue("status", "").equals("removeContent")) { //$NON-NLS-3$
			e.setContent(new Vector<>());
			e.removeAttribute("status");
		}
		if (!e.getAttributeValue("conref", "").equals("") && e.getAttributeValue("conaction", "").equals("")) { //$NON-NLS-6$
			emptyElement(e);
		}
		if (!e.getAttributeValue("conkeyref", "").equals("") && e.getAttributeValue("conaction", "").equals("")) { //$NON-NLS-6$
			emptyElement(e);
		}
		if (e.getAttributeValue("status", "").equals("removeContent")) {
			e.setContent(new Vector<>());
			e.removeAttribute("status");
		}
		List<Element> list = e.getChildren();
		for (int i = 0; i < list.size(); i++) {
			cleanConref(list.get(i));
		}
	}

	private void emptyElement(Element e) {
		List<Element> children = e.getChildren();
		if (children.isEmpty()) {
			e.setContent(new Vector<>());
		} else {
			List<XMLNode> content = e.getContent();
			Vector<XMLNode> newContent = new Vector<>();
			Iterator<XMLNode> it = content.iterator();
			while (it.hasNext()) {
				XMLNode n = it.next();
				if (n.getNodeType() == XMLNode.ELEMENT_NODE) {
					Element child = (Element) n;
					emptyElement(child);
					newContent.add(child);
				}
			}
			e.setContent(newContent);
		}
	}

	private static void saveFile(Element element, String xliffFile) throws IOException {
		Document doc = new Document(null, "xliff", null, null);
		Element root = doc.getRootElement();
		root.setAttribute("version", "1.2");
		Element file = new Element("file");
		file.clone(element);
		root.addContent(file);
		List<PI> encoding = element.getPI("encoding");
		if (!encoding.isEmpty()) {
			root.addContent(encoding.get(0));
		}
		File xliff = File.createTempFile("tmp", ".xlf", new File(xliffFile).getParentFile());
		Element internal = file.getChild("header").getChild("skl").getChild("internal-file");
		if (internal != null) {
			// embedded skeleton
			File tmp = File.createTempFile("internal", ".skl", new File(xliffFile).getParentFile());
			tmp.deleteOnExit();
			if (internal.getAttributeValue("form", "").equals("base64")) {
				Utils.decodeToFile(internal.getText(), tmp.getAbsolutePath());
			} else {
				try (FileOutputStream out = new FileOutputStream(tmp)) {
					List<XMLNode> content = internal.getContent();
					for (int h = 0; h < content.size(); h++) {
						XMLNode n = content.get(h);
						if (n.getNodeType() == XMLNode.TEXT_NODE) {
							out.write(StringConverter.toByteArray(((TextNode) n).getText()));
						} else if (n.getNodeType() == XMLNode.CDATA_SECTION_NODE) {
							out.write(((CData) n).getData().getBytes(StandardCharsets.UTF_8));
						}
					}
				}
			}
			file.getChild("header").getChild("skl").addContent(new Element("external-file"));
			file.getChild("header").getChild("skl").getChild("external-file").setAttribute("href",
					tmp.getAbsolutePath());
			file.getChild("header").getChild("skl").removeChild("internal-file");
		}
		XMLOutputter outputter = new XMLOutputter();
		outputter.preserveSpace(true);
		try (FileOutputStream output = new FileOutputStream(xliff.getAbsolutePath())) {
			outputter.output(doc, output);
		}
		filesTable.put(element.getAttributeValue("original"), new String[] { xliff.getAbsolutePath(),
				file.getChild("header").getChild("skl").getChild("external-file").getAttributeValue("href") });
	}

}
