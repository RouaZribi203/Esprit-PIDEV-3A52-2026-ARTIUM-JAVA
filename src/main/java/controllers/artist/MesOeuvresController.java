package controllers.artist;

import Services.OeuvreService;
import Services.OeuvreService.OeuvreFeedItem;
import entities.Oeuvre;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MesOeuvresController {

    private static final String TYPE_ALL = "Tous les types";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRANCE);
    private static final double POST_IMAGE_MAX_WIDTH = 760;
    private static final double POST_IMAGE_MAX_HEIGHT = 360;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> typeCombo;

    @FXML
    private VBox oeuvresContainer;

    @FXML
    private Label emptyStateLabel;

    private final OeuvreService oeuvreService = new OeuvreService();
    private final List<OeuvreFeedItem> allFeedItems = new ArrayList<>();

    // TODO: brancher l'ID depuis la session utilisateur quand elle sera disponible.
    private final int artisteId = 3;

    @FXML
    public void initialize() {
        typeCombo.setItems(FXCollections.observableArrayList(TYPE_ALL));
        typeCombo.setValue(TYPE_ALL);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        typeCombo.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());

        loadFeed();
    }

    @FXML
    private void onSearchClick() {
        applyFilters();
    }

    @FXML
    private void onFilterClick() {
        applyFilters();
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

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label authorLabel = new Label("artist artist");
        authorLabel.getStyleClass().add("oeuvre-post-author");

        Label dateLabel = new Label(formatDate(oeuvre.getDateCreation()));
        dateLabel.getStyleClass().add("oeuvre-post-date");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topRow.getChildren().addAll(authorLabel, dateLabel, spacer);

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
}
