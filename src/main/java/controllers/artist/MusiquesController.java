package controllers.artist;

import Services.MusiqueService;
import entities.Musique;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.geometry.Side;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import utils.MyDatabase;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.SQLDataException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MusiquesController {

    @FXML
    private TextField titreField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private ComboBox<String> genreComboBox;

    @FXML
    private ComboBox<CollectionChoice> collectionComboBox;

    @FXML
    private TextField imagePathField;

    @FXML
    private TextField audioPathField;

    @FXML
    private Label feedbackLabel;

    @FXML
    private Button toggleFormButton;

    @FXML
    private VBox formSection;

    @FXML
    private TilePane musiqueGrid;

    @FXML
    private Label emptyListLabel;

    @FXML
    private Label nowPlayingTitleLabel;

    @FXML
    private Label nowPlayingMetaLabel;

    @FXML
    private Label playerStatusLabel;

    @FXML
    private Button playPauseButton;

    @FXML
    private Label formTitleLabel;

    @FXML
    private Button submitMusiqueButton;

    private final MusiqueService musiqueService = new MusiqueService();
    private final ObservableList<Musique> tracks = FXCollections.observableArrayList();
    private final ObservableList<String> genres = FXCollections.observableArrayList(
            "Pop",
            "Rap",
            "Rock",
            "Jazz",
            "Classique",
            "Soul",
            "R&B",
            "Electro"
    );
    private MediaPlayer mediaPlayer;
    private int currentTrackIndex = -1;
    private static final Set<String> PLAYABLE_EXTENSIONS = new HashSet<>(Arrays.asList("mp3", "m4a", "wav"));
    private Integer editingMusiqueId;
    private LocalDate editingDateCreation;
    private byte[] editingImageBytes;
    private String editingAudioPath;

    @FXML
    public void initialize() {
        genreComboBox.setItems(genres);
        loadCollections();
        feedbackLabel.setText("");
        resetEditMode();
        showListView();
        refreshMusiquesList();
    }

    @FXML
    private void handleToggleForm() {
        if (formSection.isVisible()) {
            clearForm();
            resetEditMode();
            showListView();
        } else {
            clearForm();
            resetEditMode();
            showFormView();
        }
    }

    @FXML
    private void handlePlayPause() {
        if (mediaPlayer == null) {
            int selectedIndex = currentTrackIndex;
            if (selectedIndex < 0 && !tracks.isEmpty()) {
                selectedIndex = 0;
            }
            if (selectedIndex >= 0) {
                playTrackAtIndex(selectedIndex, true);
            }
            return;
        }

        MediaPlayer.Status status = mediaPlayer.getStatus();
        if (status == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            playPauseButton.setText("Play");
            playerStatusLabel.setText("En pause");
        } else {
            mediaPlayer.play();
            playPauseButton.setText("Pause");
            playerStatusLabel.setText("Lecture en cours");
        }
    }

    @FXML
    private void handlePreviousTrack() {
        if (tracks.isEmpty()) {
            return;
        }
        int targetIndex = currentTrackIndex <= 0 ? tracks.size() - 1 : currentTrackIndex - 1;
        playTrackAtIndex(targetIndex, true);
    }

    @FXML
    private void handleNextTrack() {
        if (tracks.isEmpty()) {
            return;
        }
        int targetIndex = currentTrackIndex >= tracks.size() - 1 ? 0 : currentTrackIndex + 1;
        playTrackAtIndex(targetIndex, true);
    }

    @FXML
    private void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image de couverture");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File file = chooser.showOpenDialog(imagePathField.getScene().getWindow());
        if (file != null) {
            imagePathField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void handleChooseAudio() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un fichier audio");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Audio lisible (JavaFX)", "*.mp3", "*.m4a", "*.wav")
        );

        File file = chooser.showOpenDialog(audioPathField.getScene().getWindow());
        if (file != null) {
            audioPathField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void handleCreateMusique() {
        String titre = titreField.getText() != null ? titreField.getText().trim() : "";
        String description = descriptionArea.getText() != null ? descriptionArea.getText().trim() : "";
        String genre = genreComboBox.getValue() != null ? genreComboBox.getValue().trim() : "";
        CollectionChoice collectionChoice = collectionComboBox.getValue();
        String imagePath = imagePathField.getText() != null ? imagePathField.getText().trim() : "";
        String audioPath = audioPathField.getText() != null ? audioPathField.getText().trim() : "";
        boolean editMode = editingMusiqueId != null;
        String resolvedAudioPath = !audioPath.isEmpty() ? audioPath : (editingAudioPath != null ? editingAudioPath : "");

        if (titre.length() < 3 || titre.length() > 255) {
            setFeedback("Le titre doit contenir entre 3 et 255 caracteres.", false);
            return;
        }

        if (description.length() < 10 || description.length() > 5000) {
            setFeedback("La description doit contenir entre 10 et 5000 caracteres.", false);
            return;
        }

        if (genre.isEmpty() || resolvedAudioPath.isEmpty()) {
            setFeedback("Veuillez renseigner le genre et le fichier audio.", false);
            return;
        }

        if (!isPlayableAudioPath(resolvedAudioPath)) {
            setFeedback("Format audio non supporte. Utilisez: MP3, M4A ou WAV.", false);
            return;
        }

        Musique musique = new Musique();
        musique.setTitre(titre);
        musique.setDescription(description);
        musique.setId(editingMusiqueId);
        musique.setDateCreation(editingDateCreation != null ? editingDateCreation : LocalDate.now());
        musique.setGenre(genre);
        musique.setCollectionId(collectionChoice != null && collectionChoice.getId() > 0 ? collectionChoice.getId() : null);
        musique.setAudio(resolvedAudioPath);

        byte[] imageBytes = editingImageBytes;

        if (!imagePath.isEmpty()) {
            try {
                imageBytes = Files.readAllBytes(new File(imagePath).toPath());
            } catch (IOException e) {
                setFeedback("Impossible de lire l'image selectionnee.", false);
                return;
            }
        }
        musique.setImage(imageBytes);

        try {
            if (editMode) {
                musique.setUpdatedAt(LocalDateTime.now());
                musiqueService.update(musique);
                setFeedback("Musique modifiee avec succes.", true);
            } else {
                musiqueService.add(musique);
                setFeedback("Musique ajoutee avec succes.", true);
            }
            clearForm();
            resetEditMode();
            refreshMusiquesList();
            showListView();
        } catch (SQLDataException e) {
            setFeedback("Erreur lors de l'enregistrement: " + e.getMessage(), false);
        }
    }

    private void clearForm() {
        titreField.clear();
        descriptionArea.clear();
        genreComboBox.getSelectionModel().clearSelection();
        collectionComboBox.getSelectionModel().clearSelection();
        imagePathField.clear();
        audioPathField.clear();
    }

    private void resetEditMode() {
        editingMusiqueId = null;
        editingDateCreation = null;
        editingImageBytes = null;
        editingAudioPath = null;
        updateFormTexts();
    }

    private void startEditMusique(Musique musique) {
        if (musique == null || musique.getId() == null) {
            setFeedback("Impossible de modifier cette musique (id manquant).", false);
            return;
        }

        editingMusiqueId = musique.getId();
        editingDateCreation = musique.getDateCreation();
        editingImageBytes = musique.getImage();
        editingAudioPath = musique.getAudio();

        titreField.setText(musique.getTitre() != null ? musique.getTitre() : "");
        descriptionArea.setText(musique.getDescription() != null ? musique.getDescription() : "");
        genreComboBox.setValue(musique.getGenre());
        imagePathField.clear();
        audioPathField.setText(musique.getAudio() != null ? musique.getAudio() : "");

        collectionComboBox.getSelectionModel().clearSelection();
        if (musique.getCollectionId() != null) {
            for (CollectionChoice choice : collectionComboBox.getItems()) {
                if (choice.getId() == musique.getCollectionId()) {
                    collectionComboBox.getSelectionModel().select(choice);
                    break;
                }
            }
        }

        showFormView();
        updateFormTexts();
        setFeedback("Mode modification: mettez a jour les champs puis enregistrez.", true);
    }

    private void updateFormTexts() {
        boolean editMode = editingMusiqueId != null;
        if (formTitleLabel != null) {
            formTitleLabel.setText(editMode ? "Modifier la musique" : "Ajouter une nouvelle musique");
        }
        if (submitMusiqueButton != null) {
            submitMusiqueButton.setText(editMode ? "Enregistrer" : "Creer");
        }
        if (toggleFormButton != null) {
            if (formSection != null && formSection.isVisible()) {
                toggleFormButton.setText(editMode ? "Annuler la modification" : "Fermer le formulaire");
            } else {
                toggleFormButton.setText("Ajouter une musique");
            }
        }
    }

    private void setFeedback(String message, boolean success) {
        feedbackLabel.setText(message);
        feedbackLabel.setStyle(success ? "-fx-text-fill: #1f7a1f;" : "-fx-text-fill: #b00020;");
    }

    private void loadCollections() {
        List<CollectionChoice> choices = new ArrayList<>();
        String[] queries = {
                "SELECT id, titre FROM collections",
                "SELECT id, titre FROM collection_oeuvre",
                "SELECT id, titre FROM collectionoeuvre",
                "SELECT id, titre FROM collection"
        };

        Connection connection = MyDatabase.getInstance().getConnection();
        if (connection == null) {
            collectionComboBox.setItems(FXCollections.observableArrayList());
            return;
        }

        for (String query : queries) {
            try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(query)) {
                while (resultSet.next()) {
                    choices.add(new CollectionChoice(resultSet.getInt("id"), resultSet.getString("titre")));
                }
                if (!choices.isEmpty()) {
                    collectionComboBox.setItems(FXCollections.observableArrayList(choices));
                    return;
                }
            } catch (SQLException ignored) {
                choices.clear();
            }
        }

        choices.add(new CollectionChoice(0, "Aucune collection disponible"));
        collectionComboBox.setItems(FXCollections.observableArrayList(choices));
        collectionComboBox.getSelectionModel().selectFirst();
    }

    private void refreshMusiquesList() {
        try {
            List<Musique> musiques = musiqueService.getAll();
            tracks.setAll(musiques);

            if (tracks.isEmpty()) {
                currentTrackIndex = -1;
                stopPlayer();
                nowPlayingTitleLabel.setText("Selectionnez une musique");
                nowPlayingMetaLabel.setText("Genre: -");
                playerStatusLabel.setText("Pret");
                playPauseButton.setText("Play");
            } else if (currentTrackIndex >= tracks.size()) {
                currentTrackIndex = -1;
            }

            renderTrackGrid();
            boolean hasData = !tracks.isEmpty();
            emptyListLabel.setVisible(!hasData);
            emptyListLabel.setManaged(!hasData);
            if (hasData && currentTrackIndex < 0) {
                updateNowPlayingLabels(tracks.get(0));
            }
        } catch (SQLDataException e) {
            tracks.clear();
            renderTrackGrid();
            emptyListLabel.setText("Impossible de charger les musiques: " + e.getMessage());
            emptyListLabel.setVisible(true);
            emptyListLabel.setManaged(true);
        }
    }

    private void showListView() {
        formSection.setVisible(false);
        formSection.setManaged(false);
        updateFormTexts();
    }

    private void showFormView() {
        formSection.setVisible(true);
        formSection.setManaged(true);
        updateFormTexts();
    }

    private void playTrackAtIndex(int index, boolean autoPlay) {
        if (index < 0 || index >= tracks.size()) {
            return;
        }

        Musique selectedTrack = tracks.get(index);
        String source = toMediaSource(selectedTrack.getAudio());
        if (source == null) {
            playerStatusLabel.setText("Fichier audio introuvable");
            return;
        }

        stopPlayer();
        try {
            Media media = new Media(source);
            mediaPlayer = new MediaPlayer(media);
            currentTrackIndex = index;
            renderTrackGrid();
            updateNowPlayingLabels(selectedTrack);

            mediaPlayer.setOnReady(() -> {
                if (autoPlay) {
                    mediaPlayer.play();
                    playPauseButton.setText("Pause");
                    playerStatusLabel.setText("Lecture en cours");
                } else {
                    playPauseButton.setText("Play");
                    playerStatusLabel.setText("Pret");
                }
            });
            mediaPlayer.setOnEndOfMedia(this::handleNextTrack);
            mediaPlayer.setOnError(() -> playerStatusLabel.setText(describeMediaError(mediaPlayer.getError())));
        } catch (MediaException mediaException) {
            playPauseButton.setText("Play");
            playerStatusLabel.setText(describeMediaError(mediaException));
        } catch (RuntimeException runtimeException) {
            playPauseButton.setText("Play");
            playerStatusLabel.setText("Impossible de lire ce fichier audio");
        }
    }

    private String describeMediaError(Throwable throwable) {
        if (throwable instanceof MediaException mediaException) {
            if (mediaException.getType() == MediaException.Type.MEDIA_UNSUPPORTED) {
                return "Format non pris en charge par JavaFX (ex: OPUS)";
            }

            String message = mediaException.getMessage();
            if (message != null && message.toLowerCase().contains("unrecognized file signature")) {
                return "Codec audio non pris en charge (essayez MP3/M4A/WAV)";
            }
        }
        return "Impossible de lire ce fichier audio";
    }

    private void updateNowPlayingLabels(Musique musique) {
        String titre = musique.getTitre() != null ? musique.getTitre() : "Sans titre";
        String genre = musique.getGenre() != null ? musique.getGenre() : "-";
        nowPlayingTitleLabel.setText(titre);
        nowPlayingMetaLabel.setText("Genre: " + genre);
    }

    private String toMediaSource(String audioPath) {
        if (audioPath == null || audioPath.isBlank()) {
            return null;
        }

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

    private boolean isPlayableAudioPath(String audioPath) {
        if (audioPath == null || audioPath.isBlank()) {
            return false;
        }

        String normalized = audioPath.trim();
        int questionMarkIndex = normalized.indexOf('?');
        if (questionMarkIndex >= 0) {
            normalized = normalized.substring(0, questionMarkIndex);
        }

        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == normalized.length() - 1) {
            return false;
        }

        String extension = normalized.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        return PLAYABLE_EXTENSIONS.contains(extension);
    }

    private void stopPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }

    private void renderTrackGrid() {
        musiqueGrid.getChildren().clear();
        for (int i = 0; i < tracks.size(); i++) {
            Musique musique = tracks.get(i);
            musiqueGrid.getChildren().add(createTrackCard(musique, i));
        }
    }

    private void handleDeleteMusique(Musique musique) {
        if (musique == null || musique.getId() == null) {
            setFeedback("Impossible de supprimer cette musique (id manquant).", false);
            return;
        }

        try {
            if (currentTrackIndex >= 0 && currentTrackIndex < tracks.size()) {
                Musique current = tracks.get(currentTrackIndex);
                if (current.getId() != null && current.getId().equals(musique.getId())) {
                    stopPlayer();
                    currentTrackIndex = -1;
                    playPauseButton.setText("Play");
                    playerStatusLabel.setText("Pret");
                }
            }

            musiqueService.delete(musique);
            setFeedback("Musique supprimee avec succes.", true);

            if (editingMusiqueId != null && editingMusiqueId.equals(musique.getId())) {
                clearForm();
                resetEditMode();
                showListView();
            }

            refreshMusiquesList();
        } catch (SQLDataException e) {
            setFeedback("Erreur lors de la suppression: " + e.getMessage(), false);
        }
    }

    private boolean confirmDeleteMusique(Musique musique) {
        String titre = musique != null && musique.getTitre() != null ? musique.getTitre() : "cette musique";

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmer la suppression");
        alert.setHeaderText("Supprimer \"" + titre + "\" ?");
        alert.setContentText("Cette action est definitive.");

        return alert.showAndWait()
                .filter(buttonType -> buttonType == ButtonType.OK)
                .isPresent();
    }

    private VBox createTrackCard(Musique musique, int index) {
        VBox card = new VBox(6);
        card.setPrefWidth(170);
        card.setMaxWidth(170);
        card.setStyle(index == currentTrackIndex
                ? "-fx-background-color: #202020; -fx-background-radius: 8; -fx-border-color: #1DB954; -fx-border-radius: 8; -fx-padding: 8;"
                : "-fx-background-color: #202020; -fx-background-radius: 8; -fx-padding: 8;");

        Node coverNode = buildCoverNode(musique.getImage());

        String titre = musique.getTitre() != null ? musique.getTitre() : "Sans titre";
        Label titleLabel = new Label(titre);
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold;");

        Button menuButton = new Button("⋮");
        menuButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 18px; -fx-padding: 0 6 0 6; -fx-cursor: hand;");

        MenuItem editItem = new MenuItem("Modifier");
        editItem.setOnAction(event -> {
            event.consume();
            startEditMusique(musique);
        });

        MenuItem deleteItem = new MenuItem("Supprimer");

        ContextMenu actionsMenu = new ContextMenu(editItem, deleteItem);

        menuButton.setOnMouseClicked(event -> {
            event.consume();
            if (actionsMenu.isShowing()) {
                actionsMenu.hide();
            } else {
                actionsMenu.show(menuButton, Side.BOTTOM, 0, 0);
            }
        });

        deleteItem.setOnAction(event -> {
            event.consume();
            if (confirmDeleteMusique(musique)) {
                handleDeleteMusique(musique);
            }
        });

        HBox titleRow = new HBox(8, titleLabel, menuButton);

        String genre = musique.getGenre() != null ? musique.getGenre() : "-";
        Label metaLabel = new Label("Genre: " + genre);
        metaLabel.setStyle("-fx-text-fill: #b0b0b0;");

        card.getChildren().addAll(coverNode, titleRow, metaLabel);
        card.setOnMouseClicked(event -> playTrackAtIndex(index, true));
        return card;
    }

    private Node buildCoverNode(byte[] imageBytes) {
        StackPane placeholder = new StackPane();
        placeholder.setPrefSize(150, 150);
        placeholder.setStyle("-fx-background-color: #2b2b2b; -fx-background-radius: 6;");

        if (imageBytes == null || imageBytes.length == 0) {
            Label noImageLabel = new Label("No cover");
            noImageLabel.setStyle("-fx-text-fill: #8a8a8a;");
            placeholder.getChildren().add(noImageLabel);
            return placeholder;
        }

        try {
            Image image = new Image(new ByteArrayInputStream(imageBytes));
            if (image.isError()) {
                Label noImageLabel = new Label("No cover");
                noImageLabel.setStyle("-fx-text-fill: #8a8a8a;");
                placeholder.getChildren().add(noImageLabel);
                return placeholder;
            }

            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(150);
            imageView.setFitHeight(150);
            imageView.setPreserveRatio(false);
            return imageView;
        } catch (Exception ex) {
            Label noImageLabel = new Label("No cover");
            noImageLabel.setStyle("-fx-text-fill: #8a8a8a;");
            placeholder.getChildren().add(noImageLabel);
            return placeholder;
        }
    }

    public static final class CollectionChoice {
        private final int id;
        private final String titre;

        public CollectionChoice(int id, String titre) {
            this.id = id;
            this.titre = titre != null ? titre : ("Collection " + id);
        }

        public int getId() {
            return id;
        }

        @Override
        public String toString() {
            return titre + " (#" + id + ")";
        }
    }
}
