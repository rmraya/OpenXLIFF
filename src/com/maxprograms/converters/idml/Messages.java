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
package com.maxprograms.converters.idml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

public class Messages {

    private static Properties props;

    private Messages() {
    }

    public static String getString(String key) {
        try {
            if (props == null) {
                Locale locale = Locale.getDefault();
                String extension = "en".equals(locale.getLanguage()) ? ".properties"
                        : "_" + locale.getLanguage() + ".properties";
                try (InputStream is = Messages.class.getResourceAsStream("idml" + extension)) {
                    try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        props = new Properties();
                        props.load(reader);
                    }
                }
            }
            return props.getProperty(key, '!' + key + '!');
        } catch (IOException e) {
            return '!' + key + '!';
        }
    }
}
