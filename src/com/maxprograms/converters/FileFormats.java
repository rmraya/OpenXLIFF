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
package com.maxprograms.converters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileFormats {

	private FileFormats() {
		// do not instantiate this class
	}

	public static final String INX = "Adobe InDesign Interchange";
	public static final String IDML = "Adobe InDesign IDML";
	public static final String DITA = "DITA Map";
	public static final String HTML = "HTML Page";
	public static final String JS = "JavaScript";
	public static final String JAVA = "Java Properties";
	public static final String MIF = "MIF (Maker Interchange Format)";
	public static final String OFF = "Microsoft Office 2007 Document";
	public static final String OO = "OpenOffice Document";
	public static final String TEXT = "Plain Text";
	public static final String PO = "PO (Portable Objects)";
	public static final String RC = "RC (Windows C/C++ Resources)";
	public static final String RESX = "ResX (Windows .NET Resources)";
	public static final String SDLXLIFF = "SDLXLIFF Document";
	public static final String TS = "TS (Qt Linguist translation source)";
	public static final String TXML = "TXML Document";
	public static final String XML = "XML Document";
	public static final String XMLG = "XML (Generic)";

	protected static final String[] formats = { INX, IDML, DITA, HTML, JS, JAVA, MIF, OFF, OO, TEXT, PO, RC, RESX,
			SDLXLIFF, TS, TXML, XML, XMLG };

	public static String detectFormat(String fileName) {

		File file = new File(fileName);
		if (!file.exists()) {
			return null;
		}
		try {
			FileInputStream input = new FileInputStream(file);
			byte[] array = new byte[40960];
			if (input.read(array) == -1) {
				input.close();
				throw new IOException("Premature end of file");
			}
			input.close();
			String string = "";

			Charset bom = EncodingResolver.getBOM(fileName);

			if (bom != null) {
				byte[] efbbbf = { -17, -69, -65 }; // UTF-8
				String utf8 = new String(efbbbf);
				string = new String(array, bom);
				if (string.startsWith("\uFFFE")) {
					string = string.substring("\uFFFE".length());
				} else if (string.startsWith("\uFEFF")) {
					string = string.substring("\uFEFF".length());
				} else if (string.startsWith(utf8)) {
					string = string.substring(utf8.length());
				}
			} else {
				string = new String(array);
			}

			if (string.startsWith("<MIFFile")) {
				return MIF;
			}

			if (string.startsWith("<?xml")) {
				if (string.indexOf("<xliff") != -1 && string.indexOf("xmlns:sdl") != -1) {
					return SDLXLIFF;
				}
				if (string.indexOf("<txml ") != -1) {
					return TXML;
				}
				if (string.indexOf("<docu") != -1 && string.indexOf("<?aid ") != -1) {
					return INX;
				}
				if (string.indexOf("xmlns:msdata") != -1 && string.indexOf("<root") != -1) {
					return RESX;
				}
				if (string.indexOf("!DOCTYPE TS>") != -1) {
					return TS;
				}
				if (string.indexOf("<map") != -1 || string.indexOf("<bookmap") != -1) {
					return DITA;
				}
				return XML;
			}
			if (string.startsWith("<svg")) {
				return XML;
			}
			if (string.indexOf("<!DOCTYPE TS>") != -1) {
				return TS;
			}

			if (string.startsWith("<!DOCTYPE")) {
				int index = string.indexOf("-//IETF//DTD HTML");
				if (index != -1) {
					return HTML;
				}
				index = string.indexOf("-//W3C//DTD HTML");
				if (index != -1) {
					return HTML;
				}
				index = string.indexOf(" html");
				if (index != -1) {
					return HTML;
				}
				return XML;
			}
			if (string.indexOf("msgid") != -1 && string.indexOf("msgstr") != -1) {
				return PO;
			}
			int index = string.toLowerCase().indexOf("<html");
			if (index != -1) {
				return HTML;
			}

			if (string.startsWith("PK")) {
				// might be a zipped file
				boolean openOffice = false;
				boolean hasXML = false;
				boolean idml = false;
				try (ZipInputStream in = new ZipInputStream(new FileInputStream(file))) {
					ZipEntry entry = null;
					while ((entry = in.getNextEntry()) != null) {
						if (entry.getName().equals("content.xml")) {
							openOffice = true;
							break;
						}
						if (entry.getName().equals("designmap.xml")) {
							idml = true;
							break;
						}
						if (entry.getName().matches(".*\\.xml")) {
							hasXML = true;
						}
					}
				}
				if (idml) {
					return IDML;
				}
				if (openOffice) {
					return OO;
				}
				if (hasXML) {
					return OFF;
				}
			}
			if (string.indexOf("#include") != -1 || string.indexOf("#define") != -1 || string.indexOf("DIALOG") != -1
					|| string.indexOf("DIALOGEX") != -1 || string.indexOf("MENU") != -1
					|| string.indexOf("MENUEX") != -1 || string.indexOf("POPUP") != -1
					|| string.indexOf("STRINGTABLE") != -1 || string.indexOf("AUTO3STATE") != -1
					|| string.indexOf("AUTOCHECKBOX") != -1 || string.indexOf("AUTORADIOBUTTON") != -1
					|| string.indexOf("CHECKBOX") != -1 || string.indexOf("COMBOBOX") != -1
					|| string.indexOf("CONTROL") != -1 || string.indexOf("CTEXT") != -1
					|| string.indexOf("DEFPUSHBUTTON") != -1 || string.indexOf("GROUPBOX") != -1
					|| string.indexOf("ICON") != -1 || string.indexOf("LISTBOX") != -1 || string.indexOf("LTEXT") != -1
					|| string.indexOf("PUSHBOX") != -1 || string.indexOf("PUSHBUTTON") != -1
					|| string.indexOf("RADIOBUTTON") != -1 || string.indexOf("RTEXT") != -1
					|| string.indexOf("SCROLLBAR") != -1 || string.indexOf("STATE3") != -1) {
				return RC;
			}

		} catch (Exception e) {
			// do nothing
		}
		if (fileName.endsWith(".properties")) {
			return JAVA;
		}
		if (fileName.toLowerCase().endsWith(".rc")) {
			return RC;
		}
		return null;
	}

	public static String getShortName(String type) {
		if (type == null) {
			return null;
		}
		if (type.equals(INX)) {
			return "INX";
		}
		if (type.equals(IDML)) {
			return "IDML";
		}
		if (type.equals(DITA)) {
			return "DITA";
		}
		if (type.equals(HTML)) {
			return "HTML";
		}
		if (type.equals(JS)) {
			return "JS";
		}
		if (type.equals(JAVA)) {
			return "JAVA";
		}
		if (type.equals(MIF)) {
			return "MIF";
		}
		if (type.equals(OFF)) {
			return "OFF";
		}
		if (type.equals(OO)) {
			return "OO";
		}
		if (type.equals(TEXT)) {
			return "TEXT";
		}
		if (type.equals(PO)) {
			return "PO";
		}
		if (type.equals(RC)) {
			return "RC";
		}
		if (type.equals(RESX)) {
			return "RESX";
		}
		if (type.equals(SDLXLIFF)) {
			return "SDLXLIFF";
		}
		if (type.equals(TS)) {
			return "TS";
		}
		if (type.equals(TXML)) {
			return "TXML";
		}
		if (type.equals(XML)) {
			return "XML";
		}
		if (type.equals(XMLG)) {
			return "XMLG";
		}
		return null;
	}

	public static String getFullName(String type) {
		if (type.equals("INX")) {
			return INX;
		}
		if (type.equals("IDML")) {
			return IDML;
		}
		if (type.equals("DITA")) {
			return DITA;
		}
		if (type.equals("HTML")) {
			return HTML;
		}
		if (type.equals("JS")) {
			return JS;
		}
		if (type.equals("JAVA")) {
			return JAVA;
		}
		if (type.equals("MIF")) {
			return MIF;
		}
		if (type.equals("OFF")) {
			return OFF;
		}
		if (type.equals("OO")) {
			return OO;
		}
		if (type.equals("TEXT")) {
			return TEXT;
		}
		if (type.equals("PO")) {
			return PO;
		}
		if (type.equals("RC")) {
			return RC;
		}
		if (type.equals("RESX")) {
			return RESX;
		}
		if (type.equals("SDLXLIFF")) {
			return SDLXLIFF;
		}
		if (type.equals("TS")) {
			return TS;
		}
		if (type.equals("TXML")) {
			return TXML;
		}
		if (type.equals("XML")) {
			return XML;
		}
		if (type.equals("XMLG")) {
			return XMLG;
		}
		return null;
	}

	public static String[] getFormats() {
		return formats;
	}

}
