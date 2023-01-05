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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class StringConverter {

	private StringConverter() {
		// do not instantiate this class
	}

	public static String encodeString(String string) {
		if (string.isEmpty()) {
			return "";
		}
		return toHexString(string.getBytes(StandardCharsets.UTF_8));
	}

	public static String toHexString(byte[] array) {
		return toHexString(array, 0, array.length);
	}

	public static String decodeString(String string) {
		if (string.isEmpty()) {
			return "";
		}
		return new String(toByteArray(string), StandardCharsets.UTF_8);
	}

	public static byte[] toByteArray(String data) {
		if (data.isEmpty()) {
			return new byte[0];
		}
		int count = data.length() / 2;
		byte[] bytearray = new byte[count];
		for (int i = 0; i < count; i++) {
			String value = "" + data.charAt(i * 2) + data.charAt(i * 2 + 1);
			int hex = Integer.parseInt(value, 16);
			if (hex > 127) {
				hex = -1 * (hex - 127);
			}
			Integer in = Integer.valueOf(hex);
			byte ch = in.byteValue();
			bytearray[i] = ch;
		}
		return bytearray;
	}

	public static String toHexString(byte[] array, int offset, int length) {
		if (length == 0) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (int i = offset; i < length; i++) {
			int ch = array[i];
			if (ch < 0) {
				ch = -1 * ch + 127;
			}
			String str = Integer.toHexString(ch);
			if (str.length() < 2) {
				str = "0" + str;
			}
			builder.append(str);
		}
		return builder.toString();
	}

	public static void decodeFile(String source, String target) throws IOException {
		try (FileInputStream input = new FileInputStream(source)) {
			try (FileOutputStream output = new FileOutputStream(target)) {
				byte[] array = new byte[1024 * 10];
				int len;
				while ((len = input.read(array)) != -1) {
					String s = new String(array, 0, len, StandardCharsets.UTF_8);
					output.write(toByteArray(s));
				}
			}
		}
	}

	public static void encodeFile(String source, String target) throws IOException {
		try (FileInputStream input = new FileInputStream(source)) {
			try (FileOutputStream output = new FileOutputStream(target)) {
				byte[] array = new byte[1024 * 10];
				int len;
				while ((len = input.read(array)) != -1) {
					String s = toHexString(array, 0, len);
					output.write(s.getBytes(StandardCharsets.UTF_8));
				}
			}
		}
	}
}
