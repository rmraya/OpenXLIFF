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
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.converters.Utils;
import com.maxprograms.xml.Catalog;
import com.maxprograms.xml.Document;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.SAXBuilder;

public class SvgStats {
	
	protected class SegmentStatus {
		
		private boolean approved;
		private int match;

		public SegmentStatus() {
			approved = false;
			match = 0;
		}

		public boolean isApproved() {
			return approved;
		}

		public void setApproved(boolean approved) {
			this.approved = approved;
		}

		public int getMatch() {
			return match;
		}

		public void setMatch(int match) {
			this.match = match;
		}
		
	}
	
	private static Logger LOGGER = System.getLogger(SvgStats.class.getName());

	private List<SegmentStatus>  segmentsList;

	public static void main(String[] args) {

		String[] fixedArgs = Utils.fixPath(args);

		String file = "";
		String catalog = "";
		for (int i = 0; i < fixedArgs.length; i++) {
			String arg = fixedArgs[i];
			if (arg.equals("-file") && (i + 1) < fixedArgs.length) {
				file = fixedArgs[i + 1];
			}
			if (arg.equals("-catalog") && (i + 1) < fixedArgs.length) {
				catalog = fixedArgs[i + 1];
			}
		}
		if (fixedArgs.length < 2) {
			return;
		}
		if (file.isEmpty()) {
			LOGGER.log(Level.ERROR, "Missing '-file' parameter.");
			return;
		}
		if (catalog.isEmpty()) {
			File catalogFolder = new File(new File(System.getProperty("user.dir")), "catalog");
			catalog = new File(catalogFolder, "catalog.xml").getAbsolutePath();
		}
		File catalogFile = new File(catalog);
		if (!catalogFile.exists()) {
			LOGGER.log(Level.ERROR, "Catalog file does not exist.");
			return;
		}
		try {
			SvgStats instance = new SvgStats();
			instance.analyze(file, catalog);
		} catch (IOException | SAXException | ParserConfigurationException e) {
			LOGGER.log(Level.ERROR, "Error analyzing file", e);
		}
	}

	
	private void analyze(String file, String catalog) throws SAXException, IOException, ParserConfigurationException {
		// TODO Auto-generated method stub
		SAXBuilder builder = new SAXBuilder();
		builder.setEntityResolver(new Catalog(catalog));
		Document document = builder.build(file);
		Element root = document.getRootElement();
		if (!"xliff".equals(root.getLocalName())) {
			throw new IOException("Selected file is not an XLIFF document");
		}
		segmentsList = new ArrayList<>();
		if (root.getAttributeValue("version").startsWith("1.")) {
			parseXliff1(root);
		} else if (root.getAttributeValue("version").startsWith("2.")) {
			parseXliff2(root);
		} else {
			throw new IOException("Unsupported XLIFF version");
		}
	}


	private void parseXliff2(Element e) {
		// TODO Auto-generated method stub
		if ("segment".equals(e.getName())) {
			
		}
		List<Element> children = e.getChildren();
		Iterator<Element> it = children.iterator();
		while (it.hasNext()) {
			parseXliff1(it.next());
		}
		
	}

	private void parseXliff1(Element e) {
		// TODO Auto-generated method stub
		if ("trans-unit".equals(e.getName())) {
			
		}
		List<Element> children = e.getChildren();
		Iterator<Element> it = children.iterator();
		while (it.hasNext()) {
			parseXliff1(it.next());
		}
	}
	
}
