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
package com.maxprograms.segmenter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import com.maxprograms.xml.Attribute;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.TextNode;
import com.maxprograms.xml.XMLNode;
import com.maxprograms.xml.XMLUtils;

import org.xml.sax.SAXException;

public class Segmenter {

	private Element root;
	private boolean cascade;
	private List<String> maps;
	private List<Element> rules;
	private Map<String, String> tags;
	private int tagId;

	public Segmenter(String srxFile, String srcLanguage, String catalog)
			throws SAXException, IOException, ParserConfigurationException, URISyntaxException {
		SAXBuilder builder = new SAXBuilder();
		builder.setEntityResolver(new Catalog(catalog));
		Document doc = builder.build(srxFile);
		root = doc.getRootElement();
		if (!root.getName().equals("srx")) {
			throw new IOException("Selected file is not an SRX document.");
		}
		if (!root.getAttributeValue("version").equals("2.0")) {
			throw new IOException("Incorrect SRX version.");
		}
		cascade = isCascading();
		buildRulesList(srcLanguage);
	}

	public Segmenter(Document doc, String srcLanguage) throws IOException {
		root = doc.getRootElement();
		if (!root.getName().equals("srx")) {
			throw new IOException("Selected file is not an SRX document.");
		}
		cascade = isCascading();
		buildRulesList(srcLanguage);
	}

	public String[] segment(String string) {
		if (string == null || string.equals("")) {
			return new String[] {};
		}
		String pureText = prepareString(string);
		List<String> parts = new ArrayList<>();
		for (int pos = 0; pos < pureText.length(); pos++) {
			String left = hideTags(pureText.substring(0, pos));
			String right = hideTags(pureText.substring(pos));
			if (left.equals("")) {
				continue;
			}
			for (int i = 0; i < rules.size(); i++) {
				Element rule = rules.get(i);
				boolean breaks = rule.getAttributeValue("break", "yes").equals("yes");
				Element before = rule.getChild("beforebreak");
				Element after = rule.getChild("afterbreak");
				String beforexp = "";
				if (before != null) {
					beforexp = before.getText();
				}
				String afterxp = "";
				if (after != null) {
					afterxp = after.getText();
				}
				if (!beforexp.equals("") && !afterxp.equals("")) {
					// match left and right
					if (endsWith(left, beforexp) && startsWith(right, afterxp)) {
						if (breaks) {
							parts.add(pureText.substring(0, pos));
							pureText = pureText.substring(pos);
							pos = 0;
						}
						break;
					}
				} else if (!beforexp.equals("")) {
					// match left side only
					if (endsWith(left, beforexp)) {
						if (breaks) {
							parts.add(pureText.substring(0, pos));
							pureText = pureText.substring(pos);
							pos = 0;
						}
						break;
					}
				} else {
					// match right side only
					if (startsWith(right, afterxp)) {
						if (breaks) {
							parts.add(pureText.substring(0, pos));
							pureText = pureText.substring(pos);
							pos = 0;
						}
						break;
					}
				}
			}
		}
		parts.add(pureText);
		String[] result = new String[parts.size()];
		for (int i = 0; i < parts.size(); i++) {
			result[i] = cleanup(parts.get(i));
		}
		return result;
	}

	private String hideTags(String string) {
		String result = string;
		Set<String> keys = tags.keySet();
		Iterator<String> it = keys.iterator();
		while (it.hasNext()) {
			String key = it.next();
			int index = result.indexOf(key);
			if (index != -1) {
				result = result.substring(0, index) + result.substring(index + 1);
			}
		}
		return result;
	}

	private static boolean endsWith(String string, String exp) {
		Pattern p = Pattern.compile(exp);
		String[] parts = p.split(string);
		if (parts.length > 0) {
			if (!string.endsWith(parts[parts.length - 1])) {
				String ends = string
						.substring(string.lastIndexOf(parts[parts.length - 1]) + parts[parts.length - 1].length());
				Matcher m = p.matcher(ends);
				return m.lookingAt();
			}
			Matcher m = p.matcher(parts[parts.length - 1]);
			return m.lookingAt();
		}
		// split() did not return any part (everything was removed from the returned
		// result).
		// perhaps the whole text matches
		Matcher m = p.matcher(string);
		return m.matches();
	}

	private static boolean startsWith(String string, String exp) {
		Pattern p = Pattern.compile(exp);
		Matcher m = p.matcher(string);
		return m.lookingAt();
	}

