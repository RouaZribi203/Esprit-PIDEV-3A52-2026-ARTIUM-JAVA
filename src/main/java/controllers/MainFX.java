package controllers;

import entities.User;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.SessionManager;

import java.io.IOException;
import java.net.URL;

public class MainFX extends Application {

    private static Stage primaryStage;
    private static User authenticatedUser;

    public static void switchToAuthLandingView() {
        authenticatedUser = null;
        switchScene("/views/auth/Landing.fxml", "/views/styles/auth.css", "Artium | Accueil");
    }

    public static void switchToLoginView() {
        authenticatedUser = null;
        switchScene("/views/auth/Connexion.fxml", "/views/styles/auth.css", "Artium | Connexion");
    }

    public static void switchToRegistrationView() {
        authenticatedUser = null;
        switchScene("/views/pages/inscription.fxml", "/views/styles/auth.css", "Artium | Inscription");
    }

    public static void switchToForgotPasswordView() {
        authenticatedUser = null;
        switchScene("/views/auth/ForgotPassword.fxml", "/views/styles/auth.css", "Artium | Mot de passe oublie");
    }

    public static void switchToArtistView() {
        authenticatedUser = null;
        switchScene("/views/artist/ArtistMain.fxml", "/views/styles/artist-theme.css", "Artist Dashboard");
    }

    public static void switchToArtistView(User user) {
        authenticatedUser = user;
        switchScene("/views/artist/ArtistMain.fxml", "/views/styles/artist-theme.css", "Artist Dashboard");
    }

    public static void switchToAdminView() {
        authenticatedUser = null;
        switchScene("/views/MainLayout.fxml", "/views/styles/dashboard.css", "Admin Dashboard");
    }

    public static void switchToAdminView(User user) {
        authenticatedUser = user;
        switchScene("/views/MainLayout.fxml", "/views/styles/dashboard.css", "Admin Dashboard");
    }

    public static void switchToAmateurView() {
        authenticatedUser = null;
        switchScene("/views/amateur/AmateurMain.fxml", "/views/styles/amateur-theme.css", "Amateur Dashboard");
    }

    public static void switchToAmateurView(User user) {
        authenticatedUser = user;
        switchScene("/views/amateur/AmateurMain.fxml", "/views/styles/amateur-theme.css", "Amateur Dashboard");
    }

    public static User getAuthenticatedUser() {
        return authenticatedUser;
    }

    private static void switchScene(String fxmlPath, String stylesheetPath, String title) {
        if (primaryStage == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(MainFX.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            URL stylesheet = MainFX.class.getResource(stylesheetPath);
            if (stylesheet != null) {
                scene.getStylesheets().add(stylesheet.toExternalForm());
            }
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to switch scene: " + fxmlPath, e);
        }
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        // Set default admin session for dev/test convenience
        User admin = createDevUser(3, "admin", "user", "Admin");
        SessionManager.setCurrentUser(admin);

        switchToAdminView();
        // Taille minimale globale pour garder le layout lisible, sans figer la taille des scenes.
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(650);
        switchToAuthLandingView();
    }

    private User createDevUser(int id, String prenom, String nom, String role) {
        User user = new User();
        user.setId(id);
        user.setPrenom(prenom);
        user.setNom(nom);
        user.setRole(role);
        user.setStatut("Activé");
        user.setEmail(prenom + "." + nom + "@test.com");
        return user;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
