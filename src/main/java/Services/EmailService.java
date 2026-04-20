package Services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Properties;

import javax.net.ssl.SSLSocketFactory;

public final class EmailService {

	private static final DateTimeFormatter EXPIRY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	private static final Path SMTP_CONFIG_PATH = Paths.get("smtp.properties");
	private static volatile Properties cachedFileConfig;

	private EmailService() {
	}

	public static void sendPasswordResetCode(String recipientEmail, String resetCode, LocalDateTime expiresAt) {
		String host = readConfig("smtp.host", "SMTP_HOST", "");
		String port = readConfig("smtp.port", "SMTP_PORT", "587");
		String username = readConfig("smtp.username", "SMTP_USERNAME", "");
		String password = readConfig("smtp.password", "SMTP_PASSWORD", "");
		String from = readConfig("smtp.from", "SMTP_FROM", username);
		boolean startTls = Boolean.parseBoolean(readConfig("smtp.starttls", "SMTP_STARTTLS", "true"));
		boolean ssl = Boolean.parseBoolean(readConfig("smtp.ssl", "SMTP_SSL", "false"));

		if (host.isBlank()) {
			throw new IllegalStateException("Configuration SMTP manquante: smtp.host / SMTP_HOST ou fichier smtp.properties.");
		}
		if (from.isBlank()) {
			throw new IllegalStateException("Configuration SMTP manquante: smtp.from / SMTP_FROM ou fichier smtp.properties.");
		}

		String subject = "Artium - code de reinitialisation";
		String body = "Bonjour,\n\n"
				+ "Votre code de reinitialisation est : " + resetCode + "\n"
				+ "Ce code expire le : " + expiresAt.format(EXPIRY_FORMAT) + "\n\n"
				+ "Si vous n'êtes pas à l'origine de cette demande, ignorez cet e-mail.";

		Socket socket = null;
		BufferedReader reader = null;
		BufferedWriter writer = null;

		try {
			socket = openSocket(host, port, ssl);
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
			writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII));

			expectCode(readResponse(reader), 220);
			sendCommand(writer, reader, "EHLO localhost", 250);

			if (startTls && !ssl) {
				sendCommand(writer, reader, "STARTTLS", 220);
				socket = upgradeToTls(socket, host, port);
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
				writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII));
				sendCommand(writer, reader, "EHLO localhost", 250);
			}

			if (!username.isBlank() && !password.isBlank()) {
				sendCommand(writer, reader, "AUTH LOGIN", 334);
				sendCommand(writer, reader, base64(username), 334);
				sendCommand(writer, reader, base64(password), 235);
			}

			sendCommand(writer, reader, "MAIL FROM:<" + from + ">", 250);
			sendCommand(writer, reader, "RCPT TO:<" + recipientEmail + ">", 250, 251);
			sendCommand(writer, reader, "DATA", 354);

			writer.write("From: " + from + "\r\n");
			writer.write("To: " + recipientEmail + "\r\n");
			writer.write("Subject: " + subject + "\r\n");
			writer.write("Date: " + DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneId.systemDefault())) + "\r\n");
			writer.write("MIME-Version: 1.0\r\n");
			writer.write("Content-Type: text/plain; charset=UTF-8\r\n");
			writer.write("Content-Transfer-Encoding: 8bit\r\n");
			writer.write("\r\n");
			writer.write(body.replace("\n", "\r\n"));
			writer.write("\r\n.\r\n");
			writer.flush();

			expectCode(readResponse(reader), 250);
			sendCommand(writer, reader, "QUIT", 221);
		} catch (IOException e) {
			throw new IllegalStateException("Envoi de l'e-mail impossible: " + e.getMessage(), e);
		} finally {
			closeQuietly(writer);
			closeQuietly(reader);
			closeQuietly(socket);
		}
	}

	private static Socket openSocket(String host, String port, boolean ssl) throws IOException {
		int parsedPort;
		try {
			parsedPort = Integer.parseInt(port);
		} catch (NumberFormatException e) {
			throw new IOException("Port SMTP invalide: " + port, e);
		}
		if (ssl) {
			return SSLSocketFactory.getDefault().createSocket(host, parsedPort);
		}
		return new Socket(host, parsedPort);
	}

	private static Socket upgradeToTls(Socket socket, String host, String port) throws IOException {
		int parsedPort = Integer.parseInt(port);
		return ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(socket, host, parsedPort, true);
	}

	private static void sendCommand(BufferedWriter writer, BufferedReader reader, String command, int... expectedCodes) throws IOException {
		writer.write(command);
		writer.write("\r\n");
		writer.flush();
		expectCode(readResponse(reader), expectedCodes);
	}

	private static String readResponse(BufferedReader reader) throws IOException {
		String line = null;
		String current;
		do {
			current = reader.readLine();
			if (current == null) {
				break;
			}
			line = current;
		} while (current.length() >= 4 && current.charAt(3) == '-');

		if (line == null) {
			throw new IOException("Réponse SMTP vide.");
		}
		return line;
	}

	private static void expectCode(String response, int... expectedCodes) throws IOException {
		int code = parseCode(response);
		for (int expected : expectedCodes) {
			if (code == expected) {
				return;
			}
		}
		throw new IOException("Réponse SMTP inattendue: " + response);
	}

	private static int parseCode(String response) throws IOException {
		if (response == null || response.length() < 3) {
			throw new IOException("Réponse SMTP invalide.");
		}
		return Integer.parseInt(response.substring(0, 3));
	}

	private static String base64(String value) {
		return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	private static String readConfig(String propertyName, String envName, String defaultValue) {
		String value = System.getProperty(propertyName);
		if (value == null || value.isBlank()) {
			value = System.getenv(envName);
		}
		if (value == null || value.isBlank()) {
			value = loadFileConfig().getProperty(propertyName);
		}
		return value == null || value.isBlank() ? defaultValue : value.trim();
	}

	private static Properties loadFileConfig() {
		Properties snapshot = cachedFileConfig;
		if (snapshot != null) {
			return snapshot;
		}

		synchronized (EmailService.class) {
			snapshot = cachedFileConfig;
			if (snapshot != null) {
				return snapshot;
			}

			Properties properties = new Properties();
			if (Files.exists(SMTP_CONFIG_PATH)) {
				try (FileInputStream input = new FileInputStream(SMTP_CONFIG_PATH.toFile())) {
					properties.load(input);
				} catch (IOException ignored) {
					// Ignore and fall back to env/system properties.
				}
			}
			cachedFileConfig = properties;
			return properties;
		}
	}

	private static void closeQuietly(AutoCloseable closeable) {
		if (closeable == null) {
			return;
		}
		try {
			closeable.close();
		} catch (Exception ignored) {
		}
	}
}









