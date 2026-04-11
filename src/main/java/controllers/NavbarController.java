package controllers;

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

public class NavbarController {

    public interface ActionHandler {
        void onToggleSidebar();

        void onThemeSelected(boolean darkMode);
    }

    @FXML
    private Button notificationsButton;

    @FXML
    private MenuButton userMenuButton;

    private ActionHandler actionHandler;

    public void setActionHandler(ActionHandler actionHandler) {
        this.actionHandler = actionHandler;
    }

    @FXML
    private void onSidebarToggle() {
        if (actionHandler != null) {
            actionHandler.onToggleSidebar();
        }
    }

    @FXML
    private void onThemeLight() {
        if (actionHandler != null) {
            actionHandler.onThemeSelected(false);
        }
    }

    @FXML
    private void onThemeDark() {
        if (actionHandler != null) {
            actionHandler.onThemeSelected(true);
        }
    }

    @FXML
    private void onNotificationsClick() {
        notificationsButton.setText("!!");
    }

    @FXML
    private void onProfileClick() {
        userMenuButton.setText("Admin");
    }

    @FXML
    private void onSwitchToAdminView() {
        userMenuButton.setText("Admin");
        switchScene("/views/MainLayout.fxml", "/views/styles/dashboard.css", "Admin Dashboard");
    }

    @FXML
    private void onSwitchToArtistView() {
        userMenuButton.setText("Artiste");
        switchScene("/views/artist/ArtistMain.fxml", "/views/styles/artist-theme.css", "Artist Dashboard");
    }

    @FXML
    private void onSwitchToAmateurView() {
        userMenuButton.setText("Amateur");
        switchScene("/views/amateur/AmateurMain.fxml", "/views/styles/amateur-theme.css", "Amateur Dashboard");
    }

    @FXML
    private void onLogoutClick() {
        userMenuButton.setText("Deconnexion");
    }

    private void switchScene(String fxmlPath, String stylesheetPath, String title) {
        Stage stage = (Stage) userMenuButton.getScene().getWindow();
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
