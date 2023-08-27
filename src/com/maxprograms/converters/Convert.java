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
package com.maxprograms.converters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.xml.parsers.ParserConfigurationException;

import com.maxprograms.converters.ditamap.DitaMap2Xliff;
import com.maxprograms.converters.html.Html2Xliff;
import com.maxprograms.converters.idml.Idml2Xliff;
import com.maxprograms.converters.idml.Story2Xliff;
import com.maxprograms.converters.javaproperties.Properties2Xliff;
import com.maxprograms.converters.javascript.Jscript2xliff;
import com.maxprograms.converters.json.Json2Xliff;
import com.maxprograms.converters.mif.Mif2Xliff;
import com.maxprograms.converters.office.Office2Xliff;
import com.maxprograms.converters.php.Php2Xliff;
import com.maxprograms.converters.plaintext.Text2Xliff;
import com.maxprograms.converters.po.Po2Xliff;
import com.maxprograms.converters.rc.Rc2Xliff;
import com.maxprograms.converters.resx.Resx2Xliff;
import com.maxprograms.converters.sdlppx.Sdlppx2Xliff;
import com.maxprograms.converters.sdlxliff.Sdl2Xliff;
import com.maxprograms.converters.srt.Srt2Xliff;
import com.maxprograms.converters.ts.Ts2Xliff;
import com.maxprograms.converters.txlf.Txlf2Xliff;
import com.maxprograms.converters.txml.Txml2Xliff;
import com.maxprograms.converters.wpml.Wpml2Xliff;
import com.maxprograms.converters.xliff.ToOpenXliff;
import com.maxprograms.converters.xml.Xml2Xliff;
import com.maxprograms.xliff2.Resegmenter;
import com.maxprograms.xliff2.ToXliff2;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.Indenter;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.XMLOutputter;

import org.xml.sax.SAXException;

public class Convert {

	private static Logger logger = System.getLogger(Convert.class.getName());

