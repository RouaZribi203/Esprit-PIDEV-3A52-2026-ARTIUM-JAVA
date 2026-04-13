package controllers.pages.reclamations;

import Services.ReclamationService;
import entities.Reclamation;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

import java.sql.SQLDataException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.text.Normalizer;


public class ReclamationsController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statutFilter;

    @FXML private ToggleGroup typeTabsGroup;
    @FXML private ToggleButton tabAll;
    @FXML private ToggleButton tabPaiement;
    @FXML private ToggleButton tabOeuvre;
    @FXML private ToggleButton tabEvenement;
    @FXML private ToggleButton tabCompte;

    @FXML private GridPane cardsContainer;

    private final ReclamationService reclamationService = new ReclamationService();
    private final Services.ReponseService reponseService = new Services.ReponseService();

    private final List<Reclamation> all = new ArrayList<>();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        statutFilter.getItems().setAll("Tous", "Traite", "Non traite");
        statutFilter.getSelectionModel().selectFirst();

        searchField.textProperty().addListener((obs, o, n) -> applySearchAndFilter());
        statutFilter.valueProperty().addListener((obs, o, n) -> applySearchAndFilter());
        typeTabsGroup.selectedToggleProperty().addListener((obs, o, n) -> applySearchAndFilter());

        refresh();
    }

    private void refresh() {
        try {
            all.clear();
            all.addAll(reclamationService.getAll());
            applySearchAndFilter();
        } catch (SQLDataException e) {
            showError("Chargement impossible", e.getMessage());
        }
    }

    private void applySearchAndFilter() {
        String q = safe(searchField.getText()).trim().toLowerCase(Locale.ROOT);
        String selectedStatut = statutFilter.getValue();
        String selectedType = getSelectedType();

        List<Reclamation> filtered = all.stream()
                .filter(r -> matchesQuery(r, q))
                .filter(r -> matchesStatut(r, selectedStatut))
                .filter(r -> matchesType(r, selectedType))
                .sorted(Comparator.comparing((Reclamation r) -> r.getId() == null ? 0 : r.getId()).reversed())
                .collect(Collectors.toList());

        renderCards(filtered);
    }

    private void renderCards(List<Reclamation> list) {
        cardsContainer.getChildren().clear();

        for (int index = 0; index < list.size(); index++) {
            Reclamation r = list.get(index);
            try {
                FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/views/pages/reclamations/reclamation_card.fxml")));
                Parent card = loader.load();

                controllers.pages.reclamations.ReclamationCardController cardController = loader.getController();
                cardController.setData(r, buildUserText(r), buildDateText(r), new controllers.pages.reclamations.ReclamationCardController.CardActionHandler() {
                    @Override
                    public void onReply(Reclamation reclamation) {
                        cardController.openReplyDialog(reclamation);
                        refresh();
                    }

                    @Override
                    public void onDelete(Reclamation reclamation) {
                        handleDelete(reclamation);
                    }
                });

                if (card instanceof Region region) {
                    region.setMaxWidth(Double.MAX_VALUE);
                }
                cardsContainer.add(card, 0, index);
            } catch (Exception e) {
                showError("Affichage impossible", "Erreur pendant le rendu d'une carte reclamation: " + e.getMessage());
                return;
            }
        }
    }

    private void handleDelete(Reclamation rec) {
        ButtonType deleteButton = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "", deleteButton, cancelButton);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer la reclamation #" + (rec.getId() == null ? "" : rec.getId()) + " ?");
        confirm.setContentText("Cette action est irreversible.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != deleteButton) return;

        try {
            try { reponseService.deleteByReclamationId(rec.getId()); } catch (Exception ignored) {}
            reclamationService.delete(rec);
            refresh();
        } catch (Exception e) {
            showError("Suppression impossible", e.getMessage());
        }
    }

    private String getSelectedType() {
        Toggle t = typeTabsGroup.getSelectedToggle();
        if (t == tabPaiement) return "Paiement";
        if (t == tabOeuvre) return "Oeuvre";
        if (t == tabEvenement) return "Evenement";
        if (t == tabCompte) return "Compte";
        return "Tous les types";
    }

    private boolean matchesQuery(Reclamation r, String q) {
        if (q == null || q.isEmpty()) return true;
        String userText = buildUserText(r).toLowerCase(Locale.ROOT);
        return contains(r.getTexte(), q)
                || contains(r.getType(), q)
                || contains(r.getStatut(), q)
                || userText.contains(q);
    }

    private boolean matchesStatut(Reclamation r, String selected) {
        if (selected == null || selected.equals("Tous")) return true;
        String s = normalize(r.getStatut());
        String sel = normalize(selected);

        // normalize status values from DB: "non traitée", "NON_TRAITEE", "traitee", "en_cours"...
        boolean isNon = s.contains("nontra") || s.contains("non tra") || s.contains("non-tr") || s.contains("en cours") || s.contains("encours") || s.contains("pending");
        boolean isTraite = s.contains("traite") || s.contains("trait") || s.contains("resolu") || s.contains("resolved") || s.contains("done");

        if (sel.contains("traite") && !sel.contains("non")) {
            return isTraite && !isNon;
        }
        if (sel.contains("non")) {
            return isNon;
        }
        return true;
    }

    private boolean matchesType(Reclamation r, String selected) {
        if (selected == null || selected.equals("Tous les types")) return true;
        return normalize(r.getType()).contains(normalize(selected));
    }

    private String buildDateText(Reclamation r) {
        return r.getDateCreation() != null ? r.getDateCreation().format(DATE_FMT) : "";
    }

    private String buildUserText(Reclamation r) {
        // pas de UserService dans le projet -> affichage fallback
        return r.getUserId() != null ? "User #" + r.getUserId() : "-";
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private String normalize(String s) {
        String v = safe(s).toLowerCase(Locale.ROOT).replace("_", " ").replace("-", " ");
        v = Normalizer.normalize(v, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        while (v.contains("  ")) v = v.replace("  ", " ");
        return v.trim();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private void showError(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur");
        a.setHeaderText(title);
        a.setContentText(message);
        a.showAndWait();
    }
}


