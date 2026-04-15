# Artium - Library Management System

## 1. Project Description
Artium is a Java Desktop Application built with JavaFX for managing a modern digital library. This specific module handles the "Books" (Livre) and "Book Rentals" (Location_Livre) features. It provides a robust interface for administrators to manage the library inventory, artists to publish their books, and amateurs to browse the catalog.

## 2. Technologies Used
* **Java 17**: Core programming language.
* **JavaFX 20**: UI toolkit for building rich desktop applications.
* **JDBC**: For direct database connectivity.
* **MySQL**: Relational database (using the `artium_db` schema).
* **Maven**: Dependency management and build tool.

## 3. Architecture Explanation (MVC)
The application strictly follows the **Model-View-Controller (MVC)** architectural pattern to ensure clean separation of concerns:

* **Model (Entities & Services)**
  * Located in `src/main/java/entities` and `src/main/java/services`.
  * **Entities** (`Livre`, `LocationLivre`, `CollectionOeuvre`, `Oeuvre`) represent the data structure.
  * **Services** (`JdbcLivreService`, `JdbcCollectionService`) handle business logic and database interactions (Data Access Object pattern).

* **View (FXML & CSS)**
  * Located in `src/main/resources/views`.
  * XML-based definitions of the user interface (`livres.fxml`, `Bibliotheque.fxml`).
  * CSS files (`dashboard.css`, `artist-theme.css`, `amateur-theme.css`) define the styling for different roles.

* **Controller (Java Controllers)**
  * Located in `src/main/java/controllers`.
  * Java classes (e.g., `LivresController`, `BibliothequArtisteController`) that handle user input, interact with the Service layer, and update the Views.

## 4. Explanation of CRUD Operations (ECRUD)
The system supports full **ECRUD** operations for Books (`Livre`):

* **Create (Add)**: Users can fill out a form with Title, Category, Rental Price, Collection, Cover Image, and PDF file. The `JdbcLivreService.add()` method inserts this data into both the `oeuvre` and `livre` tables.
* **Read (Search/List)**: Books are fetched using `JdbcLivreService.getAll()` or `search()`. The results are displayed in a JavaFX `TableView` with dynamic columns (e.g., availability status).
* **Update (Edit)**: Selecting a row in the table populates the form. Modifying the fields and clicking "Modifier" calls `JdbcLivreService.update()`, updating the specific row in the database.
* **Delete**: Selecting a row and clicking "Supprimer" removes the book from the database via `JdbcLivreService.delete()`.

## 5. How the Roles Work
The application provides customized navigation and features based on three distinct user roles:

1. **ADMIN (BackOffice)**
   * **Dashboard**: `MainLayout.fxml` -> `livres.fxml`
   * **Features**: Full CRUD access to all books across all collections in the system. The admin can add, edit, delete, and search any book.

2. **ARTIST**
   * **Dashboard**: `ArtistMain.fxml` -> `Bibliotheque.fxml`
   * **Features**: Full CRUD access to their own library. Artists can publish new books, update existing ones, and remove books they no longer wish to share.

3. **AMATEUR**
   * **Dashboard**: `AmateurMain.fxml` -> `Bibliotheque.fxml`
   * **Features**: Read-only access. Amateurs can browse the catalog, search for specific titles or categories, and view book details. They cannot add, modify, or delete books.

## 6. How to Run the Application
1. **Database Setup**:
   * Ensure MySQL is running on `localhost:3306`.
   * Import the provided `artium_db (2).sql` file into your MySQL server to create the schema and initial data.
2. **IDE Setup**:
   * Open the project in IntelliJ IDEA (or your preferred IDE).
   * Ensure the project SDK is set to **Java 17**.
   * Reload the Maven project to download JavaFX and MySQL dependencies.
3. **Execution**:
   * Run the `MainFX.java` class located in `src/main/java/controllers/MainFX.java`.
   * By default, the `MainFX.start()` method launches the Admin Dashboard. You can modify `MainFX.java` to call `switchToArtistView()` or `switchToAmateurView()` to test different roles.

## 7. How to Maintain the App
* **Code Structure**:
  * `entities/`: Data models. Modify these if database columns change.
  * `services/`: Database queries. Modify these to change how data is fetched or saved.
  * `controllers/`: UI logic. Modify these to change button behaviors or table population.
  * `views/`: FXML files. Use JavaFX Scene Builder to visually edit these files.
* **Where to modify logic**:
  * If a database query is failing, check `JdbcLivreService.java`.
  * If a button click does nothing, check the `@FXML` methods in the respective controller (e.g., `LivresController.java`).

## 8. How to Extend the App
* **Add a new entity**:
  1. Create a new Java class in `entities/` (e.g., `Review.java`).
  2. Create a new interface and JDBC implementation in `services/` (e.g., `ReviewService.java`, `JdbcReviewService.java`).
  3. Create a new FXML view in `views/` and a corresponding Controller in `controllers/`.
* **Add a new role**:
  1. Create a new main FXML layout (e.g., `ModeratorMain.fxml`).
  2. Add a new `switchToModeratorView()` method in `MainFX.java`.
  3. Create specific controllers and restrict UI elements as needed.
* **Add new features (e.g., Book Rental)**:
  1. Implement `JdbcLocationLivreService` to handle INSERT operations into the `location_livre` table.
  2. Add a "Rent" button in the Amateur UI that calls this service.

## 9. Example Screenshots Description
*(Note: Actual images are not included, this describes what the UI looks like)*

1. **Admin Books Management (livres.fxml)**:
   * A split-screen view. The left side features a large table listing all books with columns for ID, Title, Author, Category, Price, and Availability. Above the table is a search bar. The right side contains a form with text fields, a dropdown for Collections, file choosers for Cover Image and PDF, and Action buttons (Ajouter, Supprimer, Vider).
2. **Artist Library (Bibliotheque.fxml)**:
   * A dark-themed dashboard. The main content area shows a grid or list of the artist's books with a prominent "Ajouter un Livre" button at the top right.
3. **Amateur Browsing (Bibliotheque.fxml)**:
   * A clean, read-only interface. The top section contains search filters (Title, Category). The main area displays recommended books and a grid of available books. There are no edit or delete buttons visible.