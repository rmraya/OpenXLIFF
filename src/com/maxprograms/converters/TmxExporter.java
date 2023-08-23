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
import java.text.MessageFormat;
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

	private static Logger logger = System.getLogger(TmxExporter.class.getName());

	public static final String DOUBLEPRIME = "\u2033"; //$NON-NLS-1$
	public static final String MATHLT = "\u2039"; //$NON-NLS-1$
	public static final String MATHGT = "\u200B\u203A"; //$NON-NLS-1$
	public static final String GAMP = "\u200B\u203A"; //$NON-NLS-1$

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

	public static void main(String[] args) {
		String[] arguments = Utils.fixPath(args);
		String xliff = ""; //$NON-NLS-1$
		String tmx = ""; //$NON-NLS-1$
		String catalog = ""; //$NON-NLS-1$

		for (int i = 0; i < arguments.length; i++) {
			String arg = arguments[i];
			if (arg.equals("-help")) { //$NON-NLS-1$
				help();
				return;
			}
			if (arg.equals("-xliff") && (i + 1) < arguments.length) { //$NON-NLS-1$
				xliff = arguments[i + 1];
			}
			if (arg.equals("-tmx") && (i + 1) < arguments.length) { //$NON-NLS-1$
				tmx = arguments[i + 1];
			}
			if (arg.equals("-catalog") && (i + 1) < arguments.length) { //$NON-NLS-1$
				catalog = arguments[i + 1];
			}
		}
		if (arguments.length < 2) {
			help();
			return;
		}
		if (catalog.isEmpty()) {
			String home = System.getenv("OpenXLIFF_HOME"); //$NON-NLS-1$
			if (home == null) {
				home = System.getProperty("user.dir"); //$NON-NLS-1$
			}
			File catalogFolder = new File(new File(home), "catalog"); //$NON-NLS-1$
			if (!catalogFolder.exists()) {
				logger.log(Level.ERROR, Messages.getString("TmxExporter.0")); //$NON-NLS-1$
				return;
			}
			catalog = new File(catalogFolder, "catalog.xml").getAbsolutePath(); //$NON-NLS-1$
		}
		File catalogFile = new File(catalog);
		if (!catalogFile.exists()) {
			logger.log(Level.ERROR, Messages.getString("TmxExporter.1")); //$NON-NLS-1$
			return;
		}
		if (!catalogFile.isAbsolute()) {
			catalog = catalogFile.getAbsoluteFile().getAbsolutePath();
		}
		if (xliff.isEmpty()) {
			logger.log(Level.ERROR, Messages.getString("TmxExporter.2")); //$NON-NLS-1$
			return;
		}
		File xliffFile = new File(xliff);
		if (!xliffFile.isAbsolute()) {
			xliff = xliffFile.getAbsoluteFile().getAbsolutePath();
		}
		if (tmx.isEmpty()) {
			tmx = xliff + ".tmx"; //$NON-NLS-1$
		}
		
		export(xliff, tmx, catalog);
	}

	private static void help() {
		MessageFormat mf = new MessageFormat(
				Messages.getString("TmxExporter.3")); //$NON-NLS-1$
		String help = mf.format(
				new String[] { "\\".equals(File.pathSeparator) ? "exporttmx.bat" : "exporttmx.sh" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		System.out.println(help);
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
			if (root.getAttributeValue("version").startsWith("2.")) { //$NON-NLS-1$ //$NON-NLS-2$
				File tmpXliff = File.createTempFile("temp", ".xlf", new File(xliff).getParentFile()); //$NON-NLS-1$ //$NON-NLS-2$
				FromXliff2.run(xliff, tmpXliff.getAbsolutePath(), catalog);
				doc = builder.build(tmpXliff);
				root = doc.getRootElement();
				Files.delete(Paths.get(tmpXliff.toURI()));
			}

			try (FileOutputStream output = new FileOutputStream(tmx)) {
				Element firstFile = root.getChild("file"); //$NON-NLS-1$

				docProperties = new HashMap<>();
				List<PI> slist = root.getPI("subject"); //$NON-NLS-1$
				if (!slist.isEmpty()) {
					docProperties.put("subject", slist.get(0).getData()); //$NON-NLS-1$
				} else {
					docProperties.put("subject", ""); //$NON-NLS-1$ //$NON-NLS-2$
				}
				List<PI> plist = root.getPI("project"); //$NON-NLS-1$
				if (!plist.isEmpty()) {
					docProperties.put("project", plist.get(0).getData()); //$NON-NLS-1$
				} else {
					docProperties.put("project", ""); //$NON-NLS-1$ //$NON-NLS-2$
				}
				List<PI> clist = root.getPI("customer"); //$NON-NLS-1$
				if (!clist.isEmpty()) {
					docProperties.put("customer", clist.get(0).getData()); //$NON-NLS-1$
				} else {
					docProperties.put("customer", ""); //$NON-NLS-1$ //$NON-NLS-2$
				}

				sourceLang = firstFile.getAttributeValue("source-language"); //$NON-NLS-1$

				writeString(output, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"); //$NON-NLS-1$
				writeString(output,
						"<!DOCTYPE tmx PUBLIC \"-//LISA OSCAR:1998//DTD for Translation Memory eXchange//EN\" \"tmx14.dtd\" >\n"); //$NON-NLS-1$
				writeString(output, "<tmx version=\"1.4\">\n"); //$NON-NLS-1$
				writeString(output,
						"<header \n" + //$NON-NLS-1$
								"      creationtool=\"" + Constants.TOOLID + "\" \n" + //$NON-NLS-1$ //$NON-NLS-2$
								"      creationtoolversion=\"" + Constants.VERSION + "\" \n" + //$NON-NLS-1$ //$NON-NLS-2$
								"      srclang=\"" + sourceLang + "\" \n" + //$NON-NLS-1$ //$NON-NLS-2$
								"      adminlang=\"en\"  \n      datatype=\"xml\" \n" + //$NON-NLS-1$
								"      o-tmf=\"XLIFF\" \n" + //$NON-NLS-1$
								"      segtype=\"block\"\n>\n" + //$NON-NLS-1$
								"</header>\n"); //$NON-NLS-1$
				writeString(output, "<body>\n"); //$NON-NLS-1$

				List<Element> files = root.getChildren("file"); //$NON-NLS-1$
				Iterator<Element> fileiterator = files.iterator();
				while (fileiterator.hasNext()) {
					Element file = fileiterator.next();
					sourceLang = file.getAttributeValue("source-language"); //$NON-NLS-1$
					targetLang = file.getAttributeValue("target-language"); //$NON-NLS-1$
					original = "" + file.getAttributeValue("original").hashCode(); //$NON-NLS-1$ //$NON-NLS-2$
					recurse(output, file);
					filenumbr++;
				}
				writeString(output, "</body>\n"); //$NON-NLS-1$
				writeString(output, "</tmx>"); //$NON-NLS-1$
			}
			result.add(Constants.SUCCESS);
		} catch (IOException | SAXException | ParserConfigurationException | URISyntaxException e) {
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
			if (element.getName().equals("trans-unit")) { //$NON-NLS-1$
				writeSegment(output, element);
			} else {
				recurse(output, element);
			}
		}
	}

	private static void writeSegment(FileOutputStream output, Element segment) throws IOException {

		String id = original + "-" + filenumbr + "-" + segment.getAttributeValue("id").hashCode(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		if (segment.getAttributeValue("approved").equals("yes")) { //$NON-NLS-1$ //$NON-NLS-2$
			Element source = segment.getChild("source"); //$NON-NLS-1$
			if (source.getContent().isEmpty()) {
				// empty segment, nothing to export
				return;
			}
			Element target = segment.getChild("target"); //$NON-NLS-1$
			if (target == null) {
				return;
			}
			String srcLang = source.getAttributeValue("xml:lang"); //$NON-NLS-1$
			if (srcLang.isEmpty()) {
				srcLang = sourceLang;
			}
			String tgtLang = target.getAttributeValue("xml:lang"); //$NON-NLS-1$
			if (tgtLang.isEmpty()) {
				tgtLang = targetLang;
			}
			writeString(output,
					"<tu creationtool=\"" + Constants.TOOLNAME + "\" creationtoolversion=\"" + Constants.VERSION //$NON-NLS-1$ //$NON-NLS-2$
							+ "\" tuid=\"" + id + "\" creationdate=\"" + today + "\">\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			String customer = docProperties.get("customer"); //$NON-NLS-1$
			String project = docProperties.get("project"); //$NON-NLS-1$
			String subject = docProperties.get("subject"); //$NON-NLS-1$

			if (!subject.isEmpty()) {
				writeString(output, "<prop type=\"subject\">" + XMLUtils.cleanText(subject) + "</prop>\n"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (!project.isEmpty()) {
				writeString(output, "<prop type=\"project\">" + XMLUtils.cleanText(project) + "</prop>\n"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (!customer.isEmpty()) {
				writeString(output, "<prop type=\"customer\">" + XMLUtils.cleanText(customer) + "</prop>\n"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			List<Element> notes = segment.getChildren("note"); //$NON-NLS-1$
			Iterator<Element> it = notes.iterator();
			while (it.hasNext()) {
				Element note = it.next();
				String lang = note.getAttributeValue("xml:lang"); //$NON-NLS-1$
				if (!lang.isEmpty()) {
					lang = " xml:lang=\"" + lang + "\""; //$NON-NLS-1$ //$NON-NLS-2$
				}
				writeString(output, "<note" + lang + ">" + XMLUtils.cleanText(note.getText()) + "</note>\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			String srcText = extractText(source);
			String tgtText = extractText(target);
			if (!segment.getAttributeValue("xml:space", "default").equals("preserve")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				srcText = srcText.trim();
				tgtText = tgtText.trim();
			}
			writeString(output, "<tuv xml:lang=\"" + srcLang + "\" creationdate=\"" + today + "\">\n<seg>" + srcText //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+ "</seg>\n</tuv>\n"); //$NON-NLS-1$
			writeString(output, "<tuv xml:lang=\"" + tgtLang + "\" creationdate=\"" + today + "\">\n<seg>" + tgtText //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+ "</seg>\n</tuv>\n"); //$NON-NLS-1$
			writeString(output, "</tu>\n"); //$NON-NLS-1$
		}
	}

	public static String extractText(Element src) {

		String type = src.getName();

		if (type.equals("source") || type.equals("target")) { //$NON-NLS-1$ //$NON-NLS-2$
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

		if (type.equals("bx") || type.equals("ex") || type.equals("ph")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			List<XMLNode> l = src.getContent();
			Iterator<XMLNode> i = l.iterator();
			String ctype = src.getAttributeValue("ctype"); //$NON-NLS-1$
			if (!ctype.isEmpty()) {
				ctype = " type=\"" + XMLUtils.cleanText(ctype) + "\""; //$NON-NLS-1$ //$NON-NLS-2$
			}
			String assoc = src.getAttributeValue("assoc"); //$NON-NLS-1$
			if (!assoc.isEmpty()) {
				assoc = " assoc=\"" + XMLUtils.cleanText(assoc) + "\""; //$NON-NLS-1$ //$NON-NLS-2$
			}
			String x = ""; //$NON-NLS-1$
			if (type.equals("ph")) { //$NON-NLS-1$
				x = src.getAttributeValue("id"); //$NON-NLS-1$
				if (!x.isEmpty()) {
					x = " x=\"" + XMLUtils.cleanText(x).hashCode() + "\""; //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			StringBuilder text = new StringBuilder();
			text.append("<ph"); //$NON-NLS-1$
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
						if (e.getName().equals("sub")) { //$NON-NLS-1$
							text.append(extractText(e));
						}
						if (!e.getName().equals("mrk")) { //$NON-NLS-1$
							text.append(extractText(e));
						}
						break;
					default:
						// ignore
				}
			}
			return text + "</ph>"; //$NON-NLS-1$
		}

		if (type.equals("g") || type.equals("x")) { //$NON-NLS-1$ //$NON-NLS-2$
			StringBuilder open = new StringBuilder();
			open.append('<');
			open.append(src.getName());
			List<Attribute> atts = src.getAttributes();
			Iterator<Attribute> h = atts.iterator();
			while (h.hasNext()) {
				Attribute a = h.next();
				open.append(' ');
				open.append(a.getName());
				open.append("=\""); //$NON-NLS-1$
				open.append(a.getValue());
				open.append('\"');
			}
			List<XMLNode> l = src.getContent();
			if (!l.isEmpty()) {
				open.append('>');
				int i = match;
				match++;
				StringBuilder text = new StringBuilder();
				text.append("<bpt type=\"xliff-"); //$NON-NLS-1$
				text.append(src.getName());
				text.append("\" i=\""); //$NON-NLS-1$
				text.append(i);
				text.append("\">"); //$NON-NLS-1$
				text.append(XMLUtils.cleanText(open.toString()));
				text.append("</bpt>"); //$NON-NLS-1$
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
				String close = "</" + src.getName() + ">"; //$NON-NLS-1$ //$NON-NLS-2$
				return text.toString() + "<ept i=\"" + i + "\">" + XMLUtils.cleanText(close) + "</ept>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			return "<ph type=\"xliff-" + src.getName() + "\">" + XMLUtils.cleanText(open + "/>") + "</ph>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}

		if (type.equals("it")) { //$NON-NLS-1$
			List<XMLNode> l = src.getContent();
			Iterator<XMLNode> i = l.iterator();
			String ctype = src.getAttributeValue("ctype"); //$NON-NLS-1$
			if (!ctype.isEmpty()) {
				ctype = " type=\"" + XMLUtils.cleanText(ctype) + "\""; //$NON-NLS-1$ //$NON-NLS-2$
			}
			String pos = src.getAttributeValue("pos"); //$NON-NLS-1$
			if (pos.equals("open")) { //$NON-NLS-1$
				pos = " pos=\"begin\""; //$NON-NLS-1$
			} else if (pos.equals("close")) { //$NON-NLS-1$
				pos = " pos=\"end\""; //$NON-NLS-1$
			}
			StringBuilder text = new StringBuilder();
			text.append("<it"); //$NON-NLS-1$
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
			text.append("</it>"); //$NON-NLS-1$
			return text.toString();
		}

		if (type.equals("bpt") || type.equals("ept")) { //$NON-NLS-1$ //$NON-NLS-2$
			List<XMLNode> l = src.getContent();
			Iterator<XMLNode> i = l.iterator();
			String ctype = src.getAttributeValue("ctype"); //$NON-NLS-1$
			if (!ctype.isEmpty()) {
				ctype = " type=\"" + XMLUtils.cleanText(ctype) + "\""; //$NON-NLS-1$ //$NON-NLS-2$
			}
			String rid = src.getAttributeValue("rid"); //$NON-NLS-1$
			if (!rid.isEmpty()) {
				rid = " i=\"" + XMLUtils.cleanText(rid).hashCode() + "\""; //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				rid = " i=\"" + XMLUtils.cleanText(src.getAttributeValue("id")).hashCode() + "\""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
			text.append("</"); //$NON-NLS-1$
			text.append(type);
			text.append('>');
			return text.toString();
		}

		if (type.equals("sub")) { //$NON-NLS-1$
			List<XMLNode> l = src.getContent();
			Iterator<XMLNode> i = l.iterator();
			StringBuilder text = new StringBuilder();
			text.append("<sub>"); //$NON-NLS-1$
			while (i.hasNext()) {
				XMLNode o = i.next();
				switch (o.getNodeType()) {
					case XMLNode.TEXT_NODE:
						text.append(o.toString());
						break;
					case XMLNode.ELEMENT_NODE:
						Element e = (Element) o;
						if (!e.getName().equals("mrk")) { //$NON-NLS-1$
							text.append(extractText(e));
						}
						break;
					default:
						// ignore
				}
			}
			text.append("</sub>"); //$NON-NLS-1$
			return text.toString();
		}
		if (type.equals("mrk")) { //$NON-NLS-1$
			if (src.getAttributeValue("mtype").equals("term")) { //$NON-NLS-1$ //$NON-NLS-2$
				// ignore terminology entries
				return XMLUtils.cleanText(src.getText());
			}
			if (src.getAttributeValue("mtype").equals("protected")) { //$NON-NLS-1$ //$NON-NLS-2$
				String ts = src.getAttributeValue("ts"); //$NON-NLS-1$
				ts = restoreChars(ts).trim();
				StringBuilder name = new StringBuilder();
				for (int i = 1; i < ts.length(); i++) {
					if (Character.isSpaceChar(ts.charAt(i))) {
						break;
					}
					name.append(ts.charAt(i));
				}
				return "<ph type=\"mrk-protected\" x=\"" //$NON-NLS-1$
						+ XMLUtils.cleanText(src.getAttributeValue("mid", "-")).hashCode() //$NON-NLS-1$ //$NON-NLS-2$
						+ "\">" + XMLUtils.cleanText(ts) + "</ph>" + XMLUtils.cleanText(src.getText()) //$NON-NLS-1$ //$NON-NLS-2$
						+ "<ph type=\"mrk-close\">" + XMLUtils.cleanText("</" + name.toString() + ">") + "</ph>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
			return "<hi type=\"" + src.getAttributeValue("mtype", "xliff-mrk") + "\">" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					+ XMLUtils.cleanText(src.getText()) + "</hi>"; //$NON-NLS-1$
		}
		return null;
	}

	private static String restoreChars(String string) {
		String result = string.replace(MATHLT, "<"); //$NON-NLS-1$
		result = result.replace(MATHGT, ">"); //$NON-NLS-1$
		result = result.replace(DOUBLEPRIME, "\""); //$NON-NLS-1$
		return result.replace(GAMP, "&"); //$NON-NLS-1$
	}

	private static void writeString(FileOutputStream output, String input) throws IOException {
		output.write(input.getBytes(StandardCharsets.UTF_8));
	}

	public static String getTmxDate() {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT")); //$NON-NLS-1$
		String sec = (calendar.get(Calendar.SECOND) < 10 ? "0" : "") + calendar.get(Calendar.SECOND); //$NON-NLS-1$ //$NON-NLS-2$
		String min = (calendar.get(Calendar.MINUTE) < 10 ? "0" : "") + calendar.get(Calendar.MINUTE); //$NON-NLS-1$ //$NON-NLS-2$
		String hour = (calendar.get(Calendar.HOUR_OF_DAY) < 10 ? "0" : "") + calendar.get(Calendar.HOUR_OF_DAY); //$NON-NLS-1$ //$NON-NLS-2$
		String mday = (calendar.get(Calendar.DATE) < 10 ? "0" : "") + calendar.get(Calendar.DATE); //$NON-NLS-1$ //$NON-NLS-2$
		String mon = (calendar.get(Calendar.MONTH) < 9 ? "0" : "") + (calendar.get(Calendar.MONTH) + 1); //$NON-NLS-1$ //$NON-NLS-2$
		String longyear = "" + calendar.get(Calendar.YEAR); //$NON-NLS-1$

		return longyear + mon + mday + "T" + hour + min + sec + "Z"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}