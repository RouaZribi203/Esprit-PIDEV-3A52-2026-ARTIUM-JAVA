package controllers;

import entities.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;

import java.io.File;

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

    @FXML
    private ImageView navbarAvatarImageView;

    @FXML
    public void initialize() {
        installCircularClip();
    }

    public void setActionHandler(ActionHandler actionHandler) {
        this.actionHandler = actionHandler;
    }

    public void setUser(User user) {
        if (user == null || navbarAvatarImageView == null) {
            return;
        }

        String imagePath = pickProfileImage(user);
        if (imagePath != null) {
            try {
                Image image = new Image(toImageUrl(imagePath), 64, 64, false, true);
                if (!image.isError()) {
                    navbarAvatarImageView.setImage(image);
                    return;
                }
            } catch (IllegalArgumentException ex) {
                // Image invalide, garder l'image par defaut
            }
        }
    }

    private void installCircularClip() {
        Circle clip = new Circle(17);
        clip.setCenterX(17);
        clip.setCenterY(17);
        navbarAvatarImageView.setClip(clip);
    }

    private String pickProfileImage(User user) {
        if (user.getPhotoProfil() != null && !user.getPhotoProfil().trim().isEmpty()) {
            return user.getPhotoProfil().trim();
        }
        if (user.getPhotoReferencePath() != null && !user.getPhotoReferencePath().trim().isEmpty()) {
            return user.getPhotoReferencePath().trim();
        }
        return null;
    }

    private String toImageUrl(String imagePath) {
        if (imagePath.startsWith("file:") || imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            return imagePath;
        }
        return new File(imagePath).toURI().toString();
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
