/*******************************************************************************
 * Copyright (c) 2003, 2019 Maxprograms.
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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import com.maxprograms.languages.RegistryParser;
import com.maxprograms.xml.Attribute;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.XMLNode;
import com.maxprograms.xml.XMLUtils;

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

	private static Logger LOGGER = System.getLogger(Xliff20.class.getName());
	private String reason = "";
	private Catalog resolver;

	private RegistryParser registry;

	private Hashtable<String, String> declaredNamespaces;
	private String srcLang;
	private String trgLang;
	private HashSet<String> fileId;
	private HashSet<String> groupId;
	private HashSet<String> unitId;
	private HashSet<String> cantDelete;
	private HashSet<String> sourceId;
	private HashSet<String> dataId;
	private HashSet<String> matchId;
	private HashSet<String> metaId;
	private HashSet<String> glossId;
	private boolean inMatch;
	private boolean isReference;
	private boolean inSource;
	private boolean inTarget;
	
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
		} catch (SAXException | IOException | ParserConfigurationException e) {
			LOGGER.log(Level.ERROR, e);
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
			List<Attribute> atts = e.getAttributes();
			Iterator<Attribute> it = atts.iterator();
			while (it.hasNext()) {
				Attribute a = it.next();
				if ("xmlns".equals(a.getNamespace())) {
					declaredNamespaces.put(a.getLocalName(), a.getValue());
				}
			}
			srcLang = e.getAttributeValue("srcLang");
			if (!checkLanguage(srcLang)) {
				reason = "Invalid source language";
				return false;
			}
			trgLang = e.getAttributeValue("trgLang");
			if (!trgLang.isEmpty() && !checkLanguage(trgLang)) {
				reason = "Invalid target language";
				return false;
			}
		}

		if ("file".equals(e.getLocalName())) {
			String id = e.getAttributeValue("id");
			fileId = new HashSet<>();
			if (fileId.contains(id)) {
				reason = "Duplicated \"id\" in <file>";
				return false;
			}
			fileId.add(id);
			groupId = new HashSet<>();
			unitId = new HashSet<>();
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
			dataId = new HashSet<>();
		}

		if ("source".equals(e.getLocalName())) {
			String lang = e.getAttributeValue("xml:lang");
			if (!lang.isEmpty() && !srcLang.equals(lang)) {
				reason = "Different \"xml:lang\" in <source>";
				return false;
			}
			sourceId = new HashSet<>();
			cantDelete = new HashSet<>();
			inSource = true;
		}

		if ("target".equals(e.getLocalName())) {
			String lang = e.getAttributeValue("xml:lang");
			if (trgLang.isEmpty()) {
				reason = "Missing \"trgLang\" in <file>";
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
			inTarget = true;
		}

		if ("data".equals(e.getLocalName())) {
			String id = e.getAttributeValue("id");
			if (dataId.contains(id)) {
				reason = "Duplicated \"id\" in <data>";
				return false;
			}
			dataId.add(id);
		}
		
		// Inline elements

		if ("cp".equals(e.getLocalName())) {
			String hex = e.getAttributeValue("hex");
			int value = Integer.valueOf(hex, 16);
			String s = "" + (char) value;
			if (s.equals(XMLUtils.validChars(s))) {
				reason = "Valid XML character represented as <cp>";
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
			String dataRef = e.getAttributeValue("dataRef");
			if (!dataRef.isEmpty()) {
				if (!dataId.contains(dataRef)) {
					reason = "Missing <data> element referenced by <ph>";
					return false;
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
			String dataRefStart = e.getAttributeValue("dataRefStart");
			if (!dataRefStart.isEmpty() && !dataId.contains(dataRefStart)) {
				reason = "Missing <data> element referenced by \"dataRefStart\" <pc>";
				return false;
			}
			String dataRefEnd = e.getAttributeValue("dataRefEnd");
			if (!dataRefEnd.isEmpty() && !dataId.contains(dataRefEnd)) {
				reason = "Missing <data> element referenced by \"dataRefEnd\" in <pc>";
				return false;
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
				if (e.getAttributeValue("canDelete", "yes").equals("no")) {
					cantDelete.add(id);
				}
			}
			if (inTarget && e.getAttributeValue("canDelete", "yes").equals("no")) {
				cantDelete.remove(id);
			}
			String dataRef = e.getAttributeValue("dataRef");
			if (!dataRef.isEmpty() && !dataId.contains(dataRef)) {
				reason = "Missing <data> element referenced by <sc>";
				return false;
			}
		}
		
		if ("ec".equals(e.getLocalName())) {
			String id = e.getAttributeValue("id");
			if (!id.isEmpty()) {
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
			} 
			String dataRef = e.getAttributeValue("dataRef");
			if (!dataRef.isEmpty() && !dataId.contains(dataRef)) {
				reason = "Missing <data> element referenced by <ec>";
				return false;
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
		}
		
		if ("sm".equals(e.getLocalName())) {
			String id = e.getAttributeValue("id");
			if (inSource) {
				if (sourceId.contains(id)) {
					reason = "Duplicated \"id\" in <sm/>";
					return false;
				}
				sourceId.add(id);
			}			
		}
		
		if ("em".equals(e.getLocalName())) {
			String startRef = e.getAttributeValue("startRef");
			if (!sourceId.contains(startRef)) {
				reason = "Missing <sm> referenced by <em>";
				return false;
			}
		}
		
		List<Element> children = e.getChildren();
		for (int i = 0; i < children.size(); i++) {
			boolean result = recurse(children.get(i));
			if (!result) {
				return false;
			}
		}
		if (XLIFF_MATCHES_2_0.equals(declaredNamespaces.get(namespace)) && "match".equals(e.getLocalName())) {
			inMatch = false;
			isReference = false;
		}
		if ("source".equals(e.getLocalName())) {
			inSource = false;
		}
		if ("target".equals(e.getLocalName())) {
			if (!cantDelete.isEmpty()) {
				reason = "Inline element with \"canDelete\" set to \"no\" is missing in <target>";
				return false;
			}
			inTarget = false;
		}

		return true;
	}

	private Source getSource(String string) {
		String location = resolver.getLocation(string);
		Source source = new StreamSource(location);
		return source;
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
