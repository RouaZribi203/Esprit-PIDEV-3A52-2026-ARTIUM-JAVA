package controllers.artist;

import controllers.amateur.BookReaderController;
import entities.CollectionOeuvre;
import entities.Livre;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import services.CollectionService;
import services.JdbcCollectionService;
import services.JdbcLivreService;
import services.LivreService;
import utils.SessionManager;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLDataException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BibliothequArtisteController {
    private final LivreService livreService = new JdbcLivreService();
    private final CollectionService collectionService = new JdbcCollectionService();
    private final ObservableList<Livre> livres = FXCollections.observableArrayList();
    private final ObservableList<CollectionOeuvre> collections = FXCollections.observableArrayList();

    private int getCurrentArtistId() {
        return SessionManager.isLoggedIn() ? SessionManager.getCurrentUser().getId() : 1;
    }

    private String getCurrentArtistName() {
        return SessionManager.isLoggedIn() ? 
               (SessionManager.getCurrentUser().getPrenom() + " " + SessionManager.getCurrentUser().getNom()) : 
               "Artiste";
    }

    @FXML
    private TilePane cardsTile;

    @FXML
    private VBox formPanel;

    @FXML
    private StackPane formStack;

    @FXML
    private TextField searchField;

    @FXML
    private TextField titreField;

    @FXML
    private TextField categorieField;

    @FXML
    private TextField prixField;

    @FXML
    private ComboBox<CollectionOeuvre> collectionComboBox;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private Label pdfLabel;

    @FXML
    private Button saveButton;

    @FXML
    private Button deleteButton;

    @FXML
    private ImageView bookImageView;

    @FXML
    private Button chooseImageButton;

    @FXML
    private Label formTitleLabel;

    private Livre selected;
    private byte[] selectedPdfBytes;
    private byte[] selectedImageBytes;

    @FXML
    public void initialize() {
        // Add numeric filter to price field
        if (prixField != null) {
            prixField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d*(\\.\\d*)?")) {
                    prixField.setText(oldValue);
                }
            });
        }

        collectionComboBox.setItems(collections);
        collectionComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(CollectionOeuvre collection) {
                return collection == null ? "" : collection.getTitre();
            }

            @Override
            public CollectionOeuvre fromString(String string) {
                return null;
            }
        });

        loadCollections(); // Load collections FIRST
        refresh();         // Then refresh books (now has fallback filtering)
        clearForm();
    }

    private void loadCollections() {
        try {
            collections.setAll(collectionService.getByArtist(getCurrentArtistId()));
        } catch (SQLDataException e) {
            showError("Chargement collections", e.getMessage());
        }
    }

    private void refresh() {
        try {
            // First try to get by artist specifically
            List<Livre> allBooks = livreService.getByArtist(getCurrentArtistId());
            
            // If empty, it might be a link issue, try fetching all and filtering manually as fallback
            if (allBooks.isEmpty()) {
                List<Livre> everything = livreService.getAll();
                allBooks = everything.stream()
                        .filter(l -> {
                            // Link through collections
                            for (CollectionOeuvre c : collections) {
                                if (c.getId().equals(l.getCollectionId())) return true;
                            }
                            // Fallback: if book auteur matches artist name
                            String artistName = getCurrentArtistName().toLowerCase();
                            return l.getAuteur() != null && l.getAuteur().toLowerCase().contains(artistName);
                        })
                        .toList();
            }
            
            livres.setAll(allBooks);
            updateCardsView(livres);
        } catch (SQLDataException e) {
            showError("Chargement impossible", e.getMessage());
        }
    }

    private void updateCardsView(List<Livre> books) {
        cardsTile.getChildren().clear();
        for (Livre livre : books) {
            VBox card = createBookCard(livre);
            cardsTile.getChildren().add(card);
        }
    }

    private VBox createBookCard(Livre livre) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(216);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        if (livre.getImage() != null && livre.getImage().length > 0) {
            imageView.setImage(new Image(new ByteArrayInputStream(livre.getImage())));
        }

        Label title = new Label(livre.getTitre() == null ? "" : livre.getTitre());
        title.setStyle("-fx-font-weight: 700; -fx-font-size: 14px;");

        Label categorie = new Label(livre.getCategorie() == null ? "" : livre.getCategorie());
        categorie.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");

        Label prix = new Label(livre.getPrixLocation() == null ? "0" : String.valueOf(livre.getPrixLocation()) + " TND");
        prix.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #3b82f6;");

        Button detailsBtn = new Button("Détails");
        detailsBtn.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 5 10; -fx-font-size: 11px;");
        detailsBtn.setOnAction(e -> showBookDetails(livre));

        Button modifyBtn = new Button("Modifier");
        modifyBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 5 10; -fx-font-size: 11px;");
        modifyBtn.setOnAction(e -> selectBookForEdit(livre));

        Button deleteBtn = new Button("Supprimer");
        deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 5 10; -fx-font-size: 11px;");
        deleteBtn.setOnAction(e -> deleteBook(livre));

        HBox buttons = new HBox(5, detailsBtn, modifyBtn, deleteBtn);

        VBox card = new VBox(10, imageView, title, categorie, prix, buttons);
        card.setStyle("-fx-padding: 12; -fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-background-radius: 10; -fx-pref-width: 240;");
        VBox.setVgrow(imageView, Priority.NEVER);

        return card;
    }

    private void selectBookForEdit(Livre livre) {
        selected = livre;
        selectedPdfBytes = null;
        selectedImageBytes = null;
        titreField.setText(livre.getTitre());
        categorieField.setText(livre.getCategorie());
        prixField.setText(livre.getPrixLocation() == null ? "" : String.valueOf(livre.getPrixLocation()));
        descriptionArea.setText(livre.getDescription());
        pdfLabel.setText(livre.getFichierPdf() != null && livre.getFichierPdf().length > 0 ? "PDF sélectionné" : "Aucun PDF");
        if (livre.getImage() != null && livre.getImage().length > 0) {
            bookImageView.setImage(new Image(new ByteArrayInputStream(livre.getImage())));
        } else {
            bookImageView.setImage(null);
        }
        if (livre.getCollectionId() != null) {
            for (CollectionOeuvre c : collections) {
                if (c.getId().equals(livre.getCollectionId())) {
                    collectionComboBox.setValue(c);
                    break;
                }
            }
        }

        boolean isRented = !Boolean.TRUE.equals(livre.getDisponibilite());
        prixField.setDisable(isRented);
        if (isRented) {
            prixField.setStyle("-fx-background-color: #f3f4f6; -fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 10 14; -fx-font-size: 13px; -fx-text-fill: #9ca3af;");
        } else {
            prixField.setStyle("");
        }

        deleteButton.setDisable(false);
        formStack.setVisible(true);
        formPanel.setVisible(true);
        formTitleLabel.setText("Modifier le livre");
        saveButton.setText("✓ Enregistrer");
    }

    private void showBookDetails(Livre livre) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails du livre - " + livre.getTitre());
        dialog.setHeaderText(null);
        dialog.setResizable(true);

        VBox content = new VBox(15);
        content.setPadding(new Insets(10));

        ImageView bookImage = new ImageView();
        bookImage.setFitWidth(300);
        bookImage.setFitHeight(180);
        bookImage.setPreserveRatio(true);
        if (livre.getImage() != null && livre.getImage().length > 0) {
            bookImage.setImage(new Image(new ByteArrayInputStream(livre.getImage())));
        }

        Label titleLabel = new Label(livre.getTitre());
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label categorieLabel = new Label("Catégorie: " + (livre.getCategorie() != null ? livre.getCategorie() : "N/A"));
        categorieLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151;");

        Label prixLabel = new Label("Prix: " + (livre.getPrixLocation() != null ? livre.getPrixLocation() : "0") + " TND");
        prixLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3b82f6;");

        Label auteurLabel = new Label("Auteur: " + (livre.getAuteur() != null ? livre.getAuteur() : "N/A"));
        auteurLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151;");

        Label descLabel = new Label("Description:");
        descLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #374151;");

        TextArea descArea = new TextArea(livre.getDescription() != null ? livre.getDescription() : "Aucune description");
        descArea.setEditable(false);
        descArea.setWrapText(true);
        descArea.setPrefRowCount(4);
        descArea.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-font-size: 13px;");

        ButtonType closeButton = new ButtonType("Fermer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(closeButton);

        VBox pdfSection = new VBox(8);
        if (livre.getFichierPdf() != null && livre.getFichierPdf().length > 0) {
            Label pdfTitle = new Label("Aperçu du PDF:");
            pdfTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #374151;");

            ImageView pdfPreview = new ImageView();
            pdfPreview.setFitWidth(350);
            pdfPreview.setFitHeight(250);
            pdfPreview.setPreserveRatio(true);
            pdfPreview.setSmooth(true);

            try {
                PDDocument pdfDoc = PDDocument.load(new ByteArrayInputStream(livre.getFichierPdf()));
                PDFRenderer renderer = new PDFRenderer(pdfDoc);
                if (pdfDoc.getNumberOfPages() > 0) {
                    BufferedImage buffered = renderer.renderImageWithDPI(0, 100);
                    pdfPreview.setImage(SwingFXUtils.toFXImage(buffered, null));
                }
                pdfDoc.close();
            } catch (Exception e) {
                pdfPreview.setVisible(false);
            }

            Button openPdfBtn = new Button("📖 Ouvrir le PDF");
            openPdfBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 8 16; -fx-background-radius: 8;");
            openPdfBtn.setOnAction(e -> openPdfInNewWindow(livre));

            pdfSection.getChildren().addAll(pdfTitle, pdfPreview, openPdfBtn);
        } else {
            Label noPdf = new Label("Aucun PDF disponible");
            noPdf.setStyle("-fx-text-fill: #9ca3af; -fx-font-style: italic;");
            pdfSection.getChildren().add(noPdf);
        }

        content.getChildren().addAll(
            new HBox(15, bookImage, new VBox(10, titleLabel, categorieLabel, prixLabel, auteurLabel)),
            descLabel, descArea, pdfSection
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(700, 600);
        dialog.showAndWait();
    }

    private void openPdfInNewWindow(Livre livre) {
        if (livre.getFichierPdf() == null || livre.getFichierPdf().length == 0) {
            showError("PDF", "Aucun fichier PDF pour ce livre.");
            return;
        }

        // Run in background thread to prevent UI freeze during font cache rebuild/loading
        CompletableFuture.runAsync(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/amateur/BookReader.fxml"));
                Parent root = loader.load();
                BookReaderController controller = loader.getController();
                
                // This might take a while if PDFBox is rebuilding its font cache
                controller.setPdfBytes(livre.getFichierPdf());

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

    private void deleteBook(Livre livre) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vraiment supprimer le livre \"" + livre.getTitre() + "\" ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    livreService.delete(livre.getId());
                    showInfo("Supprimé", "Le livre a été supprimé.");
                    refresh();
                    clearForm();
                } catch (SQLDataException e) {
                    showError("Erreur", e.getMessage());
                }
            }
        });
    }

    @FXML
    private void onShowForm() {
        clearForm();
        formTitleLabel.setText("Nouveau livre");
        saveButton.setText("✓ Publier");

        formStack.setVisible(true);
        formPanel.setVisible(true);
        formStack.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(300), formStack);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    @FXML
    private void onHideForm() {
        FadeTransition ft = new FadeTransition(Duration.millis(300), formStack);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            formStack.setVisible(false);
            formPanel.setVisible(false);
            clearForm();
        });
        ft.play();
    }

    @FXML
    private void onSearch() {
        try {
            List<Livre> result = livreService.search(searchField.getText(), null);
            List<Livre> artistBooks = result.stream()
                    .filter(l -> {
                        for (CollectionOeuvre c : collections) {
                            if (c.getId().equals(l.getCollectionId())) return true;
                        }
                        return false;
                    })
                    .toList();
            updateCardsView(artistBooks);
        } catch (SQLDataException e) {
            showError("Recherche impossible", e.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    @FXML
    private void onChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif")
        );
        File file = fileChooser.showOpenDialog(chooseImageButton.getScene().getWindow());
        if (file != null) {
            try {
                selectedImageBytes = Files.readAllBytes(file.toPath());
                bookImageView.setImage(new Image(new ByteArrayInputStream(selectedImageBytes)));
            } catch (IOException e) {
                showError("Erreur", "Impossible de lire l'image.");
            }
        }
    }

    @FXML
    private void onSave() {
        boolean hasError = false;
        StringBuilder errorMessage = new StringBuilder("Veuillez corriger les erreurs suivantes :\n");

        String titre = titreField.getText();
        if (titre == null || titre.trim().isEmpty()) {
            errorMessage.append("- Le champ 'Titre' est obligatoire.\n");
            hasError = true;
            titreField.setStyle("-fx-border-color: red;");
        } else if (titre.trim().length() < 3) {
            errorMessage.append("- Le champ 'Titre' doit contenir au moins 3 caractères.\n");
            hasError = true;
            titreField.setStyle("-fx-border-color: red;");
        } else {
            titreField.setStyle("");
        }

        String categorie = categorieField.getText();
        if (categorie == null || categorie.trim().isEmpty()) {
            errorMessage.append("- Le champ 'Catégorie' est obligatoire.\n");
            hasError = true;
            categorieField.setStyle("-fx-border-color: red;");
        } else {
            categorieField.setStyle("");
        }

        String prixStr = prixField.getText();
        Double prixLocation = null;
        if (prixField.isDisabled()) {
            prixLocation = selected != null ? selected.getPrixLocation() : 0.0;
        } else if (prixStr == null || prixStr.trim().isEmpty()) {
            errorMessage.append("- Le champ 'Prix location' est obligatoire.\n");
            hasError = true;
            prixField.setStyle("-fx-border-color: red;");
        } else {
            try {
                prixLocation = Double.parseDouble(prixStr.trim());
                if (prixLocation < 0) {
                    errorMessage.append("- Le champ 'Prix location' ne peut pas être négatif.\n");
                    hasError = true;
                    prixField.setStyle("-fx-border-color: red;");
                } else {
                    prixField.setStyle("");
                }
            } catch (NumberFormatException e) {
                errorMessage.append("- Le champ 'Prix location' doit être un nombre valide.\n");
                hasError = true;
                prixField.setStyle("-fx-border-color: red;");
            }
        }

        CollectionOeuvre selectedCollection = collectionComboBox.getValue();
        if (selectedCollection == null) {
            errorMessage.append("- Veuillez choisir une collection.\n");
            hasError = true;
            collectionComboBox.setStyle("-fx-border-color: red;");
        } else {
            collectionComboBox.setStyle("");
        }

        String description = descriptionArea != null ? descriptionArea.getText() : null;
        if (description == null || description.trim().isEmpty()) {
            errorMessage.append("- Le champ 'Description' est obligatoire.\n");
            hasError = true;
            if (descriptionArea != null) {
                descriptionArea.setStyle("-fx-border-color: red;");
            }
        } else {
            if (descriptionArea != null) {
                descriptionArea.setStyle("");
            }
        }

        boolean hasExistingPdf = selected != null && selected.getFichierPdf() != null && selected.getFichierPdf().length > 0;
        if (selectedPdfBytes == null && !hasExistingPdf) {
            errorMessage.append("- Le fichier PDF est obligatoire.\n");
            hasError = true;
            if (pdfLabel != null) {
                pdfLabel.setStyle("-fx-text-fill: red;");
            }
        } else {
            if (pdfLabel != null) {
                pdfLabel.setStyle("");
            }
        }

        if (hasError) {
            showError("Erreur de validation", errorMessage.toString());
            return;
        }

        Livre livre = selected != null ? selected : new Livre();
        livre.setTitre(titre.trim());
        livre.setCategorie(categorie.trim());
        livre.setPrixLocation(prixLocation);
        livre.setCollectionId(selectedCollection.getId());
        livre.setAuteur(getCurrentArtistName());

        if (descriptionArea != null) {
            livre.setDescription(description.trim());
        }
        if (selectedPdfBytes != null) {
            livre.setFichierPdf(selectedPdfBytes);
        }
        if (selectedImageBytes != null) {
            livre.setImage(selectedImageBytes);
        }

        try {
            if (selected == null) {
                livreService.add(livre);
                showInfo("Livre ajouté", "Le livre a été publié avec succès.");
            } else {
                livreService.update(livre);
                showInfo("Livre modifié", "Le livre a été modifié avec succès.");
            }
            refresh();
            clearForm();
            formPanel.setVisible(false);
            formStack.setVisible(false);
        } catch (SQLDataException e) {
            showError("Enregistrement impossible", e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        if (selected == null || selected.getId() == null) {
            showError("Suppression", "Veuillez sélectionner un livre à supprimer.");
            return;
        }
        deleteBook(selected);
    }

    @FXML
    private void onClear() {
        clearForm();
    }

    @FXML
    private void onChoosePdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un fichier PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        File file = fileChooser.showOpenDialog(saveButton.getScene().getWindow());
        if (file != null) {
            try {
                selectedPdfBytes = Files.readAllBytes(file.toPath());
                if (pdfLabel != null) {
                    pdfLabel.setText(file.getName());
                    pdfLabel.setStyle("");
                }
            } catch (IOException e) {
                showError("Erreur", "Impossible de lire le PDF.");
            }
        }
    }

    private void clearForm() {
        selected = null;
        selectedPdfBytes = null;
        selectedImageBytes = null;
        titreField.clear();
        categorieField.clear();
        prixField.clear();
        prixField.setDisable(false);
        prixField.setStyle("");
        descriptionArea.clear();
        pdfLabel.setText("Aucun PDF");
        pdfLabel.setStyle("");
        collectionComboBox.setValue(null);
        bookImageView.setImage(null);
        deleteButton.setDisable(true);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
