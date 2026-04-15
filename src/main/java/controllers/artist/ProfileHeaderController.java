package controllers.artist;

import controllers.MainFX;
import entities.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Locale;
import java.util.List;

public class ProfileHeaderController {

    public interface NavigationHandler {
        void onNavigate(String route);
    }

    @FXML
    private Label fullNameLabel;

    @FXML
    private Label metaLabel;

    @FXML
    private ImageView profileImageView;

    @FXML
    private Label avatarLabelFallback;

    @FXML
    private Button collectionsTabButton;

    @FXML
    private Button bibliothequeTabButton;

    @FXML
    private Button contentTabButton;

    @FXML
    private Button musiquesTabButton;

    @FXML
    private Button evenementsTabButton;

    @FXML
    private Button reclamationsTabButton;

    @FXML
    private Button statistiquesTabButton;

    private NavigationHandler navigationHandler;
    private String specialite = "Sculpteur";
    private String dynamicRoute = "oeuvres";
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).localizedBy(Locale.FRANCE);

    @FXML
    public void initialize() {
        installCircularClip();
        User connectedUser = MainFX.getAuthenticatedUser();
        if (connectedUser != null) {
            setUser(connectedUser);
        } else {
            applyDefaultProfile();
        }
        updateDynamicTab();
    }

    public void setUser(User user) {
        if (user == null) {
            applyDefaultProfile();
            return;
        }

        String fullName = buildFullName(user.getPrenom(), user.getNom());
        fullNameLabel.setText(fullName.isEmpty() ? "Artiste" : fullName);

        String specialite = user.getSpecialite() != null ? user.getSpecialite().trim() : "Artiste";
        this.specialite = specialite;

        String ville = user.getVille() != null ? user.getVille().trim() : "-";
        String dateInscription = user.getDateInscription() != null
            ? DATE_FORMATTER.format(user.getDateInscription())
            : "-";

        metaLabel.setText(specialite + "  -  " + ville + "  -  Inscrit le " + dateInscription);

        String imagePath = pickProfileImage(user);
        if (imagePath != null) {
            try {
                Image image = new Image(toImageUrl(imagePath), true);
                if (!image.isError()) {
                    profileImageView.setImage(image);
                    profileImageView.setVisible(true);
                    avatarLabelFallback.setVisible(false);
                    avatarLabelFallback.setManaged(false);
                    return;
                }
            } catch (IllegalArgumentException ex) {
                // Image invalide, utiliser le fallback
            }
        }
        clearProfileImage();
    }

    private void applyDefaultProfile() {
        fullNameLabel.setText("Artiste");
        metaLabel.setText("Artiste  -  -  Inscrit le -");
        clearProfileImage();
    }

    private void clearProfileImage() {
        profileImageView.setImage(null);
        profileImageView.setVisible(false);
        avatarLabelFallback.setVisible(true);
        avatarLabelFallback.setManaged(true);
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

    private String pickProfileImage(User user) {
        if (user.getPhotoProfil() != null && !user.getPhotoProfil().trim().isEmpty()) {
            return user.getPhotoProfil().trim();
        }
        if (user.getPhotoReferencePath() != null && !user.getPhotoReferencePath().trim().isEmpty()) {
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

    private String buildFullName(String prenom, String nom) {
        String firstName = prenom != null ? prenom.trim() : "";
        String lastName = nom != null ? nom.trim() : "";
        return (firstName + " " + lastName).trim();
    }

    public void setNavigationHandler(NavigationHandler navigationHandler) {
        this.navigationHandler = navigationHandler;
    }

    public void setActiveTab(String route) {
        List<Button> tabs = Arrays.asList(
                collectionsTabButton,
                bibliothequeTabButton,
                contentTabButton,
                evenementsTabButton,
                reclamationsTabButton,
                statistiquesTabButton
        );
        for (Button tab : tabs) {
            tab.getStyleClass().remove("active");
        }

        if ("collections".equals(route)) {
            collectionsTabButton.getStyleClass().add("active");
        } else if ("bibliotheque".equals(route)) {
            bibliothequeTabButton.getStyleClass().add("active");
        } else if ("musiques".equals(route)) {
            musiquesTabButton.getStyleClass().add("active");
        } else if (dynamicRoute.equals(route)) {
            contentTabButton.getStyleClass().add("active");
        } else if ("evenements".equals(route)) {
            evenementsTabButton.getStyleClass().add("active");
        } else if ("reclamations".equals(route)) {
            reclamationsTabButton.getStyleClass().add("active");
        } else if ("statistiques".equals(route)) {
            statistiquesTabButton.getStyleClass().add("active");
        }
    }

    @FXML
    private void onCollectionsClick() {
        navigate("collections");
    }

    @FXML
    private void onBibliothequeClick() {
        navigate("bibliotheque");
    }

    @FXML
    private void onDynamicContentClick() {
        navigate(dynamicRoute);
    }

    @FXML
    private void onMusiquesClick() {
        navigate("musiques");
    }

    @FXML
    private void onEvenementsClick() {
        navigate("evenements");
    }

    @FXML
    private void onReclamationsClick() {
        navigate("reclamations");
    }

    @FXML
    private void onStatistiquesClick() {
        navigate("statistiques");
    }

    @FXML
    private void onEditProfileClick() {
        // Placeholder for edit profile drawer/dialog.
    }

    private void navigate(String route) {
        if (navigationHandler != null) {
            navigationHandler.onNavigate(route);
        }
    }

    private void updateDynamicTab() {
        if ("Musicien".equalsIgnoreCase(specialite)) {
            dynamicRoute = "musiques";
            contentTabButton.setText("Musiques");
        } else if ("Auteur".equalsIgnoreCase(specialite)) {
            dynamicRoute = "bibliotheque";
            contentTabButton.setText("Bibliotheque");
        } else {
            dynamicRoute = "oeuvres";
            contentTabButton.setText("Mes Oeuvres");
        }
    }
}

