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
            case "active" -> "Actif";
            case "pending" -> "En attente";
            case "blocked" -> "Bloque";
            default -> normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
        };
    }

    private String statusStyleClass(String status) {
        String normalized = safe(status).toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "active" -> "status-active";
            case "pending" -> "status-pending";
            case "blocked" -> "status-blocked";
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
        dialog.setHeaderText(creation ? "Nouveau " + managedRoleLabel().toLowerCase(Locale.ROOT) : "Mise a jour des informations");

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);

        TextField nomField = new TextField();
        TextField prenomField = new TextField();
        DatePicker dateNaissancePicker = new DatePicker();
        TextField emailField = new TextField();
        PasswordField passwordField = new PasswordField();
        PasswordField confirmPasswordField = new PasswordField();
        TextField telField = new TextField();
        TextField villeField = new TextField();
        ComboBox<String> statutCombo = new ComboBox<>(FXCollections.observableArrayList("active", "pending", "blocked"));
        TextField roleSpecificField = new TextField();
        TextArea bioArea = new TextArea();
        Label formErrorLabel = new Label();

        bioArea.setPrefRowCount(3);

        String roleSpecificLabel = "Artiste".equalsIgnoreCase(managedRole()) ? "Specialite" : "Centre interet";

        if (creation) {
            statutCombo.setValue("active");
        }

        if (!creation) {
            nomField.setText(existingUser.getNom());
            prenomField.setText(existingUser.getPrenom());
            dateNaissancePicker.setValue(existingUser.getDateNaissance());
            emailField.setText(existingUser.getEmail());
            telField.setText(existingUser.getNumTel());
            villeField.setText(existingUser.getVille());
            statutCombo.setValue(existingUser.getStatut());
            bioArea.setText(existingUser.getBiographie());
            roleSpecificField.setText("Artiste".equalsIgnoreCase(managedRole()) ? safe(existingUser.getSpecialite()) : safe(existingUser.getCentreInteret()));
        }

        int row = 0;
        form.add(new Label("Nom"), 0, row);
        form.add(nomField, 1, row++);
        form.add(new Label("Prenom"), 0, row);
        form.add(prenomField, 1, row++);
        form.add(new Label("Date naissance"), 0, row);
        form.add(dateNaissancePicker, 1, row++);
        form.add(new Label("Email"), 0, row);
        form.add(emailField, 1, row++);
        form.add(new Label("Mot de passe"), 0, row);
        passwordField.setPromptText(creation ? "Minimum 8 caracteres" : "Laisser vide pour conserver");
        form.add(passwordField, 1, row++);
        form.add(new Label("Confirmation"), 0, row);
        form.add(confirmPasswordField, 1, row++);
        form.add(new Label("Telephone"), 0, row);
        form.add(telField, 1, row++);
        form.add(new Label("Ville"), 0, row);
        form.add(villeField, 1, row++);
        form.add(new Label("Statut"), 0, row);
        form.add(statutCombo, 1, row++);
        form.add(new Label(roleSpecificLabel), 0, row);
        form.add(roleSpecificField, 1, row++);
        form.add(new Label("Biographie"), 0, row);
        form.add(bioArea, 1, row);

        VBox dialogContent = new VBox(10, form, formErrorLabel);
        formErrorLabel.getStyleClass().add("dialog-error");
        dialog.getDialogPane().setContent(dialogContent);

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
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
                    roleSpecificField.getText());

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
            if ("Artiste".equalsIgnoreCase(managedRole())) {
                user.setSpecialite(clean(roleSpecificField.getText()));
                user.setCentreInteret(null);
            } else {
                user.setCentreInteret(clean(roleSpecificField.getText()));
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


