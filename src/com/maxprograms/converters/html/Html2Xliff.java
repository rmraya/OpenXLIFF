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
package com.maxprograms.converters.html;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.CDataNode;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.FormElement;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.XmlDeclaration;
import org.xml.sax.SAXException;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.Utils;
import com.maxprograms.segmenter.Segmenter;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.TextNode;
import com.maxprograms.xml.XMLNode;

public class Html2Xliff {

	private static String inputFile;
	private static String skeletonFile;
	private static String sourceLanguage;
	private static String srcEncoding;

	private static int segId;
	private static int tagId;

	private static boolean segByElement;
	private static Segmenter segmenter;
	private static String catalog;
	private static String first;
	private static String last;
	private static String targetLanguage;

	private static SAXBuilder builder;
	private static StringBuffer segmentText;

	private static List<String> inlineElements = Arrays.asList("a", "abbr", "acronym", "audio", "b", "bdi", "bdo",
			"big", /* "br", */ "button", "canvas", "cite", "code", "data", "datalist", "del", "dfn", "em", "embed", "i",
			"iframe", /* "img", "input", */ "ins", "kbd", /* "label",*/ "map", "mark", "meter", "noscript", "object",
			"output",
			"picture", "progress", "q", "ruby", "s", "samp", "script", "select", "slot", "small", "span", "strong",
			"sub", "sup", /* "svg", */"template", "textarea", "time", "u", "tt", "var", "video", "wbr");

	private static List<String> translatableAttributes = Arrays.asList("alt", "label", "placeholder", "title");

	private Html2Xliff() {
		// do not instantiate this class
		// use run method instead
	}

	public static List<String> run(Map<String, String> params) {
		List<String> result = new ArrayList<>();

		inputFile = params.get("source");
		String xliffFile = params.get("xliff");
		skeletonFile = params.get("skeleton");
		sourceLanguage = params.get("srcLang");
		targetLanguage = params.get("tgtLang");
		srcEncoding = params.get("srcEncoding");
		catalog = params.get("catalog");
		String paragraphSegmentation = params.get("paragraph");
		if (paragraphSegmentation == null) {
			segByElement = false;
		} else {
			segByElement = paragraphSegmentation.equals("yes");
		}
		segmentText = new StringBuffer();
		try {
			org.jsoup.nodes.Document htmlDoc = Jsoup.parse(new File(inputFile), srcEncoding);
			builder = new SAXBuilder();
			if (!segByElement) {
				String initSegmenter = params.get("srxFile");
				segmenter = new Segmenter(initSegmenter, sourceLanguage, new Catalog(catalog));
			}
			try (FileOutputStream xlf = new FileOutputStream(xliffFile)) {
				try (FileOutputStream skl = new FileOutputStream(skeletonFile)) {
					writeHeader(xlf);
					recurse(htmlDoc, skl, xlf);
					writeTail(xlf);
				}
			}
			result.add(Constants.SUCCESS);
		} catch (IOException | SAXException | ParserConfigurationException | URISyntaxException e) {
			e.printStackTrace();
			Logger logger = System.getLogger(Html2Xliff.class.getName());
			logger.log(Level.ERROR, Messages.getString("Html2Xliff.2"), e);
			result.add(Constants.ERROR);
			result.add(e.getMessage());
		}
		return result;
	}

