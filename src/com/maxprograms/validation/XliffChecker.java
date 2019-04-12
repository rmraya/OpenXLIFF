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

package com.maxprograms.validation;

import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.converters.Utils;
import com.maxprograms.languages.RegistryParser;
import com.maxprograms.xml.Attribute;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;

public class XliffChecker {

	private static Logger LOGGER = System.getLogger(XliffChecker.class.getName());
	private String reason;
	private String version;

	private RegistryParser registry;
	
	private HashSet<String> xliffNamespaces;
	private Hashtable<String, Set<String>> attributesTable;
	private Hashtable<String, String> altSrcItTable;
	private Hashtable<String, String> altTgtItTable;
	private Hashtable<String, String> tgtItTable;
	private Hashtable<String, String> phasesTable;
	private Hashtable<String, String> xids;
	private Hashtable<String, String> groupIds;
	private Hashtable<String, String> srcItTable;
	private Hashtable<String, String> toolsTable;
	private Hashtable<String, String> ids;
	private Hashtable<String, String> midTable;
	private Hashtable<String, String> bxTable;
	private Hashtable<String, String> exTable;
	private Hashtable<String, String> bptTable;
	private Hashtable<String, String> eptTable;
	private Hashtable<String, String> inlineIds;
	private String sourceLanguage;
	private String targetLanguage;
	private boolean inSegSource;
	private boolean inSource;
	private boolean inAltTrans;

