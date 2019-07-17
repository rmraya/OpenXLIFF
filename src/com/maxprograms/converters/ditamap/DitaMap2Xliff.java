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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.lang.System.Logger.Level;
import java.lang.System.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.EncodingResolver;
import com.maxprograms.converters.FileFormats;
import com.maxprograms.converters.Utils;
import com.maxprograms.converters.xml.Xml2Xliff;
import com.maxprograms.xml.Attribute;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.Indenter;
import com.maxprograms.xml.PI;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.SilentErrorHandler;
import com.maxprograms.xml.XMLNode;
import com.maxprograms.xml.XMLOutputter;

public class DitaMap2Xliff {

	private static final Logger LOGGER = System.getLogger(DitaMap2Xliff.class.getName());

	private static Element mergedRoot;
	private static SAXBuilder builder;
	private static boolean hasConref;
	private static Scope rootScope;
	private static boolean hasConKeyRef;
	private static Hashtable<String, Set<String>> excludeTable;
	private static Hashtable<String, Set<String>> includeTable;
	private static boolean filterAttributes;
	private static boolean elementsExcluded;

	private DitaMap2Xliff() {
		// do not instantiate this class
		// use run method instead
	}

	public static Vector<String> run(Hashtable<String, String> params) {
		Vector<String> result = new Vector<>();
		try {
			String xliffFile = params.get("xliff");
			String skeleton = params.get("skeleton");
			Catalog catalog = new Catalog(params.get("catalog"));
			String mapFile = params.get("source");

			DitaParser parser = new DitaParser();
			Vector<String> filesMap = parser.run(params);
			rootScope = parser.getScope();

			builder = new SAXBuilder();
			builder.setEntityResolver(catalog);
			builder.preserveCustomAttributes(true);
			builder.setErrorHandler(new SilentErrorHandler());

			Vector<String> xliffs = new Vector<>();
			Vector<String> skels = new Vector<>();

			String ditaval = params.get("ditaval");
			if (ditaval != null) {
				parseDitaVal(ditaval, catalog);
			}

			for (int i = 0; i < filesMap.size(); i++) {
				String file = filesMap.get(i);
				String source = "";
				try {
					source = checkConref(file);
				} catch (SkipException skip) {
					// skip untranslatable files
					continue;
				} catch (Exception ex) {
					continue;
				}
				if (excludeTable != null) {
					try {
						source = removeFiltered(source);
					} catch (Exception sax) {
						// skip directly referenced images
						continue;
					}
				}
				try {
					source = checkConKeyRef(source);
				} catch (Exception sax) {
					// skip directly referenced images
					continue;
				}

				File sklParent = new File(skeleton).getParentFile();
				if (!sklParent.exists()) {
					sklParent.mkdirs();
				}
				File skl = File.createTempFile("dita", ".skl", sklParent);
				skels.add(skl.getAbsolutePath());
				File xlf = File.createTempFile("dita", ".xlf", new File(skeleton).getParentFile());
				xlf.deleteOnExit();
				xliffs.add(xlf.getAbsolutePath());

				Charset encoding = EncodingResolver.getEncoding(source, FileFormats.XML);
				Hashtable<String, String> params2 = new Hashtable<>();
				params2.put("source", source);
				params2.put("xliff", xlf.getAbsolutePath());
				params2.put("skeleton", skl.getAbsolutePath());
				params2.put("srcLang", params.get("srcLang"));
				String tgtLang = params.get("tgtLang");
				if (tgtLang != null) {
					params2.put("tgtLang", tgtLang);
				}
				params2.put("catalog", params.get("catalog"));
				params2.put("srcEncoding", encoding.name());
				params2.put("srxFile", params.get("srxFile"));
				params2.put("paragraph", params.get("paragraph"));
				params2.put("dita_based", "yes");
				String tComments = params.get("translateComments");
				if (tComments != null) {
					params2.put("translateComments", tComments);
				}
				Vector<String> res = Xml2Xliff.run(params2);
				if (!Constants.SUCCESS.equals(res.get(0))) {
					return res;
				}
				if (!source.equals(filesMap.get(i))) {
					// original has conref
					fixSource(xlf.getAbsolutePath(), filesMap.get(i));
				}

			}

			Document merged = new Document(null, "xliff", null, null);
			mergedRoot = merged.getRootElement();
			mergedRoot.setAttribute("version", "1.2");
			mergedRoot.setAttribute("xmlns", "urn:oasis:names:tc:xliff:document:1.2");
			mergedRoot.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			mergedRoot.setAttribute("xsi:schemaLocation",
					"urn:oasis:names:tc:xliff:document:1.2 xliff-core-1.2-transitional.xsd");
			mergedRoot.addContent("\n");
			mergedRoot.addContent(new PI("encoding", params.get("srcEncoding")));

			for (int i = 0; i < xliffs.size(); i++) {
				mergedRoot.addContent("\n");
				addFile(xliffs.get(i), mapFile);
			}

			// output final XLIFF

			XMLOutputter outputter = new XMLOutputter();
			Indenter.indent(mergedRoot, 2);
			outputter.preserveSpace(true);
			try (FileOutputStream output = new FileOutputStream(xliffFile)) {
				outputter.output(merged, output);
			}
			result.add(Constants.SUCCESS);
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, "Error converting DITA Map", e);
			result.add(Constants.ERROR);
			result.add(e.getMessage());
		}
		return result;
	}

	private static String removeFiltered(String source) throws IOException, SAXException, ParserConfigurationException {
		Document doc = builder.build(source);
		Element root = doc.getRootElement();
		elementsExcluded = false;
		recurseExcluding(root);
		if (elementsExcluded) {
			Charset encoding = EncodingResolver.getEncoding(source, FileFormats.XML);
			File temp = File.createTempFile("temp", ".dita");
			temp.deleteOnExit();
			XMLOutputter outputter = new XMLOutputter();
			outputter.setEncoding(encoding);
			outputter.preserveSpace(true);
			try (FileOutputStream out = new FileOutputStream(temp)) {
				outputter.output(doc, out);
			}
			return temp.getAbsolutePath();
		}
		doc = null;
		root = null;
		return source;
	}

	private static void recurseExcluding(Element root) {
		List<Element> children = root.getChildren();
		for (int i = 0; i < children.size(); i++) {
			Element child = children.get(i);
			if (filterOut(child)) {
				child.setAttribute("fluentaIgnore", "yes");
				elementsExcluded = true;
			} else {
				recurseExcluding(child);
			}
		}
	}

	private static String checkConKeyRef(String source) throws IOException, SAXException, ParserConfigurationException {
		Document doc = builder.build(source);
		Element root = doc.getRootElement();
		hasConKeyRef = false;
		fixConKeyRef(root, source, doc);
		if (hasConKeyRef) {
			Charset encoding = EncodingResolver.getEncoding(source, FileFormats.XML);
			File temp = File.createTempFile("temp", ".dita");
			temp.deleteOnExit();
			XMLOutputter outputter = new XMLOutputter();
			outputter.setEncoding(encoding);
			outputter.preserveSpace(true);
			try (FileOutputStream out = new FileOutputStream(temp)) {
				outputter.output(doc, out);
			}
			return temp.getAbsolutePath();
		}
		return source;
	}

	private static void fixConKeyRef(Element e, String source, Document doc)
			throws IOException, SAXException, ParserConfigurationException {
		String conkeyref = e.getAttributeValue("conkeyref", "");
		String conaction = e.getAttributeValue("conaction", "");
		String keyref = e.getAttributeValue("keyref", "");
		if (!conaction.equals("")) { // it's a conref push
			conkeyref = "";
		}
		if (!conkeyref.isEmpty()) {
			if (conkeyref.indexOf('/') != -1) {
				String key = conkeyref.substring(0, conkeyref.indexOf('/'));
				String id = conkeyref.substring(conkeyref.indexOf('/') + 1);
				Key k = rootScope.getKey(key);
				String file = k.getHref();
				if (file == null) {
					LOGGER.log(Level.WARNING, "Key not defined for conkeyref: \"" + conkeyref + "\"");
					return;
				}
				Element ref = getConKeyReferenced(file, id);
				if (ref != null) {
					hasConKeyRef = true;
					if (e.getChildren().isEmpty()) {
						e.setAttribute("status", "removeContent");
					}
					e.setContent(ref.getContent());
					if (ref.getAttributeValue("translate", "yes").equals("no")
							&& e.getAttributeValue("translate", "yes").equals("yes")) {
						e.setAttribute("translate", "no");
						e.setAttribute("removeTranslate", "yes");
					}
					List<Element> children = e.getChildren();
					Iterator<Element> its = children.iterator();
					while (its.hasNext()) {
						fixConKeyRef(its.next(), file, doc);
					}
				} else {
					LOGGER.log(Level.WARNING, "Invalid conkeyref: \"" + conkeyref + "\" - Element with id=\"" + id
							+ "\" not found in \"" + file + "\"");
				}
			} else {
				LOGGER.log(Level.WARNING, "Invalid conkeyref: \"" + conkeyref + "\" - Bad format.");
			}

		} else if (!keyref.isEmpty() && e.getContent().isEmpty() && keyref.indexOf('/') == -1) {

			Key k = rootScope.getKey(keyref);

			if (k != null) {
				if (k.getTopicmeta() != null) {
					// empty element that reuses from <topicmenta>
					Element matched = DitaParser.getMatched(e.getName(), k.getTopicmeta());
					if (matched != null) {
						if (e.getChildren().isEmpty()) {
							e.setAttribute("status", "removeContent");
						}
						e.setContent(matched.getContent());
						if (matched.getAttributeValue("translate", "yes").equals("no")
								&& e.getAttributeValue("translate", "yes").equals("yes")) {
							e.setAttribute("translate", "no");
							e.setAttribute("removeTranslate", "yes");
						}
						hasConKeyRef = true;
					} else {
						if (DitaParser.ditaClass(e, "topic/image") || DitaParser.isImage(e.getName())) {
							Element keyword = DitaParser.getMatched("keyword", k.getTopicmeta());
							if (keyword != null) {
								Element alt = new Element("alt");
								alt.setContent(keyword.getContent());
								if (keyword.getAttributeValue("translate", "yes").equals("no")
										&& e.getAttributeValue("translate", "yes").equals("yes")) {
									e.setAttribute("translate", "no");
									e.setAttribute("removeTranslate", "yes");
								}
								e.addContent(alt);
								e.setAttribute("status", "removeContent");
								hasConKeyRef = true;
							}
						} else if (DitaParser.ditaClass(e, "topic/xref") || DitaParser.ditaClass(e, "topic/link")
								|| DitaParser.isXref(e.getName()) || DitaParser.isLink(e.getName())) {
							Element keyword = DitaParser.getMatched("keyword", k.getTopicmeta());
							if (keyword != null) {
								Element alt = new Element("linktext");
								alt.setContent(keyword.getContent());
								if (keyword.getAttributeValue("translate", "yes").equals("no")
										&& e.getAttributeValue("translate", "yes").equals("yes")) {
									e.setAttribute("translate", "no");
									e.setAttribute("removeTranslate", "yes");
								}
								e.addContent(alt);
								e.setAttribute("status", "removeContent");
								hasConKeyRef = true;
							}
						} else {
							Element keyword = DitaParser.getMatched("keyword", k.getTopicmeta());
							if (keyword != null) {
								e.addContent(keyword);
								if (keyword.getAttributeValue("translate", "yes").equals("no")
										&& e.getAttributeValue("translate", "yes").equals("yes")) {
									e.setAttribute("translate", "no");
									e.setAttribute("removeTranslate", "yes");
								}
								e.setAttribute("status", "removeContent");
								hasConKeyRef = true;
							}
						}
					}
				} else {
					String href = k.getHref();
					try {
						Document d = builder.build(href);
						Element r = d.getRootElement();
						Element referenced = r;
						if (keyref.indexOf('/') != -1) {
							String id = keyref.substring(keyref.indexOf('/') + 1);
							referenced = locateReferenced(r, id);
						}
						if (referenced != null && e.getName().equals(referenced.getName())) {
							if (e.getChildren().isEmpty()) {
								e.setAttribute("status", "removeContent");
							}
							e.setContent(referenced.getContent());
							hasConKeyRef = true;
						} else {
							if (e.getName().equals("abbreviated-form") && referenced != null
									&& referenced.getName().equals("glossentry")) {
								List<XMLNode> content = getGlossContent(referenced);
								if (content != null && !content.isEmpty()) {
									if (e.getChildren().isEmpty()) {
										e.setAttribute("status", "removeContent");
									}
									e.setContent(content);
									hasConKeyRef = true;
								}
							}
						}
						List<Element> children = e.getChildren();
						Iterator<Element> it = children.iterator();
						while (it.hasNext()) {
							fixConKeyRef(it.next(), source, doc);
						}
					} catch (Exception ex) {
						// do nothing
					}
				}
			}
		} else {
			List<Element> children = e.getChildren();
			Iterator<Element> it = children.iterator();
			while (it.hasNext()) {
				fixConKeyRef(it.next(), source, doc);
			}
		}
	}

	private static List<XMLNode> getGlossContent(Element glossentry) {
		Element e = getGlossComponent("glossSurfaceForm", glossentry);
		if (e != null) {
			return e.getContent();
		}
		e = getGlossComponent("glossAbbreviation", glossentry);
		if (e != null) {
			return e.getContent();
		}
		e = getGlossComponent("glossAlt", glossentry);
		if (e != null) {
			return e.getContent();
		}
		e = getGlossComponent("glossterm", glossentry);
		if (e != null) {
			return e.getContent();
		}
		return null;
	}

	private static Element getGlossComponent(String component, Element e) {
		if (e.getName().equals(component)) {
			return e;
		}
		List<Element> children = e.getChildren();
		for (int i = 0; i < children.size(); i++) {
			Element g = getGlossComponent(component, children.get(i));
			if (g != null) {
				return g;
			}
		}
		return null;
	}

	private static Element getConKeyReferenced(String file, String id)
			throws SAXException, IOException, ParserConfigurationException {
		Document doc = builder.build(file);
		Element root = doc.getRootElement();
		return locateReferenced(root, id);
	}

	private static Element locateReferenced(Element root, String id) {
		String current = root.getAttributeValue("id", "");
		if (current.equals(id)) {
			return root;
		}
		List<Element> children = root.getChildren();
		Iterator<Element> it = children.iterator();
		while (it.hasNext()) {
			Element child = it.next();
			Element result = locateReferenced(child, id);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	private static void fixSource(String xliff, String string)
			throws SAXException, IOException, ParserConfigurationException {
		Document doc = builder.build(xliff);
		Element root = doc.getRootElement();
		root.getChild("file").setAttribute("original", string);
		XMLOutputter outputter = new XMLOutputter();
		outputter.preserveSpace(true);
		try (FileOutputStream out = new FileOutputStream(xliff)) {
			outputter.output(doc, out);
		}
	}

	private static String checkConref(String source)
			throws SkipException, IOException, SAXException, ParserConfigurationException {
		Document doc = builder.build(source);
		Element root = doc.getRootElement();
		if (root.getAttributeValue("translate", "yes").equalsIgnoreCase("no")) {
			throw new SkipException("Untranslatable!");
		}
		hasConref = false;
		fixConref(root, source, doc);
		if (hasConref) {
			Charset encoding = EncodingResolver.getEncoding(source, FileFormats.XML);
			File temp = File.createTempFile("temp", ".dita");
			temp.deleteOnExit();
			XMLOutputter outputter = new XMLOutputter();
			outputter.setEncoding(encoding);
			outputter.preserveSpace(true);
			try (FileOutputStream out = new FileOutputStream(temp)) {
				outputter.output(doc, out);
			}
			return temp.getAbsolutePath();
		}
		return source;
	}

	private static void fixConref(Element e, String source, Document doc)
			throws IOException, SAXException, ParserConfigurationException {
		String conref = e.getAttributeValue("conref", "");
		String conaction = e.getAttributeValue("conaction", "");
		if (!conaction.equals("")) { // it's a conref push
			conref = "";
		}
		if (!conref.equals("")) {
			if (conref.indexOf('#') != -1) {
				String file = conref.substring(0, conref.indexOf('#'));
				if (file.length() == 0) {
					file = source;
				} else {
					File f = new File(file);
					if (!f.isAbsolute()) {
						file = Utils.getAbsolutePath(source, file);
					}
				}
				String id = conref.substring(conref.indexOf('#') + 1);
				Element ref = getReferenced(file, id);
				if (ref != null) {
					hasConref = true;
					if (e.getChildren().isEmpty()) {
						e.setAttribute("status", "removeContent");
					}
					e.setContent(ref.getContent());
					if (ref.getAttributeValue("translate", "yes").equals("no")
							&& e.getAttributeValue("translate", "yes").equals("yes")) {
						e.setAttribute("translate", "no");
						e.setAttribute("removeTranslate", "yes");
					}
					List<Element> children = e.getChildren();
					Iterator<Element> its = children.iterator();
					while (its.hasNext()) {
						fixConref(its.next(), file, doc);
					}
				}
			}
		} else {
			List<Element> children = e.getChildren();
			Iterator<Element> it = children.iterator();
			while (it.hasNext()) {
				fixConref(it.next(), source, doc);
			}
		}
	}

	private static Element getReferenced(String file, String id)
			throws SAXException, IOException, ParserConfigurationException {
		Document doc = builder.build(file);
		Element root = doc.getRootElement();
		String topicId = root.getAttributeValue("id", "");
		if (topicId.equals(id)) {
			return root;
		}
		return locate(root, topicId, id);
	}

	private static Element locate(Element root, String topicId, String id) {
		String current = root.getAttributeValue("id", "");
		if (id.equals(topicId + "/" + current)) {
			return root;
		}
		List<Element> children = root.getChildren();
		Iterator<Element> it = children.iterator();
		while (it.hasNext()) {
			Element child = it.next();
			Element result = locate(child, topicId, id);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	private static void addFile(String xliff, String mapFile)
			throws SAXException, IOException, ParserConfigurationException {
		Document doc = builder.build(xliff);
		Element root = doc.getRootElement();
		Element file = root.getChild("file");
		Element newFile = new Element("file");
		newFile.clone(file);
		newFile.setAttribute("datatype", "x-ditamap");
		String old = file.getAttributeValue("original");
		newFile.setAttribute("original", Utils.makeRelativePath(mapFile, old));
		List<PI> encoding = root.getPI("encoding");
		if (!encoding.isEmpty()) {
			newFile.addContent(encoding.get(0));
		}
		mergedRoot.addContent(newFile);
		File f = new File(xliff);
		Files.delete(Paths.get(f.toURI()));
	}

	private static void parseDitaVal(String ditaval, Catalog catalog)
			throws SAXException, IOException, ParserConfigurationException {
		SAXBuilder bder = new SAXBuilder();
		bder.setEntityResolver(catalog);
		Document doc = bder.build(ditaval);
		Element root = doc.getRootElement();
		if (root.getName().equals("val")) {
			List<Element> props = root.getChildren("prop");
			Iterator<Element> it = props.iterator();
			excludeTable = new Hashtable<>();
			includeTable = new Hashtable<>();
			while (it.hasNext()) {
				Element prop = it.next();
				if (prop.getAttributeValue("action", "include").equals("exclude")) {
					String att = prop.getAttributeValue("att", "");
					String val = prop.getAttributeValue("val", "");
					if (!att.equals("")) {
						Set<String> set = excludeTable.get(att);
						if (set == null) {
							set = new HashSet<>();
						}
						if (!val.equals("")) {
							set.add(val);
						}
						excludeTable.put(att, set);
					}
				}
				if (prop.getAttributeValue("action", "include").equals("include")) {
					String att = prop.getAttributeValue("att", "");
					String val = prop.getAttributeValue("val", "");
					if (!att.equals("") && !val.equals("")) {
						Set<String> set = includeTable.get(att);
						if (set == null) {
							set = new HashSet<>();
						}
						set.add(val);
						includeTable.put(att, set);
					}
				}
			}
			filterAttributes = true;
		}
	}

	private static boolean filterOut(Element e) {
		if (filterAttributes) {
			List<Attribute> atts = e.getAttributes();
			Iterator<Attribute> it = atts.iterator();
			while (it.hasNext()) {
				Attribute a = it.next();
				if (excludeTable.containsKey(a.getName())) {
					Set<String> forbidden = excludeTable.get(a.getName());
					if (forbidden.isEmpty() && includeTable.containsKey(a.getName())) {
						Set<String> allowed = includeTable.get(a.getName());
						StringTokenizer tokenizer = new StringTokenizer(a.getValue());
						while (tokenizer.hasMoreTokens()) {
							String token = tokenizer.nextToken();
							if (allowed.contains(token)) {
								return false;
							}
						}
					}
					StringTokenizer tokenizer = new StringTokenizer(a.getValue());
					Vector<String> tokens = new Vector<>();
					while (tokenizer.hasMoreTokens()) {
						tokens.add(tokenizer.nextToken());
					}
					if (forbidden.containsAll(tokens)) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
