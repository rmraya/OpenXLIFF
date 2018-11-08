/*******************************************************************************
 * Copyright (c) 2003, 2018 Maxprograms.
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.lang.System.Logger.Level;
import java.lang.System.Logger;

import com.maxprograms.languages.RegistryParser;
import com.maxprograms.xml.XMLUtils;

public class Utils {

	protected static final Logger logger = System.getLogger(Utils.class.getName());
	private static RegistryParser registry;

	private Utils() {
		// do not instantiate this class
	}

	public static String cleanString(String input) {
		input = input.replaceAll("&", "&amp;");
		input = input.replaceAll("<", "&lt;");
		input = input.replaceAll(">", "&gt;");
		return XMLUtils.validChars(input);
	}

	public static String getAbsolutePath(String homeFile, String relative) throws IOException {
		try {
			if (relative.indexOf('%') != -1) {
				relative = URLDecoder.decode(relative, StandardCharsets.UTF_8);
			}
			File result = new File(relative);
			if (!result.isAbsolute()) {
				File home = new File(homeFile);
				// If home is a file, get the parent
				if (!home.isDirectory()) {
					home = home.getParentFile();
				}
				result = new File(home, relative);
			}
			return result.getCanonicalPath();
		} catch (IOException e) {
			logger.log(Level.ERROR, "Invalid path", e);
			throw e;
		}
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

	public static void decodeToFile(String dataToDecode, String filename) throws java.io.IOException {
		Decoder decoder = Base64.getMimeDecoder();
		try (FileOutputStream output = new FileOutputStream(filename)) {
			output.write(decoder.decode(dataToDecode));
		}
	}

	public static String encodeFromFile(String filename) throws java.io.IOException {
		File file = new File(filename);
		int size = Math.max((int) (file.length() * 1.4), 4096);
		byte[] buffer = new byte[size]; // Need max() for math on small files (v2.2.1)
		int length = 0;
		int numBytes = 0;
		try (FileInputStream input = new FileInputStream(file)) {
			while ((numBytes = input.read(buffer, length, size - length)) != -1) {
				length += numBytes;
			}
		}
		Encoder encoder = Base64.getMimeEncoder();
		return encoder.encodeToString(Arrays.copyOf(buffer, length));
	}

	public static boolean isValidLanguage(String lang) throws IOException {
		if (registry == null) {
			registry = new RegistryParser();
		}
		return !registry.getTagDescription(lang).isEmpty();
	}

}
