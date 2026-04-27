package services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import entities.Evenement;
import entities.Ticket;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.imageio.ImageIO;

public class TicketPdfService {

    private static final PDRectangle TICKET_PAGE = new PDRectangle(760, 320);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("0.##");
    private static final String LOGO_RESOURCE = "/views/assets/PNG Icon.png";
    private static final String WINDOWS_ARIAL = "C:/Windows/Fonts/arial.ttf";
    private static final String WINDOWS_ARIAL_BOLD = "C:/Windows/Fonts/arialbd.ttf";
    private static final int QR_SIZE = 280;
    // Configure with env vars or JVM props:
    // QR_API_URL / -Dqr.api.url
    // QR_API_KEY / -Dqr.api.key
    // QR_API_HOST / -Dqr.api.host
    // QR_API_LOGO_URL / -Dqr.api.logoUrl
    private static final String QR_API_URL_PROP = "qr.api.url";
    private static final String QR_API_KEY_PROP = "qr.api.key";
    private static final String QR_API_HOST_PROP = "qr.api.host";
    private static final String QR_API_LOGO_PROP = "qr.api.logoUrl";
    private static final String QR_API_SIZE_PROP = "qr.api.size";
    private static final String QR_API_FORMAT_PROP = "qr.api.format";
    private static final String QR_API_ECC_PROP = "qr.api.ecc";
    private static final String QR_API_MARGIN_PROP = "qr.api.margin";
    private static final String QR_API_QZONE_PROP = "qr.api.qzone";
    private static final String QR_API_COLOR_PROP = "qr.api.color";
    private static final String QR_API_BGCOLOR_PROP = "qr.api.bgcolor";
    private static final String QR_API_CHARSET_SOURCE_PROP = "qr.api.charsetSource";
    private static final String QR_API_CHARSET_TARGET_PROP = "qr.api.charsetTarget";
    private static final String QR_API_CONFIG_PATH_PROP = "qr.api.configPath";
    private static final String QR_API_STRICT_PROP = "qr.api.strict";
    private static final String QR_API_DEBUG_PROP = "qr.api.debug";
    private static final String QR_API_URL_ENV = "QR_API_URL";
    private static final String QR_API_KEY_ENV = "QR_API_KEY";
    private static final String QR_API_HOST_ENV = "QR_API_HOST";
    private static final String QR_API_LOGO_ENV = "QR_API_LOGO_URL";
    private static final String QR_API_SIZE_ENV = "QR_API_SIZE";
    private static final String QR_API_FORMAT_ENV = "QR_API_FORMAT";
    private static final String QR_API_ECC_ENV = "QR_API_ECC";
    private static final String QR_API_MARGIN_ENV = "QR_API_MARGIN";
    private static final String QR_API_QZONE_ENV = "QR_API_QZONE";
    private static final String QR_API_COLOR_ENV = "QR_API_COLOR";
    private static final String QR_API_BGCOLOR_ENV = "QR_API_BGCOLOR";
    private static final String QR_API_CHARSET_SOURCE_ENV = "QR_API_CHARSET_SOURCE";
    private static final String QR_API_CHARSET_TARGET_ENV = "QR_API_CHARSET_TARGET";
    private static final String QR_API_CONFIG_PATH_ENV = "QR_API_CONFIG_PATH";
    private static final String QR_API_STRICT_ENV = "QR_API_STRICT";
    private static final String QR_API_DEBUG_ENV = "QR_API_DEBUG";
    private static final String LOCAL_QR_CONFIG_FILE = "config/qr-api.local.properties";

    public File createPreviewPdf(Evenement event, Ticket ticket) throws IOException {
        File preview = Files.createTempFile("ticket-preview-", ".pdf").toFile();
        preview.deleteOnExit();
        exportTicketPdf(event, ticket, preview);
        return preview;
    }

    public File exportTicketPdf(Evenement event, Ticket ticket, File destination) throws IOException {
        validate(event, ticket, destination);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(TICKET_PAGE);
            document.addPage(page);

            PDFont regular = loadFont(document, WINDOWS_ARIAL, PDType1Font.HELVETICA);
            PDFont bold = loadFont(document, WINDOWS_ARIAL_BOLD, PDType1Font.HELVETICA_BOLD);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                drawTicket(document, content, event, ticket, regular, bold);
            }

