package controllers.artist;

import entities.Evenement;
import entities.Galerie;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.GalerieService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLDataException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class EvenementArtisteFormController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private Label formTitleLabel;

    @FXML
    private TextField titreField;

    @FXML
    private ComboBox<String> typeComboBox;

    @FXML
    private TextField dateDebutField;

    @FXML
    private TextField dateFinField;

    @FXML
    private TextField capaciteField;

    @FXML
    private TextField prixField;

    @FXML
    private ComboBox<GalerieOption> galerieComboBox;

    @FXML
    private Label imageFileLabel;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private Label validationErrorLabel;

    private final GalerieService galerieService = new GalerieService();
    private final List<GalerieOption> galerieOptions = new ArrayList<>();

    private Stage dialogStage;
    private Evenement originalEvenement;
    private Evenement resultEvenement;
    private byte[] selectedImageBytes;

    @FXML
    public void initialize() {
        typeComboBox.getItems().addAll("Exposition", "Concert", "Spectacle", "Conference", "Atelier", "Autre");
        typeComboBox.setValue("Exposition");

        clearValidationError();
        wireFieldListeners();
        loadGaleries();
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setEvenement(Evenement evenement) {
        this.originalEvenement = evenement;
        clearValidationError();

        if (evenement == null) {
            formTitleLabel.setText("Ajouter un evenement");
            titreField.clear();
            typeComboBox.setValue("Exposition");
            dateDebutField.clear();
            dateFinField.clear();
            capaciteField.clear();
            prixField.clear();
            if (!galerieOptions.isEmpty()) {
                galerieComboBox.setValue(galerieOptions.get(0));
            }
            descriptionArea.clear();
            imageFileLabel.setText("Aucun fichier choisi");
            selectedImageBytes = null;
            return;
        }

        formTitleLabel.setText("Modifier un evenement");
        titreField.setText(evenement.getTitre());
        typeComboBox.setValue(evenement.getType() == null || evenement.getType().isBlank() ? "Exposition" : evenement.getType());
        dateDebutField.setText(formatDateTime(evenement.getDateDebut()));
        dateFinField.setText(formatDateTime(evenement.getDateFin()));
        capaciteField.setText(evenement.getCapaciteMax() == null ? "" : String.valueOf(evenement.getCapaciteMax()));
        prixField.setText(evenement.getPrixTicket() == null ? "" : String.valueOf(evenement.getPrixTicket()));
        descriptionArea.setText(evenement.getDescription());
        imageFileLabel.setText(evenement.getImageCouverture() == null || evenement.getImageCouverture().length == 0
                ? "Aucun fichier choisi"
                : "Image existante" );
        selectedImageBytes = evenement.getImageCouverture();

        GalerieOption selected = findGalerieOption(evenement.getGalerieId());
        if (selected != null) {
            galerieComboBox.setValue(selected);
        }
    }

    public Evenement getResultEvenement() {
        return resultEvenement;
    }

    @FXML
    private void onChooseImageClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir image de couverture");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        File selectedFile = fileChooser.showOpenDialog(dialogStage);
        if (selectedFile == null) {
            return;
        }

        try {
            selectedImageBytes = Files.readAllBytes(selectedFile.toPath());
            imageFileLabel.setText(selectedFile.getName());
            clearValidationError();
        } catch (IOException e) {
            showValidationError("Impossible de lire le fichier image choisi.");
        }
    }

    @FXML
    private void onSaveClick() {
        clearValidationError();

        String titre = safeTrim(titreField.getText());
        String type = safeTrim(typeComboBox.getValue());
        String dateDebutText = safeTrim(dateDebutField.getText());
        String dateFinText = safeTrim(dateFinField.getText());
        String capaciteText = safeTrim(capaciteField.getText());
        String prixText = safeTrim(prixField.getText());
        String description = safeTrim(descriptionArea.getText());
        GalerieOption selectedGalerie = galerieComboBox.getValue();

        if (titre.isEmpty() || type.isEmpty() || dateDebutText.isEmpty() || dateFinText.isEmpty() || selectedGalerie == null) {
            showValidationError("Veuillez remplir les champs obligatoires.");
            return;
        }

        LocalDateTime dateDebut = parseDateTime(dateDebutText, "Date debut invalide. Format attendu: dd/MM/yyyy HH:mm");
        if (dateDebut == null) {
            return;
        }

        LocalDateTime dateFin = parseDateTime(dateFinText, "Date fin invalide. Format attendu: dd/MM/yyyy HH:mm");
        if (dateFin == null) {
            return;
        }

        if (dateFin.isBefore(dateDebut)) {
            showValidationError("La date fin doit etre apres la date debut.");
            return;
        }

        Integer capacite = null;
        if (!capaciteText.isEmpty()) {
            try {
                capacite = Integer.parseInt(capaciteText);
                if (capacite <= 0) {
                    showValidationError("La capacite doit etre superieure a 0.");
                    return;
                }
            } catch (NumberFormatException e) {
                showValidationError("La capacite max doit etre un nombre entier.");
                return;
            }
        }

        Double prixTicket = null;
        if (!prixText.isEmpty()) {
            try {
                prixTicket = Double.parseDouble(prixText.replace(',', '.'));
                if (prixTicket < 0) {
                    showValidationError("Le prix ticket doit etre positif.");
                    return;
                }
            } catch (NumberFormatException e) {
                showValidationError("Le prix ticket doit etre un nombre valide.");
                return;
            }
        }

        Evenement evenement = new Evenement();
        if (originalEvenement != null) {
            evenement.setId(originalEvenement.getId());
            evenement.setArtisteId(originalEvenement.getArtisteId());
        }

        evenement.setTitre(titre);
        evenement.setType(type);
        evenement.setDateDebut(dateDebut);
        evenement.setDateFin(dateFin);
        evenement.setDateCreation(originalEvenement != null && originalEvenement.getDateCreation() != null
                ? originalEvenement.getDateCreation()
                : LocalDate.now());
        evenement.setDescription(description);
        evenement.setCapaciteMax(capacite);
        evenement.setPrixTicket(prixTicket);
        evenement.setGalerieId(selectedGalerie.id());
        evenement.setImageCouverture(selectedImageBytes);
        evenement.setStatut(computeStatut(dateDebut, dateFin));

        resultEvenement = evenement;
        closeDialog();
    }

    @FXML
    private void onCancelClick() {
        resultEvenement = null;
        closeDialog();
    }

    private void loadGaleries() {
        try {
            galerieOptions.clear();
            for (Galerie galerie : galerieService.getAll()) {
                galerieOptions.add(new GalerieOption(galerie.getId(), galerie.getNom()));
            }
            galerieComboBox.getItems().setAll(galerieOptions);
            if (!galerieOptions.isEmpty()) {
                galerieComboBox.setValue(galerieOptions.get(0));
            }
        } catch (SQLDataException e) {
            showValidationError("Impossible de charger la liste des galeries.");
        }
    }

    private GalerieOption findGalerieOption(Integer galerieId) {
        if (galerieId == null) {
            return null;
        }
        for (GalerieOption option : galerieOptions) {
            if (galerieId.equals(option.id())) {
                return option;
            }
        }
        return null;
    }

    private void wireFieldListeners() {
        titreField.textProperty().addListener((obs, oldValue, newValue) -> clearValidationError());
        dateDebutField.textProperty().addListener((obs, oldValue, newValue) -> clearValidationError());
        dateFinField.textProperty().addListener((obs, oldValue, newValue) -> clearValidationError());
        capaciteField.textProperty().addListener((obs, oldValue, newValue) -> clearValidationError());
        prixField.textProperty().addListener((obs, oldValue, newValue) -> clearValidationError());
        descriptionArea.textProperty().addListener((obs, oldValue, newValue) -> clearValidationError());
        typeComboBox.valueProperty().addListener((obs, oldValue, newValue) -> clearValidationError());
        galerieComboBox.valueProperty().addListener((obs, oldValue, newValue) -> clearValidationError());
    }

    private LocalDateTime parseDateTime(String text, String errorMessage) {
        try {
            return LocalDateTime.parse(text, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            showValidationError(errorMessage);
            return null;
        }
    }

    private String computeStatut(LocalDateTime debut, LocalDateTime fin) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(debut)) {
            return "A venir";
        }
        if (now.isAfter(fin)) {
            return "Termine";
        }
        return "En cours";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : DATE_TIME_FORMATTER.format(dateTime);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private void showValidationError(String message) {
        validationErrorLabel.setText(message);
        validationErrorLabel.setVisible(true);
        validationErrorLabel.setManaged(true);
    }

    private void clearValidationError() {
        if (validationErrorLabel == null) {
            return;
        }
        validationErrorLabel.setText("");
        validationErrorLabel.setVisible(false);
        validationErrorLabel.setManaged(false);
    }

    private record GalerieOption(Integer id, String nom) {
        @Override
        public String toString() {
            return nom == null || nom.isBlank() ? "Galerie #" + id : nom;
        }
    }
}

