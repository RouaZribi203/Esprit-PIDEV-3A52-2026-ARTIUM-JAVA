package controllers.pages.reclamations;

import Services.ReclamationService;
import Services.ReponseService;
import entities.Reclamation;
import entities.Reponse;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReclamationReplyDialogController {

    @FXML private Label reclamationIdLabel;
    @FXML private TextArea reclamationTexteArea;
    @FXML private ListView<Reponse> historyList;

    @FXML private VBox replyFieldsBox;

    @FXML private Button saveBtn;
    @FXML private Button updateBtn;
    @FXML private Button deleteBtn;
    @FXML private Button cancelBtn;

    private final ReponseService reponseService = new ReponseService();
    private final ReclamationService reclamationService = new ReclamationService();

    private Reclamation reclamation;
    private Reponse selectedForEdit;

    private boolean readOnly = false;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int MIN_REPONSE_LEN = 10;

    @FXML
    public void initialize() {
        reclamationTexteArea.setEditable(false);
        reclamationTexteArea.setWrapText(true);

        historyList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Reponse item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String d = item.getDateReponse() != null ? item.getDateReponse().format(DATE_FMT) : "";
                    setText("[" + d + "] " + (item.getContenu() == null ? "" : item.getContenu()));
                }
            }
        });

        historyList.setOnMouseClicked(e -> {
            if (readOnly) return;
            if (e.getClickCount() == 2 && historyList.getSelectionModel().getSelectedItem() != null) {
                selectedForEdit = historyList.getSelectionModel().getSelectedItem();
                fillEditField(selectedForEdit);
            }
        });

        addReplyField();
        updateBtn.setDisable(true);
        deleteBtn.setDisable(true);
    }

    public void setReclamation(Reclamation reclamation) {
        this.reclamation = reclamation;
        reclamationIdLabel.setText(reclamation.getId() != null ? String.valueOf(reclamation.getId()) : "-");
        reclamationTexteArea.setText(reclamation.getTexte() != null ? reclamation.getTexte() : "");
        loadHistory();
    }

    /**
     * Mode lecture seule: l'utilisateur voit uniquement les réponses existantes.
     * - cache la zone de saisie
     * - désactive les actions d'édition
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;

        if (replyFieldsBox != null) {
            replyFieldsBox.setManaged(!readOnly);
            replyFieldsBox.setVisible(!readOnly);
        }
        if (saveBtn != null) saveBtn.setManaged(!readOnly);
        if (saveBtn != null) saveBtn.setVisible(!readOnly);

        if (updateBtn != null) updateBtn.setManaged(!readOnly);
        if (updateBtn != null) updateBtn.setVisible(!readOnly);
        if (updateBtn != null) updateBtn.setDisable(true);

        if (deleteBtn != null) deleteBtn.setManaged(!readOnly);
        if (deleteBtn != null) deleteBtn.setVisible(!readOnly);
        if (deleteBtn != null) deleteBtn.setDisable(true);

        if (reclamationTexteArea != null) reclamationTexteArea.setEditable(false);
    }

    @FXML
    private void onAddField() {
        addReplyField();
    }

    private void addReplyField() {
        TextArea ta = new TextArea();
        ta.setPromptText("Votre reponse...");
        ta.setWrapText(true);
        ta.setPrefRowCount(3);

        VBox wrapper = new VBox(6);
        wrapper.getChildren().add(ta);
        replyFieldsBox.getChildren().add(wrapper);
    }

    private void fillEditField(Reponse rep) {
        if (rep == null) return;

        if (replyFieldsBox.getChildren().isEmpty()) {
            addReplyField();
        }

        VBox wrapper = (VBox) replyFieldsBox.getChildren().get(0);
        TextArea ta = null;
        for (var child : wrapper.getChildren()) {
            if (child instanceof TextArea t) {
                ta = t;
                break;
            }
        }

        if (ta != null) {
            ta.setText(rep.getContenu() != null ? rep.getContenu() : "");
            ta.requestFocus();
        }

        updateBtn.setDisable(false);
        deleteBtn.setDisable(false);
    }

    private void loadHistory() {
        if (reclamation == null || reclamation.getId() == null) return;
        try {
            List<Reponse> reps = reponseService.getByReclamationId(reclamation.getId());
            historyList.getItems().setAll(reps);
        } catch (Exception e) {
            historyList.getItems().clear();
        }
    }

    private void markReclamationTraite() {
        if (reclamation == null || reclamation.getId() == null) return;
        try {
            // Update only statut to avoid failing because of other columns
            reclamationService.updateStatutById(reclamation.getId(), "Traitée");
            reclamation.setStatut("Traitée");
        } catch (Exception ignored) {
        }
    }

    private void syncStatutWithReponses() {
        if (reclamation == null || reclamation.getId() == null) return;
        try {
            List<Reponse> reps = reponseService.getByReclamationId(reclamation.getId());
            boolean hasReps = reps != null && !reps.isEmpty();

            String target = hasReps ? "Traitée" : "Non traitée";
            reclamationService.updateStatutById(reclamation.getId(), target);
            reclamation.setStatut(target);
        } catch (Exception ignored) {
        }
    }

    @FXML
    private void onSave() {
        if (reclamation == null || reclamation.getId() == null) {
            showError("Reclamation invalide", "Aucun ID.");
            return;
        }

        List<String> contenus = new ArrayList<>();
        for (var node : replyFieldsBox.getChildren()) {
            if (node instanceof VBox v) {
                for (var child : v.getChildren()) {
                    if (child instanceof TextArea ta) {
                        String txt = ta.getText() != null ? ta.getText().trim() : "";
                        if (!txt.isEmpty()) contenus.add(txt);
                    }
                }
            }
        }

        for (String c : contenus) {
            if (c.length() < MIN_REPONSE_LEN) {
                showError("Validation", "La reponse doit contenir au moins " + MIN_REPONSE_LEN + " caracteres.");
                return;
            }
        }


        if (contenus.isEmpty()) {
            syncStatutWithReponses();
            close();
            return;
        }

        try {
            for (String c : contenus) {
                Reponse r = new Reponse();
                r.setReclamationId(reclamation.getId());
                r.setContenu(c);
                r.setDateReponse(LocalDate.now());
                reponseService.add(r);
            }

            markReclamationTraite();

            close();
        } catch (Exception e) {
            showError("Enregistrement impossible", e.getMessage());
        }
    }

    @FXML
    private void onUpdate() {
        if (selectedForEdit == null || selectedForEdit.getId() == null) {
            showError("Edition", "Veuillez selectionner une reponse a modifier (double clic).");
            return;
        }

        if (replyFieldsBox.getChildren().isEmpty()) {
            showError("Edition", "Aucun champ de saisie.");
            return;
        }

        VBox wrapper = (VBox) replyFieldsBox.getChildren().get(0);
        TextArea ta = null;
        for (var child : wrapper.getChildren()) {
            if (child instanceof TextArea t) {
                ta = t;
                break;
            }
        }
        if (ta == null) {
            showError("Edition", "Champ introuvable.");
            return;
        }

        String newText = ta.getText() == null ? "" : ta.getText().trim();
        if (newText.isEmpty()) {
            showError("Validation", "La reponse ne peut pas etre vide.");
            return;
        }

        if (newText.length() < MIN_REPONSE_LEN) {
            showError("Validation", "La reponse doit contenir au moins " + MIN_REPONSE_LEN + " caracteres.");
            return;
        }

        try {
            selectedForEdit.setContenu(newText);
            selectedForEdit.setDateReponse(LocalDate.now());
            reponseService.update(selectedForEdit);
            loadHistory();

            markReclamationTraite();

            selectedForEdit = null;
            updateBtn.setDisable(true);
            ta.clear();
        } catch (Exception e) {
            showError("Modification impossible", e.getMessage());
        }
    }

    @FXML
    private void onDeleteReponse() {
        Reponse selected = historyList.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getId() == null) {
            showError("Suppression", "Veuillez selectionner une reponse a supprimer.");
            return;
        }

        ButtonType deleteButton = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "", deleteButton, cancelButton);
        confirm.setTitle("Confirmer");
        confirm.setHeaderText("Supprimer cette reponse ?");
        confirm.setContentText("Cette action est irreversible.");

        if (confirm.showAndWait().orElse(cancelButton) != deleteButton) return;

        try {
            reponseService.deleteById(selected.getId());
            loadHistory();


            syncStatutWithReponses();

            historyList.getSelectionModel().clearSelection();

            // reset edit mode
            selectedForEdit = null;
            updateBtn.setDisable(true);
            deleteBtn.setDisable(true);
        } catch (Exception e) {
            showError("Suppression impossible", e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }

    private void showError(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(message);
        a.showAndWait();
    }
}



