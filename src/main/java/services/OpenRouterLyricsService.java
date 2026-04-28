package services;

import entities.Musique;
import utils.LanguageDetector;
import utils.AudioAnalyzer;

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
import utils.TranscriptionService;

public class OpenRouterLyricsService {
    private static final String DEFAULT_MODEL = "llama-3.1-8b-instant";
    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
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
            String responseBody = response.body();
            System.err.println("API RAW RESPONSE: " + responseBody);
            throw new IllegalStateException("L'IA n'a pas renvoyé de paroles. Réponse: " + (responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody));
        }
        return content.trim();
    }

    private String resolveApiKey() {
        Properties config = loadConfig();

        String apiKey = config.getProperty("groq.apiKey");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getProperty("groq.apiKey");
        }
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("GROQ_API_KEY");
        }
        if (apiKey == null || apiKey.isBlank()) {
            // Fallback to openrouter key if groq is missing
            apiKey = config.getProperty("openrouter.apiKey");
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException("Clé API manquante. Ajoutez groq.apiKey dans config/openrouter.properties.");
            }
        }
        return apiKey.trim();
    }

    private String resolveModel() {
        Properties config = loadConfig();

        String model = config.getProperty("groq.model");
        if (model == null || model.isBlank()) {
            model = System.getProperty("groq.model");
        }
        if (model == null || model.isBlank()) {
            model = System.getenv("GROQ_MODEL");
        }
        if (model == null || model.isBlank()) {
            // Check if openrouter model is set, otherwise default
            model = config.getProperty("openrouter.model");
            if (model == null || model.isBlank() || model.contains("free")) {
                model = DEFAULT_MODEL;
            }
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
        String language = LanguageDetector.detectLanguage(track);

        // Analyze audio file to get features
        AudioAnalyzer.AudioFeatures audioFeatures = AudioAnalyzer.analyzeAudio(
            track != null ? track.getAudio() : null
        );

        return "You are a professional songwriter. Create original and engaging song lyrics in " + language + " based on these specifications:\n\n"
                + "SONG TITLE: " + title + "\n"
                + "GENRE: " + genre + "\n"
                + "THEME/DESCRIPTION: " + description + "\n"
                + "ESTIMATED BPM: " + audioFeatures.estimatedBpm + "\n"
                + "APPROXIMATE KEY: " + audioFeatures.approximateKey + "\n"
                + "ENERGY LEVEL: " + audioFeatures.energyLevel + "\n"
                + "MOOD: " + audioFeatures.mood + "\n\n"
                + "IMPORTANT INSTRUCTIONS:\n"
                + "1. MUST be directly inspired by and related to the theme described above\n"
                + "2. MUST follow the style and tone appropriate for " + genre + " music\n"
                + "3. MUST match the mood: " + audioFeatures.mood + "\n"
                + "4. MUST have a rhythm and pacing suitable for " + audioFeatures.estimatedBpm + " BPM\n"
                + "5. MUST include verses and a memorable chorus/refrain\n"
                + "6. MUST be emotionally resonant and match the song's energy level\n"
                + "7. Use vivid imagery and emotional language that reflects the theme\n"
                + "8. MUST be completely original - do not reproduce any existing songs\n"
                + "9. Return ONLY the lyrics without any explanation, intro, or metadata\n\n"
                + "Now write the lyrics:";
    }

    private String buildRequestBody(String model, String prompt) {
        return "{" +
                "\"model\":\"" + jsonEscape(model) + "\"," +
                "\"messages\":[" +
                "{\"role\":\"system\",\"content\":\"You are a song writing assistant. Generate only original and non-copyrighted lyrics.\"}," +
                "{\"role\":\"user\",\"content\":\"" + jsonEscape(prompt) + "\"}" +
                "]," +
                "\"temperature\":0.9," +
                "\"max_tokens\":1000," +
                "\"top_p\":0.95," +
                "\"stream\":false" +
                "}";
    }

    private String extractContent(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }

        // Use string search instead of regex to avoid StackOverflowError on huge text
        String searchStr = "\"content\":";
        int idx = responseBody.indexOf(searchStr);
        while (idx != -1) {
            int quoteStart = responseBody.indexOf("\"", idx + searchStr.length());
            if (quoteStart != -1) {
                int quoteEnd = responseBody.indexOf("\"", quoteStart + 1);
                while (quoteEnd != -1 && responseBody.charAt(quoteEnd - 1) == '\\') {
                    quoteEnd = responseBody.indexOf("\"", quoteEnd + 1);
                }
                
                if (quoteEnd != -1) {
                    String raw = responseBody.substring(quoteStart + 1, quoteEnd);
                    // Check if it's not the system message echoing prompt
                    if (!raw.contains("You are a song writing assistant") && raw.length() > 20) {
                        String unescaped = unescapeJson(raw);
                        if (unescaped != null && !unescaped.isBlank()) {
                            return unescaped;
                        }
                    }
                }
            }
            idx = responseBody.indexOf(searchStr, idx + searchStr.length());
        }

        // Fallback to regex
        Matcher matcher = CONTENT_PATTERN.matcher(responseBody);
        while (matcher.find()) {
            String raw = matcher.group(1);
            if (!raw.contains("You are a song writing assistant")) {
                String unescaped = unescapeJson(raw);
                if (unescaped != null && !unescaped.isBlank()) {
                    return unescaped;
                }
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

    /**
     * Returns the verbatim transcript of the track's audio using local transcription (Whisper).
     * If transcription fails, returns null.
     */
    public String transcribeTrack(Musique track) {
        if (track == null) return null;
        String audio = track.getAudio();
        if (audio == null || audio.isBlank()) return null;
        return TranscriptionService.transcribe(audio);
    }
}
