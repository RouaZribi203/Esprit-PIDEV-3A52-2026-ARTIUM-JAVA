package controllers.artist;

import Services.MusiqueService;
import entities.Musique;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
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
import java.util.ArrayList;
import java.util.List;

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

    @FXML
    public void initialize() {
        genreComboBox.setItems(genres);
        loadCollections();
        feedbackLabel.setText("");
        showListView();
        refreshMusiquesList();
    }

    @FXML
    private void handleToggleForm() {
        if (formSection.isVisible()) {
            showListView();
        } else {
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
                new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav", "*.aac", "*.m4a", "*.flac", "*.ogg", "*.wma")
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

        if (titre.length() < 3 || titre.length() > 255) {
            setFeedback("Le titre doit contenir entre 3 et 255 caracteres.", false);
            return;
        }

        if (description.length() < 10 || description.length() > 5000) {
            setFeedback("La description doit contenir entre 10 et 5000 caracteres.", false);
            return;
        }

        if (genre.isEmpty() || audioPath.isEmpty()) {
            setFeedback("Veuillez renseigner le genre et le fichier audio.", false);
            return;
        }

        Musique musique = new Musique();
        musique.setTitre(titre);
        musique.setDescription(description);
        musique.setDateCreation(LocalDate.now());
        musique.setGenre(genre);
        musique.setCollectionId(collectionChoice != null && collectionChoice.getId() > 0 ? collectionChoice.getId() : null);
        musique.setAudio(audioPath);

        if (!imagePath.isEmpty()) {
            try {
                musique.setImage(Files.readAllBytes(new File(imagePath).toPath()));
            } catch (IOException e) {
                setFeedback("Impossible de lire l'image selectionnee.", false);
                return;
            }
        }

        try {
            musiqueService.add(musique);
            clearForm();
            setFeedback("Musique ajoutee avec succes.", true);
            refreshMusiquesList();
            showListView();
        } catch (SQLDataException e) {
            setFeedback("Erreur lors de l'ajout: " + e.getMessage(), false);
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
        toggleFormButton.setText("Ajouter une musique");
    }

    private void showFormView() {
        formSection.setVisible(true);
        formSection.setManaged(true);
        toggleFormButton.setText("Fermer le formulaire");
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
        mediaPlayer.setOnError(() -> playerStatusLabel.setText("Impossible de lire ce fichier audio"));
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

        String genre = musique.getGenre() != null ? musique.getGenre() : "-";
        Label metaLabel = new Label("Genre: " + genre);
        metaLabel.setStyle("-fx-text-fill: #b0b0b0;");

        card.getChildren().addAll(coverNode, titleLabel, metaLabel);
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
