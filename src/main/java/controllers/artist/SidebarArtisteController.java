package controllers.artist;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class SidebarArtisteController {

    @FXML
    private Label bioLabel;

    @FXML
    public void initialize() {
        bioLabel.setText("hello");
    }
}

