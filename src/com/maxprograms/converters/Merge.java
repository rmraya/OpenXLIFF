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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.converters.ditamap.Xliff2DitaMap;
import com.maxprograms.converters.html.Xliff2Html;
import com.maxprograms.converters.idml.Xliff2Idml;
import com.maxprograms.converters.javaproperties.Xliff2Properties;
import com.maxprograms.converters.javascript.Xliff2jscript;
import com.maxprograms.converters.mif.Xliff2Mif;
import com.maxprograms.converters.office.Xliff2Office;
import com.maxprograms.converters.plaintext.Xliff2Text;
import com.maxprograms.converters.po.Xliff2Po;
import com.maxprograms.converters.rc.Xliff2Rc;
import com.maxprograms.converters.resx.Xliff2Resx;
import com.maxprograms.converters.sdlxliff.Xliff2Sdl;
import com.maxprograms.converters.ts.Xliff2Ts;
import com.maxprograms.converters.txml.Xliff2Txml;
import com.maxprograms.converters.xml.Xliff2Xml;
import com.maxprograms.xliff2.FromXliff2;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.PI;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.XMLNode;

public class Merge {

	private static final Logger LOGGER = System.getLogger(Merge.class.getName());

	private static Vector<Element> segments;
	protected static HashSet<String> fileSet;
	private static String dataType;
	private static String encoding;

	private static Document doc;
	private static Element root;

