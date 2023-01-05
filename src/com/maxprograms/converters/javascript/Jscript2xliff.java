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
package com.maxprograms.converters.javascript;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.Utils;

public class Jscript2xliff {

	private static FileOutputStream output;
	private static FileOutputStream skeleton;
	private static int segId;

	private Jscript2xliff() {
		// do not instantiate this class
		// use run method instead
	}

	public static List<String> run(Map<String, String> params) {
		List<String> result = new ArrayList<>();

		String inputFile = params.get("source");
		String xliffFile = params.get("xliff");
		String skeletonFile = params.get("skeleton");
		String sourceLanguage = params.get("srcLang");
		String targetLanguage = params.get("tgtLang");
		String encoding = params.get("srcEncoding");
		String tgtLang = "";
		if (targetLanguage != null) {
			tgtLang = "\" target-language=\"" + targetLanguage;
		}

		try {
			try (FileInputStream stream = new FileInputStream(inputFile)) {
				try (InputStreamReader input = new InputStreamReader(stream, encoding)) {
					BufferedReader buffer = new BufferedReader(input);
					output = new FileOutputStream(xliffFile);

					writeString("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
					writeString("<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" "
							+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
							+ "xsi:schemaLocation=\"urn:oasis:names:tc:xliff:document:1.2 xliff-core-1.2-transitional.xsd\">\n");
					writeString("<file original=\"" + inputFile + "\" source-language=\"" + sourceLanguage + tgtLang
							+ "\" tool-id=\"" + Constants.TOOLID + "\" datatype=\"javascript\">\n");
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

					String line;
					String comment = "";
					while ((line = buffer.readLine()) != null) {
						line = line + "\n";
						comment = findComment(line);
						if (!comment.isEmpty()) {
							line = line.substring(0, line.indexOf(comment));
						}

						// Pending: check for /* block comments */

						if (line.indexOf('\"') == -1 && line.indexOf('\'') == -1) {
							// no text in this line
							writeSkeleton(line + comment);
						} else {
							// check for strings to extract
							int number = countQuotes(line, '\"') + countQuotes(line, '\'');
							if (number > 0 && number % 2 == 0) {
								// all strings closed in the same line
								extractStrings(line);
								writeSkeleton(comment);
							} else {
								// check if the line ends with "/"
								if (line.trim().endsWith("/") && !line.trim().endsWith("//")) {
									String nextLine = buffer.readLine();
									if (nextLine == null) {
										result.add(Constants.ERROR);
										result.add("Unexpected end of file.");
										return result;
									}
									comment = findComment(nextLine);
									if (!comment.isEmpty()) {
										nextLine = nextLine.substring(0, nextLine.indexOf(comment));
									}
									line = line + nextLine;
									continue;
								}
								result.add(Constants.ERROR);
								result.add("Found a string that is not properly closed.");
								return result;
							}
						}
					}

					skeleton.close();

					writeString("</body>\n");
					writeString("</file>\n");
					writeString("</xliff>");
				}
			}
			output.close();
			result.add(Constants.SUCCESS);
		} catch (IOException e) {
			Logger logger = System.getLogger(Jscript2xliff.class.getName());
			logger.log(Level.ERROR, "Error converting JavaScript file.", e);
			result.add(Constants.ERROR);
			result.add(e.getMessage());
		}
		return result;
	}

	private static String findComment(String line) {
		boolean inString = false;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == '\"' || c == '\'') {
				if (i > 0) {
					// not at start of line
					if (inString) {
						if (line.charAt(i - 1) == '\\') {
							// escaped quote, ignore
						} else {
							// close the string
							inString = false;
						}
					} else {
						// string starts here
						inString = true;
					}
				} else {
					// start of line
					inString = true;
				}
			}
			if (!inString && c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/') {
				// it's a comment!
				return line.substring(i);
			}
		}
		return "";
	}

	private static void extractStrings(String line) throws IOException {
		while (line.length() > 0) {
			line = checkForQuote(line, '\"');
			line = checkForQuote(line, '\'');
			if (line.indexOf('\"') == -1 && line.indexOf('\'') == -1) {
				// no more quoted sections in the string
				writeSkeleton(line);
				line = "";
			}
		} // line length > 0 ?
	}

	private static String checkForQuote(String line, char c) throws IOException {
		boolean isString = true;
		int index = line.indexOf(c);
		if (index > 0 && line.charAt(index - 1) == '\\') {
			isString = false;
		}
		if (isString) {
			String start = line.substring(0, index + 1);
			writeSkeleton(start);
			line = line.substring(index + 1);
			StringBuilder buff = new StringBuilder();
			for (int i = 0; i < line.length(); i++) {
				if (line.charAt(i) == c) {
					boolean endsString = true;
					if (i > 0 && line.charAt(i - 1) == '\\') {
						endsString = false;
					}
					if (endsString) {
						writeSegment(buff.toString());
						line = line.substring(buff.toString().length() + 1);
						writeSkeleton("" + c);
						break;
					}
				}
				buff.append(line.charAt(i));
			}
		}
		return line;
	}

	private static void writeSegment(String segment) throws IOException {
		if (segment.isEmpty()) {
			return;
		}
		writeString("   <trans-unit id=\"" + segId + "\" xml:space=\"preserve\">\n      <source>"
				+ Utils.cleanString(segment) + "</source>\n" + "   </trans-unit>\n");
		writeSkeleton("%%%" + segId++ + "%%%");
	}

	private static int countQuotes(String line, char quote) {
		int result = 0;
		int index = line.indexOf(quote);
		while (index != -1) {
			result++;
			if (index > 0 && line.charAt(index - 1) == '\\') {
				result--;
			}
			if (index < line.length()) {
				index++;
				index = line.indexOf(quote, index);
			} else {
				index = -1;
			}
		}
		return result;
	}

	private static void writeString(String string) throws IOException {
		output.write(string.getBytes(StandardCharsets.UTF_8));
	}

	private static void writeSkeleton(String string) throws IOException {
		skeleton.write(string.getBytes(StandardCharsets.UTF_8));
	}
}
