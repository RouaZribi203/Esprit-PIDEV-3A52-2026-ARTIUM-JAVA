package controllers;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;

public class MainFX extends Application {
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainLayout.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        URL stylesheet = Objects.requireNonNull(getClass().getResource("/views/styles/dashboard.css"), "Missing stylesheet");
        scene.getStylesheets().add(stylesheet.toExternalForm());
        stage.setTitle("Admin Dashboard");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        //commentaire de test
        launch(args);
    }
}
