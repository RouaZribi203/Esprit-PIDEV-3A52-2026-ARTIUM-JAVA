package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;

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
        // Profil: point d'extension pour ouvrir une page profil admin.
    }

    @FXML
    private void onLogoutClick() {
        MainFX.switchToLoginView();
    }
}
