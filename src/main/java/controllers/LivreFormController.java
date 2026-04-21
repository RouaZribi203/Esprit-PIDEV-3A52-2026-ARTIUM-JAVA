package controllers;

import entities.CollectionOeuvre;
import entities.Livre;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class LivreFormController {

    @FXML
    private Label formTitleLabel;

    @FXML
    private TextField titreField;

    @FXML
    private TextField categorieField;

    @FXML
    private TextField prixField;

    @FXML
    private ComboBox<CollectionOeuvre> collectionComboBox;

    @FXML
    private ImageView couvertureImageView;

    @FXML
    private Label couvertureLabel;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private Label pdfLabel;

    @FXML
    private Label validationErrorLabel;

    private Livre originalLivre;
    private Livre resultLivre;
    private String selectedImagePath;
    private String selectedPdfPath;

    @FXML
    public void initialize() {
        clearValidationError();

        prixField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                prixField.setText(oldValue);
            }
        });

        collectionComboBox.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(CollectionOeuvre item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getId() + " - " + item.getTitre());
                }
            }
        });
        collectionComboBox.setButtonCell(collectionComboBox.getCellFactory().call(null));
    }

    public void setCollections(List<CollectionOeuvre> collections) {
        collectionComboBox.setItems(FXCollections.observableArrayList(collections));
    }

    public void setLivre(Livre livre) {
        originalLivre = livre;
        resultLivre = null;
        selectedImagePath = null;
        selectedPdfPath = null;
        clearValidationError();

        if (livre == null) {
            formTitleLabel.setText("Ajouter un livre");
            titreField.clear();
            categorieField.clear();
            prixField.clear();
            descriptionArea.clear();
            collectionComboBox.setValue(null);
            couvertureImageView.setImage(null);
            couvertureLabel.setText("Aucun fichier choisi");
            pdfLabel.setText("Aucun fichier choisi");
            return;
        }

        formTitleLabel.setText("Modifier le livre");
        titreField.setText(livre.getTitre() == null ? "" : livre.getTitre());
        categorieField.setText(livre.getCategorie() == null ? "" : livre.getCategorie());
        prixField.setText(livre.getPrixLocation() == null ? "" : String.valueOf(livre.getPrixLocation()));
        descriptionArea.setText(livre.getDescription() == null ? "" : livre.getDescription());

        CollectionOeuvre selectedCollection = collectionComboBox.getItems().stream()
                .filter(item -> item.getId().equals(livre.getCollectionId()))
                .findFirst()
                .orElse(null);
        collectionComboBox.setValue(selectedCollection);

        if (livre.getImage() != null && !livre.getImage().isBlank()) {
            couvertureImageView.setImage(toImage(livre.getImage()));
            couvertureLabel.setText("(Image actuelle)");
        } else {
            couvertureImageView.setImage(null);
            couvertureLabel.setText("Aucune image");
        }

        if (livre.getFichierPdf() != null && !livre.getFichierPdf().isBlank()) {
            pdfLabel.setText("(PDF actuel)");
        } else {
            pdfLabel.setText("Aucun PDF");
        }
    }

    public Livre getResultLivre() {
        return resultLivre;
    }

    @FXML
    private void onChooseImageClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image de couverture");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers Image", "*.png", "*.jpg", "*.jpeg"));

        File file = fileChooser.showOpenDialog(getCurrentStage());
        if (file == null) {
            return;
        }

        try {
            selectedImagePath = file.getAbsolutePath();
            couvertureImageView.setImage(toImage(selectedImagePath));
            couvertureLabel.setText(file.getName());
            clearValidationError();
        } catch (IllegalArgumentException e) {
            showValidationError("Impossible de lire l'image selectionnee.");
        }
    }

    @FXML
    private void onChoosePdfClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un fichier PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));

        File file = fileChooser.showOpenDialog(getCurrentStage());
        if (file == null) {
            return;
        }

        selectedPdfPath = file.getAbsolutePath();
        pdfLabel.setText(file.getName());
        clearValidationError();
    }

    @FXML
    private void onSaveClick() {
        clearValidationError();

        String titre = titreField.getText() == null ? "" : titreField.getText().trim();
        String categorie = categorieField.getText() == null ? "" : categorieField.getText().trim();
        String prixText = prixField.getText() == null ? "" : prixField.getText().trim();
        String description = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();

        boolean hasError = false;
        StringBuilder errorMessage = new StringBuilder("Veuillez corriger les erreurs suivantes :\n");

        if (titre.length() < 3) {
            errorMessage.append("- Le titre doit contenir au moins 3 caracteres.\n");
            hasError = true;
        }

        if (categorie.isEmpty()) {
            errorMessage.append("- La categorie est obligatoire.\n");
            hasError = true;
        }

        Double prixLocation = null;
        try {
            prixLocation = Double.parseDouble(prixText);
            if (prixLocation < 0) {
                throw new NumberFormatException("negative");
            }
        } catch (Exception e) {
            errorMessage.append("- Le prix doit etre un nombre positif.\n");
            hasError = true;
        }

        if (collectionComboBox.getValue() == null) {
            errorMessage.append("- Veuillez selectionner une collection.\n");
            hasError = true;
        }

        if (description.isEmpty()) {
            errorMessage.append("- La description est obligatoire.\n");
            hasError = true;
        }

        boolean hasImage = selectedImagePath != null && !selectedImagePath.isBlank()
                || (originalLivre != null && originalLivre.getImage() != null && !originalLivre.getImage().isBlank());
        if (!hasImage) {
            errorMessage.append("- Une image de couverture est obligatoire.\n");
            hasError = true;
        }

        boolean hasPdf = (selectedPdfPath != null && !selectedPdfPath.isBlank())
                || (originalLivre != null && originalLivre.getFichierPdf() != null && !originalLivre.getFichierPdf().isBlank());
        if (!hasPdf) {
            errorMessage.append("- Un fichier PDF est obligatoire.\n");
            hasError = true;
        }

        if (hasError) {
            showValidationError(errorMessage.toString());
            return;
        }

        Livre livre = originalLivre != null ? originalLivre : new Livre();
        livre.setTitre(titre);
        livre.setCategorie(categorie);
        livre.setPrixLocation(prixLocation);
        livre.setDescription(description);
        livre.setCollectionId(collectionComboBox.getValue().getId());

        if (selectedImagePath != null && !selectedImagePath.isBlank()) {
            livre.setImage(selectedImagePath);
        }
        if (selectedPdfPath != null && !selectedPdfPath.isBlank()) {
            livre.setFichierPdf(selectedPdfPath);
        }

        resultLivre = livre;
        closeDialog();
    }

    @FXML
    private void onCancelClick() {
        resultLivre = null;
        closeDialog();
    }

    private Stage getCurrentStage() {
        return (Stage) formTitleLabel.getScene().getWindow();
    }

    private void closeDialog() {
        Stage stage = getCurrentStage();
        if (stage != null) {
            stage.close();
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

    private Image toImage(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        if (source.startsWith("http://") || source.startsWith("https://") || source.startsWith("file:")) {
            return new Image(source, true);
        }
        return new Image(new File(source).toURI().toString(), true);
    }
}

