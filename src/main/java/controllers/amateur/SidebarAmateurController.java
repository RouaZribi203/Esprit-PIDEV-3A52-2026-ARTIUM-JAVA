package controllers.amateur;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class SidebarAmateurController {

    @FXML
    private Button feedButton;

    @FXML
    private Button favorisButton;

    @FXML
    private Button evenementsButton;

    @FXML
    private Button bibliothequeButton;

    @FXML
    private Button reclamationsButton;

    private Consumer<String> navigationHandler;

    public void setNavigationHandler(Consumer<String> navigationHandler) {
        this.navigationHandler = navigationHandler;
    }

    public void setActiveItem(String route) {
        List<Button> allButtons = Arrays.asList(feedButton, favorisButton, evenementsButton, bibliothequeButton, reclamationsButton);
        for (Button button : allButtons) {
            button.getStyleClass().remove("active");
        }

        if (route.startsWith("feed")) {
            feedButton.getStyleClass().add("active");
        } else if ("favoris".equals(route)) {
            favorisButton.getStyleClass().add("active");
        } else if ("evenements".equals(route) || "event-detail".equals(route) || "payment-success".equals(route)) {
            evenementsButton.getStyleClass().add("active");
        } else if ("bibliotheque".equals(route) || "book-reader".equals(route)) {
            bibliothequeButton.getStyleClass().add("active");
        } else if ("reclamations".equals(route) || "reclamation-detail".equals(route)) {
            reclamationsButton.getStyleClass().add("active");
        }
    }

    @FXML
    private void onFeedClick() {
        navigate("feed");
    }

    @FXML
    private void onFavorisClick() {
        navigate("favoris");
    }

    @FXML
    private void onEvenementsClick() {
        navigate("evenements");
    }

    @FXML
    private void onBibliothequeClick() {
        navigate("bibliotheque");
    }

    @FXML
    private void onReclamationsClick() {
        navigate("reclamations");
    }

    @FXML
    private void onEditProfileClick() {
        navigate("edit-profile");
    }

    private void navigate(String route) {
        if (navigationHandler != null) {
            navigationHandler.accept(route);
        }
    }
}


