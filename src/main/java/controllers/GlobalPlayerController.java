package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.concurrent.Task;
import services.GlobalMediaPlayerService;
import services.OpenRouterLyricsService;
import entities.Musique;

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

    @FXML
    private VBox lyricsPanel;

    @FXML
    private Label lyricsStatusLabel;

    @FXML
    private Button generateLyricsButton;

    @FXML
    private Button copyLyricsButton;

    @FXML
    private TextArea lyricsTextArea;

    @FXML
    private Button lyricsToggleButton;

    private final GlobalMediaPlayerService mediaPlayerService = GlobalMediaPlayerService.getInstance();
    private final OpenRouterLyricsService lyricsService = new OpenRouterLyricsService();
    private boolean lyricsLoading = false;

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

        mediaPlayerService.currentTrackProperty().addListener((obs, oldTrack, newTrack) -> {
            if (lyricsPanel != null && lyricsPanel.isVisible()) {
                updateLyricsPanel(newTrack);
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

    @FXML
    private void handleToggleLyrics() {
        if (lyricsPanel == null) return;
        boolean isVisible = !lyricsPanel.isVisible();
        lyricsPanel.setVisible(isVisible);
        lyricsPanel.setManaged(isVisible);
        
        if (isVisible) {
            lyricsToggleButton.setStyle("-fx-background-color: #3d3bc2; -fx-text-fill: white;");
            updateLyricsPanel(mediaPlayerService.getCurrentTrack());
        } else {
            lyricsToggleButton.setStyle("");
        }
    }

    @FXML
    private void handleGenerateLyrics() {
        Musique track = mediaPlayerService.getCurrentTrack();
        if (track == null) {
            setLyricsStatus("Aucune musique en cours de lecture.");
            return;
        }
        if (lyricsLoading) {
            setLyricsStatus("Génération en cours, veuillez patienter...");
            return;
        }

        lyricsLoading = true;
        setLyricsStatus("Génération des paroles en cours...");
        generateLyricsButton.setDisable(true);
        copyLyricsButton.setDisable(true);
        lyricsTextArea.setText("L'intelligence artificielle écrit les paroles...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return lyricsService.generateLyrics(track);
            }
        };

        task.setOnSucceeded(event -> {
            lyricsLoading = false;
            lyricsTextArea.setText(task.getValue());
            setLyricsStatus("Paroles générées avec succès.");
            generateLyricsButton.setDisable(false);
            copyLyricsButton.setDisable(false);
        });

        task.setOnFailed(event -> {
            lyricsLoading = false;
            Throwable error = task.getException();
            lyricsTextArea.setText("Impossible de générer les paroles.");
            setLyricsStatus(error != null && error.getMessage() != null ? error.getMessage() : "Erreur.");
            generateLyricsButton.setDisable(false);
            copyLyricsButton.setDisable(false);
        });

        Thread worker = new Thread(task, "openrouter-lyrics-generator");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void handleCopyLyrics() {
        if (lyricsTextArea == null || lyricsTextArea.getText() == null || lyricsTextArea.getText().isBlank()) {
            setLyricsStatus("Aucune parole à copier.");
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(lyricsTextArea.getText());
        Clipboard.getSystemClipboard().setContent(content);
        setLyricsStatus("Paroles copiées dans le presse-papiers.");
    }

    private void updateLyricsPanel(Musique track) {
        if (track == null) {
            lyricsTextArea.setText("Aucune piste en cours.");
            setLyricsStatus("Lancez une musique pour générer les paroles.");
            generateLyricsButton.setDisable(true);
            copyLyricsButton.setDisable(true);
        } else {
            if (!lyricsLoading) {
                lyricsTextArea.setText("Cliquez sur Générer pour créer des paroles pour ce morceau.");
                setLyricsStatus("Prêt à générer.");
                generateLyricsButton.setDisable(false);
                copyLyricsButton.setDisable(true);
            }
        }
    }

    private void setLyricsStatus(String text) {
        if (lyricsStatusLabel != null) {
            lyricsStatusLabel.setText(text);
        }
    }
}