            document.save(destination);
        }

        return destination;
    }

    public void openPdf(File pdfFile) throws IOException {
        if (pdfFile == null || !pdfFile.exists()) {
            throw new IOException("Le fichier PDF est introuvable.");
        }

        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.OPEN)) {
                desktop.open(pdfFile);
                return;
            }
        }

        throw new IOException("L'ouverture automatique du PDF n'est pas supportée sur cette machine.");
    }

    public String buildSuggestedFileName(Evenement event, Ticket ticket) {
        String title = safeFileToken(event == null ? null : event.getTitre(), "ticket");
        String reference = safeFileToken(resolveReference(ticket), "ref");
        return "ticket-" + title + "-" + reference + ".pdf";
    }

    private void drawTicket(PDDocument document,
                            PDPageContentStream content,
                            Evenement event,
                            Ticket ticket,
                            PDFont regular,
                            PDFont bold) throws IOException {
        float pageWidth = TICKET_PAGE.getWidth();
        float pageHeight = TICKET_PAGE.getHeight();

        drawRoundedCard(content, 18, 18, pageWidth - 36, pageHeight - 36, 18);

        // Header band
        content.setNonStrokingColor(new java.awt.Color(61, 59, 194));
        content.addRect(18, pageHeight - 86, pageWidth - 36, 68);
        content.fill();

        // Logo circle
        PDImageXObject logo = loadLogo(document);
        if (logo != null) {
            content.drawImage(logo, 34, pageHeight - 76, 44, 44);
        } else {
            content.setNonStrokingColor(java.awt.Color.WHITE);
            content.addRect(34, pageHeight - 76, 44, 44);
            content.fill();
        }

        writeText(content, bold, 92, pageHeight - 50, 22, "ARTIUM");
        writeText(content, regular, 92, pageHeight - 68, 9, "Ticket officiel / prêt à présenter");

        String status = textOrDefault(event.getStatut(), "A venir");
        drawChip(content, pageWidth - 190, pageHeight - 61, 140, 24, status.toUpperCase(), regular);

        // Left block
        float leftX = 34;
        float leftY = pageHeight - 118;

        writeText(content, bold, leftX, leftY, 20, textOrDefault(event.getTitre(), "Evenement"));
        writeText(content, regular, leftX, leftY - 20, 11, textOrDefault(event.getType(), "Type non precise") + "  •  Galerie #" + textOrDash(event.getGalerieId()));
        writeText(content, regular, leftX, leftY - 38, 10, textOrDefault(event.getDescription(), "Aucune description disponible."), 430);

        float labelY = leftY - 78;
        writeKeyValue(content, regular, bold, leftX, labelY, "Début", formatDateTime(event.getDateDebut()));
        writeKeyValue(content, regular, bold, leftX, labelY - 22, "Fin", formatDateTime(event.getDateFin()));
        writeKeyValue(content, regular, bold, leftX, labelY - 44, "Prix", formatPrice(event.getPrixTicket()));
        writeKeyValue(content, regular, bold, leftX, labelY - 66, "Capacité", formatCapacity(event.getCapaciteMax()));

        // Right block
        float rightX = 540;
        writeText(content, bold, rightX, pageHeight - 118, 12, "Ticket #" + resolveReference(ticket));
        writeText(content, regular, rightX, pageHeight - 136, 10, "Référence : " + resolveReference(ticket));
        writeText(content, regular, rightX, pageHeight - 152, 10, "Utilisateur : " + textOrDash(ticket.getUserId()));
        writeText(content, regular, rightX, pageHeight - 168, 10, "Achat : " + formatDate(ticket.getDateAchat()));
        writeText(content, regular, rightX, pageHeight - 184, 10, "Évènement : " + textOrDash(ticket.getEvenementId()));

        BufferedImage qrImage = createQrImage(resolveQrPayload(ticket));
        if (qrImage != null) {
            PDImageXObject qr = LosslessFactory.createFromImage(document, qrImage);
            content.drawImage(qr, rightX - 2, 56, 152, 152);
        }

        writeText(content, bold, rightX + 4, 42, 9, "Scan QR / vérification rapide");
        writeText(content, regular, rightX + 4, 28, 8, resolveQrPayload(ticket), 150);

        // Footer
        content.setStrokingColor(new java.awt.Color(214, 223, 238));
        content.moveTo(34, 44);
        content.lineTo(pageWidth - 34, 44);
        content.stroke();
        writeText(content, regular, 34, 24, 8, "Merci pour votre achat. Présentez ce ticket à l'entrée ou enregistrez le PDF sur votre appareil.");
    }

    private void drawRoundedCard(PDPageContentStream content, float x, float y, float width, float height, float radius) throws IOException {
        content.setNonStrokingColor(255, 255, 255);
        content.addRect(x, y, width, height);
        content.fill();
        content.setStrokingColor(214, 223, 238);
        content.addRect(x, y, width, height);
        content.stroke();
    }

    private void drawChip(PDPageContentStream content, float x, float y, float width, float height, String text, PDFont font) throws IOException {
        content.setNonStrokingColor(new java.awt.Color(232, 248, 243));
        content.addRect(x, y, width, height);
        content.fill();
        content.setStrokingColor(new java.awt.Color(199, 235, 223));
        content.addRect(x, y, width, height);
        content.stroke();

        float textWidth = (font.getStringWidth(text) / 1000f) * 9;
        float textX = x + Math.max(10, (width - textWidth) / 2);
        writeText(content, font, textX, y + 8, 9, text);
    }

    private void writeKeyValue(PDPageContentStream content, PDFont labelFont, PDFont valueFont, float x, float y, String label, String value) throws IOException {
        writeText(content, labelFont, x, y, 10, label + ":");
        writeText(content, valueFont, x + 68, y, 10, value);
    }

    private void writeText(PDPageContentStream content, PDFont font, float x, float y, float size, String text) throws IOException {
        writeText(content, font, x, y, size, text, Float.MAX_VALUE);
    }

    private void writeText(PDPageContentStream content, PDFont font, float x, float y, float size, String text, float maxWidth) throws IOException {
        if (text == null) {
            return;
        }

        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);

        for (String line : wrapText(font, size, text, maxWidth)) {
            content.showText(line);
            content.newLineAtOffset(0, -size - 3);
        }

        content.endText();
    }

    private String[] wrapText(PDFont font, float size, String text, float maxWidth) throws IOException {
        if (maxWidth == Float.MAX_VALUE) {
            return new String[]{text};
        }

        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        java.util.List<String> lines = new java.util.ArrayList<>();

        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            float width = (font.getStringWidth(candidate) / 1000f) * size;
            if (width <= maxWidth || line.isEmpty()) {
                line.setLength(0);
                line.append(candidate);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }

        if (!line.isEmpty()) {
            lines.add(line.toString());
        }

        return lines.toArray(new String[0]);
    }

    private BufferedImage createQrImage(String payload) throws IOException {
        try {
            BufferedImage image = createQrImageFromApi(payload);
            debug("QR API used successfully.");
            return image;
        } catch (IOException apiFailure) {
            if (isApiStrict()) {
                throw new IOException("QR API failed in strict mode: " + apiFailure.getMessage(), apiFailure);
            }
            // Keep PDF export reliable even when API config/network/provider fails.
            debug("QR API failed, fallback ZXing used: " + apiFailure.getMessage());
            return createQrImageWithZxing(payload);
        }
    }

    private BufferedImage createQrImageFromApi(String payload) throws IOException {
        String endpoint = readConfig(QR_API_URL_PROP, QR_API_URL_ENV);
        if (endpoint.isBlank()) {
            throw new IOException("QR API non configuree (QR_API_URL ou -D" + QR_API_URL_PROP + ").");
        }
        debug("QR endpoint: " + endpoint);

        if (isQrServerEndpoint(endpoint)) {
            debug("QR provider detected: qrserver (GET)");
            return createQrImageFromQrServer(endpoint, payload);
        }
        debug("QR provider detected: generic/RapidAPI (POST)");

        String apiKey = readConfig(QR_API_KEY_PROP, QR_API_KEY_ENV);
        String apiHost = readConfig(QR_API_HOST_PROP, QR_API_HOST_ENV);
        String logoUrl = readConfig(QR_API_LOGO_PROP, QR_API_LOGO_ENV);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(12))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildQrApiBody(payload, logoUrl)));

        if (!apiKey.isBlank()) {
            requestBuilder.header("X-RapidAPI-Key", apiKey);
        }
        if (!apiHost.isBlank()) {
            requestBuilder.header("X-RapidAPI-Host", apiHost);
        }

        HttpResponse<byte[]> response;
        try {
            response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Appel API QR interrompu.", e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("API QR en echec: HTTP " + response.statusCode());
        }

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.body()));
        if (image == null) {
            throw new IOException("Reponse API QR invalide (image illisible).");
        }

        return toArgbImage(image);
    }

    private BufferedImage createQrImageFromQrServer(String endpoint, String payload) throws IOException {
        String requestUrl = buildQrServerUrl(endpoint, payload);
        debug("QR request URL: " + requestUrl);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(requestUrl))
                .timeout(Duration.ofSeconds(12))
                .GET()
                .build();

        HttpResponse<byte[]> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Appel API QR interrompu.", e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("API QR en echec: HTTP " + response.statusCode());
        }

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.body()));
        if (image == null) {
            throw new IOException("Reponse API QR invalide (image illisible).");
        }

        return toArgbImage(image);
    }

    private boolean isQrServerEndpoint(String endpoint) {
        String normalized = endpoint.toLowerCase();
        return normalized.contains("api.qrserver.com") && normalized.contains("create-qr-code");
    }

    private String buildQrServerUrl(String endpoint, String payload) {
        String size = readConfig(QR_API_SIZE_PROP, QR_API_SIZE_ENV);
        if (size.isBlank()) {
            size = QR_SIZE + "x" + QR_SIZE;
        }

        String format = readConfig(QR_API_FORMAT_PROP, QR_API_FORMAT_ENV);
        if (format.isBlank()) {
            format = "png";
        }

        String ecc = readConfig(QR_API_ECC_PROP, QR_API_ECC_ENV);
        String margin = readConfig(QR_API_MARGIN_PROP, QR_API_MARGIN_ENV);
        String qzone = readConfig(QR_API_QZONE_PROP, QR_API_QZONE_ENV);
        String color = readConfig(QR_API_COLOR_PROP, QR_API_COLOR_ENV);
        String bgColor = readConfig(QR_API_BGCOLOR_PROP, QR_API_BGCOLOR_ENV);
        String charsetSource = readConfig(QR_API_CHARSET_SOURCE_PROP, QR_API_CHARSET_SOURCE_ENV);
        String charsetTarget = readConfig(QR_API_CHARSET_TARGET_PROP, QR_API_CHARSET_TARGET_ENV);

        StringBuilder url = new StringBuilder(endpoint);
        char separator = endpoint.contains("?") ? '&' : '?';

        separator = appendQueryParam(url, separator, "data", payload);
        separator = appendQueryParam(url, separator, "size", size);
        separator = appendQueryParam(url, separator, "format", format);
        separator = appendQueryParam(url, separator, "ecc", ecc);
        separator = appendQueryParam(url, separator, "margin", margin);
        separator = appendQueryParam(url, separator, "qzone", qzone);
        separator = appendQueryParam(url, separator, "color", color);
        separator = appendQueryParam(url, separator, "bgcolor", bgColor);
        separator = appendQueryParam(url, separator, "charset-source", charsetSource);
        appendQueryParam(url, separator, "charset-target", charsetTarget);

        return url.toString();
    }

    private char appendQueryParam(StringBuilder url, char separator, String key, String value) {
        if (value == null || value.isBlank()) {
            return separator;
        }

        url.append(separator)
                .append(key)
                .append('=')
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));

        return '&';
    }

    private BufferedImage createQrImageWithZxing(String payload) throws IOException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);

        try {
            BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
            return MatrixToImageWriter.toBufferedImage(matrix);
        } catch (WriterException e) {
            throw new IOException("Impossible de générer le QR code du ticket.", e);
        }
    }

    private BufferedImage toArgbImage(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_ARGB || source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }

        BufferedImage converted = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = converted.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return converted;
    }

    private String buildQrApiBody(String payload, String logoUrl) {
        // If your provider expects different field names, adjust this JSON only.
        StringBuilder body = new StringBuilder();
        body.append('{')
                .append("\"data\":\"").append(escapeJson(payload)).append("\",")
                .append("\"size\":").append(QR_SIZE).append(',')
                .append("\"format\":\"png\"");

        if (!logoUrl.isBlank()) {
            body.append(',').append("\"logo\":\"").append(escapeJson(logoUrl)).append("\"");
        }

        body.append('}');
        return body.toString();
    }

    private String readConfig(String propName, String envName) {
        String byFile = readFromLocalConfig(propName);
        if (!byFile.isBlank()) {
            return byFile;
        }

        String byProp = System.getProperty(propName);
        if (byProp != null && !byProp.isBlank()) {
            return byProp.trim();
        }
        String byEnv = System.getenv(envName);
        if (byEnv != null && !byEnv.isBlank()) {
            return byEnv.trim();
        }
        return "";
    }

    private String readFromLocalConfig(String key) {
        Properties config = loadLocalQrConfig();
        String value = config.getProperty(key);
        return value == null ? "" : value.trim();
    }

    private Properties loadLocalQrConfig() {
        Properties loaded = new Properties();
        Path configPath = resolveLocalConfigPath();
        if (Files.exists(configPath) && Files.isRegularFile(configPath)) {
            try (InputStream in = Files.newInputStream(configPath)) {
                loaded.load(in);
            } catch (IOException ignored) {
                // Fallback to JVM/env config when local file is missing or invalid.
            }
        }

        return loaded;
    }

    private Path resolveLocalConfigPath() {
        String customPath = firstNonBlank(
                System.getProperty(QR_API_CONFIG_PATH_PROP),
                System.getenv(QR_API_CONFIG_PATH_ENV)
        );
        if (!customPath.isBlank()) {
            return Paths.get(customPath.trim());
        }

        Path defaultPath = Paths.get(LOCAL_QR_CONFIG_FILE);
        if (defaultPath.isAbsolute()) {
            return defaultPath;
        }
        return Paths.get(System.getProperty("user.dir")).resolve(defaultPath).normalize();
    }

    private boolean isApiStrict() {
        String raw = readConfig(QR_API_STRICT_PROP, QR_API_STRICT_ENV);
        return "true".equalsIgnoreCase(raw) || "1".equals(raw);
    }

    private boolean isApiDebugEnabled() {
        String raw = readConfig(QR_API_DEBUG_PROP, QR_API_DEBUG_ENV);
        return "true".equalsIgnoreCase(raw) || "1".equals(raw);
    }

    private void debug(String message) {
        if (isApiDebugEnabled()) {
            System.out.println("[TicketPdfService] " + message);
        }
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return "";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private PDImageXObject loadLogo(PDDocument document) {
        try (InputStream inputStream = getClass().getResourceAsStream(LOGO_RESOURCE)) {
            if (inputStream == null) {
                return null;
            }
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                return null;
            }
            return LosslessFactory.createFromImage(document, image);
        } catch (IOException e) {
            return null;
        }
    }

    private PDFont loadFont(PDDocument document, String windowsPath, PDFont fallback) {
        File file = new File(windowsPath);
        if (file.exists()) {
            try {
                return PDType0Font.load(document, file);
            } catch (IOException ignored) {
                // fallback below
            }
        }
        return fallback;
    }

    private void validate(Evenement event, Ticket ticket, File destination) throws IOException {
        if (event == null) {
            throw new IOException("L'évènement est introuvable.");
        }
        if (ticket == null) {
            throw new IOException("Le ticket est introuvable.");
        }
        if (destination == null) {
            throw new IOException("La destination du PDF est introuvable.");
        }
        File parent = destination.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Impossible de créer le dossier de destination.");
        }
    }

    private String resolveQrPayload(Ticket ticket) {
        if (ticket == null || ticket.getCodeQr() == null || ticket.getCodeQr().isBlank()) {
            return "QR non disponible";
        }
        return ticket.getCodeQr();
    }

    private String resolveReference(Ticket ticket) {
        if (ticket == null || ticket.getCodeQr() == null || ticket.getCodeQr().isBlank()) {
            return "-";
        }
        String payload = ticket.getCodeQr();
        int refIndex = payload.indexOf("|ref=");
        if (refIndex >= 0 && refIndex + 5 < payload.length()) {
            return payload.substring(refIndex + 5);
        }
        return payload;
    }

    private String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String textOrDash(Integer value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "Date non définie" : DATE_TIME_FORMATTER.format(value);
    }

    private String formatDate(LocalDate value) {
        return value == null ? "-" : DATE_FORMATTER.format(value);
    }

    private String formatPrice(Double value) {
        return value == null ? "Prix non défini" : PRICE_FORMAT.format(value) + " TND";
    }

    private String formatCapacity(Integer value) {
        return value == null ? "Capacité non définie" : value + " personnes";
    }

    private String safeFileToken(String value, String fallback) {
        String token = value == null ? fallback : value.trim().toLowerCase();
        token = token.replaceAll("[^a-z0-9]+", "-");
        token = token.replaceAll("^-+|-+$", "");
        return token.isBlank() ? fallback : token;
    }
}


