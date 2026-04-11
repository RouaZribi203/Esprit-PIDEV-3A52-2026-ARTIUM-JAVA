package components;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;

public class CalendarPane extends VBox {

    public CalendarPane() {
        getStyleClass().add("list-card");
        setSpacing(10);
        setPadding(new Insets(12));

        LocalDate today = LocalDate.now();
        YearMonth month = YearMonth.of(today.getYear(), today.getMonth());

        Label title = new Label("Calendrier des evenements");
        title.getStyleClass().add("section-title");

        Label monthLabel = new Label(month.getMonth().name() + " " + month.getYear());
        monthLabel.getStyleClass().add("section-subtitle");

        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);

        String[] days = {"L", "M", "M", "J", "V", "S", "D"};
        for (int i = 0; i < days.length; i++) {
            Label day = new Label(days[i]);
            day.getStyleClass().add("calendar-day-header");
            grid.add(day, i, 0);
        }

        int firstDay = month.atDay(1).getDayOfWeek().getValue();
        int monthLength = month.lengthOfMonth();
        int row = 1;
        int col = firstDay - 1;

        for (int value = 1; value <= monthLength; value++) {
            Label dayLabel = new Label(String.valueOf(value));
            dayLabel.getStyleClass().add("calendar-day");
            if (value == today.getDayOfMonth()) {
                dayLabel.getStyleClass().add("today");
            }
            grid.add(dayLabel, col, row);
            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }

        HBox footer = new HBox();
        Label filter = new Label("Filtre: Tous / A venir / Aujourd'hui");
        filter.getStyleClass().add("section-subtitle");
        HBox.setHgrow(filter, Priority.ALWAYS);
        footer.getChildren().add(filter);

        getChildren().addAll(title, monthLabel, grid, footer);
    }
}

