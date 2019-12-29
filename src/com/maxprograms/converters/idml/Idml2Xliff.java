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
package com.maxprograms.converters.idml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.ParserConfigurationException;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.Utils;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.Indenter;
import com.maxprograms.xml.PI;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.XMLNode;
import com.maxprograms.xml.XMLOutputter;

import org.xml.sax.SAXException;

public class Idml2Xliff {

	private static final Logger LOGGER = System.getLogger(Idml2Xliff.class.getName());

	private static Element mergedRoot;
	private static String inputFile;
	private static String skeleton;
	private static ZipOutputStream out;
	private static List<String> used = null;

	private Idml2Xliff() {
		// do not instantiate this class
		// use run method instead
	}

	private static void sortStories() {
		List<Element> files = mergedRoot.getChildren("file");
		List<PI> instructions = mergedRoot.getPI();
		Map<String, Integer> table = new HashMap<>();
		for (int i = 0; i < files.size(); i++) {
			String key = getKey(files.get(i));
			table.put(key, i);
		}
		List<XMLNode> v = new ArrayList<>();
		Iterator<String> it = used.iterator();
		while (it.hasNext()) {
			String key = it.next();
			if (table.containsKey(key)) {
				v.add(files.get(table.get(key).intValue()));
			}
		}
		mergedRoot.setContent(v);
		Iterator<PI> pit = instructions.iterator();
		while (pit.hasNext()) {
			mergedRoot.addContent(pit.next());
		}
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

	private static List<String> getStories(String map)
			throws SAXException, IOException, ParserConfigurationException {
		List<String> result = new ArrayList<>();
		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(map);
		Element root = doc.getRootElement();
		List<Element> stories = root.getChildren("idPkg:Story");
		Iterator<Element> it = stories.iterator();
		while (it.hasNext()) {
			result.add(it.next().getAttributeValue("src"));
		}
		return result;
	}

	private static void addFile(String xliff) throws SAXException, IOException, ParserConfigurationException {
		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(xliff);
		Element root = doc.getRootElement();
		Element file = root.getChild("file");
		Element newFile = new Element("file");
		newFile.clone(file);
		mergedRoot.addContent(newFile);
	}

	private static void updateXliff(String xliff, String original)
			throws SAXException, IOException, ParserConfigurationException {
		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(xliff);
		Element root = doc.getRootElement();
		Element file = root.getChild("file");
		file.setAttribute("datatype", "x-idml");
		file.setAttribute("original", Utils.cleanString(inputFile));
		file.setAttribute("tool-id", Constants.TOOLID);
		Element header = file.getChild("header");
		Element propGroup = new Element("prop-group");
		propGroup.setAttribute("name", "document");
		Element prop = new Element("prop");
		prop.setAttribute("prop-type", "original");
		prop.setText(original);
		propGroup.addContent(prop);
		Element tool = new Element("tool");
		tool.setAttribute("tool-id", Constants.TOOLID);
		tool.setAttribute("tool-name", Constants.TOOLNAME);
		tool.setAttribute("tool-version", Constants.VERSION + " " + Constants.BUILD);
		header.addContent(tool);
		header.addContent(propGroup);

		Element ext = header.getChild("skl").getChild("external-file");
		ext.setAttribute("href", Utils.cleanString(skeleton));

		XMLOutputter outputter = new XMLOutputter();
		outputter.preserveSpace(true);
		Indenter.indent(root, 2);
		try (FileOutputStream output = new FileOutputStream(xliff)) {
			outputter.output(doc, output);
		}
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

	public static List<String> run(Map<String, String> params) {
		List<String> result = new ArrayList<>();

		inputFile = params.get("source");
		String xliff = params.get("xliff");
		skeleton = params.get("skeleton");
		String encoding = params.get("srcEncoding");

		try {
			Document merged = new Document(null, "xliff", null, null);
			mergedRoot = merged.getRootElement();
			mergedRoot.setAttribute("version", "1.2");
			mergedRoot.setAttribute("xmlns", "urn:oasis:names:tc:xliff:document:1.2");
			mergedRoot.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			mergedRoot.setAttribute("xsi:schemaLocation",
					"urn:oasis:names:tc:xliff:document:1.2 xliff-core-1.2-transitional.xsd");
			mergedRoot.addContent(new PI("encoding", encoding));

			out = new ZipOutputStream(new FileOutputStream(skeleton));
			try (ZipInputStream in = new ZipInputStream(new FileInputStream(inputFile))) {

				ZipEntry entry = null;
				while ((entry = in.getNextEntry()) != null) {
					if (entry.getName().matches(".*Story_.*\\.xml")
							&& (used != null && used.contains(entry.getName()))) {
						File f = new File(entry.getName());
						String name = f.getName();
						File tmp = File.createTempFile(name.substring(0, name.lastIndexOf('.')), ".xml",
								new File(skeleton).getParentFile());
						try (FileOutputStream output = new FileOutputStream(tmp.getAbsolutePath())) {
							byte[] buf = new byte[1024];
							int len;
							while ((len = in.read(buf)) > 0) {
								output.write(buf, 0, len);
							}
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
							List<String> res = Story2Xliff.run(table);

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
						} catch (Exception e) {
							// do nothing
							saveEntry(entry, tmp.getAbsolutePath());
						}
						Files.delete(Paths.get(tmp.toURI()));
					} else {
						// not a story
						File tmp = File.createTempFile("zip", ".tmp");
						try (FileOutputStream output = new FileOutputStream(tmp.getAbsolutePath())) {
							byte[] buf = new byte[1024];
							int len;
							while ((len = in.read(buf)) > 0) {
								output.write(buf, 0, len);
							}
						}
						if (entry.getName().matches(".*designmap\\.xml")) {
							used = getStories(tmp.getAbsolutePath());
						}
						saveEntry(entry, tmp.getAbsolutePath());
						Files.delete(Paths.get(tmp.toURI()));
					}
				}

			}
			out.close();

			sortStories();

			// output final XLIFF

			XMLOutputter outputter = new XMLOutputter();
			outputter.preserveSpace(true);
			try (FileOutputStream output = new FileOutputStream(xliff)) {
				outputter.output(merged, output);
			}
			result.add(Constants.SUCCESS);
		} catch (IOException | SAXException | ParserConfigurationException e) {
			LOGGER.log(Level.ERROR, "Error converting IDML file", e);
			result.add(Constants.ERROR);
			result.add(e.getMessage());
		}
		return result;
	}
}
