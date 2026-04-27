package controllers.amateur;

import controllers.MainFX;
import entities.Evenement;
import entities.Galerie;
import entities.Ticket;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import services.TicketService;
import services.TicketPdfService;

import java.io.File;
import java.sql.SQLDataException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import services.GalerieService;

public class EventDetailController {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm");
	private static final DateTimeFormatter PURCHASE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

	@FXML
	private ImageView coverImageView;

	@FXML
	private Label titleLabel;

	@FXML
	private Label typeLabel;

	@FXML
	private Label statusLabel;

	@FXML
	private Label dateDebutLabel;

	@FXML
	private Label dateFinLabel;

	@FXML
	private Label priceLabel;

	@FXML
	private Label capacityLabel;

	@FXML
	private Label galleryLabel;

	@FXML
	private Label descriptionLabel;

	@FXML
	private Label metaLabel;

	@FXML
	private Button buyTicketButton;

	@FXML
	private Button backButton;

	@FXML
	private VBox ticketsContainer;

	@FXML
	private Label ticketsEmptyLabel;

	@FXML
	private Label ticketsCountLabel;

	private final TicketService ticketService = new TicketService();
	private final TicketPdfService ticketPdfService = new TicketPdfService();
	private final GalerieService galerieService = new GalerieService();
	private Evenement event;
	private Consumer<Ticket> purchaseHandler;
	private Runnable backHandler;

	public void setEvent(Evenement event) {
		this.event = event;
		if (event == null) {
			buyTicketButton.setDisable(true);
			if (backButton != null) {
				backButton.setDisable(false);
			}
			renderPurchasedTickets(List.of());
			return;
		}

		boolean purchasable = event.getStatut() == null || !"Annulé".equalsIgnoreCase(event.getStatut().trim());
		Integer currentUserId = resolveCurrentUserId();
		if (currentUserId == null) {
			buyTicketButton.setDisable(true);
			buyTicketButton.setText("Connexion requise");
		} else {
			if (purchasable && event.getDateDebut() != null) {
				purchasable = LocalDateTime.now().isBefore(event.getDateDebut());
			}
			buyTicketButton.setDisable(!purchasable);
			buyTicketButton.setText(purchasable ? "Acheter ticket" : "Ticket indisponible");
		}
		if (backButton != null) {
			backButton.setDisable(false);
		}

		titleLabel.setText(textOrDefault(event.getTitre(), "Evenement"));
		typeLabel.setText(textOrDefault(event.getType(), "Type non precise"));
		statusLabel.setText(textOrDefault(event.getStatut(), "A venir"));
		dateDebutLabel.setText(formatDate(event.getDateDebut()));
		dateFinLabel.setText(formatDate(event.getDateFin()));
		priceLabel.setText(formatPrice(event.getPrixTicket()));
		capacityLabel.setText(formatCapacity(event.getCapaciteMax()));
		galleryLabel.setText(resolveGalleryName(event));
		descriptionLabel.setText(textOrDefault(event.getDescription(), "Aucune description disponible."));
		metaLabel.setText("Date de creation: " + (event.getDateCreation() == null ? "-" : event.getDateCreation().toString()));
		applyImage(event.getImageCouverture());
		loadPurchasedTickets();
	}

	public void setPurchaseHandler(Consumer<Ticket> purchaseHandler) {
		this.purchaseHandler = purchaseHandler;
	}

	public void setBackHandler(Runnable backHandler) {
		this.backHandler = backHandler;
	}

