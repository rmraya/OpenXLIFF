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
package com.maxprograms.converters.po;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.lang.System.Logger.Level;
import java.lang.System.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.converters.UnexistentSegmentException;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;

public class Xliff2Po {

	private static String xliffFile;
	private static Hashtable<String, Element> segments;
	private static FileOutputStream output;
	private static String encoding;

	private Xliff2Po() {
		// do not instantiate this class
		// use run method instead
	}

	public static Vector<String> run(Hashtable<String, String> params) {

		Vector<String> result = new Vector<>();

		String sklFile = params.get("skeleton");
		xliffFile = params.get("xliff");
		encoding = params.get("encoding");

		try {
			String outputFile = params.get("backfile");
			File f = new File(outputFile);
			if (!f.getParentFile().exists()) {
				f.getParentFile().mkdirs();
			}
			if (!f.exists()) {
				Files.createFile(Paths.get(f.toURI()));
			}
			output = new FileOutputStream(f);
			loadSegments();
			try (InputStreamReader input = new InputStreamReader(new FileInputStream(sklFile),
					StandardCharsets.UTF_8)) {
				BufferedReader buffer = new BufferedReader(input);
				String line;
				while ((line = buffer.readLine()) != null) {
					line = line + "\n";

					if (line.indexOf("%%%") != -1) {
						//
						// contains translatable text
						//
						int index = line.indexOf("%%%");
						while (index != -1) {
							String start = line.substring(0, index);
							writeString(start);
							line = line.substring(index + 3);
							String code = line.substring(0, line.indexOf("%%%"));
							line = line.substring(line.indexOf("%%%") + 3);
							Element segment = segments.get(code);
							if (segment != null) {
								writeSegment(segment);
							} else {
								MessageFormat mf = new MessageFormat("Segment {0} not found.");
								throw new UnexistentSegmentException(mf.format(new Object[] { code }));
							}

							index = line.indexOf("%%%");
							if (index == -1) {
								writeString(line);
							}
						} // end while
					} else {
						//
						// non translatable portion
						//
						writeString(line);
					}
				}
			}
			output.close();
			result.add("0");
		} catch (IOException | SAXException | UnexistentSegmentException | ParserConfigurationException e) {
			Logger logger = System.getLogger(Xliff2Po.class.getName());
			logger.log(Level.ERROR, "Error merging PO file.", e);
			result.add("1");
			result.add(e.getMessage());
		}
		return result;
	}

	private static void writeSegment(Element segment) throws IOException {
		if (segment.getName().equals("trans-unit")) {
			// singular only
			Element target = segment.getChild("target");
			Element source = segment.getChild("source");
			boolean newLine = false;
			boolean fuzzy = false;
			if (!segment.getAttributeValue("approved", "no").equalsIgnoreCase("Yes") && target != null
					&& !target.getText().trim().equals("")) {
				fuzzy = true;
			}
			writeComments(segment);
			writeContext(segment);
			writeReferences(segment);
			writeFlags(segment, fuzzy);

			if (source.getText().endsWith("\n")) {
				newLine = true;
			} else {
				newLine = false;
			}
			if (!segment.getAttributeValue("restype", "").equals("x-gettext-domain-header")) {
				writeString("msgid \"" + addQuotes(source.getText()) + "\"\n");
			} else {
				writeString("msgid \"\"\n");
			}
			if (target != null) {
				String text = target.getText();
				if (newLine && !text.endsWith("\n")) {
					text = text + "\"\"\n";
				}
				writeString("msgstr \"" + addQuotes(text) + "\"");
			} else {
				writeString("msgstr \"\"");
			}
		} else {
			// has plurals
			writeComments(segment);
			writeContext(segment);
			writeReferences(segment);
			List<Element> units = segment.getChildren("trans-unit");
			boolean fuzzy = false;
			for (int i = 0; i < units.size(); i++) {
				if (units.get(i).getAttributeValue("approved").equalsIgnoreCase("no")) {
					fuzzy = true;
				}
			}
			writeFlags(segment, fuzzy);
			Element singular = units.get(0);
			Element source = singular.getChild("source");
			writeString("msgid \"" + addQuotes(source.getText()) + "\"\n");
			if (units.size() > 1) {
				Element plural = units.get(1);
				source = plural.getChild("source");
				writeString("msgid_plural \"" + addQuotes(source.getText()) + "\"\n");
				for (int i = 0; i < units.size(); i++) {
					Element target = units.get(i).getChild("target");
					if (target != null) {
						writeString("msgstr[" + i + "] \"" + addQuotes(target.getText()) + "\"\n");
					} else {
						writeString("msgstr[" + i + "] \"\"\n");
					}
				}
			}
		}
	}

	private static void writeFlags(Element segment, boolean fuzzy) throws IOException {
		List<Element> groups = segment.getChildren("prop-group");
		Iterator<Element> i = groups.iterator();
		String flags = "";
		while (i.hasNext()) {
			Element group = i.next();
			List<Element> contexts = group.getChildren();
			Iterator<Element> h = contexts.iterator();
			while (h.hasNext()) {
				Element prop = h.next();
				if (prop.getAttributeValue("ctype", "").equals("x-po-flags")) {
					flags = prop.getText();
				}
			}
		}
		if (fuzzy) {
			if (flags.indexOf("fuzzy") == -1) {
				writeString("#, fuzzy " + flags + "\n");
			} else {
				writeString("#, " + flags + "\n");
			}
		} else {
			if (flags.indexOf("fuzzy") == -1) {
				if (!flags.equals("")) {
					writeString("#, " + flags + "\n");
				}
			} else {
				flags = flags.substring(0, flags.indexOf("fuzzy")) + flags.substring(flags.indexOf("fuzzy") + 5);
				if (!flags.equals("")) {
					writeString("#, " + flags + "\n");
				}
			}
		}
	}

