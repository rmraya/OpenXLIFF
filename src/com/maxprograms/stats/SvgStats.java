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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.Indenter;
import com.maxprograms.xml.SAXBuilder;

public class SvgStats {

	protected class SegmentStatus {

		private boolean approved;
		private boolean translated;
		private float match;

		public SegmentStatus() {
			approved = false;
			translated = false;
			match = 0;
		}

		public boolean isApproved() {
			return approved;
		}

		public void setApproved(boolean approved) {
			this.approved = approved;
		}

		public float getMatch() {
			return match;
		}

		public void setMatch(float match) {
			this.match = match;
		}

		public boolean isTranslated() {
			return translated;
		}

		public void setTranslated(boolean translated) {
			this.translated = translated;
		}

	}

	private List<SegmentStatus> segmentsList;

	public void analyse(String file, String catalog) throws SAXException, IOException, ParserConfigurationException {
		SAXBuilder builder = new SAXBuilder();
		builder.setEntityResolver(new Catalog(catalog));
		Document document = builder.build(file);
		Element root = document.getRootElement();
		if (!"xliff".equals(root.getLocalName())) {
			throw new IOException("Selected file is not an XLIFF document");
		}
		segmentsList = new ArrayList<>();
		if (root.getAttributeValue("version").startsWith("1.")) {
			parseXliff(root);
		} else {
			throw new IOException("Unsupported XLIFF version");
		}
		if (segmentsList.isEmpty()) {
			throw new IOException("Empty XLIFF");
		}
	}

	private Element basicSvg() {
		Element svg = new Element("svg");
		svg.setAttribute("xmlns", "http://www.w3.org/2000/svg");
		svg.setAttribute("width", "640px");
		svg.setAttribute("height", "130px");

		Element rect = new Element("rect");
		rect.setAttribute("width", "640px");
		rect.setAttribute("height", "130px");
		rect.setAttribute("style", "fill:white;");
		svg.addContent(rect);

		Element xAxe = new Element("line");
		xAxe.setAttribute("x1", "5");
		xAxe.setAttribute("y1", "110");
		xAxe.setAttribute("x2", "615");
		xAxe.setAttribute("y2", "110");
		xAxe.setAttribute("style", "stroke:rgb(0,0,0);stroke-width:2;");
		svg.addContent(xAxe);

		for (int x = 0; x < 20; x++) {
			Element pipe = new Element("line");
			pipe.setAttribute("x1", "" + (40 + x * 30));
			pipe.setAttribute("x2", "" + (40 + x * 30));
			if (x % 2 == 0) {
				pipe.setAttribute("y1", "108");
				pipe.setAttribute("y2", "112");
			} else {
				pipe.setAttribute("y1", "106");
				pipe.setAttribute("y2", "114");
			}
			pipe.setAttribute("style", "stroke:rgb(0,0,0);stroke-width:1;");
			svg.addContent(pipe);
		}
		int size = segmentsList.size() * 10 / 1000;
		for (int x = 10; x <= 100; x += 10) {
			Element text = new Element("text");
			text.setText(x * size + "");
			text.setAttribute("style", "font-family:sans-serif;font-size:10px;");
			text.setAttribute("x", "" + +x * 6);
			text.setAttribute("y", "122");
			svg.addContent(text);
		}

		Element yAxe = new Element("line");
		yAxe.setAttribute("x1", "10");
		yAxe.setAttribute("y1", "5");
		yAxe.setAttribute("x2", "10");
		yAxe.setAttribute("y2", "115");
		yAxe.setAttribute("style", "stroke:rgb(0,0,0);stroke-width:2;");
		svg.addContent(yAxe);

		Element yTop = new Element("line");
		yTop.setAttribute("x1", "10");
		yTop.setAttribute("y1", "10");
		yTop.setAttribute("x2", "610");
		yTop.setAttribute("y2", "10");
		yTop.setAttribute("style", "stroke:grey;stroke-width:1;stroke-dasharray:5,5;");
		svg.addContent(yTop);

		return svg;
	}

