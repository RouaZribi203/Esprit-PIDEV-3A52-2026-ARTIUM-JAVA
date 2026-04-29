package controllers.amateur;

import controllers.MainFX;
import entities.User;
import entities.Evenement;
import entities.Ticket;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class AmateurMainController {

    private static final String AMATEUR_STYLESHEET = "/views/styles/amateur-theme.css";

    @FXML
    private BorderPane rootPane;

    @FXML
    private StackPane amateurContentArea;

    @FXML
    private NavbarAmateurController navbarIncludeController;

    @FXML
    private SidebarAmateurController sidebarIncludeController;

    @FXML
    private MiniAudioPlayerController miniAudioPlayerIncludeController;

    private Evenement selectedEvent;

    @FXML
    public void initialize() {
        applyStylesheet();
        navbarIncludeController.setNavigationHandler(this::onNavigate);
        navbarIncludeController.setThemeHandler(this::applyTheme);

        User connectedUser = MainFX.getAuthenticatedUser();
        if (connectedUser != null) {
            navbarIncludeController.setUser(connectedUser);
            sidebarIncludeController.setUser(connectedUser);
        } else {
            navbarIncludeController.setUser(null);
        }

        sidebarIncludeController.setNavigationHandler(this::onNavigate);
        miniAudioPlayerIncludeController.setNavigationHandler(this::onNavigate);

        onNavigate("feed");
    }

    private void onNavigate(String route) {
        navbarIncludeController.setActiveRoute(route);
        sidebarIncludeController.setActiveItem(route);
        miniAudioPlayerIncludeController.setVisibleForRoute(route);

        Object controller = loadAmateurView(route, resolveRoute(route));
        configureLoadedController(controller);
    }

    public void openEventDetail(Evenement event) {
        this.selectedEvent = event;
        onNavigate("event-detail");
    }

    public void onTicketPurchased(Ticket ticket) {
        // Handle post-purchase actions, e.g. show confirmation, update UI, etc.
        onNavigate("payment-success");
    }

    private String resolveRoute(String route) {
        return switch (route) {
            case "feed", "feed-peintures", "feed-sculptures", "feed-photos", "feed-recommandations" -> "/views/amateur/Feed.fxml";
            case "favoris" -> "/views/amateur/Favoris.fxml";
            case "evenements" -> "/views/amateur/Evenements.fxml";
            case "event-detail" -> "/views/amateur/EventDetail.fxml";
            case "payment-success" -> "/views/amateur/PaymentSuccess.fxml";
            case "bibliotheque" -> "/views/amateur/Bibliotheque.fxml";
            case "book-reader" -> "/views/amateur/BookReader.fxml";
            case "musique" -> "/views/amateur/Musique.fxml";
            case "reclamations" -> "/views/amateur/Reclamations.fxml";
            case "reclamation-detail" -> "/views/amateur/ReclamationDetail.fxml";
            case "edit-profile" -> "/views/amateur/EditProfile.fxml";
            default -> "/views/amateur/Feed.fxml";
        };
    }

    private void applyTheme(boolean darkMode) {
        if (darkMode) {
            if (!rootPane.getStyleClass().contains("dark-mode")) {
                rootPane.getStyleClass().add("dark-mode");
            }
        } else {
            rootPane.getStyleClass().remove("dark-mode");
        }
    }

    private void applyStylesheet() {
        URL stylesheet = Objects.requireNonNull(getClass().getResource(AMATEUR_STYLESHEET), "Missing stylesheet: " + AMATEUR_STYLESHEET);
        String stylesheetUrl = stylesheet.toExternalForm();
        if (!rootPane.getStylesheets().contains(stylesheetUrl)) {
            rootPane.getStylesheets().add(stylesheetUrl);
        }
    }

    private Object loadAmateurView(String route, String fxmlPath) {
        try {
            URL resource = Objects.requireNonNull(getClass().getResource(fxmlPath), "FXML not found: " + fxmlPath);
            FXMLLoader loader = new FXMLLoader(resource);
            Node page = loader.load();

            Object controller = loader.getController();
            if (controller instanceof FeedController feedController) {
                feedController.setRouteFilter(route);
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Node page = loader.load();

            Object controller = loader.getController();
            if (controller instanceof EditProfileController) {
                ((EditProfileController) controller).setOnProfileUpdated(() -> {
                    sidebarIncludeController.setUser(MainFX.getAuthenticatedUser());
                });
            }

            amateurContentArea.getChildren().setAll(page);
            return controller;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load amateur page: " + fxmlPath, e);
        }
    }

    private void configureLoadedController(Object controller) {
        if (controller instanceof EventsfrontController eventsController) {
            eventsController.setDetailNavigationHandler(this::openEventDetail);
        } else if (controller instanceof EventDetailController detailController) {
            detailController.setEvent(selectedEvent);
            detailController.setPurchaseHandler(this::onTicketPurchased);
            detailController.setBackHandler(() -> onNavigate("evenements"));
        }
    }
}


