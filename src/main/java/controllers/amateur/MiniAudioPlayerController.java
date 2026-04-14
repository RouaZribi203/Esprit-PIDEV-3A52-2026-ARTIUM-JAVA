package controllers.amateur;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

public class MiniAudioPlayerController {

    @FXML
    private HBox playerBar;

    @FXML
    private Button togglePlayButton;

    private NavigationHandler navigationHandler;

    @FXML
    public void initialize() {
        // Initialize mini audio player
        if (playerBar != null) {
            playerBar.setVisible(false);
            playerBar.setManaged(false);
        }
    }

    @FXML
    public void onPrevTrack() {
        // Handle previous track
    }

    @FXML
    public void onTogglePlay() {
        // Handle toggle play
        if (togglePlayButton != null) {
            togglePlayButton.setText(togglePlayButton.getText().equals("▶") ? "⏸" : "▶");
        }
    }

    @FXML
    public void onNextTrack() {
        // Handle next track
    }

    @FXML
    public void onOpenMusic() {
        // Open music page
        if (navigationHandler != null) {
            navigationHandler.navigate("musique");
        }
    }

    public void setNavigationHandler(NavigationHandler handler) {
        this.navigationHandler = handler;
    }

    public void setVisibleForRoute(String route) {
        if (playerBar != null) {
            boolean showPlayer = "musique".equals(route) || "feed".equals(route);
            playerBar.setVisible(showPlayer);
            playerBar.setManaged(showPlayer);
        }
    }

    @FunctionalInterface
    public interface NavigationHandler {
        void navigate(String route);
    }
}



