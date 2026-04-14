package controllers.amateur;

import entities.Evenement;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EventCardController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm");

    @FXML
    private ImageView coverImageView;

    @FXML
    private Label categoryBadgeLabel;

    @FXML
    private Label titleLabel;

    @FXML
    private Label dateLabel;

    @FXML
    private Label placeLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label priceLabel;

    @FXML
    private Label descriptionLabel;

    public void setData(Evenement evenement) {
        categoryBadgeLabel.setText(textOrDefault(evenement.getType(), "Evenement"));
        titleLabel.setText(textOrDefault(evenement.getTitre(), "Evenement sans titre"));
        dateLabel.setText(formatDateTime(evenement.getDateDebut()));
        placeLabel.setText(formatCapacity(evenement.getCapaciteMax()));
        statusLabel.setText(textOrDefault(evenement.getStatut(), "A venir"));
        priceLabel.setText(formatPrice(evenement.getPrixTicket()));
        descriptionLabel.setText(textOrDefault(evenement.getDescription(), ""));
        applyImage(evenement.getImageCouverture());
    }

    private void applyImage(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            coverImageView.setImage(null);
            return;
        }
        try {
            Image image = new Image(new ByteArrayInputStream(imageBytes));
            coverImageView.setImage(image.isError() ? null : image);
        } catch (Exception e) {
            coverImageView.setImage(null);
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "Date non definie" : DATE_FORMATTER.format(dateTime);
    }

    private String formatCapacity(Integer capacity) {
        return capacity == null ? "Capacite non definie" : capacity + " places";
    }

    private String formatPrice(Double price) {
        return price == null ? "Prix non defini" : String.format("%.0f TND", price);
    }

    private String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}