	private static void recurse(Node node, FileOutputStream skl, FileOutputStream xlf)
			throws IOException, SAXException, ParserConfigurationException {
		if (node instanceof org.jsoup.nodes.Document doc) {
			// root node, nothing to translate
			// simply iterate its children
		}
		if (node instanceof DocumentType docType) {
			write(skl, docType.toString());
		}
		if (node instanceof org.jsoup.nodes.Element element && !(node instanceof org.jsoup.nodes.Document)) {

			boolean inline = inlineElements.contains(element.nodeName());
			boolean emptyElement = element.childNodes().isEmpty();

			if (!segmentText.isEmpty() && segmentText.toString().isBlank()) {
				write(skl, segmentText.toString());
				segmentText = new StringBuffer();
			}

			if (!isTranslatable(element)) {
				write(skl, element.toString());
				return;
			}

			if (hasTranslatableAttributes(element)) {
				segmentText.append(getTranslatableHead(element));
			} else {
				if (!inline) {
					write(skl, getHead(element));
				} else {
					if (emptyElement) {
						segmentText.append("<ph id=\"");
						segmentText.append(tagId++);
						segmentText.append("\">");
						segmentText.append(Utils.cleanString(element.toString()));
						segmentText.append("</ph>");
					} else {
						segmentText.append("<ph id=\"");
						segmentText.append(tagId++);
						segmentText.append("\">");
						segmentText.append(Utils.cleanString(getHead(element)));
						segmentText.append("</ph>");
					}
				}
			}
		}
		if (node instanceof org.jsoup.nodes.TextNode text) {
			segmentText.append(text.getWholeText());
		}
		if (node instanceof CDataNode cdata) {
			if (!segmentText.isEmpty() && segmentText.toString().isBlank()) {
				write(skl, segmentText.toString());
				segmentText = new StringBuffer();
			}
			write(skl, cdata.toString());
		}
		if (node instanceof Comment comment) {
			if (!segmentText.isEmpty() && segmentText.toString().isBlank()) {
				write(skl, segmentText.toString());
				segmentText = new StringBuffer();
			}
			write(skl, comment.toString());
		}
		if (node instanceof DataNode data) {
			if (!segmentText.isEmpty() && segmentText.toString().isBlank()) {
				write(skl, segmentText.toString());
				segmentText = new StringBuffer();
			}
			write(skl, data.toString());
		}
		if (node instanceof FormElement form) {
			// process forms as elements
		}
		if (node instanceof XmlDeclaration decl) {
			if (!segmentText.isEmpty() && segmentText.toString().isBlank()) {
				write(skl, segmentText.toString());
				segmentText = new StringBuffer();
			}
			write(skl, decl.toString());
		}

		// process children nodes
		List<Node> children = node.childNodes();
		Iterator<Node> it = children.iterator();
		while (it.hasNext()) {
			recurse(it.next(), skl, xlf);
		}

		if (node instanceof org.jsoup.nodes.Element element && !(node instanceof org.jsoup.nodes.Document)) {
			// finish processing element content
			boolean inline = inlineElements.contains(element.nodeName());
			boolean emptyElement = element.childNodes().isEmpty();

			if (!inline) {
				String segment = segmentText.toString();
				String endSpaces = getEndingSpaces(segment);
				writeSegment(xlf, skl, segment.substring(0, segment.length() - endSpaces.length()),
						"pre".equalsIgnoreCase(element.nodeName()));
				segmentText = new StringBuffer();
				write(skl, endSpaces);
			}

			if (!emptyElement) {
				if (inline) {
					// close inline
					segmentText.append("<ph id=\"");
					segmentText.append(tagId++);
					segmentText.append("\">&lt;/");
					segmentText.append(element.nodeName());
					segmentText.append("&gt;</ph>");
				} else {
					// send closing tag to skeleton
					write(skl, "</" + element.nodeName() + ">");
				}
			}
		}
	}

	private static String getEndingSpaces(String segment) {
		StringBuilder sb = new StringBuilder();
		for (int i = segment.length(); i > 0; i--) {
			char c = segment.charAt(i - 1);
			if (Character.isWhitespace(c)) {
				sb.append(c);
			} else {
				break;
			}
		}
		return sb.toString();
	}

