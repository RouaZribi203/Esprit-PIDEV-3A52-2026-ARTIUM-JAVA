package controllers.artist;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.media.AudioEqualizer;
import javafx.scene.media.EqualizerBand;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

import java.io.File;
import java.net.URI;
import java.util.Locale;

public class StudioController {

    @FXML private Slider pitchSlider;
    @FXML private Label pitchLabel;
    @FXML private Slider bassSlider;
    @FXML private Label bassLabel;
    @FXML private Slider midSlider;
    @FXML private Label midLabel;
    @FXML private Slider trebleSlider;
    @FXML private Label trebleLabel;
    @FXML private Label statusLabel;

    private String baseAudioPath;
    private String finalAudioPath;
    private MediaPlayer previewPlayer;
    private Stage dialogStage;
    private boolean isPlaying = false;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
        dialogStage.setOnCloseRequest(event -> stopPreview());
    }

    public void setAudioPath(String audioPath) {
        if (audioPath == null || audioPath.isBlank()) {
            return;
        }

        if (audioPath.contains("?")) {
            this.baseAudioPath = audioPath.substring(0, audioPath.indexOf('?'));
            parseExistingParams(audioPath.substring(audioPath.indexOf('?') + 1));
        } else {
            this.baseAudioPath = audioPath;
        }
        this.finalAudioPath = audioPath;
        updateLabels();
    }

    public String getFinalAudioPath() {
        return finalAudioPath;
    }

    private void parseExistingParams(String query) {
        String[] params = query.split("&");
        for (String param : params) {
            String[] kv = param.split("=");
            if (kv.length == 2) {
                try {
                    double val = Double.parseDouble(kv[1]);
                    switch (kv[0]) {
                        case "rate": pitchSlider.setValue(val); break;
                        case "bass": bassSlider.setValue(val); break;
                        case "mid": midSlider.setValue(val); break;
                        case "treble": trebleSlider.setValue(val); break;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    @FXML
    public void initialize() {
        pitchSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            pitchLabel.setText(String.format(Locale.ROOT, "%.2fx", newVal.doubleValue()));
            if (previewPlayer != null) {
                previewPlayer.setRate(newVal.doubleValue());
            }
        });
        
        bassSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            bassLabel.setText(String.format(Locale.ROOT, "%.1f dB", newVal.doubleValue()));
            updateEqualizer();
        });
        
        midSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            midLabel.setText(String.format(Locale.ROOT, "%.1f dB", newVal.doubleValue()));
            updateEqualizer();
        });
        
        trebleSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            trebleLabel.setText(String.format(Locale.ROOT, "%.1f dB", newVal.doubleValue()));
            updateEqualizer();
        });
    }

    private void updateLabels() {
        pitchLabel.setText(String.format(Locale.ROOT, "%.2fx", pitchSlider.getValue()));
        bassLabel.setText(String.format(Locale.ROOT, "%.1f dB", bassSlider.getValue()));
        midLabel.setText(String.format(Locale.ROOT, "%.1f dB", midSlider.getValue()));
        trebleLabel.setText(String.format(Locale.ROOT, "%.1f dB", trebleSlider.getValue()));
    }

    private void updateEqualizer() {
        if (previewPlayer == null) return;
        AudioEqualizer eq = previewPlayer.getAudioEqualizer();
        if (eq == null) return;
        
        eq.setEnabled(true);
        double bass = bassSlider.getValue();
        double mid = midSlider.getValue();
        double treble = trebleSlider.getValue();
        
        for (EqualizerBand band : eq.getBands()) {
            double freq = band.getCenterFrequency();
            if (freq < 250) {
                band.setGain(bass);
            } else if (freq < 4000) {
                band.setGain(mid);
            } else {
                band.setGain(treble);
            }
        }
    }

    @FXML
    private void handlePreview() {
        if (isPlaying) {
            stopPreview();
            statusLabel.setText("Aperçu arrêté");
            return;
        }

        if (baseAudioPath == null || baseAudioPath.isBlank()) {
            statusLabel.setText("Aucun fichier audio selectionné.");
            return;
        }

        String mediaSource = toMediaSource(baseAudioPath);
        if (mediaSource == null) {
            statusLabel.setText("Impossible de charger l'audio.");
            return;
        }

        try {
            Media media = new Media(mediaSource);
            previewPlayer = new MediaPlayer(media);
            previewPlayer.setRate(pitchSlider.getValue());
            
            previewPlayer.setOnReady(() -> {
                updateEqualizer();
                previewPlayer.play();
                isPlaying = true;
                statusLabel.setText("Lecture en cours...");
            });
            
            previewPlayer.setOnEndOfMedia(() -> {
                isPlaying = false;
                statusLabel.setText("Aperçu terminé");
            });

        } catch (Exception e) {
            statusLabel.setText("Erreur: " + e.getMessage());
        }
    }

    private void stopPreview() {
        if (previewPlayer != null) {
            previewPlayer.stop();
            previewPlayer.dispose();
            previewPlayer = null;
        }
        isPlaying = false;
    }

    @FXML
    private void handleSave() {
        stopPreview();
        double rate = pitchSlider.getValue();
        double bass = bassSlider.getValue();
        double mid = midSlider.getValue();
        double treble = trebleSlider.getValue();
        
        // Build final path
        if (rate != 1.0 || bass != 0.0 || mid != 0.0 || treble != 0.0) {
            finalAudioPath = String.format(Locale.ROOT, "%s?rate=%.2f&bass=%.1f&mid=%.1f&treble=%.1f", 
                baseAudioPath, rate, bass, mid, treble);
        } else {
            finalAudioPath = baseAudioPath;
        }
        
        dialogStage.close();
    }

    @FXML
    private void handleCancel() {
        stopPreview();
        dialogStage.close();
    }

    private String toMediaSource(String audioPath) {
        String trimmed = audioPath.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("file:/")) {
            return trimmed;
        }
        File file = new File(trimmed);
        if (!file.exists()) {
            return null;
        }
        return file.toURI().toString();
    }
}
