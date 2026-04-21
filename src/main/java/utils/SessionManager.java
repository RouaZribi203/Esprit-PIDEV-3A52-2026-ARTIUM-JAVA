package utils;

import entities.User;
import java.io.*;
import java.nio.file.*;

/**
 * Gestionnaire de session persistante pour l'application ARTIUM.
 * Maintient la session utilisateur et la sauvegarde dans un fichier.
 * L'utilisateur reste connecté après fermeture/réouverture de l'application.
 */
public class SessionManager {
    private static User currentUser;
    private static final String SESSION_FILE = System.getProperty("user.home") + "/.artium/session.dat";
    private static final String SESSION_DIR = System.getProperty("user.home") + "/.artium";

    static {
        // Initialiser le répertoire de session au démarrage
        initializeSessionDirectory();
        // Charger la session persistante si elle existe
        loadSessionFromFile();
    }

    /**
     * Initialise le répertoire de session.
     */
    private static void initializeSessionDirectory() {
        try {
            Path dir = Paths.get(SESSION_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du répertoire de session: " + e.getMessage());
        }
    }

    /**
     * Récupère l'utilisateur actuellement connecté.
     * @return L'utilisateur connecté, ou null s'il n'y en a pas
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * Définit l'utilisateur actuellement connecté et sauvegarde la session.
     * @param user L'utilisateur à connecter
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
        if (user != null) {
            saveSessionToFile();
        }
    }

    /**
     * Vérifie si un utilisateur est actuellement connecté.
     * @return true si un utilisateur est connecté, false sinon
     */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Efface la session utilisateur (déconnexion) et supprime le fichier de session.
     */
    public static void clearSession() {
        currentUser = null;
        deleteSessionFile();
    }

    /**
     * Obtient l'ID de l'utilisateur actuellement connecté.
     * @return L'ID de l'utilisateur, ou null s'il n'y en a pas
     */
    public static Integer getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : null;
    }

    /**
     * Obtient le rôle de l'utilisateur actuellement connecté.
     * @return Le rôle de l'utilisateur, ou une chaîne vide s'il n'y en a pas
     */
    public static String getCurrentUserRole() {
        return currentUser != null ? currentUser.getRole() : "";
    }

    /**
     * Obtient l'email de l'utilisateur actuellement connecté.
     * @return L'email de l'utilisateur, ou null s'il n'y en a pas
     */
    public static String getCurrentUserEmail() {
        return currentUser != null ? currentUser.getEmail() : null;
    }

    /**
     * Sauvegarde la session utilisateur dans un fichier.
     */
    private static void saveSessionToFile() {
        if (currentUser == null) {
            return;
        }

        try {
            Path sessionPath = Paths.get(SESSION_FILE);
            
            // Créer le répertoire s'il n'existe pas
            Path dir = sessionPath.getParent();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            // Sérialiser l'utilisateur en JSON (format simple et lisible)
            String json = serializeUserToJson(currentUser);
            
            // Écrire dans le fichier
            Files.write(sessionPath, json.getBytes(), 
                StandardOpenOption.CREATE, 
                StandardOpenOption.WRITE, 
                StandardOpenOption.TRUNCATE_EXISTING);
            
            System.out.println("✓ Session sauvegardée pour: " + currentUser.getEmail());
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde de la session: " + e.getMessage());
        }
    }

