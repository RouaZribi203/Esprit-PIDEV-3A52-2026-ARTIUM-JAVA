package controllers;

import entities.User;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

public class InscriptionController {

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
	private static final String ROLE_AMATEUR = "Amateur";
	private static final String ROLE_ARTISTE = "Artiste";
	private static final String ROLE_ADMIN = "Admin";
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

	@FXML private VBox stepOnePane;
	@FXML private VBox stepTwoPane;
	@FXML private VBox stepThreePane;
	@FXML private Label stepOneBadge;
	@FXML private Label stepTwoBadge;
	@FXML private Label stepThreeBadge;
	@FXML private VBox specialiteContainer;
	@FXML private VBox centreInteretContainer;
	@FXML private Label messageLabel;
	@FXML private Label photoPathLabel;
	@FXML private Label photoPlaceholderLabel;
	@FXML private TextField nomField;
	@FXML private TextField prenomField;
	@FXML private DatePicker dateNaissancePicker;
	@FXML private TextField phoneField;
	@FXML private TextField villeField;
	@FXML private TextField emailField;
	@FXML private PasswordField passwordField;
	@FXML private PasswordField confirmPasswordField;
	@FXML private ComboBox<String> specialiteComboBox;
	@FXML private ComboBox<String> centreInteretComboBox;
	@FXML private TextArea biographieArea;
	@FXML private ToggleGroup roleToggleGroup;
	@FXML private Button previousButton;
	@FXML private Button nextButton;
	@FXML private Button submitButton;

	private final User draftUser = new User();
	private int currentStep = 1;
	private String selectedPhotoPath;

