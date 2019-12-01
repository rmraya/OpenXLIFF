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
package com.maxprograms.xml;

import java.io.File;
import java.io.IOException;

public class XMLUtils {

	public static final byte[] UTF8BOM = { -17, -69, -65 };
	public static final byte[] UTF16BEBOM  = { -1, -2 };
	public static final byte[] UTF16LEBOM = { -2, -1 }; 

	private XMLUtils() {
		// do not instantiate
	}

	public static String cleanText(String string) {
		if (string == null) {
			return null;
		}
		String result = string.replace("&", "&amp;");
		result = result.replace("<", "&lt;");
		return result.replace(">", "&gt;");
	}

	public static String validChars(String input) {
		// Valid: #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] |
		// [#x10000-#x10FFFF]
		// Discouraged: [#x7F-#x84], [#x86-#x9F], [#xFDD0-#xFDDF]
		//
		StringBuilder buffer = new StringBuilder();
		char c;
		int length = input.length();
		for (int i = 0; i < length; i++) {
			c = input.charAt(i);
			if (c == '\t' || c == '\n' || c == '\r' || c >= '\u0020' && c <= '\uD7DF'
					|| c >= '\uE000' && c <= '\uFFFD') {
				// normal character
				buffer.append(c);
			} else if (c >= '\u007F' && c <= '\u0084' || c >= '\u0086' && c <= '\u009F'
					|| c >= '\uFDD0' && c <= '\uFDDF') {
				// Control character
				buffer.append("&#x" + Integer.toHexString(c) + ";");
			} else if (c >= '\uDC00' && c <= '\uDFFF' || c >= '\uD800' && c <= '\uDBFF') {
				// Multiplane character
				buffer.append(input.substring(i, i + 1));
			}
		}
		return buffer.toString();
	}

	public static String uncleanText(String string) {
		String result = string.replaceAll("&amp;", "&");
		result = result.replaceAll("&lt;", "<");
		result = result.replaceAll("&gt;", ">");
		result = result.replaceAll("&quot;", "\"");
		return result.replaceAll("&apos;", "\'");
	}

	public static String getAbsolutePath(String homeFile, String relative) throws IOException {
		File home = new File(homeFile);
		// If home is a file, get the parent
		File result;
		if (!home.isDirectory()) {
			home = home.getParentFile();
		}
		result = new File(home, relative);
		return result.getCanonicalPath();
	}
}
