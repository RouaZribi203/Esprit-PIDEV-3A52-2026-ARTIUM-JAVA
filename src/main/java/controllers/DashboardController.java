package controllers;

import Services.DashboardService;
import components.CalendarPane;
import components.StatCard;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

public class DashboardController {

    @FXML private HBox statsContainer;
    @FXML private StackPane lineChartContainer;
    @FXML private StackPane rolePieContainer;

    @FXML private VBox signupsList;
    @FXML private VBox reclamationsList;
    @FXML private VBox topArtistesList;

    @FXML private Label reclamationsBadge;
    @FXML private StackPane calendarContainer;

    private final DashboardService dashboardService = new DashboardService();

    @FXML
    public void initialize() {
        calendarContainer.getChildren().add(new CalendarPane());
        showLoadingState();
        loadDashboardDataAsync();
    }

    private void loadDashboardDataAsync() {
        Task<DashboardService.DashboardData> loadTask = new Task<>() {
            @Override
            protected DashboardService.DashboardData call() throws Exception {
                return dashboardService.loadDashboardData();
            }
        };
        loadTask.setOnSucceeded(event -> renderDashboard(loadTask.getValue()));
        loadTask.setOnFailed(event -> showErrorState(loadTask.getException()));
        Thread worker = new Thread(loadTask, "dashboard-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private void renderDashboard(DashboardService.DashboardData data) {
        buildStatCards(data);
        buildLineChart(data);
        buildRolePie(data);
        fillRecentLists(data);
    }

    private void showLoadingState() {
        buildStatCards(new DashboardService.DashboardData(
                0, 0, 0, 0, 0, 0,
                java.util.List.of(),
                java.util.Map.of("Admin", 0, "Artistes", 0, "Amateurs", 0),
                java.util.List.of("Chargement..."),
                java.util.List.of("Chargement..."),
                java.util.List.of("Chargement...")
        ));
    }

    private void showErrorState(Throwable error) {
        String message = error == null || error.getMessage() == null
                ? "Erreur de chargement."
                : error.getMessage();
        buildStatCards(new DashboardService.DashboardData(
                0, 0, 0, 0, 0, 0,
                java.util.List.of(),
                java.util.Map.of("Admin", 0, "Artistes", 0, "Amateurs", 0),
                java.util.List.of("Erreur: " + message),
                java.util.List.of("Erreur: " + message),
                java.util.List.of("Erreur: " + message)
        ));
        lineChartContainer.getChildren().clear();
        rolePieContainer.getChildren().clear();
    }

    private void buildStatCards(DashboardService.DashboardData data) {
        statsContainer.getChildren().setAll(
                new StatCard("U", "icon-users",     data.getTotalUsers(),        "Utilisateurs"),
                new StatCard("A", "icon-artistes",  data.getTotalArtistes(),     "Artistes"),
                new StatCard("M", "icon-amateurs",  data.getTotalAmateurs(),     "Amateurs"),
                new StatCard("O", "icon-oeuvres",   data.getTotalOeuvres(),      "Oeuvres"),
                new StatCard("R", "icon-reclam",    data.getTotalReclamations(), "Reclamations"),
                new StatCard("E", "icon-events",    data.getTotalEvenements(),   "Evenements")
        );
    }

    private void buildLineChart(DashboardService.DashboardData data) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setLabel("Mois");
        yAxis.setLabel("Inscriptions");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(true);
        chart.setMinHeight(250.0);
        chart.setPrefHeight(250.0);
        chart.setMaxHeight(Double.MAX_VALUE);
        chart.getStyleClass().add("dashboard-line-chart");
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (data.getSignupsByMonth().isEmpty()) {
            series.getData().add(new XYChart.Data<>("Aucune donnée", 0));
        } else {
            for (DashboardService.MonthlySignupPoint point : data.getSignupsByMonth()) {
                series.getData().add(new XYChart.Data<>(point.getMonthLabel(), point.getTotal()));
            }
        }
        chart.getData().add(series);
        lineChartContainer.getChildren().setAll(chart);
    }

