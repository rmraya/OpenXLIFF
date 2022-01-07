/*******************************************************************************
 * Copyright (c) 2022 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/
package com.maxprograms.converters.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.Utils;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.PI;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.TextNode;
import com.maxprograms.xml.XMLNode;
import com.maxprograms.xml.XMLOutputter;

import org.json.JSONObject;
import org.xml.sax.SAXException;

public class Xliff2Xml {

	private static final Logger LOGGER = System.getLogger(Xliff2Xml.class.getName());

	private static String xliffFile;
	private static Map<String, Element> segments;
	private static String encoding;
	private static Catalog catalog;
	private static Map<String, String> entities;
	private static boolean inDesign = false;
	private static boolean inAttribute;
	private static boolean inCData;
	private static boolean dita_based = false;
	private static boolean IDML;
	private static List<PI> skipped;

	private Xliff2Xml() {
		// do not instantiate this class
		// use run method instead
	}

	public static List<String> run(Map<String, String> params) {
		List<String> result = new ArrayList<>();

		String sklFile = params.get("skeleton");
		xliffFile = params.get("xliff");
		encoding = params.get("encoding");
		String isInDesign = params.get("InDesign");
		if (isInDesign != null) {
			inDesign = true;
		}
		String isIDML = params.get("IDML");
		if (isIDML != null) {
			IDML = true;
		}
		String isDitaBased = params.get("dita_based");
		if (isDitaBased != null) {
			dita_based = true;
		}
		try {
			catalog = new Catalog(params.get("catalog"));
			String outputFile = params.get("backfile");
			File f = new File(outputFile);
			File p = f.getParentFile();
			if (p == null) {
				p = new File(System.getProperty("user.dir"));
			}
			if (!p.exists()) {
				p.mkdirs();
			}
			if (!f.exists()) {
				Files.createFile(Paths.get(f.toURI()));
			}
			try (FileOutputStream output = new FileOutputStream(f)) {
				loadSegments();
				InputStreamReader input = new InputStreamReader(new FileInputStream(sklFile), StandardCharsets.UTF_8);
				try (BufferedReader buffer = new BufferedReader(input)) {
					String line = buffer.readLine();
					while (line != null) {
						line = line + "\n";
						if (line.indexOf("%%%") != -1) {
							//
							// contains translatable text
							//
							int index = line.indexOf("%%%");
							while (index != -1) {
								String start = line.substring(0, index);
								writeString(output, start);
								line = line.substring(index + 3);
								String code = line.substring(0, line.indexOf("%%%"));
								line = line.substring(line.indexOf("%%%\n") + 4);
								Element segment = segments.get(code);
								if (segment != null) {
									inAttribute = segment.getAttributeValue("restype").equals("x-attribute");
									inCData = segment.getAttributeValue("restype").equals("x-cdata");
									Element target = segment.getChild("target");
									Element source = segment.getChild("source");
									if (target != null) {
										if (segment.getAttributeValue("approved", "no").equals("yes")) {
											writeString(output, extractText(target));
										} else {
											writeString(output, extractText(source));
										}
									} else {
										writeString(output, extractText(source));
									}
								} else {
									result.add(Constants.ERROR);
									MessageFormat mf = new MessageFormat("Segment {0} not found.");
									result.add(mf.format(new Object[] { code }));
									return result;
								}

								index = line.indexOf("%%%");
								if (index == -1) {
									writeString(output, line);
								}
							} // end while
						} else {
							//
							// non translatable portion
							//
							writeString(output, line);
						}
						line = buffer.readLine();
					}
				}
			}
			if (dita_based) {
				try {
					removeTranslate(outputFile);
					if (skipped != null) {
						for (int i = 0; i < skipped.size(); i++) {
							PI pi = skipped.get(i);
							JSONObject json = new JSONObject(pi.getData());
							String file = json.getString("file");
							File folder = new File(outputFile).getParentFile();
							File destination = new File(folder, file);
							Utils.decodeToFile(json.getString("base64"), destination.getAbsolutePath());
						}
					}
				} catch (SAXException sax) {
					LOGGER.log(Level.ERROR, "removeTranslate error: " + outputFile);
					throw sax;
				}
			}
			if (inDesign) {
				removeSeparators(outputFile);
			}
			result.add(Constants.SUCCESS);
		} catch (IOException | SAXException | ParserConfigurationException | URISyntaxException e) {
			LOGGER.log(Level.ERROR, "Error merging file", e);
			result.add(Constants.ERROR);
			result.add(e.getMessage());
		}
		return result;
	}

	private static void removeTranslate(String outputFile)
			throws SAXException, IOException, ParserConfigurationException {
		SAXBuilder builder = new SAXBuilder();
		builder.setEntityResolver(catalog);
		Document doc = builder.build(outputFile);
		Element root = doc.getRootElement();
		removeTranslateAtt(root);
		XMLOutputter outputter = new XMLOutputter();
		outputter.preserveSpace(true);
		try (FileOutputStream out = new FileOutputStream(outputFile)) {
			outputter.output(doc, out);
		}
	}

	private static void removeTranslateAtt(Element e) {
		if (e.getAttributeValue("removeTranslate", "no").equals("yes")) {
			e.removeAttribute("translate");
			e.removeAttribute("removeTranslate");
		}
		List<Element> children = e.getChildren();
		for (Element child : children) {
			removeTranslateAtt(child);
		}
	}

	private static void removeSeparators(String outputFile)
			throws SAXException, IOException, ParserConfigurationException {
		SAXBuilder builder = new SAXBuilder();
		builder.setEntityResolver(catalog);
		Document doc = builder.build(outputFile);
		Element root = doc.getRootElement();
		recurse(root);
		XMLOutputter outputter = new XMLOutputter();
		outputter.preserveSpace(true);
		try (FileOutputStream out = new FileOutputStream(outputFile)) {
			outputter.output(doc, out);
		}
	}

	private static void recurse(Element e) {
		List<XMLNode> content = e.getContent();
		for (int i = 0; i < content.size(); i++) {
			XMLNode n = content.get(i);
			if (n.getNodeType() == XMLNode.TEXT_NODE) {
				TextNode t = (TextNode) n;
				if (t.getText().startsWith("c_")) {
					t.setText("c_" + t.getText().substring(2).replace("_", "~sep~"));
				}
			}
			if (n.getNodeType() == XMLNode.ELEMENT_NODE) {
				recurse((Element) n);
			}
		}
	}

	private static String extractText(Element element) throws SAXException {
		String result = "";
		List<XMLNode> content = element.getContent();
		Iterator<XMLNode> i = content.iterator();

		if (element.getName().equals("ph")) {
			return fixEntities(element);
		}
		if (dita_based && element.getName().equals("mrk")) {
			return cleanMrk(element);
		}
		while (i.hasNext()) {
			XMLNode n = i.next();
			switch (n.getNodeType()) {
			case XMLNode.ELEMENT_NODE:
				Element e = (Element) n;
				String ph = extractText(e);
				result = result + ph;
				break;
			case XMLNode.TEXT_NODE:
				if (inAttribute) {
					result = result + addEntities(((TextNode) n).getText()).replaceAll("\"", "&quot;");
				} else if (inCData) {
					result = result + ((TextNode) n).getText();
				} else {
					String text = ((TextNode) n).getText();
					if (IDML && text.indexOf('\n') != -1) {
						text = text.replaceAll("\\n", "");
					}
					result = result + addEntities(text);
				}
				break;
			default:
				// ignore
				break;
			}
		}
		return result;
	}

	private static String cleanMrk(Element element) throws SAXException {
		String ts = element.getAttributeValue("ts");
		if (ts.isEmpty()) {
			throw new SAXException("Broken <mrk> element.");
		}
		ts = restoreChars(ts).trim();
		String name = "";
		for (int i = 1; i < ts.length(); i++) {
			if (Character.isSpaceChar(ts.charAt(i))) {
				break;
			}
			name = name + ts.charAt(i);
		}
		String content = "";
		List<XMLNode> nodes = element.getContent();
		Iterator<XMLNode> it = nodes.iterator();
		while (it.hasNext()) {
			XMLNode n = it.next();
			if (n.getNodeType() == XMLNode.TEXT_NODE) {
				content = content + addEntities(((TextNode) n).getText());
			}
			if (n.getNodeType() == XMLNode.ELEMENT_NODE) {
				Element e = (Element) n;
				String ph = extractText(e);
				content = content + ph;
			}
		}
		return ts + content + "</" + name + ">";
	}

	private static String restoreChars(String string) {
		String result = string.replace(Xml2Xliff.MATHLT, "<");
		result = result.replace(Xml2Xliff.MATHGT, ">");
		result = result.replace(Xml2Xliff.DOUBLEPRIME, "\"");
		result = result.replace(Xml2Xliff.GAMP, "&");
		return result;
	}

	public static String fixEntities(Element element) {
		String string = element.getText();

		int start = string.indexOf('&');
		String result = "";
		if (start > 0) {
			result = string.substring(0, start);
			string = string.substring(start);
		}
		while (start != -1) {
			int colon = string.indexOf(';');
			if (colon == -1) {
				// no ";", we are not in an entity
				result = result + "&amp;";
				string = string.substring(1);
			} else {
				boolean inEntity = true;
				for (int i = 1; i < colon; i++) {
					char c = string.charAt(i);
					if (Character.isWhitespace(c) || "&.@$*()[]{},/?\\\"\'+=-^".indexOf(c) != -1) {
						inEntity = false;
						break;
					}
				}
				if (!inEntity) {
					result = result + "&amp;";
					string = string.substring(1);
				} else {
					result = result + string.substring(0, colon + 1);
					string = string.substring(colon + 1);
				}
			}
			start = string.indexOf('&');
			if (start > 0) {
				result = result + string.substring(0, start);
				string = string.substring(start);
			}
		}

		return (result + string).replace("###AMP###", "&amp;");
	}

	private static String replaceEntities(String original, String token, String entity) {
		String result = original;
		int index = result.indexOf(token);
		while (index != -1) {
			String before = result.substring(0, index);
			String after = result.substring(index + token.length());
			// check if we are not inside an entity
			int amp = before.lastIndexOf('&');
			if (amp == -1) {
				// we are not in an entity
				result = before + entity + after;
			} else {
				boolean inEntity = true;
				for (int i = amp; i < before.length(); i++) {
					char c = before.charAt(i);
					if (Character.isWhitespace(c) || ";.@$*()[]{},/?\\\"\'+=-^".indexOf(c) != -1) {
						inEntity = false;
						break;
					}
				}
				if (inEntity) {
					// check for a colon in "after"
					int colon = after.indexOf(';');
					if (colon == -1) {
						// we are not in an entity
						result = before + entity + after;
					} else {
						// verify is there is something that breaks the entity before
						for (int i = 0; i < colon; i++) {
							char c = after.charAt(i);
							if (Character.isWhitespace(c) || "&.@$*()[]{},/?\\\"\'+=-^".indexOf(c) != -1) {
								break;
							}
						}
					}
				} else {
					// we are not in an entity
					result = before + entity + after;
				}
			}
			if (index < result.length()) {
				index = result.indexOf(token, index + 1);
			}
		}
		return result;
	}

	private static String addEntities(String string) {
		String result = string;
		int index = result.indexOf('&');
		while (index != -1) {
			int smcolon = result.indexOf(';', index);
			if (smcolon == -1) {
				// no semicolon. there is no chance it is an entity
				result = result.substring(0, index) + "&amp;" + result.substring(index + 1);
				index++;
			} else {
				int space = result.indexOf(' ', index);
				if (space == -1) {
					String name = result.substring(index + 1, smcolon);
					if (entities.containsKey(name)) {
						// it is an entity, jump to the semicolon
						index = smcolon;
					} else {
						result = result.substring(0, index) + "&amp;" + result.substring(index + 1);
						index++;
					}
				} else {
					// check if space appears before the semicolon
					if (space < smcolon) {
						// not an entity!
						result = result.substring(0, index) + "&amp;" + result.substring(index + 1);
						index++;
					} else {
						String name = result.substring(index + 1, smcolon);
						if (entities.containsKey(name)) {
							// it is a known entity, jump to the semicolon
							index = smcolon;
						} else {
							result = result.substring(0, index) + "&amp;" + result.substring(index + 1);
							index++;
						}
					}
				}
			}
			if (index < result.length() && index >= 0) {
				index = result.indexOf('&', index);
			} else {
				index = -1;
			}
		}
		StringTokenizer tok = new StringTokenizer(result, "<>", true);
		StringBuilder buff = new StringBuilder();
		while (tok.hasMoreElements()) {
			String str = tok.nextToken();
			if (str.equals("<")) {
				buff.append("&lt;");
			} else if (str.equals(">")) {
				buff.append("&gt;");
			} else {
				buff.append(str);
			}
		}
		result = buff.toString();
		// now replace common text with
		// the entities declared in the DTD

		Set<String> enu = entities.keySet();
		Iterator<String> it = enu.iterator();
		while (it.hasNext()) {
			String key = it.next();
			String value = entities.get(key);
			if (!value.isEmpty() && !key.equals("amp") && !key.equals("lt") && !key.equals("gt") && !key.equals("quot")
					&& !key.equals("apos")) {
				result = replaceEntities(result, value, "&" + key + ";");
			}
		}
		return result;
	}

	private static void writeString(FileOutputStream output, String string) throws IOException {
		output.write(string.getBytes(encoding));
	}

	private static void loadSegments() throws SAXException, IOException, ParserConfigurationException {
		SAXBuilder builder = new SAXBuilder();
		if (catalog != null) {
			builder.setEntityResolver(catalog);
		}

		Document doc = builder.build(xliffFile);
		Element root = doc.getRootElement();
		Element body = root.getChild("file").getChild("body");
		List<Element> units = body.getChildren("trans-unit");
		Iterator<Element> i = units.iterator();

		segments = new HashMap<>();

		while (i.hasNext()) {
			Element unit = i.next();
			if (dita_based) {
				checkUntranslatable(unit);
			}
			segments.put(unit.getAttributeValue("id"), unit);
		}

		entities = new HashMap<>();

		Element header = root.getChild("file").getChild("header");
		List<Element> groups = header.getChildren("prop-group");
		if (groups != null) {
			Iterator<Element> g = groups.iterator();
			while (g.hasNext()) {
				Element group = g.next();
				if (group.getAttributeValue("name").equals("entities")) {
					List<Element> props = group.getChildren("prop");
					Iterator<Element> p = props.iterator();
					while (p.hasNext()) {
						Element prop = p.next();
						entities.put(prop.getAttributeValue("prop-type"), prop.getText());
					}
				}
				if (group.getAttributeValue("name").equals("encoding")) {
					String stored = group.getChild("prop").getText();
					if (!stored.equals(encoding)) {
						encoding = stored;
					}
				}
			}
		}
		skipped = root.getChild("file").getPI("skipped");
	}

	private static void checkUntranslatable(Element unit) {
		Element source = unit.getChild("source");
		Element target = unit.getChild("target");
		if (target == null) {
			return;
		}
		List<Element> slist = source.getChildren("mrk");
		List<Element> tlist = target.getChildren("mrk");
		for (int i = 0; i < slist.size(); i++) {
			Element sg = slist.get(i);
			if (!sg.getAttributeValue("mtype").equals("protected")) {
				continue;
			}
			for (int j = 0; j < tlist.size(); j++) {
				Element tg = tlist.get(j);
				if (tg.getAttributeValue("mid").equals(sg.getAttributeValue("mid", "-"))) {
					tg.setContent(sg.getContent());
					break;
				}
			}
		}
	}

	protected static String replaceToken(String string, String token, String newText) {
		String result = string;
		int index = result.indexOf(token);
		while (index != -1) {
			result = result.substring(0, index) + newText + result.substring(index + token.length());
			index = result.indexOf(token, index + newText.length());
		}
		return result;
	}
}