    /**
     * Charge la session utilisateur depuis le fichier.
     */
    private static void loadSessionFromFile() {
        try {
            Path sessionPath = Paths.get(SESSION_FILE);
            
            if (!Files.exists(sessionPath)) {
                System.out.println("Aucune session persistante trouvée.");
                return;
            }

            String json = new String(Files.readAllBytes(sessionPath));
            currentUser = deserializeUserFromJson(json);
            
            if (currentUser != null) {
                System.out.println("✓ Session restaurée pour: " + currentUser.getEmail());
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la session: " + e.getMessage());
            currentUser = null;
        }
    }

    /**
     * Supprime le fichier de session.
     */
    private static void deleteSessionFile() {
        try {
            Path sessionPath = Paths.get(SESSION_FILE);
            if (Files.exists(sessionPath)) {
                Files.delete(sessionPath);
                System.out.println("✓ Session supprimée.");
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la suppression de la session: " + e.getMessage());
        }
    }

    /**
     * Sérialise un utilisateur en JSON.
     */
    private static String serializeUserToJson(User user) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        appendJsonField(json, "id", String.valueOf(user.getId()), false);
        appendJsonField(json, "nom", user.getNom(), true);
        appendJsonField(json, "prenom", user.getPrenom(), true);
        appendJsonField(json, "email", user.getEmail(), true);
        appendJsonField(json, "role", user.getRole(), true);
        appendJsonField(json, "statut", user.getStatut(), true);
        appendJsonField(json, "photoProfil", user.getPhotoProfil(), true);
        appendJsonField(json, "biographie", user.getBiographie(), true);
        appendJsonField(json, "specialite", user.getSpecialite(), true);
        appendJsonField(json, "centreInteret", user.getCentreInteret(), true);
        appendJsonField(json, "ville", user.getVille(), true);
        appendJsonField(json, "numTel", user.getNumTel(), true, true);
        json.append("}");
        return json.toString();
    }

    /**
     * Désérialise un utilisateur depuis JSON.
     */
    private static User deserializeUserFromJson(String json) {
        try {
            User user = new User();
            
            user.setId(extractIntValue(json, "id"));
            user.setNom(extractStringValue(json, "nom"));
            user.setPrenom(extractStringValue(json, "prenom"));
            user.setEmail(extractStringValue(json, "email"));
            user.setRole(extractStringValue(json, "role"));
            user.setStatut(extractStringValue(json, "statut"));
            user.setPhotoProfil(extractStringValue(json, "photoProfil"));
            user.setBiographie(extractStringValue(json, "biographie"));
            user.setSpecialite(extractStringValue(json, "specialite"));
            user.setCentreInteret(extractStringValue(json, "centreInteret"));
            user.setVille(extractStringValue(json, "ville"));
            user.setNumTel(extractStringValue(json, "numTel"));
            
            return user;
        } catch (Exception e) {
            System.err.println("Erreur lors de la désérialisation de la session: " + e.getMessage());
            return null;
        }
    }

    /**
     * Ajoute un champ JSON au StringBuilder.
     */
    private static void appendJsonField(StringBuilder json, String key, String value, boolean isString) {
        appendJsonField(json, key, value, isString, false);
    }

    /**
     * Ajoute un champ JSON au StringBuilder avec option pour le dernier champ.
     */
    private static void appendJsonField(StringBuilder json, String key, String value, boolean isString, boolean isLast) {
        if (json.length() > 1) {
            json.append(",");
        }
        json.append("\"").append(key).append("\":");
        if (isString) {
            json.append("\"").append(escapeJsonString(value)).append("\"");
        } else {
            json.append(value);
        }
    }

    /**
     * Échappe les caractères spéciaux pour JSON.
     */
    private static String escapeJsonString(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r");
    }

    /**
     * Extrait une valeur de chaîne du JSON.
     */
    private static String extractStringValue(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int startIndex = json.indexOf(pattern);
        if (startIndex == -1) {
            return null;
        }
        startIndex += pattern.length();
        int endIndex = json.indexOf("\"", startIndex);
        if (endIndex == -1) {
            return null;
        }
        return json.substring(startIndex, endIndex).replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "\r");
    }

    /**
     * Extrait une valeur entière du JSON.
     */
    private static Integer extractIntValue(String json, String key) {
        String pattern = "\"" + key + "\":";
        int startIndex = json.indexOf(pattern);
        if (startIndex == -1) {
            return null;
        }
        startIndex += pattern.length();
        int endIndex = json.indexOf(",", startIndex);
        if (endIndex == -1) {
            endIndex = json.indexOf("}", startIndex);
        }
        try {
            String valueStr = json.substring(startIndex, endIndex).trim();
            return Integer.parseInt(valueStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

