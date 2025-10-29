/*******************************************************************************
 * Copyright (c) 2018 - 2025 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/
package com.maxprograms.segmenter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.maxprograms.xml.Catalog;

public final class SegmenterPool {

    private static final ConcurrentMap<SegmenterKey, Segmenter> CACHE = new ConcurrentHashMap<>();

    private SegmenterPool() {
        // utility class
    }

    public static Segmenter getSegmenter(String srxFile, String language, Catalog catalog)
            throws SAXException, IOException, ParserConfigurationException {
        if (srxFile == null || language == null) {
            throw new IOException(Messages.getString("SegmenterPool.1"));
        }
        SegmenterKey key = SegmenterKey.of(srxFile, language);
        Segmenter cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        Segmenter created = new Segmenter(srxFile, language, catalog);
        Segmenter existing = CACHE.putIfAbsent(key, created);
        return existing != null ? existing : created;
    }

    private record SegmenterKey(String srxFile, String language) {

        static SegmenterKey of(String srxFile, String language) {
            return new SegmenterKey(normalize(srxFile), language);
        }

        private static String normalize(String path) {
            return Path.of(path).toAbsolutePath().normalize().toString();
        }
    }
}