	@FXML
	private void onBuyTicketClick() {
		if (event == null) {
			return;
		}

		Integer currentUserId = resolveCurrentUserId();
		if (currentUserId == null) {
			Alert alert = new Alert(Alert.AlertType.WARNING);
			alert.setTitle("Connexion requise");
			alert.setHeaderText("Vous devez être connecté pour acheter un ticket");
			alert.setContentText("Veuillez vous reconnecter avant de continuer.");
			alert.showAndWait();
			return;
		}

		try {
			Ticket ticket = ticketService.purchaseTicket(event, currentUserId);
			loadPurchasedTickets();
			if (purchaseHandler != null) {
				purchaseHandler.accept(ticket);
			} else {
				Alert alert = new Alert(Alert.AlertType.INFORMATION);
				alert.setTitle("Ticket généré");
				alert.setHeaderText("Votre ticket a été créé avec succès");
				alert.setContentText("Le ticket a été enregistré, mais aucun écran de succès n'est configuré.");
				alert.showAndWait();
			}
		} catch (SQLDataException e) {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("Achat impossible");
			alert.setHeaderText("Le ticket n'a pas pu etre achete");
			alert.setContentText(e.getMessage());
			alert.showAndWait();
		}
	}

	private void loadPurchasedTickets() {
		Integer currentUserId = resolveCurrentUserId();
		if (event == null || event.getId() == null || currentUserId == null) {
			renderPurchasedTickets(List.of());
			return;
		}

		try {
			List<Ticket> tickets = ticketService.getTicketsByEventAndUser(event.getId(), currentUserId);
			renderPurchasedTickets(tickets);
		} catch (SQLDataException e) {
			renderPurchasedTickets(List.of());
			ticketsEmptyLabel.setText("Impossible de charger vos tickets.");
			ticketsEmptyLabel.setVisible(true);
			ticketsEmptyLabel.setManaged(true);
		}
	}

	private void renderPurchasedTickets(List<Ticket> tickets) {
		ticketsContainer.getChildren().clear();
		ticketsCountLabel.setText(tickets.size() + " ticket(s)");

		if (tickets.isEmpty()) {
			ticketsEmptyLabel.setText("Aucun ticket achete pour cet evenement.");
			ticketsEmptyLabel.setVisible(true);
			ticketsEmptyLabel.setManaged(true);
			return;
		}

		ticketsEmptyLabel.setVisible(false);
		ticketsEmptyLabel.setManaged(false);

		for (int i = 0; i < tickets.size(); i++) {
			Ticket ticket = tickets.get(i);
			HBox row = new HBox(10);
			row.getStyleClass().add("ticket-row");

			VBox info = new VBox(3);
			info.getStyleClass().add("ticket-info");
			Label title = new Label("Ticket #" + (i + 1));
			title.getStyleClass().add("ticket-title");
			Label subtitle = new Label("Achat le " + formatPurchaseDate(ticket) + " | Ref: " + resolveReference(ticket));
			subtitle.getStyleClass().add("ticket-subtitle");
			info.getChildren().addAll(title, subtitle);

			HBox actions = new HBox(8);
			actions.getStyleClass().add("ticket-actions");
			Button pdfButton = new Button("Aperçu PDF");
			pdfButton.getStyleClass().add("ticket-action-secondary");
			pdfButton.setOnAction(event -> showTicketPreview(ticket));
			Button downloadButton = new Button("Télécharger PDF");
			downloadButton.getStyleClass().add("ticket-action-primary");
			downloadButton.setOnAction(event -> downloadTicketPdf(ticket));
			actions.getChildren().addAll(pdfButton, downloadButton);

			HBox.setHgrow(info, Priority.ALWAYS);
			row.getChildren().addAll(info, actions);
			ticketsContainer.getChildren().add(row);
		}
	}

	private void showTicketPreview(Ticket ticket) {
		if (event == null) {
			return;
		}

		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Aperçu du ticket");
		alert.setHeaderText("Ticket PDF prêt à être ouvert");
		alert.setContentText("Le PDF va être généré dans un fichier temporaire et ouvert dans votre lecteur PDF par défaut.");
		alert.showAndWait();

		try {
			File previewPdf = ticketPdfService.createPreviewPdf(event, ticket);
			ticketPdfService.openPdf(previewPdf);
		} catch (Exception e) {
			Alert error = new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir le PDF: " + e.getMessage(), ButtonType.OK);
			error.setTitle("Aperçu PDF indisponible");
			error.showAndWait();
		}
	}

