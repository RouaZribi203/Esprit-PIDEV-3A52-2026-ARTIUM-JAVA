package controllers;

import entities.Galerie;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class GalerieFormController {

    @FXML
    private Label formTitleLabel;

    @FXML
    private TextField nomField;

    @FXML
    private TextField adresseField;

    @FXML
    private TextField localisationField;

    @FXML
    private TextField capaciteField;

    @FXML
    private TextArea descriptionArea;

    private Stage dialogStage;
    private Galerie originalGalerie;
    private Galerie resultGalerie;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setGalerie(Galerie galerie) {
        this.originalGalerie = galerie;
        if (galerie == null) {
            formTitleLabel.setText("Ajouter une galerie");
            return;
        }

        formTitleLabel.setText("Modifier la galerie");
        nomField.setText(galerie.getNom());
        adresseField.setText(galerie.getAdresse());
        localisationField.setText(galerie.getLocalisation());
        capaciteField.setText(galerie.getCapaciteMax() == null ? "" : String.valueOf(galerie.getCapaciteMax()));
        descriptionArea.setText(galerie.getDescription());
    }

    public Galerie getResultGalerie() {
        return resultGalerie;
    }

    @FXML
    private void onSaveClick() {
        String nom = nomField.getText() == null ? "" : nomField.getText().trim();
        String adresse = adresseField.getText() == null ? "" : adresseField.getText().trim();
        String localisation = localisationField.getText() == null ? "" : localisationField.getText().trim();
        String capaciteText = capaciteField.getText() == null ? "" : capaciteField.getText().trim();
        String description = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();

        if (nom.isEmpty() || adresse.isEmpty() || localisation.isEmpty() || capaciteText.isEmpty()) {
            showValidationAlert("Veuillez remplir les champs obligatoires.");
            return;
        }

        int capacite;
        try {
            capacite = Integer.parseInt(capaciteText);
            if (capacite <= 0) {
                showValidationAlert("La capacite doit etre superieure a 0.");
                return;
            }
        } catch (NumberFormatException e) {
            showValidationAlert("La capacite max doit etre un nombre entier.");
            return;
        }

        Galerie galerie = new Galerie();
        if (originalGalerie != null) {
            galerie.setId(originalGalerie.getId());
        }
        galerie.setNom(nom);
        galerie.setAdresse(adresse);
        galerie.setLocalisation(localisation);
        galerie.setDescription(description);
        galerie.setCapaciteMax(capacite);

        resultGalerie = galerie;
        closeDialog();
    }

    @FXML
    private void onCancelClick() {
        resultGalerie = null;
        closeDialog();
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private void showValidationAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation");
        alert.setHeaderText("Formulaire invalide");
        alert.setContentText(message);
        alert.showAndWait();
    }
}

