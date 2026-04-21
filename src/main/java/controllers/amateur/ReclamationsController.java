package controllers.amateur;

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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;
import utils.UserSession;

import java.net.URL;
import java.sql.SQLDataException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Amateur reclamations page: same behavior as the artist reclamation page
 * (send + list + search/filter + view replies + edit/delete).
 */
public class ReclamationsController implements Initializable {

	@FXML
	private TabPane tabs;

	// Send tab
	@FXML
	private ComboBox<String> typeCombo;

	@FXML
	private TextArea descriptionArea;

	@FXML
	private Label sendValidationLabel;

	// My reclamations tab
	@FXML
	private VBox myReclamationsContainer;

	@FXML
	private Label emptyMyReclamationsLabel;

	@FXML
	private TextField mySearchField;

	@FXML
	private ComboBox<String> myStatutFilter;
	@FXML
	private Button typeFilterAll;
	@FXML
	private Button typeFilterPayment;
	@FXML
	private Button typeFilterWork;
	@FXML
	private Button typeFilterEvent;
	@FXML
	private Button typeFilterAccount;

	private final ReclamationService reclamationService = new ReclamationService();
	private final List<Reclamation> myAll = new ArrayList<>();
	private Integer currentUserId;

	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRENCH);
	private static final int MIN_DESCRIPTION_LEN = 10;
	private static final int MAX_DESCRIPTION_LEN = 500;
	private String selectedTypeFilter = null;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		currentUserId = UserSession.getCurrentUserId();
		if (currentUserId == null) {
			showError("Session", "Session utilisateur introuvable. Veuillez vous reconnecter.");
			return;
		}

		// Types
		if (typeCombo != null) {
			typeCombo.setItems(FXCollections.observableArrayList(
					"Paiement",
					"Oeuvre",
					"Evenement",
					"Compte",
					"Autre"
			));
		}

		refreshMyReclamationsEmptyState();

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
		updateTypeFilterButtons(typeFilterAll);

		refreshMyReclamations();
	}

	@FXML
	private void onReset(ActionEvent event) {
		if (typeCombo != null) {
			typeCombo.getSelectionModel().clearSelection();
		}
		if (descriptionArea != null) {
			descriptionArea.clear();
		}
		clearSendValidation();
	}

	@FXML
	private void onSend(ActionEvent event) {
		clearSendValidation();
		String type = typeCombo == null ? null : typeCombo.getValue();
		String descriptionRaw = descriptionArea == null || descriptionArea.getText() == null ? "" : descriptionArea.getText();
		String description = descriptionRaw.trim();

		if (type == null || type.isBlank()) {
			showSendValidation("Veuillez selectionner un type.");
			return;
		}
		if (isBlankOrTooShort(description)) {
			showSendValidation("La reclamation doit contenir au moins " + MIN_DESCRIPTION_LEN + " caracteres.");
			return;
		}
		if (isTooLong(descriptionRaw)) {
			showSendValidation("La description ne peut pas depasser " + MAX_DESCRIPTION_LEN + " caracteres.");
			return;
		}

		if (currentUserId == null) {
			showError("Session", "Session utilisateur introuvable. Veuillez vous reconnecter.");
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

			// Switch to "Mes Réclamations" after send
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
			if (currentUserId == null) {
				myAll.clear();
				renderMyCards(List.of());
				return;
			}
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
				.filter(r -> {
					if (selectedTypeFilter == null) return true;
					return normalize(r.getType()).equals(normalize(selectedTypeFilter));
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
		if (myReclamationsContainer == null) {
			return;
		}

		myReclamationsContainer.getChildren().clear();
		for (Reclamation r : list) {
			myReclamationsContainer.getChildren().add(buildMyCard(r));
		}
		refreshMyReclamationsEmptyState();
	}

	private VBox buildMyCard(Reclamation r) {
		VBox card = new VBox(10);
		card.getStyleClass().add("my-reclamation-card");
		card.setMaxWidth(Double.MAX_VALUE);

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

		Label body = new Label(truncate(r.getTexte(), 120));
		body.getStyleClass().add("my-reclamation-meta");
		body.setWrapText(true);

		card.getChildren().addAll(top, meta, body);
		return card;
	}

	private void initCardMenu(Button dotsButton, Reclamation r) {
		ContextMenu menu = new ContextMenu();

		MenuItem viewReplies = new MenuItem("Voir réponses");
		MenuItem edit = new MenuItem("Modifier");
		MenuItem delete = new MenuItem("Supprimer");
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
			controller.setReadOnly(true);

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
			if (result != ButtonType.OK) return;
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
		if (emptyMyReclamationsLabel == null) return;
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

	private static boolean isBlankOrTooShort(String value) {
		String v = value == null ? "" : value.trim();
		if (v.isEmpty()) return true;
		// on compte les caractères hors espaces pour éviter "          "
		String noSpaces = v.replaceAll("\\s+", "");
		return noSpaces.length() < MIN_DESCRIPTION_LEN;
	}

	private static boolean isTooLong(String value) {
		String v = value == null ? "" : value.trim();
		return v.length() > MAX_DESCRIPTION_LEN;
	}

	private void showSendValidation(String message) {
		if (sendValidationLabel == null) {
			showWarning("Attention", message);
			return;
		}
		sendValidationLabel.setText(message);
		sendValidationLabel.setVisible(true);
		sendValidationLabel.setManaged(true);
	}

	@FXML
	private void onTypeFilterAll(ActionEvent event) {
		selectedTypeFilter = null;
		updateTypeFilterButtons(typeFilterAll);
		applyMyFilters();
	}

	@FXML
	private void onTypeFilterPayment(ActionEvent event) {
		selectedTypeFilter = "Paiement";
		updateTypeFilterButtons(typeFilterPayment);
		applyMyFilters();
	}

	@FXML
	private void onTypeFilterWork(ActionEvent event) {
		selectedTypeFilter = "Oeuvre";
		updateTypeFilterButtons(typeFilterWork);
		applyMyFilters();
	}

	@FXML
	private void onTypeFilterEvent(ActionEvent event) {
		selectedTypeFilter = "Evenement";
		updateTypeFilterButtons(typeFilterEvent);
		applyMyFilters();
	}

	@FXML
	private void onTypeFilterAccount(ActionEvent event) {
		selectedTypeFilter = "Compte";
		updateTypeFilterButtons(typeFilterAccount);
		applyMyFilters();
	}

	private void updateTypeFilterButtons(Button activeButton) {
		if (typeFilterAll != null) typeFilterAll.getStyleClass().remove("type-filter-active");
		if (typeFilterPayment != null) typeFilterPayment.getStyleClass().remove("type-filter-active");
		if (typeFilterWork != null) typeFilterWork.getStyleClass().remove("type-filter-active");
		if (typeFilterEvent != null) typeFilterEvent.getStyleClass().remove("type-filter-active");
		if (typeFilterAccount != null) typeFilterAccount.getStyleClass().remove("type-filter-active");

		if (activeButton != null && !activeButton.getStyleClass().contains("type-filter-active")) {
			activeButton.getStyleClass().add("type-filter-active");
		}
	}

	private void clearSendValidation() {
		if (sendValidationLabel == null) {
			return;
		}
		sendValidationLabel.setText("");
		sendValidationLabel.setVisible(false);
		sendValidationLabel.setManaged(false);
	}
}

