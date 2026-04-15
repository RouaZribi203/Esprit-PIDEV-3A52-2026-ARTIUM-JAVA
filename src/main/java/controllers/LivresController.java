package controllers;

import entities.CollectionOeuvre;
import entities.Livre;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import services.CollectionService;
import services.JdbcCollectionService;
import services.JdbcLivreService;
import services.LivreService;

import java.awt.Desktop;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLDataException;
import java.util.ArrayList;
import java.util.List;

public class LivresController {
    private final LivreService livreService = new JdbcLivreService();
    private final CollectionService collectionService = new JdbcCollectionService();
    private final ObservableList<Livre> livres = FXCollections.observableArrayList();
    private final ObservableList<CollectionOeuvre> collections = FXCollections.observableArrayList();
    private final List<Livre> allLivres = new ArrayList<>();
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(250));

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
    private ScrollPane cardsScroll;

    @FXML
    private TilePane cardsTile;

    @FXML
    private Label couvertureLabel;

    @FXML
    private Label pdfLabel;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private VBox formPanel;

    @FXML
    private Label formTitleLabel;

    @FXML
    private Button addBookButton;

    @FXML
    private ImageView couvertureImageView;

    private Livre selected;
    private byte[] selectedImageBytes;
    private byte[] selectedPdfBytes;

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

        // Configure ComboBox
        if (collectionComboBox != null) {
            collectionComboBox.setItems(collections);
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

        loadCollections();
        refresh();

        if (searchField != null) {
            searchDebounce.setOnFinished(e -> applyFilter(searchField.getText()));
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                searchDebounce.stop();
                searchDebounce.playFromStart();
            });
        }
    }

    private void loadCollections() {
        try {
            collections.setAll(collectionService.getAll());
        } catch (SQLDataException e) {
            showError("Erreur collections", "Impossible de charger les collections: " + e.getMessage());
        }
    }

    @FXML
    private void onSearch() {
        applyFilter(searchField.getText());
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    @FXML
    private void onChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image de couverture");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers Image", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(cardsTile.getScene().getWindow());
        if (file != null) {
            try {
                selectedImageBytes = Files.readAllBytes(file.toPath());
                couvertureLabel.setText(file.getName());
                if (couvertureImageView != null) {
                    couvertureImageView.setImage(new Image(new ByteArrayInputStream(selectedImageBytes)));
                }
            } catch (IOException e) {
                showError("Erreur", "Impossible de lire l'image.");
            }
        }
    }

    @FXML
    private void onChoosePdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un fichier PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        File file = fileChooser.showOpenDialog(cardsTile.getScene().getWindow());
        if (file != null) {
            try {
                selectedPdfBytes = Files.readAllBytes(file.toPath());
                pdfLabel.setText(file.getName());
            } catch (IOException e) {
                showError("Erreur", "Impossible de lire le PDF.");
            }
        }
    }

    private void showFormDialog(Livre livre) {
        this.selected = livre;
        selectedImageBytes = null;
        selectedPdfBytes = null;

        Dialog<javafx.scene.control.ButtonType> dialog = new Dialog<>();
        dialog.setTitle(livre == null ? "Ajouter un livre" : "Modifier le livre");
        
        // Setup content
        if (livre != null) {
            formTitleLabel.setText("Modifier le livre");
            titreField.setText(livre.getTitre());
            categorieField.setText(livre.getCategorie());
            prixField.setText(livre.getPrixLocation() == null ? "" : String.valueOf(livre.getPrixLocation()));
            descriptionArea.setText(livre.getDescription() == null ? "" : livre.getDescription());
            
            CollectionOeuvre col = collections.stream()
                    .filter(c -> c.getId().equals(livre.getCollectionId()))
                    .findFirst()
                    .orElse(null);
            collectionComboBox.setValue(col);

            if (livre.getImage() != null && livre.getImage().length > 0) {
                couvertureImageView.setImage(new Image(new ByteArrayInputStream(livre.getImage())));
                couvertureLabel.setText("(Image actuelle)");
            } else {
                couvertureImageView.setImage(null);
                couvertureLabel.setText("Aucune image");
            }
            pdfLabel.setText(livre.getFichierPdf() != null && livre.getFichierPdf().length > 0 ? "(PDF actuel)" : "Aucun PDF");
        } else {
            formTitleLabel.setText("Ajouter un nouveau livre");
            clearFormFields();
        }

        dialog.getDialogPane().setContent(formPanel);

        javafx.scene.control.ButtonType saveBtnType = new javafx.scene.control.ButtonType(livre == null ? "Ajouter" : "Enregistrer", ButtonBar.ButtonData.OK_DONE);
        javafx.scene.control.ButtonType cancelBtnType = new javafx.scene.control.ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, cancelBtnType);

        if (livre != null) {
            javafx.scene.control.ButtonType deleteBtnType = new javafx.scene.control.ButtonType("Supprimer", ButtonBar.ButtonData.OTHER);
            dialog.getDialogPane().getButtonTypes().add(deleteBtnType);
        }

        final Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveBtnType);
        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!validateAndSave()) {
                event.consume();
            }
        });

        dialog.showAndWait().ifPresent(result -> {
            if (result.getButtonData() == ButtonBar.ButtonData.OTHER) {
                onDeleteLivre(livre);
            }
            clearFormFields();
        });
    }

    private boolean validateAndSave() {
        boolean hasError = false;
        StringBuilder errorMessage = new StringBuilder("Veuillez corriger les erreurs suivantes :\n");

        String titre = titreField.getText();
        if (titre == null || titre.trim().isEmpty() || titre.trim().length() < 3) {
            errorMessage.append("- Le titre doit contenir au moins 3 caractères.\n");
            hasError = true;
            titreField.setStyle("-fx-border-color: #ef4444;");
        } else {
            titreField.setStyle("");
        }

        String categorie = categorieField.getText();
        if (categorie == null || categorie.trim().isEmpty()) {
            errorMessage.append("- La catégorie est obligatoire.\n");
            hasError = true;
            categorieField.setStyle("-fx-border-color: #ef4444;");
        } else {
            categorieField.setStyle("");
        }

        String prixStr = prixField.getText();
        Double prixLocation = 0.0;
        try {
            prixLocation = Double.parseDouble(prixStr.trim());
            if (prixLocation < 0) throw new NumberFormatException();
            prixField.setStyle("");
        } catch (Exception e) {
            errorMessage.append("- Le prix doit être un nombre positif.\n");
            hasError = true;
            prixField.setStyle("-fx-border-color: #ef4444;");
        }

        if (collectionComboBox.getValue() == null) {
            errorMessage.append("- Veuillez sélectionner une collection.\n");
            hasError = true;
            collectionComboBox.setStyle("-fx-border-color: #ef4444;");
        } else {
            collectionComboBox.setStyle("");
        }

        if (descriptionArea.getText().trim().isEmpty()) {
            errorMessage.append("- La description est obligatoire.\n");
            hasError = true;
            descriptionArea.setStyle("-fx-border-color: #ef4444;");
        } else {
            descriptionArea.setStyle("");
        }

        boolean hasImage = selectedImageBytes != null || (selected != null && selected.getImage() != null && selected.getImage().length > 0);
        if (!hasImage) {
            errorMessage.append("- Une image de couverture est obligatoire.\n");
            hasError = true;
        }

        boolean hasPdf = selectedPdfBytes != null || (selected != null && selected.getFichierPdf() != null && selected.getFichierPdf().length > 0);
        if (!hasPdf) {
            errorMessage.append("- Un fichier PDF est obligatoire.\n");
            hasError = true;
        }

        if (hasError) {
            showError("Erreur de validation", errorMessage.toString());
            return false;
        }

        Livre livre = selected != null ? selected : new Livre();
        livre.setTitre(titre.trim());
        livre.setCategorie(categorie.trim());
        livre.setPrixLocation(prixLocation);
        livre.setDescription(descriptionArea.getText().trim());
        livre.setCollectionId(collectionComboBox.getValue().getId());

        if (selectedImageBytes != null) livre.setImage(selectedImageBytes);
        if (selectedPdfBytes != null) livre.setFichierPdf(selectedPdfBytes);

        try {
            if (selected == null) {
                livreService.add(livre);
                showInfo("Livre ajouté", "Le livre a été ajouté avec succès.");
            } else {
                livreService.update(livre);
                showInfo("Livre modifié", "Le livre a été modifié avec succès.");
            }
            refresh();
            return true;
        } catch (SQLDataException e) {
            showError("Erreur", "Impossible d'enregistrer : " + e.getMessage());
            return false;
        }
    }

    @FXML
    private void onShowForm() {
        showFormDialog(null);
    }

    @FXML
    private void onHideForm() {
        // Form is handled by Dialog now
    }

    private void refresh() {
        try {
            allLivres.clear();
            allLivres.addAll(livreService.getAll());
            applyFilter(searchField.getText());
        } catch (SQLDataException e) {
            showError("Chargement impossible", e.getMessage());
        }
    }

    private void clearFormFields() {
        selected = null;
        selectedImageBytes = null;
        selectedPdfBytes = null;
        if (titreField != null) titreField.clear();
        if (categorieField != null) categorieField.clear();
        if (prixField != null) {
            prixField.clear();
            prixField.setStyle("");
        }
        if (collectionComboBox != null) collectionComboBox.setValue(null);
        if (couvertureLabel != null) couvertureLabel.setText("Aucune image");
        if (couvertureImageView != null) couvertureImageView.setImage(null);
        if (pdfLabel != null) pdfLabel.setText("Aucun PDF");
        if (descriptionArea != null) descriptionArea.clear();
    }

    private void applyFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        List<Livre> filtered = allLivres.stream()
                .filter(l -> q.isEmpty()
                        || contains(l.getTitre(), q)
                        || contains(l.getCategorie(), q)
                        || contains(l.getAuteur(), q))
                .toList();
        livres.setAll(filtered);
        updateCards(filtered);
    }

    private void updateCards(List<Livre> items) {
        if (cardsTile == null) return;
        cardsTile.getChildren().setAll(items.stream().map(this::createBookCard).toList());
    }

    private Node createBookCard(Livre livre) {
        VBox card = new VBox(12);
        card.getStyleClass().add("book-card");
        card.setPrefWidth(200);
        card.setPadding(new Insets(15));
        card.setAlignment(javafx.geometry.Pos.TOP_CENTER);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(160);
        imageView.setFitHeight(220);
        imageView.setPreserveRatio(true);
        if (livre.getImage() != null && livre.getImage().length > 0) {
            imageView.setImage(new Image(new ByteArrayInputStream(livre.getImage())));
        }
        
        StackPane imageContainer = new StackPane(imageView);
        imageContainer.getStyleClass().add("card-image-container");

        Label title = new Label(livre.getTitre());
        title.getStyleClass().add("card-title");
        title.setWrapText(true);
        title.setAlignment(javafx.geometry.Pos.CENTER);

        Label author = new Label(livre.getAuteur());
        author.getStyleClass().add("card-author");

        Label price = new Label(String.format("%.2f TND", livre.getPrixLocation()));
        price.getStyleClass().add("card-price");

        Button editBtn = new Button("Modifier");
        editBtn.getStyleClass().add("secondary-button");
        editBtn.setMaxWidth(Double.MAX_VALUE);
        editBtn.setOnAction(e -> showFormDialog(livre));

        card.getChildren().addAll(imageContainer, title, author, price, editBtn);
        
        // Add hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-border-color: #3d3bc2; -fx-background-color: #f5f3ff;"));
        card.setOnMouseExited(e -> card.setStyle(""));

        return card;
    }

    private void onDeleteLivre(Livre livre) {
        if (livre == null || livre.getId() == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer le livre ?");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer '" + livre.getTitre() + "' ? Cette action est irréversible.");
        
        confirm.showAndWait().ifPresent(type -> {
            if (type == javafx.scene.control.ButtonType.OK) {
                try {
                    livreService.delete(livre.getId());
                    showInfo("Supprimé", "Le livre a été supprimé.");
                    refresh();
                } catch (SQLDataException e) {
                    showError("Erreur", "Suppression impossible : " + e.getMessage());
                }
            }
        });
    }

    private static boolean contains(String value, String queryLower) {
        return value != null && value.toLowerCase().contains(queryLower);
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
