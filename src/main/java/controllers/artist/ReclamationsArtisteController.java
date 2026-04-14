package controllers.artist;

import Services.ReclamationService;
import entities.Reclamation;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.sql.SQLDataException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class ReclamationsArtisteController implements Initializable {

	@FXML
	private TabPane tabs;

	@FXML
	private ComboBox<String> typeCombo;

	@FXML
	private TextArea descriptionArea;

	@FXML
	private VBox myReclamationsContainer;

	@FXML
	private Label emptyMyReclamationsLabel;

	private final ReclamationService reclamationService = new ReclamationService();

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// Valeurs de démonstration (à remplacer par des valeurs venant de la BD)
		typeCombo.setItems(FXCollections.observableArrayList(
				"Paiement",
				"Oeuvre",
				"Evenement",
				"Compte",
				"Autre"
		));

		refreshMyReclamationsEmptyState();
	}

	@FXML
	private void onReset(ActionEvent event) {
		typeCombo.getSelectionModel().clearSelection();
		descriptionArea.clear();
	}

	@FXML
	private void onSend(ActionEvent event) {
		String type = typeCombo.getValue();
		String description = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();

		if (type == null || type.isBlank()) {
			showWarning("Champs obligatoire", "Veuillez sélectionner un type.");
			return;
		}
		if (description.isBlank()) {
			showWarning("Champs obligatoire", "Veuillez saisir une description.");
			return;
		}

		// TODO: remplacer par l'utilisateur connecté.
		// En attendant, on met 1 pour éviter un INSERT avec user_id null.
		int userId = 1;

		LocalDateTime now = LocalDateTime.now();
		Reclamation r = new Reclamation();
		r.setTexte(description);
		r.setType(type);
		r.setStatut("Non traite");
		r.setDateCreation(now);
		r.setUpdatedAt(now);
		r.setFileName(null);
		r.setUserId(userId);

		try {
			reclamationService.add(r);
			showInfo("Réclamation envoyée", "Votre réclamation a été envoyée avec succès.");
			onReset(event);

			// Passer sur l'onglet "Mes Réclamations" après envoi (optionnel mais pratique)
			if (tabs != null) {
				for (Tab t : tabs.getTabs()) {
					if (t != null && "Mes Réclamations".equals(t.getText())) {
						tabs.getSelectionModel().select(t);
						break;
					}
				}
			}
		} catch (SQLDataException e) {
			showError("Envoi impossible", e.getMessage());
		} catch (Exception e) {
			showError("Envoi impossible", "Erreur inattendue: " + e.getMessage());
		}
	}

	private void refreshMyReclamationsEmptyState() {
		boolean empty = myReclamationsContainer == null || myReclamationsContainer.getChildren().isEmpty();
		emptyMyReclamationsLabel.setVisible(empty);
		emptyMyReclamationsLabel.setManaged(empty);
	}

	private void showInfo(String header, String message) {
		Alert a = new Alert(Alert.AlertType.INFORMATION);
		a.setTitle("Info");
		a.setHeaderText(header);
		a.setContentText(message);
		a.showAndWait();
	}

	private void showWarning(String header, String message) {
		Alert a = new Alert(Alert.AlertType.WARNING);
		a.setTitle("Attention");
		a.setHeaderText(header);
		a.setContentText(message);
		a.showAndWait();
	}

	private void showError(String header, String message) {
		Alert a = new Alert(Alert.AlertType.ERROR);
		a.setTitle("Erreur");
		a.setHeaderText(header);
		a.setContentText(message);
		a.showAndWait();
	}
}

