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
/*
 * Created on Jun 2, 2004
 *
 */
package com.maxprograms.converters.plaintext;

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

import com.maxprograms.converters.Utils;
import com.maxprograms.segmenter.Segmenter;

public class Text2Xliff {

	private static FileOutputStream output;
	private static FileOutputStream skeleton;
	private static String source;
	private static String sourceLanguage;
	private static int segId;
	private static Segmenter segmenter;
	private static boolean segByElement;

	private Text2Xliff() {
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
		boolean breakOnCRLF = "yes".equals(params.get("breakOnCRLF"));
		String catalog = params.get("catalog");
		String tgtLang = "";
		if (targetLanguage != null) {
			tgtLang = "\" target-language=\"" + targetLanguage;
		}

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

				writeString("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
				writeString("<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" "
						+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
						+ "xsi:schemaLocation=\"urn:oasis:names:tc:xliff:document:1.2 xliff-core-1.2-transitional.xsd\">\n");
				writeString("<?encoding " + srcEncoding + "?>\n");

				writeString("<file original=\"" + inputFile + "\" source-language=\"" + sourceLanguage + tgtLang
						+ "\" datatype=\"plaintext\">\n");
				writeString("<header>\n");
				writeString("   <skl>\n");
				writeString("      <external-file href=\"" + Utils.cleanString(skeletonFile) + "\"/>\n");
				writeString("   </skl>\n");
				writeString("</header>\n");
				writeString("<body>\n");

				skeleton = new FileOutputStream(skeletonFile);

				if (breakOnCRLF) {
					source = buffer.readLine();
					while (source != null) {
						if (source.trim().length() == 0) {
							writeSkeleton(source + "\n");
						} else {
							writeSegment();
						}
						source = buffer.readLine();
					}
				} else {
					String line = buffer.readLine();
					while (line != null) {
						line = line + "\n";

						if (line.trim().length() == 0) {
							// no text in this line
							// segment separator
							writeSkeleton(line);
						} else {
							while (line != null && line.trim().length() != 0) {
								source = source + line;
								line = buffer.readLine();
								if (line != null) {
									line = line + "\n";
								}
							}
							writeSegment();
						}
						line = buffer.readLine();
					}
				}
				skeleton.close();

				writeString("</body>\n");
				writeString("</file>\n");
				writeString("</xliff>");
			}
			output.close();
			result.add("0"); // success
		} catch (IOException | SAXException | ParserConfigurationException e) {
			Logger logger = System.getLogger(Text2Xliff.class.getName());
			logger.log(Level.ERROR, "Error converting TEXT file", e);
			result.add("1");
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
			segments = segmenter.segment(source);
		} else {
			segments = new String[1];
			segments[0] = source;
		}
		for (int i = 0; i < segments.length; i++) {
			if (Utils.cleanString(segments[i]).trim().equals("")) {
				writeSkeleton(segments[i]);
			} else {
				writeString("   <trans-unit id=\"" + segId + "\" xml:space=\"preserve\" approved=\"no\">\n"
						+ "      <source xml:lang=\"" + sourceLanguage + "\">" + Utils.cleanString(segments[i])
						+ "</source>\n");
				writeString("   </trans-unit>\n");
				writeSkeleton("%%%" + segId++ + "%%%\n");
			}
		}
		source = "";
	}

}
