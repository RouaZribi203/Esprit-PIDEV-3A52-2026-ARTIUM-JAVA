package controllers;

import services.UserService;
import entities.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import utils.InputValidator;

import java.sql.SQLDataException;

public class ConnexionController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        emailField.textProperty().addListener((obs, oldV, newV) -> validateLiveInput());
        passwordField.textProperty().addListener((obs, oldV, newV) -> validateLiveInput());
    }

    @FXML
    private void onLogin() {
        clearMessage();

        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        String localError = validateCredentials(email, password);
        if (localError != null) {
            setMessage(localError);
            return;
        }

        try {
            User user = userService.authenticate(email, password);
            String statut = user.getStatut() == null ? "" : user.getStatut().trim().toLowerCase();
            if ("bloqué".equals(statut) || "blocked".equals(statut)) {
                setMessage("Compte bloqué. Contactez l'administrateur.");
                return;
            }
            routeByRole(user);
        } catch (SQLDataException e) {
            setMessage(e.getMessage());
        }
    }

    @FXML
    private void onCreateAccount() {
        MainFX.switchToRegistrationView();
    }

    @FXML
    private void onBackToLanding() {
        MainFX.switchToAuthLandingView();
    }

    @FXML
    private void onForgotPassword() {
        MainFX.switchToForgotPasswordView();
    }

    private void routeByRole(User user) {
        String role = user.getRole() == null ? "" : user.getRole().trim().toLowerCase();
        switch (role) {
            case "amateur":
                MainFX.switchToAmateurView(user);
                break;
            case "artiste":
            case "artist":
                MainFX.switchToArtistView(user);
                break;
            case "admin":
                MainFX.switchToAdminView(user);
                break;
            default:
                setMessage("Rôle utilisateur non supporté : " + user.getRole());
        }
    }

    private void validateLiveInput() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();

        if (InputValidator.isBlank(email)) {
            clearMessage();
            return;
        }

        if (!InputValidator.isValidEmail(email)) {
            setMessage("Format d'e-mail invalide.");
            return;
        }

        clearMessage();
    }

    private String validateCredentials(String email, String password) {
        if (InputValidator.isBlank(email) || InputValidator.isBlank(password)) {
            return "Veuillez saisir votre e-mail et votre mot de passe.";
        }
        if (!InputValidator.isValidEmail(email)) {
            return "Format d'e-mail invalide.";
        }
        return null;
    }

    private void setMessage(String message) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("error", "success");
        messageLabel.getStyleClass().add("error");
    }

    private void clearMessage() {
        messageLabel.setText("");
        messageLabel.getStyleClass().removeAll("error", "success");
    }
}




