package controllers;

import components.CalendarPane;
import components.StatCard;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class DashboardController {

    @FXML
    private HBox statsContainer;

    @FXML
    private StackPane lineChartContainer;

    @FXML
    private StackPane rolePieContainer;

    @FXML
    private ListView<String> signupsList;

    @FXML
    private ListView<String> reclamationsList;

    @FXML
    private ListView<String> topArtistesList;

    @FXML
    private StackPane calendarContainer;

    @FXML
    public void initialize() {
        buildStatCards();
        buildLineChart();
        buildRolePie();
        fillRecentLists();
        calendarContainer.getChildren().add(new CalendarPane());
    }

    private void buildStatCards() {
        statsContainer.getChildren().setAll(
                new StatCard("U", "icon-users", 124, "Utilisateurs"),
                new StatCard("A", "icon-artistes", 48, "Artistes"),
                new StatCard("M", "icon-amateurs", 76, "Amateurs"),
                new StatCard("O", "icon-oeuvres", 392, "Oeuvres"),
                new StatCard("R", "icon-reclam", 13, "Reclamations"),
                new StatCard("E", "icon-events", 27, "Evenements")
        );
    }

    private void buildLineChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Mois");
        yAxis.setLabel("Inscriptions");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.getStyleClass().add("dashboard-line-chart");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Nov", 21));
        series.getData().add(new XYChart.Data<>("Dec", 34));
        series.getData().add(new XYChart.Data<>("Jan", 29));
        series.getData().add(new XYChart.Data<>("Fev", 46));
        series.getData().add(new XYChart.Data<>("Mar", 41));
        series.getData().add(new XYChart.Data<>("Avr", 53));

        chart.getData().add(series);
        lineChartContainer.getChildren().setAll(chart);
    }

    private void buildRolePie() {
        PieChart pieChart = new PieChart();
        pieChart.getData().addAll(
                new PieChart.Data("Admin", 5),
                new PieChart.Data("Artistes", 48),
                new PieChart.Data("Amateurs", 76)
        );
        pieChart.getStyleClass().add("dashboard-pie");
        rolePieContainer.getChildren().setAll(pieChart);
    }

    private void fillRecentLists() {
        signupsList.getItems().setAll(
                "Maya Trabelsi - 11/04/2026 09:24",
                "Yassine Gharbi - 11/04/2026 08:10",
                "Ines Mejri - 10/04/2026 23:55",
                "Amine Khemiri - 10/04/2026 19:07"
        );

        reclamationsList.getItems().setAll(
                "Paiement non valide... - 11/04/2026",
                "Probleme de publication oeuvre... - 11/04/2026",
                "Impossible de modifier mon profil... - 10/04/2026"
        );

        topArtistesList.getItems().setAll(
                "Sarra Ben Ali - 25 oeuvres",
                "Walid Messaoud - 19 oeuvres",
                "Nour Baccar - 15 oeuvres",
                "Rami Khadhraoui - 13 oeuvres",
                "Yasmine Jerbi - 11 oeuvres"
        );
    }
}


