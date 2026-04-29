package controllers.artist;

import entities.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;

import java.io.File;
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

        String imagePath = pickProfileImage(user);
        if (imagePath == null) {
            clearProfileImage();
            return;
        }

        try {
            Image image = new Image(toImageUrl(imagePath), false); // chargement synchrone
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
        Circle clip = new Circle(29, 29, 29);
        profileImageView.setClip(clip);
    }

    private void applyDefaultProfile() {
        bioLabel.setText(DEFAULT_BIO);
        dateNaissanceLabel.setText(DEFAULT_DATE);
        dateInscriptionLabel.setText(DEFAULT_DATE);
        emailLabel.setText(DEFAULT_EMAIL);
        clearProfileImage();
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
