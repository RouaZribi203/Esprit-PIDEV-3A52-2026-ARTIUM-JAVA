package controllers.amateur;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BookReaderController {
    static {
        // Suppress PDFBox font cache warnings that can be perceived as errors
        Logger.getLogger("org.apache.pdfbox").setLevel(Level.SEVERE);
    }
    
    @FXML
    private ImageView pageImageView;

    @FXML
    private Label pageLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private Label titleLabel;

    @FXML
    private Button prevButton;

    @FXML
    private Button nextButton;

    private PDDocument document;
    private PDFRenderer renderer;
    private int pageIndex;
    private int pageCount;
    private Stage currentStage;
    private int windowWidth = 900;
    private int windowHeight = 750;

    public void setPdfBytes(byte[] pdfBytes) throws IOException {
        close();
        this.document = PDDocument.load(new ByteArrayInputStream(pdfBytes));
        this.renderer = new PDFRenderer(document);
        this.pageCount = document.getNumberOfPages();
        this.pageIndex = 0;
        renderCurrentPage();
    }

    public void setStage(Stage stage) {
        this.currentStage = stage;
        stage.setWidth(windowWidth);
        stage.setHeight(windowHeight);
        stage.setMinWidth(600);
        stage.setMinHeight(500);
    }

    public void setBookTitle(String title) {
        if (titleLabel != null && title != null && !title.isEmpty()) {
            titleLabel.setText(title);
        }
    }

    @FXML
    private void onPrev() {
        if (pageIndex > 0) {
            pageIndex--;
            renderCurrentPage();
        }
    }

    @FXML
    private void onNext() {
        if (pageIndex < pageCount - 1) {
            pageIndex++;
            renderCurrentPage();
        }
    }

    private void renderCurrentPage() {
        if (document == null || renderer == null || pageCount <= 0) {
            setUiState(null);
            return;
        }
        try {
            BufferedImage buffered = renderer.renderImageWithDPI(pageIndex, 150);
            Image fxImage = SwingFXUtils.toFXImage(buffered, null);
            setUiState(fxImage);
        } catch (IOException e) {
            setUiState(null);
        }
    }

    private void setUiState(Image image) {
        if (pageImageView != null) {
            pageImageView.setImage(image);
            if (image != null) {
                double imgWidth = image.getWidth();
                double imgHeight = image.getHeight();
                if (imgWidth > 0 && imgHeight > 0) {
                    double scaleX = (windowWidth - 40) / imgWidth;
                    double scaleY = (windowHeight - 200) / imgHeight;
                    double scale = Math.min(scaleX, scaleY);
                    pageImageView.setFitWidth(imgWidth * scale);
                    pageImageView.setFitHeight(imgHeight * scale);
                }
            }
        }
        if (pageLabel != null) {
            pageLabel.setText((pageCount == 0 ? 0 : (pageIndex + 1)) + " / " + pageCount);
        }
        if (subtitleLabel != null) {
            subtitleLabel.setText(pageCount > 0 ? "Page " + (pageIndex + 1) + " de " + pageCount : "Aucun document");
        }
        if (prevButton != null) {
            prevButton.setDisable(pageIndex <= 0);
        }
        if (nextButton != null) {
            nextButton.setDisable(pageCount == 0 || pageIndex >= pageCount - 1);
        }
    }

    public void close() {
        if (document != null) {
            try {
                document.close();
            } catch (IOException ignored) {
            } finally {
                document = null;
                renderer = null;
                pageCount = 0;
                pageIndex = 0;
            }
        }
    }
}
