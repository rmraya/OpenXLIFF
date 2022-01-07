/*******************************************************************************
 * Copyright (c) 2022 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/
package com.maxprograms.converters.office;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.ParserConfigurationException;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.FileFormats;
import com.maxprograms.converters.Utils;
import com.maxprograms.converters.msoffice.MSOffice2Xliff;
import com.maxprograms.converters.xml.Xml2Xliff;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.Indenter;
import com.maxprograms.xml.PI;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.TextNode;
import com.maxprograms.xml.XMLNode;
import com.maxprograms.xml.XMLOutputter;

import org.xml.sax.SAXException;

public class Office2Xliff {

	private static Element mergedRoot;
	private static ZipInputStream in;
	private static ZipOutputStream out;
	private static String inputFile;
	private static String skeleton;
	private static boolean isPPTX;

	private Office2Xliff() {
		// do not instantiate this class
		// use run method instead
	}

	public static List<String> run(Map<String, String> params) {
		List<String> result = new ArrayList<>();

		inputFile = params.get("source");
		String xliff = params.get("xliff");
		skeleton = params.get("skeleton");
		String catalog = params.get("catalog");

		try {
			Document merged = new Document(null, "xliff", null, null);
			mergedRoot = merged.getRootElement();
			mergedRoot.setAttribute("version", "1.2");
			mergedRoot.setAttribute("xmlns", "urn:oasis:names:tc:xliff:document:1.2");
			mergedRoot.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			mergedRoot.setAttribute("xsi:schemaLocation",
					"urn:oasis:names:tc:xliff:document:1.2 xliff-core-1.2-transitional.xsd");
			mergedRoot.addContent("\n");

			try {
				out = new ZipOutputStream(new FileOutputStream(skeleton));
				in = new ZipInputStream(new FileInputStream(inputFile));
			} catch (IOException e) {
				result.add(Constants.ERROR);
				if (params.get("format").equals(FileFormats.OFF)) {
					result.add("Selected file is not a Microsoft Office 2007 document.");
				} else {
					result.add("Wrong document type.");
				}
				return result;
			}
			ZipEntry entry = null;
			while ((entry = in.getNextEntry()) != null) {
				if (entry.getName().matches(".*\\.[xX][mM][lL]") && !(entry.getName().matches(".*slideMaster.*")
						|| entry.getName().matches(".*slideLayout.*") || entry.getName().matches(".*handoutMaster.*")
						|| entry.getName().matches(".*notesMaster.*"))) {
					File f = new File(entry.getName());
					String name = f.getName();
					File tmp = File.createTempFile(name.substring(0, name.lastIndexOf('.')), ".xml");
					try (FileOutputStream output = new FileOutputStream(tmp.getAbsolutePath())) {
						byte[] buf = new byte[1024];
						int len;
						while ((len = in.read(buf)) > 0) {
							output.write(buf, 0, len);
						}
					}
					if (name.equals("content.xml")) {
						cleanTags(tmp.getAbsolutePath(), catalog);
					}
					try {
						Map<String, String> table = new HashMap<>();
						table.put("source", tmp.getAbsolutePath());
						table.put("xliff", tmp.getAbsolutePath() + ".xlf");
						table.put("skeleton", tmp.getAbsolutePath() + ".skl");
						table.put("catalog", params.get("catalog"));
						table.put("srcLang", params.get("srcLang"));
						String tgtLang = params.get("tgtLang");
						if (tgtLang != null) {
							table.put("tgtLang", tgtLang);
						}
						table.put("srcEncoding", params.get("srcEncoding"));
						table.put("paragraph", params.get("paragraph"));
						table.put("srxFile", params.get("srxFile"));
						table.put("format", params.get("format"));
						List<String> res = null;
						if (params.get("format").equals(FileFormats.OFF)) {
							res = MSOffice2Xliff.run(table);
							if (tmp.getName().indexOf("slide") != -1) {
								isPPTX = true;
							}
						} else {
							res = Xml2Xliff.run(table);
						}
						if (Constants.SUCCESS.equals(res.get(0))) {
							if (countSegments(tmp.getAbsolutePath() + ".xlf") > 0) {
								updateXliff(tmp.getAbsolutePath() + ".xlf", entry.getName());
								addFile(tmp.getAbsolutePath() + ".xlf");
								ZipEntry content = new ZipEntry(entry.getName() + ".skl");
								content.setMethod(ZipEntry.DEFLATED);
								out.putNextEntry(content);
								try (FileInputStream input = new FileInputStream(tmp.getAbsolutePath() + ".skl")) {
									byte[] array = new byte[1024];
									int len;
									while ((len = input.read(array)) > 0) {
										out.write(array, 0, len);
									}
									out.closeEntry();
								}
							} else {
								saveEntry(entry, tmp.getAbsolutePath());
							}
							File skl = new File(tmp.getAbsolutePath() + ".skl");
							Files.delete(Paths.get(skl.toURI()));
							File xlf = new File(tmp.getAbsolutePath() + ".xlf");
							Files.delete(Paths.get(xlf.toURI()));
						} else {
							saveEntry(entry, tmp.getAbsolutePath());
						}
					} catch (IOException e) {
						// do nothing
						saveEntry(entry, tmp.getAbsolutePath());
					}
					Files.delete(Paths.get(tmp.toURI()));
				} else {
					// not an XML file
					File tmp = File.createTempFile("zip", ".tmp");
					try (FileOutputStream output = new FileOutputStream(tmp.getAbsolutePath())) {
						byte[] buf = new byte[1024];
						int len;
						while ((len = in.read(buf)) > 0) {
							output.write(buf, 0, len);
						}
					}
					saveEntry(entry, tmp.getAbsolutePath());
					Files.delete(Paths.get(tmp.toURI()));
				}
			}
			try {
				in.close();
				out.close();
			} catch (IOException e) {
				result.add(Constants.ERROR);
				if (params.get("format").equals(FileFormats.OFF)) {
					result.add("Selected file is not a Microsoft Office 2007 document.");
				} else {
					result.add("Wrong document type.");
				}
				return result;
			}
			// sort the slides if it is PPTX

			if (params.get("format").equals(FileFormats.OFF) && isPPTX) {
				sortSlides();
			}

			// output final XLIFF

			XMLOutputter outputter = new XMLOutputter();
			mergedRoot.addContent("\n");
			outputter.preserveSpace(true);
			try (FileOutputStream output = new FileOutputStream(xliff)) {
				outputter.output(merged, output);
			}
			result.add(Constants.SUCCESS);
		} catch (IOException | SAXException | ParserConfigurationException | URISyntaxException e) {
			Logger logger = System.getLogger(Office2Xliff.class.getName());
			logger.log(Level.ERROR, "Error converting Office file", e);
			result.add(Constants.ERROR);
			result.add(e.getMessage());
		}
		return result;
	}

	private static void cleanTags(String file, String catalog)
			throws SAXException, IOException, ParserConfigurationException, URISyntaxException {
		SAXBuilder builder = new SAXBuilder();
		builder.setEntityResolver(new Catalog(catalog));
		Document doc = builder.build(file);
		Element root = doc.getRootElement();
		recurseCleaning(root);
		XMLOutputter outputter = new XMLOutputter();
		try (FileOutputStream output = new FileOutputStream(file)) {
			outputter.output(doc, output);
		}
	}

	private static void recurseCleaning(Element e) {
		if (!e.getChildren("text:s").isEmpty()) {
			List<XMLNode> newContent = new ArrayList<>();
			List<XMLNode> content = e.getContent();
			Iterator<XMLNode> it = content.iterator();
			while (it.hasNext()) {
				XMLNode n = it.next();
				if (n.getNodeType() == XMLNode.ELEMENT_NODE) {
					Element e1 = (Element) n;
					if (e1.getName().equals("text:s")) {
						newContent.add(new TextNode(" "));
					} else {
						newContent.add(n);
					}
				} else {
					newContent.add(n);
				}
			}
			e.setContent(newContent);
		} else {
			List<Element> list = e.getChildren();
			Iterator<Element> it = list.iterator();
			while (it.hasNext()) {
				recurseCleaning(it.next());
			}
		}
	}

	private static void sortSlides() {
		List<Element> files = mergedRoot.getChildren("file");
		List<PI> instructions = mergedRoot.getPI();
		Map<String, String> table = new HashMap<>();
		TreeSet<String> tree = new TreeSet<>();
		for (int i = 0; i < files.size(); i++) {
			String key = padKey(getKey(files.get(i)));
			tree.add(key);
			table.put(key, "" + i);
		}
		List<XMLNode> v = new ArrayList<>();
		Iterator<String> it = tree.iterator();
		while (it.hasNext()) {
			String key = it.next();
			v.add(files.get(Integer.parseInt(table.get(key))));
		}
		mergedRoot.setContent(v);
		Iterator<PI> pit = instructions.iterator();
		while (pit.hasNext()) {
			mergedRoot.addContent(pit.next());
		}
	}

	private static String padKey(String key) {
		String path = key.substring(0, key.lastIndexOf('/'));
		String file = key.substring(key.lastIndexOf('/'));
		String name = "";
		String number = "";
		String extension = "";
		int i = 0;
		for (i = 0; i < file.length(); i++) {
			if (Character.isDigit(file.charAt(i))) {
				break;
			}
			name = name + file.charAt(i);
		}
		for (; i < file.length(); i++) {
			if (Character.isDigit(file.charAt(i))) {
				number = number + file.charAt(i);
			} else {
				break;
			}
		}
		while (number.length() < 5) {
			number = "0" + number;
		}
		for (; i < file.length(); i++) {
			if (Character.isDigit(file.charAt(i))) {
				break;
			}
			extension = extension + file.charAt(i);
		}
		if (name.equals("/slide")) {
			name = "aaa";
		}
		if (name.equals("/notesSlide")) {
			name = "bbb";
		}
		if (!number.equals("00000")) {
			return number + name + extension;
		}
		return path + file;
	}

	private static String getKey(Element file) {
		List<Element> groups = file.getChild("header").getChildren("prop-group");
		for (int i = 0; i < groups.size(); i++) {
			Element group = groups.get(i);
			if (group.getAttributeValue("name").equals("document")) {
				return group.getChild("prop").getText();
			}
		}
		return null;
	}

	private static int countSegments(String string) throws SAXException, IOException, ParserConfigurationException {
		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(string);
		Element root = doc.getRootElement();
		return root.getChild("file").getChild("body").getChildren("trans-unit").size();
	}

	private static void saveEntry(ZipEntry entry, String name) throws IOException {
		ZipEntry content = new ZipEntry(entry.getName());
		content.setMethod(ZipEntry.DEFLATED);
		out.putNextEntry(content);
		try (FileInputStream input = new FileInputStream(name)) {
			byte[] array = new byte[1024];
			int len;
			while ((len = input.read(array)) > 0) {
				out.write(array, 0, len);
			}
			out.closeEntry();
		}
	}

	private static void addFile(String xliff) throws SAXException, IOException, ParserConfigurationException {
		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(xliff);
		Element root = doc.getRootElement();
		Element file = root.getChild("file");
		Element newFile = new Element("file");
		newFile.clone(file);
		List<PI> pi = root.getPI();
		for (int i = 0; i < pi.size(); i++) {
			newFile.addContent(pi.get(i));
		}
		Indenter.indent(newFile, 2, 2);
		mergedRoot.addContent(newFile);
	}

	private static void updateXliff(String xliff, String original)
			throws SAXException, IOException, ParserConfigurationException {
		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(xliff);
		Element root = doc.getRootElement();
		Element file = root.getChild("file");
		file.setAttribute("datatype", "x-office");
		file.setAttribute("original", Utils.cleanString(inputFile));
		Element header = file.getChild("header");
		Element propGroup = new Element("prop-group");
		propGroup.setAttribute("name", "document");
		Element prop = new Element("prop");
		prop.setAttribute("prop-type", "original");
		prop.setText(original);
		propGroup.addContent(prop);
		header.addContent(propGroup);

		Element ext = header.getChild("skl").getChild("external-file");
		ext.setAttribute("href", Utils.cleanString(skeleton));

		XMLOutputter outputter = new XMLOutputter();
		Indenter.indent(root, 2);
		try (FileOutputStream output = new FileOutputStream(xliff)) {
			outputter.output(doc, output);
		}
	}

}