	private String prepareString(String raw) {
		String string = raw;
		tags = new HashMap<>();
		int k = 0;
		int start = string.indexOf("<mrk ");
		int end = string.indexOf("</mrk>");

		while (start != -1 && end != -1) {
			if (start > end) {
				break;
			}
			String tag = string.substring(start, end + 6);
			string = string.substring(0, start) + (char) ('\uE000' + k) + string.substring(end + 6);
			tags.put("" + (char) ('\uE000' + k), tag);
			k++;
			start = string.indexOf("<mrk ");
			end = string.indexOf("</mrk>");
		}

		start = string.indexOf("<ph");
		end = string.indexOf("</ph>");

		while (start != -1 && end != -1) {
			if (start > end) {
				break;
			}
			String tag = string.substring(start, end + 5);
			string = string.substring(0, start) + (char) ('\uE000' + k) + string.substring(end + 5);
			tags.put("" + (char) ('\uE000' + k), tag);
			k++;
			start = string.indexOf("<ph");
			end = string.indexOf("</ph>");
		}

		StringBuffer buffer = new StringBuffer();
		StringBuffer element = new StringBuffer();
		int length = string.length();
		boolean inElement = false;
		for (int i = 0; i < length; i++) {
			char c = string.charAt(i);
			if (c == '<' && string.indexOf('>', i) != -1) {
				inElement = true;
				int a = string.indexOf('<', i + 1);
				int b = string.indexOf('>', i + 1);
				if (a != -1 && a < b) {
					inElement = false;
				}
				if (i < length - 1 && !Character.isLetter(string.charAt(i + 1)) && string.charAt(i + 1) != '/') {
					inElement = false;
				}
			}
			if (inElement) {
				element.append(c);
			} else {
				buffer.append(c);
			}
			if (c == '>' && inElement) {
				inElement = false;
				tags.put("" + (char) ('\uE000' + k), element.toString());
				buffer.append((char) ('\uE000' + k));
				element = new StringBuffer();
				k++;
			}
		}
		return buffer.toString();
	}

	private String cleanup(String string) {
		String result = string;
		Set<String> keys = tags.keySet();
		Iterator<String> it = keys.iterator();
		while (it.hasNext()) {
			String key = it.next();
			int index = result.indexOf(key);
			if (index != -1) {
				result = result.substring(0, index) + tags.get(key) + result.substring(index + 1);
			}
		}
		return result;
	}

	private void buildRulesList(String srcLanguage) {
		maps = new ArrayList<>();
		List<Element> allMaps = root.getChild("body").getChild("maprules").getChildren("languagemap");
		Iterator<Element> it = allMaps.iterator();
		while (it.hasNext()) {
			Element map = it.next();
			if (srcLanguage.matches(map.getAttributeValue("languagepattern"))) {
				maps.add(map.getAttributeValue("languagerulename"));
				if (!cascade) {
					break;
				}
			}
		}
		rules = new ArrayList<>();
		List<Element> languageRules = root.getChild("body").getChild("languagerules").getChildren("languagerule");
		it = languageRules.iterator();
		while (it.hasNext()) {
			Element languagerule = it.next();
			String name = languagerule.getAttributeValue("languagerulename");
			if (maps.contains(name)) {
				List<Element> ruleset = languagerule.getChildren("rule");
				Iterator<Element> rit = ruleset.iterator();
				while (rit.hasNext()) {
					rules.add(rit.next());
				}
			}
		}
	}

	private boolean isCascading() {
		return root.getChild("header").getAttributeValue("cascade").equals("yes");
	}

