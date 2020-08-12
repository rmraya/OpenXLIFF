/*******************************************************************************
 * Copyright (c) 2003-2020 Maxprograms.
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
 * Created on 24-nov-2004
 *
 */
package com.maxprograms.converters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.lang.System.Logger.Level;
import java.lang.System.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class EncodingResolver {

	private EncodingResolver() {
		// do not instantiate this class
	}

	private static final Logger LOGGER = System.getLogger(EncodingResolver.class.getName());

	public static Charset getEncoding(String fileName, String fileType) {
		if (fileType == null || fileName == null) {
			return null;
		} else if (fileType.equals(FileFormats.OO) || fileType.equals(FileFormats.OFF)
				|| fileType.equals(FileFormats.IDML)) {
			return StandardCharsets.UTF_8;
		} else if (fileType.equals(FileFormats.MIF)) {
			return StandardCharsets.US_ASCII;
		} else if (fileType.equals(FileFormats.JAVA)) {
			return StandardCharsets.ISO_8859_1;
		} else if (fileType.equals(FileFormats.PO)) {
			return StandardCharsets.UTF_8;
		} else if (fileType.equals(FileFormats.XML) || fileType.equals(FileFormats.XMLG)
				|| fileType.equals(FileFormats.TXML) || fileType.equals(FileFormats.RESX)
				|| fileType.equals(FileFormats.INX) || fileType.equals(FileFormats.TS)
				|| fileType.equals(FileFormats.DITA) || fileType.equals(FileFormats.SDLXLIFF)
				|| fileType.equals(FileFormats.WPML) || fileType.equals(FileFormats.XLIFF)) {
			try {
				return getXMLEncoding(fileName);
			} catch (IOException e) {
				LOGGER.log(Level.ERROR, "Error detecting XML encoding", e);
			}
		} else if (fileType.equals(FileFormats.SDLPPX)) {
			return StandardCharsets.UTF_8;
		} else if (fileType.equals(FileFormats.RC)) {
			try {
				return getRCEncoding(fileName);
			} catch (IOException e) {
				LOGGER.log(Level.ERROR, "Error detecting XML encoding", e);
			}
		} else if (fileType.equals(FileFormats.HTML)) {
			try {
				return getHTMLEncoding(fileName);
			} catch (IOException e) {
				LOGGER.log(Level.ERROR, "Error detecting HTML encoding", e);
			}
		}
		return null;
	}

	private static Charset getHTMLEncoding(String fileName) throws IOException {
		File f = new File(fileName);
		Document doc = Jsoup.parse(f, StandardCharsets.UTF_8.name());
		Elements list = doc.getElementsByAttributeValue("http-equiv", "Content-Type");
		if (list != null) {
			for (int i = 0; i < list.size(); i++) {
				Element e = list.get(i);
				if (e.toString().indexOf("charset=") != -1) {
					String part = e.toString().substring(e.toString().indexOf("charset=") + "charset=".length());
					if (part.indexOf('\"') != -1) {
						part = part.substring(0, part.indexOf('\"')).trim();
						String[] pageCodes = getPageCodes();
						for (int h = 0; h < pageCodes.length; h++) {
							if (pageCodes[h].equalsIgnoreCase(part)) {
								return Charset.forName(pageCodes[h]);
							}
						}
					}
				}
			}
		}
		list = doc.getElementsByAttribute("charset");
		if (list != null) {
			for (int i = 0; i < list.size(); i++) {
				Element e = list.get(i);
				String charset = e.attr("charset");
				String[] pageCodes = getPageCodes();
				for (int h = 0; h < pageCodes.length; h++) {
					if (pageCodes[h].equalsIgnoreCase(charset)) {
						return Charset.forName(pageCodes[h]);
					}
				}
			}
		}
		DocumentType type = doc.documentType();
		if (type != null) {
			if ("html".equals(type.name()) && type.systemId().isEmpty() && type.publicId().isEmpty()) {
				// HTML5
				return StandardCharsets.UTF_8;
			}
			// HTML 4 or older
			return StandardCharsets.ISO_8859_1;
		}
		return null;
	}

	private static Charset getRCEncoding(String fileName) throws IOException {
		try (FileInputStream input = new FileInputStream(fileName)) {
			// read 4K bytes
			int read = 4096;
			if (input.available() < read) {
				read = input.available();
			}
			byte[] bytes = new byte[read];
			if (input.read(bytes) == -1) {
				throw new IOException("Premature end of file");
			}

			String content = new String(bytes);

			if (content.indexOf("code_page(") != -1) {
				String code = content.substring(content.indexOf("code_page(") + 10);
				code = code.substring(0, code.indexOf(')'));
				return Charset.forName(parseMicrosoftEncoding(code));
			}
		}
		return null;
	}

	private static Charset getXMLEncoding(String fileName) throws IOException {
		// return UTF-8 as default
		String result = StandardCharsets.UTF_8.name();
		// check if there is a BOM (byte order mark)
		// at the start of the document
		Charset bom = getBOM(fileName);
		if (bom != null) {
			return bom;
		}
		// check declared encoding
		try (FileReader input = new FileReader(fileName)) {
			BufferedReader buffer = new BufferedReader(input);
			String line = buffer.readLine();
			if (line.startsWith("<?")) {
				line = line.substring(2, line.indexOf("?>"));
				line = line.replace("\'", "\"");
				StringTokenizer tokenizer = new StringTokenizer(line);
				while (tokenizer.hasMoreTokens()) {
					String token = tokenizer.nextToken();
					if (token.startsWith("encoding")) {
						result = token.substring(token.indexOf('\"') + 1, token.lastIndexOf('\"'));
					}
				}
			}
		}

		String[] encodings = getPageCodes();
		for (int i = 0; i < encodings.length; i++) {
			if (encodings[i].equalsIgnoreCase(result)) {
				return Charset.forName(encodings[i]);
			}
		}
		return Charset.forName(result);
	}

	public static Charset getBOM(String fileName) throws IOException {
		byte[] array = new byte[3];
		try (FileInputStream inputStream = new FileInputStream(fileName)) {
			if (inputStream.read(array) == -1) {
				throw new IOException("Premature end of file");
			}
		}
		byte[] lt = "<".getBytes();
		byte[] feff = { -1, -2 }; // UTF-16BE
		byte[] fffe = { -2, -1 }; // UTF-16LE
		byte[] efbbbf = { -17, -69, -65 }; // UTF-8
		if (array[0] != lt[0]) {
			// there is a BOM, now check the order
			if (array[0] == fffe[0] && array[1] == fffe[1]) {
				return StandardCharsets.UTF_16BE;
			}
			if (array[0] == feff[0] && array[1] == feff[1]) {
				return StandardCharsets.UTF_16LE;
			}
			if (array[0] == efbbbf[0] && array[1] == efbbbf[1] && array[2] == efbbbf[2]) {
				return StandardCharsets.UTF_8;
			}
		}
		return null;
	}

	private static String parseMicrosoftEncoding(String encoding) {
		String[] codes = getPageCodes();
		for (int h = 0; h < codes.length; h++) {
			if (codes[h].toLowerCase().indexOf("windows-" + encoding) != -1) {
				return codes[h];
			}
		}
		if (encoding.equals("10000")) {
			for (int h = 0; h < codes.length; h++) {
				if (codes[h].toLowerCase().indexOf("macroman") != -1) {
					return codes[h];
				}
			}
		}
		if (encoding.equals("10006")) {
			for (int h = 0; h < codes.length; h++) {
				if (codes[h].toLowerCase().indexOf("macgreek") != -1) {
					return codes[h];
				}
			}
		}
		if (encoding.equals("10007")) {
			for (int h = 0; h < codes.length; h++) {
				if (codes[h].toLowerCase().indexOf("maccyrillic") != -1) {
					return codes[h];
				}
			}
		}
		if (encoding.equals("10029")) {
			for (int h = 0; h < codes.length; h++) {
				if (codes[h].toLowerCase().indexOf("maccentraleurope") != -1) {
					return codes[h];
				}
			}
		}
		if (encoding.equals("10079")) {
			for (int h = 0; h < codes.length; h++) {
				if (codes[h].toLowerCase().indexOf("maciceland") != -1) {
					return codes[h];
				}
			}
		}
		if (encoding.equals("10081")) {
			for (int h = 0; h < codes.length; h++) {
				if (codes[h].toLowerCase().indexOf("macturkish") != -1) {
					return codes[h];
				}
			}
		}
		if (encoding.equals("65000")) {
			for (int h = 0; h < codes.length; h++) {
				if (codes[h].toLowerCase().indexOf("utf-7") != -1) {
					return codes[h];
				}
			}
		}
		if (encoding.equals("650001")) {
			for (int h = 0; h < codes.length; h++) {
				if (codes[h].toLowerCase().indexOf("utf-8") != -1) {
					return codes[h];
				}
			}
		}
		if (encoding.equals("932")) {
			for (int h = 0; h < codes.length; h++) {
				if (codes[h].toLowerCase().indexOf("shift_jis") != -1) {
					return codes[h];
				}
			}
		}
		if (encoding.equals("936")) {
			for (int h = 0; h < codes.length; h++) {
				if (codes[h].toLowerCase().indexOf("gbk") != -1) {
					return codes[h];
				}
			}
		}
		if (encoding.equals("949")) {
			for (int h = 0; h < codes.length; h++) {
				if (codes[h].toLowerCase().indexOf("euc-kr") != -1) {
					return codes[h];
				}
			}
		}
		if (encoding.equals("950")) {
			for (int h = 0; h < codes.length; h++) {
				if (codes[h].toLowerCase().indexOf("big5") != -1) {
					return codes[h];
				}
			}
		}
		if (encoding.equals("1361")) {
			for (int h = 0; h < codes.length; h++) {
				if (codes[h].toLowerCase().indexOf("johab") != -1) {
					return codes[h];
				}
			}
		}
		return null;
	}

	public static String[] getPageCodes() {
		TreeMap<String, Charset> charsets = new TreeMap<>(Charset.availableCharsets());
		Set<String> keys = charsets.keySet();
		String[] codes = new String[keys.size()];

		Iterator<String> i = keys.iterator();
		int j = 0;
		while (i.hasNext()) {
			Charset cset = charsets.get(i.next());
			codes[j++] = cset.displayName();
		}
		return codes;
	}
}
