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
package com.maxprograms.stats;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;
import com.maxprograms.xml.XMLNode;

public class RepetitionAnalysis {

	private static String srcLang;

	private static FileOutputStream output;
	private static String curreFile;
	private static Hashtable<String, Vector<Element>> segments;
	private static Vector<Element> sources;
	private static Vector<String> files;

	private static void createList(Element root) {
		List<Element> elements = root.getChildren();
		Iterator<Element> it = elements.iterator();
		while (it.hasNext()) {
			Element el = it.next();
			if (el.getName().equals("file")) {
				curreFile = el.getAttributeValue("original");
				srcLang = el.getAttributeValue("source-language", "");

				if (!segments.containsKey(curreFile)) {
					sources = new Vector<>();
					segments.put(curreFile, sources);
					files.add(curreFile);
				} else {
					sources = segments.get(curreFile);
				}
			}
			if (el.getName().equals("trans-unit")) {
				String approved = el.getAttributeValue("approved", "no");
				Element src = removeTags(el.getChild("source"));
				Element target = el.getChild("target");
				String translated = "no";
				if (target != null && !target.getText().equals("")) {
					translated = "yes";
				}
				List<Element> trans_units = el.getChildren("alt-trans");
				String type = "";
				int[] count = getCount(el);
				if (trans_units.size() > 0) {
					type = getMatch(el);
				} else {
					if (approved.equalsIgnoreCase("yes") && target != null
							&& target.getAttributeValue("state-qualifier", "").equals("leveraged-inherited")) {
						type = "ice";
					} else {
						type = "new";
					}
				}
				if (el.getAttributeValue("translate", "yes").equals("yes")) {
					src.setAttribute("words", "" + count[0]);
					src.setAttribute("untranslatable", "" + count[1]);
				} else {
					src.setAttribute("words", "0");
					src.setAttribute("untranslatable", "" + (count[0] + count[1]));
				}
				src.setAttribute("type", type);
				src.setAttribute("approved", approved);
				src.setAttribute("translated", translated);
				sources.add(src);
			} else {
				createList(el);
			}
		}
	}

