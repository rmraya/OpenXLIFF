/*******************************************************************************
 * Copyright (c) 2018 - 2026 Maxprograms.
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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.converters.Constants;
import com.maxprograms.segmenter.Segmenter;
import com.maxprograms.segmenter.SegmenterPool;
import com.maxprograms.xml.CatalogBuilder;
import com.maxprograms.xml.XMLUtils;

public class Properties2Xliff {

	private static final class Context {
		FileOutputStream output;
		FileOutputStream skeleton;
		String source = "";
		int segId;
		Segmenter segmenter;
		boolean segByElement;
	}

	private Properties2Xliff() {
		// do not instantiate this class
		// use run method instead
	}

	public static List<String> run(Map<String, String> params) {
		List<String> result = new ArrayList<>();
		Context ctx = new Context();
		ctx.segId = 0;

		String inputFile = params.get("source");
		String xliffFile = params.get("xliff");
		String skeletonFile = params.get("skeleton");
		String sourceLanguage = params.get("srcLang");
		String targetLanguage = params.get("tgtLang");
		String srcEncoding = params.get("srcEncoding");
		String elementSegmentation = params.get("paragraph");
		String catalog = params.get("catalog");

		if (elementSegmentation == null) {
			ctx.segByElement = false;
		} else {
			if (elementSegmentation.equals("yes")) {
				ctx.segByElement = true;
			} else {
				ctx.segByElement = false;
			}
		}

		ctx.source = "";
		try {
			if (!ctx.segByElement) {
				String initSegmenter = params.get("srxFile");
				ctx.segmenter = SegmenterPool.getSegmenter(initSegmenter, sourceLanguage, CatalogBuilder.getCatalog(catalog));
			}
			FileInputStream stream = new FileInputStream(inputFile);
			try (InputStreamReader input = new InputStreamReader(stream, srcEncoding)) {
				BufferedReader buffer = new BufferedReader(input);
				ctx.output = new FileOutputStream(xliffFile);
				String tgtLang = "";
				if (targetLanguage != null) {
					tgtLang = "\" target-language=\"" + targetLanguage;
				}

				writeString(ctx, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
				writeString(ctx, "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" "
						+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
						+ "xsi:schemaLocation=\"urn:oasis:names:tc:xliff:document:1.2 xliff-core-1.2-transitional.xsd\">\n");

				writeString(ctx, "<file original=\"" + inputFile + "\" source-language=\"" + sourceLanguage + tgtLang
						+ "\" tool-id=\"" + Constants.TOOLID + "\" datatype=\"javapropertyresourcebundle\">\n");
				writeString(ctx, "<header>\n");
				writeString(ctx, "   <skl>\n");
				writeString(ctx, "      <external-file href=\"" + skeletonFile + "\"/>\n");
				writeString(ctx, "   </skl>\n");
				writeString(ctx, "   <tool tool-version=\"" + Constants.VERSION + " " + Constants.BUILD + "\" tool-id=\""
						+ Constants.TOOLID + "\" tool-name=\"" + Constants.TOOLNAME + "\"/>\n");
				writeString(ctx, "</header>\n");
				writeString(ctx, "<?encoding " + srcEncoding + "?>\n");
				writeString(ctx, "<body>\n");

				ctx.skeleton = new FileOutputStream(skeletonFile);

				String line;
				while ((line = buffer.readLine()) != null) {
					if (line.isBlank()) {
						// no text in this line
						// segment separator
						writeSkeleton(ctx, line + "\n");
					} else if (line.trim().startsWith("#")) {
						// this line is a comment
						// send to skeleton
						writeSkeleton(ctx, line + "\n");
					} else {
						String tmp = line;
						if (line.endsWith("\\")) {
							do {
								line = buffer.readLine();
								tmp += '\n' + line;
							} while (line != null && line.endsWith("\\"));
						}
						int index = tmp.indexOf('=');
						if (index != -1) {
							String key = tmp.substring(0, index + 1);
							writeSkeleton(ctx, key);
							ctx.source = tmp.substring(index + 1);
							writeSegment(ctx, key);
							writeSkeleton(ctx, "\n");
						} else {
							// this line may be wrong, send to skeleton
							// and continue
							writeSkeleton(ctx, tmp);
						}
					}
				}

				ctx.skeleton.close();

				writeString(ctx, "</body>\n");
				writeString(ctx, "</file>\n");
				writeString(ctx, "</xliff>");
			}
			ctx.output.close();

			result.add(Constants.SUCCESS);
		} catch (IOException | SAXException | ParserConfigurationException | URISyntaxException e) {
			Logger logger = System.getLogger(Properties2Xliff.class.getName());
			logger.log(Level.ERROR, Messages.getString("Properties2Xliff.1"), e);
			result.add(Constants.ERROR);
			result.add(e.getMessage());
		}

		return result;
	}

	private static void writeString(Context ctx, String string) throws IOException {
		ctx.output.write(string.getBytes(StandardCharsets.UTF_8));
	}

	private static void writeSkeleton(Context ctx, String string) throws IOException {
		ctx.skeleton.write(string.getBytes(StandardCharsets.UTF_8));
	}

	private static void writeSegment(Context ctx, String key) throws IOException {
		String[] segments;
		if (!ctx.segByElement) {
			segments = ctx.segmenter.segment(fixChars(ctx.source));
		} else {
			segments = new String[1];
			segments[0] = fixChars(ctx.source);
		}
		for (int i = 0; i < segments.length; i++) {
			if (!segments[i].trim().isEmpty()) {
				writeString(ctx, "   <trans-unit id=\"" + ctx.segId + "\" xml:space=\"preserve\" approved=\"no\" resname=\""
						+ key.substring(0, key.length() - 1)
						+ "\">\n      <source>"
						+ XMLUtils.cleanText(segments[i])
						+ "</source>\n");
				writeString(ctx, "   </trans-unit>\n");
				writeSkeleton(ctx, "%%%" + ctx.segId++ + "%%%");
			} else {
				writeSkeleton(ctx, segments[i]);
			}
		}
		ctx.source = "";
	}

	private static String fixChars(String string) {
		String result = string.replace("\\n", "\n");
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