	public static void main(String[] args) {

		String[] fixedArgs = Utils.fixPath(args);

		String file = "";
		String catalog = "";
		for (int i = 0; i < fixedArgs.length; i++) {
			String arg = fixedArgs[i];
			if (arg.equals("-help")) {
				help();
				return;
			}
			if (arg.equals("-file") && (i + 1) < fixedArgs.length) {
				file = fixedArgs[i + 1];
			}
			if (arg.equals("-catalog") && (i + 1) < fixedArgs.length) {
				catalog = fixedArgs[i + 1];
			}
		}
		if (fixedArgs.length < 2) {
			help();
			return;
		}
		if (file.isEmpty()) {
			LOGGER.log(Level.ERROR, "Missing '-file' parameter.");
			return;
		}
		if (catalog.isEmpty()) {
			File catalogFolder = new File(new File(System.getProperty("user.dir")), "catalog");
			if (!catalogFolder.exists()) {
				LOGGER.log(Level.ERROR, "'catalog' folder not found.");
				return;
			}
			catalog = new File(catalogFolder, "catalog.xml").getAbsolutePath();
		}
		File catalogFile = new File(catalog);
		if (!catalogFile.exists()) {
			LOGGER.log(Level.ERROR, "Catalog file does not exist.");
			return;
		}
		try {
			XliffChecker instance = new XliffChecker();
			if (!instance.validate(file, catalog)) {
				LOGGER.log(Level.ERROR, "Invalid XLIFF. Reason: " + instance.getReason());
				return;
			}
			LOGGER.log(Level.INFO, "File is valid XLIFF " + instance.getVersion());
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, "Error creating validator", e);
		}
	}

	private static void help() {
		String launcher = "   xliffchecker.sh ";
		if (System.getProperty("file.separator").equals("\\")) {
			launcher = "   xliffchecker.bat ";
		}
		String help = "Usage:\n\n" + launcher
				+ "[-help] -file xliffFile [-catalog catalogFile] \n\n"
				+ "Where:\n\n" 
				+ "   -help:      (optional) Display this help information and exit\n"
				+ "   -file:      XLIFF file to validate\n" 
				+ "   -catalog:   (optional) XML catalog to use for processing\n";
		System.out.println(help);
	}

	public String getVersion() {
		return version;
	}

	public XliffChecker() throws IOException {
		registry = new RegistryParser();
		xliffNamespaces = new HashSet<>();
		xliffNamespaces.add("urn:oasis:names:tc:xliff:document:1.1");
		xliffNamespaces.add("urn:oasis:names:tc:xliff:document:1.2");
	}

	public boolean validate(String file, String catalog) {
		try {
			SAXBuilder builder = new SAXBuilder();
			builder.setValidating(true);
			builder.setEntityResolver(new Catalog(catalog));
			Document document = builder.build(file);
			Element root = document.getRootElement();
			if (!"xliff".equals(root.getLocalName())) {
				reason = "Selected file is not an XLIFF document";
				return false;
			}
			version = root.getAttributeValue("version");
			if ("1.0".equals(version)) {
				// validate with DTD and recurse
				Xliff10 validator = new Xliff10();
				boolean valid = validator.validate(document, catalog);
				if (!valid) {
					reason = validator.getReason();
					return false;
				}
			} else if ("1.1".equals(version)) {
				// validate with schema and recurse
				Xliff11 validator = new Xliff11();
				boolean valid = validator.validate(document, catalog);
				if (!valid) {
					reason = validator.getReason();
					return false;
				}
			} else if ("1.2".equals(version)) {
				// validate against translational only
				Xliff12 validator = new Xliff12();
				boolean valid = validator.validate(document, catalog);
				if (!valid) {
					reason = validator.getReason();
					return false;
				}
			} else if ("2.0".equals(version)) {
				// validate with schemas only, for now
				Xliff20 validator = new Xliff20();
				boolean valid = validator.validate(file, catalog);
				if (!valid) {
					reason = validator.getReason();
					return false;
				}
				return true;
			} else if ("2.1".equals(version)) {
				reason = "XLIFF 2.1 not supported yet";
				return false;
			} else {
				reason = "Invalid XLIFF version";
				return false;
			}

			// There is a bug in XLIFF 1.1 schema, duplicated in XLIFF 1.2 Transitional
			// schema,
			// they allow any attribute in XLIFF elements.
			// Load a table of attributes for validating details that the parser ignored.

			createAttributesTable();
			return recurse(root);

		} catch (IOException | SAXException | ParserConfigurationException e) {
			reason = e.getMessage();
			return false;
		}
	}

	public String getReason() {
		return reason;
	}

	private void createAttributesTable() {
		attributesTable = new Hashtable<>();

		Set<String> xliffSet = new HashSet<>();
		xliffSet.add("version");
		attributesTable.put("xliff", xliffSet);

		Set<String> fileSet = new HashSet<>();
		fileSet.add("original");
		fileSet.add("source-language");
		fileSet.add("datatype");
		fileSet.add("tool");
		fileSet.add("tool-id");
		fileSet.add("date");
		fileSet.add("ts");
		fileSet.add("category");
		fileSet.add("target-language");
		fileSet.add("product-name");
		fileSet.add("product-version");
		fileSet.add("build-num");
		attributesTable.put("file", fileSet);

		attributesTable.put("header", new HashSet<>()); // this element doesn't have attributes
		attributesTable.put("skl", new HashSet<>()); // this element doesn't have attributes

		Set<String> internal_fileSet = new HashSet<>();
		internal_fileSet.add("form");
		internal_fileSet.add("crc");
		attributesTable.put("internal-file", internal_fileSet);

		Set<String> external_fileSet = new HashSet<>();
		external_fileSet.add("href");
		external_fileSet.add("uid");
		external_fileSet.add("crc");
		attributesTable.put("external-file", external_fileSet);

		attributesTable.put("glossary", new HashSet<>()); // this element doesn't have attributes
		attributesTable.put("reference", new HashSet<>()); // this element doesn't have attributes

		Set<String> noteSet = new HashSet<>();
		noteSet.add("from");
		noteSet.add("priority");
		noteSet.add("annotates");
		attributesTable.put("note", noteSet);

		attributesTable.put("phase-group", new HashSet<>()); // this element doesn't have attributes

		Set<String> phaseSet = new HashSet<>();
		phaseSet.add("phase-name");
		phaseSet.add("process-name");
		phaseSet.add("company-name");
		phaseSet.add("tool");
		phaseSet.add("tool-id");
		phaseSet.add("date");
		phaseSet.add("job-id");
		phaseSet.add("contact-name");
		phaseSet.add("contact-email");
		phaseSet.add("contact-phone");
		attributesTable.put("phase", phaseSet);

		Set<String> toolSet = new HashSet<>();
		toolSet.add("tool-id");
		toolSet.add("tool-name");
		toolSet.add("tool-version");
		toolSet.add("tool-company");
		attributesTable.put("tool", toolSet);

		Set<String> count_groupSet = new HashSet<>();
		count_groupSet.add("name");
		attributesTable.put("count-group", count_groupSet);

		Set<String> countSet = new HashSet<>();
		countSet.add("count-type");
		countSet.add("phase-name");
		countSet.add("unit");
		attributesTable.put("count", countSet);

		Set<String> context_groupSet = new HashSet<>();
		context_groupSet.add("crc");
		context_groupSet.add("name");
		context_groupSet.add("purpose");
		attributesTable.put("context-group", context_groupSet);

		Set<String> contextSet = new HashSet<>();
		contextSet.add("context-type");
		contextSet.add("match-mandatory");
		contextSet.add("crc");
		attributesTable.put("context", contextSet);

		Set<String> prop_groupSet = new HashSet<>();
		prop_groupSet.add("name");
		attributesTable.put("prop-group", prop_groupSet);

		Set<String> propSet = new HashSet<>();
		propSet.add("prop-type");
		attributesTable.put("prop", propSet);

		attributesTable.put("body", new HashSet<>()); // this element doesn't have attributes

		Set<String> groupSet = new HashSet<>();
		groupSet.add("id");
		groupSet.add("datatype");
		groupSet.add("ts");
		groupSet.add("restype");
		groupSet.add("resname");
		groupSet.add("extradata");
		groupSet.add("help-id");
		groupSet.add("menu");
		groupSet.add("menu-option");
		groupSet.add("menu-name");
		groupSet.add("coord");
		groupSet.add("font");
		groupSet.add("css-style");
		groupSet.add("style");
		groupSet.add("exstyle");
		groupSet.add("extype");
		groupSet.add("translate");
		groupSet.add("reformat");
		groupSet.add("maxbytes");
		groupSet.add("minbytes");
		groupSet.add("size-unit");
		groupSet.add("maxheight");
		groupSet.add("minheight");
		groupSet.add("maxwidth");
		groupSet.add("minwidth");
		groupSet.add("charclass");
		if (!version.equals("1.1")) {
			groupSet.add("merged-trans");
		}
		attributesTable.put("group", groupSet);

		Set<String> trans_unitSet = new HashSet<>();
		trans_unitSet.add("id");
		trans_unitSet.add("approved");
		trans_unitSet.add("translate");
		trans_unitSet.add("reformat");
		trans_unitSet.add("datatype");
		trans_unitSet.add("ts");
		trans_unitSet.add("phase-name");
		trans_unitSet.add("restype");
		trans_unitSet.add("resname");
		trans_unitSet.add("extradata");
		trans_unitSet.add("help-id");
		trans_unitSet.add("menu");
		trans_unitSet.add("menu-option");
		trans_unitSet.add("menu-name");
		trans_unitSet.add("coord");
		trans_unitSet.add("font");
		trans_unitSet.add("css-style");
		trans_unitSet.add("style");
		trans_unitSet.add("exstyle");
		trans_unitSet.add("extype");
		trans_unitSet.add("maxbytes");
		trans_unitSet.add("minbytes");
		trans_unitSet.add("size-unit");
		trans_unitSet.add("maxheight");
		trans_unitSet.add("minheight");
		trans_unitSet.add("maxwidth");
		trans_unitSet.add("minwidth");
		trans_unitSet.add("charclass");
		attributesTable.put("trans-unit", trans_unitSet);

		Set<String> sourceSet = new HashSet<>();
		sourceSet.add("ts");
		attributesTable.put("source", sourceSet);

		Set<String> targetSet = new HashSet<>();
		targetSet.add("state");
		targetSet.add("state-qualifier");
		targetSet.add("phase-name");
		targetSet.add("ts");
		targetSet.add("restype");
		targetSet.add("resname");
		targetSet.add("coord");
		targetSet.add("font");
		targetSet.add("css-style");
		targetSet.add("style");
		targetSet.add("exstyle");
		if (!version.equals("1.1")) {
			targetSet.add("equiv-trans");
		}
		attributesTable.put("target", targetSet);

		Set<String> alt_transSet = new HashSet<>();
		if (!version.equals("1.1")) {
			alt_transSet.add("mid");
		}
		alt_transSet.add("match-quality");
		alt_transSet.add("tool");
		alt_transSet.add("tool-id");
		alt_transSet.add("crc");
		alt_transSet.add("datatype");
		alt_transSet.add("ts");
		alt_transSet.add("restype");
		alt_transSet.add("resname");
		alt_transSet.add("extradata");
		alt_transSet.add("help-id");
		alt_transSet.add("menu");
		alt_transSet.add("menu-option");
		alt_transSet.add("menu-name");
		alt_transSet.add("coord");
		alt_transSet.add("font");
		alt_transSet.add("css-style");
		alt_transSet.add("style");
		alt_transSet.add("exstyle");
		alt_transSet.add("extype");
		alt_transSet.add("origin");
		if (!version.equals("1.1")) {
			alt_transSet.add("phase-name");
			alt_transSet.add("alttranstype");
		}
		attributesTable.put("alt-trans", alt_transSet);

		Set<String> bin_unitSet = new HashSet<>();
		bin_unitSet.add("id");
		bin_unitSet.add("mime-type");
		bin_unitSet.add("approved");
		bin_unitSet.add("translate");
		bin_unitSet.add("reformat");
		bin_unitSet.add("ts");
		bin_unitSet.add("phase-name");
		bin_unitSet.add("restype");
		bin_unitSet.add("resname");
		attributesTable.put("bin-unit", bin_unitSet);

		Set<String> bin_sourceSet = new HashSet<>();
		bin_sourceSet.add("ts");
		attributesTable.put("bin-source", bin_sourceSet);

		Set<String> bin_targetSet = new HashSet<>();
		bin_targetSet.add("mime-type");
		bin_targetSet.add("ts");
		bin_targetSet.add("state");
		bin_targetSet.add("phase-name");
		bin_targetSet.add("restype");
		bin_targetSet.add("resname");
		if (!version.equals("1.1")) {
			bin_targetSet.add("state-qualifier");
		}
		attributesTable.put("bin-target", bin_targetSet);

		if (!version.equals("1.1")) {
			Set<String> seg_sourceSet = new HashSet<>();
			seg_sourceSet.add("ts");
			attributesTable.put("seg-source", seg_sourceSet);
		}

		Set<String> gSet = new HashSet<>();
		gSet.add("id");
		gSet.add("ctype");
		gSet.add("ts");
		gSet.add("clone");
		gSet.add("xid");
		if (!version.equals("1.1")) {
			gSet.add("equiv-text");
		}
		attributesTable.put("g", gSet);

		Set<String> xSet = new HashSet<>();
		xSet.add("id");
		xSet.add("ctype");
		xSet.add("ts");
		xSet.add("clone");
		xSet.add("xid");
		if (!version.equals("1.1")) {
			xSet.add("equiv-text");
		}
		attributesTable.put("x", xSet);

		Set<String> bxSet = new HashSet<>();
		bxSet.add("id");
		bxSet.add("rid");
		bxSet.add("ctype");
		bxSet.add("ts");
		bxSet.add("clone");
		bxSet.add("xid");
		if (!version.equals("1.1")) {
			bxSet.add("equiv-text");
		}
		attributesTable.put("bx", bxSet);

		Set<String> exSet = new HashSet<>();
		exSet.add("id");
		exSet.add("rid");
		exSet.add("ts");
		exSet.add("xid");
		if (!version.equals("1.1")) {
			exSet.add("equiv-text");
		}
		attributesTable.put("ex", exSet);

		Set<String> phSet = new HashSet<>();
		phSet.add("id");
		phSet.add("ctype");
		phSet.add("ts");
		phSet.add("crc");
		phSet.add("assoc");
		phSet.add("xid");
		if (!version.equals("1.1")) {
			phSet.add("equiv-text");
		}
		attributesTable.put("ph", phSet);

		Set<String> bptSet = new HashSet<>();
		bptSet.add("id");
		bptSet.add("rid");
		bptSet.add("ctype");
		bptSet.add("ts");
		bptSet.add("crc");
		bptSet.add("xid");
		if (!version.equals("1.1")) {
			bptSet.add("equiv-text");
		}
		attributesTable.put("bpt", bptSet);

		Set<String> eptSet = new HashSet<>();
		eptSet.add("id");
		eptSet.add("rid");
		eptSet.add("ts");
		eptSet.add("crc");
		eptSet.add("xid");
		if (!version.equals("1.1")) {
			eptSet.add("equiv-text");
		}
		attributesTable.put("ept", eptSet);

		Set<String> itSet = new HashSet<>();
		itSet.add("id");
		itSet.add("pos");
		itSet.add("rid");
		itSet.add("ctype");
		itSet.add("ts");
		itSet.add("crc");
		itSet.add("xid");
		if (!version.equals("1.1")) {
			itSet.add("equiv-text");
		}
		attributesTable.put("it", itSet);

		Set<String> subSet = new HashSet<>();
		subSet.add("datatype");
		subSet.add("ctype");
		subSet.add("xid");
		attributesTable.put("sub", subSet);

		Set<String> mrkSet = new HashSet<>();
		mrkSet.add("mtype");
		mrkSet.add("mid");
		mrkSet.add("ts");
		mrkSet.add("comment");
		attributesTable.put("mrk", mrkSet);
	}

	public boolean recurse(Element e) {
		if (!e.getNamespace().isEmpty()) {
			// ignore non-XLIFF elements
			return true;
		}
		String namespace = e.getAttributeValue("xmlns");
		if (!namespace.isEmpty() && !xliffNamespaces.contains(namespace)) {
			// ignore element from another namespace
			return true;
		}
		
		if (!"1.0".equals(version)) {
			// validate the attributes (the parser can't do it due to bugs in the schemas
			List<Attribute> atts = e.getAttributes();
			for (int i = 0; i < atts.size(); i++) {
				Attribute att = atts.get(i);
				if ("xmlns".equals(att.getLocalName())) {
					// attribute from XML standard
					continue;
				}
				if (!att.getNamespace().isEmpty()) {
					// attribute from another namespace
					continue;
				}
				Set<String> set = attributesTable.get(e.getLocalName());
				if (set != null && !set.contains(att.getLocalName())) {
					MessageFormat mf = new MessageFormat("Invalid attribute in <{0}>: {1}.");
					reason = mf.format(new Object[] { e.getName(), att.getName() });
					return false;
				}
			}
		}
		// "date" attributes must be in ISO 8601 format
		if (e.getLocalName().equals("file") || e.getLocalName().equals("phase")) {
			String date = e.getAttributeValue("date");
			if (!date.isEmpty() && !checkDate(date)) {
				MessageFormat mf = new MessageFormat("Invalid date: {0}");
				reason = mf.format(new Object[] { date });
				return false;
			}
		}

		// external files should be resolvable resources
		if (e.getLocalName().equals("external-file") && !checkURL(e.getAttributeValue("href"))) {
			MessageFormat mf = new MessageFormat("Invalid URI: {0}");
			reason = mf.format(new Object[] { e.getAttribute("href") });
			return false;
		}

		// source file should be resolvable resources
		if (e.getLocalName().equals("file") && !checkURL(e.getAttributeValue("original"))) {
			MessageFormat mf = new MessageFormat("Invalid URI: {0}");
			reason = mf.format(new Object[] { e.getAttribute("original") });
			return false;
		}

		// language codes should be valid ISO codes
		if (e.getLocalName().equals("xliff") || e.getLocalName().equals("note") || e.getLocalName().equals("prop")
				|| e.getLocalName().equals("source") || e.getLocalName().equals("target")
				|| e.getLocalName().equals("alt-trans") || e.getLocalName().equals("seg-source")) {
			String lang = e.getAttributeValue("xml:lang");
			if (!lang.equals("") && !checkLanguage(lang)) {
				MessageFormat mf = new MessageFormat("Invalid language: {0}");
				reason = mf.format(new Object[] { lang });
				return false;
			}
		}

		if (e.getLocalName().equals("file")) {
			// create tables to make sure that "id" attributes are unique within the <file>
			ids = new Hashtable<>();
			groupIds = new Hashtable<>();

			// create tables to check that <it> tags have both start and end positions
			srcItTable = new Hashtable<>();
			tgtItTable = new Hashtable<>();

			// create table to check if "xid" points to valid <trans-unit>
			xids = new Hashtable<>();

			// check <phase> and <tool> elements
			phasesTable = new Hashtable<>();
			toolsTable = new Hashtable<>();

			// check language codes used
			sourceLanguage = e.getAttributeValue("source-language");
			if (!checkLanguage(sourceLanguage)) {
				MessageFormat mf = new MessageFormat("Invalid source language: {0}");
				reason = mf.format(new Object[] { sourceLanguage });
				return false;
			}
			targetLanguage = e.getAttributeValue("target-language");
			if (!targetLanguage.equals("")) {
				if (!checkLanguage(targetLanguage)) {
					MessageFormat mf = new MessageFormat("Invalid target language: {0}");
					reason = mf.format(new Object[] { targetLanguage });
					return false;
				}
			}
		}

		if (e.getLocalName().equals("source")) {
			inSource = true;
		}

		// store phase name
		if (e.getLocalName().equals("phase")) {
			String name = e.getAttributeValue("phase-name");
			if (!phasesTable.containsKey(name)) {
				phasesTable.put(name, "");
			} else {
				MessageFormat mf = new MessageFormat("Duplicated \"name\" in <phase>: {0}");
				reason = mf.format(new Object[] { name });
				return false;
			}
		}

		// store tool-id
		if (e.getLocalName().equals("tool")) {
			String id = e.getAttributeValue("tool-id");
			if (!toolsTable.containsKey(id)) {
				toolsTable.put(id, "");
			} else {
				MessageFormat mf = new MessageFormat("Duplicated \"tool-id\" in <tool>: {0}");
				reason = mf.format(new Object[] { id });
				return false;
			}
		}

		if (e.getLocalName().equals("alt-trans")) {
			// language codes in <alt-trans> are independent from those declared in <file>
			inAltTrans = true;

			// check for valid segment reference
			String mid = e.getAttributeValue("mid");
			if (!mid.equals("")) {
				if (!midTable.containsKey(mid)) {
					reason = "Incorrect segment referenced in <alt-trans> element.";
					return false;
				}
			}

			// check for declared <tool>
			String tool = e.getAttributeValue("tool-id");
			if (!tool.equals("")) {
				if (!toolsTable.containsKey(tool)) {
					reason = "Undeclared <tool> referenced in <alt-trans> element.";
					return false;
				}
			}
			// create tables to check if <it> tags are duplicated
			altSrcItTable = new Hashtable<>();
			altTgtItTable = new Hashtable<>();
		}

		// validate "phase-name" attribute
		if (e.getLocalName().equals("count") || e.getLocalName().equals("trans-unit")
				|| e.getLocalName().equals("bin-unit") || e.getLocalName().equals("target")
				|| e.getLocalName().equals("bin-target") || e.getLocalName().equals("alt-trans")) {
			String phase = e.getAttributeValue("phase-name");
			if (!phase.equals("")) {
				if (!phasesTable.containsKey(phase)) {
					reason = "Undeclared <phase> referenced in <alt-trans> element.";
					return false;
				}
			}
		}

		// check language code in <source> and <target>
		if (e.getLocalName().equals("source") && !inAltTrans) {
			String lang = e.getAttributeValue("xml:lang");
			if (!lang.equals("") && !lang.equalsIgnoreCase(sourceLanguage)) {
				MessageFormat mf = new MessageFormat("Found <source> with wrong language code: {0}");
				reason = mf.format(new Object[] { lang });
				return false;
			}
		}
		if (e.getLocalName().equals("target") && !inAltTrans) {
			String lang = e.getAttributeValue("xml:lang");
			if (targetLanguage.equals("") && lang.equals("")) {
				// Missing target language code
				// bad practice, but legal
			}
			if (!targetLanguage.equals("") && !lang.equals("") && !lang.equalsIgnoreCase(targetLanguage)) {
				MessageFormat mf = new MessageFormat("Found <target> with wrong language code: {0}");
				reason = mf.format(new Object[] { lang });
				return false;
			}
		}

		// check for unique "id" in <trans-unit> and <bin-unit>
		if (version.equals("1.2") && (e.getLocalName().equals("trans-unit") || e.getLocalName().equals("bin-unit"))) { 
			String id = e.getAttributeValue("id");
			if (!ids.containsKey(id)) {
				ids.put(id, "");
			} else {
				MessageFormat mf = new MessageFormat("Duplicated \"id\" in <trans-unit>: {0}");
				reason = mf.format(new Object[] { id });
				return false;
			}
		}

		// initialize table for checking "mid" attribute in <alt-trans>
		if (e.getLocalName().equals("trans-unit")) {
			midTable = new Hashtable<>();
		}

		if (e.getLocalName().equals("seg-source")) {
			// entering <seg-source>
			inSegSource = true;
		}

		// check segment ids
		if (e.getLocalName().equals("mrk") && e.getAttributeValue("mtype").equals("seg")) { 
			String mid = e.getAttributeValue("mid");
			if (inSegSource) {
				if (!midTable.containsKey(mid)) {
					midTable.put(mid, "");
				} else {
					MessageFormat mf = new MessageFormat("Duplicated \"mid\" in <mrk>: {0}");
					reason = mf.format(new Object[] { mid });
					return false;
				}
			} else {
				// in <target>
				if (!midTable.containsKey(mid)) {
					reason = "Incorrect segment referenced in <target> element.";
					return false;
				}
			}
		}

		// check for unique "id" in <group>
		if (e.getLocalName().equals("group") && version.equals("1.2")) {
			String id = e.getAttributeValue("id");
			if (!id.equals("")) {
				if (!groupIds.containsKey(id)) {
					groupIds.put(id, "");
				} else {
					MessageFormat mf = new MessageFormat("Duplicated \"id\" in <group>: {0}");
					reason = mf.format(new Object[] { id });
					return false;
				}
			}
		}

		// initialize tables for checking matched pairs of inline elements
		if (e.getLocalName().equals("source") || e.getLocalName().equals("seg-source")
				|| e.getLocalName().equals("target")) {
			bxTable = new Hashtable<>();
			exTable = new Hashtable<>();
			bptTable = new Hashtable<>();
			eptTable = new Hashtable<>();
			inlineIds = new Hashtable<>();
		}

		// check for unique id at <source>, <seg-source> and <target> level
		if (e.getLocalName().equals("bx") || e.getLocalName().equals("ex") || e.getLocalName().equals("bpt")
				|| e.getLocalName().equals("ept") || e.getLocalName().equals("ph")) {
			String id = e.getAttributeValue("id");
			if (!inlineIds.contains(e.getLocalName() + id)) {
				inlineIds.put(e.getLocalName() + id, "");
			} else {
				MessageFormat mf = new MessageFormat("Found <{0}> with duplicated \"id\".");
				reason = mf.format(new Object[] { e.getLocalName() });
				return false;
			}
		}

		// check for paired <it> tags in <file>
		if (e.getLocalName().equals("it") && !inAltTrans) {
			String id = e.getAttributeValue("id");
			String pos = e.getAttributeValue("pos");
			if (inSource) {
				if (!srcItTable.containsKey(id)) {
					srcItTable.put(id, pos);
				} else {
					if (!srcItTable.get(id).equals(pos)) {
						// matched
						srcItTable.remove(id);
					} else {
						// duplicated
						reason = "Found duplicated <it> element.";
						return false;
					}
				}
			} else {
				if (!tgtItTable.containsKey(id)) {
					tgtItTable.put(id, pos);
				} else {
					if (!tgtItTable.get(id).equals(pos)) {
						// matched
						tgtItTable.remove(id);
					} else {
						// duplicated
						reason = "Found duplicated <it> element.";
						return false;
					}
				}
			}
		}
		// check for duplicated <it> tags in <alt-trans>
		if (e.getLocalName().equals("it") && inAltTrans) {
			String id = e.getAttributeValue("id");
			String pos = e.getAttributeValue("pos");
			if (inSource) {
				if (!altSrcItTable.containsKey(id)) {
					altSrcItTable.put(id, pos);
				} else {
					if (altSrcItTable.get(id).equals(pos)) {
						// duplicated
						reason = "Found duplicated <it> element.";
						return false;
					}
				}
			} else {
				if (!altTgtItTable.containsKey(id)) {
					altTgtItTable.put(id, pos);
				} else {
					if (altTgtItTable.get(id).equals(pos)) {
						// duplicated
						reason = "Found duplicated <it> element.";
						return false;
					}
				}
			}
		}
		// check if "xid" attribute points to a valid <trans-unit> or <bin-unit>
		if (e.getLocalName().equals("ph") || e.getLocalName().equals("it") || e.getLocalName().equals("sub")
				|| e.getLocalName().equals("bx") || e.getLocalName().equals("ex") || e.getLocalName().equals("bpt")
				|| e.getLocalName().equals("ept") || e.getLocalName().equals("g") || e.getLocalName().equals("x")) {
			String xid = e.getAttributeValue("xid");
			if (!xid.equals("")) {
				xids.put(xid, "");
			}
		}

		if (e.getLocalName().equals("bx")) {
			String id = e.getAttributeValue("rid");
			if (id.equals("")) {
				id = e.getAttributeValue("id");
			}
			if (id.equals("")) {
				reason = "Found <bx> without \"rid\" or \"id\" attributes.";
				return false;
			}
			bxTable.put(id, "");
		}
		if (e.getLocalName().equals("ex")) {
			String id = e.getAttributeValue("rid");
			if (id.equals("")) {
				id = e.getAttributeValue("id");
			}
			if (id.equals("")) {
				reason = "Found <ex> without \"rid\" or \"id\" attributes.";
				return false;
			}
			exTable.put(id, "");
		}
		if (e.getLocalName().equals("bpt")) {
			String id = e.getAttributeValue("rid");
			if (id.equals("")) {
				id = e.getAttributeValue("id");
			}
			if (id.equals("")) {
				reason = "Found <bpt> without \"rid\" or \"id\" attributes.";
				return false;
			}
			bptTable.put(id, "");
		}
		if (e.getLocalName().equals("ept")) {
			String id = e.getAttributeValue("rid");
			if (id.equals("")) {
				id = e.getAttributeValue("id");
			}
			if (id.equals("")) {
				reason = "Found <ept> without \"rid\" or \"id\" attributes.";
				return false;
			}
			eptTable.put(id, "");
		}

		// Recurse all children
		List<Element> children = e.getChildren();
		for (int i = 0; i < children.size(); i++) {
			boolean result = recurse(children.get(i));
			if (result == false) {
				return false;
			}
		}

		// check if inline tags are paired at <source>, <seg-source> and <target>
		if (e.getLocalName().equals("source") || e.getLocalName().equals("seg-source")
				|| e.getLocalName().equals("target")) {
			Enumeration<String> keys = bxTable.keys();
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();
				if (exTable.containsKey(key)) {
					exTable.remove(key);
					bxTable.remove(key);
				}
			}

			if (exTable.size() > 0 || bxTable.size() > 0) {
				MessageFormat mf = new MessageFormat("Unmatched <ex>/<bx> in <{0}>.");
				reason = mf.format(new Object[] { e.getLocalName() });
				return false;
			}
			keys = bptTable.keys();
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();
				if (eptTable.containsKey(key)) {
					eptTable.remove(key);
					bptTable.remove(key);
				}
			}
			if (eptTable.size() > 0 || bptTable.size() > 0) {
				MessageFormat mf = new MessageFormat("Unmatched <bpt>/<ept> in <{0}>.");
				reason = mf.format(new Object[] { e.getLocalName() });
				return false;
			}

		}

		// check for not paired <it> tags in <file>
		if (e.getLocalName().equals("file") && srcItTable.size() + tgtItTable.size() > 0) {
			reason = "Unmatched <it> in <file>.";
			return false;
		}

		// check for missing <trans-unite> referenced in <sub>
		if (e.getLocalName().equals("file")) {
			Enumeration<String> keys = xids.keys();
			while (keys.hasMoreElements()) {
				if (!ids.containsKey(keys.nextElement())) {
					reason = "Incorrect segment referenced in \"xid\" attribute.";
					return false;
				}
			}
		}

		if (e.getLocalName().equals("alt-trans")) {
			// leaving an <alt-trans> element
			inAltTrans = false;
		}

		if (e.getLocalName().equals("seg-source")) {
			// leaving <seg-source>
			inSegSource = false;
		}

		if (e.getLocalName().equals("source")) {
			// leaving <source>
			inSource = false;
		}

		// all seems fine so far
		return true;
	}

	private static boolean checkDate(String date) {

		// YYYY-MM-DDThh:mm:ssZ
		if (date.length() != 20) {
			return false;
		}
		if (date.charAt(4) != '-') {
			return false;
		}
		if (date.charAt(7) != '-') {
			return false;
		}
		if (date.charAt(10) != 'T') {
			return false;
		}
		if (date.charAt(13) != ':') {
			return false;
		}
		if (date.charAt(16) != ':') {
			return false;
		}
		if (date.charAt(19) != 'Z') {
			return false;
		}
		try {
			int year = Integer.parseInt("" + date.charAt(0) + date.charAt(1) + date.charAt(2) + date.charAt(3));
			if (year < 0) {
				return false;
			}
			int month = Integer.parseInt("" + date.charAt(5) + date.charAt(6));
			if (month < 1 || month > 12) {
				return false;
			}
			int day = Integer.parseInt("" + date.charAt(8) + date.charAt(9));
			switch (month) {
			case 1:
			case 3:
			case 5:
			case 7:
			case 8:
			case 10:
			case 12:
				if (day < 1 || day > 31) {
					return false;
				}
				break;
			case 4:
			case 6:
			case 9:
			case 11:
				if (day < 1 || day > 30) {
					return false;
				}
				break;
			case 2:
				// check for leap years
				if (year % 4 == 0) {
					if (year % 100 == 0) {
						// not all centuries are leap years
						if (year % 400 == 0) {
							if (day < 1 || day > 29) {
								return false;
							}
						} else {
							// not leap year
							if (day < 1 || day > 28) {
								return false;
							}
						}
					}
					if (day < 1 || day > 29) {
						return false;
					}
				} else if (day < 1 || day > 28) {
					return false;
				}
				break;
			default:
				// wrong month
				return false;
			}
			int hour = Integer.parseInt("" + date.charAt(11) + date.charAt(12));
			if (hour < 0 || hour > 23) {
				return false;
			}
			int min = Integer.parseInt("" + date.charAt(14) + date.charAt(15));
			if (min < 0 || min > 59) {
				return false;
			}
			int sec = Integer.parseInt("" + date.charAt(17) + date.charAt(18));
			if (sec < 0 || sec > 59) {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	@SuppressWarnings("unused")
	private static boolean checkURL(String string) {
		try {
			new URL(string);
			return true;
		} catch (Exception e1) {
			try {
				new File(string);
				return true;
			} catch (Exception e2) {
				return false;
			}
		}
	}

	private boolean checkLanguage(String lang) {
		if (lang.startsWith("x-") || lang.startsWith("X-")) {
			// custom language code
			return true;
		}
		return !registry.getTagDescription(lang).isEmpty();
	}

}
