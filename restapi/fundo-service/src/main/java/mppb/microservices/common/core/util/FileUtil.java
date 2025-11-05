package mppb.microservices.common.core.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;

public final class FileUtil {

	private FileUtil() {
		throw new UnsupportedOperationException("Utility class");
	}

	public static String readStream(InputStream stream, String newline) throws IOException {
		try (stream; var reader = new BufferedReader(new InputStreamReader(stream))) {
			return readReader(reader, newline);
		}
	}

	public static String readReader(BufferedReader reader, String newline) throws IOException {
		var sb = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			sb.append(line).append(newline);
		}

		// remove \n \r finais
		var text = sb.toString().stripTrailing();
		return text;
	}

	public static String readUrl(URL url, String newline) throws IOException {
		try (var stream = url.openStream()) {
			return readStream(stream, newline);
		}
	}

	public static String readFile(File file, String newline) throws IOException {
		return Files.readString(file.toPath());
	}
}
