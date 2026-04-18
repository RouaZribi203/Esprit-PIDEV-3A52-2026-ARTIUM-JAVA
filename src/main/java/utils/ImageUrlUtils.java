package utils;

/**
 * Normalizes image values before persistence so DB stores a stable absolute URL.
 */
public final class ImageUrlUtils {

    public static final String IMAGE_BASE_URL = "http://127.0.0.1/img/";

    private ImageUrlUtils() {
        // Utility class.
    }

    public static String normalizeForDatabase(String rawValue) {
        if (rawValue == null) {
            return null;
        }

        String candidate = rawValue.trim();
        if (candidate.isEmpty()) {
            return null;
        }

        String fileName = extractFileName(candidate);
        if (fileName.isEmpty()) {
            return null;
        }

        return IMAGE_BASE_URL + fileName;
    }

    private static String extractFileName(String value) {
        String normalized = value.replace('\\', '/');

        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }

        int fragmentIndex = normalized.indexOf('#');
        if (fragmentIndex >= 0) {
            normalized = normalized.substring(0, fragmentIndex);
        }

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        int lastSlash = normalized.lastIndexOf('/');
        String fileName = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
        return fileName.trim();
    }
}

