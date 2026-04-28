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
import javafx.scene.layout.Region;
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

         VBox container = new VBox();
         container.setSpacing(8);
         container.setStyle("-fx-background-color: transparent; -fx-padding: 4px;");
         
         // Add header
         Label headerLabel = new Label("Notifications");
         headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #111827; -fx-padding: 4px 8px;");
         container.getChildren().add(headerLabel);
         
         Region separator = new Region();
         separator.setStyle("-fx-background-color: #f3f4f6; -fx-min-height: 2px; -fx-max-height: 2px;");
         container.getChildren().add(separator);

         if (userId == null) {
             notificationsButton.setText("🔔");
             container.getChildren().add(createEmptyNode("Aucune session utilisateur active."));
         } else {
             List<Notification> notifications = notificationService.getUnreadNotifications(userId);
             notificationsButton.setText(notifications.isEmpty() ? "🔔" : "🔔 " + notifications.size());

             if (notifications.isEmpty()) {
                 container.getChildren().add(createEmptyNode("Aucune notification pour le moment."));
             } else {
                 for (Notification notification : notifications) {
                     container.getChildren().add(createNotificationNode(notification));
                 }
             }
         }

         CustomMenuItem wrapperItem = new CustomMenuItem(container, false);
         wrapperItem.setHideOnClick(false);
         // Prevent default MenuItem styles from breaking our layout
         wrapperItem.setStyle("-fx-background-color: transparent;");
         
         notificationsButton.getItems().add(wrapperItem);
     }

     private javafx.scene.Node createEmptyNode(String text) {
         Label emptyLabel = new Label(text);
         emptyLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 14px; -fx-font-style: italic;");
         HBox wrapper = new HBox(emptyLabel);
         wrapper.setStyle("-fx-padding: 24px 20px; -fx-alignment: center;");
         wrapper.setPrefWidth(320);
         return wrapper;
     }

     private javafx.scene.Node createNotificationNode(Notification notification) {
         String title = resolveTitle(notification);
         Label titleLabel = new Label(title);
         Label messageLabel = new Label(resolveMessage(notification));
         messageLabel.setWrapText(true);
         messageLabel.setMaxWidth(300);
         Label timeLabel = new Label(resolveTime(notification));

         VBox content = new VBox(6, titleLabel, messageLabel, timeLabel);
         content.setPrefWidth(320);
         
         if (title.toLowerCase().contains("annul")) {
             content.setStyle("-fx-background-color: #fff7ed; -fx-border-color: #fed7aa; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 14px; -fx-cursor: hand;");
             titleLabel.setStyle("-fx-text-fill: #c2410c; -fx-font-size: 15px; -fx-font-weight: 800;");
             messageLabel.setStyle("-fx-text-fill: #ea580c; -fx-font-size: 13px;");
             timeLabel.setStyle("-fx-text-fill: #f97316; -fx-font-size: 11px; -fx-font-weight: bold;");
         } else {
             content.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 14px; -fx-cursor: hand;");
             titleLabel.setStyle("-fx-text-fill: #1e293b; -fx-font-size: 15px; -fx-font-weight: 800;");
             messageLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");
             timeLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");
         }
         return content;
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


