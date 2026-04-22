package controllers.artist;

import controllers.MainFX;
import entities.User;
import entities.Notification;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.stage.Stage;
import services.NotificationService;
import utils.UserSession;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

public class NavbarArtisteController {

    public interface ActionHandler {
        void onThemeSelected(boolean darkMode);
    }

    @FXML
    private MenuButton notificationsButton;

    @FXML
    private MenuButton userMenuButton;

    private final NotificationService notificationService = new NotificationService();
    private ActionHandler actionHandler;

    @FXML
    public void initialize() {
        if (notificationsButton != null) {
            notificationsButton.setOnShowing(event -> populateNotificationsMenu());
        }
    }

    public void setActionHandler(ActionHandler actionHandler) {
        this.actionHandler = actionHandler;
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
        switchScene("/views/MainLayout.fxml", "/views/styles/dashboard.css", "Admin Dashboard");
    }

    @FXML
    private void onSwitchToArtistView() {
        switchScene("/views/artist/ArtistMain.fxml", "/views/styles/artist-theme.css", "Artist Dashboard");
    }

    @FXML
    private void onSwitchToAmateurView() {
        switchScene("/views/amateur/AmateurMain.fxml", "/views/styles/amateur-theme.css", "Amateur Dashboard");
    }

    @FXML
    private void onNotificationsClick() {
        populateNotificationsMenu();
    }

    @FXML
    private void onLogoutClick() {
        MainFX.switchToLoginView();
    }

    private void switchScene(String fxmlPath, String stylesheetPath, String title) {
        if (notificationsButton == null || notificationsButton.getScene() == null) {
            return;
        }
        Stage stage = (Stage) notificationsButton.getScene().getWindow();
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

     private void populateNotificationsMenu() {
         if (notificationsButton == null) {
             return;
         }

         notificationsButton.getItems().clear();
         
         // Add header
         MenuItem headerItem = new MenuItem("Notifications");
         headerItem.getStyleClass().add("notifications-header");
         headerItem.setDisable(true);
         notificationsButton.getItems().add(headerItem);
         notificationsButton.getItems().add(new SeparatorMenuItem());
         
         // Apply style to context menu
         notificationsButton.getStyleClass().add("notifications-context-menu");
         
         Integer userId = UserSession.getCurrentUserId();
         notificationsButton.setText("🔔");

         if (userId == null) {
             notificationsButton.getItems().add(disabledItem("Aucune session utilisateur active."));
             return;
         }

         List<Notification> notifications = notificationService.getUnreadNotifications(userId);
         if (notifications.isEmpty()) {
             notificationsButton.getItems().add(disabledItem("Aucune notification pour le moment."));
             return;
         }

         for (Notification notification : notifications) {
             notificationsButton.getItems().add(createNotificationItem(notification));
         }
     }

     private MenuItem disabledItem(String text) {
         MenuItem item = new MenuItem(text);
         item.setDisable(true);
         item.getStyleClass().add("notifications-item");
         return item;
     }

     private MenuItem createNotificationItem(Notification notification) {
         String displayText = formatNotification(notification);
         MenuItem item = new MenuItem(displayText);
         item.getStyleClass().add("notifications-item");
         item.setDisable(true);
         return item;
     }

     private String formatNotification(Notification notification) {
         String title = notification.getTitle() == null || notification.getTitle().isBlank() ? "Notification" : notification.getTitle();
         String message = notification.getMessage() == null || notification.getMessage().isBlank() ? "" : notification.getMessage();
         if (message.isEmpty()) {
             return "📌 " + title;
         }
         return "📌 " + title + " - " + message;
     }
}
