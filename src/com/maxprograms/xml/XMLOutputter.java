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
package com.maxprograms.xml;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

public class XMLOutputter {

	private FileOutputStream output = null;
	private Charset defaultEncoding = StandardCharsets.UTF_8;
	private boolean preserve = false;
	private Hashtable<String, String> entities = null;
	private boolean escape = false;
	private boolean emptyDoctype = false;
	private boolean skipLinefeed;
	private boolean writeBOM;

	private static final Logger LOGGER = System.getLogger(XMLOutputter.class.getName());

	public void output(Document sdoc, FileOutputStream outputFile) throws IOException {
		output = outputFile;
		if (defaultEncoding.name().equalsIgnoreCase("UTF-16LE")) {
			byte[] feff = { -1, -2 };
			output.write(feff);
		}
		if (defaultEncoding.name().equalsIgnoreCase("UTF-16BE")) {
			byte[] fffe = { -2, -1 };
			output.write(fffe);
		}
		if (writeBOM) {
			byte[] efbbbf = { -17, -69, -65 }; // UTF-8
			output.write(efbbbf);
		}
		if (!skipLinefeed) {
			writeString("<?xml version=\"1.0\" encoding=\"" + defaultEncoding + "\" ?>\n");
		} else {
			writeString("<?xml version=\"1.0\" encoding=\"" + defaultEncoding + "\"?>");
		}
		String doctype = sdoc.getRootElement().getName();
		String publicId = sdoc.getPublicId();
		String systemId = sdoc.getSystemId();
		String internalSubset = sdoc.getInternalSubset();
		Vector<String> customAttributes = sdoc.getAttributes();
		if (customAttributes != null) {
			if (internalSubset == null) {
				internalSubset = "";
			}
			for (int i = 0; i < customAttributes.size(); i++) {
				internalSubset = internalSubset + "\n" + customAttributes.get(i);
			}
		}
		if (publicId != null || systemId != null || internalSubset != null) {
			writeString("<!DOCTYPE " + doctype + " ");
			if (publicId != null) {
				writeString("PUBLIC \"" + publicId + "\" \"" + systemId + "\"");
				if (internalSubset != null && !internalSubset.equals("")) {
					writeString(" [" + internalSubset + "]>\n");
				} else {
					writeString(">\n");
				}
			} else {
				if (systemId != null) {
					writeString("SYSTEM \"" + systemId + "\" ");
				}
				if (internalSubset != null) {
					writeString("[\n" + internalSubset + "]");
				}
				writeString(">\n");
			}
		} else {
			if (emptyDoctype) {
				writeString("<!DOCTYPE " + doctype + ">");
			}
		}
		entities = sdoc.getEntities();
		if (entities == null) {
			entities = new Hashtable<>();
			entities.put("lt", "&#38;#60;");
			entities.put("gt", "&#62;");
			entities.put("amp", "&#38;#38;");
		}
		processHeader(sdoc.getContent());
	}

	private void processHeader(List<XMLNode> list) throws IOException {
		int length = list.size();
		for (int i = 0; i < length; i++) {
			XMLNode n = list.get(i);
			switch (n.getNodeType()) {
			case XMLNode.PROCESSING_INSTRUCTION_NODE:
				PI pi = (PI) n;
				writeString("<?" + pi.getTarget() + " " + pi.getData() + "?>");
				if (!preserve) {
					writeString("\n");
				}
				break;
			case XMLNode.DOCUMENT_NODE:
				// DOCTYPE already written
				break;
			case XMLNode.ELEMENT_NODE:
				traverse((Element) n);
				break;
			case XMLNode.COMMENT_NODE:
				Comment c = (Comment) n;
				if (!preserve) {
					writeString("\n<!-- " + c.getText() + " -->");
				} else {
					writeString("<!-- " + c.getText() + " -->");
				}
				break;
			case XMLNode.CDATA_SECTION_NODE:
				CData cd = (CData) n;
				writeString("<![CDATA[" + cd.getData() + "]]>");
				break;
			default:
				// should never happen
				LOGGER.log(Level.WARNING, "Header contains wrong content type.");
			}
		}
	}

