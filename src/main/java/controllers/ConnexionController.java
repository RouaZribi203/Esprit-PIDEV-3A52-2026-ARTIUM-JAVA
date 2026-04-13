package controllers;

import Services.UserService;
import entities.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

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
    private void onLogin() {
        clearMessage();

        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            setMessage("Veuillez saisir votre e-mail et votre mot de passe.");
            return;
        }

        try {
            User user = userService.authenticate(email, password);
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
                MainFX.switchToAmateurView();
                break;
            case "artiste":
            case "artist":
                MainFX.switchToArtistView();
                break;
            case "admin":
                MainFX.switchToAdminView();
                break;
            default:
                setMessage("Rôle utilisateur non supporté : " + user.getRole());
        }
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




