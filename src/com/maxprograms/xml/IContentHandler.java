package com.maxprograms.xml;

import org.xml.sax.ContentHandler;
import org.xml.sax.ext.LexicalHandler;

public interface IContentHandler extends ContentHandler, LexicalHandler {
	
	void setCatalog(Catalog catalog);
	Document getDocument();

}
