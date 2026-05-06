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
    @FXML private Slider balanceSlider;
    @FXML private Label balanceLabel;
    
    @FXML private Slider subSlider;
    @FXML private Label subLabel;
    @FXML private Slider bassSlider;
    @FXML private Label bassLabel;
    @FXML private Slider midSlider;
    @FXML private Label midLabel;
    @FXML private Slider presSlider;
    @FXML private Label presLabel;
    @FXML private Slider brillSlider;
    @FXML private Label brillLabel;
    
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
                        case "bal": balanceSlider.setValue(val); break;
                        case "sub": subSlider.setValue(val); break;
                        case "bass": bassSlider.setValue(val); break;
                        case "mid": midSlider.setValue(val); break;
                        case "pres": presSlider.setValue(val); break;
                        case "brill": brillSlider.setValue(val); break;
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
        
        balanceSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            balanceLabel.setText(String.format(Locale.ROOT, "%.2f", newVal.doubleValue()));
            if (previewPlayer != null) {
                previewPlayer.setBalance(newVal.doubleValue());
            }
        });
        
        setupEqSlider(subSlider, subLabel);
        setupEqSlider(bassSlider, bassLabel);
        setupEqSlider(midSlider, midLabel);
        setupEqSlider(presSlider, presLabel);
        setupEqSlider(brillSlider, brillLabel);
    }

    private void setupEqSlider(Slider slider, Label label) {
        if (slider != null) {
            slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (label != null) {
                    label.setText(String.format(Locale.ROOT, "%.1f dB", newVal.doubleValue()));
                }
                updateEqualizer();
            });
        }
    }

    private void updateLabels() {
        if (pitchLabel != null) pitchLabel.setText(String.format(Locale.ROOT, "%.2fx", pitchSlider.getValue()));
        if (balanceLabel != null) balanceLabel.setText(String.format(Locale.ROOT, "%.2f", balanceSlider.getValue()));
        if (subLabel != null) subLabel.setText(String.format(Locale.ROOT, "%.1f dB", subSlider.getValue()));
        if (bassLabel != null) bassLabel.setText(String.format(Locale.ROOT, "%.1f dB", bassSlider.getValue()));
        if (midLabel != null) midLabel.setText(String.format(Locale.ROOT, "%.1f dB", midSlider.getValue()));
        if (presLabel != null) presLabel.setText(String.format(Locale.ROOT, "%.1f dB", presSlider.getValue()));
        if (brillLabel != null) brillLabel.setText(String.format(Locale.ROOT, "%.1f dB", brillSlider.getValue()));
    }

    private void updateEqualizer() {
        if (previewPlayer == null) return;
        AudioEqualizer eq = previewPlayer.getAudioEqualizer();
        if (eq == null) return;
        
        eq.setEnabled(true);
        double sub = subSlider.getValue();
        double bass = bassSlider.getValue();
        double mid = midSlider.getValue();
        double pres = presSlider.getValue();
        double brill = brillSlider.getValue();
        
        for (EqualizerBand band : eq.getBands()) {
            double freq = band.getCenterFrequency();
            if (freq <= 64) {
                band.setGain(sub);
            } else if (freq <= 250) {
                band.setGain(bass);
            } else if (freq <= 1000) {
                band.setGain(mid);
            } else if (freq <= 4000) {
                band.setGain(pres);
            } else {
                band.setGain(brill);
            }
        }
    }

    @FXML
    private void handlePreview() {
        if (isPlaying) {
            stopPreview();
            if (statusLabel != null) statusLabel.setText("Aperçu arrêté");
            return;
        }

        if (baseAudioPath == null || baseAudioPath.isBlank()) {
            if (statusLabel != null) statusLabel.setText("Aucun fichier audio selectionné.");
            return;
        }

        String mediaSource = toMediaSource(baseAudioPath);
        if (mediaSource == null) {
            if (statusLabel != null) statusLabel.setText("Impossible de charger l'audio.");
            return;
        }

        try {
            Media media = new Media(mediaSource);
            previewPlayer = new MediaPlayer(media);
            previewPlayer.setRate(pitchSlider.getValue());
            previewPlayer.setBalance(balanceSlider.getValue());
            
            previewPlayer.setOnReady(() -> {
                updateEqualizer();
                previewPlayer.play();
                isPlaying = true;
                if (statusLabel != null) statusLabel.setText("Lecture en cours...");
            });
            
            previewPlayer.setOnEndOfMedia(() -> {
                isPlaying = false;
                if (statusLabel != null) statusLabel.setText("Aperçu terminé");
            });

        } catch (Exception e) {
            if (statusLabel != null) statusLabel.setText("Erreur: " + e.getMessage());
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
        double bal = balanceSlider.getValue();
        double sub = subSlider.getValue();
        double bass = bassSlider.getValue();
        double mid = midSlider.getValue();
        double pres = presSlider.getValue();
        double brill = brillSlider.getValue();
        
        // Build final path
        if (rate != 1.0 || bal != 0.0 || sub != 0.0 || bass != 0.0 || mid != 0.0 || pres != 0.0 || brill != 0.0) {
            finalAudioPath = String.format(Locale.ROOT, "%s?rate=%.2f&bal=%.2f&sub=%.1f&bass=%.1f&mid=%.1f&pres=%.1f&brill=%.1f", 
                baseAudioPath, rate, bal, sub, bass, mid, pres, brill);
        } else {
            finalAudioPath = baseAudioPath;
        }
        
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    @FXML
    private void handleCancel() {
        stopPreview();
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    @FXML
    private void handleReset() {
        pitchSlider.setValue(1.0);
        balanceSlider.setValue(0.0);
        subSlider.setValue(0.0);
        bassSlider.setValue(0.0);
        midSlider.setValue(0.0);
        presSlider.setValue(0.0);
        brillSlider.setValue(0.0);
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
