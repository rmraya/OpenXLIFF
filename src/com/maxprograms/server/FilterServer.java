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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
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
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class FilterServer implements HttpHandler {

	private static Logger LOGGER = System.getLogger(FilterServer.class.getName());

	HttpServer server;

	private Vector<String> running;

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
		running = new Vector<>();
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
		String command = "";
		try {
			if (!request.isBlank()) {
				json = new JSONObject(request);
				command = json.getString("command");
			}
			if (command.isEmpty()) {
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
			if (command.equals("getTypes") || uri.toString().endsWith("/getTypes")) {
				response = getTypes();
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

	private String getStatus(JSONObject json) {
		String status = "completed";
		try {
			String process = json.getString("process");
			if (running.contains(process)) {
				status = "running";
			}
		} catch (JSONException je) {
			status = "error";
		}
		return "{\"status\": \"" + status + "\"}";
	}

	private String merge(JSONObject json) {
		Vector<String> params = new Vector<>();
		params.add("-xliff");
		params.add(json.getString("xliff"));
		params.add("-target");
		params.add(json.getString("target"));
		// optional parameters:
		try {
			String catalog = json.getString("catalog");
			params.add("-catalog");
			params.add(catalog);
		} catch (JSONException je) {
			// do nothing
		}
		try {
			boolean unapproved = json.getBoolean("unapproved");
			if (unapproved) {
				params.add("-unapproved");
			}
		} catch (JSONException je) {
			// do nothing
		}
		String process = "" + System.currentTimeMillis();
		new Thread(new Runnable() {

			@Override
			public void run() {
				running.addElement(process);
				Merge.main(params.toArray(new String[params.size()]));
				running.remove(process);
			}
		}).start();
		return "{\"process\":\"" + process + "\"}";
	}

	private String convert(JSONObject json) {
		Vector<String> params = new Vector<>();
		params.add("-file");
		params.add(json.getString("file"));
		params.add("-srcLang");
		params.add(json.getString("srcLang"));
		// optional parameters:
		try {
			String tgtLang = json.getString("tgtLang");
			params.add("-tgtLang");
			params.add(tgtLang);
		} catch (JSONException je) {
			// do nothing
		}
		try {
			String xliff = json.getString("xliff");
			params.add("-xliff");
			params.add(xliff);
		} catch (JSONException je) {
			// do nothing
		}
		try {
			String skl = json.getString("skl");
			params.add("-skl");
			params.add(skl);
		} catch (JSONException je) {
			// do nothing
		}
		try {
			String type = json.getString("type");
			params.add("-type");
			params.add(type);
		} catch (JSONException je) {
			// do nothing
		}
		try {
			String enc = json.getString("enc");
			params.add("-enc");
			params.add(enc);
		} catch (JSONException je) {
			// do nothing
		}
		try {
			String srx = json.getString("srx");
			params.add("-srx");
			params.add(srx);
		} catch (JSONException je) {
			// do nothing
		}
		try {
			String catalog = json.getString("catalog");
			params.add("-catalog");
			params.add(catalog);
		} catch (JSONException je) {
			// do nothing
		}
		try {
			String ditaval = json.getString("ditaval");
			params.add("-ditaval");
			params.add(ditaval);
		} catch (JSONException je) {
			// do nothing
		}
		try {
			boolean embed = json.getBoolean("embed");
			if (embed) {
				params.add("-embed");
			}
		} catch (JSONException je) {
			// do nothing
		}
		try {
			boolean paragraph = json.getBoolean("paragraph");
			if (paragraph) {
				params.add("-paragraph");
			}
		} catch (JSONException je) {
			// do nothing
		}
		try {
			boolean is20 = json.getBoolean("20");
			if (is20) {
				params.add("-2.0");
			}
		} catch (JSONException je) {
			// do nothing
		}
		String process = "" + System.currentTimeMillis();
		new Thread(new Runnable() {

			@Override
			public void run() {
				running.addElement(process);
				Convert.main(params.toArray(new String[params.size()]));
				running.remove(process);
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
