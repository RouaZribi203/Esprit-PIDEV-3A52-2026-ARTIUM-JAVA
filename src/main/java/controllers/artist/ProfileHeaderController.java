package controllers.artist;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.util.Arrays;
import java.util.List;

public class ProfileHeaderController {

    public interface NavigationHandler {
        void onNavigate(String route);
    }

    @FXML
    private Label fullNameLabel;

    @FXML
    private Label metaLabel;

    @FXML
    private Button collectionsTabButton;

    @FXML
    private Button contentTabButton;

    @FXML
    private Button musiquesTabButton;

    @FXML
    private Button evenementsTabButton;

    @FXML
    private Button reclamationsTabButton;

    @FXML
    private Button statistiquesTabButton;

    private NavigationHandler navigationHandler;
    private String specialite = "Sculpteur";
    private String dynamicRoute = "oeuvres";

    @FXML
    public void initialize() {
        fullNameLabel.setText("wael zribi");
        metaLabel.setText("Sculpteur  -  Tunis  -  Inscrit le 11 Apr 2026");
        updateDynamicTab();
    }

    public void setNavigationHandler(NavigationHandler navigationHandler) {
        this.navigationHandler = navigationHandler;
    }

    public void setActiveTab(String route) {
        List<Button> tabs = Arrays.asList(
                collectionsTabButton,
                contentTabButton,
                musiquesTabButton,
                evenementsTabButton,
                reclamationsTabButton,
                statistiquesTabButton
        );
        for (Button tab : tabs) {
            tab.getStyleClass().remove("active");
        }

        if ("collections".equals(route)) {
            collectionsTabButton.getStyleClass().add("active");
        } else if ("musiques".equals(route)) {
            musiquesTabButton.getStyleClass().add("active");
        } else if (dynamicRoute.equals(route)) {
            contentTabButton.getStyleClass().add("active");
        } else if ("evenements".equals(route)) {
            evenementsTabButton.getStyleClass().add("active");
        } else if ("reclamations".equals(route)) {
            reclamationsTabButton.getStyleClass().add("active");
        } else if ("statistiques".equals(route)) {
            statistiquesTabButton.getStyleClass().add("active");
        }
    }

    @FXML
    private void onCollectionsClick() {
        navigate("collections");
    }

    @FXML
    private void onDynamicContentClick() {
        navigate(dynamicRoute);
    }

    @FXML
    private void onMusiquesClick() {
        navigate("musiques");
    }

    @FXML
    private void onEvenementsClick() {
        navigate("evenements");
    }

    @FXML
    private void onReclamationsClick() {
        navigate("reclamations");
    }

    @FXML
    private void onStatistiquesClick() {
        navigate("statistiques");
    }

    @FXML
    private void onEditProfileClick() {
        // Placeholder for edit profile drawer/dialog.
    }

    private void navigate(String route) {
        if (navigationHandler != null) {
            navigationHandler.onNavigate(route);
        }
    }

    private void updateDynamicTab() {
        if ("Musicien".equalsIgnoreCase(specialite)) {
            dynamicRoute = "musiques";
            contentTabButton.setText("Musiques");
        } else if ("Auteur".equalsIgnoreCase(specialite)) {
            dynamicRoute = "bibliotheque";
            contentTabButton.setText("Bibliotheque");
        } else {
            dynamicRoute = "oeuvres";
            contentTabButton.setText("Mes Oeuvres");
        }
    }
}