	private void downloadTicketPdf(Ticket ticket) {
		if (event == null) {
			return;
		}

		FileChooser chooser = new FileChooser();
		chooser.setTitle("Télécharger le ticket PDF");
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
		chooser.setInitialFileName(ticketPdfService.buildSuggestedFileName(event, ticket));

		Window owner = buyTicketButton == null ? null : buyTicketButton.getScene().getWindow();
		File target = chooser.showSaveDialog(owner);
		if (target == null) {
			return;
		}

		File pdfFile = ensurePdfExtension(target);
		try {
			ticketPdfService.exportTicketPdf(event, ticket, pdfFile);
			Alert info = new Alert(Alert.AlertType.INFORMATION);
			info.setTitle("Ticket téléchargé");
			info.setHeaderText("PDF généré avec succès");
			info.setContentText("Le ticket a été enregistré dans:\n" + pdfFile.getAbsolutePath());
			info.showAndWait();
		} catch (Exception e) {
			Alert error = new Alert(Alert.AlertType.ERROR);
			error.setTitle("Téléchargement impossible");
			error.setHeaderText("Le PDF n'a pas pu être créé");
			error.setContentText(e.getMessage());
			error.showAndWait();
		}
	}

	private File ensurePdfExtension(File target) {
		String name = target.getName().toLowerCase();
		if (name.endsWith(".pdf")) {
			return target;
		}
		File parent = target.getParentFile();
		String baseName = target.getName() + ".pdf";
		return parent == null ? new File(baseName) : new File(parent, baseName);
	}

	@FXML
	private void onBackClick() {
		if (backHandler != null) {
			backHandler.run();
		}
	}

	private void applyImage(String imageSource) {
		if (imageSource == null || imageSource.isBlank()) {
			coverImageView.setImage(null);
			return;
		}
		try {
			Image image;
			if (imageSource.startsWith("http://") || imageSource.startsWith("https://") || imageSource.startsWith("file:")) {
				image = new Image(imageSource, true);
			} else {
				image = new Image(new File(imageSource).toURI().toString(), true);
			}
			coverImageView.setImage(image.isError() ? null : image);
		} catch (Exception e) {
			coverImageView.setImage(null);
		}
	}

	private String formatDate(java.time.LocalDateTime dateTime) {
		return dateTime == null ? "Date non definie" : DATE_FORMATTER.format(dateTime);
	}

	private String formatPrice(Double price) {
		return price == null ? "Prix non defini" : String.format("%.0f TND", price);
	}

	private String formatCapacity(Integer value) {
		return value == null ? "Capacite non definie" : value + " personnes";
	}

	private String textOrDefault(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}

	private String resolveGalleryName(Evenement event) {
		if (event == null || event.getGalerieId() == null) {
			return "Galerie";
		}

		try {
			for (Galerie galerie : galerieService.getAll()) {
				if (galerie != null && event.getGalerieId().equals(galerie.getId())) {
					return textOrDefault(galerie.getNom(), "Galerie");
				}
			}
		} catch (SQLDataException ignored) {
			// Fallback below keeps the UI readable even if the lookup fails.
		}

		return "Galerie";
	}

	private String resolveReference(Ticket ticket) {
		if (ticket.getCodeQr() == null || ticket.getCodeQr().isBlank()) {
			return "-";
		}

		String payload = ticket.getCodeQr();
		int refIndex = payload.indexOf("|ref=");
		if (refIndex >= 0 && refIndex + 5 < payload.length()) {
			return payload.substring(refIndex + 5);
		}
		return payload;
	}

	private String formatPurchaseDate(Ticket ticket) {
		return ticket.getDateAchat() == null ? "-" : PURCHASE_DATE_FORMATTER.format(ticket.getDateAchat());
	}

	private Integer resolveCurrentUserId() {
		if (MainFX.getAuthenticatedUser() == null || MainFX.getAuthenticatedUser().getId() == null) {
			return null;
		}
		return MainFX.getAuthenticatedUser().getId();
	}
}


