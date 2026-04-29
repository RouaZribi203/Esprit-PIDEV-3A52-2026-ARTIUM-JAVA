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
import services.EmailServiceEvent;

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
    private Ticket pendingPurchasedTicket;

    @FXML
    public void initialize() {
        applyStylesheet();
        navbarIncludeController.setNavigationHandler(this::onNavigate);
        navbarIncludeController.setThemeHandler(this::applyTheme);

        User connectedUser = MainFX.getAuthenticatedUser();
        if (connectedUser != null) {
            sidebarIncludeController.setUser(connectedUser);
        }

        sidebarIncludeController.setNavigationHandler(this::onNavigate);
        miniAudioPlayerIncludeController.setNavigationHandler(this::onNavigate);

        onNavigate("feed");
    }

    private void onNavigate(String route) {
        navbarIncludeController.setActiveRoute(route);
        sidebarIncludeController.setActiveItem(route);
        miniAudioPlayerIncludeController.setVisibleForRoute(route);
        Object controller = loadAmateurView(resolveRoute(route));
        configureLoadedController(controller);
    }

    public void openEventDetail(Evenement event) {
        this.selectedEvent = event;
        onNavigate("event-detail");
    }

    public void onTicketPurchased(Ticket ticket) {
        this.pendingPurchasedTicket = ticket;

        // Envoi de l'email en arrière-plan
        User connectedUser = MainFX.getAuthenticatedUser();
        if (connectedUser != null && connectedUser.getEmail() != null) {
            EmailServiceEvent emailService = new EmailServiceEvent();
            emailService.sendTicketEmailAsync(connectedUser.getEmail(), selectedEvent, ticket);
        }

        onNavigate("payment-success");
    }

    private String resolveRoute(String route) {
        switch (route) {
            case "feed":
            case "feed-peintures":
            case "feed-sculptures":
            case "feed-photos":
                return "/views/amateur/Feed.fxml";
            case "feed-recommandations":
                return "/views/amateur/FeedReco.fxml";
            case "favoris":
                return "/views/amateur/Favoris.fxml";
            case "evenements":
                return "/views/amateur/Evenements.fxml";
            case "event-detail":
                return "/views/amateur/EventDetail.fxml";
            case "payment-form":
                return "/views/amateur/PaymentForm.fxml";
            case "payment-success":
                return "/views/amateur/PaymentSuccess.fxml";
            case "bibliotheque":
                return "/views/amateur/Bibliotheque.fxml";
            case "book-reader":
                return "/views/amateur/BookReader.fxml";
            case "musique":
                return "/views/amateur/Musique.fxml";
            case "reclamations":
                return "/views/amateur/Reclamations.fxml";
            case "reclamation-detail":
                return "/views/amateur/ReclamationDetail.fxml";
            case "edit-profile":
                return "/views/amateur/EditProfile.fxml";
            default:
                return "/views/amateur/Feed.fxml";
        }
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

    private Object loadAmateurView(String fxmlPath) {
        try {
            URL resource = Objects.requireNonNull(getClass().getResource(fxmlPath), "FXML not found: " + fxmlPath);
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
            detailController.setPayHandler(ev -> {
                this.selectedEvent = ev;
                onNavigate("payment-form");
            });
            detailController.setBackHandler(() -> onNavigate("evenements"));
        } else if (controller instanceof PaymentFormController formController) {
            formController.setEvent(selectedEvent);
            formController.setSuccessHandler(this::onTicketPurchased);
            formController.setCancelHandler(() -> onNavigate("event-detail"));
        } else if (controller instanceof PaymentController paymentController) {
            paymentController.setTicket(pendingPurchasedTicket);
            paymentController.setBackToEventHandler(() -> onNavigate("event-detail"));
            paymentController.setBackToEventsHandler(() -> onNavigate("evenements"));
            pendingPurchasedTicket = null;
        }
    }
}


