package controllers.amateur;

import controllers.MainFX;
import controllers.pages.reclamations.ReclamationReplyDialogController;
import entities.Reclamation;
import entities.ReclamationNotification;
import entities.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.ReclamationService;
import services.ReclamationNotificationService;
import utils.UserSession;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLDataException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class NavbarAmateurController {

    private static final int NOTIFICATION_LIMIT = 8;

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

    private final ReclamationNotificationService notificationService = new ReclamationNotificationService();
    private final ReclamationService reclamationService = new ReclamationService();
    private final ContextMenu notificationsMenu = new ContextMenu();
    private final Set<Integer> seenReplyIds = new HashSet<>();
    private final Set<Integer> pushedReplyIds = new HashSet<>();
    private final Map<Integer, LocalDateTime> pushedReplyDates = new HashMap<>();
    private final List<ReclamationNotification> currentNotifications = new ArrayList<>();
    private Integer currentUserId;
    private Timeline refreshTimeline;
    private boolean notificationsBootstrapped;

    @FXML
    public void initialize() {
        configureNotificationsMenu();
        currentUserId = UserSession.getCurrentUserId();
        refreshNotifications();
        startAutoRefresh();
    }

    private void configureNotificationsMenu() {
        notificationsMenu.getStyleClass().add("amateur-notifications-menu");
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
        refreshNotifications();
        buildNotificationsMenu();

        if (notificationsMenu.isShowing()) {
            notificationsMenu.hide();
            return;
        }

        notificationsMenu.show(notificationsButton, javafx.geometry.Side.BOTTOM, 0, 4);
        markCurrentAsSeen();
        updateBellCounter();
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

    private void refreshNotifications() {
        Integer sessionUserId = UserSession.getCurrentUserId();
        if (!Objects.equals(currentUserId, sessionUserId)) {
            currentUserId = sessionUserId;
            seenReplyIds.clear();
            pushedReplyIds.clear();
            pushedReplyDates.clear();
            notificationsBootstrapped = false;
        }

        currentNotifications.clear();

        if (currentUserId == null) {
            updateBellCounter();
            return;
        }

        try {
            currentNotifications.addAll(notificationService.getLatestByUser(currentUserId, NOTIFICATION_LIMIT));
            pushDesktopNotifications(currentNotifications);
        } catch (SQLDataException ignored) {
            // Keep navbar responsive even if notification query fails.
        }

        updateBellCounter();
    }

    private void pushDesktopNotifications(List<ReclamationNotification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return;
        }

        if (!notificationsBootstrapped) {
            for (ReclamationNotification notification : notifications) {
                if (notification != null && notification.getReponseId() != null) {
                    pushedReplyIds.add(notification.getReponseId());
                    pushedReplyDates.put(notification.getReponseId(), notification.getReponseDate());
                }
            }
            notificationsBootstrapped = true;
            return;
        }

        for (ReclamationNotification notification : notifications) {
            if (notification == null || notification.getReponseId() == null) {
                continue;
            }
            Integer reponseId = notification.getReponseId();
            LocalDateTime currentDate = notification.getReponseDate();
            LocalDateTime lastPushedDate = pushedReplyDates.get(reponseId);

            boolean alreadyPushed = pushedReplyIds.contains(reponseId);
            boolean isUpdated = alreadyPushed && isNewerResponseVersion(currentDate, lastPushedDate);
            if (alreadyPushed && !isUpdated) {
                continue;
            }

            String body = truncate(notification.getReponseContenu(), 110);
            String reclamationPreview = truncate(notification.getReclamationTexte(), 55);
            String messagePrefix = isUpdated
                    ? "Admin a modifie la reponse"
                    : "Nouvelle reponse admin";

            Runnable onClickAction = () -> openNotificationResponse(notification);
            DesktopPushNotifier.notify(
                    "Nouvelle reponse admin",
                    messagePrefix + " | Reclamation: " + reclamationPreview + " | Reponse: " + body,
                    onClickAction
            );
            pushedReplyIds.add(reponseId);
            pushedReplyDates.put(reponseId, currentDate);
        }
    }

    private boolean isNewerResponseVersion(LocalDateTime currentDate, LocalDateTime lastPushedDate) {
        if (currentDate == null) {
            return false;
        }
        if (lastPushedDate == null) {
            return true;
        }
        return currentDate.isAfter(lastPushedDate);
    }

    private void buildNotificationsMenu() {
        notificationsMenu.getItems().clear();

        if (currentNotifications.isEmpty()) {
            MenuItem empty = new MenuItem("Aucune nouvelle reponse");
            empty.getStyleClass().add("amateur-notifications-empty");
            empty.setDisable(true);
            notificationsMenu.getItems().add(empty);
            return;
        }

        for (ReclamationNotification notification : currentNotifications) {
            VBox box = new VBox(4);
            box.getStyleClass().add("amateur-notification-card");
            box.setPrefWidth(320);
            box.setMaxWidth(320);

            Label title = new Label("Reponse a votre reclamation");
            title.getStyleClass().add("amateur-notification-title");

            String excerpt = truncate(notification.getReponseContenu(), 90);
            Label body = new Label(excerpt);
            body.setWrapText(true);
            body.getStyleClass().add("amateur-notification-body");

            Label time = new Label(formatRelativeTime(notification.getReponseDate()));
            time.getStyleClass().add("amateur-notification-time");

            if (notification.getReponseId() != null && !seenReplyIds.contains(notification.getReponseId())) {
                box.getStyleClass().add("amateur-notification-card-unread");
            }

            box.getChildren().addAll(title, body, time);

            CustomMenuItem item = new CustomMenuItem(box, true);
            item.getStyleClass().add("amateur-notification-item");
            item.setOnAction(e -> {
                notificationsMenu.hide();
                openNotificationResponse(notification);
            });
            notificationsMenu.getItems().add(item);
        }

        MenuItem seeAll = new MenuItem("Voir toutes mes reclamations");
        seeAll.getStyleClass().add("amateur-notifications-see-all");
        seeAll.setOnAction(e -> {
            ReclamationsController.requestOpenMyReclamationsTab();
            navigate("reclamations");
        });
        notificationsMenu.getItems().add(seeAll);
    }

    private void openNotificationResponse(ReclamationNotification notification) {
        if (notification == null || notification.getReclamationId() == null) {
            openReclamationsList();
            return;
        }

        try {
            Reclamation reclamation = reclamationService.getById(notification.getReclamationId());
            if (reclamation == null || reclamation.getId() == null) {
                openReclamationsList();
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/pages/reclamation_reply_dialog.fxml"));
            Parent root = loader.load();

            ReclamationReplyDialogController controller = loader.getController();
            controller.setReclamation(reclamation);
            controller.setReadOnly(true);
            controller.focusResponse(notification.getReponseId());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            if (notificationsButton != null && notificationsButton.getScene() != null
                    && notificationsButton.getScene().getWindow() instanceof Stage ownerStage) {
                stage.initOwner(ownerStage);
            }
            stage.setTitle("Reclamation #" + reclamation.getId());

            Scene scene = new Scene(root);
            URL dialogCss = getClass().getResource("/views/styles/reclamation-reply-dialog.css");
            if (dialogCss != null) {
                scene.getStylesheets().add(dialogCss.toExternalForm());
            }
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception ignored) {
            openReclamationsList();
        }
    }

    private void openReclamationsList() {
        ReclamationsController.requestOpenMyReclamationsTab();
        navigate("reclamations");
    }

    private void markCurrentAsSeen() {
        for (ReclamationNotification notification : currentNotifications) {
            if (notification.getReponseId() != null) {
                seenReplyIds.add(notification.getReponseId());
            }
        }
    }

    private void updateBellCounter() {
        if (notificationsButton == null) {
            return;
        }

        int unread = 0;
        for (ReclamationNotification notification : currentNotifications) {
            if (notification.getReponseId() != null && !seenReplyIds.contains(notification.getReponseId())) {
                unread++;
            }
        }

        notificationsButton.setText(unread > 0 ? "🔔 " + unread : "🔔");
    }

    private void startAutoRefresh() {
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(15), e -> refreshNotifications()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private String formatRelativeTime(LocalDateTime timestamp) {
        if (timestamp == null) {
            return "date inconnue";
        }

        LocalDateTime now = LocalDateTime.now();
        if (timestamp.isAfter(now)) {
            return "a l'instant";
        }

        long minutes = ChronoUnit.MINUTES.between(timestamp, now);
        if (minutes < 1) {
            return "a l'instant";
        }
        if (minutes < 60) {
            return "il y a " + minutes + " min";
        }

        long hours = ChronoUnit.HOURS.between(timestamp, now);
        if (hours < 24) {
            return "il y a " + hours + (hours == 1 ? " heure" : " heures");
        }

        long days = ChronoUnit.DAYS.between(timestamp, now);
        if (days < 7) {
            return "il y a " + days + (days == 1 ? " jour" : " jours");
        }

        long weeks = Math.max(1, days / 7);
        if (weeks < 5) {
            return "il y a " + weeks + (weeks == 1 ? " semaine" : " semaines");
        }

        return timestamp.truncatedTo(ChronoUnit.MINUTES).toString().replace('T', ' ');
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "Reponse admin";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= max) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, max - 3)) + "...";
    }

    private static final class DesktopPushNotifier {
        private static final Object LOCK = new Object();
        private static TrayIcon trayIcon;
        private static boolean trayInitAttempted;
        private static Runnable lastClickAction;

        private static void notify(String title, String message, Runnable onClickAction) {
            String safeTitle = sanitize(title, "Notification");
            String safeMessage = sanitize(message, "Vous avez une nouvelle notification.");
            lastClickAction = onClickAction;

            if (showUsingSystemTray(safeTitle, safeMessage)) {
                return;
            }
            showFallbackAlert(safeTitle, safeMessage, onClickAction);
        }

        private static boolean showUsingSystemTray(String title, String message) {
            if (GraphicsEnvironment.isHeadless() || !SystemTray.isSupported()) {
                return false;
            }

            ensureTrayIcon();
            if (trayIcon == null) {
                return false;
            }

            try {
                trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }

        private static void ensureTrayIcon() {
            synchronized (LOCK) {
                if (trayInitAttempted) {
                    return;
                }
                trayInitAttempted = true;

                try {
                    SystemTray tray = SystemTray.getSystemTray();
                    TrayIcon icon = new TrayIcon(createDefaultIcon(), "Artium Notifications");
                    icon.setImageAutoSize(true);

                    icon.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent e) {
                            if ((e.getButton() == java.awt.event.MouseEvent.BUTTON1 || 
                                 e.getButton() == java.awt.event.MouseEvent.BUTTON3) && lastClickAction != null) {
                                Platform.runLater(lastClickAction);
                            }
                        }
                    });

                    icon.addActionListener(e -> {
                        if (lastClickAction != null) {
                            Platform.runLater(lastClickAction);
                        }
                    });

                    tray.add(icon);
                    trayIcon = icon;
                } catch (AWTException | SecurityException ignored) {
                    trayIcon = null;
                }
            }
        }

        private static Image createDefaultIcon() {
            BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setColor(new Color(31, 63, 114));
            g.fillRoundRect(0, 0, 16, 16, 6, 6);
            g.setColor(Color.WHITE);
            g.setFont(g.getFont().deriveFont(11f));
            g.drawString("A", 4, 12);
            g.dispose();
            return image;
        }

        private static void showFallbackAlert(String title, String message, Runnable onClickAction) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(title);
                alert.setHeaderText(title);
                alert.setContentText(message);
                alert.showAndWait();
                if (onClickAction != null) {
                    onClickAction.run();
                }
            });
        }

        private static String sanitize(String value, String fallback) {
            if (value == null || value.trim().isEmpty()) {
                return fallback;
            }
            return value.trim();
        }
    }
}
