package controllers.artist;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import services.OeuvreService;
import utils.UserSession;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatistiquesArtisteController {

    @FXML
    private Label chartTitleLabel;

    @FXML
    private Label attributeLabel;

    @FXML
    private Button prevButton;

    @FXML
    private Button nextButton;

    @FXML
    private StackPane pieChartContainer;

    @FXML
    private Button prevMonthButton;

    @FXML
    private Button nextMonthButton;

    @FXML
    private Label monthLabel;

    @FXML
    private StackPane commentsLineChartContainer;

    private final OeuvreService oeuvreService = new OeuvreService();

    private Integer currentArtistId;
    private List<Map<String, Object>> collectionsStats = new ArrayList<>();
    private boolean isAnimating = false;
    private boolean isLoadingMonthChart = false;

    private int currentAttributeMode = 0; // 0=Commentaires, 1=Likes, 2=Favoris
    private YearMonth currentCommentsMonth = YearMonth.now();

    private static final String[] ATTRIBUTES = {"Commentaires", "Likes", "Favoris"};
    private static final String[] CHART_COLORS = {
            "#6366F1", "#EC4899", "#F59E0B", "#10B981",
            "#3B82F6", "#8B5CF6", "#06B6D4", "#EF4444",
            "#14B8A6", "#F97316"
    };
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    @FXML
    public void initialize() {
        try {
            currentArtistId = UserSession.getCurrentUserId();
            if (currentArtistId != null) {
                prevButton.setOnAction(e -> switchAttribute(-1));
                nextButton.setOnAction(e -> switchAttribute(1));

                prevMonthButton.setOnAction(e -> switchCommentsMonth(-1));
                nextMonthButton.setOnAction(e -> switchCommentsMonth(1));

                loadStatistics();
            }
        } catch (Exception e) {
            System.err.println("Erreur init statistiques: " + e.getMessage());
        }
    }

    private void loadStatistics() {
        new Thread(() -> {
            try {
                collectionsStats = oeuvreService.getCollectionsStatsForArtiste(currentArtistId);
                Platform.runLater(() -> {
                    currentAttributeMode = 0;
                    renderAttributeState(false, 0);
                    updateMonthLabel();
                    loadCommentsChartForMonth();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    pieChartContainer.getChildren().setAll(buildEmptyChartNode("Erreur chargement statistiques."));
                    commentsLineChartContainer.getChildren().setAll(buildEmptyChartNode("Erreur chargement commentaires."));
                });
            }
        }).start();
    }

    private void switchAttribute(int direction) {
        if (isAnimating) {
            return;
        }
        int nextMode = (currentAttributeMode + direction + ATTRIBUTES.length) % ATTRIBUTES.length;
        renderAttributeState(true, direction > 0 ? 1 : -1, nextMode);
    }

    private void renderAttributeState(boolean animated, int direction) {
        renderAttributeState(animated, direction, currentAttributeMode);
    }

    private void renderAttributeState(boolean animated, int direction, int targetMode) {
        String attributeName = ATTRIBUTES[targetMode];
        chartTitleLabel.setText("🎨 Repartition des " + attributeName.toLowerCase(Locale.ROOT) + " par collection");

        if (!animated) {
            currentAttributeMode = targetMode;
            attributeLabel.setText(attributeName);
            pieChartContainer.getChildren().setAll(buildAttributeChartNode(targetMode));
            return;
        }

        isAnimating = true;
        prevButton.setDisable(true);
        nextButton.setDisable(true);

        animateAttributeLabel(attributeName, direction);
        animateChartSwitch(buildAttributeChartNode(targetMode), direction, () -> {
            currentAttributeMode = targetMode;
            isAnimating = false;
            prevButton.setDisable(false);
            nextButton.setDisable(false);
        });
    }

    private void switchCommentsMonth(int direction) {
        if (isLoadingMonthChart) {
            return;
        }
        currentCommentsMonth = currentCommentsMonth.plusMonths(direction);
        updateMonthLabel();
        loadCommentsChartForMonth();
    }

    private void updateMonthLabel() {
        monthLabel.setText(currentCommentsMonth.format(MONTH_FORMATTER));
    }

    private void loadCommentsChartForMonth() {
        isLoadingMonthChart = true;
        prevMonthButton.setDisable(true);
        nextMonthButton.setDisable(true);

        commentsLineChartContainer.getChildren().setAll(buildEmptyChartNode("Chargement..."));

        final YearMonth targetMonth = currentCommentsMonth;
        new Thread(() -> {
            try {
                Map<Integer, Integer> dailyComments = oeuvreService.getDailyCommentsForArtisteByMonth(currentArtistId, targetMonth);
                Platform.runLater(() -> {
                    commentsLineChartContainer.getChildren().setAll(buildCommentsLineChart(targetMonth, dailyComments));
                    isLoadingMonthChart = false;
                    prevMonthButton.setDisable(false);
                    nextMonthButton.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    commentsLineChartContainer.getChildren().setAll(buildEmptyChartNode("Aucune donnee pour ce mois."));
                    isLoadingMonthChart = false;
                    prevMonthButton.setDisable(false);
                    nextMonthButton.setDisable(false);
                });
            }
        }).start();
    }

    private Node buildCommentsLineChart(YearMonth month, Map<Integer, Integer> dailyComments) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Jour du mois");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Commentaires");
        yAxis.setForceZeroInRange(true);

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.getStyleClass().add("stats-comments-line-chart");
        lineChart.setTitle("Evolution - " + month.format(MONTH_FORMATTER));
        lineChart.setLegendVisible(false);
        lineChart.setAnimated(false);
        lineChart.setCreateSymbols(true);
        lineChart.setHorizontalGridLinesVisible(true);
        lineChart.setVerticalGridLinesVisible(false);
        lineChart.setAlternativeColumnFillVisible(false);
        lineChart.setAlternativeRowFillVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            int count = dailyComments.getOrDefault(day, 0);
            series.getData().add(new XYChart.Data<>(String.valueOf(day), count));
        }

        lineChart.getData().setAll(series);
        return lineChart;
    }

    private void animateAttributeLabel(String nextText, int direction) {
        TranslateTransition out = new TranslateTransition(Duration.millis(140), attributeLabel);
        out.setToX(direction > 0 ? -14 : 14);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(140), attributeLabel);
        fadeOut.setToValue(0.15);

        ParallelTransition exit = new ParallelTransition(out, fadeOut);
        exit.setOnFinished(e -> {
            attributeLabel.setText(nextText);
            attributeLabel.setTranslateX(direction > 0 ? 14 : -14);

            TranslateTransition in = new TranslateTransition(Duration.millis(180), attributeLabel);
            in.setToX(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(180), attributeLabel);
            fadeIn.setToValue(1);
            new ParallelTransition(in, fadeIn).play();
        });
        exit.play();
    }

    private void animateChartSwitch(Node newChart, int direction, Runnable onFinished) {
        Node oldChart = pieChartContainer.getChildren().isEmpty() ? null : pieChartContainer.getChildren().get(0);

        if (oldChart == null) {
            pieChartContainer.getChildren().setAll(newChart);
            onFinished.run();
            return;
        }

        newChart.setOpacity(0);
        newChart.setTranslateX(direction > 0 ? 44 : -44);
        pieChartContainer.getChildren().add(newChart);

        TranslateTransition outMove = new TranslateTransition(Duration.millis(220), oldChart);
        outMove.setToX(direction > 0 ? -44 : 44);
        FadeTransition outFade = new FadeTransition(Duration.millis(220), oldChart);
        outFade.setToValue(0);

        TranslateTransition inMove = new TranslateTransition(Duration.millis(220), newChart);
        inMove.setToX(0);
        FadeTransition inFade = new FadeTransition(Duration.millis(220), newChart);
        inFade.setToValue(1);

        ParallelTransition transition = new ParallelTransition(outMove, outFade, inMove, inFade);
        transition.setOnFinished(e -> {
            pieChartContainer.getChildren().setAll(newChart);
            onFinished.run();
        });
        transition.play();
    }

    private Node buildAttributeChartNode(int attributeMode) {
        if (collectionsStats.isEmpty()) {
            return buildEmptyChartNode("Aucune collection");
        }

        String metricKey = switch (attributeMode) {
            case 0 -> "commentairesCount";
            case 1 -> "likesCount";
            default -> "favorisCount";
        };

        int totalMetric = collectionsStats.stream()
                .mapToInt(stat -> (Integer) stat.get(metricKey))
                .sum();

        if (totalMetric <= 0) {
            return buildEmptyChartNode("Aucune donnee pour " + ATTRIBUTES[attributeMode].toLowerCase(Locale.ROOT));
        }

        PieChart pieChart = new PieChart();
        pieChart.getStyleClass().add("stats-pie-chart");
        pieChart.setLegendVisible(false);
        pieChart.setLabelsVisible(false);
        pieChart.setPrefSize(420, 280);
        pieChart.setMinSize(420, 280);

        List<PieChart.Data> dataItems = collectionsStats.stream()
                .map(stat -> new PieChart.Data(
                        stat.get("collectionTitre") + " (" + stat.get(metricKey) + ")",
                        (Integer) stat.get(metricKey)
                ))
                .toList();

        pieChart.setData(FXCollections.observableArrayList(dataItems));

        VBox legendBox = buildCustomLegend(dataItems);
        HBox chartRow = new HBox(10, pieChart, legendBox);
        chartRow.setAlignment(Pos.CENTER_LEFT);
        chartRow.setPadding(new Insets(4, 0, 0, 0));
        HBox.setHgrow(pieChart, Priority.ALWAYS);

        Platform.runLater(() -> applySliceColors(dataItems));

        return chartRow;
    }

    private VBox buildCustomLegend(List<PieChart.Data> dataItems) {
        VBox legendBox = new VBox(7);
        legendBox.getStyleClass().add("stats-pie-legend");
        legendBox.setMinWidth(200);
        legendBox.setPrefWidth(200);

        for (int i = 0; i < dataItems.size(); i++) {
            PieChart.Data data = dataItems.get(i);
            String color = CHART_COLORS[i % CHART_COLORS.length];

            Circle dot = new Circle(5);
            dot.setStyle("-fx-fill: " + color + ";");

            Label label = new Label(data.getName());
            label.getStyleClass().add("stats-pie-legend-label");

            HBox item = new HBox(8, dot, label);
            item.getStyleClass().add("stats-pie-legend-item");
            item.setAlignment(Pos.CENTER_LEFT);
            legendBox.getChildren().add(item);
        }

        return legendBox;
    }

    private void applySliceColors(List<PieChart.Data> dataItems) {
        for (int i = 0; i < dataItems.size(); i++) {
            PieChart.Data data = dataItems.get(i);
            String color = CHART_COLORS[i % CHART_COLORS.length];

            if (data.getNode() != null) {
                data.getNode().setStyle("-fx-pie-color: " + color + ";");
            }

            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-pie-color: " + color + ";");
                }
            });
        }
    }

    private Node buildEmptyChartNode(String message) {
        Label emptyLabel = new Label(message);
        emptyLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 14;");
        StackPane pane = new StackPane(emptyLabel);
        pane.setPrefHeight(280);
        return pane;
    }
}
