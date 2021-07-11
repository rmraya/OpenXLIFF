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

package com.maxprograms.validation;

import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import com.maxprograms.languages.RegistryParser;
import com.maxprograms.xml.Attribute;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.XMLNode;

import org.xml.sax.SAXException;

public class Xliff20 {

	private static final String XLIFF_VALIDATION_2_0 = "urn:oasis:names:tc:xliff:validation:2.0";
	private static final String XLIFF_SIZERESTRICTION_2_0 = "urn:oasis:names:tc:xliff:sizerestriction:2.0";
	private static final String XLIFF_RESOURCEDATA_2_0 = "urn:oasis:names:tc:xliff:resourcedata:2.0";
	private static final String XLIFF_MATCHES_2_0 = "urn:oasis:names:tc:xliff:matches:2.0";
	private static final String XLIFF_GLOSSARY_2_0 = "urn:oasis:names:tc:xliff:glossary:2.0";
	private static final String XLIFF_FS_2_0 = "urn:oasis:names:tc:xliff:fs:2.0";
	private static final String XLIFF_CHANGETRACKING_2_0 = "urn:oasis:names:tc:xliff:changetracking:2.0";
	private static final String XLIFF_METADATA_2_0 = "urn:oasis:names:tc:xliff:metadata:2.0";
	private static final String XLIFF_DOCUMENT_2_0 = "urn:oasis:names:tc:xliff:document:2.0";
	private static final String W3_ORG_XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace";

	private static Logger logger = System.getLogger(Xliff20.class.getName());
	private String reason = "";
	private Catalog resolver;

	private RegistryParser registry;

	private Map<String, String> declaredNamespaces;
	private String srcLang;
	private String trgLang;
	private HashSet<String> fileId;
	private HashSet<String> groupId;
	private HashSet<String> unitId;
	private HashSet<String> cantDelete;
	private HashSet<String> sourceId;
	private HashSet<String> dataId;
	private HashSet<String> matchDataId;
	private HashSet<String> matchId;
	private HashSet<String> metaId;
	private HashSet<String> glossId;
	private HashSet<String> noteId;
	private HashSet<String> smId;
	private HashSet<String> orderSet;
	private Map<String, Element> unitSc;
	private int segCount;
	private int maxSegment;
	private boolean inMatch;
	private boolean isReference;
	private boolean inSource;
	private boolean inTarget;
	private String fsPrefix = "fs";

	private List<String> knownPrefixes = Arrays.asList("xlf", "mtc", "gls", "fs", "mda", "res", "ctr", "slr", "val",
			"its", "my");
	private List<String> knownTypes = Arrays.asList("fmt", "ui", "quote", "link", "image", "other");
	private List<String> xlfSubTypes = Arrays.asList( "xlf:lb", "xlf:pb", "xlf:b", "xlf:i", "xlf:u", "xlf:var");
	private List<String> fmtSubTypes = Arrays.asList("xlf:b", "xlf:i", "xlf:u", "xlf:lb", "xlf:pb");

	public Xliff20() throws IOException {
		registry = new RegistryParser();
	}

	public boolean validate(String file, String catalog) {
		try {
			StreamSource source = new StreamSource(new File(file));
			resolver = new Catalog(catalog);
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Source[] schemas = new Source[] { getSource(W3_ORG_XML_NAMESPACE), getSource(XLIFF_DOCUMENT_2_0),
					getSource(XLIFF_METADATA_2_0), getSource(XLIFF_CHANGETRACKING_2_0), getSource(XLIFF_FS_2_0),
					getSource(XLIFF_GLOSSARY_2_0), getSource(XLIFF_MATCHES_2_0), getSource(XLIFF_RESOURCEDATA_2_0),
					getSource(XLIFF_SIZERESTRICTION_2_0), getSource(XLIFF_VALIDATION_2_0) };
			Schema schema = schemaFactory.newSchema(schemas);
			schema.newValidator().validate(source);
			return validateContent(file);
		} catch (SAXException | IOException | ParserConfigurationException | URISyntaxException e) {
			logger.log(Level.ERROR, e);
			reason = e.getMessage();
		}
		return false;
	}

	private boolean validateContent(String file) throws SAXException, IOException, ParserConfigurationException {
		SAXBuilder builder = new SAXBuilder();
		builder.setValidating(true);
		builder.setEntityResolver(resolver);
		Document document = builder.build(file);
		declaredNamespaces = new Hashtable<>();
		return recurse(document.getRootElement());
	}

