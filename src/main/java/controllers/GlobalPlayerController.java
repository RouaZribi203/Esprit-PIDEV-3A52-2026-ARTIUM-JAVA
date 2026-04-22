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

    private final GlobalMediaPlayerService mediaPlayerService = GlobalMediaPlayerService.getInstance();
    private boolean seeking;

    @FXML
    public void initialize() {
        titleLabel.textProperty().bind(mediaPlayerService.trackTitleProperty());
        artistLabel.textProperty().bind(mediaPlayerService.trackArtistProperty());
        metaLabel.textProperty().bind(mediaPlayerService.trackMetaProperty());
        statusLabel.textProperty().bind(mediaPlayerService.statusTextProperty());
        timeLabel.textProperty().bind(mediaPlayerService.timeTextProperty());
        coverImageView.imageProperty().bind(mediaPlayerService.coverImageProperty());

        mediaPlayerService.playingProperty().addListener((obs, oldValue, newValue) -> {
            playPauseButton.setText(newValue ? "Pause" : "Play");
        });
        playPauseButton.setText(mediaPlayerService.isPlaying() ? "Pause" : "Play");

        progressSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (seeking) {
                mediaPlayerService.seekToFraction(newValue.doubleValue());
            }
        });

        mediaPlayerService.timeTextProperty().addListener((obs, oldValue, newValue) -> {
            if (!seeking) {
                String[] parts = newValue.split("/");
                if (parts.length == 2) {
                    double elapsed = parseTime(parts[0].trim());
                    double total = parseTime(parts[1].trim());
                    progressSlider.setValue(total <= 0.0 ? 0.0 : elapsed / total);
                }
            }
        });
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
        seeking = true;
    }

    @FXML
    private void handleSeekEnd() {
        mediaPlayerService.seekToFraction(progressSlider.getValue());
        seeking = false;
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