	public Element generateMatchesSvg() {
		Element svg = basicSvg();

		Element text100 = new Element("text");
		text100.setText("100%");
		text100.setAttribute("style", "font-family:sans-serif;font-size:10px;");
		text100.setAttribute("x", "612");
		text100.setAttribute("y", "15");
		svg.addContent(text100);

		Element yCenter = new Element("line");
		yCenter.setAttribute("x1", "10");
		yCenter.setAttribute("y1", "60");
		yCenter.setAttribute("x2", "610");
		yCenter.setAttribute("y2", "60");
		yCenter.setAttribute("style", "stroke:grey;stroke-width:1;stroke-dasharray:5,5;");
		svg.addContent(yCenter);

		Element text50 = new Element("text");
		text50.setText("50%");
		text50.setAttribute("style", "font-family:sans-serif;font-size:10px;");
		text50.setAttribute("x", "612");
		text50.setAttribute("y", "65");
		svg.addContent(text50);

		int size = segmentsList.size() * 10 / 2000;
		for (int x = 0; x < 200; x++) {
			float y = matchAverage(x, size);
			if (y < 1) {
				continue;
			}
			Element line = new Element("line");
			line.setAttribute("x1", "" + (12 + 3 * x));
			line.setAttribute("y1", "" + (110 - y));
			line.setAttribute("x2", "" + (12 + 3 * x));
			line.setAttribute("y2", "110");
			line.setAttribute("style", "stroke:blue;stroke-width:1;");
			svg.addContent(line);
		}
		Indenter.indent(svg, 2);
		return svg;
	}

	public Element generateTranslatedSvg() {
		Element svg = basicSvg();

		int size = segmentsList.size() * 10 / 2000;
		for (int x = 0; x < 200; x++) {
			float y = translatedAverage(x, size);
			if (y < 1) {
				continue;
			}

			Element line = new Element("line");
			line.setAttribute("x1", "" + (12 + 3 * x));
			line.setAttribute("y1", "" + (110 - y));
			line.setAttribute("x2", "" + (12 + 3 * x));
			line.setAttribute("y2", "110");
			line.setAttribute("style", "stroke:green;stroke-width:2;");
			svg.addContent(line);
		}
		Indenter.indent(svg, 2);
		return svg;
	}

	public Element generateApprovedSvg() {
		Element svg = basicSvg();

		int size = segmentsList.size() * 10 / 2000;
		for (int x = 0; x < 200; x++) {
			float y = approvedAverage(x, size);
			if (y < 1) {
				continue;
			}

			Element line = new Element("line");
			line.setAttribute("x1", "" + (12 + 3 * x));
			line.setAttribute("y1", "" + (110 - y));
			line.setAttribute("x2", "" + (12 + 3 * x));
			line.setAttribute("y2", "110");
			line.setAttribute("style", "stroke:red;stroke-width:2;");
			svg.addContent(line);
		}
		Indenter.indent(svg, 2);
		return svg;
	}

	private float translatedAverage(int x, int size) {
		float total = 0;
		int counted = 0;
		for (int count = 0; count < size; count++) {
			if (x * size + count < segmentsList.size()) {
				total += segmentsList.get(x * size + count).isTranslated() ? 1 : 0;
				counted++;
			}
		}
		if (counted == 0) {
			return 0;
		}
		return total > counted/2 ? 100 : 0;
	}

	private float approvedAverage(int x, int size) {
		float total = 0;
		int counted = 0;
		for (int count = 0; count < size; count++) {
			if (x * size + count < segmentsList.size()) {
				total += segmentsList.get(x * size + count).isApproved() ? 1 : 0;
				counted++;
			}
		}
		if (counted == 0) {
			return 0;
		}
		return total > counted/2 ? 100 : 0;
	}

	private float matchAverage(int x, int size) {
		float total = 0;
		int counted = 0;
		for (int count = 0; count < size; count++) {
			if (x * size + count < segmentsList.size()) {
				total += segmentsList.get(x * size + count).getMatch();
				counted++;
			}
		}
		if (counted == 0) {
			return 0;
		}
		return total / counted;
	}

	private void parseXliff(Element e) {
		if ("trans-unit".equals(e.getName())) {
			SegmentStatus status = new SegmentStatus();
			status.setApproved(e.getAttributeValue("approved", "no").equalsIgnoreCase("yes"));
			Element target = e.getChild("target");
			if (target == null) {
				status.setTranslated(false);
			} else {
				status.setTranslated(!target.getContent().isEmpty());
			}
			List<Element> matches = e.getChildren("alt-trans");
			if (!matches.isEmpty()) {
				float max = 0;
				Iterator<Element> it = matches.iterator();
				while (it.hasNext()) {
					Element match = it.next();
					try {
						float quality = Float.parseFloat(match.getAttributeValue("match-quality", "0"));
						if (quality > max) {
							max = quality;
						}
					} catch (NumberFormatException ex) {
						// ignore
					}
				}
				if (max > 100) {
					max = 100;
				}
				status.setMatch(max);
			}
			segmentsList.add(status);
		}
		List<Element> children = e.getChildren();
		Iterator<Element> it = children.iterator();
		while (it.hasNext()) {
			parseXliff(it.next());
		}
	}

}
