package controllers.amateur;

import entities.Evenement;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class EventCardController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm");
    private static final String BADGE_EXPOSITION_CLASS = "amateur-event-badge-exposition";
    private static final String BADGE_CONCERT_CLASS = "amateur-event-badge-concert";
    private static final String BADGE_SPECTACLE_CLASS = "amateur-event-badge-spectacle";
    private static final String BADGE_CONFERENCE_CLASS = "amateur-event-badge-conference";

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

    private Evenement evenement;
    private Consumer<Evenement> detailHandler;

    public void setData(Evenement evenement) {
        this.evenement = evenement;
        String category = textOrDefault(evenement.getType(), "Evenement");
        categoryBadgeLabel.setText(category);
        applyCategoryBadgeStyle(category);
        titleLabel.setText(textOrDefault(evenement.getTitre(), "Evenement sans titre"));
        dateLabel.setText(formatDateTime(evenement.getDateDebut()));
        placeLabel.setText(formatCapacity(evenement.getCapaciteMax()));
        statusLabel.setText(textOrDefault(evenement.getStatut(), "A venir"));
        priceLabel.setText(formatPrice(evenement.getPrixTicket()));
        descriptionLabel.setText(textOrDefault(evenement.getDescription(), ""));
        applyImage(evenement.getImageCouverture());
    }

    public void setDetailHandler(Consumer<Evenement> detailHandler) {
        this.detailHandler = detailHandler;
    }

    @FXML
    private void onCardClick(MouseEvent event) {
        if (detailHandler != null && evenement != null) {
            detailHandler.accept(evenement);
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

    private void applyCategoryBadgeStyle(String category) {
        categoryBadgeLabel.getStyleClass().removeAll(
                BADGE_EXPOSITION_CLASS,
                BADGE_CONCERT_CLASS,
                BADGE_SPECTACLE_CLASS,
                BADGE_CONFERENCE_CLASS
        );

        String normalized = category == null ? "" : category.trim().toLowerCase();
        switch (normalized) {
            case "exposition":
                categoryBadgeLabel.getStyleClass().add(BADGE_EXPOSITION_CLASS);
                break;
            case "concert":
                categoryBadgeLabel.getStyleClass().add(BADGE_CONCERT_CLASS);
                break;
            case "spectacle":
                categoryBadgeLabel.getStyleClass().add(BADGE_SPECTACLE_CLASS);
                break;
            case "conference":
            case "conférence":
                categoryBadgeLabel.getStyleClass().add(BADGE_CONFERENCE_CLASS);
                break;
            default:
                break;
        }
    }
}