	private void traverse(Element el) throws IOException {

		String type = el.getName();
		String space = el.getAttributeValue("xml:space", "default");
		if (space.equals("preserve") && !preserve) {
			preserve = true;
		}
		writeString("<" + type);
		List<Attribute> attrs = el.getAttributes();
		for (int i = 0; i < attrs.size(); i++) {
			Attribute a = attrs.get(i);
			writeString(" " + a.toString());
		}
		List<XMLNode> list = el.getContent();
		if (!list.isEmpty()) {
			writeString(">");
			for (int i = 0; i < list.size(); i++) {
				XMLNode n = list.get(i);
				switch (n.getNodeType()) {
				case XMLNode.ELEMENT_NODE:
					traverse((Element) n);
					break;
				case XMLNode.TEXT_NODE:
					TextNode tn = (TextNode) n;
					String text = cleanString(tn.getText());
					if (text == null) {
						text = "";
					}
					if (escape) {
						text = text.replaceAll("\"", "&quot;");
						text = text.replaceAll("'", "&apos;");
					}
					if (preserve) {
						writeString(text);
					} else {
						writeString(normalize(text));
					}
					break;
				case XMLNode.PROCESSING_INSTRUCTION_NODE:
					PI pi = (PI) n;
					writeString("<?" + pi.getTarget() + " " + pi.getData() + "?>");
					break;
				case XMLNode.COMMENT_NODE:
					Comment c = (Comment) n;
					if (!preserve) {
						writeString("\n<!-- " + c.getText() + " -->");
					} else {
						writeString("<!-- " + c.getText() + " -->");
					}
					break;
				case XMLNode.CDATA_SECTION_NODE:
					writeString(n.toString());
					break;
				default:
					// should never happen
					LOGGER.log(Level.WARNING, "Unknown node type.");
				}
			}
			if (!preserve) {
				writeString("</" + type + ">\n");
			} else {
				writeString("</" + type + ">");
			}
		} else {
			if (skipLinefeed) {
				writeString(" />");
			} else {
				writeString("/>");
			}
		}
	}

	private String cleanString(String input) {
		if (input == null) {
			return null;
		}
		input = input.replaceAll("&", "&amp;");
		input = input.replaceAll("<", "&lt;");
		input = input.replaceAll(">", "&gt;");

		// now replace common text with
		// the entities declared in the DTD

		Enumeration<String> enu = entities.keys();
		while (enu.hasMoreElements()) {
			String key = enu.nextElement();
			String value = entities.get(key);
			if (!value.equals("") && !key.equals("amp") && !key.equals("lt") && !key.equals("gt")
					&& !key.equals("quot")) {
				input = replaceEntities(input, value, "&" + key + ";");
			}
		}

		// now check for valid characters

		return XMLUtils.validChars(input);

	}

	private static String replaceEntities(String string, String token, String entity) {
		int index = string.indexOf(token);
		while (index != -1) {
			String before = string.substring(0, index);
			String after = string.substring(index + token.length());
			// check if we are not inside an entity
			int amp = before.lastIndexOf('&');
			if (amp == -1) {
				// we are not in an entity
				string = before + entity + after;
			} else {
				boolean inEntity = true;
				for (int i = amp; i < before.length(); i++) {
					char c = before.charAt(i);
					if (Character.isWhitespace(c) || ";.@$*()[]{},/?\\\"\'+=-^".indexOf(c) != -1) {
						inEntity = false;
						break;
					}
				}
				if (inEntity) {
					// check for a colon in "after"
					int colon = after.indexOf(';');
					if (colon == -1) {
						// we are not in an entity
						string = before + entity + after;
					} else {
						// verify is there is something that breaks the entity before
						for (int i = 0; i < colon; i++) {
							char c = after.charAt(i);
							if (Character.isWhitespace(c) || "&.@$*()[]{},/?\\\"\'+=-^".indexOf(c) != -1) {
								break;
							}
						}
					}
				} else {
					// we are not in an entity
					string = before + entity + after;
				}
			}
			if (index < string.length()) {
				index = string.indexOf(token, index + 1);
			}
		}
		return string;
	}

	private void writeString(String input) throws IOException {
		output.write(input.getBytes(defaultEncoding));
	}

	private static String normalize(String string) {
		StringBuilder rs = new StringBuilder("");
		int length = string.length();
		for (int i = 0; i < length; i++) {
			char ch = string.charAt(i);
			if (!Character.isSpaceChar(ch)) {
				if (ch != '\n') {
					rs.append(ch);
				} else {
					rs.append(" ");
				}
			} else {
				rs.append(" ");
				while (i < length - 1 && Character.isSpaceChar(string.charAt(i + 1))) {
					i++;
				}
			}
		}
		return rs.toString();
	}

	public void setEncoding(Charset charset) {
		defaultEncoding = charset;
	}

	public void preserveSpace(boolean value) {
		preserve = value;
	}

	public void escapeQuotes(boolean value) {
		escape = value;
	}

	protected static String replaceToken(String string, String token, String newText) {
		int index = string.indexOf(token);
		while (index != -1) {
			string = string.substring(0, index) + newText + string.substring(index + token.length());
			index = string.indexOf(token, index + newText.length());
		}
		return string;
	}

	public void setEmptyDoctype(boolean b) {
		emptyDoctype = b;
	}

	public void setSkipLinefeed(boolean b) {
		skipLinefeed = b;
	}

	public void writeBOM(boolean b) {
		writeBOM = b;
	}

}
