package controllers.artist;

import entities.Evenement;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.ByteArrayInputStream;
import java.time.format.DateTimeFormatter;

public class EvenementArtisteCardController {

    public interface CardActionHandler {
        void onEdit(Evenement evenement);

        void onDelete(Evenement evenement);
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private ImageView coverImageView;

    @FXML
    private Label titreLabel;

    @FXML
    private Label dateTypeLabel;

    @FXML
    private Label statusMetaLabel;

    @FXML
    private Label descriptionLabel;

    private Evenement evenement;
    private CardActionHandler actionHandler;

    public void setData(Evenement evenement, CardActionHandler actionHandler) {
        this.evenement = evenement;
        this.actionHandler = actionHandler;

        titreLabel.setText(textOrDefault(evenement.getTitre(), "Evenement sans titre"));
        dateTypeLabel.setText(formatMainMeta(evenement));
        statusMetaLabel.setText(formatSecondaryMeta(evenement));
        descriptionLabel.setText(textOrDefault(evenement.getDescription(), "Aucune description"));
        applyImage(evenement.getImageCouverture());
    }

    @FXML
    private void onEditClick() {
        if (actionHandler != null && evenement != null) {
            actionHandler.onEdit(evenement);
        }
    }

    @FXML
    private void onDeleteClick() {
        if (actionHandler != null && evenement != null) {
            actionHandler.onDelete(evenement);
        }
    }

    private String formatMainMeta(Evenement evenement) {
        String date = evenement.getDateDebut() == null ? "Date non definie" : DATE_FORMATTER.format(evenement.getDateDebut());
        String type = textOrDefault(evenement.getType(), "Type non precise");
        return date + "  |  " + type;
    }

    private String formatSecondaryMeta(Evenement evenement) {
        String statut = textOrDefault(evenement.getStatut(), "A venir");
        String capacite = evenement.getCapaciteMax() == null ? "-" : String.valueOf(evenement.getCapaciteMax());
        return "Statut: " + statut + "  |  Places: " + capacite;
    }

    private void applyImage(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            coverImageView.setImage(null);
            return;
        }

        try {
            Image image = new Image(new ByteArrayInputStream(imageBytes));
            if (image.isError()) {
                coverImageView.setImage(null);
                return;
            }
            coverImageView.setImage(image);
        } catch (Exception ignored) {
            coverImageView.setImage(null);
        }
    }

    private String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}