	public static void analyse(String projectFileName, String catalog)
			throws SAXException, IOException, ParserConfigurationException {

		SAXBuilder builder = new SAXBuilder();
		builder.setEntityResolver(new Catalog(catalog));
		Iterator<Element> it = null;
		String shortName = new File(projectFileName).getName();
		Document doc = builder.build(projectFileName);
		Element root = doc.getRootElement();
		segments = new Hashtable<>();
		files = new Vector<>();

		createList(root);

		TreeSet<String> set = new TreeSet<>(new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				return o1.compareToIgnoreCase(o2);
			}
		});
		for (int i = 0; i < files.size(); i++) {
			set.add(files.get(i));
		}
		files = new Vector<>();
		Iterator<String> its = set.iterator();
		while (its.hasNext()) {
			files.add(its.next());
		}

		String title = "Translation Status Analysis: {0}";

		// List of files ready, process individually
		MessageFormat mf = new MessageFormat("File: {0}");

		//
		// all segments are in, now check repeated
		//
		mf = new MessageFormat("Step 1 - {0}");
		for (int i = 0; i < files.size(); i++) {
			List<Element> currFile = segments.get(files.get(i));
			for (int j = 0; j < currFile.size(); j++) {
				Element src = currFile.get(j);
				// check segments without matches from TM only
				if (src.getAttributeValue("type").equals("new")) {
					String currText = src.toString();
					// check in current file
					// start with next segment
					for (int k = j + 1; k < currFile.size(); k++) {
						Element other = currFile.get(k);
						if (currText.equals(other.toString())) {
							other.setAttribute("type", "rep-int");
						}
					}
					// check in other files
					// start with following one
					for (int k = i + 1; k < files.size(); k++) {
						List<Element> otherFile = segments.get(files.get(k));
						for (int m = 0; m < otherFile.size(); m++) {
							Element other = otherFile.get(m);
							if (currText.equals(other.toString())) {
								other.setAttribute("type", "rep-ext");
							}
						}
					}
				}
			}
		}

		//
		// publish results
		//
		int newSegs_t = 0;
		int iceSegs_t = 0;
		int untrSegs_t = 0;
		int matches_t = 0;
		int repeated_t = 0;
		int repInt_t = 0;
		int repExt_t = 0;
		int matches_95_t = 0;
		int matches_85_t = 0;
		int matches_75_t = 0;
		int matches_50_t = 0;

		output = new FileOutputStream(projectFileName + ".log.html");
		writeString("<html>\n");
		writeString("<head>\n");
		writeString("  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n");
		writeString("  <title>" + "Repetition Analysis" + "</title>\n");
		writeString("  <style type=\"text/css\">\n");
		writeString("   table.wordCount{\n");
		writeString("       border-left:1px solid grey;\n");
		writeString("   }\n");
		writeString("   .wordCount th{\n");
		writeString("       border-left:1px solid grey;\n");
		writeString("       border-right:1px solid grey;\n");
		writeString("       background:#003854;\n");
		writeString("       color:white;\n");
		writeString("       text-align:center;\n");
		writeString("       padding:3px\n");
		writeString("   }\n");
		writeString("   .wordCount td.left{\n");
		writeString("       border-right:1px solid grey;\n");
		writeString("       border-bottom:1px solid grey;\n");
		writeString("       text-align:left;\n");
		writeString("       padding:2px;\n");
		writeString("   }\n");
		writeString("   .wordCount td.center{\n");
		writeString("       border-right:1px solid grey;\n");
		writeString("       border-bottom:1px solid grey;\n");
		writeString("       text-align:center;\n");
		writeString("       padding:2px;\n");
		writeString("   }\n");
		writeString("   .wordCount td.right{\n");
		writeString("       border-right:1px solid grey;\n");
		writeString("       border-bottom:1px solid grey;\n");
		writeString("       text-align:right;\n");
		writeString("       padding:2px;\n");
		writeString("   }\n");
		writeString("  </style>\n");
		writeString("</head>\n");
		writeString("<body>\n");

		//
		// Segment based analysis
		//
		mf = new MessageFormat(title);
		Object[] args = { shortName };
		writeString("<h2  style=\"font-family: Arial,Helvetica,sans-serif;\">" + mf.format(args) + "</h2>\n");
		writeString("<h2  style=\"font-family: Arial,Helvetica,sans-serif;\">" + "Segments Based Analysis" + "</h2>\n");
		writeString("<table class=\"wordCount\" width=\"100%\">\n");

		writeString(
				"<tr><th>#</th><th>" + "Document" + "</th><th>" + "New" + "</th><th>" + "ICE" + "</th><th>" + "Matches"
						+ "</th><th>" + "Int.Rep." + "</th><th>" + "Ext.Rep." + "</th><th>" + "SUM" + "</th></tr>\n");
		mf = new MessageFormat("Step 2 - {0}");
		for (int i = 0; i < files.size(); i++) {
			List<Element> content = segments.get(files.get(i));
			it = content.iterator();
			int newSegs = 0;
			int iceSegs = 0;
			int matches = 0;
			int repInt = 0;
			int repExt = 0;
			while (it.hasNext()) {
				Element e = it.next();
				String type = e.getAttributeValue("type", "new");
				if (type.equals("new")) {
					newSegs++;
				}
				if (type.equals("ice")) {
					iceSegs++;
				}
				if (type.equals("exact") || type.equals("95") || type.equals("85") || type.equals("75")
						|| type.equals("50")) {
					matches++;
				}
				if (type.equals("rep-int")) {
					repInt++;
				}
				if (type.equals("rep-ext")) {
					repExt++;
				}
			}
			writeString("<tr>");
			writeString("<td class=\"center\">" + (i + 1) + "</td>" + "<td class=\"left\">" + files.get(i)
					+ "</td><td class=\"right\">" + newSegs + "</td><td class=\"right\">" + iceSegs
					+ "</td><td class=\"right\">" + matches + "</td><td class=\"right\">" + repInt
					+ "</td><td class=\"right\">" + repExt + "</td><td class=\"right\">"
					+ (newSegs + iceSegs + matches + repInt + repExt) + "</td>");
			writeString("</tr>\n");
			newSegs_t += newSegs;
			iceSegs_t += iceSegs;
			matches_t += matches;
			repInt_t += repInt;
			repExt_t += repExt;
		}

		writeString("<tr>");
		writeString(
				"<td bgcolor=\"#ededed\" style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\">&nbsp;</td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"center\" bgcolor=\"#ededed\"><b>"
						+ "SUM"
						+ "</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\" bgcolor=\"#ededed\"><b>"
						+ newSegs_t
						+ "</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\" bgcolor=\"#ededed\"><b>"
						+ iceSegs_t
						+ "</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\" bgcolor=\"#ededed\"><b>"
						+ matches_t
						+ "</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\" bgcolor=\"#ededed\"><b>"
						+ repInt_t
						+ "</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\" bgcolor=\"#ededed\"><b>"
						+ repExt_t
						+ "</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\" bgcolor=\"#ededed\"><b>"
						+ (newSegs_t + matches_t + repInt_t + repExt_t) + "</b></td>");
		writeString("</tr>\n");
		writeString("</table>\n");

		//
		// Words based analysis
		//
		iceSegs_t = 0;
		matches_t = 0;
		repeated_t = 0;
		newSegs_t = 0;
		writeString("<h2  style=\"font-family: Arial,Helvetica,sans-serif;\">" + "Words Based Analysis" + "</h2>\n");
		writeString("<table class=\"wordCount\" width=\"100%\">\n");
		writeString("<tr><th>#</th><th>" + "Document" + "</th><th>" + "New" + "</th><th>" + "ICE" + "</th><th>"
				+ "Not Translatable" + "</th><th>" + "100%" + "</th><th>" + "Repeated" + "</th><th>" + "95-99%"
				+ "</th><th>" + "85-94%" + "</th><th>" + "75-84%" + "</th><th>" + "50-74%" + "</th><th>" + "SUM"
				+ "</th></tr>\n");
		mf = new MessageFormat("Step 3 - {0}");
		for (int i = 0; i < files.size(); i++) {
			List<Element> content = segments.get(files.get(i));
			it = content.iterator();
			int newSegs = 0;
			int iceSegs = 0;
			int untrSegs = 0;
			int matches = 0;
			int repeated = 0;
			int matches_95 = 0;
			int matches_85 = 0;
			int matches_75 = 0;
			int matches_50 = 0;

			while (it.hasNext()) {
				Element e = it.next();
				String type = e.getAttributeValue("type", "new");
				if (type.equals("new")) {
					newSegs += Integer.valueOf(e.getAttributeValue("words")).intValue();
				}
				if (type.equals("ice")) {
					iceSegs += Integer.valueOf(e.getAttributeValue("words")).intValue();
				}
				if (type.equals("exact")) {
					matches += Integer.valueOf(e.getAttributeValue("words")).intValue();
				}
				if (type.equals("95")) {
					matches_95 += Integer.valueOf(e.getAttributeValue("words")).intValue();
				}
				if (type.equals("85")) {
					matches_85 += Integer.valueOf(e.getAttributeValue("words")).intValue();
				}
				if (type.equals("75")) {
					matches_75 += Integer.valueOf(e.getAttributeValue("words")).intValue();
				}
				if (type.equals("50")) {
					matches_50 += Integer.valueOf(e.getAttributeValue("words")).intValue();
				}
				if (type.startsWith("rep")) {
					repeated += Integer.valueOf(e.getAttributeValue("words")).intValue();
				}
				untrSegs += Integer.valueOf(e.getAttributeValue("untranslatable")).intValue();
			}
			writeString("<tr>");
			writeString("<td class=\"center\">" + (i + 1) + "</td>" + "<td class=\"left\">" + files.get(i)
					+ "</td><td class=\"right\">" + newSegs + "</td><td class=\"right\">" + iceSegs
					+ "</td><td class=\"right\">" + untrSegs + "</td><td class=\"right\">" + matches
					+ "</td><td class=\"right\">" + repeated + "</td><td class=\"right\">" + matches_95
					+ "</td><td class=\"right\">" + matches_85 + "</td><td class=\"right\">" + matches_75
					+ "</td><td class=\"right\">" + matches_50 + "</td><td class=\"right\">" + (newSegs + iceSegs
							+ matches + untrSegs + repeated + matches_95 + matches_85 + matches_75 + matches_50)
					+ "</td>");
			writeString("</tr>\n");
			newSegs_t += newSegs;
			iceSegs_t += iceSegs;
			untrSegs_t += untrSegs;
			matches_t += matches;
			repeated_t += repeated;
			matches_95_t += matches_95;
			matches_85_t += matches_85;
			matches_75_t += matches_75;
			matches_50_t += matches_50;

			newSegs = 0;
			iceSegs = 0;
			matches = 0;
			matches_95 = 0;
			matches_85 = 0;
			matches_75 = 0;
			matches_50 = 0;
			repeated = 0;
		}

		writeString("<tr>");
		writeString(
				"<td bgcolor=\"#ededed\" style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\">&nbsp;</td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"center\" bgcolor=\"#ededed\"><b>"
						+ "SUM");
		writeString(
				"</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\" bgcolor=\"#ededed\"><b>"
						+ newSegs_t
						+ "</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\" bgcolor=\"#ededed\"><b>"
						+ iceSegs_t
						+ "</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\" bgcolor=\"#ededed\"><b>"
						+ untrSegs_t
						+ "</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\" bgcolor=\"#ededed\"><b>"
						+ matches_t
						+ "</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\" bgcolor=\"#ededed\"><b>"
						+ repeated_t
						+ "</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\" bgcolor=\"#ededed\"><b>"
						+ matches_95_t
						+ "</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\" bgcolor=\"#ededed\"><b>"
						+ matches_85_t
						+ "</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\" bgcolor=\"#ededed\"><b>"
						+ matches_75_t
						+ "</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\" bgcolor=\"#ededed\"><b>"
						+ matches_50_t
						+ "</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\" bgcolor=\"#ededed\"><b>"
						+ (newSegs_t + iceSegs_t + untrSegs_t + matches_t + repeated_t + matches_95_t + matches_85_t
								+ matches_75_t + matches_50_t)
						+ "</b></td>");
		writeString("</tr>\n");
		writeString("</table>\n");

		writeString(
				"<h2  style=\"font-family: Arial,Helvetica,sans-serif;\">" + "Translation Status Analysis" + "</h2>\n");
		//
		// Translation status by segments
		//

		writeString("<h3>" + "Segments" + "</h3>\n");

		writeString("<table class=\"wordCount\" width=\"100%\">\n");
		writeString("<tr><th>#</th><th>" + "Document" + "</th><th>" + "Not Translated" + "</th><th>" + "Translated"
				+ "</th><th>" + "Approved" + "</th><th>" + "Not Approved" + "</th><th>" + "SUM" + "</th></tr>\n");

		int allnumapproved = 0;
		int allnumtranslated = 0;
		int allnotapproved = 0;
		int allnottranslated = 0;

		mf = new MessageFormat("Step 4 - {0}");
		for (int i = 0; i < files.size(); i++) {
			List<Element> content = segments.get(files.get(i));
			it = content.iterator();
			int numapproved = 0;
			int numtranslated = 0;
			int nottranslated = 0;
			int notapproved = 0;

			while (it.hasNext()) {
				Element e = it.next();
				String approved = e.getAttributeValue("approved");
				String translated = e.getAttributeValue("translated");
				if (approved.equals("yes")) {
					numapproved++;
				} else {
					notapproved++;
				}
				if (translated.equals("yes")) {
					numtranslated++;
				} else {
					nottranslated++;
				}
			}
			allnumapproved = allnumapproved + numapproved;
			allnumtranslated = allnumtranslated + numtranslated;
			allnotapproved = allnotapproved + notapproved;
			allnottranslated = allnottranslated + nottranslated;
			writeString("<tr><td class=\"center\">" + (i + 1) + "</td>" + "<td class=\"left\">" + files.get(i)
					+ "</td><td class=\"right\">" + nottranslated + "</td><td class=\"right\">" + numtranslated
					+ "</td><td class=\"right\">" + numapproved + "</td><td class=\"right\">" + notapproved
					+ "</td><td class=\"right\">" + (numapproved + notapproved) + "</td></tr>\n");
		}

		writeString(
				"<tr><td  bgcolor=\"#ededed\" style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\">&nbsp;</td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"center\" bgcolor=\"#ededed\"><b>"
						+ "SUM" + "</b>");
		writeString(
				"</td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\" align=\"right\"><b>"
						+ allnottranslated
						+ "</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\" align=\"right\"><b>"
						+ allnumtranslated
						+ "</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\" align=\"right\"><b>"
						+ allnumapproved
						+ "</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\" align=\"right\"><b>"
						+ allnotapproved
						+ "</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\" align=\"right\"><b>"
						+ (allnumapproved + allnotapproved) + "</b></td></tr>\n");
		writeString("</table>\n");

		//
		// Translation status by words
		//

		writeString("<h3>" + "Words" + "</h3>\n");

		writeString("<table class=\"wordCount\" width=\"100%\">\n");
		writeString("<tr><th>#</th><th>" + "Document" + "</th><th>" + "Not Translated" + "</th><th>" + "Translated"
				+ "</th><th>" + "Not Translatable" + "</th><th>" + "Approved" + "</th><th>" + "Not Approved"
				+ "</th><th>" + "SUM" + "</th></tr>\n");

		allnumapproved = 0;
		allnumtranslated = 0;
		int alluntranslatable = 0;
		allnotapproved = 0;
		allnottranslated = 0;

		mf = new MessageFormat("Step 5 - {0}");
		for (int i = 0; i < files.size(); i++) {
			List<Element> content = segments.get(files.get(i));
			it = content.iterator();
			int numapproved = 0;
			int numtranslated = 0;
			int untranslatable = 0;
			int notapproved = 0;
			int nottranslated = 0;

			while (it.hasNext()) {
				Element e = it.next();
				String approved = e.getAttributeValue("approved");
				String translated = e.getAttributeValue("translated");
				int words = Integer.valueOf(e.getAttributeValue("words")).intValue();
				if (approved.equals("yes")) {
					numapproved += words;
				} else {
					notapproved += words;
				}
				if (translated.equals("yes")) {
					numtranslated += words;
				} else {
					nottranslated += words;
				}
				untranslatable += Integer.valueOf(e.getAttributeValue("untranslatable")).intValue();
			}
			allnumapproved = allnumapproved + numapproved;
			allnumtranslated = allnumtranslated + numtranslated;
			allnotapproved = allnotapproved + notapproved;
			allnottranslated = allnottranslated + nottranslated;
			alluntranslatable = alluntranslatable + untranslatable;

			writeString("<tr><td class=\"center\">" + (i + 1) + "</td>" + "<td class=\"left\">" + files.get(i)
					+ "</td><td class=\"right\">" + nottranslated + "</td><td class=\"right\">" + numtranslated
					+ "</td><td class=\"right\">" + untranslatable + "</td><td class=\"right\">" + numapproved
					+ "</td><td class=\"right\">" + notapproved + "</td><td class=\"right\">"
					+ (notapproved + numapproved + untranslatable) + "</td></tr>\n");
		}

		writeString(
				"<tr><td bgcolor=\"#ededed\" style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\">&nbsp;</td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"center\" bgcolor=\"#ededed\"><b>"
						+ "SUM" + "</b>");
		writeString(
				"</td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\" align=\"right\"><b>"
						+ allnottranslated
						+ "</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\" align=\"right\"><b>"
						+ allnumtranslated
						+ "</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\" align=\"right\"><b>"
						+ alluntranslatable
						+ "</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\" align=\"right\"><b>"
						+ allnumapproved
						+ "</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\" align=\"right\"><b>"
						+ allnotapproved
						+ "</b></td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\" align=\"right\"><b>"
						+ (allnumapproved + allnotapproved + alluntranslatable) + "</b></td></tr>\n");
		writeString("</table>\n");

		writeString("<p><b><u>" + "Comments:" + "</u></b><br />");
		writeString("<b>" + "Int.Rep." + "</b> " + "Internal Repetition = Segment repetitions within one document"
				+ "<br />");
		writeString("<b>" + "Ext.Rep." + "</b> " + "External Repetition = Segment repetitions between all documents"
				+ "</p>");
		writeString("</body>\n");
		writeString("</html>\n");

		output.close();
	}

	private static String getMatch(Element e) {
		List<Element> trans_units = e.getChildren("alt-trans");
		int max = 0;
		String type = "";
		Iterator<Element> i = trans_units.iterator();
		while (i.hasNext()) {
			Element trans = i.next();
			String quality = trans.getAttributeValue("match-quality");
			try {
				Integer value = Integer.valueOf(quality);
				if (value.intValue() > max) {
					max = value.intValue();
				}
			} catch (Exception e1) {
				// do nothing here
			}
		}
		if (max == 100) {
			type = "exact";
		} else if (max >= 95 && max < 100) {
			type = "95";
		} else if (max >= 85 && max < 95) {
			type = "85";
		} else if (max >= 75 && max < 85) {
			type = "75";
		} else if (max >= 50 && max < 75) {
			type = "50";
		} else {
			type = "new";
		}
		return type;
	}

	public static String pureText(Element src, String space, String translate) {
		if (src == null) {
			return "";
		}
		String text = "";
		List<XMLNode> l = src.getContent();
		if (l == null) {
			return "";
		}
		Iterator<XMLNode> i = l.iterator();
		while (i.hasNext()) {
			XMLNode o = i.next();
			if (o.getNodeType() == XMLNode.TEXT_NODE
					&& src.getAttributeValue("mtype", "translatable").equals(translate)) {
				if (space.equals("default")) {
					if (text.length() > 0) {
						text = text + " " + o.toString().trim();
					} else {
						text = text + o.toString().trim();
					}
					text = normalise(text, true);
				} else {
					text = text + o.toString();
				}
			} else if (o.getNodeType() == XMLNode.ELEMENT_NODE) {
				Element el = (Element) o;
				String type = el.getName();
				// discard all inline elements
				// except <g>, <mrk>, <hi> and <sub>
				if (!type.equals("bpt") && !type.equals("ept") && !type.equals("it") && !type.equals("ph")
						&& !type.equals("ut") && !type.equals("x")) {
					text = text + " " + pureText(el, space, translate);
				}
			}
		}
		return text;
	}

	private static int[] getCount(Element e) {
		if (e == null) {
			return new int[] { 0, 0 };
		}
		Element source = e.getChild("source");
		int res1 = wordCount(pureText(source, e.getAttributeValue("xml:space", "default"), "translatable"), srcLang);
		int res2 = wordCount(pureText(source, e.getAttributeValue("xml:space", "default"), "protected"), srcLang);
		return new int[] { res1, res2 };
	}

	private static void writeString(String text) throws UnsupportedEncodingException, IOException {
		output.write(text.getBytes("UTF-8"));
	}

	public static int wordCount(String str, String lang) {
		if (lang.toLowerCase().startsWith("zh")) {
			return chineseCount(str);
		}
		return europeanCount(str);
	}

	private static int chineseCount(String str) {
		// basic idea is that we need to remove unicode that higher than 255
		// and then we count by europeanCount
		// after that remove 0-255 unicode value and just count character
		StringBuffer european = new StringBuffer();
		int chineseCount = 0;
		char[] chars = str.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			char chr = chars[i];
			if (chr <= 255 || chr == '\u00A0' || chr == '\u3001' || chr == '\u3002' || chr == '\uff1a'
					|| chr == '\uff01' || chr == '\uff1f' || chr == '\u4ecb') {
				european.append(chr);
			} else {
				chineseCount++;
			}
		}
		int euroCount = europeanCount(european.toString());
		return euroCount + chineseCount;
	}

	private static int europeanCount(String source) {
		int wordnum = 0;
		StringTokenizer tok = new StringTokenizer(source, " \t\r\n()?\u00A0\u3001\u3002\uff1a\uff01\uff1f\u4ecb");
		String charsInNumber = ".,-/<>";
		while (tok.hasMoreTokens()) {
			String str = tok.nextToken();
			if (!containsSeparator(str, charsInNumber)) {
				wordnum++;
			} else {
				if (!isFormatNumber(str)) {
					// concatenated words
					StringTokenizer tok2 = new StringTokenizer(str, charsInNumber);
					while (tok2.hasMoreTokens()) {
						str = tok2.nextToken();
						wordnum++;
					}
				} else {
					// single number
					wordnum++;
				}
			}
		}
		return wordnum;
	}

	private static boolean containsSeparator(String str, String seps) {
		for (int i = 0; i < seps.length(); i++) {
			if (str.indexOf(seps.charAt(i)) != -1) {
				return true;
			}
		}
		return false;
	}

	public static boolean isFormatNumber(String str) {
		char[] chars = str.toCharArray();
		boolean hasDigit = false;
		for (int i = 0; i < chars.length; i++) {
			if (Character.isDigit(chars[i])) {
				hasDigit = true;
			}
		}
		return hasDigit;
	}

	private static Element removeTags(Element src) {
		src.removeChild("ph");
		src.removeChild("bpt");
		src.removeChild("ept");
		return src;
	}

	public static int[] analyseWords(Document doc) {
		Iterator<Element> it = null;
		Element rootClone = new Element();
		rootClone.clone(doc.getRootElement());
		srcLang = rootClone.getChild("file").getAttributeValue("source-language");

		Vector<Element> segs = new Vector<>();
		createList(rootClone, segs);

		rootClone = null;
		it = segs.iterator();
		Vector<Element> srcs = new Vector<>();
		while (it.hasNext()) {
			Element e = it.next();
			String approved = e.getAttributeValue("approved", "no");
			Element src = e.getChild("source");
			src = removeTags(src);

			Element target = e.getChild("target");
			String translated = "no";
			if (target != null && !target.getText().equals("")) {
				translated = "yes";
			}

			List<Element> trans_units = e.getChildren("alt-trans");
			String type = "";
			int[] count = getCount(e);
			if (trans_units.size() > 0) {
				type = getMatch(e);
			} else {
				if (approved.equalsIgnoreCase("yes") && target != null
						&& target.getAttributeValue("state-qualifier", "").equals("leveraged-inherited")) {
					type = "ice";
				} else {
					type = "new";
				}
			}
			e = null;
			src.setAttribute("words", "" + count[0]);
			src.setAttribute("untranslatable", "" + count[1]);
			src.setAttribute("type", type);
			src.setAttribute("approved", approved);
			src.setAttribute("translated", translated);
			srcs.add(src);
		}
		//
		// all segments are in, now check repeated
		//

		for (int j = 0; j < srcs.size(); j++) {
			Element src = srcs.get(j);
			// check segments without matches from TM only
			if (src.getAttributeValue("type").equals("new")) {
				String currText = src.toString();
				// check in current file
				// start with next segment
				for (int k = j + 1; k < srcs.size(); k++) {
					Element other = srcs.get(k);
					if (currText.equals(other.toString())) {
						other.setAttribute("type", "rep-int");
					}
				}
			}
		}

		//
		// Words based analysis
		//

		it = srcs.iterator();
		int iceSegs = 0;
		int newSegs = 0;
		int matches = 0;
		int repeated = 0;
		int matches_95 = 0;
		int matches_85 = 0;
		int matches_75 = 0;
		int matches_50 = 0;

		while (it.hasNext()) {
			Element e = it.next();
			String type = e.getAttributeValue("type", "new");
			if (type.equals("ice")) {
				iceSegs += Integer.valueOf(e.getAttributeValue("words")).intValue();
			}
			if (type.equals("new")) {
				newSegs += Integer.valueOf(e.getAttributeValue("words")).intValue();
			}
			if (type.equals("exact")) {
				matches += Integer.valueOf(e.getAttributeValue("words")).intValue();
			}
			if (type.equals("95")) {
				matches_95 += Integer.valueOf(e.getAttributeValue("words")).intValue();
			}
			if (type.equals("85")) {
				matches_85 += Integer.valueOf(e.getAttributeValue("words")).intValue();
			}
			if (type.equals("75")) {
				matches_75 += Integer.valueOf(e.getAttributeValue("words")).intValue();
			}
			if (type.equals("50")) {
				matches_50 += Integer.valueOf(e.getAttributeValue("words")).intValue();
			}
			if (type.startsWith("rep")) {
				repeated += Integer.valueOf(e.getAttributeValue("words")).intValue();
			}
		}

		int numapproved = 0;
		int numtranslated = 0;
		int allwords = 0;

		it = srcs.iterator();
		while (it.hasNext()) {
			Element e = it.next();
			String approved = e.getAttributeValue("approved");
			String translated = e.getAttributeValue("translated");
			if (approved.equals("yes")) {
				numapproved += Integer.valueOf(e.getAttributeValue("words")).intValue();
			}
			if (translated.equals("yes")) {
				numtranslated += Integer.valueOf(e.getAttributeValue("words")).intValue();
			}
			allwords += Integer.valueOf(e.getAttributeValue("words")).intValue();
		}

		int[] result = new int[11];

		result[0] = newSegs;
		result[1] = matches;
		result[2] = repeated;
		result[3] = matches_95;
		result[4] = matches_85;
		result[5] = matches_75;
		result[6] = matches_50;
		result[7] = allwords;
		result[8] = numapproved;
		result[9] = numtranslated;
		result[10] = iceSegs;

		return result;
	}

	private static void createList(Element root, Vector<Element> segs) {
		List<Element> elements = root.getChildren();
		Iterator<Element> it = elements.iterator();
		while (it.hasNext()) {
			Element el = it.next();
			if (el.getName().equals("trans-unit")) {
				if (el.getAttributeValue("translate", "yes").equalsIgnoreCase("yes")) {
					segs.add(el);
				}
			} else {
				createList(el, segs);
			}
		}

	}

	public static void generateStatusHistoryView(Status[] status, String filename) throws IOException {
		output = new FileOutputStream(filename + ".status.html");
		writeString("<html>\n");
		writeString("<head>\n");
		writeString("<title>" + "Translation Status History" + "</title>\n");
		writeString("</head>\n");
		writeString("<body>\n");
		writeString(
				"<h2  style=\"font-family: Arial,Helvetica,sans-serif;\">" + "Translation Status History" + "</h2>\n");
		MessageFormat mf = new MessageFormat("File: {0}");
		Object[] args = { filename };
		writeString("<h2  style=\"font-family: Arial,Helvetica,sans-serif;\">" + mf.format(args) + "</h2>\n");
		writeString("<table width=\"100%\">\n");
		writeString(
				"<tr><th style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\">"
						+ "Description"
						+ "</th><th style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\">"
						+ "Date"
						+ "</th><th style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\">"
						+ "New"
						+ "</th><th style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\">"
						+ "ICE"
						+ "</th><th style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\">"
						+ "100%"
						+ "</th><th style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\">"
						+ "Repeated"
						+ "</th><th style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\">"
						+ "95-99%"
						+ "</th><th style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\">"
						+ "85-94%"
						+ "</th><th style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\">"
						+ "75-84%"
						+ "</th><th style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\">"
						+ "50-74%"
						+ "</th><th style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\">"
						+ "SUM"
						+ "</th><th style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\">"
						+ "Approved"
						+ "</th><th style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\">"
						+ "Not Approved"
						+ "</th><th style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\">"
						+ "Translated"
						+ "</th><th style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\">"
						+ "Not Translated"
						+ "</th><th style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" bgcolor=\"#ededed\">"
						+ "SUM" + "</th></tr>\n");

		for (int i = 0; i < status.length; i++) {
			Status stat = status[i];
			writeString("<tr>\n");
			writeString(
					"<td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"center\">"
							+ stat.getDescription()
							+ "</td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"center\">"
							+ stat.getDate()
							+ "</td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\">"
							+ stat.getNewWords()
							+ "</td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\">"
							+ stat.getIceWords()
							+ "</td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\">"
							+ stat.getRange0Count()
							+ "</td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\">"
							+ stat.getRepeated()
							+ "</td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\">"
							+ stat.getRange1Count()
							+ "</td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\">"
							+ stat.getRange2Count()
							+ "</td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\">"
							+ stat.getRange3Count()
							+ "</td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\">"
							+ stat.getRange4Count()
							+ "</td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\">"
							+ stat.getTotalWords()
							+ "</td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\">"
							+ stat.getApproved()
							+ "</td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\">"
							+ (stat.getTotalWords() - stat.getApproved())
							+ "</td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\">"
							+ stat.getTranslated()
							+ "</td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\">"
							+ (stat.getTotalWords() - stat.getTranslated())
							+ "</td><td style=\"border-right:1px #adbfbe solid;border-bottom:1px #adbfbe solid;\" align=\"right\">"
							+ stat.getTotalWords() + "</td>");
			writeString("</tr>\n");
		}

		writeString("</table>\n");

		writeString("</body>\n");
		writeString("</html>\n");

		output.close();

	}

	public static int getCount(Element e, String language) {
		srcLang = language;
		int[] count = getCount(e);
		return count[0] + count[1];
	}

	public static String normalise(String string, boolean trim) {
		boolean repeat = false;
		String rs = "";
		int length = string.length();
		for (int i = 0; i < length; i++) {
			char ch = string.charAt(i);
			if (!Character.isSpaceChar(ch)) {
				if (ch != '\n') {
					rs = rs + ch;
				} else {
					rs = rs + " ";
					repeat = true;
				}
			} else {
				rs = rs + " ";
				while (i < length - 1 && Character.isSpaceChar(string.charAt(i + 1))) {
					i++;
				}
			}
		}
		if (repeat == true) {
			return normalise(rs, trim);
		}
		if (trim) {
			return rs.trim();
		}
		return rs;
	}
}