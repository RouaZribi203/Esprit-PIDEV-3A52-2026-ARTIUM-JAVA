package controllers.artist;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class ArtistMainController {

    @FXML
    private BorderPane rootPane;

    @FXML
    private StackPane artistContentArea;

    @FXML
    private NavbarArtisteController navbarIncludeController;

    @FXML
    private ProfileHeaderController profileHeaderIncludeController;

    private boolean darkTheme;

    @FXML
    public void initialize() {
        profileHeaderIncludeController.setNavigationHandler(this::onNavigate);

        navbarIncludeController.setActionHandler(this::applyTheme);

        onNavigate("oeuvres");
    }

    private void onNavigate(String route) {
        profileHeaderIncludeController.setActiveTab(route);
        loadArtistView(resolveRoute(route));
    }

    private String resolveRoute(String route) {
        switch (route) {
            case "collections":
                return "/views/artist/Collections.fxml";
            case "oeuvres":
                return "/views/artist/MesOeuvres.fxml";
            case "musiques":
                return "/views/artist/Musiques.fxml";
            case "bibliotheque":
                return "/views/artist/Bibliotheque.fxml";
            case "evenements":
                return "/views/artist/Evenements.fxml";
            case "reclamations":
                return "/views/artist/Reclamations.fxml";
            case "statistiques":
                return "/views/artist/Statistiques.fxml";
            default:
                return "/views/artist/Collections.fxml";
        }
    }

    private void applyTheme(boolean darkModeEnabled) {
        darkTheme = darkModeEnabled;
        if (darkTheme) {
            if (!rootPane.getStyleClass().contains("dark-mode")) {
                rootPane.getStyleClass().add("dark-mode");
            }
        } else {
            rootPane.getStyleClass().remove("dark-mode");
        }
    }

    private void loadArtistView(String fxmlPath) {
        try {
            URL resource = Objects.requireNonNull(getClass().getResource(fxmlPath), "FXML not found: " + fxmlPath);
            Node page = FXMLLoader.load(resource);
            artistContentArea.getChildren().setAll(page);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load artist page: " + fxmlPath, e);
        }
    }
}
