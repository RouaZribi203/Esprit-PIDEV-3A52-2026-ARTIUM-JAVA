package controllers;

import entities.User;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import services.ServiceUser;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Controller for the Sign Up / Create Account page.
 */
public class SignUpController {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private DatePicker dobPicker;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ToggleGroup roleGroup;
    @FXML private Label errorLabel;

    // Reuse one service instance across multiple sign-up attempts.
    private final ServiceUser service = new ServiceUser();

    @FXML
    private void onSignUp() {
        errorLabel.setStyle("-fx-text-fill: #e74c3c;");
        errorLabel.setText("");

        String firstName = firstNameField.getText().trim();
        String lastName  = lastNameField.getText().trim();
        String email     = emailField.getText().trim();
        LocalDate dob    = dobPicker.getValue();
        String password  = passwordField.getText();
        String confirmPw = confirmPasswordField.getText();

        RadioButton selected = (RadioButton) roleGroup.getSelectedToggle();
        String role = selected != null ? selected.getUserData().toString() : "ROLE_AMATEUR";

        // --- Validation ---
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs obligatoires.");
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError("Adresse e-mail invalide.");
            return;
        }
        if (dob == null) {
            showError("Veuillez sélectionner votre date de naissance.");
            return;
        }
        if (dob.isAfter(LocalDate.now().minusYears(13))) {
            showError("Vous devez avoir au moins 13 ans.");
            return;
        }
        if (password.length() < 6) {
            showError("Le mot de passe doit contenir au moins 6 caractères.");
            return;
        }
        if (!password.equals(confirmPw)) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }

        // --- Persist ---
        User user = new User();
        user.setPrenom(firstName);
        user.setNom(lastName);
        user.setEmail(email);
        user.setDateNaissance(dob);
        user.setMdp(password); // TODO: hash before storing
        user.setRole(role);
        user.setStatut("active");

        try {
            if (service.emailExists(email)) {
                showError("Un compte avec cet e-mail existe déjà.");
                return;
            }
            service.add(user);
            showSuccess("Compte créé avec succès ! Vous pouvez maintenant vous connecter.");
            clearForm();
        } catch (SQLException e) {
            showError("Erreur lors de l'inscription : " + e.getMessage());
        }
    }

    @FXML
    private void onLoginLink() {
        // TODO: navigate to Login page when it is created
    }

    @FXML
    private void onHomeClick() {
        MainFX.switchToAdminView();
    }

    // --- helpers ---

    private void showError(String message) {
        errorLabel.setStyle("-fx-text-fill: #e74c3c;");
        errorLabel.setText(message);
    }

    private void showSuccess(String message) {
        errorLabel.setStyle("-fx-text-fill: #27ae60;");
        errorLabel.setText(message);
    }

    private void clearForm() {
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        dobPicker.setValue(null);
        passwordField.clear();
        confirmPasswordField.clear();
        roleGroup.getToggles().stream()
                .filter(t -> "ROLE_AMATEUR".equals(t.getUserData()))
                .findFirst()
                .ifPresent(t -> t.setSelected(true));
    }
}
