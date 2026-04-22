package controllers.amateur;

import controllers.MainFX;
import utils.SessionManager;
import entities.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import java.util.function.Consumer;

public class NavbarAmateurController {

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

    private Consumer<String> navigationHandler;
    private Consumer<Boolean> themeHandler;

    public void setNavigationHandler(Consumer<String> navigationHandler) {
        this.navigationHandler = navigationHandler;
    }

    public void setThemeHandler(Consumer<Boolean> themeHandler) {
        this.themeHandler = themeHandler;
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
        notificationsButton.setText("!!");
    }

    @FXML
    private void onSwitchToAdminView() {
        MainFX.switchToAdminView(resolveCurrentUser());
    }

    @FXML
    private void onSwitchToArtistView() {
        MainFX.switchToArtistView(resolveCurrentUser());
    }

    @FXML
    private void onSwitchToAmateurView() {
        MainFX.switchToAmateurView(resolveCurrentUser());
    }

	@FXML
	private void onLogoutClick() {
		// Effacer la session persistante
		SessionManager.clearSession();
		// Rediriger vers la page d'authentification
		MainFX.switchToLoginView();
	}

    private void navigate(String route) {
        if (navigationHandler != null) {
            navigationHandler.accept(route);
        }
    }

    private User resolveCurrentUser() {
        User user = MainFX.getAuthenticatedUser();
        if (user == null) {
            user = SessionManager.getCurrentUser();
        }
        return user;
    }
}


