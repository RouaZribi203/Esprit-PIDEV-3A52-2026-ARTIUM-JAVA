package controllers.artist;

import services.ReclamationService;
import entities.Reclamation;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;
import utils.UserSession;

import java.sql.SQLDataException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.text.Normalizer;

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

	@FXML
	private TextField mySearchField;

	@FXML
	private ComboBox<String> myStatutFilter;

	@FXML
	private Label sendValidationLabel;

	private final ReclamationService reclamationService = new ReclamationService();
	private final List<Reclamation> myAll = new ArrayList<>();
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRENCH);
	private Integer currentUserId;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		currentUserId = UserSession.getCurrentUserId();
		if (currentUserId == null) {
			handleMissingSession();
			return;
		}

		// Valeurs de démonstration (à remplacer par des valeurs venant de la BD)
		typeCombo.setItems(FXCollections.observableArrayList(
				"Paiement",
				"Oeuvre",
				"Evenement",
				"Compte",
				"Autre"
		));

		refreshMyReclamationsEmptyState();
		clearSendValidation();

		if (myStatutFilter != null) {
			myStatutFilter.setItems(FXCollections.observableArrayList("Tous", "Traitée", "Non traitée"));
			myStatutFilter.getSelectionModel().selectFirst();
			myStatutFilter.valueProperty().addListener((obs, o, n) -> applyMyFilters());
		}
		if (mySearchField != null) {
			mySearchField.textProperty().addListener((obs, o, n) -> applyMyFilters());
		}
		if (typeCombo != null) {
			typeCombo.valueProperty().addListener((obs, o, n) -> clearSendValidation());
		}
		if (descriptionArea != null) {
			descriptionArea.textProperty().addListener((obs, o, n) -> clearSendValidation());
		}

		refreshMyReclamations();
	}

	private void handleMissingSession() {
		if (myReclamationsContainer != null) {
			myReclamationsContainer.getChildren().clear();
		}
		if (emptyMyReclamationsLabel != null) {
			emptyMyReclamationsLabel.setText("Session utilisateur introuvable. Veuillez vous reconnecter.");
			emptyMyReclamationsLabel.setVisible(true);
			emptyMyReclamationsLabel.setManaged(true);
		}
		if (typeCombo != null) {
			typeCombo.setDisable(true);
		}
		if (descriptionArea != null) {
			descriptionArea.setDisable(true);
		}
	}

	@FXML
	private void onReset(ActionEvent event) {
		typeCombo.getSelectionModel().clearSelection();
		descriptionArea.clear();
		clearSendValidation();
	}

	@FXML
	private void onSend(ActionEvent event) {
		clearSendValidation();
		String type = typeCombo.getValue();
		String description = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();
		String descriptionNoSpaces = description.replaceAll("\\s+", "");

		if (type == null || type.isBlank()) {
			showSendValidation("Veuillez selectionner un type.");
			return;
		}
		if (description.isBlank()) {
			showSendValidation("Veuillez saisir une description.");
			return;
		}
		if (descriptionNoSpaces.length() < 10) {
			showSendValidation("La reclamation doit contenir au moins 10 caracteres.");
			return;
		}

		int userId = currentUserId;

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
			clearSendValidation();
			refreshMyReclamations();

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

	private void refreshMyReclamations() {
		try {
			myAll.clear();
			for (Reclamation r : reclamationService.getAll()) {
				if (r.getUserId() != null && r.getUserId().equals(currentUserId)) {
					myAll.add(r);
				}
			}
			applyMyFilters();
		} catch (SQLDataException e) {
			showError("Chargement impossible", e.getMessage());
		}
	}

	private void applyMyFilters() {
		String q = (mySearchField == null || mySearchField.getText() == null)
				? ""
				: mySearchField.getText().trim().toLowerCase(Locale.ROOT);
		String statutSel = myStatutFilter == null ? "Tous" : myStatutFilter.getValue();
		String statutSelNorm = normalize(statutSel);

		List<Reclamation> filtered = myAll.stream()
				// Recherche appliquée uniquement sur le texte/description
				.filter(r -> q.isEmpty() || contains(r.getTexte(), q))
				.filter(r -> {
					if (statutSelNorm.isEmpty() || statutSelNorm.equals("tous")) return true;
					String s = normalize(r.getStatut());
					boolean isNon = s.contains("non") || s.contains("en cours") || s.contains("pending") || s.contains("non tra") || s.contains("nontra");
					boolean isTraite = !isNon && (s.contains("traite") || s.contains("resolu") || s.contains("resolved") || s.contains("done"));
					if (statutSelNorm.contains("traite") && !statutSelNorm.contains("non")) return isTraite;
					if (statutSelNorm.contains("non")) return isNon;
					return true;
				})
				.sorted((a, b) -> {
					int ia = a.getId() == null ? 0 : a.getId();
					int ib = b.getId() == null ? 0 : b.getId();
					return Integer.compare(ib, ia);
				})
				.toList();

		renderMyCards(filtered);
	}

	private void renderMyCards(List<Reclamation> list) {
		if (myReclamationsContainer == null) return;
		myReclamationsContainer.getChildren().clear();

		for (Reclamation r : list) {
			myReclamationsContainer.getChildren().add(buildMyCard(r));
		}

		refreshMyReclamationsEmptyState();
	}

	private VBox buildMyCard(Reclamation r) {
		// Card root
		VBox card = new VBox(10);
		card.getStyleClass().add("my-reclamation-card");
		card.setMaxWidth(Double.MAX_VALUE);

		// Top row: badge + title + dots
		HBox top = new HBox(10);
		top.setAlignment(Pos.CENTER_LEFT);

		Label badge = new Label(r.getStatut() == null ? "" : r.getStatut());
		badge.getStyleClass().addAll("status-badge", computeStatusClass(r.getStatut()));

		Label title = new Label(truncate(r.getTexte(), 32));
		title.getStyleClass().add("my-reclamation-title");
		HBox.setHgrow(title, Priority.ALWAYS);
		title.setMaxWidth(Double.MAX_VALUE);

		Button dots = new Button("...");
		dots.setMnemonicParsing(false);
		dots.getStyleClass().add("dots-button");
		initCardMenu(dots, r);

		top.getChildren().addAll(badge, title, dots);

		// Second row: type + date
		HBox meta = new HBox(16);
		meta.setAlignment(Pos.CENTER_LEFT);

		Label type = new Label("Type: " + (r.getType() == null ? "-" : r.getType()));
		type.getStyleClass().add("my-reclamation-meta");

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		String dateText = r.getDateCreation() != null ? r.getDateCreation().format(DATE_FMT) : "-";
		Label date = new Label("Créée le: " + dateText);
		date.getStyleClass().add("my-reclamation-meta");

		meta.getChildren().addAll(type, spacer, date);

		// Body (preview)
		Label body = new Label(truncate(r.getTexte(), 120));
		body.getStyleClass().add("my-reclamation-meta");
		body.setWrapText(true);

		card.getChildren().addAll(top, meta, body);
		return card;
	}

	private void initCardMenu(Button dotsButton, Reclamation r) {
		ContextMenu menu = new ContextMenu();
		menu.getStyleClass().add("reclamation-actions-menu");

		MenuItem viewReplies = new MenuItem("Voir réponses");
		viewReplies.getStyleClass().add("reclamation-actions-view");
		MenuItem edit = new MenuItem("Modifier");
		edit.getStyleClass().add("reclamation-actions-edit");
		MenuItem delete = new MenuItem("Supprimer");
		delete.getStyleClass().add("reclamation-actions-delete");
		menu.getItems().addAll(viewReplies, edit, new SeparatorMenuItem(), delete);

		viewReplies.setOnAction(e -> onViewReplies(r));
		edit.setOnAction(e -> onEditReclamation(r));
		delete.setOnAction(e -> onDeleteReclamation(r));

		dotsButton.setOnAction(e -> menu.show(dotsButton, javafx.geometry.Side.BOTTOM, 0, 0));
	}

	private void onViewReplies(Reclamation r) {
		try {
			FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/views/pages/reclamation_reply_dialog.fxml")));
			Parent root = loader.load();

			controllers.pages.reclamations.ReclamationReplyDialogController controller = loader.getController();
			controller.setReclamation(r);
			controller.setReadOnly(true); // utilisateur: consultation uniquement

			Stage stage = new Stage();
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("Réclamation #" + (r.getId() == null ? "-" : r.getId()));

			Scene scene = new Scene(root);
			URL dialogCss = getClass().getResource("/views/styles/reclamation-reply-dialog.css");
			if (dialogCss != null) scene.getStylesheets().add(dialogCss.toExternalForm());
			URL appCss = getClass().getResource("/views/styles/dashboard.css");
			if (appCss != null) scene.getStylesheets().add(appCss.toExternalForm());

			stage.setScene(scene);
			stage.showAndWait();
		} catch (Exception ex) {
			showError("Ouverture impossible", ex.getMessage());
		}
	}

	private void onEditReclamation(Reclamation r) {
		TextArea area = new TextArea(r.getTexte() == null ? "" : r.getTexte());
		area.setWrapText(true);
		area.setPrefRowCount(8);

		Dialog<ButtonType> dialog = new Dialog<>();
		dialog.setTitle("Modifier réclamation");
		dialog.setHeaderText("Modifier la description");
		dialog.getDialogPane().setContent(area);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

		dialog.showAndWait().ifPresent(bt -> {
			if (bt != ButtonType.OK) return;
			String newText = area.getText() == null ? "" : area.getText().trim();
			String noSpaces = newText.replaceAll("\\s+", "");
			if (newText.isBlank() || noSpaces.length() < 10) {
				showWarning("Modification refusée", "Veuillez saisir une description d'au moins 10 caractères.");
				return;
			}
			try {
				r.setTexte(newText);
				r.setUpdatedAt(LocalDateTime.now());
				reclamationService.update(r);
				refreshMyReclamations();
				showInfo("Modification", "Réclamation modifiée avec succès.");
			} catch (SQLDataException ex) {
				showError("Modification impossible", ex.getMessage());
			} catch (Exception ex) {
				showError("Modification impossible", "Erreur inattendue: " + ex.getMessage());
			}
		});
	}

	private void onDeleteReclamation(Reclamation r) {
		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
		confirm.setTitle("Confirmation");
		confirm.setHeaderText("Supprimer cette réclamation ?");
		confirm.setContentText("Cette action est irréversible.");
		confirm.showAndWait().ifPresent(result -> {
			if (result != javafx.scene.control.ButtonType.OK) return;
			try {
				reclamationService.delete(r);
				refreshMyReclamations();
				showInfo("Suppression", "Réclamation supprimée.");
			} catch (SQLDataException ex) {
				showError("Suppression impossible", ex.getMessage());
			} catch (Exception ex) {
				showError("Suppression impossible", "Erreur inattendue: " + ex.getMessage());
			}
		});
	}

	private String computeStatusClass(String statut) {
		String s = normalize(statut);
		boolean isNon = s.contains("non") || s.contains("en cours") || s.contains("pending") || s.contains("non tra") || s.contains("nontra");
		boolean isTraite = !isNon && (s.contains("traite") || s.contains("resolu") || s.contains("resolved") || s.contains("done"));
		return isTraite ? "traite" : "nontraite";
	}

	private static String truncate(String s, int max) {
		if (s == null) return "";
		String t = s.trim();
		if (t.length() <= max) return t;
		return t.substring(0, Math.max(0, max - 3)) + "...";
	}

	private boolean contains(String value, String search) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(search);
	}

	private static String normalize(String value) {
		String s = value == null ? "" : value;
		s = s.toLowerCase(Locale.ROOT).replace("_", " ").replace("-", " ");
		s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		while (s.contains("  ")) s = s.replace("  ", " ");
		return s.trim();
	}

	private void refreshMyReclamationsEmptyState() {
		boolean empty = myReclamationsContainer == null || myReclamationsContainer.getChildren().isEmpty();
		emptyMyReclamationsLabel.setVisible(empty);
		emptyMyReclamationsLabel.setManaged(empty);
	}

	private void showSendValidation(String message) {
		if (sendValidationLabel == null) {
			return;
		}
		sendValidationLabel.setText(message);
		sendValidationLabel.setVisible(true);
		sendValidationLabel.setManaged(true);
	}

	private void clearSendValidation() {
		if (sendValidationLabel == null) {
			return;
		}
		sendValidationLabel.setText("");
		sendValidationLabel.setVisible(false);
		sendValidationLabel.setManaged(false);
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

