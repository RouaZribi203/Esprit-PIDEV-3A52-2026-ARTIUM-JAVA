package controllers;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class MainFX extends Application {

    private static Stage primaryStage;

    public static void switchToAuthLandingView() {
        switchScene("/views/auth/Landing.fxml", "/views/styles/auth.css", "Artium | Accueil");
    }

    public static void switchToLoginView() {
        switchScene("/views/auth/Connexion.fxml", "/views/styles/auth.css", "Artium | Connexion");
    }

    public static void switchToRegistrationView() {
        switchScene("/views/pages/inscription.fxml", "/views/styles/auth.css", "Artium | Inscription");
    }

    public static void switchToForgotPasswordView() {
        switchScene("/views/auth/ForgotPassword.fxml", "/views/styles/auth.css", "Artium | Mot de passe oublie");
    }

    public static void switchToArtistView() {
        switchScene("/views/artist/ArtistMain.fxml", "/views/styles/artist-theme.css", "Artist Dashboard");
    }

    public static void switchToAdminView() {
        switchScene("/views/MainLayout.fxml", "/views/styles/dashboard.css", "Admin Dashboard");
    }

    public static void switchToAmateurView() {
        switchScene("/views/amateur/AmateurMain.fxml", "/views/styles/amateur-theme.css", "Amateur Dashboard");
    }

    private static void switchScene(String fxmlPath, String stylesheetPath, String title) {
        if (primaryStage == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(MainFX.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1100, 650);
            URL stylesheet = Objects.requireNonNull(MainFX.class.getResource(stylesheetPath), "Missing stylesheet");
            scene.getStylesheets().add(stylesheet.toExternalForm());
            primaryStage.setTitle(title);
            primaryStage.setMinWidth(1100);
            primaryStage.setMinHeight(650);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to switch scene: " + fxmlPath, e);
        }
    }

    public void start(Stage stage) {
        primaryStage = stage;
        switchToAuthLandingView();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