	public Element segment(Element source) throws SAXException, IOException, ParserConfigurationException {
		tags = new HashMap<>();
		tagId = 0;
		String pureText = pureText(source);
		List<String> parts = new ArrayList<>();
		for (int pos = 0; pos < pureText.length(); pos++) {
			String left = hideTags(pureText.substring(0, pos));
			String right = hideTags(pureText.substring(pos));
			if (left.equals("")) {
				continue;
			}
			for (int i = 0; i < rules.size(); i++) {
				Element rule = rules.get(i);
				boolean breaks = rule.getAttributeValue("break", "yes").equals("yes");
				Element before = rule.getChild("beforebreak");
				Element after = rule.getChild("afterbreak");
				String beforexp = "";
				if (before != null) {
					beforexp = before.getText();
				}
				String afterxp = "";
				if (after != null) {
					afterxp = after.getText();
				}
				if (!beforexp.equals("") && !afterxp.equals("")) {
					// match left and right
					if (endsWith(left, beforexp) && startsWith(right, afterxp)) {
						if (breaks) {
							parts.add(pureText.substring(0, pos));
							pureText = pureText.substring(pos);
							pos = 0;
						}
						break;
					}
				} else if (!beforexp.equals("")) {
					// match left side only
					if (endsWith(left, beforexp)) {
						if (breaks) {
							parts.add(pureText.substring(0, pos));
							pureText = pureText.substring(pos);
							pos = 0;
						}
						break;
					}
				} else {
					// match right side only
					if (startsWith(right, afterxp)) {
						if (breaks) {
							parts.add(pureText.substring(0, pos));
							pureText = pureText.substring(pos);
							pos = 0;
						}
						break;
					}
				}
			}
		}
		parts.add(pureText);
		String[] result = new String[parts.size()];
		for (int i = 0; i < parts.size(); i++) {
			result[i] = cleanup(XMLUtils.cleanText(parts.get(i)));
		}
		if (result.length == 1) {
			// return a <seg-source> with the content of source
			Element res = new Element("seg-source");
			res.addContent("\n");
			Element mrk = new Element("mrk");
			mrk.setAttribute("mtype", "seg");
			mrk.setAttribute("mid", "1");
			res.addContent(mrk);
			List<XMLNode> content = source.getContent();
			Iterator<XMLNode> it = content.iterator();
			while (it.hasNext()) {
				XMLNode n = it.next();
				if (n.getNodeType() == XMLNode.TEXT_NODE) {
					mrk.addContent(n);
				}
				if (n.getNodeType() == XMLNode.ELEMENT_NODE) {
					mrk.addContent(n);
				}
				res.addContent("\n");
			}
			return res;
		}
		// generate segments
		Element res = new Element("seg-source");
		res.addContent("\n");
		SAXBuilder b = new SAXBuilder();
		for (int i = 0; i < result.length; i++) {
			String seg = "<mrk mtype=\"seg\" mid=\"" + (i + 1) + "\">" + result[i] + "</mrk>";
			Document docu = b.build(new ByteArrayInputStream(seg.getBytes(StandardCharsets.UTF_8)));
			Element mrk = new Element("mrk");
			mrk.clone(docu.getRootElement());
			res.addContent(mrk);
			res.addContent("\n");
		}
		return res;
	}

	private String pureText(Element e) {
		StringBuilder result = new StringBuilder();
		List<XMLNode> nodes = e.getContent();
		Iterator<XMLNode> it = nodes.iterator();
		while (it.hasNext()) {
			XMLNode n = it.next();
			if (n.getNodeType() == XMLNode.TEXT_NODE) {
				result.append(((TextNode) n).getText());
			}
			if (n.getNodeType() == XMLNode.ELEMENT_NODE) {
				Element tag = (Element) n;
				if (tag.getName().equals("ph") || tag.getName().equals("x") || tag.getName().equals("bx")
						|| tag.getName().equals("bpt") || tag.getName().equals("ept") || tag.getName().equals("it")) {
					tags.put("" + (char) ('\uE000' + tagId), tag.toString());
					result.append((char) ('\uE000' + tagId));
					tagId++;
				}
				if (tag.getName().equals("g") || tag.getName().equals("mrk")) {
					if (tag.getAttributeValue("translate", "yes").equals("yes")
							&& !tag.getAttributeValue("mtype", "").equals("protected")) {
						String start = "<" + tag.getName();
						List<Attribute> atts = tag.getAttributes();
						Iterator<Attribute> at = atts.iterator();
						while (at.hasNext()) {
							Attribute a = at.next();
							start = start + " " + a.getName() + "=\"" + a.getValue().replaceAll("\"", "&quote;") + "\""; //$NON-NLS-5$
						}
						start = start + ">";
						tags.put("" + (char) ('\uE000' + tagId), start);
						result.append((char) ('\uE000' + tagId));
						tagId++;

						result.append(pureText(tag));

						String end = "</" + tag.getName() + ">";
						tags.put("" + (char) ('\uE000' + tagId), end);
						result.append((char) ('\uE000' + tagId));
						tagId++;
					} else {
						tags.put("" + (char) ('\uE000' + tagId), tag.toString());
						result.append((char) ('\uE000' + tagId));
						tagId++;
					}
				}
			}
		}
		return result.toString();
	}
}
