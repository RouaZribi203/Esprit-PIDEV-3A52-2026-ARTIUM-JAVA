package controllers;

import entities.User;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
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

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        showDevModeSelector();
    }

    private void showDevModeSelector() {
        Label title = new Label("Artium - Dev Mode");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #3b82f6;");

        Label desc = new Label("Select user type to test:");

        ComboBox<String> userTypeCombo = new ComboBox<>();
        userTypeCombo.getItems().addAll("Artiste (khadija)", "Amateur (test user)", "Admin (admin user)");
        userTypeCombo.setValue("Artiste (khadija)");

        javafx.scene.control.Button connectBtn = new javafx.scene.control.Button("Go");
        connectBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 10 30;");
        connectBtn.setOnAction(e -> {
            String selected = userTypeCombo.getValue();
            User user;
            if (selected.startsWith("Artiste")) {
                user = createDevUser(1, "khadija", "dojdoj", "Artiste");
                SessionManager.setCurrentUser(user);
                switchToArtistView();
            } else if (selected.startsWith("Amateur")) {
                user = createDevUser(2, "john", "doe", "Amateur");
                SessionManager.setCurrentUser(user);
                switchToAmateurView();
            } else {
                user = createDevUser(3, "admin", "user", "Admin");
                SessionManager.setCurrentUser(user);
                switchToAdminView();
            }
        });

        VBox root = new VBox(20, title, desc, userTypeCombo, connectBtn);
        root.setStyle("-fx-background-color: #f3f4f6; -fx-padding: 50; -fx-alignment: center;");
        root.setPrefSize(400, 300);

        Scene scene = new Scene(root);
        primaryStage.setTitle("Artium - Dev Mode");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
