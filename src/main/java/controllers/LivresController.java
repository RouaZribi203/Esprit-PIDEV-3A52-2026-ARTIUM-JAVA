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
    private TableView<Livre> livresTable;

    @FXML
    private TableColumn<Livre, Integer> colId;

    @FXML
    private TableColumn<Livre, Livre> colCouverture;

    @FXML
    private TableColumn<Livre, String> colTitre;

    @FXML
    private TableColumn<Livre, String> colAuteur;

    @FXML
    private TableColumn<Livre, String> colCategorie;

    @FXML
    private TableColumn<Livre, Double> colPrix;

    @FXML
    private TableColumn<Livre, String> colDisponibilite;

    @FXML
    private TableColumn<Livre, Livre> colDetails;

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
    private ToggleButton tableToggle;

    @FXML
    private ToggleButton cardsToggle;

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
    private Button saveButton;

    @FXML
    private Button deleteButton;

    @FXML
    private StackPane formStack;

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

        if (colId != null) colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (colCouverture != null) {
            colCouverture.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
            colCouverture.setCellFactory(col -> new TableCell<>() {
                private final ImageView imageView = new ImageView();
                {
                    imageView.setFitWidth(42);
                    imageView.setFitHeight(56);
                    imageView.setPreserveRatio(true);
                    setGraphic(imageView);
                }
                @Override
                protected void updateItem(Livre livre, boolean empty) {
                    super.updateItem(livre, empty);
                    if (empty || livre == null || livre.getImage() == null || livre.getImage().length == 0) {
                        imageView.setImage(null);
                    } else {
                        imageView.setImage(new Image(new ByteArrayInputStream(livre.getImage())));
                    }
                }
            });
        }
        if (colTitre != null) colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        if (colAuteur != null) colAuteur.setCellValueFactory(new PropertyValueFactory<>("auteur"));
        if (colCategorie != null) colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        if (colPrix != null) colPrix.setCellValueFactory(new PropertyValueFactory<>("prixLocation"));
        if (colDisponibilite != null) colDisponibilite.setCellValueFactory(cell -> new SimpleStringProperty(Boolean.TRUE.equals(cell.getValue().getDisponibilite()) ? "Disponible" : "Indisponible"));

        if (colDetails != null) {
            colDetails.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
            colDetails.setCellFactory(col -> new TableCell<>() {
                private final Button button = new Button("Voir");
                {
                    button.setOnAction(e -> {
                        Livre livre = getItem();
                        if (livre != null) {
                            showDetailsDialog(livre);
                        }
                    });
                    setGraphic(button);
                }
                @Override
                protected void updateItem(Livre livre, boolean empty) {
                    super.updateItem(livre, empty);
                    if (empty || livre == null) {
                        setGraphic(null);
                    } else {
                        setGraphic(button);
                    }
                }
            });
        }

        if (livresTable != null) {
            livresTable.setItems(livres);
            livresTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> onSelect(newVal));
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

        if (tableToggle != null && cardsToggle != null) {
            ToggleGroup viewGroup = new ToggleGroup();
            tableToggle.setToggleGroup(viewGroup);
            cardsToggle.setToggleGroup(viewGroup);
            tableToggle.setSelected(true);
            setCardsVisible(false);
            viewGroup.selectedToggleProperty().addListener((obs, old, val) -> setCardsVisible(val == cardsToggle));
        }

        loadCollections();
        refresh();
        clearForm();

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
        File file = fileChooser.showOpenDialog(saveButton.getScene().getWindow());
        if (file != null) {
            try {
                selectedImageBytes = Files.readAllBytes(file.toPath());
                couvertureLabel.setText(file.getName());
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
        File file = fileChooser.showOpenDialog(saveButton.getScene().getWindow());
        if (file != null) {
            try {
                selectedPdfBytes = Files.readAllBytes(file.toPath());
                pdfLabel.setText(file.getName());
            } catch (IOException e) {
                showError("Erreur", "Impossible de lire le PDF.");
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

        String description = descriptionArea.getText();
        if (description == null || description.trim().isEmpty()) {
            errorMessage.append("- Le champ 'Description' est obligatoire.\n");
            hasError = true;
            descriptionArea.setStyle("-fx-border-color: red;");
        } else {
            descriptionArea.setStyle("");
        }

        CollectionOeuvre selectedCol = collectionComboBox.getValue();
        if (selectedCol == null) {
            errorMessage.append("- Veuillez sélectionner une collection.\n");
            hasError = true;
            collectionComboBox.setStyle("-fx-border-color: red;");
        } else {
            collectionComboBox.setStyle("");
        }

        boolean hasExistingImage = selected != null && selected.getImage() != null && selected.getImage().length > 0;
        boolean hasExistingPdf = selected != null && selected.getFichierPdf() != null && selected.getFichierPdf().length > 0;

        if (selectedImageBytes == null && !hasExistingImage) {
            errorMessage.append("- Veuillez choisir une image de couverture.\n");
            hasError = true;
            couvertureLabel.setStyle("-fx-text-fill: red;");
        } else {
            couvertureLabel.setStyle("");
        }

        if (selectedPdfBytes == null && !hasExistingPdf) {
            errorMessage.append("- Veuillez choisir un fichier PDF.\n");
            hasError = true;
            pdfLabel.setStyle("-fx-text-fill: red;");
        } else {
            pdfLabel.setStyle("");
        }

        if (hasError) {
            showError("Erreur de validation", errorMessage.toString());
            return;
        }

        Livre livre = selected != null ? selected : new Livre();
        livre.setTitre(titre.trim());
        livre.setCategorie(categorie.trim());
        livre.setPrixLocation(prixLocation);
        livre.setDescription(description.trim());
        
        livre.setCollectionId(selectedCol.getId());

        if (selectedImageBytes != null) {
            livre.setImage(selectedImageBytes);
        }
        if (selectedPdfBytes != null) {
            livre.setFichierPdf(selectedPdfBytes);
        }

        try {
            if (selected == null) {
                livreService.add(livre);
                showInfo("Livre ajouté", "Le livre a été ajouté avec succès.");
            } else {
                livreService.update(livre);
                showInfo("Livre modifié", "Le livre a été modifié avec succès.");
            }
            refresh();
            clearForm();
        } catch (SQLDataException e) {
            showError("Enregistrement impossible", e.getMessage());
        }
    }

    @FXML
    private void onViewDetails() {
        Livre livre = selected;
        if (livre == null) {
            showError("Détails", "Veuillez sélectionner un livre.");
            return;
        }
        showDetailsDialog(livre);
    }

    private void showDetailsDialog(Livre livre) {
        Dialog<javafx.scene.control.ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Détails du livre");

        javafx.scene.control.ButtonType openPdf = new javafx.scene.control.ButtonType("Ouvrir PDF", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(openPdf, javafx.scene.control.ButtonType.CLOSE);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(180);
        imageView.setFitHeight(240);
        imageView.setPreserveRatio(true);
        if (livre.getImage() != null && livre.getImage().length > 0) {
            imageView.setImage(new Image(new ByteArrayInputStream(livre.getImage())));
        }

        Label title = new Label(livre.getTitre() == null ? "" : livre.getTitre());
        title.setStyle("-fx-font-size: 16; -fx-font-weight: 700;");
        Label auteur = new Label("Auteur: " + (livre.getAuteur() == null ? "" : livre.getAuteur()));
        Label cat = new Label("Catégorie: " + (livre.getCategorie() == null ? "" : livre.getCategorie()));
        Label prix = new Label("Prix location: " + (livre.getPrixLocation() == null ? "0" : livre.getPrixLocation()));
        Label dispo = new Label("Disponibilité: " + (Boolean.TRUE.equals(livre.getDisponibilite()) ? "Disponible" : "Indisponible"));

        TextArea desc = new TextArea(livre.getDescription() == null ? "" : livre.getDescription());
        desc.setEditable(false);
        desc.setWrapText(true);
        desc.setPrefRowCount(6);

        VBox content = new VBox(10, imageView, title, auteur, cat, prix, dispo, desc);
        content.setPrefWidth(420);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(bt -> bt);
        dialog.showAndWait().ifPresent(result -> {
            if (result == openPdf) {
                openPdf(livre);
            }
        });
    }

    private void openPdf(Livre livre) {
        if (livre.getFichierPdf() == null || livre.getFichierPdf().length == 0) {
            showError("PDF", "Aucun PDF pour ce livre.");
            return;
        }
        try {
            Path temp = Files.createTempFile("livre_" + (livre.getId() == null ? "tmp" : livre.getId()) + "_", ".pdf");
            Files.write(temp, livre.getFichierPdf());
            Desktop.getDesktop().open(temp.toFile());
        } catch (Exception e) {
            showError("PDF", "Impossible d'ouvrir le PDF.");
        }
    }

    @FXML
    private void onDelete() {
        Livre current = selected;
        if (current == null || current.getId() == null) {
            showError("Suppression", "Veuillez sélectionner un livre.");
            return;
        }
        try {
            livreService.delete(current.getId());
            showInfo("Livre supprimé", "Le livre a été supprimé.");
            refresh();
            clearForm();
        } catch (SQLDataException e) {
            showError("Suppression impossible", e.getMessage());
        }
    }

    @FXML
    private void onClear() {
        clearForm();
    }

    private void onSelect(Livre livre) {
        this.selected = livre;
        selectedImageBytes = null;
        selectedPdfBytes = null;
        if (livre == null) {
            clearForm();
            return;
        }
        titreField.setText(livre.getTitre());
        categorieField.setText(livre.getCategorie());
        prixField.setText(livre.getPrixLocation() == null ? "" : String.valueOf(livre.getPrixLocation()));
        descriptionArea.setText(livre.getDescription() == null ? "" : livre.getDescription());
        
        CollectionOeuvre col = collections.stream()
                .filter(c -> c.getId().equals(livre.getCollectionId()))
                .findFirst()
                .orElse(null);
        collectionComboBox.setValue(col);

        couvertureLabel.setText(livre.getImage() != null && livre.getImage().length > 0 ? "(Image existante)" : "Aucune image");
        if (couvertureImageView != null) {
            if (livre.getImage() != null && livre.getImage().length > 0) {
                couvertureImageView.setImage(new Image(new ByteArrayInputStream(livre.getImage())));
            } else {
                couvertureImageView.setImage(null);
            }
        }
        pdfLabel.setText(livre.getFichierPdf() != null && livre.getFichierPdf().length > 0 ? "(PDF existant)" : "Aucun PDF");

        boolean isRented = !Boolean.TRUE.equals(livre.getDisponibilite());
        prixField.setDisable(isRented);
        if (isRented) {
            prixField.setStyle("-fx-background-color: #f3f4f6; -fx-border-color: #e5e7eb;");
        } else {
            prixField.setStyle("");
        }

        saveButton.setText("Modifier");
        deleteButton.setDisable(false);
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

    private void clearForm() {
        selected = null;
        selectedImageBytes = null;
        selectedPdfBytes = null;
        if (livresTable != null) {
            livresTable.getSelectionModel().clearSelection();
        }
        if (titreField != null) titreField.clear();
        if (categorieField != null) categorieField.clear();
        if (prixField != null) {
            prixField.clear();
            prixField.setDisable(false);
            prixField.setStyle("");
        }
        if (collectionComboBox != null) collectionComboBox.setValue(null);
        if (couvertureLabel != null) couvertureLabel.setText("Aucune image");
        if (couvertureImageView != null) couvertureImageView.setImage(null);
        if (pdfLabel != null) pdfLabel.setText("Aucun PDF");
        if (descriptionArea != null) descriptionArea.clear();
        if (saveButton != null) saveButton.setText("✓ Enregistrer");
        if (deleteButton != null) deleteButton.setDisable(true);
    }

    @FXML
    private void onShowForm() {
        clearForm();
        formTitleLabel.setText("Nouveau livre");
        saveButton.setText("✓ Ajouter");
        
        formPanel.setVisible(true);
        formPanel.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(300), formPanel);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    @FXML
    private void onHideForm() {
        FadeTransition ft = new FadeTransition(Duration.millis(300), formPanel);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            formPanel.setVisible(false);
            clearForm();
        });
        ft.play();
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
        if (cardsTile == null) {
            return;
        }
        cardsTile.getChildren().setAll(items.stream().map(this::createBookCard).toList());
    }

    private Node createBookCard(Livre livre) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(120);
        imageView.setFitHeight(160);
        imageView.setPreserveRatio(true);
        if (livre.getImage() != null && livre.getImage().length > 0) {
            imageView.setImage(new Image(new ByteArrayInputStream(livre.getImage())));
        }

        Label title = new Label(livre.getTitre() == null ? "" : livre.getTitre());
        title.setStyle("-fx-font-weight: 700;");
        Label meta = new Label((livre.getCategorie() == null ? "" : livre.getCategorie()) + "  •  " + (livre.getPrixLocation() == null ? "0" : livre.getPrixLocation()));
        Label dispo = new Label(Boolean.TRUE.equals(livre.getDisponibilite()) ? "Disponible" : "Indisponible");

        Button details = new Button("Voir détails");
        details.setOnAction(e -> showDetailsDialog(livre));
        HBox actions = new HBox(10, details);
        HBox.setHgrow(details, Priority.NEVER);

        VBox card = new VBox(8, imageView, title, meta, dispo, actions);
        card.setStyle("-fx-padding: 10; -fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8;");
        card.setOnMouseClicked(e -> livresTable.getSelectionModel().select(livre));
        VBox.setVgrow(imageView, Priority.NEVER);
        return card;
    }

    private void setCardsVisible(boolean visible) {
        if (cardsScroll != null) {
            cardsScroll.setVisible(visible);
            cardsScroll.setManaged(visible);
        }
        if (livresTable != null) {
            livresTable.setVisible(!visible);
            livresTable.setManaged(!visible);
        }
    }

    private static boolean contains(String value, String queryLower) {
        return value != null && value.toLowerCase().contains(queryLower);
    }

    private static Double parsePrix(String value) {
        if (value == null || value.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
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
