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
import java.sql.SQLDataException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import java.time.Duration;
import java.time.LocalDateTime;

public class BibliofrontController {
    private final LivreService livreService = new JdbcLivreService();
    private final JdbcLocationLivreService locationLivreService = new JdbcLocationLivreService();
    private final int currentUserId = 1;

    @FXML
    private TextField searchField;

    @FXML
    private ScrollPane cardsScroll;

    @FXML
    private TilePane cardsTile;

    @FXML
    private ToggleButton filterRentedToggle;

    @FXML
    public void initialize() {
        if (filterRentedToggle != null) {
            filterRentedToggle.selectedProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        }
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

    private void applyFilter() {
        try {
            List<Livre> result = livreService.search(searchField.getText(), null);
            if (filterRentedToggle != null && filterRentedToggle.isSelected()) {
                List<Integer> rentedIds = locationLivreService.getRentedLivreIds(currentUserId);
                result = result.stream().filter(l -> rentedIds.contains(l.getId())).toList();
            }
            updateView(result);
        } catch (SQLDataException e) {
            showError("Erreur de filtrage", e.getMessage());
        }
    }

    private void refresh() {
        applyFilter();
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

        byte[] pdfBytes = livre.getFichierPdf();
        if (pdfBytes == null || pdfBytes.length == 0) {
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
                controller.setPdfBytes(pdfBytes);

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
}