	@FXML
	public void initialize() {
		specialiteComboBox.getItems().setAll(SPECIALITES_ARTISTE);
		centreInteretComboBox.getItems().setAll(CENTRES_INTERET);
		if (roleToggleGroup.getSelectedToggle() == null && !roleToggleGroup.getToggles().isEmpty()) {
			roleToggleGroup.selectToggle(roleToggleGroup.getToggles().get(0));
		}
		roleToggleGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> updateRoleHints());
		updateRoleHints();
		showStep(1, false);
	}

	@FXML
	private void onNextStep() {
		if (!validateCurrentStep()) {
			return;
		}
		showStep(Math.min(3, currentStep + 1), true);
	}

	@FXML
	private void onPreviousStep() {
		if (currentStep == 1) {
			MainFX.switchToAuthLandingView();
			return;
		}
		showStep(currentStep - 1, true);
	}

	@FXML
	private void onBackToLanding() {
		MainFX.switchToAuthLandingView();
	}

	@FXML
	private void onBrowsePhoto() {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Choisir une photo de référence");
		chooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
				new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
		);
		File file = chooser.showOpenDialog(previousButton.getScene().getWindow());
		if (file == null) {
			return;
		}
		selectedPhotoPath = file.getAbsolutePath();
		photoPathLabel.setText(file.getName());
		photoPlaceholderLabel.setVisible(false);
		photoPlaceholderLabel.setManaged(false);
		setMessage("Photo chargée avec succès.", false);
	}

	@FXML
	private void onSubmit() {
		if (!validateCurrentStep()) {
			return;
		}
		User user = buildUser();
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Inscription prête");
		alert.setHeaderText("Votre formulaire est bien structuré");
		alert.setContentText("Les données sont collectées correctement. Vous pouvez maintenant brancher la persistance métier.");
		alert.showAndWait();
		clearForm();
		MainFX.switchToAuthLandingView();
	}

	private boolean validateCurrentStep() {
		switch (currentStep) {
			case 1:
				return validateStepOne();
			case 2:
				return validateStepTwo();
			case 3:
				return validateStepThree();
			default:
				return true;
		}
	}

	private boolean validateStepOne() {
		if (isBlank(nomField.getText())) return failValidation("Veuillez renseigner le nom.", nomField);
		if (isBlank(prenomField.getText())) return failValidation("Veuillez renseigner le prénom.", prenomField);
		if (dateNaissancePicker.getValue() == null) return failValidation("Veuillez sélectionner une date de naissance.", dateNaissancePicker);
		if (isBlank(phoneField.getText())) return failValidation("Veuillez renseigner le numéro de téléphone.", phoneField);
		if (isBlank(villeField.getText())) return failValidation("Veuillez renseigner la ville.", villeField);
		return true;
	}

	private boolean validateStepTwo() {
		if (isBlank(emailField.getText()) || !EMAIL_PATTERN.matcher(emailField.getText().trim()).matches()) {
			return failValidation("Veuillez saisir une adresse e-mail valide.", emailField);
		}
		if (isBlank(passwordField.getText()) || passwordField.getText().length() < 8) {
			return failValidation("Le mot de passe doit contenir au moins 8 caractères.", passwordField);
		}
		if (!passwordField.getText().equals(confirmPasswordField.getText())) {
			return failValidation("La confirmation du mot de passe est incorrecte.", confirmPasswordField);
		}
		return true;
	}

	private boolean validateStepThree() {
		if (roleToggleGroup.getSelectedToggle() == null) return failValidation("Veuillez sélectionner un rôle.", null);
		if (isBlank(biographieArea.getText())) return failValidation("Veuillez ajouter une biographie.", biographieArea);

		String role = getSelectedRole();
		if (ROLE_AMATEUR.equalsIgnoreCase(role) && centreInteretComboBox.getValue() == null) {
			return failValidation("Veuillez choisir un centre d'intérêt.", centreInteretComboBox);
		}
		if (ROLE_ARTISTE.equalsIgnoreCase(role) && specialiteComboBox.getValue() == null) {
			return failValidation("Veuillez choisir une spécialité.", specialiteComboBox);
		}
		return true;
	}

	private boolean failValidation(String message, Node focus) {
		setMessage(message, true);
		if (focus != null) focus.requestFocus();
		return false;
	}

	private void showStep(int step, boolean animate) {
		currentStep = step;
		stepOnePane.setVisible(step == 1); stepOnePane.setManaged(step == 1);
		stepTwoPane.setVisible(step == 2); stepTwoPane.setManaged(step == 2);
		stepThreePane.setVisible(step == 3); stepThreePane.setManaged(step == 3);
		if (animate) {
			fadeIn(step == 1 ? stepOnePane : step == 2 ? stepTwoPane : stepThreePane);
		}
		previousButton.setText(step == 1 ? "Retour à l'accueil" : "Précédent");
		nextButton.setVisible(step < 3); nextButton.setManaged(step < 3);
		submitButton.setVisible(step == 3); submitButton.setManaged(step == 3);
		updateStepper();
		updateRoleHints();
	}

	private void updateStepper() {
		setBadgeState(stepOneBadge, currentStep == 1, currentStep > 1);
		setBadgeState(stepTwoBadge, currentStep == 2, currentStep > 2);
		setBadgeState(stepThreeBadge, currentStep == 3, false);
	}

	private void setBadgeState(Label badge, boolean active, boolean done) {
		badge.getStyleClass().removeAll("active", "done");
		if (active) badge.getStyleClass().add("active");
		else if (done) badge.getStyleClass().add("done");
	}

	private void updateRoleHints() {
		String role = getSelectedRole();
		boolean isAmateur = ROLE_AMATEUR.equalsIgnoreCase(role);
		boolean isArtiste = ROLE_ARTISTE.equalsIgnoreCase(role);
		boolean isAdmin = ROLE_ADMIN.equalsIgnoreCase(role);

		specialiteContainer.setVisible(isArtiste);
		specialiteContainer.setManaged(isArtiste);
		centreInteretContainer.setVisible(isAmateur);
		centreInteretContainer.setManaged(isAmateur);

		if (!isArtiste) {
			specialiteComboBox.setValue(null);
		}
		if (!isAmateur) {
			centreInteretComboBox.setValue(null);
		}
		if (isAdmin) {
			specialiteComboBox.setValue(null);
			centreInteretComboBox.setValue(null);
		}
	}

	private String getSelectedRole() {
		Toggle selected = roleToggleGroup.getSelectedToggle();
		return selected instanceof ToggleButton ? ((ToggleButton) selected).getText() : "";
	}

	private User buildUser() {
		draftUser.setNom(trim(nomField.getText()));
		draftUser.setPrenom(trim(prenomField.getText()));
		draftUser.setDateNaissance(dateNaissancePicker.getValue());
		draftUser.setNumTel(trim(phoneField.getText()));
		draftUser.setVille(trim(villeField.getText()));
		draftUser.setEmail(trim(emailField.getText()));
		draftUser.setMdp(passwordField.getText());
		draftUser.setRole(getSelectedRole());
		draftUser.setSpecialite(ROLE_ARTISTE.equalsIgnoreCase(getSelectedRole()) ? trim(specialiteComboBox.getValue()) : null);
		draftUser.setCentreInteret(ROLE_AMATEUR.equalsIgnoreCase(getSelectedRole()) ? trim(centreInteretComboBox.getValue()) : null);
		draftUser.setBiographie(trim(biographieArea.getText()));
		draftUser.setPhotoReferencePath(selectedPhotoPath);
		draftUser.setPhotoProfil(selectedPhotoPath);
		draftUser.setDateInscription(LocalDate.now());
		draftUser.setStatut("pending");
		draftUser.setAge(dateNaissancePicker.getValue() == null ? null : Period.between(dateNaissancePicker.getValue(), LocalDate.now()).getYears());
		return draftUser;
	}

	private void clearForm() {
		nomField.clear(); prenomField.clear(); dateNaissancePicker.setValue(null); phoneField.clear(); villeField.clear();
		emailField.clear(); passwordField.clear(); confirmPasswordField.clear(); specialiteComboBox.setValue(null); centreInteretComboBox.setValue(null);
		biographieArea.clear(); selectedPhotoPath = null; photoPathLabel.setText("Sélectionnez une image pour votre profil.");
		photoPlaceholderLabel.setVisible(true); photoPlaceholderLabel.setManaged(true);
		if (!roleToggleGroup.getToggles().isEmpty()) roleToggleGroup.selectToggle(roleToggleGroup.getToggles().get(0));
		showStep(1, false);
	}

	private void fadeIn(Node node) {
		node.setOpacity(0.0);
		FadeTransition transition = new FadeTransition(Duration.millis(180), node);
		transition.setFromValue(0.0);
		transition.setToValue(1.0);
		transition.play();
	}

	private void setMessage(String message, boolean error) {
		messageLabel.setText(message);
		messageLabel.getStyleClass().removeAll("error", "success");
		messageLabel.getStyleClass().add(error ? "error" : "success");
	}

	private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
	private String trim(String value) { return value == null ? null : value.trim(); }
}



