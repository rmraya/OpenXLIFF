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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
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
import java.util.Vector;

import com.maxprograms.languages.RegistryParser;
import com.maxprograms.xml.XMLUtils;

public class Utils {

	protected static final Logger logger = System.getLogger(Utils.class.getName());
	private static RegistryParser registry;

	private Utils() {
		// do not instantiate this class
	}

	public static String cleanString(String string) {
		String result = string.replaceAll("&", "&amp;");
		result = result.replaceAll("<", "&lt;");
		result = result.replaceAll(">", "&gt;");
		return XMLUtils.validChars(result);
	}

	public static String getAbsolutePath(String homeFile, String relative) throws IOException {
		try {
			File result = relative.indexOf('%') != -1 ? new File(URLDecoder.decode(relative, StandardCharsets.UTF_8))
					: new File(relative);
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


	public static String makeRelativePath(String homeFile, String filename) throws IOException {
		File home = new File(homeFile);
		// If home is a file, get the parent
		if (!home.isDirectory()) {
			if (home.getParent() != null) {
				home = new File(home.getParent());	
			} else {
				home = new File(System.getProperty("user.dir")); 
			}
			
		}
		File file = new File(filename);
		if (!file.isAbsolute()) {
			return filename;
		}
		// Check for relative path
		if (!home.isAbsolute()) {
			throw new IOException("Path must be absolute.");
		}
		Vector<String> homelist;
		Vector<String> filelist;

		homelist = getPathList(home);
		filelist = getPathList(file);
		return matchPathLists(homelist, filelist);
	}

	private static Vector<String> getPathList(File file) throws IOException{
		Vector<String> list = new Vector<>();
		File r;
		r = file.getCanonicalFile();
		while(r != null) {
			list.add(r.getName());
			r = r.getParentFile();
		}
		return list;
	}

	private static String matchPathLists(Vector<String> r, Vector<String> f) {
		int i;
		int j;
		String s = ""; 
		// start at the beginning of the lists
		// iterate while both lists are equal
		i = r.size()-1;
		j = f.size()-1;

		// first eliminate common root
		while(i >= 0&&j >= 0&&r.get(i).equals(f.get(j))) {
			i--;
			j--;
		}

		// for each remaining level in the home path, add a ..
		for(;i>=0;i--) {
			s += ".." + File.separator; 
		}

		// for each level in the file path, add the path
		for(;j>=1;j--) {
			s += f.get(j) + File.separator;
		}

		// file name
		if ( j>=0 && j<f.size()) {
			s += f.get(j);
		}
		return s;
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
		Vector<String> result = new Vector<>();
		String current = "";
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.startsWith("-")) {
				if (!current.isEmpty()) {
					result.add(current.trim());
					current = "";
				}
				result.add(arg);
			} else {
				current = current + " " + arg;
			}
		}
		if (!current.isEmpty()) {
			result.add(current.trim());
		}
		return result.toArray(new String[result.size()]);
	}	
}
