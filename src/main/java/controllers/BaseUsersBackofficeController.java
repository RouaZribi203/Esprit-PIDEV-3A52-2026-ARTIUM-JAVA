package controllers;

import Services.UserService;
import entities.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.SQLDataException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public abstract class BaseUsersBackofficeController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final String STATUT_ACTIVE = "Activé";
    private static final String STATUT_BLOQUE = "Bloqué";
    private static final String[] CENTRES_INTERET = {
            "Peinture",
            "Sculpture",
            "Photographie",
            "Musique",
            "Lecture"
    };
    private static final String[] SPECIALITES_ARTISTE = {
            "Peintre",
            "Sculpteur",
            "Photographe",
            "Musicien",
            "Auteur"
    };

    @FXML
    protected TextField searchField;

    @FXML
    protected FlowPane cardsContainer;

    @FXML
    protected Label messageLabel;

    private final UserService userService = new UserService();
    private final ObservableList<User> users = FXCollections.observableArrayList();

    protected abstract String managedRole();

    protected abstract String managedRoleLabel();

    @FXML
    public void initialize() {
        cardsContainer.setHgap(18);
        cardsContainer.setVgap(18);
        cardsContainer.setPrefWrapLength(1140);
        searchField.textProperty().addListener((obs, oldValue, newValue) -> renderCards());
        loadUsers();
    }

    @FXML
    protected void onAddUser() {
        Optional<User> dialogResult = showUserDialog(null);
        if (dialogResult.isEmpty()) {
            return;
        }

        try {
            userService.add(dialogResult.get());
            setMessage(managedRoleLabel() + " ajoute avec succes.", false);
            loadUsers();
        } catch (SQLDataException e) {
            setMessage("Erreur ajout: " + e.getMessage(), true);
        }
    }

    protected void onEditUser(User existingUser) {
        Optional<User> dialogResult = showUserDialog(existingUser);
        if (dialogResult.isEmpty()) {
            return;
        }

        try {
            userService.update(dialogResult.get());
            setMessage(managedRoleLabel() + " modifie avec succes.", false);
            loadUsers();
        } catch (SQLDataException e) {
            setMessage("Erreur modification: " + e.getMessage(), true);
        }
    }

    protected void onDeleteUser(User user) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer " + managedRoleLabel().toLowerCase(Locale.ROOT));
        confirmation.setContentText("Confirmer la suppression de " + user.getNom() + " " + user.getPrenom() + " ?");

        Optional<ButtonType> choice = confirmation.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) {
            return;
        }

        try {
            userService.delete(user);
            setMessage(managedRoleLabel() + " supprime avec succes.", false);
            loadUsers();
        } catch (SQLDataException e) {
            setMessage("Erreur suppression: " + e.getMessage(), true);
        }
    }

    private void loadUsers() {
        try {
            users.setAll(userService.getByRole(managedRole()));
            renderCards();
            setMessage(users.size() + " " + managedRoleLabel().toLowerCase(Locale.ROOT) + "(s) charges.", false);
        } catch (SQLDataException e) {
            users.clear();
            cardsContainer.getChildren().clear();
            setMessage("Erreur chargement: " + e.getMessage(), true);
        }
    }

    private void renderCards() {
        cardsContainer.getChildren().clear();

        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        List<User> filteredUsers = users.stream()
                .filter(user -> matchesQuery(user, query))
                .toList();

        for (User user : filteredUsers) {
            cardsContainer.getChildren().add(createUserCard(user));
        }

        if (filteredUsers.isEmpty()) {
            Label emptyState = new Label("Aucun " + managedRoleLabel().toLowerCase(Locale.ROOT) + " trouve.");
            emptyState.getStyleClass().add("users-empty-state");
            cardsContainer.getChildren().add(emptyState);
        }
    }

    private boolean matchesQuery(User user, String query) {
        if (query.isEmpty()) {
            return true;
        }

        String searchableText = String.join(" ",
                safe(user.getNom()),
                safe(user.getPrenom()),
                safe(user.getEmail()),
                safe(user.getVille()),
                safe(user.getNumTel()));
        return searchableText.toLowerCase(Locale.ROOT).contains(query);
    }

    private Node createUserCard(User user) {
        VBox card = new VBox(14);
        card.setPrefWidth(356);
        card.setMinHeight(248);
        card.getStyleClass().add("user-card");

        Label initials = new Label(buildInitials(user));
        initials.getStyleClass().add("user-avatar");

        Label nameLabel = new Label(safe(user.getNom()) + " " + safe(user.getPrenom()));
        nameLabel.getStyleClass().add("user-card-title");

        Label emailLabel = new Label(safe(user.getEmail()));
        emailLabel.getStyleClass().add("user-card-subtitle");

        VBox identity = new VBox(2, nameLabel, emailLabel);
        identity.getStyleClass().add("user-card-identity");

        Label statusBadge = new Label(formatStatusLabel(user.getStatut()));
        statusBadge.getStyleClass().addAll("badge-status", statusStyleClass(user.getStatut()));

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        HBox topRow = new HBox(12, initials, identity, topSpacer, statusBadge);
        topRow.getStyleClass().add("user-card-top");

        Label roleBadge = new Label(managedRoleLabel());
        roleBadge.getStyleClass().add("badge-role");

        Label roleSpecificBadge = new Label("Artiste".equalsIgnoreCase(managedRole())
                ? "Specialite"
                : "Centre interet");
        roleSpecificBadge.getStyleClass().add("badge-role-secondary");

        HBox badgeRow = new HBox(8, roleBadge, roleSpecificBadge);
        badgeRow.getStyleClass().add("user-card-badges");

        VBox body = new VBox(8,
                createInfoRow("Telephone", safe(user.getNumTel())),
                createInfoRow("Ville", safe(user.getVille())),
                createInfoRow("Naissance", user.getDateNaissance() == null ? "-" : user.getDateNaissance().toString()),
                createInfoRow("Statut", formatStatusLabel(user.getStatut())),
                createInfoRow("Artiste".equalsIgnoreCase(managedRole()) ? "Specialite" : "Centre interet",
                        "Artiste".equalsIgnoreCase(managedRole()) ? safe(user.getSpecialite()) : safe(user.getCentreInteret()))
        );
        body.getStyleClass().add("user-card-body");

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getStyleClass().add("user-card-actions");

        Button detailsButton = new Button("Voir details");
        detailsButton.getStyleClass().addAll("card-action-button", "card-soft-button");
        detailsButton.setOnAction(event -> showDetails(user));

        Button editButton = new Button("Modifier");
        editButton.getStyleClass().addAll("card-action-button", "card-soft-button");
        editButton.setOnAction(event -> onEditUser(user));

        Button deleteButton = new Button("Supprimer");
        deleteButton.getStyleClass().addAll("card-action-button", "card-danger-button");
        deleteButton.setOnAction(event -> onDeleteUser(user));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        actions.getChildren().addAll(detailsButton, spacer, editButton, deleteButton);

        card.getChildren().addAll(topRow, badgeRow, body, actions);
        return card;
    }

    private HBox createInfoRow(String label, String value) {
        Label keyLabel = new Label(label);
        keyLabel.getStyleClass().add("user-info-key");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("user-info-value");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(8, keyLabel, spacer, valueLabel);
        row.getStyleClass().add("user-info-row");
        return row;
    }

    private String formatStatusLabel(String status) {
        String normalized = safe(status).toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "activé", "active" -> "Activé";
            case "bloqué", "blocked" -> "Bloqué";
            case "pending" -> "En attente";
            default -> normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
        };
    }

    private String statusStyleClass(String status) {
        String normalized = safe(status).toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "activé", "active" -> "status-active";
            case "bloqué", "blocked" -> "status-blocked";
            case "pending" -> "status-pending";
            default -> "status-neutral";
        };
    }

    private void showDetails(User user) {
        Alert details = new Alert(Alert.AlertType.INFORMATION);
        details.setTitle("Details utilisateur");
        details.setHeaderText(user.getNom() + " " + user.getPrenom());

        String roleSpecific = "Artiste".equalsIgnoreCase(managedRole())
                ? "Specialite: " + safe(user.getSpecialite())
                : "Centre interet: " + safe(user.getCentreInteret());

        String content = String.join("\n",
                "Email: " + safe(user.getEmail()),
                "Tel: " + safe(user.getNumTel()),
                "Ville: " + safe(user.getVille()),
                "Date naissance: " + (user.getDateNaissance() == null ? "-" : user.getDateNaissance().toString()),
                "Statut: " + safe(user.getStatut()),
                roleSpecific,
                "Biographie: " + safe(user.getBiographie()));
        details.setContentText(content);
        details.showAndWait();
    }

    private Optional<User> showUserDialog(User existingUser) {
        boolean creation = existingUser == null;

        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(creation ? "Ajouter " + managedRoleLabel().toLowerCase(Locale.ROOT) : "Modifier " + managedRoleLabel().toLowerCase(Locale.ROOT));
        dialog.setHeaderText(null);
        dialog.getDialogPane().getStyleClass().add("users-dialog-pane");
        if (searchField != null && searchField.getScene() != null) {
            dialog.getDialogPane().getStylesheets().addAll(searchField.getScene().getStylesheets());
        }
        dialog.getDialogPane().setGraphic(null);
        dialog.setResizable(false);
        dialog.getDialogPane().setPrefSize(560, 640);
        dialog.getDialogPane().setMinSize(560, 640);
        dialog.getDialogPane().setMaxSize(560, 640);

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.getStyleClass().add("users-dialog-form");

        TextField nomField = new TextField();
        TextField prenomField = new TextField();
        DatePicker dateNaissancePicker = new DatePicker();
        TextField emailField = new TextField();
        PasswordField passwordField = new PasswordField();
        PasswordField confirmPasswordField = new PasswordField();
        TextField telField = new TextField();
        TextField villeField = new TextField();
        ComboBox<String> statutCombo = new ComboBox<>(FXCollections.observableArrayList(STATUT_ACTIVE, STATUT_BLOQUE));
        ComboBox<String> roleSpecificCombo = new ComboBox<>();
        TextArea bioArea = new TextArea();
        Label formErrorLabel = new Label();

        nomField.getStyleClass().add("users-dialog-field");
        prenomField.getStyleClass().add("users-dialog-field");
        dateNaissancePicker.getStyleClass().add("users-dialog-field");
        emailField.getStyleClass().add("users-dialog-field");
        passwordField.getStyleClass().add("users-dialog-field");
        confirmPasswordField.getStyleClass().add("users-dialog-field");
        telField.getStyleClass().add("users-dialog-field");
        villeField.getStyleClass().add("users-dialog-field");
        statutCombo.getStyleClass().add("users-dialog-field");
        bioArea.getStyleClass().addAll("users-dialog-field", "users-dialog-textarea");
        roleSpecificCombo.getStyleClass().add("users-dialog-field");

        bioArea.setPrefRowCount(3);

        boolean artistRole = "Artiste".equalsIgnoreCase(managedRole());
        String roleSpecificLabel = artistRole ? "Specialite" : "Centre interet";
        String dialogModeLabel = creation ? "Nouveau profil" : "Modification du profil";
        String dialogHint = creation
                ? "Crée une fiche utilisateur claire, premium et bien structurée."
                : "Mets à jour les informations avec une présentation plus soignée.";

        roleSpecificCombo.getItems().setAll(artistRole ? SPECIALITES_ARTISTE : CENTRES_INTERET);
        roleSpecificCombo.setPromptText(artistRole ? "Choisissez une spécialité" : "Choisissez un centre d'interet");

        if (creation) {
            statutCombo.setValue(STATUT_ACTIVE);
        }

        if (!creation) {
            nomField.setText(existingUser.getNom());
            prenomField.setText(existingUser.getPrenom());
            dateNaissancePicker.setValue(existingUser.getDateNaissance());
            emailField.setText(existingUser.getEmail());
            telField.setText(existingUser.getNumTel());
            villeField.setText(existingUser.getVille());
            statutCombo.setValue(normalizeStatutValue(existingUser.getStatut()));
            bioArea.setText(existingUser.getBiographie());
            String existingRoleSpecific = artistRole ? safe(existingUser.getSpecialite()) : safe(existingUser.getCentreInteret());
            selectComboValue(roleSpecificCombo, existingRoleSpecific);
        }

        Label modeChip = new Label(dialogModeLabel);
        modeChip.getStyleClass().add("users-dialog-mode-chip");

        Label roleChip = new Label(managedRoleLabel());
        roleChip.getStyleClass().add("users-dialog-role-chip");

        Label statusChip = new Label(creation ? "Création" : "Édition");
        statusChip.getStyleClass().add("users-dialog-status-chip");

        Label dialogTitle = new Label((creation ? "Créer " : "Modifier ") + managedRoleLabel().toLowerCase(Locale.ROOT));
        dialogTitle.getStyleClass().add("users-dialog-title");

        Label dialogSubtitle = new Label(dialogHint);
        dialogSubtitle.getStyleClass().add("users-dialog-subtitle");

        HBox chipRow = new HBox(8, modeChip, roleChip, statusChip);
        chipRow.getStyleClass().add("users-dialog-chip-row");

        VBox hero = new VBox(6, chipRow, dialogTitle, dialogSubtitle);
        hero.getStyleClass().add("users-dialog-hero");

        VBox identityCard = new VBox(10);
        identityCard.getStyleClass().add("users-dialog-section-card");

        Label identityTitle = new Label("Identité");
        identityTitle.getStyleClass().add("users-dialog-section-title");
        Label identitySubtitle = new Label("Les informations de base du profil.");
        identitySubtitle.getStyleClass().add("users-dialog-section-subtitle");
        VBox identityHeader = new VBox(2, identityTitle, identitySubtitle);

        GridPane identityGrid = new GridPane();
        identityGrid.setHgap(10);
        identityGrid.setVgap(10);
        Label nomLabel = new Label("Nom");
        nomLabel.getStyleClass().add("users-dialog-label");
        Label prenomLabel = new Label("Prenom");
        prenomLabel.getStyleClass().add("users-dialog-label");
        Label naissanceLabel = new Label("Date naissance");
        naissanceLabel.getStyleClass().add("users-dialog-label");
        passwordField.setPromptText(creation ? "Minimum 8 caracteres" : "Laisser vide pour conserver");
        Label emailLabel = new Label("Email");
        emailLabel.getStyleClass().add("users-dialog-label");
        Label passwordLabel = new Label("Mot de passe");
        passwordLabel.getStyleClass().add("users-dialog-label");
        Label confirmationLabel = new Label("Confirmation");
        confirmationLabel.getStyleClass().add("users-dialog-label");
        Label telephoneLabel = new Label("Telephone");
        telephoneLabel.getStyleClass().add("users-dialog-label");
        Label villeLabel = new Label("Ville");
        villeLabel.getStyleClass().add("users-dialog-label");
        Label statutLabel = new Label("Statut");
        statutLabel.getStyleClass().add("users-dialog-label");
        Label roleSpecificTextLabel = new Label(roleSpecificLabel);
        roleSpecificTextLabel.getStyleClass().add("users-dialog-label");
        Label biographieLabel = new Label("Biographie");
        biographieLabel.getStyleClass().add("users-dialog-label");

        identityGrid.add(nomLabel, 0, 0);
        identityGrid.add(nomField, 1, 0);
        identityGrid.add(prenomLabel, 0, 1);
        identityGrid.add(prenomField, 1, 1);
        identityGrid.add(naissanceLabel, 0, 2);
        identityGrid.add(dateNaissancePicker, 1, 2);

        VBox identityCardContent = new VBox(10, identityHeader, identityGrid);
        identityCard.getChildren().add(identityCardContent);

        VBox contactCard = new VBox(10);
        contactCard.getStyleClass().add("users-dialog-section-card");
        Label contactTitle = new Label("Coordonnées");
        contactTitle.getStyleClass().add("users-dialog-section-title");
        Label contactSubtitle = new Label("Email, téléphone et ville de contact.");
        contactSubtitle.getStyleClass().add("users-dialog-section-subtitle");
        VBox contactHeader = new VBox(2, contactTitle, contactSubtitle);

        GridPane contactGrid = new GridPane();
        contactGrid.setHgap(10);
        contactGrid.setVgap(10);
        contactGrid.add(emailLabel, 0, 0);
        contactGrid.add(emailField, 1, 0);
        contactGrid.add(telephoneLabel, 0, 1);
        contactGrid.add(telField, 1, 1);
        contactGrid.add(villeLabel, 0, 2);
        contactGrid.add(villeField, 1, 2);

        contactCard.getChildren().addAll(contactHeader, contactGrid);

        VBox securityCard = new VBox(10);
        securityCard.getStyleClass().add("users-dialog-section-card");
        Label securityTitle = new Label("Sécurité et statut");
        securityTitle.getStyleClass().add("users-dialog-section-title");
        Label securitySubtitle = new Label("Accès, rôle et état du compte.");
        securitySubtitle.getStyleClass().add("users-dialog-section-subtitle");
        VBox securityHeader = new VBox(2, securityTitle, securitySubtitle);

        GridPane securityGrid = new GridPane();
        securityGrid.setHgap(10);
        securityGrid.setVgap(10);
        securityGrid.add(passwordLabel, 0, 0);
        securityGrid.add(passwordField, 1, 0);
        securityGrid.add(confirmationLabel, 0, 1);
        securityGrid.add(confirmPasswordField, 1, 1);
        securityGrid.add(statutLabel, 0, 2);
        securityGrid.add(statutCombo, 1, 2);
        securityGrid.add(roleSpecificTextLabel, 0, 3);
        securityGrid.add(roleSpecificCombo, 1, 3);

        securityCard.getChildren().addAll(securityHeader, securityGrid);

        VBox bioCard = new VBox(10);
        bioCard.getStyleClass().addAll("users-dialog-section-card", "users-dialog-bio-card");
        Label bioTitle = new Label("Biographie");
        bioTitle.getStyleClass().add("users-dialog-section-title");
        Label bioSubtitle = new Label("Une présentation courte et soignée du profil.");
        bioSubtitle.getStyleClass().add("users-dialog-section-subtitle");
        VBox bioHeader = new VBox(2, bioTitle, bioSubtitle);
        bioCard.getChildren().addAll(bioHeader, bioArea);

        VBox dialogContent = new VBox(14, hero, identityCard, contactCard, securityCard, bioCard, formErrorLabel);
        dialogContent.getStyleClass().add("users-dialog-content");
        dialogContent.setFillWidth(true);
        formErrorLabel.getStyleClass().addAll("dialog-error", "users-dialog-error");

        ScrollPane dialogScroll = new ScrollPane(dialogContent);
        dialogScroll.getStyleClass().add("users-dialog-scroll");
        dialogScroll.setFitToWidth(true);
        dialogScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        dialogScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        dialogScroll.setPannable(true);
        dialogScroll.setPrefViewportWidth(520);
        dialogScroll.setPrefViewportHeight(540);
        dialog.getDialogPane().setContent(dialogScroll);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.getStyleClass().add("primary-action-button");
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().addAll("card-action-button", "card-soft-button");

        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            String validationError = validateForm(
                    creation,
                    nomField.getText(),
                    prenomField.getText(),
                    dateNaissancePicker.getValue(),
                    emailField.getText(),
                    passwordField.getText(),
                    confirmPasswordField.getText(),
                    telField.getText(),
                    villeField.getText(),
                    statutCombo.getValue(),
                    roleSpecificCombo.getValue());

            if (validationError != null) {
                formErrorLabel.setText(validationError);
                event.consume();
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) {
                return null;
            }

            User user = new User();
            if (!creation) {
                user.setId(existingUser.getId());
                user.setDateInscription(existingUser.getDateInscription());
                user.setPhotoProfil(existingUser.getPhotoProfil());
                user.setPhotoReferencePath(existingUser.getPhotoReferencePath());
            }

            user.setNom(clean(nomField.getText()));
            user.setPrenom(clean(prenomField.getText()));
            user.setDateNaissance(dateNaissancePicker.getValue());
            user.setEmail(clean(emailField.getText()));
            user.setMdp(clean(passwordField.getText()));
            user.setRole(managedRole());
            user.setStatut(clean(statutCombo.getValue()));
            user.setNumTel(clean(telField.getText()));
            user.setVille(clean(villeField.getText()));
            user.setBiographie(clean(bioArea.getText()));
            user.setDateInscription(user.getDateInscription() == null ? LocalDate.now() : user.getDateInscription());
            if (artistRole) {
                user.setSpecialite(clean(roleSpecificCombo.getValue()));
                user.setCentreInteret(null);
            } else {
                user.setCentreInteret(clean(roleSpecificCombo.getValue()));
                user.setSpecialite(null);
            }
            return user;
        });

        return dialog.showAndWait();
    }

    private String validateForm(boolean creation,
                                String nom,
                                String prenom,
                                LocalDate dateNaissance,
                                String email,
                                String password,
                                String confirmPassword,
                                String telephone,
                                String ville,
                                String statut,
                                String roleSpecificValue) {
        if (isBlank(nom)) return "Le nom est obligatoire.";
        if (isBlank(prenom)) return "Le prenom est obligatoire.";
        if (dateNaissance == null) return "La date de naissance est obligatoire.";
        if (isBlank(email) || !EMAIL_PATTERN.matcher(email.trim()).matches()) return "Email invalide.";

        if (creation) {
            if (isBlank(password) || password.trim().length() < 8) return "Mot de passe minimum 8 caracteres.";
        } else if (!isBlank(password) && password.trim().length() < 8) {
            return "Mot de passe minimum 8 caracteres.";
        }

        if (!isBlank(password) && !password.equals(confirmPassword)) return "Confirmation du mot de passe invalide.";
        if (isBlank(telephone)) return "Le telephone est obligatoire.";
        if (isBlank(ville)) return "La ville est obligatoire.";
        if (isBlank(statut)) return "Le statut est obligatoire.";
        if (isBlank(roleSpecificValue)) {
            return "Artiste".equalsIgnoreCase(managedRole())
                    ? "La specialite est obligatoire."
                    : "Le centre interet est obligatoire.";
        }
        return null;
    }

    private String normalizeStatutValue(String status) {
        String normalized = safe(status).trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "activé", "active" -> STATUT_ACTIVE;
            case "bloqué", "blocked" -> STATUT_BLOQUE;
            default -> STATUT_ACTIVE;
        };
    }

    private void selectComboValue(ComboBox<String> comboBox, String value) {
        if (comboBox == null) {
            return;
        }

        String cleaned = clean(value);
        if (isBlank(cleaned)) {
            comboBox.setValue(null);
            return;
        }

        if (!comboBox.getItems().contains(cleaned)) {
            comboBox.getItems().add(0, cleaned);
        }
        comboBox.setValue(cleaned);
    }

    private void setMessage(String message, boolean error) {
        if (messageLabel == null) {
            return;
        }
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("error", "success");
        messageLabel.getStyleClass().add(error ? "error" : "success");
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private String buildInitials(User user) {
        String first = isBlank(user.getPrenom()) ? "" : user.getPrenom().trim();
        String last = isBlank(user.getNom()) ? "" : user.getNom().trim();
        String initials = (first.isEmpty() ? "" : first.substring(0, 1)) + (last.isEmpty() ? "" : last.substring(0, 1));
        return initials.isEmpty() ? "U" : initials.toUpperCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}


