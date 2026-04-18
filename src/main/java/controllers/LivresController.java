package controllers;

import entities.CollectionOeuvre;
import entities.Livre;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import services.CollectionService;
import services.JdbcCollectionService;
import services.JdbcLivreService;
import services.LivreService;

import java.io.File;
import java.io.IOException;
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
    private ScrollPane cardsScroll;

    @FXML
    private TilePane cardsTile;

    @FXML
    private Button addBookButton;

    @FXML
    public void initialize() {
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

    private void showFormDialog(Livre livre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/pages/LivreForm.fxml"));
            Node formContent = loader.load();
            LivreFormController formController = loader.getController();

            formController.setCollections(new ArrayList<>(collections));
            formController.setLivre(livre);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle(livre == null ? "Ajouter un livre" : "Modifier le livre");
            dialog.getDialogPane().setContent(formContent);
            dialog.getDialogPane().getButtonTypes().clear();
            dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CANCEL);
            Button hiddenCancelButton = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL);
            if (hiddenCancelButton != null) {
                hiddenCancelButton.setManaged(false);
                hiddenCancelButton.setVisible(false);
            }
            dialog.getDialogPane().getStyleClass().add("custom-dialog");
            dialog.getDialogPane().getStyleClass().add("book-form-dialog");
            if (cardsTile != null && cardsTile.getScene() != null) {
                dialog.getDialogPane().getStylesheets().setAll(cardsTile.getScene().getStylesheets());
            }
            dialog.getDialogPane().setPrefSize(760, 680);
            dialog.showAndWait();

            Livre livreResultat = formController.getResultLivre();
            if (livreResultat != null) {
                persistLivre(livreResultat, livre == null);
            }
        } catch (IOException e) {
            showError("Erreur", "Impossible d'ouvrir le formulaire livre: " + e.getMessage());
        }
    }

    private void persistLivre(Livre livre, boolean isCreate) {
        try {
            if (isCreate) {
                livreService.add(livre);
                showInfo("Livre ajouté", "Le livre a été ajouté avec succès.");
            } else {
                livreService.update(livre);
                showInfo("Livre modifié", "Le livre a été modifié avec succès.");
            }
            refresh();
        } catch (SQLDataException e) {
            showError("Erreur", "Impossible d'enregistrer : " + e.getMessage());
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
        card.setPrefWidth(250);
        card.setPadding(new Insets(15));
        card.setAlignment(javafx.geometry.Pos.TOP_CENTER);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(160);
        imageView.setFitHeight(220);
        imageView.setPreserveRatio(true);
        if (livre.getImage() != null && !livre.getImage().isBlank()) {
            imageView.setImage(toImage(livre.getImage()));
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
        editBtn.setMinWidth(105);
        editBtn.setPrefWidth(110);
        editBtn.setOnAction(e -> showFormDialog(livre));

        Button deleteBtn = new Button("Supprimer");
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.setMinWidth(105);
        deleteBtn.setPrefWidth(110);
        deleteBtn.setOnAction(e -> onDeleteLivre(livre));

        HBox actions = new HBox(8, editBtn, deleteBtn);
        actions.setAlignment(javafx.geometry.Pos.CENTER);

        card.getChildren().addAll(imageContainer, title, author, price, actions);

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

    private Image toImage(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        if (source.startsWith("http://") || source.startsWith("https://") || source.startsWith("file:")) {
            return new Image(source, true);
        }
        return new Image(new File(source).toURI().toString(), true);
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

