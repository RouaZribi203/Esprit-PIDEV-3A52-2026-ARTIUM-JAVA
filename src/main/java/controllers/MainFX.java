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