	private static void writeReferences(Element segment) throws IOException {
		String reference = "#:";
		String newContext = "msgctxt \"";
		List<Element> groups = segment.getChildren("context-group");
		Iterator<Element> i = groups.iterator();
		while (i.hasNext()) {
			Element group = i.next();
			if (group.getAttributeValue("name", "").startsWith("x-po-reference")
					&& group.getAttributeValue("purpose").equals("location")) {
				String file = "";
				String linenumber = "";
				List<Element> contexts = group.getChildren();
				Iterator<Element> h = contexts.iterator();
				while (h.hasNext()) {
					Element context = h.next();
					if (context.getAttributeValue("context-type", "").equals("sourcefile")) {
						file = context.getText();
					}
					if (context.getAttributeValue("context-type", "").equals("linenumber")) {
						linenumber = context.getText();
					}
				}
				String test = reference + " " + file + ":" + linenumber;
				if (test.substring(test.lastIndexOf("#:")).length() > 80) {
					reference = reference + "\n#:";
				}
				reference = reference + " " + file + ":" + linenumber;
			}
			if (group.getAttributeValue("name", "").startsWith("x-po-reference")
					&& group.getAttributeValue("purpose").equals("x-unknown")) {
				List<Element> contexts = group.getChildren();
				Iterator<Element> h = contexts.iterator();
				while (h.hasNext()) {
					Element context = h.next();
					reference = reference + " " + context.getText();
				}
			}
			if (group.getAttributeValue("name", "").startsWith("x-po-msgctxt")) {
				List<Element> contexts = group.getChildren();
				Iterator<Element> h = contexts.iterator();
				while (h.hasNext()) {
					Element context = h.next();
					newContext = newContext + context.getText();
				}
			}
		}
		if (!reference.equals("#:")) {
			writeString(reference + "\n");
		}
		if (!newContext.equals("msgctxt \"")) {
			writeString(newContext + "\"\n");
		}
	}

	private static void writeContext(Element segment) throws IOException {
		List<Element> groups = segment.getChildren("context-group");
		Iterator<Element> i = groups.iterator();
		while (i.hasNext()) {
			Element group = i.next();
			if (group.getAttributeValue("name", "").startsWith("x-po-entry-header")
					&& group.getAttributeValue("purpose").equals("information")) {
				List<Element> contexts = group.getChildren();
				Iterator<Element> h = contexts.iterator();
				while (h.hasNext()) {
					Element context = h.next();
					if (context.getAttributeValue("context-type", "").equals("x-po-autocomment")) {
						Vector<String> comments = splitLines(context.getText());
						for (int j = 0; j < comments.size(); j++) {
							String comment = comments.get(j);
							if (!comment.trim().equals("")) {
								writeString("#. " + comment.trim() + "\n");
							} else {
								writeString("#.\n");
							}
						}
					}
				}
			}
		}
	}

	private static void writeComments(Element segment) throws IOException {
		List<Element> notes = segment.getChildren("note");
		Iterator<Element> i = notes.iterator();
		while (i.hasNext()) {
			Element note = i.next();
			if (note.getAttributeValue("annotates", "general").equals("source")) {
				continue;
			}
			Vector<String> lines = splitLines(note.getText());
			Iterator<String> h = lines.iterator();
			while (h.hasNext()) {
				String comment = h.next();
				writeString("# " + comment.trim() + "\n");
			}
		}
	}

	private static Vector<String> splitLines(String text) {
		Vector<String> result = new Vector<>();
		StringTokenizer tokenizer = new StringTokenizer(text, "\n");
		if (text.startsWith("\n\n")) {
			result.add("");
		}
		while (tokenizer.hasMoreTokens()) {
			result.add(tokenizer.nextToken());
		}
		if (text.endsWith("\n\n")) {
			result.add("");
		}
		return result;
	}

	private static String addQuotes(String string) {
		return string.replaceAll("\n", "\"\n\"");
	}

	private static void loadSegments() throws SAXException, IOException, ParserConfigurationException {

		SAXBuilder builder = new SAXBuilder();

		Document doc = builder.build(xliffFile);
		Element root = doc.getRootElement();
		segments = new Hashtable<>();

		recurse(root);

	}

	private static void recurse(Element e) {
		List<Element> list = e.getChildren();
		Iterator<Element> i = list.iterator();
		while (i.hasNext()) {
			Element u = i.next();
			if (u.getName().equals("trans-unit")) {
				segments.put(u.getAttributeValue("id"), u);
			} else if (u.getName().equals("group") && u.getAttributeValue("restype", "").equals("x-gettext-plurals")) {
				segments.put(u.getAttributeValue("id"), u);
			} else {
				recurse(u);
			}
		}
	}

	private static void writeString(String string) throws IOException {
		output.write(string.getBytes(encoding));
	}

}
