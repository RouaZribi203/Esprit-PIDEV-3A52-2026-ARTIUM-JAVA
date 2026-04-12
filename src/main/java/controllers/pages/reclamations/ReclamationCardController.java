package controllers.pages.reclamations;

import entities.Reclamation;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.stage.Modality;
import javafx.stage.Stage;


public class ReclamationCardController {

    public interface CardActionHandler {
        void onReply(Reclamation reclamation);
        void onDelete(Reclamation reclamation);
    }

    @FXML private Label texteLabel;
    @FXML private Label dateLabel;
    @FXML private Label userLabel;
    @FXML private Label typeLabel;
    @FXML private Label statutLabel;
    @FXML private Button dotsButton;

    private Reclamation reclamation;
    private CardActionHandler actionHandler;

    public void setData(Reclamation reclamation, String userText, String dateText, CardActionHandler actionHandler) {
        this.reclamation = reclamation;
        this.actionHandler = actionHandler;

        texteLabel.setText(truncate(reclamation.getTexte(), 55));
        dateLabel.setText(dateText == null ? "" : dateText);
        userLabel.setText(userText == null ? "-" : userText);
        typeLabel.setText(reclamation.getType() == null ? "" : reclamation.getType());
        statutLabel.setText(reclamation.getStatut() == null ? "" : reclamation.getStatut());

        initMenu();
    }

    private void initMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem reply = new MenuItem("Repondre");
        MenuItem delete = new MenuItem("Supprimer");
        menu.getItems().addAll(reply, delete);

        dotsButton.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                menu.show(dotsButton, e.getScreenX(), e.getScreenY());
            }
        });

        reply.setOnAction(e -> {
            if (actionHandler != null && reclamation != null) actionHandler.onReply(reclamation);
        });

        delete.setOnAction(e -> {
            if (actionHandler != null && reclamation != null) actionHandler.onDelete(reclamation);
        });
    }

    public void openReplyDialog(Reclamation reclamation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/pages/reclamation_reply_dialog.fxml"));
            Parent root = loader.load();

            ReclamationReplyDialogController controller = loader.getController();
            controller.setReclamation(reclamation);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Repondre a la reclamation #" + reclamation.getId());
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception ex) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Erreur");
            a.setHeaderText("Ouverture impossible");
            a.setContentText(ex.getMessage());
            a.showAndWait();
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        String t = s.trim();
        if (t.length() <= max) return t;
        return t.substring(0, Math.max(0, max - 3)) + "...";
    }
}

