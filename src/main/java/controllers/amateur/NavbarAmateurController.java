package controllers.amateur;

import controllers.MainFX;
import entities.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;

public class NavbarAmateurController {

    @FXML
    private Button anchorButton;

    @FXML
    private Button notificationsButton;

    @FXML
    private MenuButton oeuvresButton;

    @FXML
    private Button bibliothequeButton;

    @FXML
    private Button musiqueButton;

    @FXML
    private MenuButton userMenuButton;

    private Consumer<String> navigationHandler;
    private Consumer<Boolean> themeHandler;
    private String currentRoute = "feed";
    private String oeuvreSectionContext = "feed";

    public void setNavigationHandler(Consumer<String> navigationHandler) {
        this.navigationHandler = navigationHandler;
    }

    public void setThemeHandler(Consumer<Boolean> themeHandler) {
        this.themeHandler = themeHandler;
    }

    public void setUser(User user) {
        if (userMenuButton == null) {
            return;
        }
        if (user == null) {
            userMenuButton.setText("Compte");
            return;
        }

        String prenom = user.getPrenom() == null ? "" : user.getPrenom().trim();
        String nom = user.getNom() == null ? "" : user.getNom().trim();
        String fullName = (prenom + " " + nom).trim();
        userMenuButton.setText(fullName.isEmpty() ? "Compte" : fullName);
    }

    public void setActiveRoute(String route) {
        currentRoute = route == null ? "feed" : route;
        oeuvreSectionContext = currentRoute.startsWith("favoris") ? "favoris" : "feed";
        oeuvresButton.getStyleClass().remove("active");
        oeuvresButton.getStyleClass().remove("active-feed");
        oeuvresButton.getStyleClass().remove("active-favoris");
        bibliothequeButton.getStyleClass().remove("active");
        musiqueButton.getStyleClass().remove("active");

        if (currentRoute.startsWith("feed") || currentRoute.startsWith("favoris")) {
            oeuvresButton.getStyleClass().add("active");
            oeuvresButton.getStyleClass().add("favoris".equals(oeuvreSectionContext) ? "active-favoris" : "active-feed");
        } else if ("bibliotheque".equals(currentRoute) || "book-reader".equals(currentRoute)) {
            bibliothequeButton.getStyleClass().add("active");
        } else if ("musique".equals(currentRoute)) {
            musiqueButton.getStyleClass().add("active");
        }
    }

    @FXML
    private void onFeedClick() {
        navigate(resolveOeuvreRoute("feed", "favoris"));
    }

    @FXML
    private void onFeedPeinturesClick() {
        navigate(resolveOeuvreRoute("feed-peintures", "favoris-peintures"));
    }

    @FXML
    private void onFeedSculpturesClick() {
        navigate(resolveOeuvreRoute("feed-sculptures", "favoris-sculptures"));
    }

    @FXML
    private void onFeedPhotosClick() {
        navigate(resolveOeuvreRoute("feed-photos", "favoris-photos"));
    }

    @FXML
    private void onFeedRecommendationsClick() {
        // Recommendations always open the feed context so the Fil d'actualite section stays active.
        navigate("feed-recommandations");
    }

    @FXML
    private void onBibliothequeClick() {
        navigate("bibliotheque");
    }

    @FXML
    private void onMusiqueClick() {
        navigate("musique");
    }

    @FXML
    private void onThemeLight() {
        if (themeHandler != null) {
            themeHandler.accept(false);
        }
    }

    @FXML
    private void onThemeDark() {
        if (themeHandler != null) {
            themeHandler.accept(true);
        }
    }

    @FXML
    private void onNotificationsClick() {
        notificationsButton.setText("!!");
    }

    @FXML
    private void onSwitchToAdminView() {
        switchScene("/views/MainLayout.fxml", "/views/styles/dashboard.css", "Admin Dashboard");
    }

    @FXML
    private void onSwitchToArtistView() {
        switchScene("/views/artist/ArtistMain.fxml", "/views/styles/artist-theme.css", "Artist Dashboard");
    }

    @FXML
    private void onSwitchToAmateurView() {
        navigate("edit-profile");
    }

    @FXML
    private void onLogoutClick() {
        MainFX.switchToLoginView();
    }

    private void navigate(String route) {
        if (navigationHandler != null) {
            navigationHandler.accept(route);
        }
    }

    private String resolveOeuvreRoute(String feedRoute, String favorisRoute) {
        if ("favoris".equals(oeuvreSectionContext)) {
            return favorisRoute;
        }
        return feedRoute;
    }

    private void switchScene(String fxmlPath, String stylesheetPath, String title) {
        Stage stage = (Stage) anchorButton.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            URL stylesheet = Objects.requireNonNull(getClass().getResource(stylesheetPath), "Missing stylesheet");
            scene.getStylesheets().add(stylesheet.toExternalForm());
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to switch scene: " + fxmlPath, e);
        }
    }
}


