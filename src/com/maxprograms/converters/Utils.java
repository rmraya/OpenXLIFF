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
package com.maxprograms.converters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import com.maxprograms.languages.RegistryParser;
import com.maxprograms.xml.XMLUtils;

public class Utils {

	protected static final Logger logger = System.getLogger(Utils.class.getName());
	private static RegistryParser registry;

	private Utils() {
		// do not instantiate this class
	}

	public static String cleanString(String string) {
		String result = string.replace("&", "&amp;");
		result = result.replace("<", "&lt;");
		result = result.replace(">", "&gt;");
		return XMLUtils.validChars(result);
	}

	public static String getAbsolutePath(File homeFile, String relative) throws IOException {
		return getAbsolutePath(homeFile.getAbsolutePath(), relative);
	}

	public static String getAbsolutePath(String homeFile, String relative) throws IOException {
		try {
			if (relative.indexOf('%') != -1) {
				try {
					String decoded = URLDecoder.decode(relative, StandardCharsets.UTF_8);
					relative = decoded;
				} catch (IllegalArgumentException e) {
					// do nothing, '%' may be part of the name
				}
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
			logger.log(Level.ERROR, Messages.getString("Utils.1"), e);
			throw e;
		}
	}

	public static String getRelativePath(String home, String file) throws IOException {
		File homeFile = new File(home);
		if (!homeFile.isAbsolute()) {
			MessageFormat mf = new MessageFormat(Messages.getString("Utils.2"));
			throw new IOException(mf.format(new String[] { home }));
		}
		if (homeFile.isFile()) {
			homeFile = homeFile.getParentFile();
		}
		Path homePath = homeFile.toPath();
		Path filePath = new File(file).toPath();
		if (homePath.getRoot().equals(filePath.getRoot())) {
			Path relative = homePath.relativize(filePath);
			return relative.toString();
		}
		return filePath.toString();
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

	public static String encodeFromFile(String filename) throws IOException {
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

	public static String[] fixPath(String[] args) {
		List<String> result = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.startsWith("-")) {
				if (!current.isEmpty()) {
					result.add(current.toString().trim());
					current = new StringBuilder();
				}
				result.add(arg);
			} else {
				current.append(' ');
				current.append(arg);
			}
		}
		if (!current.isEmpty()) {
			result.add(current.toString().trim());
		}
		return result.toArray(new String[result.size()]);
	}

	public static boolean lookingAt(String target, String text, int start) {
		if (target.length() > text.length() + start) {
			return false;
		}
		for (int i = 0; i < target.length(); i++) {
			if (target.charAt(i) != text.charAt(i + start)) {
				return false;
			}
		}
		return true;
	}
}