	private static String getTranslatableHead(org.jsoup.nodes.Element element) {
		StringBuilder sb = new StringBuilder();
		sb.append("<ph>&lt;");
		sb.append(element.nodeName());
		Attributes atts = element.attributes();
		List<Attribute> attsList = atts.asList();
		Iterator<Attribute> it = attsList.iterator();
		while (it.hasNext()) {
			Attribute a = it.next();
			sb.append(' ');
			sb.append(a.getKey());
			sb.append("=\"");
			if (translatableAttributes.contains(a.getKey().toLowerCase()) || "content".equalsIgnoreCase(a.getKey())) {
				sb.append("</ph>");
				sb.append(Utils.cleanString(a.getValue()));
				sb.append("<ph>");
			} else {
				sb.append(Utils.cleanString(a.getValue()));
			}
			sb.append('\"');
		}
		sb.append("&gt;</ph>");
		return sb.toString();
	}

	private static boolean hasTranslatableAttributes(org.jsoup.nodes.Element element) {
		Attributes atts = element.attributes();
		Iterator<String> it = translatableAttributes.iterator();
		while (it.hasNext()) {
			if (atts.hasKeyIgnoreCase(it.next())) {
				return true;
			}
		}
		if ("meta".equalsIgnoreCase(element.nodeName()) && atts.hasKeyIgnoreCase("name")) {
			String name = atts.getIgnoreCase("name");
			if ("description".equalsIgnoreCase(name) || "author".equalsIgnoreCase(name)
					|| "keywords".equalsIgnoreCase(name)) {
				return atts.hasKeyIgnoreCase("content");
			}
		}
		return false;
	}

	private static String getHead(org.jsoup.nodes.Element element) {
		StringBuilder sb = new StringBuilder();
		sb.append('<');
		sb.append(element.nodeName());
		Attributes atts = element.attributes();
		List<Attribute> list = atts.asList();
		Iterator<Attribute> it = list.iterator();
		while (it.hasNext()) {
			sb.append(' ');
			sb.append(it.next().toString());
		}
		sb.append(element.childNodes().isEmpty() ? "/>" : ">");
		return sb.toString();
	}

	private static boolean isTranslatable(org.jsoup.nodes.Element element) {
		if ("script".equalsIgnoreCase(element.nodeName())) {
			return false;
		}
		Attributes atts = element.attributes();
		boolean translate = true;
		if (atts.hasKeyIgnoreCase("translate")) {
			translate = !atts.getIgnoreCase("translate").equalsIgnoreCase("no");
		}
		return translate;
	}

