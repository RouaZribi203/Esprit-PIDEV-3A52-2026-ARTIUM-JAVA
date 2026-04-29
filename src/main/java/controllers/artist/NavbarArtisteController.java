package controllers.artist;

import controllers.MainFX;
import utils.SessionManager;
import entities.User;
import controllers.MainFX;
import entities.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuButton;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class NavbarArtisteController {

    public interface ActionHandler {
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
    private void onSwitchToAdminView() {
        MainFX.switchToAdminView(resolveCurrentUser());
    }

    @FXML
    private void onSwitchToArtistView() {
        MainFX.switchToArtistView(resolveCurrentUser());
    }

    @FXML
    private void onSwitchToAmateurView() {
        MainFX.switchToAmateurView(resolveCurrentUser());
    }

    @FXML
    private void onNotificationsClick() {
        notificationsButton.setText("!!");
    }

	@FXML
	private void onLogoutClick() {
		// Effacer la session persistante
		SessionManager.clearSession();
		// Rediriger vers la page d'authentification
		MainFX.switchToLoginView();
	}

    private User resolveCurrentUser() {
        User user = MainFX.getAuthenticatedUser();
        if (user == null) {
            user = SessionManager.getCurrentUser();
        }
        return user;
    }
}
