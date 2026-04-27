package controllers.amateur;

import controllers.MainFX;
import entities.Notification;
import entities.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import services.NotificationService;
import utils.UserSession;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class NavbarAmateurController {

    private static final DateTimeFormatter NOTIFICATION_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @FXML
    private Button anchorButton;

    @FXML
    private MenuButton notificationsButton;

    @FXML
    private MenuButton oeuvresButton;

    @FXML
    private Button bibliothequeButton;

    @FXML
    private Button musiqueButton;

    @FXML
    private MenuButton userMenuButton;

    private final NotificationService notificationService = new NotificationService();
    private Consumer<String> navigationHandler;
    private Consumer<Boolean> themeHandler;

    @FXML
    public void initialize() {
        if (notificationsButton != null) {
            notificationsButton.setOnShowing(event -> populateNotificationsMenu());
        }
        if (bibliothequeButton != null) {
            bibliothequeButton.setOnAction(event -> navigate("bibliotheque"));
        }
        if (musiqueButton != null) {
            musiqueButton.setOnAction(event -> navigate("musique"));
        }
    }

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
        if (oeuvresButton == null) {
            return;
        }

        oeuvresButton.getStyleClass().remove("active");
        bibliothequeButton.getStyleClass().remove("active");
        musiqueButton.getStyleClass().remove("active");

        if (route.startsWith("feed")) {
            oeuvresButton.getStyleClass().add("active");
        } else if ("bibliotheque".equals(route) || "book-reader".equals(route)) {
            bibliothequeButton.getStyleClass().add("active");
        } else if ("musique".equals(route)) {
            musiqueButton.getStyleClass().add("active");
        }
    }

    @FXML
    private void onFeedClick() {
        navigate("feed");
    }

    @FXML
    private void onFeedPeinturesClick() {
        navigate("feed-peintures");
    }

    @FXML
    private void onFeedSculpturesClick() {
        navigate("feed-sculptures");
    }

    @FXML
    private void onFeedPhotosClick() {
        navigate("feed-photos");
    }

    @FXML
    private void onFeedRecommendationsClick() {
        navigate("feed-recommandations");
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
        populateNotificationsMenu();
    }

     private void populateNotificationsMenu() {
         if (notificationsButton == null) {
             return;
         }

         Integer userId = UserSession.getCurrentUserId();
         notificationsButton.getItems().clear();
         if (notificationsButton.getContextMenu() != null
                 && !notificationsButton.getContextMenu().getStyleClass().contains("notifications-context-menu")) {
             notificationsButton.getContextMenu().getStyleClass().add("notifications-context-menu");
         }
         
         // Add header
         MenuItem headerItem = new MenuItem("Notifications");
         headerItem.getStyleClass().add("notifications-header");
         headerItem.setDisable(true);
         notificationsButton.getItems().add(headerItem);
         notificationsButton.getItems().add(new SeparatorMenuItem());

         if (userId == null) {
             notificationsButton.setText("🔔");
             notificationsButton.getItems().add(disabledItem("Aucune session utilisateur active."));
             return;
         }

         List<Notification> notifications = notificationService.getUnreadNotifications(userId);
          notificationsButton.setText(notifications.isEmpty() ? "🔔" : "🔔 " + notifications.size());

         if (notifications.isEmpty()) {
             notificationsButton.getItems().add(disabledItem("Aucune notification pour le moment."));
             return;
         }

         for (Notification notification : notifications) {
             notificationsButton.getItems().add(createNotificationItem(notification));
         }
     }

     private MenuItem disabledItem(String text) {
         Label emptyLabel = new Label(text);
         emptyLabel.getStyleClass().add("notifications-empty-label");

         HBox wrapper = new HBox(emptyLabel);
         wrapper.getStyleClass().add("notifications-empty-row");

         CustomMenuItem item = new CustomMenuItem(wrapper, false);
         item.setHideOnClick(false);
         item.setDisable(true);
         return item;
     }

     private MenuItem createNotificationItem(Notification notification) {
         Label titleLabel = new Label(resolveTitle(notification));
         titleLabel.getStyleClass().add("notifications-item-title");

         Label messageLabel = new Label(resolveMessage(notification));
         messageLabel.setWrapText(true);
         messageLabel.setMaxWidth(320);
         messageLabel.getStyleClass().add("notifications-item-message");

         Label timeLabel = new Label(resolveTime(notification));
         timeLabel.getStyleClass().add("notifications-item-time");

         VBox content = new VBox(3, titleLabel, messageLabel, timeLabel);
         content.getStyleClass().add("notifications-item-content");

         CustomMenuItem item = new CustomMenuItem(content, false);
         item.getStyleClass().add("notifications-item");
         item.setHideOnClick(false);
         item.setDisable(true);
         return item;
     }

     private String resolveTitle(Notification notification) {
         if (notification.getTitle() == null || notification.getTitle().isBlank()) {
             return "Notification";
         }
         return notification.getTitle().trim();
     }

     private String resolveMessage(Notification notification) {
         if (notification.getMessage() == null || notification.getMessage().isBlank()) {
             return "Vous avez une nouvelle mise a jour.";
         }
         return notification.getMessage().trim();
     }

     private String resolveTime(Notification notification) {
         if (notification.getCreatedAt() == null) {
             return "Maintenant";
         }
         return NOTIFICATION_TIME_FORMATTER.format(notification.getCreatedAt());
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
}


