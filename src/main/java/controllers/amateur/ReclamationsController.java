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
import javafx.stage.FileChooser;
import utils.UserSession;

import java.io.File;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.awt.Desktop;

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
	@FXML
	private Label selectedAttachmentLabel;

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
	private static final long MAX_ATTACHMENT_SIZE_BYTES = 5 * 1024 * 1024;
	private String selectedTypeFilter = null;
	private File selectedAttachmentFile;

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
		clearAttachmentSelection();
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
		clearAttachmentSelection();
		clearSendValidation();
	}

	@FXML
	private void onChooseAttachment(ActionEvent event) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Choisir une piece jointe");
		chooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter("Images et PDF", "*.png", "*.jpg", "*.jpeg", "*.pdf")
		);

		File chosen = chooser.showOpenDialog(descriptionArea == null || descriptionArea.getScene() == null
				? null
				: descriptionArea.getScene().getWindow());
		if (chosen == null) {
			return;
		}

		String error = validateAttachment(chosen);
		if (error != null) {
			showSendValidation(error);
			return;
		}

		selectedAttachmentFile = chosen;
		updateAttachmentLabel();
		clearSendValidation();
	}

	@FXML
	private void onClearAttachment(ActionEvent event) {
		clearAttachmentSelection();
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

		if (selectedAttachmentFile != null) {
			String attachmentError = validateAttachment(selectedAttachmentFile);
			if (attachmentError != null) {
				showSendValidation(attachmentError);
				return;
			}
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
		r.setFileName(selectedAttachmentFile == null ? null : selectedAttachmentFile.getAbsolutePath());
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

		MenuItem viewDetails = new MenuItem("Voir détails");
		MenuItem viewReplies = new MenuItem("Voir réponses");
		MenuItem edit = new MenuItem("Modifier");
		MenuItem delete = new MenuItem("Supprimer");
		menu.getItems().addAll(viewDetails, viewReplies, edit, new SeparatorMenuItem(), delete);

		viewDetails.setOnAction(e -> onViewDetails(r));
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

	private void onViewDetails(Reclamation r) {
		Dialog<ButtonType> dialog = new Dialog<>();
		dialog.setTitle("Détails de la réclamation");
		dialog.setHeaderText("Réclamation #" + (r.getId() == null ? "-" : r.getId()));

		VBox content = new VBox(10);
		content.setPadding(new javafx.geometry.Insets(15));
		content.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5;");

		// ID
		HBox idBox = createDetailRow("ID", String.valueOf(r.getId() == null ? "-" : r.getId()));
		content.getChildren().add(idBox);

		// Type
		HBox typeBox = createDetailRow("Type", r.getType() == null ? "-" : r.getType());
		content.getChildren().add(typeBox);

		// Statut
		HBox statutBox = createDetailRow("Statut", r.getStatut() == null ? "-" : r.getStatut());
		content.getChildren().add(statutBox);

		// Date création
		String dateCreation = r.getDateCreation() == null ? "-" : r.getDateCreation().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.FRENCH));
		HBox dateCreationBox = createDetailRow("Date de création", dateCreation);
		content.getChildren().add(dateCreationBox);

		// Date mise à jour
		String dateUpdate = r.getUpdatedAt() == null ? "-" : r.getUpdatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.FRENCH));
		HBox dateUpdateBox = createDetailRow("Dernière modification", dateUpdate);
		content.getChildren().add(dateUpdateBox);

		// Fichier joint avec visualisation
		if (r.getFileName() != null && !r.getFileName().isBlank()) {
			File attachmentFile = new File(r.getFileName());
			if (attachmentFile.exists()) {
				VBox fileBox = new VBox(5);
				Label fileLabel = new Label("Pièce jointe");
				fileLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
				fileBox.getChildren().add(fileLabel);

				String fileName = attachmentFile.getName().toLowerCase(Locale.ROOT);
				
				// Pour les images : affichage direct
				if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
					try {
						Image image = new Image(attachmentFile.toURI().toString());
						ImageView imageView = new ImageView(image);
						imageView.setPreserveRatio(true);
						imageView.setFitWidth(300);
						imageView.setFitHeight(300);
						fileBox.getChildren().add(imageView);
					} catch (Exception e) {
						fileBox.getChildren().add(new Label("Erreur lors du chargement de l'image"));
					}
				} 
				// Pour les PDFs : bouton d'ouverture
				else if (fileName.endsWith(".pdf")) {
					HBox pdfBox = new HBox(10);
					Label pdfNameLabel = new Label(attachmentFile.getName());
					pdfNameLabel.setStyle("-fx-text-fill: #333;");
					
					Button openButton = new Button("Ouvrir le PDF");
					openButton.setStyle("-fx-padding: 8; -fx-font-size: 11;");
					openButton.setOnAction(e -> openFile(attachmentFile));
					
					pdfBox.getChildren().addAll(pdfNameLabel, openButton);
					fileBox.getChildren().add(pdfBox);
				}
				
				content.getChildren().add(fileBox);
			} else {
				HBox fileBox = createDetailRow("Pièce jointe", "Fichier non trouvé : " + r.getFileName());
				content.getChildren().add(fileBox);
			}
		} else {
			HBox fileBox = createDetailRow("Pièce jointe", "Aucun");
			content.getChildren().add(fileBox);
		}

		// Description (full text)
		VBox descBox = new VBox(5);
		Label descLabel = new Label("Description");
		descLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
		TextArea descArea = new TextArea(r.getTexte() == null ? "" : r.getTexte());
		descArea.setWrapText(true);
		descArea.setPrefRowCount(6);
		descArea.setEditable(false);
		descArea.setStyle("-fx-control-inner-background: #f5f5f5; -fx-text-fill: #333;");
		descBox.getChildren().addAll(descLabel, descArea);
		VBox.setVgrow(descArea, Priority.ALWAYS);
		content.getChildren().add(descBox);

		ScrollPane scrollPane = new ScrollPane(content);
		scrollPane.setFitToWidth(true);
		scrollPane.setPrefHeight(500);

		dialog.getDialogPane().setContent(scrollPane);
		dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
		dialog.showAndWait();
	}

	private void openFile(File file) {
		try {
			if (Desktop.isDesktopSupported()) {
				Desktop.getDesktop().open(file);
			} else {
				showError("Erreur", "Impossible d'ouvrir le fichier sur ce système.");
			}
		} catch (Exception e) {
			showError("Erreur", "Impossible d'ouvrir le fichier: " + e.getMessage());
		}
	}

	private HBox createDetailRow(String label, String value) {
		HBox box = new HBox(10);
		box.setStyle("-fx-padding: 8; -fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0;");

		Label labelNode = new Label(label + ":");
		labelNode.setStyle("-fx-font-weight: bold; -fx-min-width: 150; -fx-text-fill: #666;");
		Label valueNode = new Label(value == null ? "-" : value);
		valueNode.setStyle("-fx-text-fill: #333; -fx-wrap-text: true;");
		valueNode.setWrapText(true);

		box.getChildren().addAll(labelNode, valueNode);
		HBox.setHgrow(valueNode, Priority.ALWAYS);
		return box;
	}

	private void onEditReclamation(Reclamation r) {
		Dialog<ButtonType> dialog = new Dialog<>();
		dialog.setTitle("Modifier réclamation");
		dialog.setHeaderText("Modifier la réclamation #" + (r.getId() == null ? "-" : r.getId()));

		VBox content = new VBox(10);
		content.setPadding(new javafx.geometry.Insets(15));

		// Description
		VBox descBox = new VBox(5);
		Label descLabel = new Label("Description");
		descLabel.setStyle("-fx-font-weight: bold;");
		TextArea descArea = new TextArea(r.getTexte() == null ? "" : r.getTexte());
		descArea.setWrapText(true);
		descArea.setPrefRowCount(8);
		descArea.setStyle("-fx-font-size: 12;");
		descBox.getChildren().addAll(descLabel, descArea);
		VBox.setVgrow(descArea, Priority.ALWAYS);
		content.getChildren().add(descBox);

		// Séparateur visuel
		Separator separator = new Separator();
		content.getChildren().add(separator);

		// Pièce jointe
		VBox attachmentBox = new VBox(8);
		Label attachmentLabel = new Label("Pièce jointe (image ou PDF)");
		attachmentLabel.setStyle("-fx-font-weight: bold;");

		File currentAttachment = r.getFileName() != null && !r.getFileName().isBlank() ? new File(r.getFileName()) : null;
		Label selectedAttachmentLabel = new Label(
			currentAttachment != null && currentAttachment.exists()
				? "Fichier actuel: " + currentAttachment.getName()
				: "Aucun fichier"
		);
		selectedAttachmentLabel.setStyle("-fx-text-fill: #666;");

		HBox attachmentButtonBox = new HBox(8);
		attachmentButtonBox.setAlignment(Pos.CENTER_LEFT);

		Button chooseButton = new Button("Choisir/Modifier un fichier");
		chooseButton.setStyle("-fx-padding: 8;");

		Button removeButton = new Button("Retirer la pièce jointe");
		removeButton.setStyle("-fx-padding: 8;");
		removeButton.setDisable(currentAttachment == null || !currentAttachment.exists());

		// État pour le fichier sélectionné lors de l'édition
		final File[] editingAttachmentFile = {currentAttachment};

		chooseButton.setOnAction(e -> {
			FileChooser chooser = new FileChooser();
			chooser.setTitle("Choisir une pièce jointe");
			chooser.getExtensionFilters().add(
				new FileChooser.ExtensionFilter("Images et PDF", "*.png", "*.jpg", "*.jpeg", "*.pdf")
			);
			
			File chosen = chooser.showOpenDialog(dialog.getOwner());
			if (chosen != null) {
				String error = validateAttachment(chosen);
				if (error != null) {
					showWarning("Fichier invalide", error);
				} else {
					editingAttachmentFile[0] = chosen;
					selectedAttachmentLabel.setText("Nouveau fichier: " + chosen.getName());
					removeButton.setDisable(false);
				}
			}
		});

		removeButton.setOnAction(e -> {
			editingAttachmentFile[0] = null;
			selectedAttachmentLabel.setText("Aucun fichier");
			removeButton.setDisable(true);
		});

		attachmentButtonBox.getChildren().addAll(chooseButton, removeButton);
		attachmentBox.getChildren().addAll(attachmentLabel, selectedAttachmentLabel, attachmentButtonBox);
		content.getChildren().add(attachmentBox);

		ScrollPane scrollPane = new ScrollPane(content);
		scrollPane.setFitToWidth(true);
		scrollPane.setPrefHeight(400);

		dialog.getDialogPane().setContent(scrollPane);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

		dialog.showAndWait().ifPresent(bt -> {
			if (bt != ButtonType.OK) return;

			String newTextRaw = descArea.getText() == null ? "" : descArea.getText();
			String newText = newTextRaw.trim();
			String noSpaces = newText.replaceAll("\\s+", "");
			if (newText.isBlank() || noSpaces.length() < MIN_DESCRIPTION_LEN) {
				showWarning("Modification refusée", "Veuillez saisir une description d'au moins 10 caractères.");
				return;
			}
			if (isTooLong(newTextRaw)) {
				showWarning("Modification refusée", "La description ne peut pas depasser " + MAX_DESCRIPTION_LEN + " caracteres.");
				return;
			}

			if (editingAttachmentFile[0] != null) {
				String attachmentError = validateAttachment(editingAttachmentFile[0]);
				if (attachmentError != null) {
					showWarning("Fichier invalide", attachmentError);
					return;
				}
			}

			try {
				r.setTexte(newText);
				r.setFileName(editingAttachmentFile[0] == null ? null : editingAttachmentFile[0].getAbsolutePath());
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

	private void clearAttachmentSelection() {
		selectedAttachmentFile = null;
		updateAttachmentLabel();
	}

	private void updateAttachmentLabel() {
		if (selectedAttachmentLabel == null) {
			return;
		}
		selectedAttachmentLabel.setText(selectedAttachmentFile == null
				? "Aucun fichier selectionne"
				: selectedAttachmentFile.getName());
	}

	private String validateAttachment(File file) {
		if (file == null) {
			return null;
		}
		if (!file.exists() || !file.isFile()) {
			return "Le fichier selectionne est introuvable.";
		}
		String fileName = file.getName().toLowerCase(Locale.ROOT);
		boolean allowed = fileName.endsWith(".png")
				|| fileName.endsWith(".jpg")
				|| fileName.endsWith(".jpeg")
				|| fileName.endsWith(".pdf");
		if (!allowed) {
			return "Formats autorises: PNG, JPG, JPEG, PDF.";
		}
		if (file.length() > MAX_ATTACHMENT_SIZE_BYTES) {
			return "Le fichier ne doit pas depasser 5 Mo.";
		}
		return null;
	}
}

