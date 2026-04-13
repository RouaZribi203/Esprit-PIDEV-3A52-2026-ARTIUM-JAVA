package controllers.artist;

import Services.MusiqueService;
import entities.Musique;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import utils.MyDatabase;

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

    private final MusiqueService musiqueService = new MusiqueService();
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

    @FXML
    public void initialize() {
        genreComboBox.setItems(genres);
        loadCollections();
        feedbackLabel.setText("");
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
            setFeedback("Veuillez renseigner le genre, la collection et le fichier audio.", false);
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
