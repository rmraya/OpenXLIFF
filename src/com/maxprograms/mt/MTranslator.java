/*******************************************************************************
 * Copyright (c)  Maxprograms.
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
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.maxprograms.converters.Constants;
import com.maxprograms.xml.Element;
import com.maxprograms.xml.Indenter;
import com.maxprograms.xml.TextNode;
import com.maxprograms.xml.XMLNode;

import org.json.JSONObject;

public class MTranslator {

    private List<MTEngine> engines;

    public MTranslator() {
        engines = new Vector<>();
    }

    public void addEngine(MTEngine engine) {
        engines.add(engine);
    }

    public void removeEngine(MTEngine engine) {
        for (int i = 0; i < engines.size(); i++) {
            MTEngine e = engines.get(i);
            if (e.equals(engine)) {
                engines.remove(i);
            }
        }
    }

    public void setEngines(List<MTEngine> engines) {
        this.engines = engines;
    }

    public boolean hasEngines() {
        return !engines.isEmpty();
    }

    public List<JSONObject> translate(String text) throws IOException, InterruptedException {
        List<JSONObject> result = new Vector<>();
        if (text.isBlank()) {
            return result;
        }
        Iterator<MTEngine> it = engines.iterator();
        while (it.hasNext()) {
            MTEngine engine = it.next();
            String target = engine.translate(text);
            if (target != null && !target.isEmpty()) {
                JSONObject json = new JSONObject();
                json.put("key", engine.getShortName());
                json.put("engine", engine.getName());
                json.put("target", target);
                json.put("srcLang", engine.getSourceLanguage());
                json.put("tgtLang", engine.getTargetLanguage());
                result.add(json);
            } else {
                throw new IOException("Empty or null translation received from " + engine.getName());
            }
        }
        return result;
    }

    public void translate(Element segment) throws IOException, InterruptedException {
        if (engines.isEmpty()) {
            return;
        }

        String source = extractSource(segment);
        if (source.isEmpty()) {
            return;
        }
        Iterator<MTEngine> it = engines.iterator();
        while (it.hasNext()) {
            MTEngine engine = it.next();
            String target = engine.translate(source);
            if (target != null && !target.isEmpty()) {
                addTranslation(segment, source, target, engine.getSourceLanguage(), engine.getTargetLanguage(),
                        engine.getName());
            } else {
                throw new IOException("Empty or null translation received from " + engine.getName());
            }
        }
    }

    private static String extractSource(Element segment) throws IOException {
        if ("trans-unit".equals(segment.getName())) {
            return unclean(pureText(segment.getChild("source")));
        }
        throw new IOException("Unsupported element");
    }

    private void addTranslation(Element segment, String source, String target, String srcLang, String tgtLang,
            String origin) {
        Element e = new Element("alt-trans");
        e.setAttribute("origin", origin);
        e.setAttribute("tool", Constants.TOOLNAME);
        e.setAttribute("xml:space", "default");
        Element s = new Element("source");
        s.setAttribute("xml:lang", srcLang);
        s.setText(unclean(source));
        Element t = new Element("target");
        t.setAttribute("xml:lang", tgtLang);
        t.setText(unclean(target));
        e.addContent(s);
        e.addContent(t);
        Indenter.indent(e, 2);
        segment.addContent("\n");
        segment.addContent(e);
    }

    private static String unclean(String string) {
        String result = string.replaceAll("&gt;", ">");
        result = result.replaceAll("&lt;", "<");
        result = result.replaceAll("&quot;", "\"");
        return result.replaceAll("&amp;", "&");
    }

    private static String pureText(Element seg) {
        List<XMLNode> l = seg.getContent();
        Iterator<XMLNode> i = l.iterator();
        StringBuilder text = new StringBuilder();
        while (i.hasNext()) {
            XMLNode o = i.next();
            if (o.getNodeType() == XMLNode.TEXT_NODE) {
                text.append(((TextNode) o).getText());
            } else if (o.getNodeType() == XMLNode.ELEMENT_NODE) {
                Element e = (Element) o;
                String type = e.getName();
                // discard all inline elements
                // except <mrk>, <g> and <hi>
                if (type.equals("mrk") || type.equals("hi") || type.equals("g")) {
                    text.append(pureText(e));
                }
            }
        }
        return text.toString();
    }

}