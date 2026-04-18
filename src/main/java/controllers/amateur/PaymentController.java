package controllers.amateur;

import entities.Ticket;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

import java.time.format.DateTimeFormatter;

public class PaymentController {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");

	@FXML
	private Label ticketReferenceLabel;

	@FXML
	private Label eventIdLabel;

	@FXML
	private Label userIdLabel;

	@FXML
	private Label purchaseDateLabel;

	@FXML
	private Label qrPayloadLabel;

	private Ticket ticket;
	private Runnable backToEventHandler;
	private Runnable backToEventsHandler;

	public void setTicket(Ticket ticket) {
		this.ticket = ticket;
		if (ticket == null) {
			clearView();
			return;
		}

		ticketReferenceLabel.setText(resolveReference(ticket));
		eventIdLabel.setText(formatInteger(ticket.getEvenementId()));
		userIdLabel.setText(formatInteger(ticket.getUserId()));
		purchaseDateLabel.setText(ticket.getDateAchat() == null ? "-" : DATE_FORMATTER.format(ticket.getDateAchat()));
		qrPayloadLabel.setText(resolveQrPayload(ticket));
	}

	public void setBackToEventHandler(Runnable backToEventHandler) {
		this.backToEventHandler = backToEventHandler;
	}

	public void setBackToEventsHandler(Runnable backToEventsHandler) {
		this.backToEventsHandler = backToEventsHandler;
	}

	@FXML
	private void onShowQrCodeClick() {
		if (ticket == null) {
			return;
		}

		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("QR du ticket");
		alert.setHeaderText("Contenu du ticket généré");
		alert.setContentText(resolveQrPayload(ticket));
		alert.showAndWait();
	}

	@FXML
	private void onDownloadTicketClick() {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Téléchargement du ticket");
		alert.setHeaderText("Export PDF non encore disponible");
		alert.setContentText("Le ticket a bien été créé, mais l'export PDF n'est pas encore implémenté.");
		alert.showAndWait();
	}

	@FXML
	private void onBackToEventClick() {
		if (backToEventHandler != null) {
			backToEventHandler.run();
		}
	}

	@FXML
	private void onBackToEventsClick() {
		if (backToEventsHandler != null) {
			backToEventsHandler.run();
		}
	}

	private void clearView() {
		ticketReferenceLabel.setText("-");
		eventIdLabel.setText("-");
		userIdLabel.setText("-");
		purchaseDateLabel.setText("-");
		qrPayloadLabel.setText("Aucun ticket disponible");
	}

	private String resolveReference(Ticket ticket) {
		if (ticket.getCodeQr() != null && !ticket.getCodeQr().isBlank()) {
			String payload = ticket.getCodeQr();
			int refIndex = payload.indexOf("|ref=");
			if (refIndex >= 0 && refIndex + 5 < payload.length()) {
				return payload.substring(refIndex + 5);
			}
			return payload;
		}
		return ticket.getEvenementId() + "-" + ticket.getUserId();
	}

	private String resolveQrPayload(Ticket ticket) {
		if (ticket.getCodeQr() == null || ticket.getCodeQr().isBlank()) {
			return "QR non disponible";
		}
		return ticket.getCodeQr();
	}

	private String formatInteger(Integer value) {
		return value == null ? "-" : String.valueOf(value);
	}
}

