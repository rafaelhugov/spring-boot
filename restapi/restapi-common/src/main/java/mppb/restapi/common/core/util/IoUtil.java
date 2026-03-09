package mppb.restapi.common.core.util;


import mppb.restapi.common.core.error.AppException;
import mppb.restapi.common.core.error.NotFoundException;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

public class IoUtil {

	private static final String CLASSPATH_PREFIX = "classpath:";
	private static final String CONFIG_PROPERTIES = "config.properties";

	public static URL getResource(String path) {
		if (path.startsWith(CLASSPATH_PREFIX)) {
			return classpathResource(path.replace(CLASSPATH_PREFIX, ""));
		}
		try {
			return filesystemResource(path);
		} catch (MalformedURLException e) {
			throw new AppException(e);
		}
	}


	public static String getContentsFromURL(String urlString, String postParameters, String method,
											String proxyHost, int proxyPort, String usuarioProxy, String password) throws IOException {

		if (urlString.startsWith("https:")) {
			System.setProperty("https.protocols", "SSLv3");
		}

		var proxy = (proxyHost != null && !proxyHost.isBlank())
				? new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort))
				: Proxy.NO_PROXY;

		if (proxy != Proxy.NO_PROXY) {
			Authenticator.setDefault(new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(usuarioProxy, password.toCharArray());
				}
			});
		}

		var url = new URL(urlString);
		var conn = (HttpURLConnection) url.openConnection(proxy);
		conn.setRequestMethod(method);
		conn.setUseCaches(false);
		conn.setDoInput(true);
		conn.setDoOutput(true);

		try (var wr = new DataOutputStream(conn.getOutputStream())) {
			wr.writeBytes(postParameters);
		}

		try (var reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
			return FileUtil.readReader(reader, "\n");
		}
	}

	public static String resourceToString(String path) {
		return resourceToString(getResource(path));
	}

	private static URL classpathResource(String path) {
		if (!path.isEmpty() && path.charAt(0) == '/') {
			path = path.substring(1);
		}
		return Thread.currentThread().getContextClassLoader().getResource(path);
	}

	private static URL filesystemResource(String path) throws MalformedURLException {
		return new File(path).toURI().toURL();
	}

	public static String resourceToString(URL url) {
		return resourceToString(url, null);
	}

	public static String resourceToString(URL url, String newline) {
		return resourceToString(url, newline, null);
	}

	public static String resourceToString(URL url, String newline, String encoding) {
		try (var openStream = url.openStream();
			 var reader = encoding != null
					 ? new BufferedReader(new InputStreamReader(openStream, encoding))
					 : new BufferedReader(new InputStreamReader(openStream))) {

			var buf = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				buf.append(line);
				if (newline != null) {
					buf.append(newline);
				}
			}
			return buf.toString();

		} catch (IOException e) {
			throw new AppException(e);
		}
	}

	public static File lastModifiedFile(File directory) {
		return lastModifiedFile(directory, List.of());
	}

	public static File lastModifiedFile(File directory, List<String> extensoes) {
		checkDirectory(directory);
		var files = listFilteredFiles(directory, extensoes);

		if (files == null || files.length == 0) {
			return null;
		}

		File lastFile = null;
		long lastModifiedTime = Long.MIN_VALUE;
		for (var file : files) {
			if (file.lastModified() > lastModifiedTime) {
				lastFile = file;
				lastModifiedTime = file.lastModified();
			}
		}
		return lastFile;
	}

	public static File[] listFilteredFiles(File directory, List<String> extensions) {
		checkDirectory(directory);
		return directory.listFiles(file -> {
			if (extensions != null && !extensions.isEmpty() && !hasExtension(file, extensions)) {
				return false;
			}
			return file.isFile();
		});
	}

	public static boolean hasExtension(File file, List<String> extensions) {
		return extensions.stream().anyMatch(extension -> hasExtension(file, extension));
	}

	public static boolean hasExtension(File file, String extension) {
		if (extension == null || file == null) {
			return false;
		}
		return file.getName().toUpperCase().endsWith("."+extension.toUpperCase());
	}

	 public static void checkDirectory(File directory) {
		if (directory == null || !directory.isDirectory()) {
			throw new NotFoundException("This path must be a directory: '" + directory.getAbsolutePath() + "'.");
		}
	}

	public static File getFile(String path) {
		try {
			return new File(getResource(path).toURI());
		} catch (Exception e) {
			throw new NotFoundException(e);
		}
	}

	private static Properties loadProperties() {
		return loadProperties("/" + CONFIG_PROPERTIES);
	}

	private static Properties loadProperties(String pathToFile) {
		try (var in = IoUtil.class.getResourceAsStream(pathToFile)) {
			Objects.requireNonNull(in, "Properties file not found: " + pathToFile);
			var properties = new Properties();
			properties.load(in);
			return properties;
		} catch (IOException e) {
			throw new AppException(e);
		}
	}

	public static String getProperty(String property) {
		return loadProperties().getProperty(property);
	}

	public static String getPropertyFromFile(String property, String file) {
		return loadProperties(file).getProperty(property);
	}

	public static String getPropertyURLValidacaoDocumentos() {
		return getProperty("URL_VALIDACAO_DOCUMENTOS");
	}
}
