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

package com.maxprograms.mt;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import com.maxprograms.languages.Language;

import org.xml.sax.SAXException;

public interface MTEngine {

    public String getName();

    public String getShortName();

    public List<Language> getSourceLanguages() throws IOException, InterruptedException, SAXException, ParserConfigurationException;

    public List<Language> getTargetLanguages() throws IOException, InterruptedException, SAXException, ParserConfigurationException;

    public void setSourceLanguage(String lang);

    public String getSourceLanguage();

    public void setTargetLanguage(String lang);

    public String getTargetLanguage();

    public String translate(String source) throws IOException, InterruptedException;
}