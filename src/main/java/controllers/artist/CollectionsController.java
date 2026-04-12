package controllers.artist;

import Services.OeuvreCollectionService;
import entities.CollectionOeuvre;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CollectionsController {
    private static final int DESCRIPTION_MIN_LENGTH = 10;
    private static final int DESCRIPTION_MAX_LENGTH = 500;


    @FXML
    private TextField searchField;

    @FXML
    private VBox collectionsContainer;

    @FXML
    private Label emptyStateLabel;

    private final OeuvreCollectionService oeuvreCollectionService = new OeuvreCollectionService();
    private final List<CollectionOeuvre> allCollections = new ArrayList<>();

    // TODO: brancher l'ID depuis la session utilisateur quand elle sera disponible.
    private final int artisteId = 3;

    @FXML
    public void initialize() {
        loadCollections();
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilter(newValue));
    }

    @FXML
    private void onSearchClick() {
        applyFilter(searchField.getText());
    }

    @FXML
    private void onAddCollectionClick() {
        showAddCollectionPopup();
    }

    private void showAddCollectionPopup() {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initOwner(searchField.getScene().getWindow());
        popupStage.setTitle("Ajouter une collection");

        Label headerLabel = new Label("Ajouter une Collection");
        headerLabel.getStyleClass().add("popup-title");

        Label titreLabel = new Label("Titre");
        titreLabel.getStyleClass().add("popup-field-label");
        TextField titreField = new TextField();
        titreField.setPromptText("Entrez le titre");
        titreField.getStyleClass().add("popup-input");

        Label titreErrorLabel = new Label();
        titreErrorLabel.getStyleClass().add("popup-error");
        titreErrorLabel.setVisible(false);
        titreErrorLabel.setManaged(false);

        Label descriptionLabel = new Label("Description");
        descriptionLabel.getStyleClass().add("popup-field-label");
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Entrez la description");
        descriptionArea.getStyleClass().add("popup-input");
        descriptionArea.setPrefRowCount(4);

        Label descriptionErrorLabel = new Label();
        descriptionErrorLabel.getStyleClass().add("popup-error");
        descriptionErrorLabel.setVisible(false);
        descriptionErrorLabel.setManaged(false);

        Label descriptionCounterLabel = new Label("0/" + DESCRIPTION_MAX_LENGTH);
        descriptionCounterLabel.getStyleClass().add("popup-counter");

        HBox descriptionMetaRow = new HBox(8);
        descriptionMetaRow.setAlignment(Pos.CENTER_LEFT);
        Region descriptionMetaSpacer = new Region();
        HBox.setHgrow(descriptionMetaSpacer, Priority.ALWAYS);
        descriptionMetaRow.getChildren().addAll(descriptionErrorLabel, descriptionMetaSpacer, descriptionCounterLabel);

        titreField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.trim().isEmpty()) {
                clearFieldError(titreErrorLabel, titreField);
            }
        });

        descriptionArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.length() > DESCRIPTION_MAX_LENGTH) {
                descriptionArea.setText(newValue.substring(0, DESCRIPTION_MAX_LENGTH));
                return;
            }
            int length = descriptionArea.getText() == null ? 0 : descriptionArea.getText().length();
            descriptionCounterLabel.setText(length + "/" + DESCRIPTION_MAX_LENGTH);
            descriptionCounterLabel.getStyleClass().remove("popup-counter-limit");
            if (length >= DESCRIPTION_MAX_LENGTH) {
                descriptionCounterLabel.getStyleClass().add("popup-counter-limit");
            }
            if (!descriptionErrorLabel.getText().isEmpty()) {
                validateDescriptionField(descriptionArea, descriptionErrorLabel);
            }
        });

        Button closeButton = new Button("Fermer");
        closeButton.getStyleClass().add("popup-close-button");
        closeButton.setOnAction(event -> popupStage.close());

        Button confirmButton = new Button("Ajouter Collection");
        confirmButton.getStyleClass().add("popup-confirm-button");
        confirmButton.setOnAction(event -> {
            String titre = titreField.getText() == null ? "" : titreField.getText().trim();
            String description = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();

            clearFieldError(titreErrorLabel, titreField);
            clearFieldError(descriptionErrorLabel, descriptionArea);

            boolean hasErrors = false;

            if (titre.isEmpty()) {
                showFieldError(titreErrorLabel, titreField, "Le titre est obligatoire.");
                hasErrors = true;
            }

            if (!validateDescriptionField(descriptionArea, descriptionErrorLabel)) {
                hasErrors = true;
            }

            if (hasErrors) {
                return;
            }

            try {
                if (oeuvreCollectionService.existsByTitreAndArtisteId(titre, artisteId)) {
                    showFieldError(titreErrorLabel, titreField, "Cette collection existe deja pour cet artiste.");
                    return;
                }

                CollectionOeuvre collection = new CollectionOeuvre();
                collection.setTitre(titre);
                collection.setDescription(description.isEmpty() ? null : description);
                collection.setArtisteId(artisteId);
                oeuvreCollectionService.add(collection);

                popupStage.close();
                loadCollections();
                applyFilter(searchField.getText());
            } catch (SQLException e) {
                String message = e.getMessage() == null ? "Une erreur est survenue." : e.getMessage();
                String messageLower = message.toLowerCase();
                if (messageLower.contains("description")) {
                    showFieldError(descriptionErrorLabel, descriptionArea, message);
                } else {
                    showFieldError(titreErrorLabel, titreField, message);
                }
            }
        });

        HBox footer = new HBox(10, closeButton, confirmButton);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(10,
                headerLabel,
                titreLabel,
                titreField,
                titreErrorLabel,
                descriptionLabel,
                descriptionArea,
                descriptionMetaRow,
                footer
        );
        root.setPadding(new Insets(16));
        root.getStyleClass().add("collection-popup");

        Scene scene = new Scene(root, 500, 410);
        scene.getStylesheets().addAll(searchField.getScene().getStylesheets());

        popupStage.setScene(scene);
        popupStage.showAndWait();
    }

    private boolean validateDescriptionField(TextArea descriptionArea, Label descriptionErrorLabel) {
        String description = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();
        if (description.isEmpty()) {
            showFieldError(descriptionErrorLabel, descriptionArea, "La description est obligatoire.");
            return false;
        }
        if (description.length() < DESCRIPTION_MIN_LENGTH) {
            showFieldError(descriptionErrorLabel, descriptionArea, "La description doit contenir au moins " + DESCRIPTION_MIN_LENGTH + " caracteres.");
            return false;
        }
        if (description.length() > DESCRIPTION_MAX_LENGTH) {
            showFieldError(descriptionErrorLabel, descriptionArea, "La description ne doit pas depasser " + DESCRIPTION_MAX_LENGTH + " caracteres.");
            return false;
        }
        clearFieldError(descriptionErrorLabel, descriptionArea);
        return true;
    }

    private void showFieldError(Label errorLabel, Control field, String message) {
        errorLabel.setText(message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
        field.getStyleClass().remove("popup-input-valid");
        if (!field.getStyleClass().contains("popup-input-invalid")) {
            field.getStyleClass().add("popup-input-invalid");
        }
    }

    private void clearFieldError(Label errorLabel, Control field) {
        errorLabel.setText("");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
        field.getStyleClass().remove("popup-input-invalid");
        if (!field.getStyleClass().contains("popup-input-valid")) {
            field.getStyleClass().add("popup-input-valid");
        }
    }

    private void loadCollections() {
        try {
            allCollections.clear();
            allCollections.addAll(oeuvreCollectionService.getCollectionsByArtisteId(artisteId));
            renderCollections(allCollections);
        } catch (SQLException e) {
            emptyStateLabel.setText("Erreur lors du chargement des collections: " + e.getMessage());
            emptyStateLabel.setVisible(true);
            collectionsContainer.getChildren().clear();
        }
    }

    private void applyFilter(String keyword) {
        String query = keyword == null ? "" : keyword.trim().toLowerCase();
        if (query.isEmpty()) {
            renderCollections(allCollections);
            return;
        }

        List<CollectionOeuvre> filtered = new ArrayList<>();
        for (CollectionOeuvre collection : allCollections) {
            String titre = collection.getTitre() == null ? "" : collection.getTitre().toLowerCase();
            String description = collection.getDescription() == null ? "" : collection.getDescription().toLowerCase();
            if (titre.contains(query) || description.contains(query)) {
                filtered.add(collection);
            }
        }
        renderCollections(filtered);
    }

    private void renderCollections(List<CollectionOeuvre> collections) {
        collectionsContainer.getChildren().clear();

        if (collections.isEmpty()) {
            emptyStateLabel.setText("Aucune collection trouvee pour cet artiste.");
            emptyStateLabel.setVisible(true);
            return;
        }

        emptyStateLabel.setVisible(false);

        for (int i = 0; i < collections.size(); i++) {
            CollectionOeuvre collection = collections.get(i);

            HBox row = new HBox(10);
            row.getStyleClass().add("collection-row");

            VBox textBox = new VBox(5);
            Label titleLabel = new Label(collection.getTitre() == null ? "(Sans titre)" : collection.getTitre());
            titleLabel.getStyleClass().add("collection-title");

            String description = collection.getDescription();
            Label descriptionLabel = new Label(description == null || description.isBlank() ? "Aucune description." : description);
            descriptionLabel.getStyleClass().add("collection-description");
            descriptionLabel.setWrapText(true);

            textBox.getChildren().addAll(titleLabel, descriptionLabel);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox actions = new HBox(12);
            actions.getStyleClass().add("collection-actions");

            Label menuLabel = new Label("...");
            menuLabel.getStyleClass().add("collection-action");

            Label expandLabel = new Label("v");
            expandLabel.getStyleClass().add("collection-action");

            actions.getChildren().addAll(menuLabel, expandLabel);
            row.getChildren().addAll(textBox, spacer, actions);
            collectionsContainer.getChildren().add(row);

            if (i < collections.size() - 1) {
                Separator separator = new Separator();
                separator.getStyleClass().add("collection-separator");
                collectionsContainer.getChildren().add(separator);
            }
        }
    }
}
