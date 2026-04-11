package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;

public class NavbarController {

    public interface ActionHandler {
        void onToggleSidebar();

        void onThemeSelected(boolean darkMode);
    }

    @FXML
    private Button sidebarToggleButton;

    @FXML
    private MenuButton themeMenuButton;

    @FXML
    private MenuItem lightThemeItem;

    @FXML
    private MenuItem darkThemeItem;

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
    private void onLogoutClick() {
        userMenuButton.setText("Deconnexion");
    }
}


