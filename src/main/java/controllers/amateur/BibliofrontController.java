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
        imageView.setFitWidth(160);
        imageView.setFitHeight(220);
        imageView.setPreserveRatio(true);
        if (livre.getImage() != null && !livre.getImage().isBlank()) {
            imageView.setImage(toImage(livre.getImage()));
        }

        Label title = new Label(livre.getTitre() == null ? "" : livre.getTitre());
        title.setStyle("-fx-font-weight: 700;");

        Label meta = new Label((livre.getCategorie() == null ? "" : livre.getCategorie()) + "  •  " + (livre.getPrixLocation() == null ? "0" : livre.getPrixLocation()));
        Label dispo = new Label(Boolean.TRUE.equals(livre.getDisponibilite()) ? "Disponible" : "Indisponible");

        Button details = new Button("Voir détails");
        details.setOnAction(e -> showDetailsDialog(livre));

        Button rent = new Button("Louer");
        rent.setDisable(!Boolean.TRUE.equals(livre.getDisponibilite()));
        rent.setOnAction(e -> louerLivreAvecDialog(livre));

        HBox actions = new HBox(10, details, rent);
        HBox.setHgrow(details, Priority.NEVER);
        HBox.setHgrow(rent, Priority.NEVER);

        VBox card = new VBox(10, imageView, title, meta, dispo, actions);
        card.setStyle("-fx-padding: 12; -fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-background-radius: 10;");
        VBox.setVgrow(imageView, Priority.NEVER);
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
        Integer jours = askNombreDeJours();
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

    private Integer askNombreDeJours() {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Durée de location");
        javafx.scene.control.ButtonType louer = new javafx.scene.control.ButtonType("Louer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(louer, javafx.scene.control.ButtonType.CANCEL);

        TextField joursField = new TextField();
        joursField.setPromptText("Ex: 7");
        VBox content = new VBox(10, new Label("Nombre de jours (1 - 30)"), joursField);
        dialog.getDialogPane().setContent(content);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(louer);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            Integer val = parseJours(joursField.getText());
            if (val == null) {
                event.consume();
                showError("Validation", "Veuillez saisir un nombre de jours valide (1 à 30).");
            }
        });

        dialog.setResultConverter(bt -> bt == louer ? parseJours(joursField.getText()) : null);
        return dialog.showAndWait().orElse(null);
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

        javafx.scene.control.ButtonType openPdf = new javafx.scene.control.ButtonType("Ouvrir PDF", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(openPdf, javafx.scene.control.ButtonType.CLOSE);
        Button openPdfButton = (Button) dialog.getDialogPane().lookupButton(openPdf);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(160);
        imageView.setFitHeight(220);
        imageView.setPreserveRatio(true);
        if (livre.getImage() != null && !livre.getImage().isBlank()) {
            imageView.setImage(toImage(livre.getImage()));
        }

        javafx.scene.control.Label title = new javafx.scene.control.Label(livre.getTitre() == null ? "" : livre.getTitre());
        title.setStyle("-fx-font-size: 16; -fx-font-weight: 700;");
        javafx.scene.control.Label auteur = new javafx.scene.control.Label("Auteur: " + (livre.getAuteur() == null ? "" : livre.getAuteur()));
        javafx.scene.control.Label cat = new javafx.scene.control.Label("Catégorie: " + (livre.getCategorie() == null ? "" : livre.getCategorie()));
        javafx.scene.control.Label prix = new javafx.scene.control.Label("Prix location: " + (livre.getPrixLocation() == null ? "0" : livre.getPrixLocation()));
        javafx.scene.control.Label dispo = new javafx.scene.control.Label("Disponibilité: " + (Boolean.TRUE.equals(livre.getDisponibilite()) ? "Disponible" : "Indisponible"));

        TextArea desc = new TextArea(livre.getDescription() == null ? "" : livre.getDescription());
        desc.setEditable(false);
        desc.setWrapText(true);
        desc.setPrefRowCount(6);

        VBox content = new VBox(12, imageView, title, auteur, cat, prix, dispo, desc);
        content.setPrefWidth(420);
        content.setStyle("-fx-padding: 12;");

        LocationLivre activeLocation = null;
        if (livre.getId() != null) {
            try {
                activeLocation = locationLivreService.getActiveLocation(livre.getId(), currentUserId);
            } catch (SQLDataException ignored) {
            }
        }

        if (activeLocation == null) {
            openPdfButton.setDisable(true);
        } else {
            openPdfButton.setDisable(false);

            ProgressBar progressBar = new ProgressBar(0);
            progressBar.setPrefWidth(Double.MAX_VALUE);
            progressBar.setStyle("-fx-accent: #3b82f6;");

            Label progressLabel = new Label();
            progressLabel.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 12;");

            VBox rentBox = new VBox(8, new Label("Location en cours"), progressBar, progressLabel);
            rentBox.setStyle("-fx-padding: 12; -fx-background-color: #eff6ff; -fx-background-radius: 10; -fx-border-color: #bfdbfe; -fx-border-radius: 10;");
            content.getChildren().add(1, rentBox);

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
                    progressLabel.setText("Location expirée");
                } else {
                    long daysLeft = left.toDays();
                    long hoursLeft = left.toHours() % 24;
                    long minutesLeft = left.toMinutes() % 60;
                    if (daysLeft > 0) {
                        progressLabel.setText(daysLeft + " jours " + hoursLeft + " h restants");
                    } else if (hoursLeft > 0) {
                        progressLabel.setText(hoursLeft + " h " + minutesLeft + " min restants");
                    } else {
                        progressLabel.setText(minutesLeft + " min restants");
                    }
                }
            };

            Timeline timeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> updateProgress.run()));
            timeline.setCycleCount(Timeline.INDEFINITE);
            dialog.setOnShown(e -> {
                updateProgress.run();
                timeline.play();
            });
            dialog.setOnHidden(e -> timeline.stop());
        }

        dialog.getDialogPane().setContent(content);

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
