# Artium Admin Dashboard UI (JavaFX)

This workspace now contains a full interface skeleton for an Artium-style admin dashboard.

## Implemented

- Main shell with `BorderPane`
  - Left: reusable sidebar (`Sidebar.fxml`)
  - Top: reusable navbar (`Navbar.fxml`)
  - Center: dynamic content area
- Sidebar with grouped/collapsible navigation entries
- Navbar with sidebar toggle, theme menu, notifications button, profile menu
- Dashboard interface with:
  - 6 stat cards
  - line chart area
  - pie chart area
  - recent signups list
  - recent reclamations list
  - top artistes list
  - calendar pane
- Shared CSS theme and dark-mode class support

## Main files

- `src/main/resources/views/MainLayout.fxml`
- `src/main/resources/views/Sidebar.fxml`
- `src/main/resources/views/Navbar.fxml`
- `src/main/resources/views/pages/dashboard.fxml`
- `src/main/resources/views/styles/dashboard.css`

Controllers:

- `src/main/java/controllers/MainController.java`
- `src/main/java/controllers/SidebarController.java`
- `src/main/java/controllers/NavbarController.java`
- `src/main/java/controllers/DashboardController.java`

Custom UI components:

- `src/main/java/controllers/components/StatCard.java`
- `src/main/java/controllers/components/CalendarPane.java`

## Run

Use your IDE run configuration for `Test.MainFX`, or Maven if installed:

```powershell
Set-Location "C:\Users\Khalil\Desktop\Studies\Semestre 2\Pi Dev\Java\Workshop-JDBC-2526-main"
mvn javafx:run
```

## Notes

- Current data shown in dashboard charts/lists/cards is mock data for interface preview.
- Next step is wiring `DashboardService` + repositories to real MySQL queries.

