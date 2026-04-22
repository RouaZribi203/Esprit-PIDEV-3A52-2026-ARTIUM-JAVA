package controllers.amateur;

import entities.LocationLivre;
import entities.Livre;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.FadeTransition;
import services.JdbcLivreService;
import services.JdbcLocationLivreService;
import services.LivreService;

import javafx.application.Platform;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.sql.SQLDataException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import java.time.Duration;
import java.time.LocalDateTime;
import javafx.scene.control.Tooltip;

public class BibliofrontController {
    private final LivreService livreService = new JdbcLivreService();
    private final JdbcLocationLivreService locationLivreService = new JdbcLocationLivreService();
    private final int currentUserId = 1;
    private List<Livre> livres = new ArrayList<>();

    @FXML
    private TextField searchField;

    @FXML
    private ScrollPane cardsScroll;

    @FXML
    private TilePane cardsTile;

    @FXML
    private ToggleButton filterRentedToggle;

    @FXML
    private ComboBox<String> authorCombo;

    @FXML
    private ComboBox<String> priceSortCombo;

    @FXML
    private Label badge1Icon;

    @FXML
    private Label badge1Text;

    @FXML
    private Label badge2Icon;

    @FXML
    private Label badge2Text;

    @FXML
    private Label badge3Icon;

    @FXML
    private Label badge3Text;

