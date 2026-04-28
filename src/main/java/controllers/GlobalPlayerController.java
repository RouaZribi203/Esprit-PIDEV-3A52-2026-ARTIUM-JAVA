package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import services.GlobalMediaPlayerService;

public class GlobalPlayerController {

    @FXML
    private ImageView coverImageView;

    @FXML
    private Label titleLabel;

    @FXML
    private Label artistLabel;

    @FXML
    private Label metaLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label timeLabel;

    @FXML
    private Button playPauseButton;

    @FXML
    private Slider progressSlider;

    @FXML
    private Slider volumeSlider;

    @FXML
    private Button muteButton;

    private final GlobalMediaPlayerService mediaPlayerService = GlobalMediaPlayerService.getInstance();

    @FXML
    public void initialize() {
        titleLabel.textProperty().bind(mediaPlayerService.trackTitleProperty());
        artistLabel.textProperty().bind(mediaPlayerService.trackArtistProperty());
        metaLabel.textProperty().bind(mediaPlayerService.trackMetaProperty());
        statusLabel.textProperty().bind(mediaPlayerService.statusTextProperty());
        timeLabel.textProperty().bind(mediaPlayerService.timeTextProperty());
        coverImageView.imageProperty().bind(mediaPlayerService.coverImageProperty());

        mediaPlayerService.playingProperty().addListener((obs, oldValue, newValue) -> {
            playPauseButton.setText(newValue ? "⏸" : "▶");
        });
        playPauseButton.setText(mediaPlayerService.isPlaying() ? "⏸" : "▶");

        progressSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (progressSlider.isPressed() || progressSlider.isValueChanging()) {
                mediaPlayerService.seekToFraction(newValue.doubleValue());
            }
        });

        mediaPlayerService.timeTextProperty().addListener((obs, oldValue, newValue) -> {
            if (!progressSlider.isPressed() && !progressSlider.isValueChanging()) {
                String[] parts = newValue.split("/");
                if (parts.length == 2) {
                    double elapsed = parseTime(parts[0].trim());
                    double total = parseTime(parts[1].trim());
                    progressSlider.setValue(total <= 0.0 ? 0.0 : elapsed / total);
                }
            }
        });

        if (volumeSlider != null) {
            volumeSlider.setValue(mediaPlayerService.volumeProperty().get());
            mediaPlayerService.volumeProperty().bind(volumeSlider.valueProperty());
        }

        if (muteButton != null) {
            muteButton.setText(mediaPlayerService.mutedProperty().get() ? "🔇" : "🔊");
            mediaPlayerService.mutedProperty().addListener((obs, oldV, newV) -> {
                muteButton.setText(newV ? "🔇" : "🔊");
            });
        }
    }

    @FXML
    private void handlePlayPause() {
        mediaPlayerService.togglePlayPause();
    }

    @FXML
    private void handlePrevious() {
        mediaPlayerService.playPrevious();
    }

    @FXML
    private void handleNext() {
        mediaPlayerService.playNext();
    }

    @FXML
    private void handleSeekStart() {
        // Not strictly needed anymore, handled by isPressed()
    }

    @FXML
    private void handleSeekEnd() {
        mediaPlayerService.seekToFraction(progressSlider.getValue());
    }

    @FXML
    private void handleMute() {
        mediaPlayerService.toggleMute();
    }

    private double parseTime(String time) {
        String[] parts = time.split(":");
        if (parts.length != 2) {
            return 0.0;
        }
        try {
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            return (minutes * 60.0) + seconds;
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }
}

