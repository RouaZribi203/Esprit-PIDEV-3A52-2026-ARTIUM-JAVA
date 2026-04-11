package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class MainController {

    @FXML
    private BorderPane rootPane;

    @FXML
    private StackPane contentArea;

    @FXML
    private SidebarController sidebarIncludeController;

    @FXML
    private NavbarController navbarIncludeController;

    private boolean sidebarCollapsed;
    private boolean darkTheme;

    @FXML
    public void initialize() {
        sidebarIncludeController.setNavigationHandler(this::onNavigate);
        navbarIncludeController.setActionHandler(new NavbarController.ActionHandler() {
            @Override
            public void onToggleSidebar() {
                toggleSidebar();
            }

            @Override
            public void onThemeSelected(boolean darkMode) {
                applyTheme(darkMode);
            }
        });

        onNavigate("dashboard");
    }

    private void onNavigate(String route) {
        sidebarIncludeController.setActiveItem(route);
        switch (route) {
            case "dashboard":
                loadPage("/views/pages/dashboard.fxml");
                break;
            case "artistes":
                loadPage("/views/pages/artistes.fxml");
                break;
            case "amateurs":
                loadPage("/views/pages/amateurs.fxml");
                break;
            case "oeuvres":
                loadPage("/views/pages/oeuvres.fxml");
                break;
            case "livres":
                loadPage("/views/pages/livres.fxml");
                break;
            case "musiques":
                loadPage("/views/pages/musiques.fxml");
                break;
            case "evenements":
                loadPage("/views/pages/evenements.fxml");
                break;
            case "galeries":
                loadPage("/views/pages/galeries.fxml");
                break;
            case "reclamations":
                loadPage("/views/pages/reclamations.fxml");
                break;
            default:
                loadPage("/views/pages/dashboard.fxml");
                break;
        }
    }

    private void toggleSidebar() {
        sidebarCollapsed = !sidebarCollapsed;
        sidebarIncludeController.setCollapsed(sidebarCollapsed);
    }

    private void applyTheme(boolean darkModeEnabled) {
        darkTheme = darkModeEnabled;
        if (darkTheme) {
            rootPane.getStyleClass().add("dark-mode");
        } else {
            rootPane.getStyleClass().remove("dark-mode");
        }
    }

    public void loadPage(String fxmlPath) {
        try {
            URL resource = Objects.requireNonNull(getClass().getResource(fxmlPath), "FXML not found: " + fxmlPath);
            Node page = FXMLLoader.load(resource);
            contentArea.getChildren().setAll(page);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load page: " + fxmlPath, e);
        }
    }
}







