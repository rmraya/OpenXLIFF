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
package com.maxprograms.converters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.Indenter;
import com.maxprograms.xml.PI;
import com.maxprograms.xml.SAXBuilder;

public class Join {

	private Join() {
		// do not instantiate this class
	}

	public static void join(List<String> xliffs, String out)
			throws IOException, SAXException, ParserConfigurationException {
		SAXBuilder builder = new SAXBuilder();

		try (FileOutputStream output = new FileOutputStream(out)) {
			writeString(output, "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
			writeString(output, "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" "
					+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
					+ "xsi:schemaLocation=\"urn:oasis:names:tc:xliff:document:1.2 xliff-core-1.2-transitional.xsd\">\n");

			TreeSet<String> set = new TreeSet<>();
			for (int i = 0; i < xliffs.size(); i++) {
				Document doc = builder.build(xliffs.get(i));
				Element root = doc.getRootElement();
				List<Element> list = root.getChildren("file");
				for (int j = 0; j < list.size(); j++) {
					Element file = list.get(j);
					String original = file.getAttributeValue("original");
					set.add(original);
				}
			}
			String treeRoot = findTreeRoot(set);

			Iterator<String> it = xliffs.iterator();
			while (it.hasNext()) {
				String xliff = it.next();

				Document doc = builder.build(xliff);
				Element root = doc.getRootElement();
				List<PI> pis = root.getPI();
				List<Element> files1 = root.getChildren("file");

				for (int i = 0; i < files1.size(); i++) {
					Element file = files1.get(i);
					String original = file.getAttributeValue("original");
					file.setAttribute("original", Utils.makeRelativePath(treeRoot, original));
					Iterator<PI> ip = pis.iterator();
					while (ip.hasNext()) {
						PI pi = ip.next();
						file.addContent(new PI(pi.getTarget(), pi.getData()));
					}
					Indenter.indent(file, 2, 2);
					file.writeBytes(output, StandardCharsets.UTF_8);

					writeString(output, "\n");
				}
				File f = new File(xliff);
				Files.delete(f.toPath());
			}
			writeString(output, "</xliff>");
		}
	}

	private static void writeString(FileOutputStream output, String string) throws IOException {
		output.write(string.getBytes(StandardCharsets.UTF_8));
	}

	public static String findTreeRoot(SortedSet<String> set) {
		StringBuilder result = new StringBuilder();
		MTree<String> tree = filesTree(set);
		MTree.Node<String> root = tree.getRoot();
		while (root.size() == 1) {
			result.append(root.getData());
			root = root.getChild(0);
		}
		return result.toString();
	}

	private static MTree<String> filesTree(SortedSet<String> files) {
		MTree<String> result = new MTree<>("");
		Iterator<String> it = files.iterator();
		while (it.hasNext()) {
			String s = it.next();
			StringTokenizer st = new StringTokenizer(s, "/\\:", true);
			MTree.Node<String> current = result.getRoot();
			while (st.hasMoreTokens()) {
				String name = st.nextToken();
				MTree.Node<String> level1 = current.getChild(name);
				if (level1 != null) {
					current = level1;
				} else {
					current.addChild(new MTree.Node<>(name));
					current = current.getChild(name);
				}
			}
		}
		return result;
	}
}
