package controllers;

import entities.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.shape.Circle;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class ProfileController {

    @FXML private ImageView profileAvatar;
    @FXML private Label avatarInitial;
    @FXML private Circle avatarPlaceholder;

    @FXML private Button editPhotoBtn;
    @FXML private Button editProfileBtn;

    @FXML private Label fullNameLabel;
    @FXML private Label roleLabel;
    @FXML private Label villeLabel;
    @FXML private Label emailLabel;
    @FXML private Label telLabel;

    @FXML private Label statOeuvres;
    @FXML private Label statDays;
    @FXML private Label statStatut;
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

    // ───────── AVATAR ─────────

    private void populateAvatar(User user) {

        String path = pickPhoto(user);
        Image img = null;

        if (path != null) {
            try {
                img = new Image(toUrl(path), 108, 108, false, true);
                if (img.isError()) img = null;
            } catch (Exception ignored) {}
        }

        if (img != null) {
            // IMAGE OK
            profileAvatar.setImage(img);
            profileAvatar.setVisible(true);

            if (avatarInitial != null) avatarInitial.setVisible(false);
            if (avatarPlaceholder != null) avatarPlaceholder.setVisible(false);

        } else {
            // PAS D'IMAGE → INITIAL
            profileAvatar.setVisible(false);

            String initial = "?";

            if (user.getNom() != null && !user.getNom().isBlank()) {
                initial = user.getNom().substring(0, 1).toUpperCase();
            } else if (user.getPrenom() != null && !user.getPrenom().isBlank()) {
                initial = user.getPrenom().substring(0, 1).toUpperCase();
            }

            if (avatarInitial != null) {
                avatarInitial.setText(initial);
                avatarInitial.setVisible(true);
            }

            if (avatarPlaceholder != null) {
                avatarPlaceholder.setVisible(true);
            }
        }
    }

    private String pickPhoto(User user) {
        if (user.getPhotoProfil() != null && !user.getPhotoProfil().isBlank())
            return user.getPhotoProfil().trim();

        if (user.getPhotoReferencePath() != null && !user.getPhotoReferencePath().isBlank())
            return user.getPhotoReferencePath().trim();

        return null;
    }

    private String toUrl(String p) {
        if (p.startsWith("file:") || p.startsWith("http"))
            return p;
        return new File(p).toURI().toString();
    }

    // ───────── IDENTITY ─────────

    private void populateIdentity(User user) {
        fullNameLabel.setText(orDash(user.getPrenom()) + " " + orDash(user.getNom()));

        String role = user.getRole();
        roleLabel.setText(role != null ? role.toUpperCase() : "UTILISATEUR");

        villeLabel.setText("📍 " + orDash(user.getVille()));
        emailLabel.setText("✉ " + orDash(user.getEmail()));
        telLabel.setText("☏ " + orDash(user.getNumTel()));
    }

    // ───────── STATS ─────────

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
            String s = statut.toLowerCase();

            if (s.contains("actif")) {
                statStatut.setText("Actif");
                statutDot.getStyleClass().setAll("statut-dot-actif");

            } else if (s.contains("bloqu")) {
                statStatut.setText("Bloqué");
                statutDot.getStyleClass().setAll("statut-dot-bloque");

            } else {
                statStatut.setText(capitalize(statut));
                statutDot.getStyleClass().setAll("statut-dot-inactif");
            }
        }
    }

    // ───────── BIO ─────────

    private void populateBio(User user) {
        String bio = user.getBiographie();
        bioLabel.setText(
                (bio != null && !bio.isBlank())
                        ? bio
                        : "Aucune biographie renseignée."
        );
    }

    // ───────── INFOS ─────────

    private void populatePersonalInfo(User user) {

        prenomVal.setText(orDash(user.getPrenom()));
        nomVal.setText(orDash(user.getNom()));

        naissanceVal.setText(
                user.getDateNaissance() != null
                        ? user.getDateNaissance().format(DATE_FMT)
                        : "—"
        );

        inscriptionVal.setText(
                user.getDateInscription() != null
                        ? user.getDateInscription().format(DATE_FMT)
                        : "—"
        );

        specialiteVal.setText(orDash(user.getSpecialite()));
    }

    // ───────── ACTIONS ─────────

    @FXML
    private void onEditPhoto() {
        System.out.println("Changer photo...");
    }

    @FXML
    private void onEditProfile() {
        MainController.getInstance().navigateTo("editProfile");
    }

    // ───────── UTILS ─────────

    private String orDash(String s) {
        return (s != null && !s.isBlank()) ? s : "—";
    }

    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}