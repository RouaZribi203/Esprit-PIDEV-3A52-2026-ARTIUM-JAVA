package controllers;

import javafx.fxml.FXML;

public class AuthLandingController {

	@FXML
	private void onCreateAccount() {
		MainFX.switchToRegistrationView();
	}

	@FXML
	private void onLoginClick() {
		MainFX.switchToAuthLandingView();
	}

	@FXML
	private void onHelpClick() {
		MainFX.switchToAuthLandingView();
	}
}