	private boolean recurse(Element e) {
		String namespace = e.getNamespace();
		if (!namespace.isEmpty()) {

			if (XLIFF_MATCHES_2_0.equals(declaredNamespaces.get(namespace))) {
				// In Matches module

				if ("matches".equals(e.getLocalName())) {
					matchId = new HashSet<>();
				}
				if ("match".equals(e.getLocalName())) {
					String id = e.getAttributeValue("id");
					if (!id.isEmpty()) {
						if (matchId.contains(id)) {
							reason = "Duplicated \"id\" in <mtc:match>";
							return false;
						}
						matchId.add(id);
					}
					inMatch = true;
					matchDataId = new HashSet<>();
					isReference = e.getAttributeValue("reference", "no").equals("yes");
				}
			}

			if (XLIFF_METADATA_2_0.equals(declaredNamespaces.get(namespace))) {
				// In Metadata module

				if ("metadata".equals(e.getLocalName())) {
					metaId = new HashSet<>();
					String id = e.getAttributeValue("id");
					if (!id.isEmpty()) {
						if (metaId.contains(id)) {
							reason = "Duplicated \"id\" in <mda:metadata>";
							return false;
						}
						metaId.add(id);
					}
				}
				if ("metaGroup".equals(e.getLocalName())) {
					String id = e.getAttributeValue("id");
					if (!id.isEmpty()) {
						if (metaId.contains(id)) {
							reason = "Duplicated \"id\" in <mda:metaGroup>";
							return false;
						}
						metaId.add(id);
					}
				}
			}

			if (XLIFF_GLOSSARY_2_0.equals(declaredNamespaces.get(namespace))) {
				// In Glossary module

				if ("glossary".equals(e.getLocalName())) {
					glossId = new HashSet<>();
				}
				if ("glossEntry".equals(e.getLocalName())) {
					String id = e.getAttributeValue("id");
					if (!id.isEmpty()) {
						if (glossId.contains(id)) {
							reason = "Duplicated \"id\" in <gls:glossEntry>";
							return false;
						}
						glossId.add(id);
					}
				}
				if ("translation".equals(e.getLocalName())) {
					String id = e.getAttributeValue("id");
					if (!id.isEmpty()) {
						if (glossId.contains(id)) {
							reason = "Duplicated \"id\" in <gls:translation>";
							return false;
						}
						glossId.add(id);
					}
				}
			}
		}

		// Element from XLIFF Core

		if ("xliff".equals(e.getLocalName())) {
			srcLang = e.getAttributeValue("srcLang");
			if (!checkLanguage(srcLang)) {
				reason = "Invalid source language '" + srcLang + "'";
				return false;
			}
			trgLang = e.getAttributeValue("trgLang");
			if (!trgLang.isEmpty() && !checkLanguage(trgLang)) {
				reason = "Invalid target language '" + trgLang + "'";
				return false;
			}
			fileId = new HashSet<>();
		}

		// check namespaces
		List<Attribute> atts = e.getAttributes();
		Iterator<Attribute> at = atts.iterator();
		while (at.hasNext()) {
			Attribute a = at.next();
			String prefix = a.getNamespace();
			if ("xmlns".equals(prefix)) {
				declaredNamespaces.put(a.getLocalName(), a.getValue());
				if (XLIFF_FS_2_0.equals(a.getValue())) {
					fsPrefix = a.getLocalName();

				}
			}
			if (fsPrefix.equals(prefix) && !("fs".equals(a.getLocalName()) || "subFs".equals(a.getLocalName()))) {
				reason = "Invalid Format Style attribute: '" + a + "'";
				return false;
			}
		}

		if ("file".equals(e.getLocalName())) {
			String id = e.getAttributeValue("id");
			if (fileId.contains(id)) {
				reason = "Duplicated \"id\" in <file>";
				return false;
			}
			fileId.add(id);
			groupId = new HashSet<>();
			unitId = new HashSet<>();
			if (noteId != null) {
				noteId = null;
			}
		}

		if ("skeleton".equals(e.getLocalName())) {
			List<XMLNode> content = e.getContent();
			if (content.isEmpty()) {
				String href = e.getAttributeValue("href");
				if (href.isEmpty()) {
					reason = "Missing \"href\" in skeleton";
					return false;
				}
			} else {
				if (!e.getAttributeValue("href").isEmpty()) {
					reason = "Non-empty skeleton with \"href\" found";
					return false;
				}
			}
		}

		if ("notes".equals(e.getLocalName())) {
			noteId = new HashSet<>();
		}

		if ("note".equals(e.getLocalName())) {
			String id = e.getAttributeValue("id");
			if (noteId.contains(id)) {
				reason = "Duplicated \"id\" in <note>";
				return false;
			}
			noteId.add(id);
		}

		if ("group".equals(e.getLocalName())) {
			String id = e.getAttributeValue("id");
			if (groupId.contains(id)) {
				reason = "Duplicated \"id\" in <group>";
				return false;
			}
			groupId.add(id);
		}

		if ("unit".equals(e.getLocalName())) {
			String id = e.getAttributeValue("id");
			if (unitId.contains(id)) {
				reason = "Duplicated \"id\" in <unit>";
				return false;
			}
			unitId.add(id);
			List<Element> segments = e.getChildren("segment");
			if (segments.isEmpty()) {
				reason = "<unit> without <segment> child";
				return false;
			}
			segCount = 0;
			maxSegment = segments.size() + e.getChildren("ignorable").size();
			dataId = new HashSet<>();
			if (noteId != null) {
				noteId = null;
			}
			smId = new HashSet<>();
			orderSet = new HashSet<>();
			unitSc = new Hashtable<>();
			sourceId = new HashSet<>();
		}

		if ("ignorable".equals(e.getLocalName())) {
			String id = e.getAttributeValue("id");
			if (!id.isEmpty()) {
				if (sourceId.contains(id)) {
					reason = "Duplicated \"id\" in <ignorable>";
					return false;
				}
				sourceId.add(id);
			}
		}

		if ("segment".equals(e.getLocalName())) {
			String id = e.getAttributeValue("id");
			if (!id.isEmpty()) {
				if (sourceId.contains(id)) {
					reason = "Duplicated \"id\" in <segment>";
					return false;
				}
				sourceId.add(id);
			}
			String currentState = e.getAttributeValue("state", "initial");
			Element target = e.getChild("target");
			if (!currentState.equals("initial") && target == null) {
				reason = "Missing <target> in <segment> with \"state\" other than \"initial\"";
				return false;
			}
			if ("final".equals(currentState)) {
				if (!validateInlineElements(e)) {
					return false;
				}
			}
			String subState = e.getAttributeValue("subState");
			if (!subState.isEmpty()) {
				String state = e.getAttributeValue("state");
				if (state.isEmpty()) {
					reason = "<segment> has \"subState\" attribute without corresponding \"state\"";
					return false;
				}
				int index = subState.indexOf(':');
				if (index == -1) {
					reason = "Invalid \"subState\" attribute value: " + subState;
					return false;
				}
				String prefix = subState.substring(0, index);
				if (!knownPrefixes.contains(prefix)) {
					reason = "Invalid prefix '" + prefix + "' in \"subState\" attribute";
					return false;
				}
			}
			segCount++;
		}

		if ("source".equals(e.getLocalName())) {
			String lang = e.getAttributeValue("xml:lang");
			if (!lang.isEmpty() && !srcLang.equals(lang)) {
				reason = "Different \"xml:lang\" in <source>";
				return false;
			}
			inSource = true;
			cantDelete = new HashSet<>();
		}

		if ("target".equals(e.getLocalName())) {
			String lang = e.getAttributeValue("xml:lang");
			if (trgLang.isEmpty()) {
				reason = "Missing \"trgLang\" in <xliff>";
				return false;
			}
			if (!inMatch && !lang.isEmpty() && !trgLang.equals(lang)) {
				reason = "Different \"xml:lang\" in <target>";
				return false;
			}
			if (inMatch && !isReference && !lang.isEmpty() && !trgLang.equals(lang)) {
				reason = "Different \"xml:lang\" in <target> from <mtc:match>";
				return false;
			}
			if (!inMatch) {
				String order = e.getAttributeValue("order", "" + segCount);
				int value = Integer.parseInt(order);
				if (value > maxSegment) {
					reason = "order=\"" + value + "\" in <target> is greater than the number of <segment> elements ("
							+ maxSegment + ")";
					return false;
				}
				if (orderSet.contains(order)) {
					reason = "Duplicated order=\"" + value + "\"  in <target>";
					return false;
				}
				orderSet.add(order);
			}
			inTarget = true;
		}

		if ("data".equals(e.getLocalName())) {
			String id = e.getAttributeValue("id");
			if (inMatch) {
				if (matchDataId.contains(id)) {
					reason = "Duplicated \"id\" for <data> in <mtc:match>";
					return false;
				}
				matchDataId.add(id);
			} else {
				if (dataId.contains(id)) {
					reason = "Duplicated \"id\" in <data>";
					return false;
				}
				dataId.add(id);
			}
		}

		// Inline elements

		if ("cp".equals(e.getLocalName())) {
			// #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
			String hex = e.getAttributeValue("hex");
			int value = Integer.valueOf(hex, 16);
			if (value == 0x9 || value == 0xA || value == 0xD || (value >= 0x20 && value <= 0xD7FF)
					|| (value >= 0xE000 && value <= 0xFFFD) || (value >= 0x10000 && value <= 0x10FFFF)) {
				reason = "Valid XML character represented as <cp> hex: " + hex;
				return false;
			}
		}

		if ("ph".equals(e.getLocalName())) {
			String id = e.getAttributeValue("id");
			if (inSource) {
				if (sourceId.contains(id)) {
					reason = "Duplicated \"id\" in <ph/>";
					return false;
				}
				sourceId.add(id);
				if (e.getAttributeValue("canDelete", "yes").equals("no")) {
					cantDelete.add(id);
				}
			}
			if (inTarget) {
				if (e.getAttributeValue("canDelete", "yes").equals("no")) {
					cantDelete.remove(id);
				}
			}
			boolean isCopy = !e.getAttributeValue("copyOf").isEmpty();
			String dataRef = e.getAttributeValue("dataRef");
			if (isCopy && !dataRef.isEmpty()) {
				reason = "<ph> element with both \"copyOf\" and \"dataRef\"";
				return false;
			}
			if (!dataRef.isEmpty()) {
				if (inMatch) {
					if (!matchDataId.contains(dataRef)) {
						reason = "Missing <data> element referenced by <ph> in <mtc:match>";
						return false;
					}
				} else {
					if (!dataId.contains(dataRef)) {
						reason = "Missing <data> element referenced by <ph>";
						return false;
					}
				}
			}
			String type = e.getAttributeValue("type");
			if (!type.isEmpty() && !knownTypes.contains(type)) {
				reason = "Invalid \"type\" attribute in <ph>";
				return false;
			}
			String subType = e.getAttributeValue("subType");
			if (!subType.isEmpty()) {
				if (type.isEmpty()) {
					reason = "Missing \"type\" attribute in <ph> that has \"subType\"";
					return false;
				}
				int index = subType.indexOf(':');
				if (index == -1) {
					reason = "Invalid \"subType\" attribute value: " + subType;
					return false;
				}
				String prefix = subType.substring(0, index);
				if (!knownPrefixes.contains(prefix)) {
					reason = "Invalid prefix '" + prefix + "' in \"subType\" attribute";
					return false;
				}
				if ("xlf".equals(prefix)) {
					if (!xlfSubTypes.contains(subType)) {
						reason = "Invalid \"subType\" attribute value: \'" + subType + "\' in <ph>";
						return false;
					}
					if ("ui".equals(type) && !"xlf:var".equals(subType)) {
						reason = "Invalid \"subType\" attribute value for type=\"ui\" in <ph>";
						return false;
					}
					if ("fmt".equals(type) && !fmtSubTypes.contains(subType)) {
						reason = "Invalid \"subType\" attribute value for type=\"fmt\" in <ph>";
						return false;
					}
				}
			}
		}

		if ("pc".equals(e.getLocalName())) {
			String id = e.getAttributeValue("id");
			if (inSource) {
				if (sourceId.contains(id)) {
					reason = "Duplicated \"id\" in <pc/>";
					return false;
				}
				sourceId.add(id);
				if (e.getAttributeValue("canDelete", "yes").equals("no")) {
					cantDelete.add(id);
				}
			}
			if (inTarget && e.getAttributeValue("canDelete", "yes").equals("no")) {
				cantDelete.remove(id);
			}
			boolean isCopy = !e.getAttributeValue("copyOf").isEmpty();
			String dataRefStart = e.getAttributeValue("dataRefStart");
			if (isCopy && !dataRefStart.isEmpty()) {
				reason = "<pc> element with both \"copyOf\" and \"dataRefStart\"";
				return false;
			}
			if (!dataRefStart.isEmpty()) {
				if (inMatch) {
					if (!matchDataId.contains(dataRefStart)) {
						reason = "Missing <data> element referenced by \"dataRefStart\" <pc> in <mtc:match>";
						return false;
					}
				} else {
					if (!dataId.contains(dataRefStart)) {
						reason = "Missing <data> element referenced by \"dataRefStart\" <pc>";
						return false;
					}
				}
			}
			String dataRefEnd = e.getAttributeValue("dataRefEnd");
			if (isCopy && !dataRefEnd.isEmpty()) {
				reason = "<pc> element with both \"copyOf\" and \"dataRefEnd\"";
				return false;
			}
			if (!dataRefEnd.isEmpty()) {
				if (inMatch) {
					if (!matchDataId.contains(dataRefEnd)) {
						reason = "Missing <data> element referenced by \"dataRefEnd\" in <pc> in <mtc:match>";
						return false;
					}
				} else {
					if (!dataId.contains(dataRefEnd)) {
						reason = "Missing <data> element referenced by \"dataRefEnd\" in <pc>";
						return false;
					}
				}
			}
			String type = e.getAttributeValue("type");
			if (!type.isEmpty() && !knownTypes.contains(type)) {
				reason = "Invalid \"type\" attribute in <pc>";
				return false;
			}
			String subType = e.getAttributeValue("subType");
			if (!subType.isEmpty()) {
				if (type.isEmpty()) {
					reason = "Missing \"type\" attribute in <pc> that has \"subType\"";
					return false;
				}
				int index = subType.indexOf(':');
				if (index == -1) {
					reason = "Invalid \"subType\" attribute value: " + subType;
					return false;
				}
				String prefix = subType.substring(0, index);
				if (!knownPrefixes.contains(prefix)) {
					reason = "Invalid prefix '" + prefix + "' in \"subType\" attribute";
					return false;
				}
				if ("xlf".equals(prefix)) {
					if (!xlfSubTypes.contains(subType)) {
						reason = "Invalid \"subType\" attribute value: \'" + subType + "\' in <pc>";
						return false;
					}
					if ("ui".equals(type) && !"xlf:var".equals(subType)) {
						reason = "Invalid \"subType\" attribute value for type=\"ui\" in <pc>";
						return false;
					}
					if ("fmt".equals(type) && !fmtSubTypes.contains(subType)) {
						reason = "Invalid \"subType\" attribute value for type=\"fmt\" in <pc>";
						return false;
					}
				}
			}
		}

		if ("sc".equals(e.getLocalName())) {
			String id = e.getAttributeValue("id");
			if (inSource) {
				if (sourceId.contains(id)) {
					reason = "Duplicated \"id\" in <sc/>";
					return false;
				}
				sourceId.add(id);
				if (!"yes".equals(e.getAttributeValue("isolated"))) {
					unitSc.put(id, e);
				}
				if (e.getAttributeValue("canDelete", "yes").equals("no")) {
					cantDelete.add(id);
				}
			}
			if (inTarget && e.getAttributeValue("canDelete", "yes").equals("no")) {
				cantDelete.remove(id);
			}
			boolean isCopy = !e.getAttributeValue("copyOf").isEmpty();
			String dataRef = e.getAttributeValue("dataRef");
			if (isCopy && !dataRef.isEmpty()) {
				reason = "<sc> element with both \"copyOf\" and \"dataRef\"";
				return false;
			}
			if (!dataRef.isEmpty()) {
				if (inMatch) {
					if (!matchDataId.contains(dataRef)) {
						reason = "Missing <data> element referenced by <sc>";
						return false;
					}
				} else {
					if (!dataId.contains(dataRef)) {
						reason = "Missing <data> element referenced by <sc>";
						return false;
					}
				}
			}
			String type = e.getAttributeValue("type");
			if (!type.isEmpty() && !knownTypes.contains(type)) {
				reason = "Invalid \"type\" attribute in <sc>";
				return false;
			}
			String subType = e.getAttributeValue("subType");
			if (!subType.isEmpty()) {
				if (type.isEmpty()) {
					reason = "Missing \"type\" attribute in <sc> that has \"subType\"";
					return false;
				}
				int index = subType.indexOf(':');
				if (index == -1) {
					reason = "Invalid \"subType\" attribute value: " + subType;
					return false;
				}
				String prefix = subType.substring(0, index);
				if (!knownPrefixes.contains(prefix)) {
					reason = "Invalid prefix '" + prefix + "' in \"subType\" attribute";
					return false;
				}
				if ("xlf".equals(prefix)) {
					if (!xlfSubTypes.contains(subType)) {
						reason = "Invalid \"subType\" attribute value: \'" + subType + "\' in <sc>";
						return false;
					}
					if ("ui".equals(type) && !"xlf:var".equals(subType)) {
						reason = "Invalid \"subType\" attribute value for type=\"ui\" in <sc>";
						return false;
					}
					if ("fmt".equals(type) && !fmtSubTypes.contains(subType)) {
						reason = "Invalid \"subType\" attribute value for type=\"fmt\" in <sc>";
						return false;
					}
				}
			}
		}

		if ("ec".equals(e.getLocalName())) {
			boolean isolated = e.getAttributeValue("isolated", "no").equals("yes");
			if (isolated) {
				String id = e.getAttributeValue("id");
				if (id.isEmpty()) {
					reason = "Missing \"id\" attribute in <ec/>";
					return false;
				}
				if (inSource) {
					if (sourceId.contains(id)) {
						reason = "Duplicated \"id\" in <ec/>";
						return false;
					}
					sourceId.add(id);
					if (e.getAttributeValue("canDelete", "yes").equals("no")) {
						cantDelete.add(id);
					}
				}
				if (inTarget && e.getAttributeValue("canDelete", "yes").equals("no")) {
					cantDelete.remove(id);
				}
			} else {
				if (e.hasAttribute("dir")) {
					reason = "Non isolated <ec/> has \"dir\" attribute";
					return false;
				}
				String startRef = e.getAttributeValue("startRef");
				if (startRef.isEmpty()) {
					reason = "Missing \"startRef\" attribute in <ec/>";
					return false;
				}
				if (!unitSc.containsKey(startRef)) {
					reason = "Missing <sc/> element with id=\"" + startRef + "\" referenced by <ec/>";
					return false;
				}
				Element sc = unitSc.get(startRef);
				if (!sc.getAttributeValue("canCopy", "yes").equals(e.getAttributeValue("canCopy", "yes"))) {
					reason = "Different 'canCopy' attribute in <sc/> with id=\"" + startRef + "\" and matching <ec/>";
					return false;
				}
				if (!sc.getAttributeValue("canDelete", "yes").equals(e.getAttributeValue("canDelete", "yes"))) {
					reason = "Different 'canDelete' attribute in <sc/> with id=\"" + startRef + "\" and matching <ec/>";
					return false;
				}
				if (!sc.getAttributeValue("canOverlap", "yes").equals(e.getAttributeValue("canOverlap", "yes"))) {
					reason = "Different 'canOverlap' attribute in <sc/> with id=\"" + startRef
							+ "\" and matching <ec/>";
					return false;
				}
				if (!sc.getAttributeValue("canReorder", "yes").equals(e.getAttributeValue("canReorder", "yes"))) {
					reason = "Different 'canReorder' attribute in <sc/> with id=\"" + startRef
							+ "\" and matching <ec/>";
					return false;
				}
				if (inSource) {
					unitSc.remove(startRef);
				}
			}
			boolean isCopy = !e.getAttributeValue("copyOf").isEmpty();
			String dataRef = e.getAttributeValue("dataRef");
			if (isCopy && !dataRef.isEmpty()) {
				reason = "<ec> element with both \"copyOf\" and \"dataRef\"";
				return false;
			}
			if (!dataRef.isEmpty()) {
				if (inMatch) {
					if (!matchDataId.contains(dataRef)) {
						reason = "Missing <data> element referenced by <ec> in <mtc:match>";
						return false;
					}
				} else {
					if (!dataId.contains(dataRef)) {
						reason = "Missing <data> element referenced by <ec>";
						return false;
					}
				}
			}
			String type = e.getAttributeValue("type");
			if (!type.isEmpty() && !knownTypes.contains(type)) {
				reason = "Invalid \"type\" attribute in <ec>";
				return false;
			}
			String subType = e.getAttributeValue("subType");
			if (!subType.isEmpty()) {
				if (type.isEmpty()) {
					reason = "Missing \"type\" attribute in <ec> that has \"subType\"";
					return false;
				}
				int index = subType.indexOf(':');
				if (index == -1) {
					reason = "Invalid \"subType\" attribute value: " + subType;
					return false;
				}
				String prefix = subType.substring(0, index);
				if (!knownPrefixes.contains(prefix)) {
					reason = "Invalid prefix '" + prefix + "' in \"subType\" attribute";
					return false;
				}
				if ("xlf".equals(prefix)) {
					if (!xlfSubTypes.contains(subType)) {
						reason = "Invalid \"subType\" attribute value: \'" + subType + "\' in <ec>";
						return false;
					}
					if ("ui".equals(type) && !"xlf:var".equals(subType)) {
						reason = "Invalid \"subType\" attribute value for type=\"ui\" in <ec>";
						return false;
					}
					if ("fmt".equals(type) && !fmtSubTypes.contains(subType)) {
						reason = "Invalid \"subType\" attribute value for type=\"fmt\" in <ec>";
						return false;
					}
				}
			}
		}

		if ("mrk".equals(e.getLocalName())) {
			String id = e.getAttributeValue("id");
			if (inSource) {
				if (sourceId.contains(id)) {
					reason = "Duplicated \"id\" in <mrk/>";
					return false;
				}
				sourceId.add(id);
			}
			String type = e.getAttributeValue("type");
			if ("comment".equals(type)) {
				if (e.getAttributeValue("value").isEmpty()) {
					String ref = e.getAttributeValue("ref");
					if (ref.isEmpty()) {
						reason = "Missing \"ref\" in comment annotation";
						return false;
					}
					if (!ref.startsWith("#n") || ref.indexOf('=') == -1) {
						reason = "Invalid fragment identifier '" + ref + "' in comment annotation";
						return false;
					}
					String refId = ref.substring(ref.indexOf('=') + 1);
					if (noteId == null || !noteId.contains(refId)) {
						reason = "Missing <note> referenced in comment annotation";
						return false;
					}
				} else {
					if (!e.getAttributeValue("ref").isEmpty()) {
						reason = "Comment annotation contains both \"value\" and \"ref\"";
						return false;
					}
				}
			}
		}

		if ("sm".equals(e.getLocalName())) {
			String id = e.getAttributeValue("id");
			if (inSource) {
				if (sourceId.contains(id)) {
					reason = "Duplicated \"id\" in <sm/>";
					return false;
				}
				sourceId.add(id);
				smId.add(id);
			}
		}

		if ("em".equals(e.getLocalName())) {
			String startRef = e.getAttributeValue("startRef");
			if (!smId.contains(startRef)) {
				reason = "Missing <sm> with id=\"" + startRef + "\"referenced by <em>";
				return false;
			}
		}

		// attributes from fs module

		if (!fsPrefix.isEmpty()) {
			String fs = e.getAttributeValue(fsPrefix + ":fs");
			String subFs = e.getAttributeValue(fsPrefix + ":subFs");
			if (!subFs.isEmpty() && fs.isEmpty()) {
				reason = "Attribute \"" + fsPrefix + ":subFs\" without corresponding \"" + fsPrefix + ":fs\"";
				return false;
			}
		}

		List<Element> children = e.getChildren();
		for (int i = 0; i < children.size(); i++) {
			Element child = children.get(i);
			if ("source".equals(child.getLocalName())) {

			}
			boolean result = recurse(child);
			if (!result) {
				return false;
			}
			if (result && "unit".equals(child.getName())) {
				if (!unitSc.isEmpty()) {
					reason = "<sc> element without matching <ec> in <unit>";
					return false;
				}
			}
		}

		if (XLIFF_MATCHES_2_0.equals(declaredNamespaces.get(namespace)) && "match".equals(e.getLocalName())) {
			inMatch = false;
			isReference = false;
		}

		// remove namespaces declared in current element
		at = atts.iterator();
		while (at.hasNext()) {
			Attribute a = at.next();
			String prefix = a.getNamespace();
			if ("xmlns".equals(prefix)) {
				declaredNamespaces.remove(prefix);
			}
		}
		if ("target".equals(e.getLocalName())) {
			if (!cantDelete.isEmpty()) {
				reason = "Inline element with \"canDelete\" set to \"no\" is missing in <target>";
				return false;
			}
			inTarget = false;
		}
		if ("ignorable".equals(e.getLocalName())) {
			if (!validateInlineElements(e)) {
				return false;
			}
		}
		return true;
	}

