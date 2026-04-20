package controllers.artist;

import services.CommentaireService;
import services.LikeService;
import services.OeuvreCollectionService;
import services.OeuvreService;
import entities.CollectionOeuvre;
import entities.Commentaire;
import entities.Oeuvre;
import entities.User;
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
import utils.UserSession;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class MesOeuvresController {

    private static final int DESCRIPTION_MIN_LENGTH = 10;
    private static final int DESCRIPTION_MAX_LENGTH = 500;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRANCE);
    private static final DateTimeFormatter COMMENT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRANCE);
    private static final double POST_IMAGE_MAX_WIDTH = 700;
    private static final double POST_IMAGE_MAX_HEIGHT = 360;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> sortCombo;

    @FXML
    private Button addOeuvreButton;

    @FXML
    private VBox oeuvresContainer;

    @FXML
    private Label emptyStateLabel;

    private final OeuvreService oeuvreService = new OeuvreService();
    private final OeuvreCollectionService oeuvreCollectionService = new OeuvreCollectionService();
    private final CommentaireService commentaireService = new CommentaireService();
    private final LikeService likeService = new LikeService();
    private final List<Oeuvre> allOeuvres = new ArrayList<>();
    private final Map<Integer, List<Commentaire>> commentsByOeuvreId = new HashMap<>();
    private final Map<Integer, Integer> likeCountByOeuvreId = new HashMap<>();
    private final Map<Integer, Integer> favoriCountByOeuvreId = new HashMap<>();
    private final Map<Integer, String> collectionHashtagById = new HashMap<>();
    private String artistDisplayName = "Artiste";
    private String artistSpecialite = "Specialite inconnue";

    private Integer artisteId;

    @FXML
    public void initialize() {
        artisteId = UserSession.getCurrentUserId();
        if (artisteId == null) {
            handleMissingSession();
            return;
        }
        applyIcons();
        loadArtistIdentity();
        sortCombo.getItems().addAll("Commentaires decroissant", "Commentaires croissant");
        sortCombo.setValue("Commentaires decroissant");

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        sortCombo.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        loadOeuvres();
    }

    private void handleMissingSession() {
        oeuvresContainer.getChildren().clear();
        emptyStateLabel.setText("Session utilisateur introuvable. Veuillez vous reconnecter.");
        emptyStateLabel.setVisible(true);
        if (addOeuvreButton != null) {
            addOeuvreButton.setDisable(true);
        }
    }

    private void loadArtistIdentity() {
        try {
            User artist = oeuvreService.getUserById(artisteId);
            if (artist == null) {
                artistDisplayName = "Artiste";
                artistSpecialite = "Specialite inconnue";
                return;
            }
            String nom = safeText(artist.getNom());
            String prenom = safeText(artist.getPrenom());
            String fullName = (prenom + " " + nom).trim();
            artistDisplayName = fullName.isEmpty() ? "Artiste" : fullName;

            String specialite = safeText(artist.getSpecialite());
            artistSpecialite = specialite.isEmpty() ? "Specialite inconnue" : specialite;
        } catch (Exception ignored) {
            artistDisplayName = "Artiste";
            artistSpecialite = "Specialite inconnue";
        }
    }


    @FXML
    private void onAddOeuvreClick() {
        showOeuvrePopup(null);
    }

    private void applyIcons() {
        if (addOeuvreButton != null) {
            addOeuvreButton.setGraphic(createColoredIcon("M19 11H13V5h-2v6H5v2h6v6h2v-6h6z", 0.72, "#ffffff"));
            addOeuvreButton.setGraphicTextGap(6);
        }
    }

    private void showOeuvrePopup(Oeuvre existingOeuvre) {
        boolean editMode = existingOeuvre != null;
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initOwner(addOeuvreButton.getScene().getWindow());
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

        final String[] imagePathHolder = new String[1];
        imagePathHolder[0] = editMode ? existingOeuvre.getImage() : null;
        if (editMode && imagePathHolder[0] != null && !imagePathHolder[0].isBlank()) {
            selectedFileLabel.setText("Image actuelle conservée");
        }
        titreField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (safeText(newValue).isEmpty()) {
                showFieldError(titreError, titreField, "Le titre est obligatoire.");
            } else {
                clearFieldError(titreError, titreField);
            }
        });

        if (editMode && safeText(titreField.getText()).isEmpty()) {
            showFieldError(titreError, titreField, "Le titre est obligatoire.");
        }

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
            imagePathHolder[0] = file.getAbsolutePath();
            selectedFileLabel.setText(file.getName());
            clearPopupError(imageError);
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
            if (!validateTitreField(titreField, titreError)) {
                hasError = true;
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
            if (imagePathHolder[0] == null || imagePathHolder[0].isBlank()) {
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
                oeuvre.setImage(imagePathHolder[0]);
                oeuvre.setDateCreation(editMode && existingOeuvre.getDateCreation() != null ? existingOeuvre.getDateCreation() : LocalDate.now());
                oeuvre.setType(resolveTypeFromSpecialite(artistSpecialite));
                if (editMode) {
                    oeuvreService.update(oeuvre);
                } else {
                    oeuvreService.add(oeuvre);
                }

                popupStage.close();
                loadOeuvres();
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
        scene.getStylesheets().addAll(addOeuvreButton.getScene().getStylesheets());

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

    private boolean validateTitreField(TextField titreField, Label titreError) {
        String titre = safeText(titreField.getText());
        if (titre.isEmpty()) {
            showFieldError(titreError, titreField, "Le titre est obligatoire.");
            return false;
        }
        clearFieldError(titreError, titreField);
        return true;
    }

    private void clearPopupError(Label errorLabel) {
        errorLabel.setText("");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }

    private void loadOeuvres() {
        try {
            allOeuvres.clear();
            allOeuvres.addAll(oeuvreService.getOeuvresByArtisteId(artisteId));
            commentsByOeuvreId.clear();
            likeCountByOeuvreId.clear();
            favoriCountByOeuvreId.clear();
            applyFilters();
        } catch (Exception e) {
            oeuvresContainer.getChildren().clear();
            emptyStateLabel.setText("Erreur chargement oeuvres: " + e.getMessage());
            emptyStateLabel.setVisible(true);
        }
    }


    private void renderOeuvres(List<Oeuvre> oeuvres) {
        oeuvresContainer.getChildren().clear();

        if (oeuvres.isEmpty()) {
            emptyStateLabel.setText("Aucune oeuvre trouvee.");
            emptyStateLabel.setVisible(true);
            return;
        }

        emptyStateLabel.setVisible(false);
        for (Oeuvre oeuvre : oeuvres) {
            oeuvresContainer.getChildren().add(buildPostCard(oeuvre));
        }
    }

    private VBox buildPostCard(Oeuvre oeuvre) {

        VBox card = new VBox(10);
        card.getStyleClass().add("oeuvre-post-card");
        card.setMaxWidth(760);
        card.setPrefWidth(760);

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

        ContextMenu actionsMenu = buildPostActionsMenu(oeuvre);
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

        String tags = toHashtag(oeuvre.getType());
        String collectionTag = getCollectionHashtag(oeuvre);
        if (!collectionTag.isEmpty()) {
            tags = (tags + " " + collectionTag).trim();
        }
        Label tagsLabel = new Label(tags.trim());
        tagsLabel.getStyleClass().add("oeuvre-post-tags");

        StackPane imageWrapper = new StackPane();
        imageWrapper.getStyleClass().add("oeuvre-post-image-wrapper");
        imageWrapper.setMaxWidth(POST_IMAGE_MAX_WIDTH + 20);
        imageWrapper.setPrefHeight(POST_IMAGE_MAX_HEIGHT + 20);

        ImageView imageView = createImageViewFromSource(oeuvre.getImage());
        if (imageView == null) {
            Label noImageLabel = new Label("Aucune image");
            noImageLabel.getStyleClass().add("oeuvre-post-image-placeholder");
            imageWrapper.getChildren().add(noImageLabel);
        } else {
            imageWrapper.getChildren().add(imageView);
        }

        List<Commentaire> comments = getCommentsForOeuvre(oeuvre);
        int likesCount = getLikesCount(oeuvre);
        int favorisCount = getFavorisCount(oeuvre);

        HBox statsRow = new HBox(14);
        statsRow.getStyleClass().add("oeuvre-post-stats");
        statsRow.getChildren().addAll(
                buildStatChip("M12.1 18.55 10.55 17.14C5.4 12.47 2 9.39 2 5.6 2 2.52 4.42 0 7.5 0c1.74 0 3.41.81 4.5 2.09C13.09.81 14.76 0 16.5 0 19.58 0 22 2.52 22 5.6c0 3.79-3.4 6.87-8.55 11.55z", likesCount),
                buildStatChip("M20 2H4c-1.1 0-1.99.9-1.99 2L2 22l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z", comments.size()),
                buildStatChip("M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z", favorisCount)
        );

        VBox commentsPreviewBox = buildCommentsPreview(comments);

        card.getChildren().addAll(topRow, titleLabel, descLabel, tagsLabel, imageWrapper, statsRow, commentsPreviewBox);
        return card;
    }

    private HBox buildStatChip(String iconPath, int count) {
        HBox statChip = new HBox(6);
        statChip.setAlignment(Pos.CENTER_LEFT);
        statChip.getStyleClass().add("oeuvre-post-stat-chip");

        SVGPath icon = createColoredIcon(iconPath, 0.55, "#6b7280");
        icon.getStyleClass().add("oeuvre-post-stat-icon");

        Label countLabel = new Label(String.valueOf(Math.max(0, count)));
        countLabel.getStyleClass().add("oeuvre-post-stat-count");

        statChip.getChildren().addAll(icon, countLabel);
        return statChip;
    }

    private List<Commentaire> getCommentsForOeuvre(Oeuvre oeuvre) {
        if (oeuvre == null || oeuvre.getId() == null) {
            return new ArrayList<>();
        }

        Integer oeuvreId = oeuvre.getId();
        if (commentsByOeuvreId.containsKey(oeuvreId)) {
            return commentsByOeuvreId.get(oeuvreId);
        }

        try {
            List<Commentaire> comments = commentaireService.getCommentsByOeuvreId(oeuvre.getId());
            List<Commentaire> safeComments = comments == null ? new ArrayList<>() : comments;
                commentsByOeuvreId.put(oeuvreId, safeComments);
            oeuvre.setComments(safeComments);
            return safeComments;
        } catch (Exception ignored) {
            commentsByOeuvreId.put(oeuvreId, new ArrayList<>());
            return new ArrayList<>();
        }
    }

    private int getLikesCount(Oeuvre oeuvre) {
        if (oeuvre == null || oeuvre.getId() == null) {
            return 0;
        }

        Integer oeuvreId = oeuvre.getId();
        if (likeCountByOeuvreId.containsKey(oeuvreId)) {
            return likeCountByOeuvreId.get(oeuvreId);
        }

        int count = likeService.countLikesByOeuvre(oeuvreId);
        likeCountByOeuvreId.put(oeuvreId, count);
        return count;
    }

    private int getFavorisCount(Oeuvre oeuvre) {
        if (oeuvre == null || oeuvre.getId() == null) {
            return 0;
        }

        Integer oeuvreId = oeuvre.getId();
        if (favoriCountByOeuvreId.containsKey(oeuvreId)) {
            return favoriCountByOeuvreId.get(oeuvreId);
        }

        int count = likeService.countFavorisByOeuvre(oeuvreId);
        favoriCountByOeuvreId.put(oeuvreId, count);
        return count;
    }

    private VBox buildCommentsPreview(List<Commentaire> comments) {
        VBox commentsBox = new VBox(8);
        commentsBox.getStyleClass().add("oeuvre-post-comments-box");

        Label title = new Label("Commentaires");
        title.getStyleClass().add("oeuvre-post-comments-title");
        commentsBox.getChildren().add(title);

        if (comments == null || comments.isEmpty()) {
            Label emptyLabel = new Label("Aucun commentaire pour le moment.");
            emptyLabel.getStyleClass().add("oeuvre-post-comments-empty");
            commentsBox.getChildren().add(emptyLabel);
            return commentsBox;
        }

        int displayCount = Math.min(3, comments.size());
        for (int i = 0; i < displayCount; i++) {
            commentsBox.getChildren().add(buildCommentRow(comments.get(i)));
        }

        if (comments.size() > 3) {
            Label moreLabel = new Label("+" + (comments.size() - 3) + " autres commentaires");
            moreLabel.getStyleClass().add("oeuvre-post-comments-more");
            commentsBox.getChildren().add(moreLabel);
        }

        return commentsBox;
    }

    private HBox buildCommentRow(Commentaire comment) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add("oeuvre-post-comment-row");

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("oeuvre-post-comment-avatar");

        // Récupérer le user via son ID pour affichage
        User user = null;
        if (comment.getUserId() != null) {
            user = oeuvreService.getUserById(comment.getUserId());
        }

        String authorName = "Utilisateur";
        String authorPhoto = null;
        if (user != null) {
            String prenom = safeText(user.getPrenom());
            String nom = safeText(user.getNom());
            authorName = (prenom + " " + nom).trim();
            if (authorName.isEmpty()) authorName = "Utilisateur";
            authorPhoto = user.getPhotoProfil();
        }

        ImageView profileImage = createImageFromSource(authorPhoto);
        if (profileImage != null) {
            avatar.getChildren().add(profileImage);
        } else {
            Label initial = new Label(getInitialLetter(authorName));
            initial.getStyleClass().add("oeuvre-post-comment-avatar-text");
            avatar.getChildren().add(initial);
        }

        VBox body = new VBox(2);
        HBox.setHgrow(body, Priority.ALWAYS);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label authorLabel = new Label(authorName);
        authorLabel.getStyleClass().add("oeuvre-post-comment-author");

        Label dateLabel = new Label(formatCommentDate(comment.getDateCommentaire()));
        dateLabel.getStyleClass().add("oeuvre-post-comment-date");

        Label textLabel = new Label(fallback(comment.getTexte(), "..."));
        textLabel.getStyleClass().add("oeuvre-post-comment-text");
        textLabel.setWrapText(true);

        header.getChildren().addAll(authorLabel, dateLabel);
        body.getChildren().addAll(header, textLabel);

        row.getChildren().addAll(avatar, body);
        return row;
    }

    private String formatCommentDate(LocalDate dateCommentaire) {
        if (dateCommentaire == null) {
            return "Date inconnue";
        }
        return COMMENT_DATE_FORMATTER.format(dateCommentaire);
    }

    private ContextMenu buildPostActionsMenu(Oeuvre oeuvre) {
        MenuItem editItem = new MenuItem("Modifier");
        editItem.getStyleClass().add("collection-menu-edit");
        editItem.setGraphic(createColoredIcon("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25z", 0.58, "#6b7280"));
        editItem.setOnAction(event -> showOeuvrePopup(oeuvre));

        MenuItem deleteItem = new MenuItem("Supprimer");
        deleteItem.getStyleClass().add("collection-menu-delete");
        deleteItem.setGraphic(createColoredIcon("M6 7h12v2H6V7zm2 3h8v10H8V10zm3-6h2l1 1h4v2H6V5h4l1-1z", 0.58, "#dc3545"));
        deleteItem.setOnAction(event -> onDeleteOeuvre(oeuvre));

        ContextMenu menu = new ContextMenu(editItem, deleteItem);
        menu.getStyleClass().add("collection-menu");
        return menu;
    }

    private void onDeleteOeuvre(Oeuvre oeuvre) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.initOwner(addOeuvreButton.getScene().getWindow());
        confirmation.setTitle("Supprimer l'oeuvre");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Voulez-vous supprimer cette oeuvre ?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            oeuvreService.delete(oeuvre);
            loadOeuvres();
        } catch (Exception e) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.initOwner(addOeuvreButton.getScene().getWindow());
            errorAlert.setTitle("Erreur");
            errorAlert.setHeaderText("Suppression impossible");
            errorAlert.setContentText(e.getMessage() == null ? "Une erreur est survenue." : e.getMessage());
            errorAlert.showAndWait();
        }
    }

    private ImageView createImageViewFromSource(String imageSource) {
        if (imageSource == null || imageSource.isBlank()) {
            return null;
        }

        try {
            Image image;
            if (imageSource.startsWith("http://") || imageSource.startsWith("https://") || imageSource.startsWith("file:")) {
                image = new Image(imageSource, true);
            } else {
                image = new Image(new File(imageSource).toURI().toString(), true);
            }
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

    private ImageView createImageFromSource(String source) {
        String safeSource = safeText(source);
        if (safeSource.isEmpty()) {
            return null;
        }

        try {
            Image image;
            if (safeSource.startsWith("http://") || safeSource.startsWith("https://") || safeSource.startsWith("file:")) {
                image = new Image(safeSource, true);
            } else {
                image = new Image(new File(safeSource).toURI().toString(), true);
            }

            if (image.isError()) {
                return null;
            }

            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(30);
            imageView.setFitHeight(30);
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

    private String getCollectionHashtag(Oeuvre oeuvre) {
        if (oeuvre == null || oeuvre.getCollectionId() == null) {
            return "";
        }

        Integer collectionId = oeuvre.getCollectionId();
        if (collectionHashtagById.containsKey(collectionId)) {
            return collectionHashtagById.get(collectionId);
        }

        String hashtag = "";
        try {
            CollectionOeuvre collection = oeuvreCollectionService.getCollectionById(collectionId);
            if (collection != null) {
                hashtag = toHashtag(collection.getTitre());
            }
        } catch (Exception ignored) {
            hashtag = "";
        }

        collectionHashtagById.put(collectionId, hashtag);
        return hashtag;
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

    private void applyFilters() {
        String keyword = safeText(searchField.getText()).toLowerCase(Locale.ROOT).trim();
        List<Oeuvre> filtered = new ArrayList<>();

        for (Oeuvre oeuvre : allOeuvres) {
            if (matchesSearch(oeuvre, keyword)) {
                filtered.add(oeuvre);
            }
        }

        applyCommentSort(filtered, sortCombo.getValue());
        renderOeuvres(filtered);
    }

    private boolean matchesSearch(Oeuvre oeuvre, String keyword) {
        if (keyword.isEmpty()) {
            return true;
        }

        String title = safeText(oeuvre.getTitre()).toLowerCase(Locale.ROOT);
        if (title.contains(keyword)) {
            return true;
        }

        String collectionTitle = getCollectionSearchTitle(oeuvre).toLowerCase(Locale.ROOT);
        return collectionTitle.contains(keyword);
    }

    private String getCollectionSearchTitle(Oeuvre oeuvre) {
        if (oeuvre == null || oeuvre.getCollectionId() == null) {
            return "";
        }

        try {
            CollectionOeuvre collection = oeuvreCollectionService.getCollectionById(oeuvre.getCollectionId());
            return collection == null ? "" : safeText(collection.getTitre());
        } catch (Exception ignored) {
            return "";
        }
    }

    private void applyCommentSort(List<Oeuvre> oeuvres, String sortValue) {
        if (oeuvres == null || oeuvres.isEmpty()) {
            return;
        }

        Comparator<Oeuvre> byCommentCount = Comparator.comparingInt(oeuvre -> getCommentsForOeuvre(oeuvre).size());
        if ("Commentaires croissant".equals(sortValue)) {
            oeuvres.sort(byCommentCount);
        } else {
            oeuvres.sort(byCommentCount.reversed());
        }
    }
}
