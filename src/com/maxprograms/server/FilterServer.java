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

import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetSocketAddress;
import java.nio.file.Files;

import com.maxprograms.converters.Constants;
import com.sun.net.httpserver.HttpServer;

public class FilterServer implements IServer {

	private static Logger LOGGER = System.getLogger(FilterServer.class.getName());

	private HttpServer server;
	private File workDir;

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
		server = HttpServer.create(new InetSocketAddress(port), 0);
		XliffHandler handler = new XliffHandler(this);
		server.createContext("/FilterServer", handler);
		server.setExecutor(null); // creates a default executor
	}

	public void run() {
		server.start();
		LOGGER.log(Level.INFO, "OpenXLIFF Server started");
	}

	@Override
	public void stop() {
		server.removeContext("/FilterServer");
		LOGGER.log(Level.INFO, "OpenXLIFF Server closed");
		System.exit(0);
	}

	@Override
	public File getWorkFolder() throws IOException {
		if (workDir == null) {
			String os = System.getProperty("os.name").toLowerCase();
			if (os.startsWith("mac")) {
				workDir = new File(System.getProperty("user.home") + "/Library/Application Support/OpenXLIFF/");
			} else if (os.startsWith("windows")) {
				workDir = new File(System.getenv("AppData") + "\\OpenXLIFF\\");
			} else {
				workDir = new File(System.getProperty("user.home") + "/.openxliff/");
			}
			if (!workDir.exists()) {
				Files.createDirectories(workDir.toPath());
			}
		}
		return workDir;
	}
}