	private boolean validateInlineElements(Element segment) {
		Element target = segment.getChild("target");
		if ("ignorable".equals(segment.getLocalName()) && target == null) {
			return true;
		}
		Element source = segment.getChild("source");
		List<Element> sourceList = source.getChildren();
		Iterator<Element> it = sourceList.iterator();
		while (it.hasNext()) {
			Element tag = it.next();
			if ("ph".equals(tag.getName())) {
				List<Element> phList = target.getChildren("ph");
				for (int i = 0; i < phList.size(); i++) {
					Element ph = phList.get(i);
					if (tag.getAttributeValue("id").equals(ph.getAttributeValue("id"))) {
						if (!tag.getAttributeValue("canCopy").equals(ph.getAttributeValue("canCopy"))) {
							reason = "<ph> element with different value of \"canCopy\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("canDelete").equals(ph.getAttributeValue("canDelete"))) {
							reason = "<ph> element with different value of \"canDelete\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("canReorder").equals(ph.getAttributeValue("canReorder"))) {
							reason = "<ph> element with different value of \"canReorder\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("copyOf").equals(ph.getAttributeValue("copyOf"))) {
							reason = "<ph> element with different value of \"copyOf\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("dataRef").equals(ph.getAttributeValue("dataRef"))) {
							reason = "<ph> element with different value of \"dataRef\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("subFlows").equals(ph.getAttributeValue("subFlows"))) {
							reason = "<ph> element with different value of \"subFlows\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("type").equals(ph.getAttributeValue("type"))) {
							reason = "<ph> element with different value of \"type\" in <source> and <target>";
							return false;
						}
					}
				}
			}
			if ("pc".equals(tag.getName())) {
				List<Element> pcList = target.getChildren("pc");
				for (int i = 0; i < pcList.size(); i++) {
					Element pc = pcList.get(i);
					if (tag.getAttributeValue("id").equals(pc.getAttributeValue("id"))) {
						if (!tag.getAttributeValue("canCopy").equals(pc.getAttributeValue("canCopy"))) {
							reason = "<pc> element with different value of \"canCopy\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("canDelete").equals(pc.getAttributeValue("canDelete"))) {
							reason = "<pc> element with different value of \"canDelete\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("canOverlap").equals(pc.getAttributeValue("canOverlap"))) {
							reason = "<pc> element with different value of \"canOverlap\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("canReorder").equals(pc.getAttributeValue("canReorder"))) {
							reason = "<pc> element with different value of \"canReorder\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("copyOf").equals(pc.getAttributeValue("copyOf"))) {
							reason = "<pc> element with different value of \"copyOf\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("dataRefStart").equals(pc.getAttributeValue("dataRefStart"))) {
							reason = "<pc> element with different value of \"dataRefStart\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("dataRefEnd").equals(pc.getAttributeValue("dataRefEnd"))) {
							reason = "<pc> element with different value of \"dataRefEnd\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("subFlowsStart").equals(pc.getAttributeValue("subFlowsStart"))) {
							reason = "<pc> element with different value of \"subFlowsStart\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("subFlowsEnd").equals(pc.getAttributeValue("subFlowsEnd"))) {
							reason = "<pc> element with different value of \"subFlowsEnd\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("type").equals(pc.getAttributeValue("type"))) {
							reason = "<pc> element with different value of \"type\" in <source> and <target>";
							return false;
						}
					}
				}
			}
			if ("sc".equals(tag.getName())) {
				List<Element> scList = target.getChildren("sc");
				for (int i = 0; i < scList.size(); i++) {
					Element sc = scList.get(i);
					if (tag.getAttributeValue("id").equals(sc.getAttributeValue("id"))) {
						if (!tag.getAttributeValue("canCopy").equals(sc.getAttributeValue("canCopy"))) {
							reason = "<sc> element with different value of \"canCopy\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("canDelete").equals(sc.getAttributeValue("canDelete"))) {
							reason = "<sc> element with different value of \"canDelete\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("canOverlap").equals(sc.getAttributeValue("canOverlap"))) {
							reason = "<sc> element with different value of \"canOverlap\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("canReorder").equals(sc.getAttributeValue("canReorder"))) {
							reason = "<sc> element with different value of \"canReorder\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("copyOf").equals(sc.getAttributeValue("copyOf"))) {
							reason = "<sc> element with different value of \"copyOf\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("dataRef").equals(sc.getAttributeValue("dataRef"))) {
							reason = "<sc> element with different value of \"dataRef\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("subFlows").equals(sc.getAttributeValue("subFlows"))) {
							reason = "<sc> element with different value of \"subFlows\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("type").equals(sc.getAttributeValue("type"))) {
							reason = "<sc> element with different value of \"type\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("isolated", "no").equals(sc.getAttributeValue("isolated", "no"))) {
							reason = "<sc> element with different value of \"isolated\" in <source> and <target>";
							return false;
						}
					}
				}
				boolean isolated = tag.getAttributeValue("isolated", "no").equals("yes");
				if (!isolated) {
					List<Element> ecList = target.getChildren("ec");
					for (int i = 0; i < ecList.size(); i++) {
						Element ec = ecList.get(i);
						if (tag.getAttributeValue("id").equals(ec.getAttributeValue("startRef"))) {
							if (!tag.getAttributeValue("canCopy").equals(ec.getAttributeValue("canCopy"))) {
								reason = "<sc> element with different value of \"canCopy\" in <source> and <target>";
								return false;
							}
							if (!tag.getAttributeValue("canDelete").equals(ec.getAttributeValue("canDelete"))) {
								reason = "<sc> element with different value of \"canDelete\" in <source> and <target>";
								return false;
							}
							if (!tag.getAttributeValue("canOverlap").equals(ec.getAttributeValue("canOverlap"))) {
								reason = "<sc> element with different value of \"canOverlap\" in <source> and <target>";
								return false;
							}
							if (!tag.getAttributeValue("canReorder").equals(ec.getAttributeValue("canReorder"))) {
								reason = "<sc> element with different value of \"canReorder\" in <source> and <target>";
								return false;
							}
							if (!tag.getAttributeValue("copyOf").equals(ec.getAttributeValue("copyOf"))) {
								reason = "<sc> element with different value of \"copyOf\" in <source> and <target>";
								return false;
							}
							if (!tag.getAttributeValue("dataRef").equals(ec.getAttributeValue("dataRef"))) {
								reason = "<sc> element with different value of \"dataRef\" in <source> and <target>";
								return false;
							}
							if (!tag.getAttributeValue("subFlows").equals(ec.getAttributeValue("subFlows"))) {
								reason = "<sc> element with different value of \"subFlows\" in <source> and <target>";
								return false;
							}
							if (!tag.getAttributeValue("type").equals(ec.getAttributeValue("type"))) {
								reason = "<sc> element with different value of \"type\" in <source> and <target>";
								return false;
							}
							if (!tag.getAttributeValue("isolated", "no")
									.equals(ec.getAttributeValue("isolated", "no"))) {
								reason = "<sc> element with different value of \"isolated\" in <source> and <target>";
								return false;
							}
						}
					}
				}
			}
			if ("ec".equals(tag.getName())) {
				List<Element> ecList = target.getChildren("ec");
				for (int i = 0; i < ecList.size(); i++) {
					Element ec = ecList.get(i);
					if (tag.getAttributeValue("id").equals(ec.getAttributeValue("id"))) {
						if (!tag.getAttributeValue("canCopy").equals(ec.getAttributeValue("canCopy"))) {
							reason = "<sc> element with different value of \"canCopy\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("canDelete").equals(ec.getAttributeValue("canDelete"))) {
							reason = "<ec> element with different value of \"canDelete\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("canOverlap").equals(ec.getAttributeValue("canOverlap"))) {
							reason = "<ec> element with different value of \"canOverlap\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("canReorder").equals(ec.getAttributeValue("canReorder"))) {
							reason = "<ec> element with different value of \"canReorder\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("copyOf").equals(ec.getAttributeValue("copyOf"))) {
							reason = "<ec> element with different value of \"copyOf\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("startRef").equals(ec.getAttributeValue("startRef"))) {
							reason = "<ec> element with different value of \"startRef\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("subFlows").equals(ec.getAttributeValue("subFlows"))) {
							reason = "<ec> element with different value of \"subFlows\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("type").equals(ec.getAttributeValue("type"))) {
							reason = "<ec> element with different value of \"type\" in <source> and <target>";
							return false;
						}
						if (!tag.getAttributeValue("isolated", "no").equals(ec.getAttributeValue("isolated", "no"))) {
							reason = "<ec> element with different value of \"isolated\" in <source> and <target>";
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	private Source getSource(String string) {
		String location = resolver.matchURI(string);
		if (location == null) {
			location = resolver.matchPublic(string);
		}
		if (location == null) {
			location = resolver.matchSystem("", string);
		}
		return new StreamSource(location);
	}

	public String getReason() {
		return reason;
	}

	private boolean checkLanguage(String lang) {
		if (lang.startsWith("x-") || lang.startsWith("X-")) {
			// custom language code
			return true;
		}
		return !registry.getTagDescription(lang).isEmpty();
	}
}