	public static void main(String[] args) {
		String xliff = "";
		String target = "";
		String catalog = "";
		boolean unapproved = false;

		args = Convert.fixPath(args);
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
			if (arg.equals("-xliff") && (i + 1) < args.length) {
				xliff = args[i + 1];
			}
			if (arg.equals("-target") && (i + 1) < args.length) {
				target = args[i + 1];
			}
			if (arg.equals("-catalog") && (i + 1) < args.length) {
				catalog = args[i + 1];
			}
			if (arg.equals("-unapproved")) {
				unapproved = true;
			}
		}
		if (args.length < 2) {
			help();
			return;
		}
		if (xliff.isEmpty()) {
			LOGGER.log(Level.ERROR, "Missing '-xliff' parameter.");
			return;
		}
		if (target.isEmpty()) {
			try {
				target = getTargetFile(xliff);
			} catch (IOException | SAXException | ParserConfigurationException e) {
				LOGGER.log(Level.ERROR, "Error getting target file", e);
				return;
			}
		}
		if (target.isEmpty()) {
			LOGGER.log(Level.ERROR, "Missing '-target' parameter.");
			return;
		}
		if (catalog.isEmpty()) {
			File catalogFolder = new File(new File(System.getProperty("user.dir")), "catalog");
			catalog = new File(catalogFolder, "catalog.xml").getAbsolutePath();
		}
		try {
			merge(xliff, target, catalog, unapproved);
		} catch (IOException | SAXException | ParserConfigurationException e) {
			LOGGER.log(Level.ERROR, e.getMessage(), e);
		}
	}

	public static void merge(String xliff, String target, String catalog, boolean unapproved) throws IOException, SAXException, ParserConfigurationException {
		loadXliff(xliff, catalog);
		if (root.getAttributeValue("version").equals("2.0")) {
			File tmpXliff = File.createTempFile("temp", ".xlf", new File(xliff).getParentFile());
			FromXliff2.run(xliff, tmpXliff.getAbsolutePath(), catalog);
			loadXliff(tmpXliff.getAbsolutePath(), catalog);
			Files.delete(Paths.get(tmpXliff.toURI()));
			unapproved = true;
		}
		if (unapproved) {
			approveAll(root);
		}

		List<Element> files = root.getChildren("file");
		fileSet = new HashSet<>();
		Iterator<Element> it = files.iterator();
		while (it.hasNext()) {
			Element file = it.next();
			fileSet.add(file.getAttributeValue("original"));
		}
		List<PI> encList = root.getPI("encoding");
		if (!encList.isEmpty()) {
			encoding = encList.get(0).getData();
		}
		if (encoding == null) {
			throw new IOException("Unknown character set");
		}
		segments = new Vector<>();
		createList(root);

		if (fileSet.size() != 1) {
			File f = new File(target);
			if (f.exists()) {
				if (!f.isDirectory()) {
					LOGGER.log(Level.ERROR, () -> "'" + f.getAbsolutePath() + "' is not a directory");
					return;
				}
			} else {
				f.mkdirs();
			}
		}
		Iterator<String> ft = fileSet.iterator();
		Vector<Hashtable<String, String>> paramsList = new Vector<>();
		while (ft.hasNext()) {
			String file = ft.next();
			File xliffFile = File.createTempFile("temp", ".xlf");
			saveXliff(file, xliffFile);
			Hashtable<String, String> params = new Hashtable<>();
			params.put("xliff", xliffFile.getAbsolutePath());
			if (fileSet.size() == 1) {
				params.put("backfile", target);
			} else {
				params.put("backfile", Utils.getAbsolutePath(target, file));
			}
			params.put("encoding", encoding);
			params.put("catalog", catalog);
			params.put("format", dataType);
			paramsList.add(params);
		}
		for (int i = 0; i < paramsList.size(); i++) {
			Vector<String> result = run(paramsList.get(i));
			if (!"0".equals(result.get(0))) {
				LOGGER.log(Level.ERROR, result.get(1));
				return;
			}
			File f = new File(paramsList.get(i).get("xliff"));
			Files.delete(Paths.get(f.toURI()));
		}
	}

	private static void help() {
		String launcher = "   merge.sh ";
		if (System.getProperty("file.separator").equals("\\")) {
			launcher = "   merge.bat ";
		}
		String help = "Usage:\n\n" + launcher + "[-help] [-version] -xliff xliffFile -target targetFile "
				+ "[-catalog catalogFile] [-unapproved]\n\n" + "Where:\n\n"
				+ "   -help:       (optional) Display this help information and exit\n"
				+ "   -version:    (optional) Display version & build information and exit\n"
				+ "   -xliff:      XLIFF file to merge\n"
				+ "   -target:     (optional) translated file or folder where to store translated files\n"
				+ "   -catalog:    (optional) XML catalog to use for processing\n"
				+ "   -unapproved: (optional) accept translations from unapproved segments\n\n";
		System.out.println(help);
	}

	private static void approveAll(Element e) {
		if (e.getName().equals("trans-unit")) {
			Element target = e.getChild("target");
			if (target != null) {
				e.setAttribute("approved", "yes");
			}
			return;
		}
		List<Element> children = e.getChildren();
		for (int i = 0; i < children.size(); i++) {
			approveAll(children.get(i));
		}
	}

	protected static void loadXliff(String fileName, String catalog)
			throws SAXException, IOException, ParserConfigurationException {
		SAXBuilder builder = new SAXBuilder();
		builder.setEntityResolver(new Catalog(catalog));
		doc = builder.build(fileName);
		root = doc.getRootElement();
		if (!root.getName().equals("xliff")) {
			throw new IOException("Selected file is not an XLIFF document.");
		}
	}

	private static void createList(Element e) {
		List<Element> children = e.getChildren();
		Iterator<Element> it = children.iterator();
		while (it.hasNext()) {
			Element child = it.next();
			if (child.getName().equals("trans-unit")) {
				child.removeChild("alt-trans");
				segments.add(child);
			} else {
				createList(child);
			}
		}
	}

	private static void saveXliff(String fileName, File xliff) throws IOException {
		try (FileOutputStream out = new FileOutputStream(xliff)) {
			writeStr(out, "<xliff version=\"1.2\">\n");
			List<Element> files = root.getChildren("file");
			Iterator<Element> it = files.iterator();
			List<PI> pis = null;
			while (it.hasNext()) {
				Element file = it.next();
				if (file.getAttributeValue("original").equals(fileName)) {
					if (pis == null) {
						pis = file.getPI();
						Iterator<PI> pt = pis.iterator();
						while (pt.hasNext()) {
							PI pi = pt.next();
							if (pi.getTarget().equals("encoding")) {
								encoding = pi.getData();
							}
							writeStr(out, pis.get(0).toString());
						}
						dataType = file.getAttributeValue("datatype");
					}
					file.writeBytes(out, doc.getEncoding());
				}
			}
			writeStr(out, "</xliff>\n");
		}
	}

	private static void writeStr(FileOutputStream out, String string) throws IOException {
		out.write(string.getBytes(StandardCharsets.UTF_8));
	}

	private static Vector<String> run(Hashtable<String, String> params) {
		Vector<String> result = new Vector<>();
		File temporary = null;
		try {
			dataType = params.get("format");
			loadXliff(params.get("xliff"), params.get("catalog"));
			String skl = getSkeleton();
			params.put("skeleton", skl);
			if (checkGroups(root)) {
				temporary = File.createTempFile("group", ".xlf");
				removeGroups(root, doc);
				try (FileOutputStream out = new FileOutputStream(temporary.getAbsolutePath())) {
					doc.writeBytes(out, doc.getEncoding());
				}
				params.put("xliff", temporary.getAbsolutePath());
			}

			if (dataType.equals(FileFormats.INX) || dataType.equals("x-inx")) {
				params.put("InDesign", "yes");
				result = Xliff2Xml.run(params);
			} else if (dataType.equals(FileFormats.IDML) || dataType.equals("x-idml")) {
				result = Xliff2Idml.run(params);
			} else if (dataType.equals(FileFormats.DITA) || dataType.equals("x-ditamap")) {
				result = Xliff2DitaMap.run(params);
			} else if (dataType.equals(FileFormats.HTML) || dataType.equals("html")) {
				File folder = new File(System.getProperty("user.dir"), "xmlfilter");
				params.put("iniFile", new File(folder, "init_html.xml").getAbsolutePath());
				result = Xliff2Html.run(params);
			} else if (dataType.equals(FileFormats.JS) || dataType.equals("javascript")) {
				result = Xliff2jscript.run(params);
			} else if (dataType.equals(FileFormats.JAVA) || dataType.equals("javapropertyresourcebundle")
					|| dataType.equals("javalistresourcebundle")) {
				result = Xliff2Properties.run(params);
			} else if (dataType.equals(FileFormats.MIF) || dataType.equals("mif")) {
				result = Xliff2Mif.run(params);
			} else if (dataType.equals(FileFormats.OFF) || dataType.equals("x-office")) {
				result = Xliff2Office.run(params);
			} else if (dataType.equals(FileFormats.PO) || dataType.equals("po")) {
				result = Xliff2Po.run(params);
			} else if (dataType.equals(FileFormats.RC) || dataType.equals("winres")) {
				result = Xliff2Rc.run(params);
			} else if (dataType.equals(FileFormats.RESX) || dataType.equals("resx")) {
				result = Xliff2Resx.run(params);
			} else if (dataType.equals(FileFormats.SDLXLIFF) || dataType.equals("x-sdlxliff")) {
				result = Xliff2Sdl.run(params);
			} else if (dataType.equals(FileFormats.TEXT) || dataType.equals("plaintext")) {
				result = Xliff2Text.run(params);
			} else if (dataType.equals(FileFormats.TS) || dataType.equals("x-ts")) {
				result = Xliff2Ts.run(params);
			} else if (dataType.equals(FileFormats.TXML) || dataType.equals("x-txml")) {
				result = Xliff2Txml.run(params);
			} else if (dataType.equals(FileFormats.XML) || dataType.equals("xml")) {
				result = Xliff2Xml.run(params);
			} else {
				result.add("1");
				result.add("Unsupported XLIFF file.");
			}
			if (temporary != null) {
				Files.delete(Paths.get(temporary.toURI()));
			}
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, "Error merging XLIFF", e);
			result = new Vector<>();
			result.add("1");
			result.add(e.getMessage());
		}
		return result;
	}

	private static String getSkeleton() throws IOException {
		String result = "";
		Element file = root.getChild("file");
		Element header = null;
		if (file != null) {
			dataType = file.getAttributeValue("datatype");
			header = file.getChild("header");
			if (header != null) {
				Element mskl = header.getChild("skl");
				if (mskl != null) {
					Element external = mskl.getChild("external-file");
					if (external != null) {
						result = external.getAttributeValue("href");
						result = result.replaceAll("&amp;", "&");
						result = result.replaceAll("&lt;", "<");
						result = result.replaceAll("&gt;", ">");
						result = result.replaceAll("&apos;", "\'");
						result = result.replaceAll("&quot;", "\"");
					} else {
						Element internal = mskl.getChild("internal-file");
						if (internal != null) {
							File tmp = File.createTempFile("internal", ".skl");
							tmp.deleteOnExit();
							Utils.decodeToFile(internal.getText(), tmp.getAbsolutePath());
							return tmp.getAbsolutePath();
						}
						return result;
					}
				} else {
					return result;
				}
			} else {
				return result;
			}
		} else {
			return result;
		}

		if (encoding != null && encoding.equals("")) {
			List<PI> encList = root.getPI("encoding");
			if (!encList.isEmpty()) {
				encoding = encList.get(0).getData();
			}
		}
		return result;
	}

	private static boolean checkGroups(Element e) {
		if (e.getName().equals("group") && e.getAttributeValue("ts", "").equals("hs-split")) {
			return true;
		}
		List<Element> children = e.getChildren();
		Iterator<Element> i = children.iterator();
		while (i.hasNext()) {
			Element child = i.next();
			if (checkGroups(child)) {
				return true;
			}
		}
		return false;
	}

	public static void removeGroups(Element e, Document d) {
		List<XMLNode> children = e.getContent();
		for (int i = 0; i < children.size(); i++) {
			XMLNode n = children.get(i);
			if (n.getNodeType() == XMLNode.ELEMENT_NODE) {
				Element child = (Element) n;
				if (child.getName().equals("group") && child.getAttributeValue("ts", "").equals("hs-split")) {
					child = joinGroup(child);
					Element tu = new Element("trans-unit");
					tu.clone(child);
					children.remove(i);
					children.add(i, tu);
					e.setContent(children);
				} else {
					removeGroups(child, d);
				}
			}
		}
	}

	private static Element joinGroup(Element child) {
		List<Element> pair = child.getChildren();
		Element left = pair.get(0);
		if (left.getName().equals("group")) {
			left = joinGroup(left);
		}
		Element right = pair.get(1);
		if (right.getName().equals("group")) {
			right = joinGroup(right);
		}
		List<XMLNode> srcContent = right.getChild("source").getContent();
		for (int k = 0; k < srcContent.size(); k++) {
			XMLNode n = srcContent.get(k);
			if (n.getNodeType() == XMLNode.ELEMENT_NODE) {
				left.getChild("source").addContent(n);
			}
			if (n.getNodeType() == XMLNode.TEXT_NODE) {
				left.getChild("source").addContent(n);
			}
		}
		List<XMLNode> tgtContent = right.getChild("target").getContent();
		for (int k = 0; k < tgtContent.size(); k++) {
			XMLNode n = tgtContent.get(k);
			if (n.getNodeType() == XMLNode.ELEMENT_NODE) {
				left.getChild("target").addContent(n);
			}
			if (n.getNodeType() == XMLNode.TEXT_NODE) {
				left.getChild("target").addContent(n);
			}
		}
		left.setAttribute("id", child.getAttributeValue("id"));
		if (left.getAttributeValue("approved").equalsIgnoreCase("yes")
				&& right.getAttributeValue("approved").equalsIgnoreCase("yes")) {
			left.setAttribute("approved", "yes");
		} else {
			left.setAttribute("approved", "no");
		}
		return left;
	}

	public static String getTargetFile(String file) throws IOException, SAXException, ParserConfigurationException {
		SAXBuilder builder = new SAXBuilder();
		Element r = builder.build(file).getRootElement();
		if (!r.getName().equals("xliff")) {
			throw new IOException("Selected file is not an XLIFF document");			
		}
		List<Element> files = r.getChildren("file");
		if (files.isEmpty()) {
			throw new IOException("Selected file is not a valid XLIFF document");
		}
		String version = r.getAttributeValue("version");
		String tgtLanguage = "";
		if (version.equals("1.2")) {
			tgtLanguage = files.get(0).getAttributeValue("target-language");
		} else {
			tgtLanguage = r.getAttributeValue("trgLang");
		}
		if (tgtLanguage.isEmpty()) {
			throw new IOException("Missing target language");
		}
		String target = "";
		TreeSet<String> originals = new TreeSet<>();
		Iterator<Element> it = files.iterator();
		while (it.hasNext()) {
			originals.add(it.next().getAttributeValue("original",""));
		}
		if (originals.size() == 1) {
			if (file.endsWith(".xlf")) { 
				target = file.substring(0,file.length()-4);
				if (target.indexOf(".") != -1) { 
					target = target.substring(0,target.lastIndexOf("."))  
							+ "_" + tgtLanguage + target.substring(target.lastIndexOf("."));  
				}
			} else {
				if (target.indexOf(".") != -1) { 
					target = target.substring(0,target.lastIndexOf("."))  
							+ "_" + tgtLanguage + target.substring(target.lastIndexOf("."));  
				}
			}
		} else {
			target = new File(file).getParentFile().getAbsolutePath() +  System.getProperty("file.separator") + tgtLanguage;
		}
		return target;
	}

}
