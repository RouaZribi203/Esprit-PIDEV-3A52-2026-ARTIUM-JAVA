package components;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class HumanVerificationPane extends VBox {

    private final BooleanProperty humanVerified = new SimpleBooleanProperty(false);
    private final SliderCaptcha sliderCaptcha;
    private final QuestionCaptcha questionCaptcha;

    public HumanVerificationPane() {
        setSpacing(14);

        Label stepLabel1 = new Label("Étape 1 — Glissez le curseur jusqu'au bout");
        stepLabel1.setStyle("-fx-font-size: 12; -fx-font-weight: 700; -fx-text-fill: #4f46e5;");

        sliderCaptcha = new SliderCaptcha();

        Label stepLabel2 = new Label("Étape 2 — Répondez à la question");
        stepLabel2.setStyle("-fx-font-size: 12; -fx-font-weight: 700; -fx-text-fill: #4f46e5;");

        questionCaptcha = new QuestionCaptcha();
        questionCaptcha.setDisable(true);
        questionCaptcha.setOpacity(0.45);

        // Déverrouiller l'étape 2 quand l'étape 1 est validée
        sliderCaptcha.verifiedProperty().addListener((obs, old, now) -> {
            if (now) {
                questionCaptcha.setDisable(false);
                questionCaptcha.setOpacity(1.0);
            }
        });

        // La vérification complète = slider + question
        humanVerified.bind(Bindings.createBooleanBinding(
                () -> sliderCaptcha.isVerified() && questionCaptcha.isVerified(),
                sliderCaptcha.verifiedProperty(),
                questionCaptcha.verifiedProperty()
        ));

        VBox step1Box = styled(stepLabel1, sliderCaptcha);
        VBox step2Box = styled(stepLabel2, questionCaptcha);

        getChildren().addAll(step1Box, step2Box);
    }

    private VBox styled(Label label, javafx.scene.Node content) {
        VBox box = new VBox(8, label, content);
        box.setPadding(new Insets(16));
        box.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 14;
            -fx-border-radius: 14;
            -fx-border-color: #e2e8f0;
            -fx-border-width: 1;
            """);
        return box;
    }

    public BooleanProperty humanVerifiedProperty() { return humanVerified; }
    public boolean isHumanVerified() { return humanVerified.get(); }
}
