package controllers;

import entities.CollectionOeuvre;
import entities.Livre;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
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
import java.util.Map;
import java.util.stream.Collectors;

public class LivresController {
    private final LivreService livreService = new JdbcLivreService();
    private final CollectionService collectionService = new JdbcCollectionService();
    private final ObservableList<Livre> livres = FXCollections.observableArrayList();
    private final ObservableList<CollectionOeuvre> collections = FXCollections.observableArrayList();
    private final List<Livre> allLivres = new ArrayList<>();
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(250));
    
    private static final int BOOKS_PER_PAGE = 6;
    private int currentPage = 0;

    @FXML
    private TextField searchField;

    @FXML
    private ScrollPane cardsScroll;

    @FXML
    private TilePane cardsTile;

    @FXML
    private Button addBookButton;
    
    @FXML
    private VBox statisticsContainer;
    
    @FXML
    private Button prevPageBtn;
    
    @FXML
    private Button nextPageBtn;
    
    @FXML
    private Label pageInfoLabel;

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
        
        // Initialize statistics if container exists
        if (statisticsContainer != null) {
            updateStatistics();
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
        currentPage = 0;
        applyFilter(searchField.getText());
    }

    @FXML
    private void onRefresh() {
        currentPage = 0;
        refresh();
    }
    
    @FXML
    private void onPrevPage() {
        if (currentPage > 0) {
            currentPage--;
            applyFilter(searchField.getText());
        }
    }
    
    @FXML
    private void onNextPage() {
        currentPage++;
        applyFilter(searchField.getText());
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
            if (statisticsContainer != null) {
                updateStatistics();
            }
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
        
        // Calculate pagination
        int totalPages = (int) Math.ceil((double) items.size() / BOOKS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        
        // Ensure current page is valid
        if (currentPage >= totalPages) {
            currentPage = totalPages - 1;
        }
        
        // Get items for current page
        int startIndex = currentPage * BOOKS_PER_PAGE;
        int endIndex = Math.min(startIndex + BOOKS_PER_PAGE, items.size());
        List<Livre> pageItems = items.subList(startIndex, endIndex);
        
        // Update cards display
        cardsTile.getChildren().setAll(pageItems.stream().map(this::createBookCard).toList());
        
        // Update pagination buttons
        if (prevPageBtn != null) {
            prevPageBtn.setDisable(currentPage <= 0);
        }
        if (nextPageBtn != null) {
            nextPageBtn.setDisable(currentPage >= totalPages - 1);
        }
        if (pageInfoLabel != null) {
            pageInfoLabel.setText(String.format("Page %d / %d", currentPage + 1, totalPages));
        }
    }

    private Node createBookCard(Livre livre) {
        VBox card = new VBox(12);
        card.getStyleClass().add("book-card");
        card.setPrefWidth(300);
        card.setMaxWidth(300);
        card.setMinWidth(300);
        card.setPadding(new Insets(18));
        card.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        card.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 12; -fx-background-radius: 12; "
                + "-fx-border-width: 1.5; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

        ImageView imageView = new ImageView();
        imageView.setFitWidth(200);
        imageView.setFitHeight(280);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0, 0, 3);");
        if (livre.getImage() != null && !livre.getImage().isBlank()) {
            imageView.setImage(toImage(livre.getImage()));
        }

        StackPane imageContainer = new StackPane(imageView);
        imageContainer.getStyleClass().add("card-image-container");
        imageContainer.setStyle("-fx-alignment: center;");

        Label title = new Label(livre.getTitre());
        title.getStyleClass().add("card-title");
        title.setWrapText(true);
        title.setAlignment(javafx.geometry.Pos.CENTER);
        title.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #1f2937; -fx-wrap-text: true;");
        title.setMaxWidth(260);

        Label author = new Label("✍️ " + (livre.getAuteur() != null ? livre.getAuteur() : "N/A"));
        author.getStyleClass().add("card-author");
        author.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280; -fx-font-style: italic;");

        Label price = new Label("💰 " + String.format("%.2f TND", livre.getPrixLocation() != null ? livre.getPrixLocation() : 0));
        price.getStyleClass().add("card-price");
        price.setStyle("-fx-font-size: 13; -fx-text-fill: #3b82f6; -fx-font-weight: bold;");

        Button editBtn = new Button("✏️ Modifier");
        editBtn.getStyleClass().add("secondary-button");
        editBtn.setMinWidth(120);
        editBtn.setPrefWidth(130);
        editBtn.setStyle("-fx-padding: 10 14; -fx-font-size: 12; -fx-background-color: #f3f4f6; -fx-text-fill: #374151; "
                + "-fx-border-color: #d1d5db; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
        editBtn.setOnMouseEntered(e -> editBtn.setStyle("-fx-padding: 10 14; -fx-font-size: 12; -fx-background-color: #e5e7eb; "
                + "-fx-text-fill: #1f2937; -fx-border-color: #9ca3af; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;"));
        editBtn.setOnMouseExited(e -> editBtn.setStyle("-fx-padding: 10 14; -fx-font-size: 12; -fx-background-color: #f3f4f6; "
                + "-fx-text-fill: #374151; -fx-border-color: #d1d5db; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;"));
        editBtn.setOnAction(e -> showFormDialog(livre));

        Button deleteBtn = new Button("🗑️ Supprimer");
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.setMinWidth(120);
        deleteBtn.setPrefWidth(130);
        deleteBtn.setStyle("-fx-padding: 10 14; -fx-font-size: 12; -fx-background-color: #fee2e2; -fx-text-fill: #dc2626; "
                + "-fx-border-color: #fca5a5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
        deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle("-fx-padding: 10 14; -fx-font-size: 12; -fx-background-color: #fecaca; "
                + "-fx-text-fill: #b91c1c; -fx-border-color: #f87171; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;"));
        deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle("-fx-padding: 10 14; -fx-font-size: 12; -fx-background-color: #fee2e2; "
                + "-fx-text-fill: #dc2626; -fx-border-color: #fca5a5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;"));
        deleteBtn.setOnAction(e -> onDeleteLivre(livre));

        HBox actions = new HBox(10, editBtn, deleteBtn);
        actions.setAlignment(javafx.geometry.Pos.CENTER);

        card.getChildren().addAll(imageContainer, title, author, price, actions);

        // Premium hover effect with scale animation
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: white; -fx-border-color: #3b82f6; -fx-border-radius: 12; -fx-background-radius: 12; "
                    + "-fx-border-width: 2; -fx-effect: dropshadow(gaussian, rgba(59,130,246,0.3), 15, 0, 0, 5);");
            
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 12; -fx-background-radius: 12; "
                    + "-fx-border-width: 1.5; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
            
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

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
    
    private void updateStatistics() {
        if (statisticsContainer == null) {
            return;
        }
        
        statisticsContainer.getChildren().clear();
        
        // Title
        Label statsTitle = new Label("📊 Statistiques du Catalogue");
        statsTitle.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #1f2937; -fx-padding: 15 0 10 0;");
        
        HBox statsContent = new HBox(30);
        statsContent.setStyle("-fx-padding: 20;");
        statsContent.setFillHeight(true);
        statsContent.setPrefWidth(Double.MAX_VALUE);
        
        // Left side: Category statistics (Pie chart)
        VBox categorySection = createCategoryStatistics();
        HBox.setHgrow(categorySection, Priority.ALWAYS);
        
        // Divider
        Line divider = new Line();
        divider.setStartY(0);
        divider.setEndY(400);
        divider.setStyle("-fx-stroke: #e5e7eb; -fx-stroke-width: 2;");
        
        // Right side: Author statistics (Pie chart)
        VBox authorSection = createAuthorStatistics();
        HBox.setHgrow(authorSection, Priority.ALWAYS);
        
        statsContent.getChildren().addAll(categorySection, divider, authorSection);
        
        statisticsContainer.getChildren().addAll(statsTitle, statsContent);
        statisticsContainer.setPrefWidth(Double.MAX_VALUE);
    }
    
    private VBox createCategoryStatistics() {
        VBox container = new VBox(15);
        container.setStyle("-fx-padding: 15; -fx-background-color: #f9fafb; -fx-border-radius: 12; -fx-background-radius: 12;");
        container.setPrefWidth(Double.MAX_VALUE);
        
        Label categoryTitle = new Label("📚 Livres par Catégorie");
        categoryTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");
        
        Map<String, Long> categoryCounts = allLivres.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getCategorie() != null ? l.getCategorie() : "Non spécifié",
                        Collectors.counting()
                ));
        
        ObservableList<PieChart.Data> categoryData = FXCollections.observableArrayList();
        String[] colors = {"#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#06b6d4", "#ec4899", "#14b8a6"};
        
        for (Map.Entry<String, Long> entry : categoryCounts.entrySet()) {
            PieChart.Data data = new PieChart.Data(entry.getKey(), entry.getValue());
            categoryData.add(data);
        }
        
        PieChart categoryChart = new PieChart(categoryData);
        categoryChart.setStyle("-fx-font-family: Arial;");
        categoryChart.setPrefWidth(Double.MAX_VALUE);
        categoryChart.setPrefHeight(350);
        categoryChart.setMaxWidth(Double.MAX_VALUE);
        categoryChart.setLegendSide(javafx.geometry.Side.BOTTOM);
        categoryChart.setLabelsVisible(true);
        categoryChart.setStartAngle(90);
        
        // Style pie chart slices with colors
        int colorIndex = 0;
        for (PieChart.Data data : categoryData) {
            final int currentColorIndex = colorIndex;
            data.getNode().setStyle("-fx-pie-color: " + colors[currentColorIndex % colors.length] + "; -fx-pie-label-visible: true;");
            
            data.getNode().setOnMouseEntered(e -> {
                data.getNode().setStyle("-fx-pie-color: " + colors[currentColorIndex % colors.length] + "; -fx-cursor: hand; "
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);");
            });
            data.getNode().setOnMouseExited(e -> {
                data.getNode().setStyle("-fx-pie-color: " + colors[currentColorIndex % colors.length] + ";");
            });
            
            colorIndex++;
        }
        
        // Summary stats below chart
        VBox statsBox = new VBox(8);
        statsBox.setStyle("-fx-padding: 12; -fx-background-color: white; -fx-border-radius: 8; -fx-background-radius: 8; "
                + "-fx-border-color: #e0e7ff; -fx-border-width: 1;");
        
        Label totalBooksLabel = new Label("📖 Total: " + allLivres.size() + " livres");
        totalBooksLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #3b82f6;");
        
        Label topCategoryLabel = new Label("🏆 Top: " + (categoryCounts.isEmpty() ? "N/A" : 
                categoryCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(e -> e.getKey() + " (" + e.getValue() + ")")
                    .orElse("N/A")));
        topCategoryLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #10b981;");
        
        statsBox.getChildren().addAll(totalBooksLabel, topCategoryLabel);
        
        container.getChildren().addAll(categoryTitle, categoryChart, statsBox);
        return container;
    }
    
    private VBox createAuthorStatistics() {
        VBox container = new VBox(15);
        container.setStyle("-fx-padding: 15; -fx-background-color: #f9fafb; -fx-border-radius: 12; -fx-background-radius: 12;");
        container.setPrefWidth(Double.MAX_VALUE);
        
        Label authorTitle = new Label("✍️ Profit par Auteur");
        authorTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");
        
        Map<String, Double> authorProfit = allLivres.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getAuteur() != null ? l.getAuteur() : "Non spécifié",
                        Collectors.summingDouble(l -> l.getPrixLocation() != null ? l.getPrixLocation() : 0)
                ));
        
        ObservableList<PieChart.Data> authorData = FXCollections.observableArrayList();
        String[] colors = {"#f43f5e", "#d946ef", "#0ea5e9", "#f97316", "#6366f1", "#22c55e", "#eab308", "#14b8a6"};
        
        for (Map.Entry<String, Double> entry : authorProfit.entrySet()) {
            PieChart.Data data = new PieChart.Data(entry.getKey(), entry.getValue());
            authorData.add(data);
        }
        
        PieChart authorChart = new PieChart(authorData);
        authorChart.setStyle("-fx-font-family: Arial;");
        authorChart.setPrefWidth(Double.MAX_VALUE);
        authorChart.setPrefHeight(350);
        authorChart.setMaxWidth(Double.MAX_VALUE);
        authorChart.setLegendSide(javafx.geometry.Side.BOTTOM);
        authorChart.setLabelsVisible(true);
        authorChart.setStartAngle(90);
        
        // Style pie chart slices with colors
        int colorIndex = 0;
        for (PieChart.Data data : authorData) {
            final int currentColorIndex = colorIndex;
            data.getNode().setStyle("-fx-pie-color: " + colors[currentColorIndex % colors.length] + "; -fx-pie-label-visible: true;");
            
            data.getNode().setOnMouseEntered(e -> {
                data.getNode().setStyle("-fx-pie-color: " + colors[currentColorIndex % colors.length] + "; -fx-cursor: hand; "
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);");
            });
            data.getNode().setOnMouseExited(e -> {
                data.getNode().setStyle("-fx-pie-color: " + colors[currentColorIndex % colors.length] + ";");
            });
            
            colorIndex++;
        }
        
        // Summary stats below chart
        VBox statsBox = new VBox(8);
        statsBox.setStyle("-fx-padding: 12; -fx-background-color: white; -fx-border-radius: 8; -fx-background-radius: 8; "
                + "-fx-border-color: #e0e7ff; -fx-border-width: 1;");
        
        double totalProfit = authorProfit.values().stream().mapToDouble(Double::doubleValue).sum();
        Label totalProfitLabel = new Label("💰 Revenu total: " + String.format("%.2f TND", totalProfit));
        totalProfitLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #10b981;");
        
        String topAuthor = authorProfit.isEmpty() ? "N/A" : 
                authorProfit.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(e -> e.getKey() + " (" + String.format("%.2f", e.getValue()) + " TND)")
                    .orElse("N/A");
        
        Label topAuthorLabel = new Label("🌟 Top: " + topAuthor);
        topAuthorLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");
        
        statsBox.getChildren().addAll(totalProfitLabel, topAuthorLabel);
        
        container.getChildren().addAll(authorTitle, authorChart, statsBox);
        return container;
    }
}

