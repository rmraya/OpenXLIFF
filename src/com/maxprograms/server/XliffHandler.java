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

package com.maxprograms.server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.Convert;
import com.maxprograms.converters.EncodingResolver;
import com.maxprograms.converters.FileFormats;
import com.maxprograms.converters.Merge;
import com.maxprograms.converters.TmxExporter;
import com.maxprograms.languages.Language;
import com.maxprograms.languages.LanguageUtils;
import com.maxprograms.stats.RepetitionAnalysis;
import com.maxprograms.validation.XliffChecker;
import com.maxprograms.xliff2.ToXliff2;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.SAXException;

public class XliffHandler implements HttpHandler {

    private static Logger LOGGER = System.getLogger(XliffHandler.class.getName());

    private IServer server;

    private Hashtable<String, String> running;
	private Hashtable<String, JSONObject> validationResults;
	private Hashtable<String, JSONObject> conversionResults;
	private Hashtable<String, JSONObject> mergeResults;
	private Hashtable<String, JSONObject> analysisResults;
	private boolean embed;
	private String xliff;
	private String catalog;
	private boolean is20;
	private String target;
	private boolean unapproved;
    private boolean exportTmx;
    
    public XliffHandler(IServer server) {
        this.server = server;
        running = new Hashtable<>();
		validationResults = new Hashtable<>();
		conversionResults = new Hashtable<>();
		mergeResults = new Hashtable<>();
		analysisResults = new Hashtable<>();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
		URI uri = exchange.getRequestURI();
		String request = "";
		try (InputStream is = exchange.getRequestBody()) {
			request = readRequestBody(is);
		}

		JSONObject json = null;
		String response = "";
		String command = "version";
		try {
			if (uri.toString().endsWith("/stop")) {
				command = "stop";
				response = "{\"stopping\": \"now!\"}";
			}
			if (!request.isBlank()) {
				json = new JSONObject(request);
				command = json.getString("command");
			}
			if (command.equals("version")) {
				response = "{\"tool\":\"" + Constants.TOOLNAME + "\", \"version\": \"" + Constants.VERSION + "\", \"build\": \""
						+ Constants.BUILD + "\"}";
			}
			if (command.equals("convert")) {
				response = convert(json);
			}
			if (command.equals("merge")) {
				response = merge(json);
			}
			if (command.equals("status")) {
				response = getStatus(json);
			}
			if (command.equals("validationResult")) {
				response = getValidationResult(json);
			}
			if (command.equals("conversionResult")) {
				response = getConversionResult(json);
			}
			if (command.equals("mergeResult")) {
				response = getMergeResult(json);
			}
			if (command.equals("analysisResult")) {
				response = getAnalysisResult(json);
			}
			if (command.equals("getFileType")) {
				response = getFileType(json);
			}
			if (command.equals("getTargetFile")) {
				response = getTargetFile(json);
			}
			if (command.equals("validateXliff")) {
				response = validateXliff(json);
			}
			if (command.equals("analyseXliff")) {
				response = analyseXliff(json);
			}
			if (command.equals("getTypes") || uri.toString().endsWith("/getTypes")) {
				response = getTypes();
			}
			if (command.equals("getCharsets") || uri.toString().endsWith("/getCharsets")) {
				response = getCharsets();
			}
			if (command.equals("getLanguages") || uri.toString().endsWith("/getLanguages")) {
				response = getLanguages();
			}
			exchange.getResponseHeaders().add("content-type", "application/json");
			exchange.sendResponseHeaders(200, response.length());
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(new ByteArrayInputStream(response.getBytes())))) {
				try (OutputStream os = exchange.getResponseBody()) {
					String line;
					while ((line = reader.readLine()) != null) {
						os.write(line.getBytes());
					}
				}
			}
			if (command.equals("stop")) {
				server.stop();
			}
		} catch (IOException | SAXException | ParserConfigurationException e) {
			response = e.getMessage();
			exchange.sendResponseHeaders(500, response.length());
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(response.getBytes());
			}
		}
	}

    private static String readRequestBody(InputStream is) throws IOException {
		StringBuilder request = new StringBuilder();
		try (BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			String line;
			while ((line = rd.readLine()) != null) {
				request.append(line);
			}
		}
		return request.toString();
	}

    
	private String merge(JSONObject json) {
		xliff = "";
		if (json.has("xliff")) {
			xliff = json.getString("xliff");
		}
		target = "";
		if (json.has("target")) {
			target = json.getString("target");
		}
		catalog = "";
		if (json.has("catalog")) {
			catalog = json.getString("catalog");
		}
		if (catalog.isEmpty()) {
			File catalogFolder = new File(new File(System.getProperty("user.dir")), "catalog");
			catalog = new File(catalogFolder, "catalog.xml").getAbsolutePath();
		}
		unapproved = false;
		if (json.has("unapproved")) {
			unapproved = json.getBoolean("unapproved");
		}
		exportTmx = false;
		if (json.has("exportTmx")) {
			exportTmx = json.getBoolean("exportTmx");
		}
		String process = "" + System.currentTimeMillis();
		new Thread(new Runnable() {

			@Override
			public void run() {
				running.put(process, "running");

				Vector<String> result = Merge.merge(xliff, target, catalog, unapproved);
				if (exportTmx && Constants.SUCCESS.equals(result.get(0))) {
					String tmx = "";
					if (xliff.toLowerCase().endsWith(".xlf")) {
						tmx = xliff.substring(0, xliff.lastIndexOf('.')) + ".tmx";
					} else {
						tmx = xliff + ".tmx";
					}
					result = TmxExporter.export(xliff, tmx, catalog);
				}
				JSONObject jsonResult = new JSONObject();
				if (Constants.SUCCESS.equals(result.get(0))) {
					jsonResult.put("result", "Success");
				} else {
					jsonResult.put("result", "Failed");
					jsonResult.put("reason", result.get(1));
				}
				mergeResults.put(process, jsonResult);
				if (running.get(process).equals(("running"))) {
					running.put(process, "completed");
				}
			}
		}).start();
		return "{\"process\":\"" + process + "\"}";
	}

	private String convert(JSONObject json) {
		String source = "";
		if (json.has("file")) {
			source = json.getString("file");
		}
		String srcLang = "";
		if (json.has("srcLang")) {
			srcLang = json.getString("srcLang");
		}
		String tgtLang = "";
		if (json.has("tgtLang")) {
			tgtLang = json.getString("tgtLang");
		}
		xliff = source + ".xlf";
		if (json.has("xliff")) {
			xliff = json.getString("xliff");
		}
		String skl = source + ".skl";
		if (json.has("sklFolder")) {
			File sklFolder = new File(json.getString("sklFolder"));
			if (!sklFolder.exists()) {
				try {
					Files.createDirectories(sklFolder.toPath());
				} catch (IOException e) {
					LOGGER.log(Level.ERROR, "Unable to create skeleton folder " + json.getString("sklFolder"));
				}
			}
			try {
				File tmp = File.createTempFile(new File(source).getName(), ".skl", sklFolder);
				skl = tmp.getAbsolutePath();
			} catch (IOException e) {
				LOGGER.log(Level.ERROR, "Error creating skeleton", e);
			}
		}
		if (json.has("skl")) {
			skl = json.getString("skl");
		}
		String type = "";
		if (json.has("type")) {
			type = json.getString("type");
			String fullName = FileFormats.getFullName(type);
			if (fullName != null) {
				type = fullName;
			}
		}
		if (type.isEmpty()) {
			String detected = FileFormats.detectFormat(source);
			if (detected != null) {
				type = detected;
				LOGGER.log(Level.INFO, "Auto-detected type: " + type);
			} else {
				LOGGER.log(Level.ERROR, "Unable to auto-detect file format. Use '-type' parameter.");
			}
		}
		String enc = "";
		if (json.has("enc")) {
			enc = json.getString("enc");
		}
		if (enc.isEmpty()) {
			Charset charset = EncodingResolver.getEncoding(source, type);
			if (charset != null) {
				enc = charset.name();
				LOGGER.log(Level.INFO, "Auto-detected encoding: " + enc);
			} else {
				LOGGER.log(Level.ERROR, "Unable to auto-detect character set. Use '-enc' parameter.");
			}
		}
		String srx = "";
		if (json.has("srx")) {
			srx = json.getString("srx");
		}
		if (srx.isEmpty()) {
			File srxFolder = new File(new File(System.getProperty("user.dir")), "srx");
			srx = new File(srxFolder, "default.srx").getAbsolutePath();
		}
		catalog = "";
		if (json.has("catalog")) {
			catalog = json.getString("catalog");
		}
		if (catalog.isEmpty()) {
			File catalogFolder = new File(new File(System.getProperty("user.dir")), "catalog");
			catalog = new File(catalogFolder, "catalog.xml").getAbsolutePath();
		}
		String ditaval = "";
		if (json.has("ditaval") ) {
			ditaval = json.getString("ditaval");
		}
		embed = false;
		if (json.has("embed")) {
			embed = json.getBoolean("embed");
		}
		boolean paragraph = false;
		if (json.has("paragraph")) {
			paragraph = json.getBoolean("paragraph");
		}
		is20 = false;
		if (json.has("is20")) {
			is20 = json.getBoolean("is20");
		}
		String process = "" + System.currentTimeMillis();

		Hashtable<String, String> params = new Hashtable<>();
		params.put("source", source);
		params.put("srcLang", srcLang);
		params.put("xliff", xliff);
		params.put("skeleton", skl);
		params.put("format", type);
		params.put("catalog", catalog);
		params.put("srcEncoding", enc);
		params.put("paragraph", paragraph ? "yes" : "no");
		params.put("srxFile", srx);
		if (!tgtLang.isEmpty()) {
			params.put("tgtLang", tgtLang);
		}
		if (type.equals(FileFormats.DITA) && !ditaval.isEmpty()) {
			params.put("ditaval", ditaval);
		}

		new Thread(new Runnable() {

			@Override
			public void run() {
				running.put(process, "running");
				Vector<String> result = Convert.run(params);
				if (embed && Constants.SUCCESS.equals(result.get(0))) {
					result = Convert.addSkeleton(xliff, catalog);
				}
				if (is20 && Constants.SUCCESS.equals(result.get(0))) {
					result = ToXliff2.run(new File(xliff), catalog);
				}
				JSONObject jsonResult = new JSONObject();
				if (Constants.SUCCESS.equals(result.get(0))) {
					jsonResult.put("result", "Success");
				} else {
					jsonResult.put("result", "Failed");
					jsonResult.put("reason", result.get(1));
				}
				conversionResults.put(process, jsonResult);
				if (running.get(process).equals(("running"))) {
					running.put(process, "completed");
				}
			}
		}).start();
		return "{\"process\":\"" + process + "\"}";
	}

    private static String getFileType(JSONObject json) {
		String file = json.getString("file");
		String type = "Unknown";
		String encoding = "Unknown";
		String detected = FileFormats.detectFormat(file);
		if (detected != null) {
			type = FileFormats.getShortName(detected);
			if (type != null) {
				Charset charset = EncodingResolver.getEncoding(file, detected);
				if (charset != null) {
					encoding = charset.name();
				}
			}
		}
		if (encoding.equals("Unknown")) {
			try {
				Charset bom = EncodingResolver.getBOM(file);
				if (bom != null) {
					encoding = bom.name();
				}
			} catch (IOException e) {
				// ignore
			}
		}
		JSONObject result = new JSONObject();
		result.put("file", file);
		result.put("type", type);
		result.put("encoding", encoding);
		return result.toString();
	}

	private static String getTypes() {
		String[] formats = FileFormats.getFormats();
		StringBuilder builder = new StringBuilder();
		builder.append("{\"types\": [\n");
		for (int i = 0; i < formats.length; i++) {
			if (i > 0) {
				builder.append(",\n");
			}
			builder.append("{\"type\":\"");
			builder.append(FileFormats.getShortName(formats[i]));
			builder.append("\", \"description\":\"");
			builder.append(formats[i]);
			builder.append("\"}");
		}
		builder.append("]}\n");
		return builder.toString();
	}

	private static String getLanguages() throws SAXException, IOException, ParserConfigurationException {
		List<Language> languages = LanguageUtils.getCommonLanguages();
		StringBuilder builder = new StringBuilder();
		builder.append("{\"languages\": [\n");
		for (int i = 0; i < languages.size(); i++) {
			Language lang = languages.get(i);
			if (i > 0) {
				builder.append(",\n");
			}
			builder.append("{\"code\":\"");
			builder.append(lang.getCode());
			builder.append("\", \"description\":\"");
			builder.append(lang.getDescription());
			builder.append("\"}");
		}
		builder.append("]}\n");
		return builder.toString();
    }
    
    private String getAnalysisResult(JSONObject json) {
		JSONObject result = new JSONObject();
		if (json.has("process")) {
			String process = json.getString("process");
			result = analysisResults.get(process);
			analysisResults.remove(process);
		} else {
			result.put("result", "Failed");
			result.put("reason", "Error retrieving result from server");
		}
		return result.toString(2);
	}

	private String getMergeResult(JSONObject json) {
		JSONObject result = new JSONObject();
		if (json.has("process")) {
			String process = json.getString("process");
			result = mergeResults.get(process);
			mergeResults.remove(process);
		} else {
			result.put("result", "Failed");
			result.put("reason", "Error retrieving result from server");
		}
		return result.toString(2);
	}

	private String getConversionResult(JSONObject json) {
		JSONObject result = new JSONObject();
		if (json.has("process")) {
			String process = json.getString("process");
			result = conversionResults.get(process);
			conversionResults.remove(process);
		} else {
			result.put("result", "Failed");
			result.put("reason", "Error retrieving result from server");
		}
		return result.toString(2);
	}

	private String analyseXliff(JSONObject json) {
		String file = json.getString("file");
		catalog = "";
		if (json.has("catalog")){
			catalog = json.getString("catalog");
		}
		if (catalog.isEmpty()) {
			File catalogFolder = new File(new File(System.getProperty("user.dir")), "catalog");
			catalog = new File(catalogFolder, "catalog.xml").getAbsolutePath();
		}

		String process = "" + System.currentTimeMillis();
		new Thread(new Runnable() {

			@Override
			public void run() {
				running.put(process, "running");
				Vector<String> result = new Vector<>();
				try {
					RepetitionAnalysis instance = new RepetitionAnalysis();
					instance.analyse(file, catalog);
					result.add(Constants.SUCCESS);
				} catch (IOException | SAXException | ParserConfigurationException | URISyntaxException e) {
					LOGGER.log(Level.ERROR, "Error analysing file", e);
					result.add(Constants.ERROR);
					result.add(e.getMessage());
				}
				JSONObject jsonResult = new JSONObject();
				if (Constants.SUCCESS.equals(result.get(0))) {
					jsonResult.put("result", "Success");
				} else {
					jsonResult.put("result", "Failed");
					jsonResult.put("reason", result.get(1));
				}
				analysisResults.put(process, jsonResult);
				running.put(process, "completed");
			}
		}).start();
		return "{\"process\":\"" + process + "\"}";
	}

	private String validateXliff(JSONObject json) {
		String file = json.getString("file");
		catalog = "";
		if (json.has("catalog")) {
			catalog = json.getString("catalog");
		}
		if (catalog.isEmpty()) {
			File catalogFolder = new File(new File(System.getProperty("user.dir")), "catalog");
			catalog = new File(catalogFolder, "catalog.xml").getAbsolutePath();
		}

		String process = "" + System.currentTimeMillis();
		new Thread(new Runnable() {

			@Override
			public void run() {
				running.put(process, "running");
				try {
					XliffChecker validator = new XliffChecker();
					boolean valid = validator.validate(file, catalog);
					JSONObject result = new JSONObject();
					result.put("valid", valid);
					if (valid) {
						String version = validator.getVersion();
						result.put("comment", "Selected file is valid XLIFF " + version);
					} else {
						String reason = validator.getReason();
						result.put("reason", reason);
					}
					validationResults.put(process, result);
					if (running.get(process).equals(("running"))) {
						LOGGER.log(Level.INFO, "Validation completed");
						running.put(process, "completed");
					}
				} catch (IOException e) {
					LOGGER.log(Level.ERROR, "Error validating file", e);
					running.put(process, e.getMessage());
				}
			}
		}).start();
		return "{\"process\":\"" + process + "\"}";
	}

	private static String getTargetFile(JSONObject json) {
		JSONObject result = new JSONObject();
		String file = json.getString("file");
		String target = "";
		try {
			target = Merge.getTargetFile(file);
			if (target.isEmpty()) {
				target = "Unknown";
			}
			result.put("result", "Success");
		} catch (IOException | SAXException | ParserConfigurationException e) {
			LOGGER.log(Level.ERROR, "Error getting target file", e);
			result.put("result", "Failed");
			result.put("reason", e.getMessage());
		}
		result.put("target", target);
		return result.toString();
	}

	private static String getCharsets() {
		JSONObject result = new JSONObject();
		JSONArray array = new JSONArray();
		result.put("charsets", array);
		TreeMap<String, Charset> charsets = new TreeMap<>(Charset.availableCharsets());
		Set<String> keys = charsets.keySet();
		Iterator<String> i = keys.iterator();
		while (i.hasNext()) {
			Charset cset = charsets.get(i.next());
			JSONObject charset = new JSONObject();
			charset.put("code", cset.name());
			charset.put("description", cset.displayName());
			array.put(charset);
		}
		return result.toString(2);
	}

	private String getStatus(JSONObject json) {
		String status = "unknown";
		if (json.has("process")) {
			String process = json.getString("process");
			status = running.get(process);
		}
		if (status == null) {
			status = "Error";
		}
		return "{\"status\": \"" + status + "\"}";
	}

	private String getValidationResult(JSONObject json) {
		JSONObject result = new JSONObject();
		if (json.has("process")) {
			String process = json.getString("process");
			result = validationResults.get(process);
			validationResults.remove(process);
		} else {
			result.put("valid", false);
			result.put("reason", "Error retrieving result from server");
		}
		return result.toString(2);
	}
}