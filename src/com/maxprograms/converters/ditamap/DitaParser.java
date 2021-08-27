/*******************************************************************************
 * Copyright (c)  Maxprograms.
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
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;

import com.maxprograms.converters.Utils;
import com.maxprograms.xml.Attribute;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.PI;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.TextNode;
import com.maxprograms.xml.XMLNode;

import org.xml.sax.SAXException;

public class DitaParser {

	private static final Logger LOGGER = System.getLogger(DitaParser.class.getName());

	protected class StringArray implements Comparable<StringArray> {
		private String file;
		private String id;

		public StringArray(String file, String id) {
			this.file = file;
			this.id = id;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof StringArray)) {
				return false;
			}
			StringArray a = (StringArray) obj;
			return a.getFile().equals(file) && a.getId().equals(id);
		}

		@Override
		public int compareTo(StringArray o) {
			if (o.getFile().equals(file) && o.getId().equals(id)) {
				return 0;
			}
			return 1;
		}

		public String getFile() {
			return file;
		}

		public String getId() {
			return id;
		}

		@Override
		public int hashCode() {
			return (id + file).hashCode();
		}
	}

	private Set<String> filesMap;
	private SAXBuilder builder;
	private Map<String, Set<String>> excludeTable;
	private Map<String, Set<String>> includeTable;
	private boolean filterAttributes;
	private Scope rootScope;

	private Set<StringArray> searchedConref;
	private Map<Key, Set<String>> usedKeys;
	private Set<String> recursed;
	private Set<String> pendingRecurse;
	private TreeSet<String> topicrefSet;
	private TreeSet<String> topicSet;
	private static TreeSet<String> xrefSet;
	private static TreeSet<String> linkSet;
	private static TreeSet<String> imageSet;

	private boolean containsText;

	protected List<String> run(Map<String, String> params)
			throws IOException, SAXException, ParserConfigurationException, URISyntaxException {
		List<String> result = new ArrayList<>();
		filesMap = new TreeSet<>();
		searchedConref = new TreeSet<>();
		usedKeys = new HashMap<>();
		recursed = new TreeSet<>();
		pendingRecurse = new TreeSet<>();

		String inputFile = params.get("source");
		Catalog catalog = new Catalog(params.get("catalog"));
		String ditaval = params.get("ditaval");

		builder = new SAXBuilder();
		builder.setEntityResolver(catalog);
		Document doc = builder.build(inputFile);
		String id = doc.getRootElement().getAttributeValue("id");
		List<PI> ish = doc.getPI("ish");
		if (!ish.isEmpty() || id.startsWith("GUID-")) {
			SDLFixer.fix(new File(inputFile).getParentFile(), catalog);
			doc = builder.build(inputFile);
		}

		ScopeBuilder sbuilder = new ScopeBuilder();
		rootScope = sbuilder.buildScope(inputFile, ditaval, catalog);

		if (ditaval != null) {
			parseDitaVal(ditaval, catalog);
		}

		filesMap = new TreeSet<>();
		filesMap.add(inputFile);

		Element root = doc.getRootElement();

		recurse(root, inputFile);
		recursed.add(inputFile);

		Iterator<Key> it = usedKeys.keySet().iterator();
		while (it.hasNext()) {
			Key key = it.next();
			String defined = key.getDefined();
			if (!filesMap.contains(defined)) {
				pendingRecurse.add(defined);
			}
			if (!filesMap.contains(key.getHref())) {
				pendingRecurse.add(key.getHref());
			}
		}

		int count = 0;
		do {
			List<String> files = new ArrayList<>();
			files.addAll(pendingRecurse);
			pendingRecurse.clear();
			Iterator<String> st = files.iterator();
			while (st.hasNext()) {
				String file = st.next();
				if (!recursed.contains(file)) {
					try {
						Element e = builder.build(file).getRootElement();
						if ("svg".equals(e.getName())) {
							containsText = false;
							recurseSVG(e);
							if (!containsText) {
								continue;
							}
						}
						recurse(e, file);
						recursed.add(file);
						filesMap.add(file);
					} catch (IOException | SAXException | ParserConfigurationException ex) {
						// ignore images
					}
				}
			}
			count++;
		} while (!pendingRecurse.isEmpty() && count < 4);
		result.addAll(filesMap);
		return result;
	}

	private void recurseSVG(Element e) {
		if ("text".equals(e.getName()) && !svgText(e).isEmpty()) {
			containsText = true;
			return;
		}
		if ("title".equals(e.getName()) && !svgText(e).isEmpty()) {
			containsText = true;
			return;
		}
		if ("desc".equals(e.getName()) && !svgText(e).isEmpty()) {
			containsText = true;
			return;
		}
		List<Element> children = e.getChildren();
		Iterator<Element> it = children.iterator();
		while (it.hasNext()) {
			recurseSVG(it.next());
		}
	}

	private String svgText(Element e) {
		StringBuilder sb = new StringBuilder();
		List<XMLNode> content = e.getContent();
		Iterator<XMLNode> it = content.iterator();
		while (it.hasNext()) {
			XMLNode node = it.next();
			if (node.getNodeType() == XMLNode.TEXT_NODE) {
				TextNode t = (TextNode) node;
				sb.append(t.getText());
			}
			if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
				sb.append(svgText((Element) node));
			}
		}
		return sb.toString();
	}

	private void recurse(Element e, String parentFile) throws IOException, SAXException, ParserConfigurationException {
		if (filterOut(e)) {
			return;
		}
		if (e.getAttributeValue("translate", "yes").equals("no")) {
			return;
		}
		if (ditaClass(e, "map/topicref") || isTopicref(e.getName())) {
			String href = "";
			String keyref = e.getAttributeValue("keyref");
			if (!keyref.isEmpty()) {
				Key k = rootScope.getKey(keyref);
				if (k != null) {
					if (!usedKeys.containsKey(k)) {
						usedKeys.put(k, new TreeSet<>());
					}
					usedKeys.get(k).add(parentFile);
					href = k.getHref();
				} else {
					href = e.getAttributeValue("href");
					if (!href.isEmpty()) {
						href = URLDecoder.decode(href, StandardCharsets.UTF_8);
						// use href as fallback mechanism
						String format = e.getAttributeValue("format", "dita");
						if (format.startsWith("dita")) {
							href = Utils.getAbsolutePath(parentFile, href);
						}
					}
				}
				if (k != null && k.getTopicmeta() != null && e.getContent().isEmpty()) {
					e.addContent(k.getTopicmeta());
					return;
				}
			} else {
				href = e.getAttributeValue("href");
				String format = e.getAttributeValue("format", "dita");
				if (!href.isEmpty() && format.startsWith("dita")) {
					href = URLDecoder.decode(href, StandardCharsets.UTF_8);
					href = Utils.getAbsolutePath(parentFile, href);
				}
			}
			if (href.startsWith("http:") || href.startsWith("https:") || href.startsWith("ftp:")
					|| href.startsWith("mailto:")) {
				e.setAttribute("scope", "external");
				return;
			}
			String format = e.getAttributeValue("format", "dita");
			if (!href.isEmpty() && !format.startsWith("dita")) {
				return;
			}
			if (!href.isEmpty() && !href.equals(parentFile)) {
				href = URLDecoder.decode(href, StandardCharsets.UTF_8);
				try {
					File file = new File(href);
					if (file.exists()) {
						Element root = builder.build(href).getRootElement();
						if (root.getAttributeValue("translate", "yes").equals("yes")) {
							if (!recursed.contains(href)) {
								recurse(root, href);
								filesMap.add(href);
								recursed.add(href);
							}
						} else {
							MessageFormat mf = new MessageFormat("Ignored Referenced File: {0}");
							LOGGER.log(Level.WARNING, mf.format(new Object[] { href }));
						}
					} else {
						MessageFormat mf = new MessageFormat("Referenced file {0} doesn't exist");
						LOGGER.log(Level.WARNING, mf.format(new Object[] { href }));
					}
				} catch (SAXException ex) {
					String lower = href.toLowerCase();
					if (!(lower.endsWith(".eps") || lower.endsWith(".png") || lower.endsWith(".jpeg")
							|| lower.endsWith(".jpg") || lower.endsWith(".pdf") || lower.endsWith(".tiff")
							|| lower.endsWith(".tif") || lower.endsWith(".mov") || lower.endsWith(".mp4")
							|| lower.endsWith(".m4v") || lower.endsWith(".flv") || lower.endsWith(".fl4v")
							|| lower.endsWith(".avi") || lower.endsWith(".mpg") || lower.endsWith(".mpeg")
							|| lower.endsWith(".wmv") || lower.endsWith(".asf"))) {
						throw new SAXException(ex.getMessage() + "\n File: '" + href + "'");
					}
					LOGGER.log(Level.WARNING, "Error recursing", ex);
				}
			}
		} else {
			String conref = e.getAttributeValue("conref");
			if (!conref.isEmpty()) {
				conref = URLDecoder.decode(conref, StandardCharsets.UTF_8);
				if (conref.indexOf('#') != -1) {
					String file = conref.substring(0, conref.indexOf('#'));
					if (file.length() == 0) {
						file = parentFile;
					} else {
						file = Utils.getAbsolutePath(parentFile, file);
					}
					String id = conref.substring(conref.indexOf('#') + 1);
					try {
						Element ref = getReferenced(file, id);
						if (ref != null) {
							e.setContent(ref.getContent());
							List<Element> children = e.getChildren();
							Iterator<Element> ie = children.iterator();
							while (ie.hasNext()) {
								recurse(ie.next(), file);
							}
						} else {
							MessageFormat mf = new MessageFormat("@conref not found:  \"{0}\" in file {1}");
							LOGGER.log(Level.WARNING, mf.format(new Object[] { conref, parentFile }));
						}
					} catch (Exception ex) {
						MessageFormat mf = new MessageFormat("Broken @conref \"{0}\" in file {1}");
						LOGGER.log(Level.WARNING, mf.format(new Object[] { conref, parentFile }));
					}
					return;
				}
				LOGGER.log(Level.WARNING, "@conref without fragment identifier: " + conref);
			}

			String conkeyref = e.getAttributeValue("conkeyref");
			String conaction = e.getAttributeValue("conaction");
			if (!conaction.isEmpty()) { // it's a conref push
				conkeyref = "";
			}
			if (!conkeyref.isEmpty()) {
				String key = conkeyref.substring(0, conkeyref.indexOf('/'));
				String id = conkeyref.substring(conkeyref.indexOf('/') + 1);
				Key k = rootScope.getKey(key);
				if (k == null) {
					MessageFormat mf = new MessageFormat("Key not defined for @conkeyref: \"{0}\".");
					LOGGER.log(Level.WARNING, mf.format(new Object[] { conkeyref }));
					return;
				}
				if (!usedKeys.containsKey(k)) {
					usedKeys.put(k, new TreeSet<>());
				}
				usedKeys.get(k).add(parentFile);
				String file = k.getHref();
				if (file == null) {
					MessageFormat mf = new MessageFormat("Key not defined for @conkeyref: \"{0}\".");
					LOGGER.log(Level.WARNING, mf.format(new Object[] { conkeyref }));
					return;
				}

				Element ref = getConKeyReferenced(file, id);
				if (ref != null) {
					e.setContent(ref.getContent());
					List<Element> children = e.getChildren();
					Iterator<Element> ie = children.iterator();
					while (ie.hasNext()) {
						recurse(ie.next(), file);
					}
					return;
				}
				MessageFormat mf = new MessageFormat("Broken @conkeyref \"{0}\" in file {1}");
				LOGGER.log(Level.WARNING, mf.format(new Object[] { conkeyref, parentFile }));
				return;
			}

			String keyref = e.getAttributeValue("keyref");
			if (!keyref.isEmpty()) {
				if (keyref.indexOf('/') == -1) {
					Key k = rootScope.getKey(keyref);
					if (k != null) {
						if (!usedKeys.containsKey(k)) {
							usedKeys.put(k, new TreeSet<>());
						}
						usedKeys.get(k).add(parentFile);
						if (k.getTopicmeta() != null && e.getContent().isEmpty()) {

							// empty element that reuses from <topicmeta>
							Element matched = getMatched(e.getName(), k.getTopicmeta());
							if (matched != null) {
								e.setContent(matched.getContent());
								return;
							}
							if (ditaClass(e, "topic/image") || isImage(e.getName())) {
								Element keyword = getMatched("keyword", k.getTopicmeta());
								if (keyword != null) {
									Element alt = new Element("alt");
									alt.setContent(keyword.getContent());
									e.addContent(alt);
									return;
								}
							}
							if (ditaClass(e, "topic/xref") || ditaClass(e, "topic/link") || isXref(e.getName())
									|| isLink(e.getName())) {
								Element keyword = getMatched("keyword", k.getTopicmeta());
								if (keyword != null) {
									Element alt = new Element("linktext");
									alt.setContent(keyword.getContent());
									e.addContent(alt);
									return;
								}
							}
							Element keyword = getMatched("keyword", k.getTopicmeta());
							if (keyword != null) {
								e.addContent(keyword);
							}
							return;
						}
						pendingRecurse.add(k.getHref());
						return;
					}
				} else {

					// behaves like a conkeyref
					// locate an element in the file referenced by the key

					String key = keyref.substring(0, keyref.indexOf('/'));
					String id = keyref.substring(keyref.indexOf('/') + 1);
					Key k = rootScope.getKey(key);
					if (k != null) {
						if (!usedKeys.containsKey(k)) {
							usedKeys.put(k, new TreeSet<>());
						}
						usedKeys.get(k).add(parentFile);
						filesMap.add(k.getHref());
						Element ref = getConKeyReferenced(k.getHref(), id);
						if (ref != null) {
							e.setContent(ref.getContent());
							List<Element> children = e.getChildren();
							Iterator<Element> ie = children.iterator();
							while (ie.hasNext()) {
								recurse(ie.next(), k.getHref());
							}
							return;
						}
					}
				}
				MessageFormat mf = new MessageFormat("Undefined key for @keyref \"{0}\" in file {1}");
				LOGGER.log(Level.WARNING, mf.format(new Object[] { keyref, parentFile }));
			}

			String href = e.getAttributeValue("href");
			if (!href.isEmpty() && (ditaClass(e, "topic/image") || isImage(e.getName()))
					&& !"external".equals(e.getAttributeValue("scope"))) {
				// check for SVG
				try {
					href = URLDecoder.decode(href, StandardCharsets.UTF_8);
					String path = Utils.getAbsolutePath(parentFile, href);
					File f = new File(path);
					Element svg = builder.build(f).getRootElement();
					if ("svg".equals(svg.getName()) && hasText(svg)) {
						filesMap.add(path);
					}
				} catch (Exception ex) {
					// ignore
				}
			}
		}
		List<Element> children = e.getChildren();
		Iterator<Element> it = children.iterator();
		while (it.hasNext()) {
			recurse(it.next(), parentFile);
		}
	}

	private static boolean hasText(Element svg) {
		String name = svg.getName();
		if ("text".equals(name) && !svg.getText().isEmpty()) {
			return true;
		}
		if ("title".equals(name) && !svg.getText().isEmpty()) {
			return true;
		}
		if ("desc".equals(name) && !svg.getText().isEmpty()) {
			return true;
		}
		List<Element> children = svg.getChildren();
		Iterator<Element> it = children.iterator();
		while (it.hasNext()) {
			if (hasText(it.next())) {
				return true;
			}
		}
		return false;
	}

	public static boolean isImage(String name) {
		if (imageSet == null) {
			imageSet = new TreeSet<>();
			imageSet.add("image");
			imageSet.add("glossSymbol");
			imageSet.add("hazardsymbol");
		}
		return imageSet.contains(name);
	}

	public static boolean isLink(String name) {
		if (linkSet == null) {
			linkSet = new TreeSet<>();
			linkSet.add("link");
		}
		return linkSet.contains(name);
	}

	public static boolean isXref(String name) {
		if (xrefSet == null) {
			xrefSet = new TreeSet<>();
			xrefSet.add("xref");
			xrefSet.add("glossAlternateFor");
			xrefSet.add("coderef");
			xrefSet.add("fragref");
			xrefSet.add("synnoteref");
			xrefSet.add("mathmlref");
			xrefSet.add("svgref");
		}
		return xrefSet.contains(name);
	}

	private boolean isTopicref(String name) {
		if (topicrefSet == null) {
			topicrefSet = new TreeSet<>();
			topicrefSet.add("topicref");
			topicrefSet.add("abbrevlist");
			topicrefSet.add("amendments");
			topicrefSet.add("appendix");
			topicrefSet.add("backmatter");
			topicrefSet.add("bibliolist");
			topicrefSet.add("bookabstract");
			topicrefSet.add("booklist");
			topicrefSet.add("booklists");
			topicrefSet.add("chapter");
			topicrefSet.add("colophon");
			topicrefSet.add("dedication");
			topicrefSet.add("draftintro");
			topicrefSet.add("figurelist");
			topicrefSet.add("frontmatter");
			topicrefSet.add("glossarylist");
			topicrefSet.add("indexlist");
			topicrefSet.add("notices");
			topicrefSet.add("part");
			topicrefSet.add("preface");
			topicrefSet.add("tablelist");
			topicrefSet.add("toc");
			topicrefSet.add("trademarklist");
			topicrefSet.add("anchorref");
			topicrefSet.add("keydef");
			topicrefSet.add("mapref");
			topicrefSet.add("topicgroup");
			topicrefSet.add("topichead");
			topicrefSet.add("topicset");
			topicrefSet.add("topicsetref");
			topicrefSet.add("ditavalref");
			topicrefSet.add("glossref");
			topicrefSet.add("subjectref");
			topicrefSet.add("topicapply");
			topicrefSet.add("topicsubject");
			topicrefSet.add("defaultSubject");
			topicrefSet.add("enumerationdef");
			topicrefSet.add("hasInstance");
			topicrefSet.add("hasKind");
			topicrefSet.add("hasNarrower");
			topicrefSet.add("hasPart");
			topicrefSet.add("hasRelated");
			topicrefSet.add("relatedSubjects");
			topicrefSet.add("subjectdef");
			topicrefSet.add("subjectHead");
			topicrefSet.add("schemeref");
			topicrefSet.add("learningContentRef");
			topicrefSet.add("learningGroup");
			topicrefSet.add("learningGroupMapRef");
			topicrefSet.add("learningObject");
			topicrefSet.add("learningObjectMapRef");
			topicrefSet.add("learningOverviewRef");
			topicrefSet.add("learningPlanRef");
			topicrefSet.add("learningPostAssessmentRef");
			topicrefSet.add("learningPreAssessmentRef");
			topicrefSet.add("learningSummaryRef");
		}
		return topicrefSet.contains(name);
	}

	protected static Element getMatched(String name, Element topicmeta) {
		if (topicmeta.getName().equals(name)) {
			return topicmeta;
		}
		List<Element> children = topicmeta.getChildren();
		Iterator<Element> it = children.iterator();
		while (it.hasNext()) {
			Element child = it.next();
			if (child.getName().equals(name)) {
				return child;
			}
			Element res = getMatched(name, child);
			if (res != null) {
				return res;
			}
		}
		return null;
	}

	protected static boolean ditaClass(Element e, String string) {
		String cls = e.getAttributeValue("class");
		String[] parts = cls.split("\\s");
		for (int i = 0; i < parts.length; i++) {
			if (parts[i].equals(string)) {
				return true;
			}
		}
		return false;
	}

	private void parseDitaVal(String ditaval, Catalog catalog)
			throws SAXException, IOException, ParserConfigurationException {
		SAXBuilder bder = new SAXBuilder();
		bder.setEntityResolver(catalog);
		Document doc = bder.build(ditaval);
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

	private boolean filterOut(Element e) {
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

	private Element getReferenced(String file, String id)
			throws SAXException, IOException, ParserConfigurationException {
		StringArray array = new StringArray(file, id);
		Document doc = builder.build(file);
		Element root = doc.getRootElement();
		String topicId = root.getAttributeValue("id");
		if (topicId.equals(id)) {
			if (!filesMap.contains(file)) {
				filesMap.add(file);
			}
			if (!searchedConref.contains(array)) {
				searchedConref.add(array);
				recurse(root, file);
				searchedConref.remove(array);
			}
			return root;
		}
		Element result = locate(root, topicId, id);
		if (result != null) {
			if (!filesMap.contains(file)) {
				filesMap.add(file);
			}
			if (!searchedConref.contains(array)) {
				searchedConref.add(array);
				recurse(result, file);
				searchedConref.remove(array);
			}
		}
		return result;
	}

	private Element locate(Element e, String topic, String searched) {
		String topicId = topic;
		String id = e.getAttributeValue("id");
		if (searched.equals(topicId + "/" + id)) {
			return e;
		}
		if (ditaClass(e, "topic/topic") || isTopic(e.getName())) {
			topicId = id;
		}
		List<Element> children = e.getChildren();
		Iterator<Element> it = children.iterator();
		while (it.hasNext()) {
			Element child = it.next();
			Element result = locate(child, topicId, searched);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	private boolean isTopic(String name) {
		if (topicSet == null) {
			topicSet = new TreeSet<>();
			topicSet.add("topic");
			topicSet.add("concept");
			topicSet.add("glossentry");
			topicSet.add("reference");
			topicSet.add("task");
			topicSet.add("troubleshooting");
			topicSet.add("glossgroup");
			topicSet.add("learningAssessment");
			topicSet.add("learningBase");
			topicSet.add("learningContent");
			topicSet.add("learningOverview");
			topicSet.add("learningSummary");
			topicSet.add("learningPlan");
		}
		return topicSet.contains(name);
	}

	protected Element getConKeyReferenced(String file, String id)
			throws SAXException, IOException, ParserConfigurationException {
		Document doc = builder.build(file);
		Element root = doc.getRootElement();
		return locateReferenced(root, id);
	}

	private Element locateReferenced(Element root, String id) {
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

	public Scope getScope() {
		return rootScope;
	}

}
