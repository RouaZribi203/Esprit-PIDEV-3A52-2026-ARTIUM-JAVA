package controllers;

import Services.UserService;
import components.HumanVerificationPane;
import entities.User;
import utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import utils.InputValidator;

import java.sql.SQLDataException;

public class ConnexionController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;
    @FXML private VBox loginCard; // ← nouveau, lié au fx:id du FXML

    private final UserService userService = new UserService();
    private HumanVerificationPane captchaPane; // ← nouveau

    @FXML
    public void initialize() {
        // Créer et insérer le captcha à l'index 3 (avant le bouton "Se connecter")
        captchaPane = new HumanVerificationPane();
        loginCard.getChildren().add(3, captchaPane); // ← nouveau

        emailField.textProperty().addListener((obs, oldV, newV) -> validateLiveInput());
        passwordField.textProperty().addListener((obs, oldV, newV) -> validateLiveInput());
    }

    @FXML
    private void onLogin() {
        clearMessage();

        // ← nouveau : vérification humaine avant tout
        if (!captchaPane.isHumanVerified()) {
            setMessage("Veuillez compléter la vérification humaine.");
            return;
        }

        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        String localError = validateCredentials(email, password);
        if (localError != null) {
            setMessage(localError);
            return;
        }

        try {
            User user = userService.authenticate(email, password);
            SessionManager.setCurrentUser(user);
            routeByRole(user);
        } catch (SQLDataException e) {
            setMessage(e.getMessage());
        }
    }

    @FXML private void onCreateAccount()  { MainFX.switchToRegistrationView(); }
    @FXML private void onBackToLanding()  { MainFX.switchToAuthLandingView(); }
    @FXML private void onForgotPassword() { MainFX.switchToForgotPasswordView(); }

    private void routeByRole(User user) {
        String role = user.getRole() == null ? "" : user.getRole().trim().toLowerCase();
        switch (role) {
            case "amateur"           -> MainFX.switchToAmateurView(user);
            case "artiste", "artist" -> MainFX.switchToArtistView(user);
            case "admin"             -> MainFX.switchToAdminView(user);
            default                  -> setMessage("Rôle utilisateur non supporté : " + user.getRole());
        }
    }

    private void validateLiveInput() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        if (InputValidator.isBlank(email)) { clearMessage(); return; }
        if (!InputValidator.isValidEmail(email)) { setMessage("Format d'e-mail invalide."); return; }
        clearMessage();
    }

    private String validateCredentials(String email, String password) {
        if (InputValidator.isBlank(email) || InputValidator.isBlank(password))
            return "Veuillez saisir votre e-mail et votre mot de passe.";
        if (!InputValidator.isValidEmail(email))
            return "Format d'e-mail invalide.";
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