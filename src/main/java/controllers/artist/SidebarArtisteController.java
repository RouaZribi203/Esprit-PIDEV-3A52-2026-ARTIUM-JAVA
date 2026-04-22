package controllers.artist;

import entities.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import services.EvenementService;
import services.OeuvreService;
import services.ReclamationService;

import java.io.File;
import java.sql.SQLDataException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class SidebarArtisteController {

    private static final String DEFAULT_BIO = "Aucune biographie disponible.";
    private static final String DEFAULT_DATE = "-";
    private static final String DEFAULT_EMAIL = "-";
    private static final DateTimeFormatter BIRTH_DATE_FORMATTER =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).localizedBy(Locale.FRANCE);

    @FXML
    private Label bioLabel;

    @FXML
    private Label dateNaissanceLabel;

    @FXML
    private Label dateInscriptionLabel;

    @FXML
    private Label emailLabel;

    @FXML
    private ImageView profileImageView;

    @FXML
    private Label avatarFallbackLabel;

    @FXML
    private Label oeuvresCountLabel;

    @FXML
    private Label reclamationsCountLabel;

    @FXML
    private Label evenementsCountLabel;

    @FXML
    private Label commentsCountLabel;

    @FXML
    private Label likesCountLabel;

    private final OeuvreService oeuvreService = new OeuvreService();
    private final ReclamationService reclamationService = new ReclamationService();
    private final EvenementService evenementService = new EvenementService();

    @FXML
    public void initialize() {
        installCircularClip();
        applyDefaultProfile();
    }

    public void setUser(User user) {
        if (user == null) {
            applyDefaultProfile();
            return;
        }

        bioLabel.setText(nonBlank(user.getBiographie(), DEFAULT_BIO));
        dateNaissanceLabel.setText(formatDate(user.getDateNaissance()));
        dateInscriptionLabel.setText(formatDate(user.getDateInscription()));
        emailLabel.setText(nonBlank(user.getEmail(), DEFAULT_EMAIL));

        loadStatistics(user.getId());

        String imagePath = pickProfileImage(user);
        if (imagePath == null) {
            clearProfileImage();
            return;
        }

        try {
            Image image = new Image(toImageUrl(imagePath), true);
            if (image.isError()) {
                clearProfileImage();
                return;
            }
            profileImageView.setImage(image);
            profileImageView.setVisible(true);
            avatarFallbackLabel.setVisible(false);
            avatarFallbackLabel.setManaged(false);
        } catch (IllegalArgumentException ex) {
            clearProfileImage();
        }
    }

    private void installCircularClip() {
        Circle clip = new Circle();
        profileImageView.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            double radius = Math.min(newBounds.getWidth(), newBounds.getHeight()) / 2.0;
            clip.setRadius(radius);
            clip.setCenterX(newBounds.getWidth() / 2.0);
            clip.setCenterY(newBounds.getHeight() / 2.0);
        });
        profileImageView.setClip(clip);
    }

    private void applyDefaultProfile() {
        bioLabel.setText(DEFAULT_BIO);
        dateNaissanceLabel.setText(DEFAULT_DATE);
        dateInscriptionLabel.setText(DEFAULT_DATE);
        emailLabel.setText(DEFAULT_EMAIL);
        resetStatistics();
        clearProfileImage();
    }

    private void loadStatistics(int userId) {
        new Thread(() -> {
            try {
                int oeuvresCount = oeuvreService.getTotalOeuvresForArtiste(userId);
                int reclamationsCount = reclamationService.getTotalReclamationsForUser(userId);
                int evenementsCount = evenementService.getByArtisteId(userId).size();
                int commentsCount = oeuvreService.getTotalCommentsForArtiste(userId);
                int likesCount = oeuvreService.getTotalLikesForArtiste(userId);

                javafx.application.Platform.runLater(() -> {
                    oeuvresCountLabel.setText(String.valueOf(oeuvresCount));
                    reclamationsCountLabel.setText(String.valueOf(reclamationsCount));
                    evenementsCountLabel.setText(String.valueOf(evenementsCount));
                    commentsCountLabel.setText(String.valueOf(commentsCount));
                    likesCountLabel.setText(String.valueOf(likesCount));
                });
            } catch (SQLDataException e) {
                System.err.println("Erreur lors du chargement des statistiques: " + e.getMessage());
            }
        }).start();
    }

    private void resetStatistics() {
        oeuvresCountLabel.setText("0");
        reclamationsCountLabel.setText("0");
        evenementsCountLabel.setText("0");
        commentsCountLabel.setText("0");
        likesCountLabel.setText("0");
    }

    private void clearProfileImage() {
        profileImageView.setImage(null);
        profileImageView.setVisible(false);
        avatarFallbackLabel.setVisible(true);
        avatarFallbackLabel.setManaged(true);
    }

    private String pickProfileImage(User user) {
        if (!isBlank(user.getPhotoProfil())) {
            return user.getPhotoProfil().trim();
        }
        if (!isBlank(user.getPhotoReferencePath())) {
            return user.getPhotoReferencePath().trim();
        }
        return null;
    }

    private String toImageUrl(String imagePath) {
        if (imagePath.startsWith("file:") || imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            return imagePath;
        }
        return new File(imagePath).toURI().toString();
    }

    private String formatDate(LocalDate date) {
        return date == null ? DEFAULT_DATE : BIRTH_DATE_FORMATTER.format(date);
    }

    private String nonBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
