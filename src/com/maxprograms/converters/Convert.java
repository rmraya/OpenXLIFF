/*******************************************************************************
 * Copyright (c) 2003, 2018 Maxprograms.
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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.Vector;
import java.lang.System.Logger.Level;
import java.lang.System.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.converters.ditamap.DitaMap2Xliff;
import com.maxprograms.converters.html.Html2Xliff;
import com.maxprograms.converters.idml.Idml2Xliff;
import com.maxprograms.converters.javaproperties.Properties2Xliff;
import com.maxprograms.converters.javascript.Jscript2xliff;
import com.maxprograms.converters.mif.Mif2Xliff;
import com.maxprograms.converters.office.Office2Xliff;
import com.maxprograms.converters.plaintext.Text2Xliff;
import com.maxprograms.converters.po.Po2Xliff;
import com.maxprograms.converters.rc.Rc2Xliff;
import com.maxprograms.converters.resx.Resx2Xliff;
import com.maxprograms.converters.sdlxliff.Sdl2Xliff;
import com.maxprograms.converters.ts.Ts2Xliff;
import com.maxprograms.converters.txml.Txml2Xliff;
import com.maxprograms.converters.xml.Xml2Xliff;
import com.maxprograms.xliff2.ToXliff2;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.Indenter;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.XMLNode;
import com.maxprograms.xml.XMLOutputter;

public class Convert {

	private static final Logger LOGGER = System.getLogger(Convert.class.getName());

	public static void main(String[] args) {

		args = fixPath(args);

		String source = "";
		String type = "";
		String enc = "";
		String srcLang = "";
		String tgtLang = "";
		String srx = "";
		String skl = "";
		String xliff = "";
		String catalog = "";
		String ditaval = "";
		boolean embed = false;
		boolean paragraph = false;
		boolean xliff20 = false;

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.equals("-version")) {
				LOGGER.log(Level.INFO, () -> "Version: " + Constants.VERSION + " Build: " + Constants.BUILD);
				return;
			}
			if (arg.equals("-help")) {
				help();
				return;
			}
			if (arg.equals("-charsets")) {
				listCharsets();
				return;
			}
			if (arg.equals("-file") && (i + 1) < args.length) {
				source = args[i + 1];
			}
			if (arg.equals("-type") && (i + 1) < args.length) {
				type = args[i + 1];
			}
			if (arg.equals("-enc") && (i + 1) < args.length) {
				enc = args[i + 1];
			}
			if (arg.equals("-srcLang") && (i + 1) < args.length) {
				srcLang = args[i + 1];
			}
			if (arg.equals("-tgtLang") && (i + 1) < args.length) {
				tgtLang = args[i + 1];
			}
			if (arg.equals("-srx") && (i + 1) < args.length) {
				srx = args[i + 1];
			}
			if (arg.equals("-skl") && (i + 1) < args.length) {
				skl = args[i + 1];
			}
			if (arg.equals("-xliff") && (i + 1) < args.length) {
				xliff = args[i + 1];
			}
			if (arg.equals("-catalog") && (i + 1) < args.length) {
				catalog = args[i + 1];
			}
			if (arg.equals("-ditaval") && (i + 1) < args.length) {
				ditaval = args[i + 1];
			}
			if (arg.equals("-embed")) {
				embed = true;
			}
			if (arg.equals("-paragraph")) {
				paragraph = true;
			}
			if (arg.equals("-2.0")) {
				xliff20 = true;
			}
		}
		if (args.length < 4) {
			help();
			return;
		}
		if (source.isEmpty()) {
			LOGGER.log(Level.ERROR, "Missing '-file' parameter.");
			return;
		}
		File sourceFile = new File(source);
		if (!sourceFile.exists()) {
			LOGGER.log(Level.ERROR, "Source file does not exist.");
			return;
		}
		if (type.isEmpty()) {
			String detected = FileFormats.detectFormat(source);
			if (detected != null) {
				type = FileFormats.getShortName(detected);
				LOGGER.log(Level.INFO, "Auto-detected type: " + type);
			} else {
				LOGGER.log(Level.ERROR, "Unable to auto-detect file format. Use '-type' parameter.");
				return;
			}
		}
		type = FileFormats.getFullName(type);
		if (type == null) {
			LOGGER.log(Level.ERROR, "Unknown file format.");
			return;
		}
		if (enc.isEmpty()) {
			Charset charset = EncodingResolver.getEncoding(source, type);
			if (charset != null) {
				enc = charset.name();
				LOGGER.log(Level.INFO, "Auto-detected encoding: " + enc);
			} else {
				LOGGER.log(Level.ERROR, "Unable to auto-detect character set. Use '-enc' parameter.");
				return;
			}
		}
		String[] encodings = EncodingResolver.getPageCodes();
		if (!Arrays.asList(encodings).contains(enc)) {
			LOGGER.log(Level.ERROR, "Unsupported encoding.");
			return;
		}
		if (srcLang.isEmpty()) {
			LOGGER.log(Level.ERROR, "Missing '-srcLang' parameter.");
			return;
		}
		try {
			if (!Utils.isValidLanguage(srcLang)) {
				LOGGER.log(Level.WARNING, "'" + srcLang + "' is not a valid language code.");
			}
			if (!tgtLang.isEmpty() && !Utils.isValidLanguage(tgtLang)) {
				LOGGER.log(Level.WARNING, "'" + tgtLang + "' is not a valid language code.");
			}
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, "Error validating languages.", e);
			return;
		}
		if (srx.isEmpty()) {
			File srxFolder = new File(new File(System.getProperty("user.dir")), "srx");
			srx = new File(srxFolder, "default.srx").getAbsolutePath();
		}
		File srxFile = new File(srx);
		if (!srxFile.exists()) {
			LOGGER.log(Level.ERROR, "SRX file does not exist.");
			return;
		}
		if (catalog.isEmpty()) {
			File catalogFolder = new File(new File(System.getProperty("user.dir")), "catalog");
			catalog = new File(catalogFolder, "catalog.xml").getAbsolutePath();
		}
		File catalogFile = new File(catalog);
		if (!catalogFile.exists()) {
			LOGGER.log(Level.ERROR, "Catalog file does not exist.");
			return;
		}
		if (skl.isEmpty()) {
			skl = sourceFile.getAbsolutePath() + ".skl";
		}
		if (xliff.isEmpty()) {
			xliff = sourceFile.getAbsolutePath() + ".xlf";
		}
		Hashtable<String, String> params = new Hashtable<>();
		params.put("source", source);
		params.put("xliff", xliff);
		params.put("skeleton", skl);
		params.put("format", type);
		params.put("catalog", catalog);
		params.put("srcEncoding", enc);
		params.put("paragraph", paragraph ? "yes" : "no");
		params.put("srxFile", srx);
		params.put("srcLang", srcLang);
		if (!tgtLang.isEmpty()) {
			params.put("tgtLang", tgtLang);
		}
		if (type.equals(FileFormats.getShortName(FileFormats.DITA)) && !ditaval.isEmpty()) {
			params.put("ditaval", ditaval);
		}
		Vector<String> result = run(params);
		if ("0".equals(result.get(0))) {
			if (embed) {
				try {
					addSkeleton(xliff, catalog);
				} catch (SAXException | IOException | ParserConfigurationException e) {
					LOGGER.log(Level.ERROR, "Error embedding skeleton.", e);
					return;
				}
			}
			if (xliff20) {
				result = ToXliff2.run(new File(xliff), catalog);
				if (!"0".equals(result.get(0))) {
					LOGGER.log(Level.ERROR, result.get(1));
				}
			}
		} else {
			LOGGER.log(Level.ERROR, result.get(1));
		}
	}

	protected static String[] fixPath(String[] args) {
		Vector<String> result = new Vector<>();
		String current = "";
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.startsWith("-")) {
				if (!current.isEmpty()) {
					result.add(current.trim());
					current = "";
				}
				result.add(arg);
			} else {
				current = current + " " + arg;
			}
		}
		if (!current.isEmpty()) {
			result.add(current.trim());
		}
		return result.toArray(new String[result.size()]);
	}

	private static void help() {
		String launcher = "   convert.sh ";
		if (System.getProperty("file.separator").equals("\\")) {
			launcher = "   convert.bat ";
		}
		String help = "Usage:\n\n" + launcher
				+ "[-help] [-version] -file sourceFile -srcLang sourceLang [-tgtLang targetLang] "
				+ "[-skl skeletonFile] [-xliff xliffFile] " + "[-type fileType] [-enc encoding] [-srx srxFile] "
				+ "[-catalog catalogFile] [-divatal ditaval] " + "[-embed] [-paragraph] [-2.0] [-charsets]\n\n"
				+ "Where:\n\n" 
				+ "   -help:      (optional) Display this help information and exit\n"
				+ "   -version:   (optional) Display version & build information and exit\n"
				+ "   -file:      source file to convert\n" 
				+ "   -srgLang:   source language code\n"
				+ "   -tgtLang:   (optional) target language code\n"
				+ "   -xliff:     (optional) XLIFF file to generate\n"
				+ "   -skl:       (optional) skeleton file to generate\n" 
				+ "   -type:      (optional) document type\n"
				+ "   -enc:       (optional) character set code for the source file\n"
				+ "   -srx:       (optional) SRX file to use for segmentation\n"
				+ "   -catalog:   (optional) XML catalog to use for processing\n"
				+ "   -ditaval:   (optional) conditional processing file to use when converting DITA maps\n"
				+ "   -embed:     (optional) store skeleton inside the XLIFF file\n"
				+ "   -paragraph: (optional) use paragraph segmentation\n"
				+ "   -2.0:       (optional) generate XLIFF 2.0\n"
				+ "   -charsets:  (optional) display a list of available character sets and exit\n\n"
				+ "Document Types\n\n" 
				+ "   INX = Adobe InDesign Interchange\n" 
				+ "   IDML = Adobe InDesign IDML\n"
				+ "   DITA = DITA Map\n" 
				+ "   HTML = HTML Page\n" 
				+ "   JS = JavaScript\n"
				+ "   JAVA = Java Properties\n" 
				+ "   MIF = MIF (Maker Interchange Format)\n"
				+ "   OFF = Microsoft Office 2007 Document\n" 
				+ "   OO = OpenOffice Document\n"
				+ "   PO = PO (Portable Objects)\n" 
				+ "   RC = RC (Windows C/C++ Resources)\n"
				+ "   RESX = ResX (Windows .NET Resources)\n" 
				+ "   SDLXLIFF = SDLXLIFF Document\n"
				+ "   TEXT = Plain Text\n" 
				+ "   TS = TS (Qt Linguist translation source)\n"
				+ "   TXML = TXML Document\n" 
				+ "   XML = XML Document\n" 
				+ "   XMLG = XML (Generic)\n";
		System.out.println(help);
	}

	private static void addSkeleton(String fileName, String catalog)
			throws SAXException, IOException, ParserConfigurationException {
		SAXBuilder builder = new SAXBuilder();
		builder.setEntityResolver(new Catalog(catalog));
		Document doc = builder.build(fileName);
		Element root = doc.getRootElement();
		List<Element> files = root.getChildren("file");
		Iterator<Element> it = files.iterator();
		Set<String> deleted = new HashSet<>();
		while (it.hasNext()) {
			Element file = it.next();
			Element header = file.getChild("header");
			Element skl = header.getChild("skl");
			Element external = skl.getChild("external-file");
			String sklName = external.getAttributeValue("href");
			sklName = sklName.replaceAll("&amp;", "&");
			sklName = sklName.replaceAll("&lt;", "<");
			sklName = sklName.replaceAll("&gt;", ">");
			sklName = sklName.replaceAll("&apos;", "\'");
			sklName = sklName.replaceAll("&quot;", "\"");
			if (!deleted.contains(sklName)) {
				File skeleton = new File(sklName);
				Element internal = new Element("internal-file");
				internal.setAttribute("form", "base64");
				internal.addContent(Utils.encodeFromFile(skeleton.getAbsolutePath()));
				skl.setContent(new Vector<XMLNode>());
				skl.addContent(internal);
				Files.delete(Paths.get(skeleton.toURI()));
				deleted.add(sklName);
			}
		}
		XMLOutputter outputter = new XMLOutputter();
		Indenter.indent(root, 2);
		outputter.preserveSpace(true);
		try (FileOutputStream out = new FileOutputStream(fileName)) {
			outputter.output(doc, out);
		}
	}

	public static Vector<String> run(Hashtable<String, String> params) {
		Vector<String> result = new Vector<>();
		String format = params.get("format");
		if (format.equals(FileFormats.INX)) {
			params.put("InDesign", "yes");
			result = Xml2Xliff.run(params);
		} else if (format.equals(FileFormats.IDML)) {
			result = Idml2Xliff.run(params);
		} else if (format.equals(FileFormats.DITA)) {
			result = DitaMap2Xliff.run(params);
		} else if (format.equals(FileFormats.HTML)) {
			File folder = new File(System.getProperty("user.dir"), "xmlfilter");
			params.put("iniFile", new File(folder, "init_html.xml").getAbsolutePath());
			result = Html2Xliff.run(params);
		} else if (format.equals(FileFormats.JS)) {
			result = Jscript2xliff.run(params);
		} else if (format.equals(FileFormats.JAVA)) {
			result = Properties2Xliff.run(params);
		} else if (format.equals(FileFormats.MIF)) {
			result = Mif2Xliff.run(params);
		} else if (format.equals(FileFormats.OO) || format.equals(FileFormats.OFF)) {
			result = Office2Xliff.run(params);
		} else if (format.equals(FileFormats.PO)) {
			result = Po2Xliff.run(params);
		} else if (format.equals(FileFormats.RC)) {
			result = Rc2Xliff.run(params);
		} else if (format.equals(FileFormats.RESX)) {
			result = Resx2Xliff.run(params);
		} else if (format.equals(FileFormats.SDLXLIFF)) {
			result = Sdl2Xliff.run(params);
		} else if (format.equals(FileFormats.TEXT)) {
			result = Text2Xliff.run(params);
		} else if (format.equals(FileFormats.TS)) {
			result = Ts2Xliff.run(params);
		} else if (format.equals(FileFormats.TXML)) {
			result = Txml2Xliff.run(params);
		} else if (format.equals(FileFormats.XML)) {
			result = Xml2Xliff.run(params);
		} else if (format.equals(FileFormats.XMLG)) {
			params.put("generic", "yes");
			result = Xml2Xliff.run(params);
		} else {
			result.add("1");
			result.add("Unknown file format.");
		}
		return result;
	}

	private static void listCharsets() {
		SortedMap<String, Charset> available = Charset.availableCharsets();
		Set<String> keySet = available.keySet();
		Iterator<String> it = keySet.iterator();
		while (it.hasNext()) {
			Charset charset = available.get(it.next());
			System.out.println(charset.displayName());
		}
	}
}