	public static void main(String[] args) {

		String[] arguments = Utils.fixPath(args);

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
		String config = "";
		String xmlfilter = "";
		boolean embed = false;
		boolean paragraph = false;
		boolean ignoretc = false;
		boolean xliff20 = false;
		boolean mustResegment = false;

		for (int i = 0; i < arguments.length; i++) {
			String arg = arguments[i];
			if (arg.equals("-version")) {
				MessageFormat mf = new MessageFormat(Messages.getString("Convert.02"));
				logger.log(Level.INFO, mf.format(new String[] { Constants.VERSION, Constants.BUILD }));
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
			if (arg.equals("-file") && (i + 1) < arguments.length) {
				source = arguments[i + 1];
			}
			if (arg.equals("-type") && (i + 1) < arguments.length) {
				type = arguments[i + 1];
			}
			if (arg.equals("-enc") && (i + 1) < arguments.length) {
				enc = arguments[i + 1];
			}
			if (arg.equals("-srcLang") && (i + 1) < arguments.length) {
				srcLang = arguments[i + 1];
			}
			if (arg.equals("-tgtLang") && (i + 1) < arguments.length) {
				tgtLang = arguments[i + 1];
			}
			if (arg.equals("-srx") && (i + 1) < arguments.length) {
				srx = arguments[i + 1];
			}
			if (arg.equals("-skl") && (i + 1) < arguments.length) {
				skl = arguments[i + 1];
			}
			if (arg.equals("-xliff") && (i + 1) < arguments.length) {
				xliff = arguments[i + 1];
			}
			if (arg.equals("-catalog") && (i + 1) < arguments.length) {
				catalog = arguments[i + 1];
			}
			if (arg.equals("-ditaval") && (i + 1) < arguments.length) {
				ditaval = arguments[i + 1];
			}
			if (arg.equals("-config") && (i + 1) < arguments.length) {
				config = arguments[i + 1];
			}
			if (arg.equals("-embed")) {
				embed = true;
			}
			if (arg.equals("-paragraph")) {
				paragraph = true;
			}
			if (arg.equals("-ignoretc")) {
				ignoretc = true;
			}
			if (arg.equals("-xmlfilter") && (i + 1) < arguments.length) {
				xmlfilter = arguments[i + 1];
			}
			if (arg.equals("-2.0")) {
				xliff20 = true;
			}
		}
		if (arguments.length < 4) {
			help();
			return;
		}
		if (source.isEmpty()) {
			logger.log(Level.ERROR, Messages.getString("Convert.03"));
			return;
		}
		File sourceFile = new File(source);
		if (!sourceFile.exists()) {
			logger.log(Level.ERROR, Messages.getString("Convert.04"));
			return;
		}
		if (!sourceFile.isAbsolute()) {
			source = sourceFile.getAbsoluteFile().getAbsolutePath();
		}
		if (type.isEmpty()) {
			String detected = FileFormats.detectFormat(source);
			if (detected != null) {
				type = FileFormats.getShortName(detected);
				MessageFormat mf = new MessageFormat(Messages.getString("Convert.05"));
				logger.log(Level.INFO, mf.format(new String[] { type }));
			} else {
				logger.log(Level.ERROR, Messages.getString("Convert.06"));
				return;
			}
		}
		type = FileFormats.getFullName(type);
		if (type == null) {
			logger.log(Level.ERROR, Messages.getString("Convert.07"));
			return;
		}
		if (enc.isEmpty()) {
			Charset charset = EncodingResolver.getEncoding(source, type);
			if (charset != null) {
				enc = charset.name();
				MessageFormat mf = new MessageFormat(Messages.getString("Convert.08"));
				logger.log(Level.INFO, mf.format(new String[] { enc }));
			} else {
				logger.log(Level.ERROR, Messages.getString("Convert.09"));
				return;
			}
		}
		String[] encodings = EncodingResolver.getPageCodes();
		if (!Arrays.asList(encodings).contains(enc)) {
			logger.log(Level.ERROR, Messages.getString("Convert.10"));
			return;
		}
		if (srcLang.isEmpty()) {
			logger.log(Level.ERROR, Messages.getString("Convert.11"));
			return;
		}
		try {
			if (!Utils.isValidLanguage(srcLang)) {
				MessageFormat mf = new MessageFormat(Messages.getString("Convert.12"));
				logger.log(Level.WARNING, mf.format(new String[] { srcLang }));
			}
			if (!tgtLang.isEmpty() && !Utils.isValidLanguage(tgtLang)) {
				MessageFormat mf = new MessageFormat(Messages.getString("Convert.12"));
				logger.log(Level.WARNING, mf.format(new String[] { tgtLang }));
			}
		} catch (IOException e) {
			logger.log(Level.ERROR, Messages.getString("Convert.14"), e);
			return;
		}
		if (srx.isEmpty()) {
			String home = System.getenv("OpenXLIFF_HOME");
			if (home == null) {
				home = System.getProperty("user.dir");
			}
			File srxFolder = new File(new File(home), "srx");
			srx = new File(srxFolder, "default.srx").getAbsolutePath();
		}
		File srxFile = new File(srx);
		if (!srxFile.exists()) {
			MessageFormat mf = new MessageFormat(Messages.getString("Convert.15"));
			logger.log(Level.ERROR, mf.format(new String[]{srxFile.getAbsolutePath()}));
			return;
		}
		if (!srxFile.isAbsolute()) {
			srx = srxFile.getAbsoluteFile().getAbsolutePath();
		}
		if (xmlfilter.isEmpty()) {
			String home = System.getenv("OpenXLIFF_HOME");
			if (home == null) {
				home = System.getProperty("user.dir");
			}
			File filtersFolder = new File(new File(home), "xmlfilter");
			xmlfilter = filtersFolder.getAbsolutePath();
		}
		if (catalog.isEmpty()) {
			String home = System.getenv("OpenXLIFF_HOME");
			if (home == null) {
				home = System.getProperty("user.dir");
			}
			File catalogFolder = new File(new File(home), "catalog");
			if (!catalogFolder.exists()) {
				logger.log(Level.ERROR, Messages.getString("Convert.16"));
				return;
			}
			catalog = new File(catalogFolder, "catalog.xml").getAbsolutePath();
		}
		File catalogFile = new File(catalog);
		if (!catalogFile.exists()) {
			logger.log(Level.ERROR, Messages.getString("Convert.17"));
			return;
		}
		if (!catalogFile.isAbsolute()) {
			catalog = catalogFile.getAbsoluteFile().getAbsolutePath();
		}
		if (skl.isEmpty()) {
			skl = sourceFile.getAbsoluteFile().getAbsolutePath() + ".skl";
		}
		if (xliff.isEmpty()) {
			xliff = sourceFile.getAbsoluteFile().getAbsolutePath() + ".xlf";
		}
		if (xliff20 && !paragraph && config.isEmpty()) {
			mustResegment = true;
			paragraph = true;
		}
		Map<String, String> params = new HashMap<>();
		params.put("source", source);
		params.put("xliff", xliff);
		params.put("skeleton", skl);
		params.put("format", type);
		params.put("catalog", catalog);
		params.put("srcEncoding", enc);
		params.put("paragraph", paragraph ? "yes" : "no");
		params.put("ignoretc", ignoretc ? "yes" : "no");
		params.put("srxFile", srx);
		params.put("srcLang", srcLang);
		params.put("xmlfilter", xmlfilter);
		if (!tgtLang.isEmpty()) {
			params.put("tgtLang", tgtLang);
		}
		if (type.equals(FileFormats.getShortName(FileFormats.DITA)) && !ditaval.isEmpty()) {
			params.put("ditaval", ditaval);
		}
		if (type.equals(FileFormats.getShortName(FileFormats.JSON)) && !config.isEmpty()) {
			File configFile = new File(config);
			if (!configFile.isAbsolute()) {
				config = configFile.getAbsoluteFile().getAbsolutePath();
			}
			params.put("config", config);
		}
		if (embed) {
			params.put("embed", "yes");
		}
		if (mustResegment) {
			params.put("resegment", "yes");
		}
		if (xliff20) {
			params.put("xliff20", "yes");
		}

		List<String> result = run(params);

		if (!Constants.SUCCESS.equals(result.get(0))) {
			logger.log(Level.ERROR, Messages.getString("Convert.18"), result.get(1));
		}
	}

	private static void help() {
		MessageFormat mf = new MessageFormat(Messages.getString("Convert.help"));
		String help = mf.format(new String[] { "\\".equals(File.pathSeparator) ? "convert.bat" : "convert.sh" });
		System.out.println(help);
	}

	public static List<String> addSkeleton(String fileName, String catalog) {
		List<String> result = new ArrayList<>();
		try {
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
				sklName = sklName.replace("&amp;", "&");
				sklName = sklName.replace("&lt;", "<");
				sklName = sklName.replace("&gt;", ">");
				sklName = sklName.replace("&apos;", "\'");
				sklName = sklName.replace("&quot;", "\"");
				if (!deleted.contains(sklName)) {
					File skeleton = new File(sklName);
					Element internal = new Element("internal-file");
					internal.setAttribute("form", "base64");
					internal.addContent(Utils.encodeFromFile(skeleton.getAbsolutePath()));
					skl.setContent(new ArrayList<>());
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
			result.add(Constants.SUCCESS);
		} catch (IOException | SAXException | ParserConfigurationException | URISyntaxException e) {
			logger.log(Level.ERROR, Messages.getString("Convert.19"), e);
			result.add(Constants.ERROR);
			result.add(e.getMessage());
		}
		return result;
	}

	public static List<String> run(Map<String, String> params) {
		List<String> result = new ArrayList<>();
		try {
			String format = params.get("format");
			if (format.equals(FileFormats.INX)) {
				params.put("InDesign", "yes");
				result = Xml2Xliff.run(params);
			} else if (format.equals(FileFormats.ICML)) {
				params.put("from", "x-icml");
				result = Story2Xliff.run(params);
			} else if (format.equals(FileFormats.IDML)) {
				result = Idml2Xliff.run(params);
			} else if (format.equals(FileFormats.DITA)) {
				result = DitaMap2Xliff.run(params);
			} else if (format.equals(FileFormats.HTML)) {
				result = Html2Xliff.run(params);
			} else if (format.equals(FileFormats.JS)) {
				result = Jscript2xliff.run(params);
			} else if (format.equals(FileFormats.JSON)) {
				result = Json2Xliff.run(params);
			} else if (format.equals(FileFormats.JAVA)) {
				result = Properties2Xliff.run(params);
			} else if (format.equals(FileFormats.MIF)) {
				result = Mif2Xliff.run(params);
			} else if (format.equals(FileFormats.OO) || format.equals(FileFormats.OFF)) {
				result = Office2Xliff.run(params);
			} else if (format.equals(FileFormats.PHPA)) {
				result = Php2Xliff.run(params);
			} else if (format.equals(FileFormats.PO)) {
				result = Po2Xliff.run(params);
			} else if (format.equals(FileFormats.RC)) {
				result = Rc2Xliff.run(params);
			} else if (format.equals(FileFormats.RESX)) {
				result = Resx2Xliff.run(params);
			} else if (format.equals(FileFormats.SDLPPX)) {
				result = Sdlppx2Xliff.run(params);
			} else if (format.equals(FileFormats.SDLXLIFF)) {
				result = Sdl2Xliff.run(params);
			} else if (format.equals(FileFormats.SRT)) {
				result = Srt2Xliff.run(params);
			} else if (format.equals(FileFormats.TEXT)) {
				result = Text2Xliff.run(params);
			} else if (format.equals(FileFormats.TS)) {
				result = Ts2Xliff.run(params);
			} else if (format.equals(FileFormats.TXML)) {
				result = Txml2Xliff.run(params);
			} else if (format.equals(FileFormats.TXLF)) {
				result = Txlf2Xliff.run(params);
			} else if (format.equals(FileFormats.WPML)) {
				result = Wpml2Xliff.run(params);
			} else if (format.equals(FileFormats.XML)) {
				result = Xml2Xliff.run(params);
			} else if (format.equals(FileFormats.XMLG)) {
				params.put("generic", "yes");
				result = Xml2Xliff.run(params);
			} else if (format.equals(FileFormats.XLIFF)) {
				result = ToOpenXliff.run(params);
			} else {
				result.add(Constants.ERROR);
				result.add(Messages.getString("Convert.20"));
			}
			if ("yes".equals(params.get("embed")) && Constants.SUCCESS.equals(result.get(0))) {
				result = addSkeleton(params.get("xliff"), params.get("catalog"));
			}
			if ("yes".equals(params.get("xliff20")) && Constants.SUCCESS.equals(result.get(0))) {
				result = ToXliff2.run(new File(params.get("xliff")), params.get("catalog"));
				if ("yes".equals(params.get("resegment")) && Constants.SUCCESS.equals(result.get(0))) {
					result = Resegmenter.run(params.get("xliff"), params.get("srxFile"), params.get("srcLang"),
							new Catalog(params.get("catalog")));
				}
			}
		} catch (Exception e) {
			result.add(0, Constants.ERROR);
			result.add(1, e.getMessage());
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
