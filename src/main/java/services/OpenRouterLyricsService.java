package services;

import entities.Musique;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenRouterLyricsService {
    private static final String DEFAULT_MODEL = "inclusionai/ling-2.6-1t:free";
    private static final String ENDPOINT = "https://openrouter.ai/api/v1/chat/completions";
    private static final String DEFAULT_CONFIG_PATH = "config/openrouter.properties";
    private static final Pattern CONTENT_PATTERN = Pattern.compile("\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.DOTALL);
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("\"message\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.DOTALL);

    private final HttpClient httpClient;

    public OpenRouterLyricsService() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build());
    }

    public OpenRouterLyricsService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String generateLyrics(Musique track) {
        String apiKey = resolveApiKey();
        String model = resolveModel();
        String prompt = buildPrompt(track);
        String payload = buildRequestBody(model, prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", "http://localhost")
                .header("X-Title", "Esprit PIDEV Artium")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La génération des paroles a été interrompue.", e);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de contacter OpenRouter.", e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(extractErrorMessage(response.body(), response.statusCode()));
        }

        String content = extractContent(response.body());
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("OpenRouter n'a pas renvoyé de paroles.");
        }
        return content.trim();
    }

    private String resolveApiKey() {
        Properties config = loadConfig();

        String apiKey = config.getProperty("openrouter.apiKey");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getProperty("openrouter.apiKey");
        }
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("OPENROUTER_API_KEY");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Clé OpenRouter manquante. Ajoutez-la dans config/openrouter.properties ou définissez OPENROUTER_API_KEY.");
        }
        return apiKey.trim();
    }

    private String resolveModel() {
        Properties config = loadConfig();

        String model = config.getProperty("openrouter.model");
        if (model == null || model.isBlank()) {
            model = System.getProperty("openrouter.model");
        }
        if (model == null || model.isBlank()) {
            model = System.getenv("OPENROUTER_MODEL");
        }
        if (model == null || model.isBlank()) {
            model = DEFAULT_MODEL;
        }
        return model.trim();
    }

    private Properties loadConfig() {
        Properties properties = new Properties();

        String overridePath = System.getProperty("openrouter.config");
        Path configPath = Paths.get(overridePath != null && !overridePath.isBlank() ? overridePath.trim() : DEFAULT_CONFIG_PATH);
        if (!Files.exists(configPath)) {
            return properties;
        }

        try (var inputStream = Files.newInputStream(configPath)) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire le fichier de configuration OpenRouter: " + configPath.toAbsolutePath(), e);
        }
        return properties;
    }

    private String buildPrompt(Musique track) {
        String title = safe(track != null ? track.getTitre() : null, "Morceau sans titre");
        String genre = safe(track != null ? track.getGenre() : null, "inconnu");
        String description = safe(track != null ? track.getDescription() : null, "Aucune description disponible.");

        return "Écris des paroles 100% originales en français, inspirées par ces informations de morceau. "
                + "Ne copie aucune chanson existante et n'essaie pas de reproduire des paroles connues. "
                + "Utilise une structure claire avec couplets et refrain, avec un ton cohérent avec le genre. "
                + "Retourne uniquement les paroles, sans explication ni mise en contexte.\n\n"
                + "Titre: " + title + "\n"
                + "Genre: " + genre + "\n"
                + "Description: " + description + "\n";
    }

    private String buildRequestBody(String model, String prompt) {
        return "{" +
                "\"model\":\"" + jsonEscape(model) + "\"," +
                "\"messages\":[" +
                "{\"role\":\"system\",\"content\":\"Tu es un assistant d'écriture de chansons. Génère uniquement des paroles originales et non protégées par le droit d'auteur.\"}," +
                "{\"role\":\"user\",\"content\":\"" + jsonEscape(prompt) + "\"}" +
                "]," +
                "\"temperature\":0.9," +
                "\"max_tokens\":700," +
                "\"top_p\":0.95," +
                "\"stream\":false" +
                "}";
    }

    private String extractContent(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }

        Matcher matcher = CONTENT_PATTERN.matcher(responseBody);
        while (matcher.find()) {
            String raw = matcher.group(1);
            String unescaped = unescapeJson(raw);
            if (unescaped != null && !unescaped.isBlank()) {
                return unescaped;
            }
        }
        return null;
    }

    private String extractErrorMessage(String responseBody, int statusCode) {
        if (responseBody != null && !responseBody.isBlank()) {
            Matcher messageMatcher = MESSAGE_PATTERN.matcher(responseBody);
            if (messageMatcher.find()) {
                String message = unescapeJson(messageMatcher.group(1));
                if (message != null && !message.isBlank()) {
                    return message;
                }
            }

            String content = extractContent(responseBody);
            if (content != null && !content.isBlank()) {
                return content;
            }
        }
        return "OpenRouter a retourné une erreur (HTTP " + statusCode + ").";
    }

    private String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length() + 16);
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (c < 0x20) {
                        builder.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        builder.append(c);
                    }
                }
            }
        }
        return builder.toString();
    }

    private String unescapeJson(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\' || i + 1 >= value.length()) {
                builder.append(c);
                continue;
            }

            char next = value.charAt(++i);
            switch (next) {
                case '"' -> builder.append('"');
                case '\\' -> builder.append('\\');
                case '/' -> builder.append('/');
                case 'b' -> builder.append('\b');
                case 'f' -> builder.append('\f');
                case 'n' -> builder.append('\n');
                case 'r' -> builder.append('\r');
                case 't' -> builder.append('\t');
                case 'u' -> {
                    if (i + 4 < value.length()) {
                        String hex = value.substring(i + 1, i + 5);
                        try {
                            builder.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        } catch (NumberFormatException ex) {
                            builder.append('\\').append('u').append(hex);
                            i += 4;
                        }
                    } else {
                        builder.append('\\').append('u');
                    }
                }
                default -> builder.append(next);
            }
        }
        return builder.toString();
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}


