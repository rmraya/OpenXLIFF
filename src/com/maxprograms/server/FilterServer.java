/*******************************************************************************
 * Copyright (c) 2003, 2018 Maxprograms.
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
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.maxprograms.converters.Constants;
import com.maxprograms.converters.Convert;
import com.maxprograms.converters.EncodingResolver;
import com.maxprograms.converters.FileFormats;
import com.maxprograms.converters.Merge;
import com.maxprograms.languages.Language;
import com.maxprograms.validation.XliffChecker;
import com.maxprograms.xliff2.ToXliff2;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class FilterServer implements HttpHandler {

	private static Logger LOGGER = System.getLogger(FilterServer.class.getName());

	private HttpServer server;
	private Hashtable<String, String> running;
	private boolean embed;
	private String xliff;
	private String catalog;
	private boolean is20;
	private String target;
	private boolean unapproved;

	public static void main(String[] args) {
		String port = "8000";
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.equals("-version")) {
				LOGGER.log(Level.INFO, () -> "Version: " + Constants.VERSION + " Build: " + Constants.BUILD);
				return;
			}
			if (arg.equals("-port") && (i + 1) < args.length) {
				port = args[i + 1];
			}
		}
		try {
			FilterServer instance = new FilterServer(Integer.valueOf(port));
			instance.run();
		} catch (Exception e) {
			LOGGER.log(Level.ERROR, "Server error", e);
		}
	}

	public FilterServer(int port) throws IOException {
		running = new Hashtable<>();
		server = HttpServer.create(new InetSocketAddress(port), 0);
		server.createContext("/FilterServer", this);
		server.setExecutor(null); // creates a default executor
	}

	public void run() {
		server.start();
		LOGGER.log(Level.INFO, "FilterServer started");
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		URI uri = t.getRequestURI();
		InputStream is = t.getRequestBody();
		String request = readRequestBody(is);
		is.close();

		JSONObject json = null;
		String response = "";
		String command = "version";
		try {
			if (!request.isBlank()) {
				json = new JSONObject(request);
				command = json.getString("command");
			}
			if (command.equals("version")) {
				response = "{\"tool\":\"Open XLIFF Filters\", \"version\": \"" + Constants.VERSION + "\", \"build\": \""
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
			if (command.equals("getFileType")) {
				response = getFileType(json);
			}
			if (command.equals("getTargetFile")) {
				response = getTargetFile(json);
			}
			if (command.equals("validateXliff")) {
				response = validateXliff(json);
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
			t.getResponseHeaders().add("content-type", "application/json");
			t.sendResponseHeaders(200, response.length());
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(new ByteArrayInputStream(response.getBytes())))) {
				try (OutputStream os = t.getResponseBody()) {
					String line;
					while ((line = reader.readLine()) != null) {
						os.write(line.getBytes());
					}
				}
			}
		} catch (IOException | SAXException | ParserConfigurationException e) {
			response = e.getMessage();
			t.sendResponseHeaders(500, response.length());
			try (OutputStream os = t.getResponseBody()) {
				os.write(response.getBytes());
			}
		}
	}

	private String validateXliff(JSONObject json) {
		// TODO Auto-generated method stub
		String file = json.getString("file");
		catalog = "";
		try {
			catalog = json.getString("catalog");
		} catch (JSONException je) {
			// do nothing
		}
		if (catalog.isEmpty()) {
			File catalogFolder = new File(new File(System.getProperty("user.dir")), "catalog");
			catalog = new File(catalogFolder, "catalog.xml").getAbsolutePath();
		}
		JSONObject result = new JSONObject();
		try {
			XliffChecker validator = new XliffChecker();
			boolean valid = validator.validate(file, catalog);
			result.put("valid", valid);
			if (valid) {
				String version = validator.getVersion();
				result.put("comment", "Selected file is valid XLIFF " + version);
			} else {
				String reason = validator.getReason();
				result.put("reason", reason);
			}
			result.put("status", "OK");
		} catch (IOException e) {
			result.put("status", "error");
			result.put("reason", e.getMessage());
		}
		return result.toString(2);
	}

	private static String getTargetFile(JSONObject json) {
		String file = json.getString("file");
		String target = "";
		try {
			target = Merge.getTargetFile(file);
		} catch (IOException | SAXException | ParserConfigurationException e) {
			LOGGER.log(Level.ERROR, "Error getting target file", e);
		}
		if (target.isEmpty()) {
			target = "Unknown";
		}
		JSONObject result = new JSONObject();
		result.put("target", target);
		return result.toString();
	}

	private static String getCharsets() {
		StringBuilder builder = new StringBuilder();
		builder.append("{\"charsets\": [\n");
		TreeMap<String, Charset> charsets = new TreeMap<>(Charset.availableCharsets());
		Set<String> keys = charsets.keySet();
		Iterator<String> i = keys.iterator();
		boolean first = true;
		while (i.hasNext()) {
			Charset cset = charsets.get(i.next());
			if (!first) {
				builder.append(",\n");
			} else {
				first = false;
			}
			builder.append("{\"code\":\"");
			builder.append(cset.name());
			builder.append("\", \"description\":\"");
			builder.append(cset.displayName());
			builder.append("\"}");
		}
		builder.append("]}\n");
		return builder.toString();
	}

	private String getStatus(JSONObject json) {
		String status = "unknown";
		try {
			String process = json.getString("process");
			status = running.get(process);
		} catch (JSONException je) {
			status = "error";
		}
		if (status == null) {
			status = "Error";
		}
		return "{\"status\": \"" + status + "\"}";
	}

	private String merge(JSONObject json) {
		xliff = "";
		try {
			xliff = json.getString("xliff");
		} catch (JSONException je) {
			// do nothing
		}
		target = "";
		try {
			target = json.getString("target");
		} catch (JSONException je) {
			// do nothing
		}
		catalog = "";
		try {
			catalog = json.getString("catalog");
		} catch (JSONException je) {
			// do nothing
		}
		if (catalog.isEmpty()) {
			File catalogFolder = new File(new File(System.getProperty("user.dir")), "catalog");
			catalog = new File(catalogFolder, "catalog.xml").getAbsolutePath();
		}
		unapproved = false;
		try {
			unapproved = json.getBoolean("unapproved");
		} catch (JSONException je) {
			// do nothing
		}
		String process = "" + System.currentTimeMillis();
		new Thread(new Runnable() {

			@Override
			public void run() {
				running.put(process,"running");
				try {
					Merge.merge(xliff, target, catalog, unapproved);
					if (running.get(process).equals(("running"))) {
						LOGGER.log(Level.INFO, "Merge completed");
						running.put(process,"completed");
					}
				} catch (IOException | SAXException | ParserConfigurationException e) {
					LOGGER.log(Level.ERROR, "Error merging file", e);
					running.put(process,e.getMessage());
				}
			}
		}).start();
		return "{\"process\":\"" + process + "\"}";
	}

	private String convert(JSONObject json) {
		String source = "";
		try {
			source = json.getString("file");
		} catch (JSONException je) {
			// do nothing
		}
		String srcLang = "";
		try {
			srcLang = json.getString("srcLang");
		} catch (JSONException je) {
			// do nothing
		}
		String tgtLang = "";
		try {
			tgtLang = json.getString("tgtLang");
		} catch (JSONException je) {
			// do nothing
		}
		xliff = source + ".xlf";
		try {
			xliff = json.getString("xliff");
		} catch (JSONException je) {
			// do nothing
		}
		String skl = source + ".skl";
		try {
			skl = json.getString("skl");
		} catch (JSONException je) {
			// do nothing
		}
		String type = "";
		try {
			type = json.getString("type");
			String fullName = FileFormats.getFullName(type);
			if (fullName != null) {
				type = fullName;
			}
		} catch (JSONException je) {
			// do nothing
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
		try {
			enc = json.getString("enc");
		} catch (JSONException je) {
			// do nothing
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
		try {
			srx = json.getString("srx");
		} catch (JSONException je) {
			// do nothing
		}
		if (srx.isEmpty()) {
			File srxFolder = new File(new File(System.getProperty("user.dir")), "srx");
			srx = new File(srxFolder, "default.srx").getAbsolutePath();
		}
		catalog = "";
		try {
			catalog = json.getString("catalog");
		} catch (JSONException je) {
			// do nothing
		}
		if (catalog.isEmpty()) {
			File catalogFolder = new File(new File(System.getProperty("user.dir")), "catalog");
			catalog = new File(catalogFolder, "catalog.xml").getAbsolutePath();
		}
		String ditaval = "";
		try {
			ditaval = json.getString("ditaval");
		} catch (JSONException je) {
			// do nothing
		}
		embed = false;
		try {
			embed = json.getBoolean("embed");
		} catch (JSONException je) {
			// do nothing
		}
		boolean paragraph = false;
		try {
			paragraph = json.getBoolean("paragraph");
		} catch (JSONException je) {
			// do nothing
		}
		is20 = false;
		try {
			is20 = json.getBoolean("is20");
		} catch (JSONException je) {
			// do nothing
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
				running.put(process,"running");
				Vector<String> result = Convert.run(params);
				if ("0".equals(result.get(0))) {
					if (embed) {
						try {
							Convert.addSkeleton(xliff, catalog);
						} catch (SAXException | IOException | ParserConfigurationException e) {
							LOGGER.log(Level.ERROR, "Error embedding skeleton", e);
							running.put(process,e.getMessage());
							is20 = false;
						}
					}
					if (is20) {
						result = ToXliff2.run(new File(xliff), catalog);
						if (!"0".equals(result.get(0))) {
							LOGGER.log(Level.ERROR,result.get(1));
							running.put(process,result.get(1));
						}
					}
				} else {
					LOGGER.log(Level.ERROR,result.get(1));
					running.put(process,result.get(1));
				}
				if (running.get(process).equals(("running"))) {
					LOGGER.log(Level.INFO, "Conversion completed");
					running.put(process,"completed");
				}
			}
		}).start();
		return "{\"process\":\"" + process + "\"}";
	}

	private static String readRequestBody(InputStream is) throws IOException {
		StringBuilder request = new StringBuilder();
		try (BufferedReader rd = new BufferedReader(new InputStreamReader(is))) {
			String line;
			while ((line = rd.readLine()) != null) {
				request.append(line);
			}
		}
		return request.toString();
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
		List<Language> languages = Language.getCommonLanguages();
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

}
