package controllers.artist;

import Services.OeuvreCollectionService;
import Services.OeuvreService.ArtistIdentity;
import Services.OeuvreService;
import Services.OeuvreService.OeuvreFeedItem;
import entities.CollectionOeuvre;
import entities.Oeuvre;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.Optional;

public class MesOeuvresController {

    private static final String TYPE_ALL = "Tous les types";
    private static final int DESCRIPTION_MIN_LENGTH = 10;
    private static final int DESCRIPTION_MAX_LENGTH = 500;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRANCE);
    private static final double POST_IMAGE_MAX_WIDTH = 760;
    private static final double POST_IMAGE_MAX_HEIGHT = 360;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> typeCombo;

    @FXML
    private Button addOeuvreButton;

    @FXML
    private VBox oeuvresContainer;

    @FXML
    private Label emptyStateLabel;

    private final OeuvreService oeuvreService = new OeuvreService();
    private final OeuvreCollectionService oeuvreCollectionService = new OeuvreCollectionService();
    private final List<OeuvreFeedItem> allFeedItems = new ArrayList<>();
    private String artistDisplayName = "Artiste";
    private String artistSpecialite = "Specialite inconnue";

    // TODO: brancher l'ID depuis la session utilisateur quand elle sera disponible.
    private final int artisteId = 3;

    @FXML
    public void initialize() {
        applyIcons();
        loadArtistIdentity();
        typeCombo.setItems(FXCollections.observableArrayList(TYPE_ALL));
        typeCombo.setValue(TYPE_ALL);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        typeCombo.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());

        loadFeed();
    }

    private void loadArtistIdentity() {
        try {
            ArtistIdentity identity = oeuvreService.getArtisteIdentityById(artisteId);
            artistDisplayName = safeText(identity.getFullName()).isEmpty() ? "Artiste" : identity.getFullName();
            artistSpecialite = safeText(identity.getSpecialite()).isEmpty() ? "Specialite inconnue" : identity.getSpecialite();
        } catch (Exception ignored) {
            artistDisplayName = "Artiste";
            artistSpecialite = "Specialite inconnue";
        }
    }

    @FXML
    private void onSearchClick() {
        applyFilters();
    }

    @FXML
    private void onFilterClick() {
        applyFilters();
    }

    @FXML
    private void onAddOeuvreClick() {
        showOeuvrePopup(null);
    }

    private void applyIcons() {
        if (addOeuvreButton != null) {
            addOeuvreButton.setGraphic(createColoredIcon("M19 11H13V5h-2v6H5v2h6v6h2v-6h6z", 0.72, "#3f44d4"));
            addOeuvreButton.setGraphicTextGap(6);
        }
    }

    private void showOeuvrePopup(Oeuvre existingOeuvre) {
        boolean editMode = existingOeuvre != null;
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initOwner(searchField.getScene().getWindow());
        popupStage.setTitle(editMode ? "Modifier une oeuvre" : "Ajouter une oeuvre");

        Label headerLabel = new Label(editMode ? "Modifier une oeuvre" : "Ajouter une oeuvre");
        headerLabel.getStyleClass().add("popup-title");

        Label titreLabel = new Label("Titre de l'oeuvre");
        titreLabel.getStyleClass().add("popup-field-label");
        TextField titreField = new TextField();
        titreField.getStyleClass().add("popup-input");
        if (editMode) {
            titreField.setText(existingOeuvre.getTitre() == null ? "" : existingOeuvre.getTitre());
        }
        Label titreError = createPopupErrorLabel();

        Label descriptionLabel = new Label("Description");
        descriptionLabel.getStyleClass().add("popup-field-label");
        TextArea descriptionArea = new TextArea();
        descriptionArea.getStyleClass().add("popup-input");
        descriptionArea.setPrefRowCount(4);
        if (editMode) {
            descriptionArea.setText(existingOeuvre.getDescription() == null ? "" : existingOeuvre.getDescription());
        }
        Label descriptionError = createPopupErrorLabel();
        Label descriptionCounter = new Label("0/" + DESCRIPTION_MAX_LENGTH);
        descriptionCounter.getStyleClass().add("popup-counter");
        HBox descriptionMetaRow = new HBox(8);
        descriptionMetaRow.setAlignment(Pos.CENTER_LEFT);
        Region descriptionSpacer = new Region();
        HBox.setHgrow(descriptionSpacer, Priority.ALWAYS);
        descriptionMetaRow.getChildren().addAll(descriptionError, descriptionSpacer, descriptionCounter);

        Label collectionLabel = new Label("Collection");
        collectionLabel.getStyleClass().add("popup-field-label");
        ComboBox<CollectionOeuvre> collectionCombo = new ComboBox<>();
        collectionCombo.getStyleClass().add("collections-filter-combo");
        collectionCombo.getStyleClass().add("popup-input");
        collectionCombo.setPrefWidth(460);
        collectionCombo.setPromptText("Choisir une collection");
        collectionCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(CollectionOeuvre value) {
                return value == null ? "" : fallback(value.getTitre(), "(Sans titre)");
            }

            @Override
            public CollectionOeuvre fromString(String string) {
                return null;
            }
        });
        Label collectionError = createPopupErrorLabel();

        Label imageLabel = new Label("Image");
        imageLabel.getStyleClass().add("popup-field-label");
        Button chooseImageButton = new Button("Choisir un fichier");
        chooseImageButton.getStyleClass().add("popup-close-button");
        chooseImageButton.setGraphic(createIcon("M20 6h-6.18L12 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 12H4V8h16v10z", 0.58));
        chooseImageButton.setGraphicTextGap(6);
        Label selectedFileLabel = new Label("Aucun fichier n'a ete selectionne");
        selectedFileLabel.getStyleClass().add("page-subtitle-small");
        Label imageError = createPopupErrorLabel();

        final byte[][] imageBytesHolder = new byte[1][];
        imageBytesHolder[0] = editMode ? existingOeuvre.getImage() : null;
        if (editMode && imageBytesHolder[0] != null && imageBytesHolder[0].length > 0) {
            selectedFileLabel.setText("Image actuelle conservée");
        }
        titreField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.trim().isEmpty()) {
                clearFieldError(titreError, titreField);
            }
        });

        descriptionArea.textProperty().addListener((observable, oldValue, newValue) -> {
            String current = newValue == null ? "" : newValue;
            if (current.length() > DESCRIPTION_MAX_LENGTH) {
                descriptionArea.setText(current.substring(0, DESCRIPTION_MAX_LENGTH));
                return;
            }
            validateDescriptionField(descriptionArea, descriptionError, descriptionCounter);
        });

        if (editMode) {
            validateDescriptionField(descriptionArea, descriptionError, descriptionCounter);
        }

        collectionCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                clearFieldError(collectionError, collectionCombo);
            }
        });

        chooseImageButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Selectionner une image");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
            );
            File file = chooser.showOpenDialog(popupStage);
            if (file == null) {
                return;
            }
            try {
                imageBytesHolder[0] = Files.readAllBytes(file.toPath());
                selectedFileLabel.setText(file.getName());
                clearPopupError(imageError);
            } catch (IOException e) {
                showPopupError(imageError, "Impossible de lire le fichier image.");
            }
        });

        try {
            List<CollectionOeuvre> collections = oeuvreCollectionService.getCollectionsByArtisteId(artisteId);
            collectionCombo.setItems(FXCollections.observableArrayList(collections));
            if (editMode && existingOeuvre.getCollectionId() != null) {
                for (CollectionOeuvre collection : collections) {
                    if (Objects.equals(collection.getId(), existingOeuvre.getCollectionId())) {
                        collectionCombo.setValue(collection);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            showPopupError(collectionError, "Erreur chargement collections: " + e.getMessage());
        }

        Button cancelButton = new Button("Fermer");
        cancelButton.getStyleClass().add("popup-close-button");
        cancelButton.setGraphic(createIcon("M19 6.41 17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z", 0.52));
        cancelButton.setGraphicTextGap(6);
        cancelButton.setOnAction(event -> popupStage.close());

        Button publishButton = new Button(editMode ? "Enregistrer" : "Publier");
        publishButton.getStyleClass().add("popup-confirm-button");
        publishButton.setGraphic(createIcon("M9 16.17 4.83 12 3.41 13.41 9 19l12-12-1.41-1.41z", 0.58));
        publishButton.setGraphicTextGap(6);
        publishButton.setOnAction(event -> {
            clearPopupError(titreError);
            clearPopupError(descriptionError);
            clearPopupError(collectionError);
            clearPopupError(imageError);

            String titre = safeText(titreField.getText());
            String description = safeText(descriptionArea.getText());
            CollectionOeuvre selectedCollection = collectionCombo.getValue();

            boolean hasError = false;
            if (titre.isEmpty()) {
                showFieldError(titreError, titreField, "Le titre est obligatoire.");
                hasError = true;
            } else {
                clearFieldError(titreError, titreField);
            }

            if (!validateDescriptionField(descriptionArea, descriptionError, descriptionCounter)) {
                hasError = true;
            }
            if (selectedCollection == null || selectedCollection.getId() == null) {
                showFieldError(collectionError, collectionCombo, "Selectionnez une collection.");
                hasError = true;
            } else {
                clearFieldError(collectionError, collectionCombo);
            }
            if (imageBytesHolder[0] == null || imageBytesHolder[0].length == 0) {
                showPopupError(imageError, "L'image est obligatoire.");
                hasError = true;
            }
            if (hasError) {
                return;
            }

            try {
                Oeuvre oeuvre = editMode ? existingOeuvre : new Oeuvre();
                if (oeuvre.getId() == null && editMode) {
                    oeuvre.setId(existingOeuvre.getId());
                }
                oeuvre.setTitre(titre);
                oeuvre.setDescription(description);
                oeuvre.setCollectionId(selectedCollection.getId());
                oeuvre.setImage(imageBytesHolder[0]);
                oeuvre.setDateCreation(editMode && existingOeuvre.getDateCreation() != null ? existingOeuvre.getDateCreation() : LocalDate.now());
                oeuvre.setType(resolveTypeFromSpecialite(artistSpecialite));
                if (editMode) {
                    oeuvreService.update(oeuvre);
                } else {
                    oeuvreService.add(oeuvre);
                }

                popupStage.close();
                loadFeed();
                applyFilters();
            } catch (Exception e) {
                showFieldError(titreError, titreField, e.getMessage() == null ? (editMode ? "Erreur modification oeuvre." : "Erreur ajout oeuvre.") : e.getMessage());
            }
        });

        HBox imageRow = new HBox(10, chooseImageButton, selectedFileLabel);
        imageRow.setAlignment(Pos.CENTER_LEFT);

        HBox footer = new HBox(10, cancelButton, publishButton);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(
                8,
                headerLabel,
                titreLabel,
                titreField,
                titreError,
                descriptionLabel,
                descriptionArea,
                descriptionMetaRow,
                collectionLabel,
                collectionCombo,
                collectionError,
                imageLabel,
                imageRow,
                imageError,
                footer
        );
        root.setPadding(new Insets(16));
        root.getStyleClass().add("collection-popup");

        Scene scene = new Scene(root, 720, 500);
        scene.getStylesheets().addAll(searchField.getScene().getStylesheets());

        popupStage.setScene(scene);
        popupStage.showAndWait();
    }

    private SVGPath createIcon(String path, double scale) {
        SVGPath icon = new SVGPath();
        icon.setContent(path);
        icon.setScaleX(scale);
        icon.setScaleY(scale);
        icon.setStyle("-fx-fill: #ffffff;");
        return icon;
    }

    private SVGPath createColoredIcon(String path, double scale, String fillColor) {
        SVGPath icon = new SVGPath();
        icon.setContent(path);
        icon.setScaleX(scale);
        icon.setScaleY(scale);
        icon.setStyle("-fx-fill: " + fillColor + ";");
        return icon;
    }

    private boolean validateDescriptionField(TextArea descriptionArea, Label descriptionError, Label descriptionCounter) {
        String description = safeText(descriptionArea.getText());
        int length = description.length();
        boolean valid = !description.isEmpty() && length >= DESCRIPTION_MIN_LENGTH && length <= DESCRIPTION_MAX_LENGTH;
        updateDescriptionState(descriptionArea, descriptionCounter, valid);
        if (!valid) {
            if (description.isEmpty()) {
                showPopupError(descriptionError, "La description est obligatoire.");
            } else if (length < DESCRIPTION_MIN_LENGTH) {
                showPopupError(descriptionError, "La description doit contenir au moins " + DESCRIPTION_MIN_LENGTH + " caracteres.");
            } else {
                showPopupError(descriptionError, "La description ne doit pas depasser " + DESCRIPTION_MAX_LENGTH + " caracteres.");
            }
        } else {
            clearPopupError(descriptionError);
        }
        return valid;
    }

    private void updateDescriptionState(TextArea descriptionArea, Label descriptionCounter, boolean forceValid) {
        String description = safeText(descriptionArea.getText());
        int length = description.length();
        descriptionCounter.setText(length + "/" + DESCRIPTION_MAX_LENGTH);
        descriptionCounter.getStyleClass().removeAll("popup-counter-valid", "popup-counter-limit");

        descriptionArea.getStyleClass().removeAll("popup-input-valid", "popup-input-invalid");
        if (forceValid || (!description.isEmpty() && length >= DESCRIPTION_MIN_LENGTH && length <= DESCRIPTION_MAX_LENGTH)) {
            descriptionArea.getStyleClass().add("popup-input-valid");
            descriptionCounter.getStyleClass().add("popup-counter-valid");
        } else {
            descriptionArea.getStyleClass().add("popup-input-invalid");
            descriptionCounter.getStyleClass().add("popup-counter-limit");
        }
    }

    private Label createPopupErrorLabel() {
        Label label = new Label();
        label.getStyleClass().add("popup-error");
        label.setManaged(false);
        label.setVisible(false);
        return label;
    }

    private void showPopupError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void showFieldError(Label errorLabel, javafx.scene.control.Control field, String message) {
        showPopupError(errorLabel, message);
        field.getStyleClass().remove("popup-input-valid");
        if (!field.getStyleClass().contains("popup-input-invalid")) {
            field.getStyleClass().add("popup-input-invalid");
        }
    }

    private void clearFieldError(Label errorLabel, javafx.scene.control.Control field) {
        clearPopupError(errorLabel);
        field.getStyleClass().remove("popup-input-invalid");
        if (!field.getStyleClass().contains("popup-input-valid")) {
            field.getStyleClass().add("popup-input-valid");
        }
    }

    private void clearPopupError(Label errorLabel) {
        errorLabel.setText("");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }

    private void loadFeed() {
        try {
            allFeedItems.clear();
            allFeedItems.addAll(oeuvreService.getFeedByArtisteId(artisteId));
            refreshTypeOptions();
            renderFeed(allFeedItems);
        } catch (Exception e) {
            oeuvresContainer.getChildren().clear();
            emptyStateLabel.setText("Erreur chargement oeuvres: " + e.getMessage());
            emptyStateLabel.setVisible(true);
        }
    }

    private void refreshTypeOptions() {
        Set<String> types = new LinkedHashSet<>();
        types.add(TYPE_ALL);

        for (OeuvreFeedItem item : allFeedItems) {
            String type = safeText(item.getOeuvre().getType());
            if (!type.isEmpty()) {
                types.add(type);
            }
        }

        typeCombo.setItems(FXCollections.observableArrayList(types));
        if (typeCombo.getValue() == null || !types.contains(typeCombo.getValue())) {
            typeCombo.setValue(TYPE_ALL);
        }
    }

    private void applyFilters() {
        String keyword = safeText(searchField.getText()).toLowerCase();
        String selectedType = typeCombo.getValue() == null ? TYPE_ALL : typeCombo.getValue();

        List<OeuvreFeedItem> filtered = new ArrayList<>();
        for (OeuvreFeedItem item : allFeedItems) {
            Oeuvre oeuvre = item.getOeuvre();
            String title = safeText(oeuvre.getTitre()).toLowerCase();
            String desc = safeText(oeuvre.getDescription()).toLowerCase();
            String type = safeText(oeuvre.getType()).toLowerCase();
            String collection = safeText(item.getCollectionTitre()).toLowerCase();

            boolean keywordOk = keyword.isEmpty()
                    || title.contains(keyword)
                    || desc.contains(keyword)
                    || type.contains(keyword)
                    || collection.contains(keyword);

            boolean typeOk = TYPE_ALL.equals(selectedType) || safeText(oeuvre.getType()).equalsIgnoreCase(selectedType);

            if (keywordOk && typeOk) {
                filtered.add(item);
            }
        }

        renderFeed(filtered);
    }

    private void renderFeed(List<OeuvreFeedItem> items) {
        oeuvresContainer.getChildren().clear();

        if (items.isEmpty()) {
            emptyStateLabel.setText("Aucune oeuvre trouvee.");
            emptyStateLabel.setVisible(true);
            return;
        }

        emptyStateLabel.setVisible(false);
        for (OeuvreFeedItem item : items) {
            oeuvresContainer.getChildren().add(buildPostCard(item));
        }
    }

    private VBox buildPostCard(OeuvreFeedItem item) {
        Oeuvre oeuvre = item.getOeuvre();

        VBox card = new VBox(10);
        card.getStyleClass().add("oeuvre-post-card");

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("oeuvre-post-avatar");
        Label avatarLetter = new Label(getInitialLetter(artistDisplayName));
        avatarLetter.getStyleClass().add("oeuvre-post-avatar-text");
        avatar.getChildren().add(avatarLetter);

        VBox identityBox = new VBox(1);
        Label authorLabel = new Label(artistDisplayName);
        authorLabel.getStyleClass().add("oeuvre-post-author");
        Label specialiteLabel = new Label(artistSpecialite);
        specialiteLabel.getStyleClass().add("oeuvre-post-specialite");
        identityBox.getChildren().addAll(authorLabel, specialiteLabel);

        Label dateLabel = new Label(formatDate(oeuvre.getDateCreation()));
        dateLabel.getStyleClass().add("oeuvre-post-date");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button actionsButton = new Button("...");
        actionsButton.getStyleClass().add("collection-menu-trigger");
        actionsButton.setFocusTraversable(false);

        ContextMenu actionsMenu = buildPostActionsMenu(item);
        actionsButton.setOnAction(event -> {
            if (actionsMenu.isShowing()) {
                actionsMenu.hide();
            } else {
                actionsMenu.show(actionsButton, Side.BOTTOM, 0, 4);
            }
        });

        topRow.getChildren().addAll(avatar, identityBox, spacer, dateLabel, actionsButton);

        Label titleLabel = new Label(fallback(oeuvre.getTitre(), "Sans titre"));
        titleLabel.getStyleClass().add("oeuvre-post-title");

        Label descLabel = new Label(fallback(oeuvre.getDescription(), "Aucune description"));
        descLabel.getStyleClass().add("oeuvre-post-description");
        descLabel.setWrapText(true);

        String tags = toHashtag(oeuvre.getType()) + " " + toHashtag(item.getCollectionTitre());
        Label tagsLabel = new Label(tags.trim());
        tagsLabel.getStyleClass().add("oeuvre-post-tags");

        StackPane imageWrapper = new StackPane();
        imageWrapper.getStyleClass().add("oeuvre-post-image-wrapper");
        imageWrapper.setMaxWidth(POST_IMAGE_MAX_WIDTH + 20);
        imageWrapper.setPrefHeight(POST_IMAGE_MAX_HEIGHT + 20);

        ImageView imageView = createImageViewFromBlob(oeuvre.getImage());
        if (imageView == null) {
            Label noImageLabel = new Label("Aucune image");
            noImageLabel.getStyleClass().add("oeuvre-post-image-placeholder");
            imageWrapper.getChildren().add(noImageLabel);
        } else {
            imageWrapper.getChildren().add(imageView);
        }

        HBox statsRow = new HBox(14);
        statsRow.getStyleClass().add("oeuvre-post-stats");
        Label likesLabel = new Label("Likes: " + item.getLikeCount());
        Label commentsLabel = new Label("Commentaires: 0");
        likesLabel.getStyleClass().add("oeuvre-post-stat");
        commentsLabel.getStyleClass().add("oeuvre-post-stat");
        statsRow.getChildren().addAll(likesLabel, commentsLabel);

        card.getChildren().addAll(topRow, titleLabel, descLabel, tagsLabel, imageWrapper, statsRow);
        return card;
    }

    private ContextMenu buildPostActionsMenu(OeuvreFeedItem item) {
        MenuItem editItem = new MenuItem("Modifier");
        editItem.getStyleClass().add("collection-menu-edit");
        editItem.setGraphic(createColoredIcon("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25z", 0.58, "#6b7280"));
        editItem.setOnAction(event -> showOeuvrePopup(item.getOeuvre()));

        MenuItem deleteItem = new MenuItem("Supprimer");
        deleteItem.getStyleClass().add("collection-menu-delete");
        deleteItem.setGraphic(createColoredIcon("M6 7h12v2H6V7zm2 3h8v10H8V10zm3-6h2l1 1h4v2H6V5h4l1-1z", 0.58, "#dc3545"));
        deleteItem.setOnAction(event -> onDeleteOeuvre(item.getOeuvre()));

        ContextMenu menu = new ContextMenu(editItem, deleteItem);
        menu.getStyleClass().add("collection-menu");
        return menu;
    }

    private void onDeleteOeuvre(Oeuvre oeuvre) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.initOwner(searchField.getScene().getWindow());
        confirmation.setTitle("Supprimer l'oeuvre");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Voulez-vous supprimer cette oeuvre ?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            oeuvreService.delete(oeuvre);
            loadFeed();
            applyFilters();
        } catch (Exception e) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.initOwner(searchField.getScene().getWindow());
            errorAlert.setTitle("Erreur");
            errorAlert.setHeaderText("Suppression impossible");
            errorAlert.setContentText(e.getMessage() == null ? "Une erreur est survenue." : e.getMessage());
            errorAlert.showAndWait();
        }
    }

    private ImageView createImageViewFromBlob(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }

        try {
            Image image = new Image(new ByteArrayInputStream(imageBytes));
            if (image.isError()) {
                return null;
            }

            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(POST_IMAGE_MAX_WIDTH);
            imageView.setFitHeight(POST_IMAGE_MAX_HEIGHT);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            return imageView;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "Date inconnue";
        }
        return DATE_FORMATTER.format(date);
    }

    private String toHashtag(String value) {
        String cleaned = safeText(value);
        if (cleaned.isEmpty()) {
            return "";
        }
        return "#" + cleaned.replaceAll("\\s+", "_");
    }

    private String fallback(String value, String fallback) {
        return safeText(value).isEmpty() ? fallback : value;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String getInitialLetter(String value) {
        String safe = safeText(value);
        if (safe.isEmpty()) {
            return "A";
        }
        return safe.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private String resolveTypeFromSpecialite(String specialite) {
        String value = safeText(specialite).toLowerCase(Locale.ROOT);
        return switch (value) {
            case "sculpteur" -> "Sculpture";
            case "peintre" -> "Peinture";
            case "photographe" -> "Photographie";
            case "musicien" -> "Musique";
            case "auteur" -> "Livre";
            default -> "Oeuvre";
        };
    }
}
