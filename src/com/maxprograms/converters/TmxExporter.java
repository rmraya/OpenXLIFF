/*******************************************************************************
 * Copyright (c) 2023 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/
package com.maxprograms.converters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;

import com.maxprograms.xliff2.FromXliff2;
import com.maxprograms.xml.Attribute;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.PI;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.XMLNode;
import com.maxprograms.xml.XMLUtils;

import org.xml.sax.SAXException;

public class TmxExporter {

	public static final String DOUBLEPRIME = "\u2033";
	public static final String MATHLT = "\u2039";
	public static final String MATHGT = "\u200B\u203A";
	public static final String GAMP = "\u200B\u203A";

	static Map<String, String> docProperties;
	private static String sourceLang;
	private static String targetLang;
	private static String today;
	private static int match;
	private static String original;
	private static int filenumbr;

	private TmxExporter() {
		// do not instantiate this class
	}

	public static List<String> export(String xliff, String tmx, String catalog) {
		List<String> result = new ArrayList<>();
		try {
			today = getTmxDate();
			filenumbr = 0;

			SAXBuilder builder = new SAXBuilder();
			builder.setEntityResolver(new Catalog(catalog));
			Document doc = builder.build(xliff);
			Element root = doc.getRootElement();
			if (root.getAttributeValue("version").startsWith("2.")) {
				File tmpXliff = File.createTempFile("temp", ".xlf", new File(xliff).getParentFile());
				FromXliff2.run(xliff, tmpXliff.getAbsolutePath(), catalog);
				doc = builder.build(tmpXliff);
				root = doc.getRootElement();
				Files.delete(Paths.get(tmpXliff.toURI()));
			}

			try (FileOutputStream output = new FileOutputStream(tmx)) {
				Element firstFile = root.getChild("file");

				docProperties = new HashMap<>();
				List<PI> slist = root.getPI("subject");
				if (!slist.isEmpty()) {
					docProperties.put("subject", slist.get(0).getData());
				} else {
					docProperties.put("subject", "");
				}
				List<PI> plist = root.getPI("project");
				if (!plist.isEmpty()) {
					docProperties.put("project", plist.get(0).getData());
				} else {
					docProperties.put("project", "");
				}
				List<PI> clist = root.getPI("customer");
				if (!clist.isEmpty()) {
					docProperties.put("customer", clist.get(0).getData());
				} else {
					docProperties.put("customer", "");
				}

				sourceLang = firstFile.getAttributeValue("source-language");

				writeString(output, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
				writeString(output,
						"<!DOCTYPE tmx PUBLIC \"-//LISA OSCAR:1998//DTD for Translation Memory eXchange//EN\" \"tmx14.dtd\" >\n");
				writeString(output, "<tmx version=\"1.4\">\n");
				writeString(output,
						"<header \n" +
								"      creationtool=\"" + Constants.TOOLID + "\" \n" +
								"      creationtoolversion=\"" + Constants.VERSION + "\" \n" +
								"      srclang=\"" + sourceLang + "\" \n" +
								"      adminlang=\"en\"  \n      datatype=\"xml\" \n" +
								"      o-tmf=\"XLIFF\" \n" +
								"      segtype=\"block\"\n>\n" +
								"</header>\n");
				writeString(output, "<body>\n");

				List<Element> files = root.getChildren("file");
				Iterator<Element> fileiterator = files.iterator();
				while (fileiterator.hasNext()) {
					Element file = fileiterator.next();
					sourceLang = file.getAttributeValue("source-language");
					targetLang = file.getAttributeValue("target-language");
					original = "" + file.getAttributeValue("original").hashCode();
					recurse(output, file);
					filenumbr++;
				}
				writeString(output, "</body>\n");
				writeString(output, "</tmx>");
			}
			result.add(Constants.SUCCESS);
		} catch (IOException | SAXException | ParserConfigurationException | URISyntaxException e) {
			Logger logger = System.getLogger(TmxExporter.class.getName());
			logger.log(Level.ERROR, e.getMessage(), e);
			result.add(Constants.ERROR);
			result.add(e.getMessage());
		}
		return result;
	}

	private static void recurse(FileOutputStream output, Element e) throws IOException {
		List<Element> list = e.getChildren();
		Iterator<Element> i = list.iterator();
		while (i.hasNext()) {
			Element element = i.next();
			if (element.getName().equals("trans-unit")) {
				writeSegment(output, element);
			} else {
				recurse(output, element);
			}
		}
	}

	private static void writeSegment(FileOutputStream output, Element segment) throws IOException {

		String id = original + "-" + filenumbr + "-" + segment.getAttributeValue("id").hashCode();

		if (segment.getAttributeValue("approved").equals("yes")) {
			Element source = segment.getChild("source");
			if (source.getContent().isEmpty()) {
				// empty segment, nothing to export
				return;
			}
			Element target = segment.getChild("target");
			if (target == null) {
				return;
			}
			String srcLang = source.getAttributeValue("xml:lang");
			if (srcLang.isEmpty()) {
				srcLang = sourceLang;
			}
			String tgtLang = target.getAttributeValue("xml:lang");
			if (tgtLang.isEmpty()) {
				tgtLang = targetLang;
			}
			writeString(output,
					"<tu creationtool=\"" + Constants.TOOLNAME + "\" creationtoolversion=\"" + Constants.VERSION
							+ "\" tuid=\"" + id + "\" creationdate=\"" + today + "\">\n");

			String customer = docProperties.get("customer");
			String project = docProperties.get("project");
			String subject = docProperties.get("subject");

			if (!subject.isEmpty()) {
				writeString(output, "<prop type=\"subject\">" + XMLUtils.cleanText(subject) + "</prop>\n");
			}
			if (!project.isEmpty()) {
				writeString(output, "<prop type=\"project\">" + XMLUtils.cleanText(project) + "</prop>\n");
			}
			if (!customer.isEmpty()) {
				writeString(output, "<prop type=\"customer\">" + XMLUtils.cleanText(customer) + "</prop>\n");
			}

			List<Element> notes = segment.getChildren("note");
			Iterator<Element> it = notes.iterator();
			while (it.hasNext()) {
				Element note = it.next();
				String lang = note.getAttributeValue("xml:lang");
				if (!lang.isEmpty()) {
					lang = " xml:lang=\"" + lang + "\"";
				}
				writeString(output, "<note" + lang + ">" + XMLUtils.cleanText(note.getText()) + "</note>\n");
			}
			String srcText = extractText(source);
			String tgtText = extractText(target);
			if (!segment.getAttributeValue("xml:space", "default").equals("preserve")) {
				srcText = srcText.trim();
				tgtText = tgtText.trim();
			}
			writeString(output, "<tuv xml:lang=\"" + srcLang + "\" creationdate=\"" + today + "\">\n<seg>" + srcText
					+ "</seg>\n</tuv>\n");
			writeString(output, "<tuv xml:lang=\"" + tgtLang + "\" creationdate=\"" + today + "\">\n<seg>" + tgtText
					+ "</seg>\n</tuv>\n");
			writeString(output, "</tu>\n");
		}
	}

	public static String extractText(Element src) {

		String type = src.getName();

		if (type.equals("source") || type.equals("target")) {
			match = 0;
			List<XMLNode> l = src.getContent();
			Iterator<XMLNode> i = l.iterator();
			StringBuilder text = new StringBuilder();
			while (i.hasNext()) {
				XMLNode o = i.next();
				switch (o.getNodeType()) {
					case XMLNode.TEXT_NODE:
						text.append(o.toString());
						break;
					case XMLNode.ELEMENT_NODE:
						Element e = (Element) o;
						text.append(extractText(e));
						break;
					default:
						// ignore
				}
			}
			return text.toString();
		}

		if (type.equals("bx") || type.equals("ex") || type.equals("ph")) {
			List<XMLNode> l = src.getContent();
			Iterator<XMLNode> i = l.iterator();
			String ctype = src.getAttributeValue("ctype");
			if (!ctype.isEmpty()) {
				ctype = " type=\"" + XMLUtils.cleanText(ctype) + "\"";
			}
			String assoc = src.getAttributeValue("assoc");
			if (!assoc.isEmpty()) {
				assoc = " assoc=\"" + XMLUtils.cleanText(assoc) + "\"";
			}
			String x = "";
			if (type.equals("ph")) {
				x = src.getAttributeValue("id");
				if (!x.isEmpty()) {
					x = " x=\"" + XMLUtils.cleanText(x).hashCode() + "\"";
				}
			}
			StringBuilder text = new StringBuilder();
			text.append("<ph");
			text.append(ctype);
			text.append(assoc);
			text.append(x);
			text.append('>');
			while (i.hasNext()) {
				XMLNode o = i.next();
				switch (o.getNodeType()) {
					case XMLNode.TEXT_NODE:
						text.append(o.toString());
						break;
					case XMLNode.ELEMENT_NODE:
						Element e = (Element) o;
						if (e.getName().equals("sub")) {
							text.append(extractText(e));
						}
						if (!e.getName().equals("mrk")) {
							text.append(extractText(e));
						}
						break;
					default:
						// ignore
				}
			}
			return text + "</ph>";
		}

		if (type.equals("g") || type.equals("x")) {
			StringBuilder open = new StringBuilder();
			open.append('<');
			open.append(src.getName());
			List<Attribute> atts = src.getAttributes();
			Iterator<Attribute> h = atts.iterator();
			while (h.hasNext()) {
				Attribute a = h.next();
				open.append(' ');
				open.append(a.getName());
				open.append("=\"");
				open.append(a.getValue());
				open.append('\"');
			}
			List<XMLNode> l = src.getContent();
			if (!l.isEmpty()) {
				open.append('>');
				int i = match;
				match++;
				StringBuilder text = new StringBuilder();
				text.append("<bpt type=\"xliff-");
				text.append(src.getName());
				text.append("\" i=\"");
				text.append(i);
				text.append("\">");
				text.append(XMLUtils.cleanText(open.toString()));
				text.append("</bpt>");
				Iterator<XMLNode> k = l.iterator();
				while (k.hasNext()) {
					XMLNode n = k.next();
					if (n.getNodeType() == XMLNode.TEXT_NODE) {
						text.append(n.toString());
					}
					if (n.getNodeType() == XMLNode.ELEMENT_NODE) {
						text.append(extractText((Element) n));
					}
				}
				String close = "</" + src.getName() + ">";
				return text.toString() + "<ept i=\"" + i + "\">" + XMLUtils.cleanText(close) + "</ept>";
			}
			return "<ph type=\"xliff-" + src.getName() + "\">" + XMLUtils.cleanText(open + "/>") + "</ph>";
		}

		if (type.equals("it")) {
			List<XMLNode> l = src.getContent();
			Iterator<XMLNode> i = l.iterator();
			String ctype = src.getAttributeValue("ctype");
			if (!ctype.isEmpty()) {
				ctype = " type=\"" + XMLUtils.cleanText(ctype) + "\"";
			}
			String pos = src.getAttributeValue("pos");
			if (pos.equals("open")) {
				pos = " pos=\"begin\"";
			} else if (pos.equals("close")) {
				pos = " pos=\"end\"";
			}
			StringBuilder text = new StringBuilder();
			text.append("<it");
			text.append(ctype);
			text.append(pos);
			text.append('>');
			while (i.hasNext()) {
				XMLNode o = i.next();
				switch (o.getNodeType()) {
					case XMLNode.TEXT_NODE:
						text.append(o.toString());
						break;
					case XMLNode.ELEMENT_NODE:
						text.append(extractText((Element) o));
						break;
					default:
						// ignore
				}
			}
			text.append("</it>");
			return text.toString();
		}

		if (type.equals("bpt") || type.equals("ept")) {
			List<XMLNode> l = src.getContent();
			Iterator<XMLNode> i = l.iterator();
			String ctype = src.getAttributeValue("ctype");
			if (!ctype.isEmpty()) {
				ctype = " type=\"" + XMLUtils.cleanText(ctype) + "\"";
			}
			String rid = src.getAttributeValue("rid");
			if (!rid.isEmpty()) {
				rid = " i=\"" + XMLUtils.cleanText(rid).hashCode() + "\"";
			} else {
				rid = " i=\"" + XMLUtils.cleanText(src.getAttributeValue("id")).hashCode() + "\"";
			}
			StringBuilder text = new StringBuilder();
			text.append('<');
			text.append(type);
			text.append(ctype);
			text.append(rid);
			text.append('>');
			while (i.hasNext()) {
				XMLNode o = i.next();
				switch (o.getNodeType()) {
					case XMLNode.TEXT_NODE:
						text.append(o.toString());
						break;
					case XMLNode.ELEMENT_NODE:
						text.append(extractText((Element) o));
						break;
					default:
						// ignore
				}
			}
			text.append("</");
			text.append(type);
			text.append('>');
			return text.toString();
		}

		if (type.equals("sub")) {
			List<XMLNode> l = src.getContent();
			Iterator<XMLNode> i = l.iterator();
			StringBuilder text = new StringBuilder();
			text.append("<sub>");
			while (i.hasNext()) {
				XMLNode o = i.next();
				switch (o.getNodeType()) {
					case XMLNode.TEXT_NODE:
						text.append(o.toString());
						break;
					case XMLNode.ELEMENT_NODE:
						Element e = (Element) o;
						if (!e.getName().equals("mrk")) {
							text.append(extractText(e));
						}
						break;
					default:
						// ignore
				}
			}
			text.append("</sub>");
			return text.toString();
		}
		if (type.equals("mrk")) {
			if (src.getAttributeValue("mtype").equals("term")) {
				// ignore terminology entries
				return XMLUtils.cleanText(src.getText());
			}
			if (src.getAttributeValue("mtype").equals("protected")) {
				String ts = src.getAttributeValue("ts");
				ts = restoreChars(ts).trim();
				StringBuilder name = new StringBuilder();
				for (int i = 1; i < ts.length(); i++) {
					if (Character.isSpaceChar(ts.charAt(i))) {
						break;
					}
					name.append(ts.charAt(i));
				}
				return "<ph type=\"mrk-protected\" x=\""
						+ XMLUtils.cleanText(src.getAttributeValue("mid", "-")).hashCode()
						+ "\">" + XMLUtils.cleanText(ts) + "</ph>" + XMLUtils.cleanText(src.getText())
						+ "<ph type=\"mrk-close\">" + XMLUtils.cleanText("</" + name.toString() + ">") + "</ph>";
			}
			return "<hi type=\"" + src.getAttributeValue("mtype", "xliff-mrk") + "\">"
					+ XMLUtils.cleanText(src.getText()) + "</hi>";
		}
		return null;
	}

	private static String restoreChars(String string) {
		String result = string.replace(MATHLT, "<");
		result = result.replace(MATHGT, ">");
		result = result.replace(DOUBLEPRIME, "\"");
		return result.replace(GAMP, "&");
	}

	private static void writeString(FileOutputStream output, String input) throws IOException {
		output.write(input.getBytes(StandardCharsets.UTF_8));
	}

	public static String getTmxDate() {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		String sec = (calendar.get(Calendar.SECOND) < 10 ? "0" : "") + calendar.get(Calendar.SECOND);
		String min = (calendar.get(Calendar.MINUTE) < 10 ? "0" : "") + calendar.get(Calendar.MINUTE);
		String hour = (calendar.get(Calendar.HOUR_OF_DAY) < 10 ? "0" : "") + calendar.get(Calendar.HOUR_OF_DAY);
		String mday = (calendar.get(Calendar.DATE) < 10 ? "0" : "") + calendar.get(Calendar.DATE);
		String mon = (calendar.get(Calendar.MONTH) < 9 ? "0" : "") + (calendar.get(Calendar.MONTH) + 1);
		String longyear = "" + calendar.get(Calendar.YEAR);

		return longyear + mon + mday + "T" + hour + min + sec + "Z";
	}
}