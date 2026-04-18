package controllers.amateur;

import entities.Evenement;
import entities.Ticket;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import services.TicketService;

import java.io.File;
import java.sql.SQLDataException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public class EventDetailController {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm");
	private static final DateTimeFormatter PURCHASE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
	private static final int CURRENT_USER_ID = 1; // TODO: replace with authenticated user id.

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
	private Evenement event;
	private Consumer<Ticket> purchaseHandler;
	private Runnable backHandler;

	public void setEvent(Evenement event) {
		this.event = event;
		if (event == null) {
			buyTicketButton.setDisable(true);
			renderPurchasedTickets(List.of());
			return;
		}

		buyTicketButton.setDisable(false);
		buyTicketButton.setText("Acheter ticket");

		titleLabel.setText(textOrDefault(event.getTitre(), "Evenement"));
		typeLabel.setText(textOrDefault(event.getType(), "Type non precise"));
		statusLabel.setText(textOrDefault(event.getStatut(), "A venir"));
		dateDebutLabel.setText(formatDate(event.getDateDebut()));
		dateFinLabel.setText(formatDate(event.getDateFin()));
		priceLabel.setText(formatPrice(event.getPrixTicket()));
		capacityLabel.setText(formatCapacity(event.getCapaciteMax()));
		galleryLabel.setText("Galerie #" + textOrDash(event.getGalerieId()));
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

		try {
			Ticket ticket = ticketService.purchaseTicket(event, CURRENT_USER_ID);
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
		if (event == null || event.getId() == null) {
			renderPurchasedTickets(List.of());
			return;
		}

		try {
			List<Ticket> tickets = ticketService.getTicketsByEventAndUser(event.getId(), CURRENT_USER_ID);
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
			Button qrButton = new Button("QR Code");
			qrButton.getStyleClass().add("ticket-action-secondary");
			qrButton.setOnAction(event -> showQrPayload(ticket));
			Button pdfButton = new Button("Telecharger");
			pdfButton.getStyleClass().add("ticket-action-primary");
			pdfButton.setOnAction(event -> showPdfNotReady());
			actions.getChildren().addAll(qrButton, pdfButton);

			HBox.setHgrow(info, Priority.ALWAYS);
			row.getChildren().addAll(info, actions);
			ticketsContainer.getChildren().add(row);
		}
	}

	private void showQrPayload(Ticket ticket) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("QR du ticket");
		alert.setHeaderText("Contenu du QR");
		alert.setContentText(resolveQrPayload(ticket));
		alert.showAndWait();
	}

	private void showPdfNotReady() {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Ticket PDF");
		alert.setHeaderText("Export PDF non disponible");
		alert.setContentText("Le ticket est achete. L'export PDF sera ajoute ensuite.");
		alert.showAndWait();
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

	private String textOrDash(Integer value) {
		return value == null ? "-" : String.valueOf(value);
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

	private String resolveQrPayload(Ticket ticket) {
		if (ticket.getCodeQr() == null || ticket.getCodeQr().isBlank()) {
			return "QR non disponible";
		}
		return ticket.getCodeQr();
	}

	private String formatPurchaseDate(Ticket ticket) {
		return ticket.getDateAchat() == null ? "-" : PURCHASE_DATE_FORMATTER.format(ticket.getDateAchat());
	}
}


