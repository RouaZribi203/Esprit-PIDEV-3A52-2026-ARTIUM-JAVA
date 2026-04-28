package controllers;

import entities.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class ProfileController {

    @FXML private ImageView profileAvatar;
    @FXML private Button    editPhotoBtn;
    @FXML private Button    editProfileBtn;

    @FXML private Label fullNameLabel;
    @FXML private Label roleLabel;
    @FXML private Label villeLabel;
    @FXML private Label emailLabel;
    @FXML private Label telLabel;

    @FXML private Label  statOeuvres;
    @FXML private Label  statDays;
    @FXML private Label  statStatut;
    @FXML private Circle statutDot;

    @FXML private Label bioLabel;

    @FXML private Label prenomVal;
    @FXML private Label nomVal;
    @FXML private Label naissanceVal;
    @FXML private Label inscriptionVal;
    @FXML private Label specialiteVal;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    @FXML
    public void initialize() {
        User user = MainFX.getAuthenticatedUser();
        if (user == null) return;

        populateAvatar(user);
        populateIdentity(user);
        populateStats(user);
        populateBio(user);
        populatePersonalInfo(user);
    }

    // ── Avatar ────────────────────────────────────────────────────────────────

    private void populateAvatar(User user) {
        Circle clip = new Circle(54);
        clip.setCenterX(54);
        clip.setCenterY(54);
        profileAvatar.setClip(clip);

        String path = pickPhoto(user);
        Image img = null;

        if (path != null) {
            try {
                img = new Image(toUrl(path), 108, 108, false, true);
                if (img.isError()) img = null;
            } catch (IllegalArgumentException ignored) {}
        }

        if (img == null) {
            try {
                img = new Image(
                        getClass().getResourceAsStream("/images/default_avatar.png"),
                        108, 108, false, true);
            } catch (Exception ignored) {
                return;
            }
        }

        profileAvatar.setImage(img);
    }

    private String pickPhoto(User user) {
        if (user.getPhotoProfil() != null && !user.getPhotoProfil().isBlank())
            return user.getPhotoProfil().trim();
        if (user.getPhotoReferencePath() != null && !user.getPhotoReferencePath().isBlank())
            return user.getPhotoReferencePath().trim();
        return null;
    }

    private String toUrl(String p) {
        if (p.startsWith("file:") || p.startsWith("http://") || p.startsWith("https://"))
            return p;
        return new File(p).toURI().toString();
    }

    // ── Identity ──────────────────────────────────────────────────────────────

    private void populateIdentity(User user) {
        fullNameLabel.setText(orDash(user.getPrenom()) + " " + orDash(user.getNom()));

        String role = user.getRole();
        roleLabel.setText(role != null ? role.toUpperCase() : "UTILISATEUR");

        villeLabel.setText("📍 " + orDash(user.getVille()));
        emailLabel.setText("✉  " + orDash(user.getEmail()));
        telLabel  .setText("☏  " + orDash(user.getNumTel()));
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    private void populateStats(User user) {
        int nbOeuvres = user.getOeuvres() != null ? user.getOeuvres().size() : 0;
        statOeuvres.setText(String.valueOf(nbOeuvres));

        if (user.getDateInscription() != null) {
            long days = ChronoUnit.DAYS.between(user.getDateInscription(), LocalDate.now());
            statDays.setText(String.valueOf(days));
        } else {
            statDays.setText("—");
        }

        String statut = user.getStatut();

        if (statut != null) {
            // Normaliser : minuscules + supprimer accents
            String s = statut.trim().toLowerCase()
                    .replace("é", "e")
                    .replace("è", "e")
                    .replace("ê", "e");

            if (s.equals("actif") || s.equals("active")) {
                statStatut.setText("Actif");
                statStatut.setStyle("-fx-text-fill: #10b981;");
                if (statutDot != null)
                    statutDot.getStyleClass().setAll("statut-dot-actif");

            } else if (s.contains("bloqu") || s.equals("bloque") || s.equals("blocked")) {
                statStatut.setText("Bloqué");
                statStatut.setStyle("-fx-text-fill: #ef4444;");
                if (statutDot != null)
                    statutDot.getStyleClass().setAll("statut-dot-bloque");

            } else {
                statStatut.setText(capitalize(statut));
                statStatut.setStyle("-fx-text-fill: #f59e0b;");
                if (statutDot != null)
                    statutDot.getStyleClass().setAll("statut-dot-inactif");
            }
        } else {
            statStatut.setText("—");
            statStatut.setStyle("");
        }
    }

    // ── Bio ───────────────────────────────────────────────────────────────────

    private void populateBio(User user) {
        String bio = user.getBiographie();
        bioLabel.setText(bio != null && !bio.isBlank() ? bio : "Aucune biographie renseignée.");
    }

    // ── Infos personnelles ────────────────────────────────────────────────────

    private void populatePersonalInfo(User user) {
        prenomVal    .setText(orDash(user.getPrenom()));
        nomVal       .setText(orDash(user.getNom()));
        naissanceVal .setText(user.getDateNaissance() != null
                ? user.getDateNaissance().format(DATE_FMT) : "—");
        inscriptionVal.setText(user.getDateInscription() != null
                ? user.getDateInscription().format(DATE_FMT) : "—");

        String role = user.getRole();
        specialiteVal.setText("amateur".equalsIgnoreCase(role) ? "—"
                : orDash(user.getSpecialite()));
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    @FXML
    private void onEditPhoto() {
        // TODO : FileChooser pour changer la photo de profil
    }

    @FXML
    private void onEditProfile() {
        try {
            MainController mainController = MainController.getInstance();
            if (mainController != null) {
                mainController.navigateTo("editProfile");
            } else {
                System.err.println("ERREUR: MainController instance est null");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private String orDash(String s) {
        return (s != null && !s.isBlank()) ? s : "—";
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}