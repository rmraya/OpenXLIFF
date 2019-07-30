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
package com.maxprograms.converters.javaproperties;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.Vector;
import java.lang.System.Logger.Level;
import java.lang.System.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.Utils;
import com.maxprograms.segmenter.Segmenter;

public class Properties2Xliff {

	private static FileOutputStream output;
	private static FileOutputStream skeleton;
	private static String source;
	private static String sourceLanguage;
	private static int segId;
	private static Segmenter segmenter;
	private static boolean segByElement;

	private Properties2Xliff() {
		// do not instantiate this class
		// use run method instead
	}

	public static Vector<String> run(Hashtable<String, String> params) {
		Vector<String> result = new Vector<>();
		segId = 0;

		String inputFile = params.get("source");
		String xliffFile = params.get("xliff");
		String skeletonFile = params.get("skeleton");
		sourceLanguage = params.get("srcLang");
		String targetLanguage = params.get("tgtLang");
		String srcEncoding = params.get("srcEncoding");
		String elementSegmentation = params.get("paragraph");
		String catalog = params.get("catalog");

		if (elementSegmentation == null) {
			segByElement = false;
		} else {
			if (elementSegmentation.equals("yes")) {
				segByElement = true;
			} else {
				segByElement = false;
			}
		}

		source = "";
		try {
			if (!segByElement) {
				String initSegmenter = params.get("srxFile");
				segmenter = new Segmenter(initSegmenter, sourceLanguage, catalog);
			}
			FileInputStream stream = new FileInputStream(inputFile);
			try (InputStreamReader input = new InputStreamReader(stream, srcEncoding)) {
				BufferedReader buffer = new BufferedReader(input);
				output = new FileOutputStream(xliffFile);
				String tgtLang = "";
				if (targetLanguage != null) {
					tgtLang = "\" target-language=\"" + targetLanguage;
				}

				writeString("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
				writeString("<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" "
						+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
						+ "xsi:schemaLocation=\"urn:oasis:names:tc:xliff:document:1.2 xliff-core-1.2-transitional.xsd\">\n");
				writeString("<?encoding " + srcEncoding + "?>\n");

				writeString("<file original=\"" + inputFile + "\" source-language=\"" + sourceLanguage + tgtLang
						+ "\" tool-id=\"" + Constants.TOOLID + "\" datatype=\"javapropertyresourcebundle\">\n");
				writeString("<header>\n");
				writeString("   <skl>\n");
				writeString("      <external-file href=\"" + skeletonFile + "\"/>\n");
				writeString("   </skl>\n");
				writeString("   <tool tool-version=\"" + Constants.VERSION + " " + Constants.BUILD + "\" tool-id=\""
						+ Constants.TOOLID + "\" tool-name=\"" + Constants.TOOLNAME + "\"/>\n");
				writeString("</header>\n");
				writeString("<body>\n");

				skeleton = new FileOutputStream(skeletonFile);

				String line;
				while ((line = buffer.readLine()) != null) {

					if (line.trim().length() == 0) {
						// no text in this line
						// segment separator
						writeSkeleton(line + "\n");
					} else if (line.trim().startsWith("#")) {
						// this line is a comment
						// send to skeleton
						writeSkeleton(line + "\n");
					} else {
						String tmp = line;
						if (line.endsWith("\\")) {
							do {
								line = buffer.readLine();
								tmp += "\n" + line;
							} while (line != null && line.endsWith("\\"));
						}
						int index = tmp.indexOf('=');
						if (index != -1) {
							String key = tmp.substring(0, index + 1);
							writeSkeleton(key);
							source = tmp.substring(index + 1);
							writeSegment();
							writeSkeleton("\n");
						} else {
							// this line may be wrong, send to skeleton
							// and continue
							writeSkeleton(tmp);
						}
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
			Logger logger = System.getLogger(Properties2Xliff.class.getName());
			logger.log(Level.ERROR, "Error converting .properties file", e);
			result.add(Constants.ERROR);
			result.add(e.getMessage());
		}

		return result;
	}

	private static void writeString(String string) throws IOException {
		output.write(string.getBytes(StandardCharsets.UTF_8));
	}

	private static void writeSkeleton(String string) throws IOException {
		skeleton.write(string.getBytes(StandardCharsets.UTF_8));
	}

	private static void writeSegment() throws IOException {
		String[] segments;
		if (!segByElement) {
			segments = segmenter.segment(fixChars(source));
		} else {
			segments = new String[1];
			segments[0] = fixChars(source);
		}
		for (int i = 0; i < segments.length; i++) {
			if (!segments[i].trim().equals("")) {
				writeString("   <trans-unit id=\"" + segId + "\" xml:space=\"preserve\" approved=\"no\">\n"
						+ "      <source xml:lang=\"" + sourceLanguage + "\">" + Utils.cleanString(segments[i])
						+ "</source>\n");
				writeString("   </trans-unit>\n");
				writeSkeleton("%%%" + segId++ + "%%%");
			} else {
				writeSkeleton(segments[i]);
			}
		}
		source = "";
	}

	private static String fixChars(String string) {
		String result = string;
		int start = result.indexOf("\\u");
		while (start != -1) {
			if (result.substring(start + 2, start + 6).toLowerCase()
					.matches("[\\dabcdef][\\dabcdef][\\dabcdef][\\dabcdef]")) {
				result = result.substring(0, start) + toChar(result.substring(start + 2, start + 6))
						+ result.substring(start + 6);
			}
			start = result.indexOf("\\u", start + 1);
		}
		return result;
	}

	private static String toChar(String string) {
		int hex = Integer.parseInt(string, 16);
		char result = (char) hex;
		return "" + result;
	}

}