    private void buildRolePie(DashboardService.DashboardData data) {
        Map<String, Integer> dist = data.getRoleDistribution();
        PieChart pieChart = new PieChart();
        pieChart.getData().addAll(
                new PieChart.Data("Admin",    dist.getOrDefault("Admin",    0)),
                new PieChart.Data("Artistes", dist.getOrDefault("Artistes", 0)),
                new PieChart.Data("Amateurs", dist.getOrDefault("Amateurs", 0))
        );
        pieChart.getStyleClass().add("dashboard-pie");
        rolePieContainer.getChildren().setAll(pieChart);
    }

    private void fillRecentLists(DashboardService.DashboardData data) {
        fillSignups(data.getRecentSignups());
        fillReclamations(data.getRecentReclamations());
        fillTopArtistes(data.getTopArtistes());
    }

    private void fillSignups(List<String> items) {
        signupsList.getChildren().clear();
        if (items == null || items.isEmpty()) {
            signupsList.getChildren().add(makeEmptyLabel("Aucune inscription récente."));
            return;
        }
        for (String item : items) {
            String[] parts   = splitItem(item);
            String   initiale = parts[0].isBlank() ? "?" : parts[0].substring(0, 1).toUpperCase();
            Label    avatar  = makeAvatar(initiale, "dash-item-avatar");
            signupsList.getChildren().add(makeItemRow(avatar, parts[0], parts[1], null));
        }
    }

    private void fillReclamations(List<String> items) {
        reclamationsList.getChildren().clear();
        int count = (items == null) ? 0 : items.size();
        if (reclamationsBadge != null) reclamationsBadge.setText(String.valueOf(count));
        if (items == null || items.isEmpty()) {
            reclamationsList.getChildren().add(makeEmptyLabel("Aucune réclamation récente."));
            return;
        }
        for (String item : items) {
            String[] parts   = splitItem(item);
            String   initiale = parts[0].isBlank() ? "!" : parts[0].substring(0, 1).toUpperCase();
            Label    avatar  = makeAvatar(initiale, "dash-item-avatar", "dash-item-avatar-red");
            reclamationsList.getChildren().add(makeItemRow(avatar, parts[0], parts[1], null));
        }
    }

    private void fillTopArtistes(List<String> items) {
        topArtistesList.getChildren().clear();
        if (items == null || items.isEmpty()) {
            topArtistesList.getChildren().add(makeEmptyLabel("Aucun artiste disponible."));
            return;
        }
        int rank = 1;
        for (String item : items) {
            String[] parts   = splitItem(item);
            String   initiale = parts[0].isBlank() ? "A" : parts[0].substring(0, 1).toUpperCase();
            Label    avatar  = makeAvatar(initiale, "dash-item-avatar", "dash-item-avatar-purple");
            Label    badge   = null;
            if (!parts[1].isBlank()) {
                badge = new Label(parts[1]);
                badge.getStyleClass().add("dash-item-badge");
            }
            topArtistesList.getChildren().add(makeItemRow(avatar, parts[0], null, badge));
            rank++;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Node makeItemRow(Label avatar, String title, String subtitle, Node rightNode) {
        HBox row = new HBox(10);
        row.getStyleClass().add("dash-item-row");
        row.setAlignment(Pos.CENTER_LEFT);

        VBox textBox = new VBox(2);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("premium-list-title");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setWrapText(true);
        textBox.getChildren().add(titleLabel);

        if (subtitle != null && !subtitle.isBlank()) {
            Label sub = new Label(subtitle);
            sub.getStyleClass().add("premium-list-subtitle");
            sub.setMaxWidth(Double.MAX_VALUE);
            sub.setWrapText(true);
            textBox.getChildren().add(sub);
        }

        row.getChildren().addAll(avatar, textBox);
        if (rightNode != null) row.getChildren().add(rightNode);
        return row;
    }

    private Label makeAvatar(String text, String... styleClasses) {
        Label avatar = new Label(text);
        avatar.getStyleClass().addAll(styleClasses);
        return avatar;
    }

    private Label makeEmptyLabel(String message) {
        Label lbl = new Label(message);
        lbl.getStyleClass().add("dash-empty-label");
        return lbl;
    }

    private String[] splitItem(String value) {
        if (value == null) return new String[]{"", ""};
        int sep = value.lastIndexOf(" - ");
        if (sep <= 0 || sep >= value.length() - 3) return new String[]{value.trim(), ""};
        return new String[]{value.substring(0, sep).trim(), value.substring(sep + 3).trim()};
    }
}