package controllers.amateur;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import java.util.function.Consumer;

public class MiniAudioPlayerController {

    @FXML
    private HBox playerBar;

    @FXML
    private Button togglePlayButton;

    private Consumer<String> navigationHandler;
    private boolean playing;

    @FXML
    public void initialize() {
        playerBar.setVisible(false);
        playerBar.setManaged(false);
    }

    public void setNavigationHandler(Consumer<String> navigationHandler) {
        this.navigationHandler = navigationHandler;
    }

    public void setVisibleForRoute(String route) {
        boolean isMusicPage = "musique".equals(route);
        playerBar.setVisible(!isMusicPage);
        playerBar.setManaged(!isMusicPage);
    }

    @FXML
    private void onTogglePlay() {
        playing = !playing;
        togglePlayButton.setText(playing ? "||" : ">");
    }

    @FXML
    private void onPrevTrack() {
        // Placeholder for queue previous action.
    }

    @FXML
    private void onNextTrack() {
        // Placeholder for queue next action.
    }

    @FXML
    private void onOpenMusic() {
        if (navigationHandler != null) {
            navigationHandler.accept("musique");
        }
    }
}