    @FXML
    public void initialize() {
        if (filterRentedToggle != null) {
            filterRentedToggle.selectedProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, newV) -> applyFilter());
        }
        if (authorCombo != null) {
            authorCombo.valueProperty().addListener((obs, old, newV) -> applyFilter());
        }
        if (priceSortCombo != null) {
            priceSortCombo.valueProperty().addListener((obs, old, newV) -> applyFilter());
        }
        if (searchField != null) {
            searchField.setTooltip(new Tooltip("Rechercher dans le titre, la catégorie ou l'auteur."));
        }
        if (authorCombo != null) {
            authorCombo.setTooltip(new Tooltip("Filtrer les livres par auteur spécifique."));
        }
        if (priceSortCombo != null) {
            priceSortCombo.setTooltip(new Tooltip("Trier les livres par prix de location : décroissant ou croissant."));
        }
        setupBadgeHoverEffects();
        refresh();
    }

    @FXML
    private void onSearch() {
        applyFilter();
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    private void setupBadgeHoverEffects() {
        // Badge 1 (Green - Livres numériques)
        if (badge1Icon != null) {
            badge1Icon.setOnMouseEntered(e -> {
                badge1Icon.setStyle("-fx-font-size: 16; -fx-padding: 6 12; -fx-background-color: #10b981; -fx-border-color: #10b981; -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);");
            });
            badge1Icon.setOnMouseExited(e -> {
                badge1Icon.setStyle("-fx-font-size: 16; -fx-padding: 6 12; -fx-background-color: #eff6ff; -fx-border-color: #10b981; -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: #10b981; -fx-font-weight: bold; -fx-cursor: hand;");
            });
        }
        if (badge1Text != null) {
            badge1Text.setOnMouseEntered(e -> {
                badge1Text.setStyle("-fx-font-size: 13; -fx-padding: 6 12; -fx-background-color: #10b981; -fx-border-color: #10b981; -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);");
            });
            badge1Text.setOnMouseExited(e -> {
                badge1Text.setStyle("-fx-font-size: 13; -fx-padding: 6 12; -fx-background-color: #eff6ff; -fx-border-color: #10b981; -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: #10b981; -fx-font-weight: bold; -fx-cursor: hand;");
            });
        }
        // Badge 2 (Yellow - Accès immédiat)
        if (badge2Icon != null) {
            badge2Icon.setOnMouseEntered(e -> {
                badge2Icon.setStyle("-fx-font-size: 16; -fx-padding: 6 12; -fx-background-color: #f59e0b; -fx-border-color: #f59e0b; -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);");
            });
            badge2Icon.setOnMouseExited(e -> {
                badge2Icon.setStyle("-fx-font-size: 16; -fx-padding: 6 12; -fx-background-color: #fef3c7; -fx-border-color: #f59e0b; -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-cursor: hand;");
            });
        }
        if (badge2Text != null) {
            badge2Text.setOnMouseEntered(e -> {
                badge2Text.setStyle("-fx-font-size: 13; -fx-padding: 6 12; -fx-background-color: #f59e0b; -fx-border-color: #f59e0b; -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);");
            });
            badge2Text.setOnMouseExited(e -> {
                badge2Text.setStyle("-fx-font-size: 13; -fx-padding: 6 12; -fx-background-color: #fef3c7; -fx-border-color: #f59e0b; -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-cursor: hand;");
            });
        }
        // Badge 3 (Blue - Louer & Lire)
        if (badge3Icon != null) {
            badge3Icon.setOnMouseEntered(e -> {
                badge3Icon.setStyle("-fx-font-size: 16; -fx-padding: 6 12; -fx-background-color: #3b82f6; -fx-border-color: #3b82f6; -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);");
            });
            badge3Icon.setOnMouseExited(e -> {
                badge3Icon.setStyle("-fx-font-size: 16; -fx-padding: 6 12; -fx-background-color: #dbeafe; -fx-border-color: #3b82f6; -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: #3b82f6; -fx-font-weight: bold; -fx-cursor: hand;");
            });
        }
        if (badge3Text != null) {
            badge3Text.setOnMouseEntered(e -> {
                badge3Text.setStyle("-fx-font-size: 13; -fx-padding: 6 12; -fx-background-color: #3b82f6; -fx-border-color: #3b82f6; -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);");
            });
            badge3Text.setOnMouseExited(e -> {
                badge3Text.setStyle("-fx-font-size: 13; -fx-padding: 6 12; -fx-background-color: #dbeafe; -fx-border-color: #3b82f6; -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: #3b82f6; -fx-font-weight: bold; -fx-cursor: hand;");
            });
        }
    }

    private void applyFilter() {
        List<Livre> result = new ArrayList<>(livres);
        String query = searchField.getText();
        if (query != null && !query.trim().isEmpty()) {
            String q = query.trim().toLowerCase();
            result = result.stream().filter(l -> containsIgnoreCase(l.getTitre(), q) || containsIgnoreCase(l.getCategorie(), q) || containsIgnoreCase(l.getAuteur(), q)).collect(Collectors.toList());
        }
        String author = authorCombo.getValue();
        if (author != null && !author.isEmpty()) {
            result = result.stream().filter(l -> author.equals(l.getAuteur())).collect(Collectors.toList());
        }
        String priceSort = priceSortCombo.getValue();
        if ("💰 Prix décroissant".equals(priceSort)) {
            result.sort((a, b) -> Double.compare(b.getPrixLocation() != null ? b.getPrixLocation() : 0, a.getPrixLocation() != null ? a.getPrixLocation() : 0));
        } else if ("💰 Prix croissant".equals(priceSort)) {
            result.sort((a, b) -> Double.compare(a.getPrixLocation() != null ? a.getPrixLocation() : 0, b.getPrixLocation() != null ? a.getPrixLocation() : 0));
        }
        if (filterRentedToggle != null && filterRentedToggle.isSelected()) {
            try {
                List<Integer> rentedIds = locationLivreService.getRentedLivreIds(currentUserId);
                result = result.stream().filter(l -> rentedIds.contains(l.getId())).collect(Collectors.toList());
            } catch (SQLDataException e) {
                showError("Filtrage locations", e.getMessage());
            }
        }
        updateView(result);
    }

    private void refresh() {
        try {
            livres = livreService.getAll();
            if (authorCombo != null) {
                authorCombo.getItems().clear();
                livres.stream().map(Livre::getAuteur).filter(Objects::nonNull).filter(a -> !a.isBlank()).distinct().forEach(authorCombo.getItems()::add);
            }
            if (priceSortCombo != null) {
                priceSortCombo.getItems().setAll("💰 Prix décroissant", "💰 Prix croissant");
            }
            applyFilter();
        } catch (SQLDataException e) {
            showError("Chargement", e.getMessage());
        }
    }

    private void updateView(List<Livre> items) {
        updateCards(items);
    }

    private void updateCards(List<Livre> items) {
        if (cardsTile == null) {
            return;
        }
        cardsTile.getChildren().setAll(items.stream().map(this::createBookCard).toList());
    }

    private VBox createBookCard(Livre livre) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(180);
        imageView.setFitHeight(240);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 5, 0, 0, 2);");
        if (livre.getImage() != null && !livre.getImage().isBlank()) {
            imageView.setImage(toImage(livre.getImage()));
        }

        // Center the image
        HBox imageContainer = new HBox();
        imageContainer.setAlignment(javafx.geometry.Pos.CENTER);
        imageContainer.getChildren().add(imageView);

        Label title = new Label(livre.getTitre() == null ? "" : livre.getTitre());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #1f2937; -fx-wrap-text: true; -fx-text-alignment: center;");
        title.setMaxWidth(160);
        title.setWrapText(true);

        Label categorie = new Label(livre.getCategorie() == null ? "N/A" : livre.getCategorie());
        categorie.setStyle("-fx-font-size: 11; -fx-text-fill: #6b7280; -fx-font-style: italic; -fx-text-alignment: center;");
        categorie.setMaxWidth(160);
        categorie.setWrapText(true);
        
        Label prix = new Label((livre.getPrixLocation() == null ? "0" : livre.getPrixLocation()) + " DT");
        prix.setStyle("-fx-font-size: 12; -fx-text-fill: #3b82f6; -fx-font-weight: bold;");

        Label dispo = new Label(Boolean.TRUE.equals(livre.getDisponibilite()) ? "✓ Disponible" : "✗ Indisponible");
        dispo.setStyle("-fx-font-size: 11; -fx-text-fill: " + (Boolean.TRUE.equals(livre.getDisponibilite()) ? "#10b981" : "#ef4444") + "; -fx-font-weight: bold;");

        Button details = new Button("📖 Détails");
        details.setStyle("-fx-padding: 8 12; -fx-font-size: 11; -fx-background-color: #f3f4f6; -fx-text-fill: #374151; -fx-border-color: #d1d5db; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;");
        details.setMaxWidth(Double.MAX_VALUE);
        details.setOnAction(e -> showDetailsDialog(livre));
        details.setOnMouseEntered(e -> details.setStyle("-fx-padding: 8 12; -fx-font-size: 11; -fx-background-color: #e5e7eb; -fx-text-fill: #1f2937; -fx-border-color: #9ca3af; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;"));
        details.setOnMouseExited(e -> details.setStyle("-fx-padding: 8 12; -fx-font-size: 11; -fx-background-color: #f3f4f6; -fx-text-fill: #374151; -fx-border-color: #d1d5db; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;"));

        Button rent = new Button("🎯 Louer");
        rent.setDisable(!Boolean.TRUE.equals(livre.getDisponibilite()));
        rent.setStyle("-fx-padding: 8 12; -fx-font-size: 11; -fx-background-color: " + (Boolean.TRUE.equals(livre.getDisponibilite()) ? "#10b981" : "#d1d5db") + "; -fx-text-fill: white; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-weight: bold;");
        rent.setMaxWidth(Double.MAX_VALUE);
        rent.setOnAction(e -> louerLivreAvecDialog(livre));
        if (Boolean.TRUE.equals(livre.getDisponibilite())) {
            rent.setOnMouseEntered(e -> rent.setStyle("-fx-padding: 8 12; -fx-font-size: 11; -fx-background-color: #059669; -fx-text-fill: white; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);"));
            rent.setOnMouseExited(e -> rent.setStyle("-fx-padding: 8 12; -fx-font-size: 11; -fx-background-color: #10b981; -fx-text-fill: white; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand; -fx-font-weight: bold;"));
        }

        HBox actions = new HBox(8, details, rent);
        HBox.setHgrow(details, Priority.ALWAYS);
        HBox.setHgrow(rent, Priority.ALWAYS);
        actions.setStyle("-fx-spacing: 8;");

        VBox infoBox = new VBox(6);
        infoBox.getChildren().addAll(title, categorie, prix, dispo);
        infoBox.setStyle("-fx-padding: 0;");
        infoBox.setAlignment(javafx.geometry.Pos.CENTER);

        VBox card = new VBox(12, imageContainer, infoBox, actions);
        card.setStyle("-fx-padding: 15; -fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 12; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        card.setPrefWidth(220);
        card.setMaxWidth(220);
        card.setAlignment(javafx.geometry.Pos.CENTER);
        VBox.setVgrow(imageContainer, Priority.NEVER);

        // Add hover effects for scale/zoom
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-padding: 15; -fx-background-color: white; -fx-border-color: #3b82f6; -fx-border-radius: 12; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(59,130,246,0.3), 15, 0, 0, 5);");
            card.setScaleX(1.05);
            card.setScaleY(1.05);
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-padding: 15; -fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 12; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

        return card;
    }

    private void louerLivreAvecDialog(Livre livre) {
        if (!Boolean.TRUE.equals(livre.getDisponibilite())) {
            showError("Location", "Ce livre est indisponible.");
            return;
        }
        if (livre.getId() == null) {
            showError("Location", "Livre invalide.");
            return;
        }
        Integer jours = askNombreDeJours(livre);
        if (jours == null) {
            return;
        }
        try {
            locationLivreService.louerLivre(livre.getId(), currentUserId, jours);
            showInfo("Location", "Livre loué avec succès.");
            refresh();
        } catch (SQLDataException e) {
            showError("Location impossible", e.getMessage());
        }
    }

    private Integer askNombreDeJours(Livre livre) {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("📅 Réserver votre livre");
        dialog.setWidth(700);
        dialog.setHeight(750);

        javafx.scene.control.ButtonType confirmButton = new javafx.scene.control.ButtonType("Confirmer la réservation", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButton, javafx.scene.control.ButtonType.CANCEL);
        
        Button confirmBtn = (Button) dialog.getDialogPane().lookupButton(confirmButton);
        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL);
        
        // Style both buttons identically
        String buttonStyle = "-fx-padding: 12 28; -fx-font-size: 14; -fx-background-color: #0891b2; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 8; -fx-cursor: hand;";
        confirmBtn.setStyle(buttonStyle);
        cancelBtn.setStyle("-fx-padding: 12 28; -fx-font-size: 14; -fx-background-color: #cbd5e1; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-border-radius: 8; -fx-cursor: hand;");
        
        // Hover effects for confirm button
        confirmBtn.setOnMouseEntered(e -> confirmBtn.setStyle("-fx-padding: 12 28; -fx-font-size: 14; -fx-background-color: #0891b2; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 3);"));
        confirmBtn.setOnMouseExited(e -> confirmBtn.setStyle(buttonStyle));
        
        confirmBtn.setDisable(true);

        // State holder
        java.time.LocalDate[] selectedStart = {null};
        java.time.LocalDate[] selectedEnd = {null};
        int[] currentStep = {1};

        // ===== STEP 1: CALENDAR SELECTION =====
        VBox calendarStep = new VBox(20);
        calendarStep.setStyle("-fx-padding: 30; -fx-background-color: linear-gradient(to bottom, #ffffff, #f8f9fa);");

        Label stepTitleCal = new Label("📅 Sélectionnez vos dates de location");
        stepTitleCal.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");

        VBox calendarBox = createPremiumCalendarUI(selectedStart, selectedEnd, confirmBtn);
        calendarStep.getChildren().addAll(stepTitleCal, calendarBox);
        calendarStep.setFillWidth(true);

        // ===== STEP 2: PAYMENT CONFIRMATION =====
        VBox paymentStep = new VBox(20);
        paymentStep.setStyle("-fx-padding: 30; -fx-background-color: linear-gradient(to bottom, #ffffff, #f8f9fa);");

        Label stepTitlePay = new Label("💳 Confirmation de la réservation");
        stepTitlePay.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");

        // Summary section with premium styling
        VBox summaryBox = new VBox(15);
        summaryBox.setStyle("-fx-padding: 20; -fx-background-color: white; -fx-border-color: #e0e7ff; -fx-border-radius: 12; -fx-background-radius: 12; -fx-border-width: 2; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

        Label dateRangeLabel = new Label();
        dateRangeLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #1e293b; -fx-font-weight: bold;");

        Label daysLabel = new Label();
        daysLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #0891b2;");

        Label priceLabel = new Label();
        priceLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #10b981;");

        summaryBox.getChildren().addAll(dateRangeLabel, daysLabel, priceLabel);

        // Payment method selector with modern styling
        HBox paymentMethodBox = new HBox(15);
        paymentMethodBox.setStyle("-fx-padding: 15; -fx-background-color: #f0f9ff; -fx-border-radius: 8;");

        javafx.scene.control.ToggleButton cardBtn = new javafx.scene.control.ToggleButton("💳 Carte Bancaire");
        cardBtn.setStyle("-fx-padding: 10 20; -fx-font-size: 12; -fx-background-color: #0891b2; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 6; -fx-cursor: hand;");
        cardBtn.setSelected(true);

        javafx.scene.control.ToggleButton walletBtn = new javafx.scene.control.ToggleButton("💰 Portefeuille");
        walletBtn.setStyle("-fx-padding: 10 20; -fx-font-size: 12; -fx-background-color: #cbd5e1; -fx-text-fill: #334155; -fx-border-radius: 6; -fx-cursor: hand;");

        cardBtn.setOnAction(e -> {
            if (cardBtn.isSelected()) {
                cardBtn.setStyle("-fx-padding: 10 20; -fx-font-size: 12; -fx-background-color: #0891b2; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 6;");
                walletBtn.setSelected(false);
                walletBtn.setStyle("-fx-padding: 10 20; -fx-font-size: 12; -fx-background-color: #cbd5e1; -fx-text-fill: #334155; -fx-border-radius: 6;");
            }
        });

        walletBtn.setOnAction(e -> {
            if (walletBtn.isSelected()) {
                walletBtn.setStyle("-fx-padding: 10 20; -fx-font-size: 12; -fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 6;");
                cardBtn.setSelected(false);
                cardBtn.setStyle("-fx-padding: 10 20; -fx-font-size: 12; -fx-background-color: #cbd5e1; -fx-text-fill: #334155; -fx-border-radius: 6;");
            }
        });

        paymentMethodBox.getChildren().addAll(cardBtn, walletBtn);

        // Professional card details form
        VBox cardDetailsBox = new VBox(12);
        cardDetailsBox.setStyle("-fx-padding: 20; -fx-background-color: #f8f9fa; -fx-border-color: #e2e8f0; -fx-border-radius: 10; -fx-background-radius: 10;");

        // Card number field
        Label cardNumberLabel = new Label("Numéro de carte");
        cardNumberLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #334155;");
        TextField cardNumber = new TextField();
        cardNumber.setPromptText("1234 5678 9012 3456");
        cardNumber.setStyle("-fx-font-size: 13; -fx-padding: 12; -fx-border-color: #cbd5e1; -fx-border-radius: 6; -fx-border-width: 1;");

        // Expiry and CVV
        Label expiryLabel = new Label("Date d'expiration");
        expiryLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #334155;");
        
        HBox expiryBox = new HBox(12);
        TextField cardExpiry = new TextField();
        cardExpiry.setPromptText("MM/YY");
        cardExpiry.setMaxWidth(120);
        cardExpiry.setStyle("-fx-font-size: 13; -fx-padding: 12; -fx-border-color: #cbd5e1; -fx-border-radius: 6; -fx-border-width: 1;");

        Label cvvLabel2 = new Label("CVV");
        cvvLabel2.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #334155;");
        
        TextField cardCvv = new TextField();
        cardCvv.setPromptText("123");
        cardCvv.setMaxWidth(100);
        cardCvv.setStyle("-fx-font-size: 13; -fx-padding: 12; -fx-border-color: #cbd5e1; -fx-border-radius: 6; -fx-border-width: 1;");

        expiryBox.getChildren().addAll(cardExpiry, new Label("      "), cardCvv);

        // Cardholder name
        Label nameLabel = new Label("Nom du titulaire");
        nameLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #334155;");
        TextField cardholderName = new TextField();
        cardholderName.setPromptText("Jean Dupont");
        cardholderName.setStyle("-fx-font-size: 13; -fx-padding: 12; -fx-border-color: #cbd5e1; -fx-border-radius: 6; -fx-border-width: 1;");

        cardDetailsBox.getChildren().addAll(
            cardNumberLabel, cardNumber,
            expiryLabel, expiryBox,
            nameLabel, cardholderName
        );

        // Terms checkbox
        javafx.scene.control.CheckBox termsBox = new javafx.scene.control.CheckBox("J'accepte les conditions de location");
        termsBox.setStyle("-fx-font-size: 12; -fx-text-fill: #334155; -fx-padding: 10;");

        paymentStep.getChildren().addAll(stepTitlePay, summaryBox, new Label("Méthode de paiement:"), paymentMethodBox, cardDetailsBox, termsBox);
        paymentStep.setFillWidth(true);

        // Main container - stack layout
        VBox mainContainer = new VBox();
        mainContainer.getChildren().add(calendarStep);

        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setStyle("-fx-background-color: white;");
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(false);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().setStyle("-fx-background-color: white;");

        confirmBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (currentStep[0] == 1) {
                if (selectedStart[0] == null || selectedEnd[0] == null) {
                    event.consume();
                    showError("Validation", "Veuillez sélectionner les dates de début et de fin.");
                    return;
                }

                // Move to payment step
                event.consume();
                currentStep[0] = 2;

                // Update summary with dynamic book price - add 1 to include both start and end dates
                long days = java.time.temporal.ChronoUnit.DAYS.between(selectedStart[0], selectedEnd[0]) + 1;
                double bookPrice = livre.getPrixLocation() != null ? livre.getPrixLocation() : 0;
                double totalPrice = days * bookPrice;

                dateRangeLabel.setText("📅 Du " + selectedStart[0] + " au " + selectedEnd[0]);
                daysLabel.setText("⏱️ Durée : " + days + " jour(s)");
                priceLabel.setText("💳 Total : " + String.format("%.2f", totalPrice) + " DT");

                // Smooth transition
                javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(250), calendarStep);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    mainContainer.getChildren().clear();
                    mainContainer.getChildren().add(paymentStep);
                    scrollPane.setVvalue(0);
                    javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(250), paymentStep);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();
                });
                fadeOut.play();

                confirmBtn.setText("✅ Valider la réservation");
            } else if (currentStep[0] == 2) {
                if (!termsBox.isSelected()) {
                    event.consume();
                    showError("Validation", "Veuillez accepter les conditions de location.");
                    return;
                }
                long days = java.time.temporal.ChronoUnit.DAYS.between(selectedStart[0], selectedEnd[0]);
                dialog.setResultConverter(bt -> (int) days);
            }
        });

        dialog.setResultConverter(bt -> {
            if (bt == confirmButton) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(selectedStart[0], selectedEnd[0]);
                return (int) days;
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    private VBox createPremiumCalendarUI(java.time.LocalDate[] selectedStart, java.time.LocalDate[] selectedEnd, Button confirmBtn) {
        VBox calendarContainer = new VBox(15);

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.YearMonth[] currentMonth = {java.time.YearMonth.now()};

        // Header with navigation
        HBox headerBox = new HBox(20);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER);
        headerBox.setStyle("-fx-padding: 15;");

        Button prevBtn = new Button("◀");
        prevBtn.setStyle("-fx-font-size: 14; -fx-padding: 8 16; -fx-background-color: #e0e7ff; -fx-text-fill: #1e3a8a; -fx-border-radius: 6; -fx-cursor: hand; -fx-font-weight: bold;");
        prevBtn.setOnMouseEntered(e -> prevBtn.setStyle("-fx-font-size: 14; -fx-padding: 8 16; -fx-background-color: #c7d2fe; -fx-text-fill: #1e3a8a; -fx-border-radius: 6; -fx-font-weight: bold;"));
        prevBtn.setOnMouseExited(e -> prevBtn.setStyle("-fx-font-size: 14; -fx-padding: 8 16; -fx-background-color: #e0e7ff; -fx-text-fill: #1e3a8a; -fx-border-radius: 6; -fx-font-weight: bold;"));

        Label monthLabel = new Label();
        monthLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #1e3a8a; -fx-min-width: 200;");
        monthLabel.setAlignment(javafx.geometry.Pos.CENTER);

        Button nextBtn = new Button("▶");
        nextBtn.setStyle("-fx-font-size: 14; -fx-padding: 8 16; -fx-background-color: #e0e7ff; -fx-text-fill: #1e3a8a; -fx-border-radius: 6; -fx-cursor: hand; -fx-font-weight: bold;");
        nextBtn.setOnMouseEntered(e -> nextBtn.setStyle("-fx-font-size: 14; -fx-padding: 8 16; -fx-background-color: #c7d2fe; -fx-text-fill: #1e3a8a; -fx-border-radius: 6; -fx-font-weight: bold;"));
        nextBtn.setOnMouseExited(e -> nextBtn.setStyle("-fx-font-size: 14; -fx-padding: 8 16; -fx-background-color: #e0e7ff; -fx-text-fill: #1e3a8a; -fx-border-radius: 6; -fx-font-weight: bold;"));

        headerBox.getChildren().addAll(prevBtn, monthLabel, nextBtn);

        // Calendar grid with proper 7-column layout
        VBox calendarGrid = new VBox(10);
        calendarGrid.setStyle("-fx-padding: 20; -fx-background-color: white; -fx-border-color: #e0e7ff; -fx-border-radius: 12; -fx-background-radius: 12; -fx-border-width: 2;");

        TilePane daysGrid = new TilePane();
        daysGrid.setPrefColumns(7);
        daysGrid.setHgap(8);
        daysGrid.setVgap(8);
        daysGrid.setStyle("-fx-padding: 10;");

        // Day headers in French: DIM, LUN, MAR, MER, JEU, VEN, SAM
        String[] dayNames = {"DIM", "LUN", "MAR", "MER", "JEU", "VEN", "SAM"};
        for (String day : dayNames) {
            Label dayLabel = new Label(day);
            dayLabel.setPrefWidth(70);
            dayLabel.setPrefHeight(40);
            dayLabel.setStyle("-fx-text-alignment: center; -fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 13; -fx-background-color: #1e3a8a; -fx-border-radius: 8; -fx-background-radius: 8;");
            dayLabel.setAlignment(javafx.geometry.Pos.CENTER);
            daysGrid.getChildren().add(dayLabel);
        }

        // Update calendar
        final java.util.function.Consumer<java.time.YearMonth>[] updateCalendarHolder = new java.util.function.Consumer[1];
        updateCalendarHolder[0] = month -> {
            // Format month in French
            String[] monthsInFrench = {"JANVIER", "FÉVRIER", "MARS", "AVRIL", "MAI", "JUIN", "JUILLET", "AOÛT", "SEPTEMBRE", "OCTOBRE", "NOVEMBRE", "DÉCEMBRE"};
            monthLabel.setText(monthsInFrench[month.getMonthValue() - 1] + " " + month.getYear());
            
            // Remove only date buttons, keep headers (first 7 items)
            daysGrid.getChildren().removeIf(node -> {
                int index = daysGrid.getChildren().indexOf(node);
                return index >= 7; // Keep first 7 items (headers), remove the rest
            });

            java.time.LocalDate firstDay = month.atDay(1);
            java.time.LocalDate lastDay = month.atEndOfMonth();
            int firstDayOfWeek = firstDay.getDayOfWeek().getValue() % 7;

            // Empty cells for days before month starts
            for (int i = 0; i < firstDayOfWeek; i++) {
                Label empty = new Label();
                empty.setPrefWidth(70);
                empty.setPrefHeight(70);
                daysGrid.getChildren().add(empty);
            }

            // Add all days of the month
            for (int day = 1; day <= lastDay.getDayOfMonth(); day++) {
                java.time.LocalDate date = month.atDay(day);
                Button dayBtn = new Button(String.valueOf(day));
                dayBtn.setPrefWidth(70);
                dayBtn.setPrefHeight(70);

                boolean isPast = date.isBefore(today);
                boolean isStart = selectedStart[0] != null && date.equals(selectedStart[0]);
                boolean isEnd = selectedEnd[0] != null && date.equals(selectedEnd[0]);
                boolean isInRange = (selectedStart[0] != null && selectedEnd[0] != null && 
                                    !date.isBefore(selectedStart[0]) && !date.isAfter(selectedEnd[0]));

                if (isPast) {
                    dayBtn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #94a3b8; -fx-cursor: not-allowed; -fx-font-size: 14; -fx-font-weight: bold; -fx-border-radius: 8;");
                    dayBtn.setDisable(true);
                } else if (isStart || isEnd) {
                    dayBtn.setStyle("-fx-background-color: #06b6d4; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14; -fx-border-radius: 8; -fx-effect: dropshadow(gaussian, rgba(6,182,212,0.4), 6, 0, 0, 2);");
                } else if (isInRange) {
                    dayBtn.setStyle("-fx-background-color: #cffafe; -fx-text-fill: #0c4a6e; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14; -fx-border-radius: 8;");
                } else {
                    dayBtn.setStyle("-fx-background-color: white; -fx-text-fill: #334155; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-cursor: hand; -fx-font-size: 14; -fx-border-width: 1;");
                }

                // ...existing code...
                dayBtn.setOnMouseEntered(e -> {
                    if (!isPast) {
                        if (isStart || isEnd) {
                            dayBtn.setStyle("-fx-background-color: #0891b2; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14; -fx-border-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 3);");
                        } else if (isInRange) {
                            dayBtn.setStyle("-fx-background-color: #a5f3fc; -fx-text-fill: #0c4a6e; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14; -fx-border-radius: 8;");
                        } else {
                            dayBtn.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1; -fx-border-color: #06b6d4; -fx-border-radius: 8; -fx-cursor: hand; -fx-font-size: 14; -fx-border-width: 2; -fx-font-weight: bold;");
                        }
                    }
                });

                dayBtn.setOnMouseExited(e -> {
                    if (!isPast) {
                        if (isStart || isEnd) {
                            dayBtn.setStyle("-fx-background-color: #06b6d4; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14; -fx-border-radius: 8; -fx-effect: dropshadow(gaussian, rgba(6,182,212,0.4), 6, 0, 0, 2);");
                        } else if (isInRange) {
                            dayBtn.setStyle("-fx-background-color: #cffafe; -fx-text-fill: #0c4a6e; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14; -fx-border-radius: 8;");
                        } else {
                            dayBtn.setStyle("-fx-background-color: white; -fx-text-fill: #334155; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-cursor: hand; -fx-font-size: 14; -fx-border-width: 1;");
                        }
                    }
                });

                java.time.LocalDate finalDate = date;
                dayBtn.setOnAction(e -> {
                    if (selectedStart[0] == null) {
                        selectedStart[0] = finalDate;
                        selectedEnd[0] = finalDate;
                    } else if (selectedEnd[0] == null || finalDate.isBefore(selectedStart[0])) {
                        selectedStart[0] = finalDate;
                        selectedEnd[0] = finalDate;
                    } else {
                        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(selectedStart[0], finalDate);
                        if (daysBetween > 30) {
                            showError("Limite dépassée", "La location ne peut pas dépasser 30 jours.");
                            return;
                        }
                        selectedEnd[0] = finalDate;
                    }

                    confirmBtn.setDisable(selectedStart[0] == null || selectedEnd[0] == null);
                    updateCalendarHolder[0].accept(month);
                });

                daysGrid.getChildren().add(dayBtn);
            }
        };

        updateCalendarHolder[0].accept(currentMonth[0]);

        prevBtn.setOnAction(e -> {
            currentMonth[0] = currentMonth[0].minusMonths(1);
            updateCalendarHolder[0].accept(currentMonth[0]);
        });

        nextBtn.setOnAction(e -> {
            currentMonth[0] = currentMonth[0].plusMonths(1);
            updateCalendarHolder[0].accept(currentMonth[0]);
        });

        calendarGrid.getChildren().add(daysGrid);
        calendarContainer.getChildren().addAll(headerBox, calendarGrid);
        return calendarContainer;
    }

    private static Integer parseJours(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int v = Integer.parseInt(value.trim());
            if (v < 1 || v > 30) {
                return null;
            }
            return v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void showDetailsDialog(Livre livre) {
        Dialog<javafx.scene.control.ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Détails du livre");

        javafx.scene.control.ButtonType openPdf = new javafx.scene.control.ButtonType("📖 Ouvrir PDF", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(openPdf, javafx.scene.control.ButtonType.CLOSE);
        Button openPdfButton = (Button) dialog.getDialogPane().lookupButton(openPdf);
        openPdfButton.setStyle("-fx-padding: 8 16; -fx-font-size: 12; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        ImageView imageView = new ImageView();
        imageView.setFitWidth(140);
        imageView.setFitHeight(180);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);");
        if (livre.getImage() != null && !livre.getImage().isBlank()) {
            imageView.setImage(toImage(livre.getImage()));
        }

        // Header section - Compact layout
        javafx.scene.control.Label title = new javafx.scene.control.Label(livre.getTitre() == null ? "" : livre.getTitre());
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1f2937; -fx-wrap-text: true;");
        title.setMaxWidth(280);
        title.setWrapText(true);

        // Author
        HBox authorBox = new HBox(8);
        Label authorIcon = new Label("✍️");
        authorIcon.setStyle("-fx-font-size: 14;");
        javafx.scene.control.Label authorLabel = new javafx.scene.control.Label(livre.getAuteur() == null ? "Non spécifié" : livre.getAuteur());
        authorLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");
        authorBox.getChildren().addAll(authorIcon, authorLabel);
        authorBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Category
        HBox categoryBox = new HBox(8);
        Label categoryIcon = new Label("📚");
        categoryIcon.setStyle("-fx-font-size: 14;");
        javafx.scene.control.Label categoryLabel = new javafx.scene.control.Label(livre.getCategorie() == null ? "Non spécifiée" : livre.getCategorie());
        categoryLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");
        categoryBox.getChildren().addAll(categoryIcon, categoryLabel);
        categoryBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Price & Availability in same row
        HBox priceDispoBox = new HBox(20);
        
        HBox priceBox = new HBox(6);
        Label priceIcon = new Label("💰");
        priceIcon.setStyle("-fx-font-size: 14;");
        javafx.scene.control.Label priceLabel = new javafx.scene.control.Label((livre.getPrixLocation() == null ? "0" : livre.getPrixLocation()) + " DT");
        priceLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #3b82f6; -fx-font-weight: bold;");
        priceBox.getChildren().addAll(priceIcon, priceLabel);
        priceBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        HBox dispoBox = new HBox(6);
        Label dispoIcon = new Label(Boolean.TRUE.equals(livre.getDisponibilite()) ? "✅" : "❌");
        dispoIcon.setStyle("-fx-font-size: 14;");
        javafx.scene.control.Label dispoLabel = new javafx.scene.control.Label(Boolean.TRUE.equals(livre.getDisponibilite()) ? "Disponible" : "Indisponible");
        dispoLabel.setStyle("-fx-font-size: 12; -fx-text-fill: " + (Boolean.TRUE.equals(livre.getDisponibilite()) ? "#10b981" : "#ef4444") + "; -fx-font-weight: bold;");
        dispoBox.getChildren().addAll(dispoIcon, dispoLabel);
        dispoBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        priceDispoBox.getChildren().addAll(priceBox, dispoBox);

        // Right column - Title and details
        VBox rightColumn = new VBox(8);
        rightColumn.getChildren().addAll(title, authorBox, categoryBox, priceDispoBox);
        rightColumn.setStyle("-fx-padding: 0;");

        // Header box with image and right column
        HBox headerBox = new HBox(15);
        headerBox.getChildren().addAll(imageView, rightColumn);
        headerBox.setStyle("-fx-padding: 15; -fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-background-radius: 10;");
        headerBox.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        // Description section - Dynamic height
        VBox descSection = new VBox(8);
        descSection.setStyle("-fx-padding: 15; -fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-background-radius: 10; -fx-background-color: white;");

        Label descTitle = new Label("📖 Description");
        descTitle.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        TextArea desc = new TextArea(livre.getDescription() == null || livre.getDescription().isEmpty() ? "Aucune description disponible" : livre.getDescription());
        desc.setEditable(false);
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 12; -fx-text-fill: #4b5563; -fx-control-inner-background: #f9fafb; -fx-padding: 8;");
        
        // Calculate optimal height based on text content
        int lineCount = (livre.getDescription() == null ? 1 : livre.getDescription().split("\n").length) + 2;
        int minHeight = Math.max(60, Math.min(150, lineCount * 18));
        desc.setPrefRowCount(minHeight / 18);
        desc.setMinHeight(minHeight);

        descSection.getChildren().addAll(descTitle, desc);

        // Main content
        VBox mainContent = new VBox(12);
        mainContent.getChildren().addAll(headerBox, descSection);
        mainContent.setPrefWidth(500);
        mainContent.setStyle("-fx-padding: 15;");

        LocationLivre activeLocation = null;
        if (livre.getId() != null) {
            try {
                activeLocation = locationLivreService.getActiveLocation(livre.getId(), currentUserId);
            } catch (SQLDataException ignored) {
            }
        }

        // Rental progress section
        if (activeLocation != null) {
            openPdfButton.setDisable(false);

            ProgressBar progressBar = new ProgressBar(0);
            progressBar.setPrefWidth(Double.MAX_VALUE);
            progressBar.setPrefHeight(8);
            progressBar.setStyle("-fx-accent: #3b82f6;");

            Label progressLabel = new Label();
            progressLabel.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 12; -fx-font-weight: bold;");

            HBox progressHeader = new HBox(10);
            Label rentIcon = new Label("⏱️");
            rentIcon.setStyle("-fx-font-size: 14;");
            Label rentTitle = new Label("Location en cours");
            rentTitle.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1f2937;");
            progressHeader.getChildren().addAll(rentIcon, rentTitle);
            progressHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            VBox rentBox = new VBox(10, progressHeader, progressBar, progressLabel);
            rentBox.setStyle("-fx-padding: 12; -fx-background-color: #dbeafe; -fx-background-radius: 10; -fx-border-color: #3b82f6; -fx-border-radius: 10; -fx-border-width: 2;");
            mainContent.getChildren().add(rentBox);

            LocationLivre finalActiveLocation = activeLocation;
            Runnable updateProgress = () -> {
                LocalDateTime start = finalActiveLocation.getDateDebut();
                LocalDateTime end = finalActiveLocation.getDateRetour();
                if (start == null || end == null) {
                    progressBar.setProgress(0);
                    progressLabel.setText("");
                    return;
                }
                LocalDateTime now = LocalDateTime.now();
                long totalSeconds = Duration.between(start, end).getSeconds();
                long elapsedSeconds = Duration.between(start, now).getSeconds();
                double progress = totalSeconds > 0 ? (double) elapsedSeconds / totalSeconds : 1.0;
                if (progress > 1.0) {
                    progress = 1.0;
                } else if (progress < 0) {
                    progress = 0.0;
                }
                progressBar.setProgress(progress);

                Duration left = Duration.between(now, end);
                if (left.isNegative() || left.isZero()) {
                    progressLabel.setText("⚠️ Location expirée");
                    progressLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12; -fx-font-weight: bold;");
                } else {
                    long daysLeft = left.toDays();
                    long hoursLeft = left.toHours() % 24;
                    long minutesLeft = left.toMinutes() % 60;
                    String timeRemaining;
                    if (daysLeft > 0) {
                        timeRemaining = daysLeft + "j " + hoursLeft + "h " + minutesLeft + "min";
                    } else if (hoursLeft > 0) {
                        timeRemaining = hoursLeft + "h " + minutesLeft + "min";
                    } else {
                        timeRemaining = minutesLeft + "min";
                    }
                    progressLabel.setText("⏳ " + timeRemaining + " restants");
                    progressLabel.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 12; -fx-font-weight: bold;");
                }
            };

            Timeline timeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> updateProgress.run()));
            timeline.setCycleCount(Timeline.INDEFINITE);
            dialog.setOnShown(e -> {
                updateProgress.run();
                timeline.play();
            });
            dialog.setOnHidden(e -> timeline.stop());
        } else {
            openPdfButton.setDisable(true);
            openPdfButton.setStyle("-fx-padding: 8 16; -fx-font-size: 12; -fx-background-color: #d1d5db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: not-allowed;");
        }

        ScrollPane scrollPane = new ScrollPane(mainContent);
        scrollPane.setStyle("-fx-background-color: white; -fx-padding: 0;");
        scrollPane.setFitToWidth(true);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().setStyle("-fx-background-color: white;");

        dialog.setResultConverter(bt -> bt);
        dialog.showAndWait().ifPresent(result -> {
            if (result == openPdf) {
                openPdfInApp(livre);
            }
        });
    }

    private void openPdfInApp(Livre livre) {
        if (livre.getId() == null) {
            showError("PDF", "Livre invalide.");
            return;
        }

        try {
            LocationLivre loc = locationLivreService.getActiveLocation(livre.getId(), currentUserId);
            if (loc == null) {
                showError("PDF", "Vous devez louer ce livre pour l'ouvrir.");
                return;
            }
        } catch (SQLDataException e) {
            showError("PDF", "Impossible de vérifier la location.");
            return;
        }

        String pdfSource = livre.getFichierPdf();
        if (pdfSource == null || pdfSource.isBlank()) {
            showError("PDF", "Aucun PDF pour ce livre.");
            return;
        }

        // Run in background thread to prevent UI freeze during font cache rebuild/loading
        CompletableFuture.runAsync(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/amateur/BookReader.fxml"));
                Parent root = loader.load();
                BookReaderController controller = loader.getController();
                
                // This might take a while if PDFBox is rebuilding its font cache
                controller.setPdfBytes(loadPdfBytes(pdfSource));

                Platform.runLater(() -> {
                    Stage stage = new Stage();
                    String title = livre.getTitre() == null ? "Lecteur PDF" : livre.getTitre();
                    stage.setTitle(title);
                    stage.setScene(new Scene(root));
                    controller.setStage(stage);
                    controller.setBookTitle(title);
                    stage.setOnHidden(e -> controller.close());
                    stage.show();
                });
            } catch (IOException e) {
                Platform.runLater(() -> showError("PDF", "Impossible d'ouvrir le lecteur."));
            }
        });
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

    private byte[] loadPdfBytes(String pdfSource) throws IOException {
        if (pdfSource == null || pdfSource.isBlank()) {
            throw new IOException("Source PDF vide");
        }
        if (pdfSource.startsWith("http://") || pdfSource.startsWith("https://")) {
            try (InputStream inputStream = new URL(pdfSource).openStream()) {
                return inputStream.readAllBytes();
            }
        }
        if (pdfSource.startsWith("file:")) {
            try {
                return java.nio.file.Files.readAllBytes(Path.of(URI.create(pdfSource)));
            } catch (Exception ex) {
                return java.nio.file.Files.readAllBytes(Path.of(new URL(pdfSource).getPath()));
            }
        }
        return java.nio.file.Files.readAllBytes(Path.of(pdfSource));
    }

    private static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }
}