	private static void writeHeader(FileOutputStream xlf) throws IOException {
		String tgtLang = "";
		if (targetLanguage != null) {
			tgtLang = "\" target-language=\"" + targetLanguage;
		}
		write(xlf, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		write(xlf, "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" "
				+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
				+ "xsi:schemaLocation=\"urn:oasis:names:tc:xliff:document:1.2 xliff-core-1.2-transitional.xsd\">\n");
		write(xlf,
				"  <file original=\"" + Utils.cleanString(inputFile) + "\" source-language=\"" + sourceLanguage
						+ tgtLang
						+ "\" tool-id=\"" + Constants.TOOLID + "\" datatype=\"html\">\n");

		write(xlf, "    <?encoding " + srcEncoding + "?>\n");
		write(xlf, "    <header>\n");
		write(xlf, "      <skl>\n");
		write(xlf, "        <external-file href=\"" + Utils.cleanString(skeletonFile) + "\"/>\n");
		write(xlf, "     </skl>\n");
		write(xlf, "     <tool tool-version=\"" + Constants.VERSION + " " + Constants.BUILD + "\" tool-id=\""
				+ Constants.TOOLID + "\" tool-name=\"" + Constants.TOOLNAME + "\"/>\n");
		write(xlf, "    </header>\n");
		write(xlf, "    <body>\n");
	}

	private static void writeTail(FileOutputStream xlf) throws IOException {
		write(xlf, "    </body>\n");
		write(xlf, "  </file>\n");
		write(xlf, "</xliff>");
	}

	private static void writeSegment(FileOutputStream xlf, FileOutputStream skl, String segment, boolean preserve)
			throws IOException, SAXException, ParserConfigurationException {
		String pure = removePH(segment);
		if (pure.isBlank()) {
			write(skl, phContent(segment));
			return;
		}
		if (segment.isBlank()) {
			write(skl, segment);
			return;
		}
		if (!preserve) {
			segment = segment.replaceAll("\\s+", " ");
		}

		Element source = builder
				.build(new ByteArrayInputStream(("<source>" + segment + "</source>").getBytes(StandardCharsets.UTF_8)))
				.getRootElement();

		Element segmentSource = segmenter.segment(source);
		List<Element> mrks = segmentSource.getChildren();
		for (int i = 0; i < mrks.size(); i++) {
			Element mrk = mrks.get(i);
			mrk.removeAttribute("mid");
			mrk.removeAttribute("mtype");
			first = "";
			last = "";
			segment = segmentCleanup(mrk);
			if (segment.isBlank()) {
				write(skl, first + segment + last);
				continue;
			}
			write(skl, first);
			tagId = 0;
			write(xlf, "      <trans-unit id=\"" + segId + "\" xml:space=\"preserve\">\n"
					+ "        <source>");
			write(xlf, segment);
			write(xlf, "</source>\n      </trans-unit>\n");
			write(skl, "%%%" + segId++ + "%%%" + last);
		}
	}

	private static String segmentCleanup(Element e)
			throws SAXException, IOException, ParserConfigurationException {
		List<XMLNode> content = e.getContent();
		int count = e.getChildren().size();

		if (count == 1) {
			XMLNode node = content.get(0);
			if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
				first = phContent(node.toString());
				content.remove(0);
			} else {
				node = content.get(content.size() - 1);
				if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
					last = phContent(node.toString());
					content.remove(content.size() - 1);
				}
			}
		}

		if (count == 2) {
			XMLNode firstNode = content.get(0);
			XMLNode lastNode = content.get(content.size() - 1);
			if (firstNode.getNodeType() == XMLNode.ELEMENT_NODE && lastNode.getNodeType() == XMLNode.ELEMENT_NODE) {
				first = ((Element) firstNode).getText();
				content.remove(firstNode);
				last = ((Element) lastNode).getText();
				content.remove(lastNode);
			}
		}
		e.setContent(content);
		String es = e.toString();
		return es.substring("<mrk>".length(), es.length() - "</mrk>".length());
	}

	private static String phContent(String segment) throws SAXException, IOException, ParserConfigurationException {
		ByteArrayInputStream stream = new ByteArrayInputStream(
				("<x>" + segment + "</x>").getBytes(StandardCharsets.UTF_8));
		Document d = builder.build(stream);
		Element e = d.getRootElement();
		List<XMLNode> content = e.getContent();
		Iterator<XMLNode> it = content.iterator();
		String result = "";
		while (it.hasNext()) {
			XMLNode n = it.next();
			if (n.getNodeType() == XMLNode.ELEMENT_NODE) {
				result = result + ((Element) n).getText();
			}
			if (n.getNodeType() == XMLNode.TEXT_NODE) {
				result = result + ((TextNode) n).getText();
			}
		}
		return result;
	}

	private static String removePH(String segment) throws SAXException, IOException, ParserConfigurationException {
		ByteArrayInputStream stream = new ByteArrayInputStream(
				("<x>" + segment + "</x>").getBytes(StandardCharsets.UTF_8));
		Document d = builder.build(stream);
		Element e = d.getRootElement();
		List<XMLNode> content = e.getContent();
		Iterator<XMLNode> it = content.iterator();
		String result = "";
		while (it.hasNext()) {
			XMLNode n = it.next();
			if (n.getNodeType() == XMLNode.TEXT_NODE) {
				result = result + ((TextNode) n).getText();
			}
		}
		return result;
	}

	private static void write(FileOutputStream out, String string) throws IOException {
		out.write(string.getBytes(StandardCharsets.UTF_8));
	}
}
