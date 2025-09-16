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
package com.maxprograms.converters.ditamap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.ILogger;
import com.maxprograms.converters.Utils;
import com.maxprograms.converters.xml.Xml2Xliff;
import com.maxprograms.xml.Attribute;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.CatalogBuilder;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.Indenter;
import com.maxprograms.xml.PI;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.SilentErrorHandler;
import com.maxprograms.xml.XMLNode;
import com.maxprograms.xml.XMLOutputter;

public class DitaMap2Xliff {

	private static Logger logger = System.getLogger(DitaMap2Xliff.class.getName());

	private static Element mergedRoot;
	private static boolean hasConref;
	private static boolean hasXref;
	private static boolean hasConKeyRef;
	private static Scope rootScope;
	private static Map<String, Set<String>> excludeTable;
	private static Map<String, Set<String>> includeTable;
	private static boolean filterAttributes;
	private static boolean elementsExcluded;
	private static List<String> skipped;
	private static ILogger dataLogger;
	private static List<String> issues;
	private static Map<String, List<String>> images;

	private static Document singleDoc;

	private DitaMap2Xliff() {
		// do not instantiate this class
		// use run method instead
	}

	public static List<String> run(Map<String, String> params) {
		List<String> result = new ArrayList<>();
		issues = new ArrayList<>();
		try {
			String xliffFile = params.get("xliff");
			String skeleton = params.get("skeleton");
			Catalog catalog = CatalogBuilder.getCatalog(params.get("catalog"));
			String mapFile = params.get("source");

			DitaParser parser = new DitaParser();
			if (dataLogger != null) {
				if (dataLogger.isCancelled()) {
					result.add("1");
					result.add(Constants.CANCELLED);
					return result;
				}
				dataLogger.setStage(Messages.getString("DitaMap2Xliff.01"));
				DitaParser.setDataLogger(dataLogger);
			}
			List<String> filesMap = parser.run(params, catalog);
			issues.addAll(parser.getIssues());
			rootScope = parser.getScope();
			images = parser.getImages();

			List<String> xliffs = new ArrayList<>();
			List<String> skels = new ArrayList<>();

			String ditaval = params.get("ditaval");
			if (ditaval != null) {
				parseDitaVal(ditaval, catalog);
			}
			skipped = new ArrayList<>();
			skipped.addAll(parser.getSkipped());
			if ("yes".equals(params.get("ignoresvg"))) {
				filesMap.removeAll(parser.getTranslatableSVG());
			}
			if (dataLogger != null) {
				if (dataLogger.isCancelled()) {
					result.add("1");
					result.add(Constants.CANCELLED);
					return result;
				}
				dataLogger.setStage(Messages.getString("DitaMap2Xliff.02"));
			}
			// start processing single file
			SAXBuilder builder = new SAXBuilder();
			builder.setEntityResolver(catalog);
			builder.preserveCustomAttributes(true);
			builder.setErrorHandler(new SilentErrorHandler());
			for (int i = 0; i < filesMap.size(); i++) {
				String file = filesMap.get(i);
				if (dataLogger != null) {
					if (dataLogger.isCancelled()) {
						result.add("1");
						result.add(Constants.CANCELLED);
						return result;
					}
					dataLogger.log(new File(file).getName());
				}
				singleDoc = builder.build(file);
				String source = "";
				try {
					source = checkConref(file, catalog);
				} catch (Exception skip) {
					// skip untranslatable files
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
					source = checkConKeyRef(source, catalog);
				} catch (Exception sax) {
					// skip directly referenced images
					continue;
				}
				try {
					source = checkXref(source, catalog);
				} catch (Exception sax) {
					// skip directly referenced images
					continue;
				}

				File sklParent = new File(skeleton).getParentFile();
				if (Files.notExists(sklParent.toPath())) {
					Files.createDirectories(sklParent.toPath());
				}
				File skl = File.createTempFile("dita", ".skl", sklParent);
				skels.add(skl.getAbsolutePath());
				File xlf = File.createTempFile("dita", ".xlf", new File(skeleton).getParentFile());
				xlf.deleteOnExit();

				Charset encoding = singleDoc.getEncoding();
				Map<String, String> params2 = new HashMap<>();
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
				params2.put("xmlfilter", params.get("xmlfilter"));
				params2.put("ignoretc", params.get("ignoretc"));
				String tComments = params.get("translateComments");
				if (tComments != null) {
					params2.put("translateComments", tComments);
				}
				List<String> res = Xml2Xliff.run(params2);
				if (!Constants.SUCCESS.equals(res.get(0))) {
					if (res.size() == 3 && "EMPTY".equals(res.get(2))) {
						// this DITA file does not contain text
						skipped.add(filesMap.get(i));
						continue;
					}
					MessageFormat mf = new MessageFormat(Messages.getString("DitaMap2Xliff.03"));
					String issue = mf.format(new String[] { source });
					logger.log(Level.ERROR, issue);
					issues.add(issue);
					return res;
				}
				xliffs.add(xlf.getAbsolutePath());
				if (!source.equals(filesMap.get(i))) {
					// original has conref
					fixSource(xlf.getAbsolutePath(), filesMap.get(i), catalog);
				}
				// end processing single file
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
			List<String> ignored = parser.getIgnored();

			XMLOutputter outputter = new XMLOutputter();

			if (skipped.contains(mapFile)) {
				if (dataLogger != null) {
					if (dataLogger.isCancelled()) {
						result.add("1");
						result.add(Constants.CANCELLED);
						return result;
					}
					dataLogger.setStage(Messages.getString("DitaMap2Xliff.04"));
				}
				builder = new SAXBuilder();
				builder.setEntityResolver(catalog);
				builder.preserveCustomAttributes(true);
				Document doc = builder.build(xliffs.get(0));
				Element root = doc.getRootElement();
				Element file = root.getChild("file");
				String original = file.getAttributeValue("original");
				for (int i = 0; i < skipped.size(); i++) {
					JSONObject json = new JSONObject();
					String f = skipped.get(i);
					if (dataLogger != null) {
						if (dataLogger.isCancelled()) {
							result.add("1");
							result.add(Constants.CANCELLED);
							return result;
						}
						dataLogger.log(new File(f).getName());
					}
					json.put("file", Utils.getRelativePath(original, f));
					json.put("base64", Utils.encodeFromFile(f));
					PI pi = new PI("skipped", json.toString());
					file.addContent(pi);
				}
				for (int i = 0; i < ignored.size(); i++) {
					JSONObject json = new JSONObject();
					String f = ignored.get(i);
					if (dataLogger != null) {
						if (dataLogger.isCancelled()) {
							result.add("1");
							result.add(Constants.CANCELLED);
							return result;
						}
						dataLogger.log(new File(f).getName());
					}
					json.put("file", Utils.getRelativePath(original, f));
					json.put("base64", Utils.encodeFromFile(f));
					PI pi = new PI("skipped", json.toString());
					file.addContent(pi);
				}
				try (FileOutputStream output = new FileOutputStream(xliffs.get(0))) {
					outputter.output(doc, output);
				}
			}

			for (int i = 0; i < xliffs.size(); i++) {
				mergedRoot.addContent("\n");
				addFile(xliffs.get(i), mapFile, catalog, ignored);
			}

			// output final XLIFF

			Indenter.indent(mergedRoot, 2);
			outputter.preserveSpace(true);
			File xliff = new File(xliffFile);
			File xliffParent = xliff.getParentFile();
			if (xliffParent == null) {
				xliffParent = new File(System.getProperty("user.dir"));
			}
			if (!xliffParent.exists()) {
				Files.createDirectories(xliffParent.toPath());
			}
			try (FileOutputStream output = new FileOutputStream(xliff)) {
				outputter.output(merged, output);
			}
			result.add(Constants.SUCCESS);
		} catch (Exception e) {
			logger.log(Level.ERROR, Messages.getString("DitaMap2Xliff.05"), e);
			result.add(Constants.ERROR);
			result.add(e.getMessage());
		}
		return result;
	}

	private static String checkXref(String source, Catalog catalog) throws IOException, SkipException {
		Element root = singleDoc.getRootElement();
		if (root.getAttributeValue("translate", "yes").equalsIgnoreCase("no")) {
			throw new SkipException("Untranslatable!");
		}
		hasXref = false;
		fixXref(root, source, singleDoc, catalog);
		if (hasXref) {
			Charset encoding = singleDoc.getEncoding();
			File temp = File.createTempFile("temp", ".dita");
			temp.deleteOnExit();
			XMLOutputter outputter = new XMLOutputter();
			outputter.setEncoding(encoding);
			outputter.preserveSpace(true);
			try (FileOutputStream out = new FileOutputStream(temp)) {
				outputter.output(singleDoc, out);
			}
			return temp.getAbsolutePath();
		}
		return source;
	}

	private static void fixXref(Element root, String source, Document doc, Catalog catalog) {
		if (DitaParser.ditaClass(root, "topic/xref") && !"external".equals(root.getAttributeValue("scope"))
				&& ("dita".equals(root.getAttributeValue("format","dita"))
						|| "ditamap".equals(root.getAttributeValue("format","dita")))) {
			List<XMLNode> content = root.getContent();
			if (content.isEmpty()) {
				String href = root.getAttributeValue("href");
				if (!href.isEmpty()) {
					href = URLDecoder.decode(href, StandardCharsets.UTF_8);
					try {
						String file = href;
						String id = "";
						if (href.indexOf('#') != -1) {
							file = href.substring(0, href.indexOf('#'));
							if (file.isEmpty()) {
								file = source;
							} else {
								File f = new File(file);
								if (!f.isAbsolute()) {
									file = Utils.getAbsolutePath(source, file);
								}
							}
							id = href.substring(href.indexOf('#') + 1);
						} else {
							file = Utils.getAbsolutePath(source, file);
						}
						String title = getTitle(file, id, catalog);
						if (title != null) {
							root.setText(title);
							hasXref = true;
							root.setAttribute("status", "removeContent");
							if (!root.getAttributeValue("translate", "yes").equals("no")) {
								root.setAttribute("translate", "no");
								root.setAttribute("removeTranslate", "yes");
							}
						}
					} catch (SAXException | ParserConfigurationException | IOException ex) {
						// do nothing
					}
				}
			}
		} else {
			List<Element> children = root.getChildren();
			Iterator<Element> it = children.iterator();
			while (it.hasNext()) {
				fixXref(it.next(), source, doc, catalog);
			}
		}
	}

	private static String getTitle(String file, String id, Catalog catalog)
			throws SAXException, IOException, ParserConfigurationException {
		Element referenced = getReferenced(file, id, catalog);
		if (referenced != null) {
			List<Element> children = referenced.getChildren();
			Iterator<Element> it = children.iterator();
			while (it.hasNext()) {
				Element child = it.next();
				if (DitaParser.ditaClass(child, "topic/title")) {
					return child.getTextNormalize();
				}
			}
		}
		return null;
	}

	private static String removeFiltered(String source) throws IOException {
		Element root = singleDoc.getRootElement();
		elementsExcluded = false;
		recurseExcluding(root);
		if (elementsExcluded) {
			Charset encoding = singleDoc.getEncoding();
			File temp = File.createTempFile("temp", ".dita");
			temp.deleteOnExit();
			XMLOutputter outputter = new XMLOutputter();
			outputter.setEncoding(encoding);
			outputter.preserveSpace(true);
			try (FileOutputStream out = new FileOutputStream(temp)) {
				outputter.output(singleDoc, out);
			}
			return temp.getAbsolutePath();
		}
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

	private static String checkConKeyRef(String source, Catalog catalog)
			throws IOException, SAXException, ParserConfigurationException {
		Element root = singleDoc.getRootElement();
		hasConKeyRef = false;
		fixConKeyRef(root, source, singleDoc, catalog);
		if (hasConKeyRef) {
			Charset encoding = singleDoc.getEncoding();
			File temp = File.createTempFile("temp", ".dita");
			temp.deleteOnExit();
			XMLOutputter outputter = new XMLOutputter();
			outputter.setEncoding(encoding);
			outputter.preserveSpace(true);
			try (FileOutputStream out = new FileOutputStream(temp)) {
				outputter.output(singleDoc, out);
			}
			return temp.getAbsolutePath();
		}
		return source;
	}

	private static void fixConKeyRef(Element e, String source, Document doc, Catalog catalog)
			throws IOException, SAXException, ParserConfigurationException {
		String conkeyref = e.getAttributeValue("conkeyref");
		String conaction = e.getAttributeValue("conaction");
		String keyref = e.getAttributeValue("keyref");
		if (!conaction.isEmpty()) { // it's a conref push
			conkeyref = "";
		}
		if (!conkeyref.isEmpty()) {
			if (conkeyref.indexOf('/') != -1) {
				String key = conkeyref.substring(0, conkeyref.indexOf('/'));
				String id = conkeyref.substring(conkeyref.indexOf('/') + 1);
				Key k = rootScope.getKey(key);
				String file = k.getHref();
				if (file == null) {
					MessageFormat mf = new MessageFormat(Messages.getString("DitaMap2Xliff.06"));
					String issue = mf.format(new String[] { conkeyref });
					logger.log(Level.WARNING, issue);
					issues.add(issue);
					return;
				}
				Element ref = getConKeyReferenced(file, id, catalog);
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
						fixConKeyRef(its.next(), file, doc, catalog);
					}
				} else {
					MessageFormat mf = new MessageFormat(Messages.getString("DitaMap2Xliff.07"));
					String issue = mf.format(new String[] { conkeyref, id, file });
					logger.log(Level.WARNING, issue);
					issues.add(issue);
				}
			} else {
				MessageFormat mf = new MessageFormat(Messages.getString("DitaMap2Xliff.08"));
				String issue = mf.format(new String[] { conkeyref });
				logger.log(Level.WARNING, issue);
				issues.add(issue);
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
						SAXBuilder builder = new SAXBuilder();
						builder.setEntityResolver(catalog);
						builder.preserveCustomAttributes(true);
						builder.setErrorHandler(new SilentErrorHandler());
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
								if (!content.isEmpty()) {
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
							fixConKeyRef(it.next(), source, doc, catalog);
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
				fixConKeyRef(it.next(), source, doc, catalog);
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
		return new ArrayList<>();
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

	private static Element getConKeyReferenced(String file, String id, Catalog catalog)
			throws SAXException, IOException, ParserConfigurationException {
		SAXBuilder builder = new SAXBuilder();
		builder.setEntityResolver(catalog);
		builder.preserveCustomAttributes(true);
		builder.setErrorHandler(new SilentErrorHandler());
		Document doc = builder.build(file);
		Element root = doc.getRootElement();
		return locateReferenced(root, id);
	}

	private static Element locateReferenced(Element root, String id) {
		String current = root.getAttributeValue("id");
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

	private static void fixSource(String xliff, String string, Catalog catalog)
			throws SAXException, IOException, ParserConfigurationException {
		SAXBuilder builder = new SAXBuilder();
		builder.setEntityResolver(catalog);
		builder.preserveCustomAttributes(true);
		Document doc = builder.build(xliff);
		Element root = doc.getRootElement();
		root.getChild("file").setAttribute("original", string);
		XMLOutputter outputter = new XMLOutputter();
		outputter.preserveSpace(true);
		try (FileOutputStream out = new FileOutputStream(xliff)) {
			outputter.output(doc, out);
		}
	}

	private static String checkConref(String source, Catalog catalog)
			throws SkipException, IOException, SAXException, ParserConfigurationException {
		Element root = singleDoc.getRootElement();
		if (root.getAttributeValue("translate", "yes").equalsIgnoreCase("no")) {
			throw new SkipException("Untranslatable!");
		}
		hasConref = false;
		fixConref(root, source, singleDoc, catalog);
		if (hasConref) {
			Charset encoding = singleDoc.getEncoding();
			File temp = File.createTempFile("temp", ".dita");
			temp.deleteOnExit();
			XMLOutputter outputter = new XMLOutputter();
			outputter.setEncoding(encoding);
			outputter.preserveSpace(true);
			try (FileOutputStream out = new FileOutputStream(temp)) {
				outputter.output(singleDoc, out);
			}
			return temp.getAbsolutePath();
		}
		return source;
	}

	private static void fixConref(Element e, String source, Document doc, Catalog catalog)
			throws IOException, SAXException, ParserConfigurationException {
		String conref = e.getAttributeValue("conref");
		String conaction = e.getAttributeValue("conaction");
		if (!conaction.isEmpty()) { // it's a conref push
			conref = "";
		}
		if (!conref.isEmpty() && !"#".equals(conref)) {
			conref = URLDecoder.decode(conref, StandardCharsets.UTF_8);
			if (conref.indexOf('#') != -1) {
				String file = conref.substring(0, conref.indexOf('#'));
				if (file.isEmpty()) {
					file = source;
				} else {
					File f = new File(file);
					if (!f.isAbsolute()) {
						file = Utils.getAbsolutePath(source, file);
					}
				}
				String id = conref.substring(conref.indexOf('#') + 1);
				Element ref = getReferenced(file, id, catalog);
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
						fixConref(its.next(), file, doc, catalog);
					}
				}
			}
		} else {
			List<Element> children = e.getChildren();
			Iterator<Element> it = children.iterator();
			while (it.hasNext()) {
				fixConref(it.next(), source, doc, catalog);
			}
		}
	}

	private static Element getReferenced(String file, String id, Catalog catalog)
			throws SAXException, IOException, ParserConfigurationException {
		SAXBuilder builder = new SAXBuilder();
		builder.setEntityResolver(catalog);
		builder.preserveCustomAttributes(true);
		Document doc = builder.build(file);
		Element root = doc.getRootElement();
		String topicId = root.getAttributeValue("id");
		if (id.isEmpty() || topicId.equals(id)) {
			return root;
		}
		return locate(root, topicId, id);
	}

	private static Element locate(Element root, String topicId, String id) {
		String current = root.getAttributeValue("id");
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

	private static void addFile(String xliff, String mapFile, Catalog catalog, List<String> ignored)
			throws SAXException, IOException, ParserConfigurationException {
		SAXBuilder builder = new SAXBuilder();
		builder.setEntityResolver(catalog);
		builder.preserveCustomAttributes(true);
		Document doc = builder.build(xliff);
		Element root = doc.getRootElement();
		Element file = root.getChild("file");
		Element newFile = new Element("file");
		newFile.clone(file);
		newFile.setAttribute("datatype", "x-ditamap");
		String old = file.getAttributeValue("original");
		if (images.containsKey(old)) {
			List<String> list = images.get(old);
			Encoder encoder = Base64.getMimeEncoder();
			for (int i = 0; i < list.size(); i++) {
				String data = encoder.encodeToString(list.get(i).getBytes(StandardCharsets.UTF_8));
				newFile.addContent(new PI("images", data));
			}
		}
		String relative = Utils.getRelativePath(mapFile, old);
		if (old.equals(mapFile)) {
			// preserve DITA files without text
			// put them in the map file
			if (dataLogger != null) {
				if (dataLogger.isCancelled()) {
					throw new IOException(Constants.CANCELLED);
				}
				dataLogger.setStage(Messages.getString("DitaMap2Xliff.04"));
			}
			for (int i = 0; i < skipped.size(); i++) {
				String f = skipped.get(i);
				if (dataLogger != null) {
					if (dataLogger.isCancelled()) {
						throw new IOException(Constants.CANCELLED);
					}
					dataLogger.log(new File(f).getName());
				}
				JSONObject json = new JSONObject();
				json.put("file", Utils.getRelativePath(mapFile, f));
				json.put("base64", Utils.encodeFromFile(f));
				PI pi = new PI("skipped", json.toString());
				newFile.addContent(pi);
			}
			// add files with translate="no"
			for (int i = 0; i < ignored.size(); i++) {
				String f = ignored.get(i);
				if (dataLogger != null) {
					if (dataLogger.isCancelled()) {
						throw new IOException(Constants.CANCELLED);
					}
					dataLogger.log(new File(f).getName());
				}
				JSONObject json = new JSONObject();
				json.put("file", Utils.getRelativePath(mapFile, f));
				json.put("base64", Utils.encodeFromFile(f));
				PI pi = new PI("skipped", json.toString());
				newFile.addContent(pi);
			}
		}
		newFile.setAttribute("original", relative);
		mergedRoot.addContent(newFile);
		File f = new File(xliff);
		Files.delete(Paths.get(f.toURI()));
	}

	private static void parseDitaVal(String ditaval, Catalog catalog)
			throws SAXException, IOException, ParserConfigurationException {
		SAXBuilder builder = new SAXBuilder();
		builder.setEntityResolver(catalog);
		builder.preserveCustomAttributes(true);
		Document doc = builder.build(ditaval);
		Element root = doc.getRootElement();
		if (root.getName().equals("val")) {
			List<Element> props = root.getChildren("prop");
			Iterator<Element> it = props.iterator();
			excludeTable = new HashMap<>();
			includeTable = new HashMap<>();
			while (it.hasNext()) {
				Element prop = it.next();
				if (prop.getAttributeValue("action", "include").equals("exclude")) {
					String att = prop.getAttributeValue("att");
					String val = prop.getAttributeValue("val");
					if (!att.isEmpty()) {
						Set<String> set = excludeTable.get(att);
						if (set == null) {
							set = new HashSet<>();
						}
						if (!val.isEmpty()) {
							set.add(val);
						}
						excludeTable.put(att, set);
					}
				}
				if (prop.getAttributeValue("action", "include").equals("include")) {
					String att = prop.getAttributeValue("att");
					String val = prop.getAttributeValue("val");
					if (!att.isEmpty() && !val.isEmpty()) {
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
					List<String> tokens = new ArrayList<>();
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

	public static void setDataLogger(ILogger dataLogger) {
		DitaMap2Xliff.dataLogger = dataLogger;
	}

	public static List<String> getIssues() {
		return DitaMap2Xliff.issues;
	}
}
