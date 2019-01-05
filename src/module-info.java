/*******************************************************************************
 * Copyright (c) 2003-2019 Maxprograms.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors: Maxprograms - initial API and implementation
 *******************************************************************************/
module xliffFilters {
	exports com.maxprograms.xml;
	exports com.maxprograms.converters;
	exports com.maxprograms.converters.html;
	exports com.maxprograms.converters.po;
	exports com.maxprograms.converters.ts;
	exports com.maxprograms.converters.office;
	exports com.maxprograms.converters.xml;
	exports com.maxprograms.converters.resx;
	exports com.maxprograms.converters.javascript;
	exports com.maxprograms.converters.javaproperties;
	exports com.maxprograms.converters.msoffice;
	exports com.maxprograms.converters.ditamap;
	exports com.maxprograms.converters.idml;
	exports com.maxprograms.converters.mif;
	exports com.maxprograms.converters.rc;
	exports com.maxprograms.converters.txml;
	exports com.maxprograms.converters.plaintext;
	exports com.maxprograms.languages;
	exports com.maxprograms.segmenter;

	opens com.maxprograms.xml to mapdb;	

	requires dtd;
	requires json;
	requires jsoup;
	requires java.logging;
	requires jdk.httpserver;
	requires transitive java.xml;
	requires transitive java.base;
}
