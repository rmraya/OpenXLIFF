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
package com.maxprograms.converters.mif;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.Utils;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;

import org.xml.sax.SAXException;

public class Mif2Xliff {

	private static FileOutputStream output;
	private static FileOutputStream skeleton;
	private static String sourceLanguage;
	private static ArrayList<String> translatable;
	private static String segment;
	private static int segId;
	private static Map<String, String> charmap;

	private Mif2Xliff() {
		// do not instantiate this class
		// use run method instead
	}

	public static List<String> run(Map<String, String> params) {

		List<String> result = new ArrayList<>();

		String inputFile = params.get("source");
		String xliffFile = params.get("xliff");
		String skeletonFile = params.get("skeleton");
		sourceLanguage = params.get("srcLang");
		String targetLanguage = params.get("tgtLang");
		String encoding = params.get("srcEncoding");
		String tgtLang = "";
		if (targetLanguage != null) {
			tgtLang = "\" target-language=\"" + targetLanguage;
		}

		fillTranslatable();

		boolean inPara = false;
		segment = "";
		segId = 1;
		int tagId = 1;

		try {
			loadCharMap();
			try (FileReader input = new FileReader(inputFile)) {
				BufferedReader buffer = new BufferedReader(input);

				output = new FileOutputStream(xliffFile);
				writeString("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
				writeString("<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" "
						+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
						+ "xsi:schemaLocation=\"urn:oasis:names:tc:xliff:document:1.2 xliff-core-1.2-transitional.xsd\">\n");
				writeString("<file original=\"" + inputFile + "\" source-language=\"" + sourceLanguage + tgtLang
						+ "\" tool-id=\"" + Constants.TOOLID + "\" datatype=\"mif\">\n");
				writeString("<header>\n");
				writeString("   <skl>\n");
				writeString("      <external-file href=\"" + Utils.cleanString(skeletonFile) + "\"/>\n");
				writeString("   </skl>\n");
				writeString("   <tool tool-version=\"" + Constants.VERSION + " " + Constants.BUILD + "\" tool-id=\""
						+ Constants.TOOLID + "\" tool-name=\"" + Constants.TOOLNAME + "\"/>\n");
				writeString("</header>\n");
				writeString("<?encoding " + encoding + "?>\n");
				writeString("<body>\n");

				skeleton = new FileOutputStream(skeletonFile);

				String type = null;
				String content = null;
				int starts;
				int ends;
				Stack<String> typesList = new Stack<>();

				String line;

				while ((line = buffer.readLine()) != null) {
					if (line.length() == 0) {
						line = buffer.readLine();
						continue;
					}
					char first = line.charAt(0);
					int count = 0;
					while (Character.isSpaceChar(first) && count < line.length()) {
						first = line.charAt(count++);
					}
					if (first == '&' || first == '=') {
						writeSkeleton(line + "\n");
						line = buffer.readLine();
						continue;
					}
					starts = line.indexOf('<');
					ends = line.indexOf('>');

					if (starts != -1 && first == '<') { // element starts here
						String trimmed = line.substring(starts + 1);
						int stops = trimmed.indexOf(' ');
						if (ends == -1) {
							// no clossing '>'
							// compound element starts here
							type = trimmed.substring(0, trimmed.length() - 1).toLowerCase();
							typesList.push(type.toLowerCase());
							if (type.equals("para")) {
								inPara = true;
							}
						} else {
							// the full element is defined in one line
							// get the content
							if (stops != -1) {
								type = trimmed.substring(0, stops).toLowerCase();
							} else {
								// we are not in an element!
								// ignore and let crash
							}
						}
						if (inPara) {
							if (!translatable.contains(type)) {
								if (!segment.equals("")) {
									if (segment.endsWith("</ph>")) {
										segment = segment.substring(0, segment.length() - 5);
										segment += "\n" + cleanTag(line) + "</ph>";
									} else {
										segment += "<ph id=\"" + tagId++ + "\">" + cleanTag(line) + "</ph>";
									}
								} else {
									writeSkeleton(line + "\n");
								}
							} else {
								content = trimmed.substring(stops + 2, trimmed.lastIndexOf("'>"));
								// remove comments at the end of the line
								content = removeComments(content);
								// check for ilegal characters
								content = cleanString(replaceChars(content));
								if (segment.equals("")) {
									writeSkeleton("%%%" + segId + "%%%\n");
								}
								segment += content;
							}
						} else {
							writeSkeleton(line + "\n");
						}
					}

					if (ends != -1 && starts == -1) {
						// close previous compound element
						if (!typesList.isEmpty()) {
							type = typesList.pop();
						}
						if (inPara && !segment.equals("")) {
							if (segment.endsWith("</ph>")) {
								segment = segment.substring(0, segment.length() - 5);
								segment += "\n" + cleanTag(line) + "</ph>";
							} else {
								segment += "<ph id=\"" + tagId++ + "\">" + cleanTag(line) + "</ph>";
							}
						} else {
							writeSkeleton(line + "\n");
						}
						if (type != null && type.equals("para")) {
							if (!segment.equals("")) {
								writeSegment();
							}
							inPara = false;
							segment = "";
							tagId = 1;
						}
					}

					if (ends == -1 && starts == -1) {
						// write comment to skeleton
						writeSkeleton(line + "\n");
					}
				}

				skeleton.close();

				writeString("</body>\n");
				writeString("</file>\n");
				writeString("</xliff>");
			}
			output.close();
			result.add(Constants.SUCCESS);
		} catch (IOException | SAXException | ParserConfigurationException e) {
			Logger logger = System.getLogger(Mif2Xliff.class.getName());
			logger.log(Level.ERROR, "Error converting MIF file", e);
			result.add(Constants.ERROR);
			result.add(e.getMessage());
		}

		return result;
	}

	private static String replaceChars(String string) {
		string = replaceAll(string, "\\t", "\u0009");
		string = replaceAll(string, "\\q", "\'");
		string = replaceAll(string, "\\Q", "\"");
		string = replaceAll(string, "\\>", ">");
		string = replaceAll(string, "\\\\", "\\");
		return string;
	}

	private static String replaceAll(String string, String token, String newText) {
		int index = string.indexOf(token);
		while (index != -1) {
			String before = string.substring(0, index);
			String after = string.substring(index + token.length());
			string = before + newText + after;
			index = string.indexOf(token, index + newText.length());
		}
		return string;
	}

	/**
	 * This method cleans the text that will be stored inside <ph>elements in the
	 * XLIFF file *
	 */
	private static String cleanTag(String line) {
		String s = line.replace("&", "&amp;");
		s = s.replace("<", "&lt;");
		s = s.replace(">", "&gt;");
		return s;
	}

	/**
	 * This method saves each segment into the XLIFF file
	 * 
	 * If segment text ends in a <ph>element, it is removed and added to the
	 * skeleton file instead.
	 */
	private static void writeSegment() throws IOException {
		if (segment.equals("")) {
			return;
		}
		if (segment.endsWith("</ph>")) {
			String ph = segment.substring(segment.lastIndexOf("<ph"));
			segment = segment.substring(0, segment.lastIndexOf("<ph"));
			ph = ph.substring(ph.indexOf('>') + 1, ph.length() - 5);
			writeSkeleton(restoreTags(ph) + "\n");
		}
		writeString("   <trans-unit id=\"" + segId++ + "\" xml:space=\"preserve\">\n" + "      <source xml:lang=\""
				+ sourceLanguage + "\">" + segment + "</source>\n" + "   </trans-unit>\n");
	}

	/**
	 * This method restores '&', ' <' and '>' characters to the text that will be
	 * saved in the skeleton file. Those characters were converted to entities when
	 * they were tentatively added to the segment.
	 */
	private static String restoreTags(String ph) {
		int index = ph.indexOf("&gt;");
		while (index != -1) {
			ph = ph.substring(0, index) + ">" + ph.substring(index + 4);
			index = ph.indexOf("&gt;", index);
		}
		index = ph.indexOf("&lt;");
		while (index != -1) {
			ph = ph.substring(0, index) + "<" + ph.substring(index + 4);
			index = ph.indexOf("&lt;", index);
		}
		index = ph.indexOf("&amp;");
		while (index != -1) {
			ph = ph.substring(0, index) + "&" + ph.substring(index + 5);
			index = ph.indexOf("&amp;", index);
		}
		return ph;
	}

	private static void writeString(String string) throws IOException {
		output.write(string.getBytes(StandardCharsets.UTF_8));
	}

	private static void writeSkeleton(String string) throws IOException {
		skeleton.write(string.getBytes(StandardCharsets.UTF_8));
	}

	private static String cleanString(String s) {
		int control = s.indexOf("\\x");
		while (control != -1) {
			String code = s.substring(control + 2, s.indexOf(' ', control));

			String character = "" + getCharValue(Integer.valueOf(code, 16).intValue());
			if (!character.equals("")) {
				s = s.substring(0, control) + character + s.substring(1 + s.indexOf(' ', control));
			}
			control++;
			control = s.indexOf("\\x", control);
		}

		return cleanTag(s);
	}

	private static void fillTranslatable() {
		translatable = new ArrayList<>();
		translatable.add("string");
	}

	private static String removeComments(String string) {
		int ends = string.lastIndexOf('>');
		if (ends != -1 && ends < string.lastIndexOf('#')) {
			string = string.substring(0, string.lastIndexOf('>'));
		}
		return string;
	}

	private static char getCharValue(int value) {
		switch (value) {
		case 0x04:
			return '\u0004';
		case 0x05:
			return '\u0005';
		case 0x08:
			return '\u0008';
		case 0x09:
			return '\u0009';
		case 0x0a:
			return '\u0010';
		case 0x10:
			return '\u0016';
		case 0x11:
			return '\u0017';
		case 0x12:
			return '\u0018';
		case 0x13:
			return '\u0019';
		case 0x14:
			return '\u0020';
		case 0x15:
			return '\u0021';
		}
		if (value > 0x7f) {
			String key = "\\x" + Integer.toHexString(value);
			if (charmap.containsKey(key)) {
				String result = charmap.get(key);
				if (result.length() > 0) {
					return result.charAt(0);
				}
			}
		}
		return (char) value;
	}

	private static void loadCharMap() throws SAXException, IOException, ParserConfigurationException {
		SAXBuilder cbuilder = new SAXBuilder();
		Document cdoc = cbuilder.build(Mif2Xliff.class.getResource("init_mif.xml"));
		charmap = new HashMap<>();
		Element croot = cdoc.getRootElement();
		List<Element> codes = croot.getChildren("char");
		Iterator<Element> it = codes.iterator();
		while (it.hasNext()) {
			Element e = it.next();
			charmap.put(e.getAttributeValue("code"), e.getText());
		}
	}

}
