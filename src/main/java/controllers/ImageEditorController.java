package controllers;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class ImageEditorController {

    private final String initialImagePath;
    private String editedImagePath;
    private final Stage stage;
    private ImageView imageView;
    private ColorAdjust colorAdjust;
    
    // Crop variables
    private Rectangle selectionRect;
    private double startX, startY;
    private boolean isCropping = false;

    public ImageEditorController(Stage owner, String imagePath) {
        this.initialImagePath = imagePath;
        this.editedImagePath = imagePath;
        this.stage = new Stage();
        this.stage.initModality(Modality.WINDOW_MODAL);
        this.stage.initOwner(owner);
        this.stage.setTitle("Editeur d'image");
        
        initUI();
    }

    private void initUI() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f8fafc;");
        root.setAlignment(Pos.CENTER);

        // Image Container
        StackPane imageContainer = new StackPane();
        imageContainer.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 8;");
        imageContainer.setPrefSize(600, 400);
        
        File file = new File(initialImagePath);
        Image image = new Image(file.toURI().toString());
        imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(550);
        imageView.setFitHeight(350);

        colorAdjust = new ColorAdjust();
        imageView.setEffect(colorAdjust);

        selectionRect = new Rectangle();
        selectionRect.setFill(Color.rgb(63, 68, 212, 0.3));
        selectionRect.setStroke(Color.valueOf("#3f44d4"));
        selectionRect.setStrokeWidth(2);
        selectionRect.setVisible(false);

        imageContainer.getChildren().addAll(imageView, selectionRect);

        // Controls
        VBox controls = new VBox(10);
        controls.setAlignment(Pos.CENTER);

        // Brightness Slider
        HBox brightnessBox = createSliderControl("Luminosite", -1.0, 1.0, 0, (obs, oldVal, newVal) -> colorAdjust.setBrightness(newVal.doubleValue()));
        
        // Contrast Slider
        HBox contrastBox = createSliderControl("Contraste", -1.0, 1.0, 0, (obs, oldVal, newVal) -> colorAdjust.setContrast(newVal.doubleValue()));

        // Saturation Slider
        HBox saturationBox = createSliderControl("Saturation", -1.0, 1.0, 0, (obs, oldVal, newVal) -> colorAdjust.setSaturation(newVal.doubleValue()));

        HBox filterButtons = new HBox(10);
        filterButtons.setAlignment(Pos.CENTER);
        
        Button normalBtn = createFilterButton("Normal", 0, 0);
        Button grayscaleBtn = createFilterButton("Noir & Blanc", 0, -1);
        Button sepiaBtn = new Button("Sepia");
        sepiaBtn.getStyleClass().add("filter-button");
        sepiaBtn.setOnAction(e -> {
            colorAdjust.setSaturation(-0.2);
            colorAdjust.setHue(0.05);
            colorAdjust.setBrightness(0.1);
        });

        filterButtons.getChildren().addAll(normalBtn, grayscaleBtn, sepiaBtn);

        Button cropBtn = new Button("Rogner (Crop)");
        cropBtn.getStyleClass().add("action-button");
        cropBtn.setOnAction(e -> toggleCrop());

        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_RIGHT);
        
        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> stage.close());

        Button saveBtn = new Button("Appliquer");
        saveBtn.setStyle("-fx-background-color: #3f44d4; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
        saveBtn.setOnAction(e -> saveEditedImage());

        footer.getChildren().addAll(cancelBtn, saveBtn);

        root.getChildren().addAll(imageContainer, controls, brightnessBox, contrastBox, saturationBox, filterButtons, cropBtn, footer);

        Scene scene = new Scene(root, 700, 750);
        stage.setScene(scene);

        // Mouse events for cropping
        imageContainer.setOnMousePressed(e -> {
            if (isCropping) {
                startX = e.getX();
                startY = e.getY();
                selectionRect.setX(startX);
                selectionRect.setY(startY);
                selectionRect.setWidth(0);
                selectionRect.setHeight(0);
                selectionRect.setVisible(true);
            }
        });

        imageContainer.setOnMouseDragged(e -> {
            if (isCropping) {
                double currentX = e.getX();
                double currentY = e.getY();
                
                double x = Math.min(startX, currentX);
                double y = Math.min(startY, currentY);
                double w = Math.abs(startX - currentX);
                double h = Math.abs(startY - currentY);

                selectionRect.setX(x);
                selectionRect.setY(y);
                selectionRect.setWidth(w);
                selectionRect.setHeight(h);
            }
        });
    }

    private HBox createSliderControl(String labelText, double min, double max, double initial, javafx.beans.value.ChangeListener<Number> listener) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        Label label = new Label(labelText);
        label.setPrefWidth(80);
        Slider slider = new Slider(min, max, initial);
        slider.setPrefWidth(300);
        slider.valueProperty().addListener(listener);
        box.getChildren().addAll(label, slider);
        return box;
    }

    private Button createFilterButton(String text, double hue, double saturation) {
        Button btn = new Button(text);
        btn.getStyleClass().add("filter-button");
        btn.setOnAction(e -> {
            colorAdjust.setHue(hue);
            colorAdjust.setSaturation(saturation);
        });
        return btn;
    }

    private void toggleCrop() {
        isCropping = !isCropping;
        if (!isCropping) {
            selectionRect.setVisible(false);
        }
    }

    private void saveEditedImage() {
        try {
            // If cropping is active and a selection was made
            Image imageToSave;
            if (isCropping && selectionRect.getWidth() > 10 && selectionRect.getHeight() > 10) {
                // Get the coordinates relative to the image view
                double scaleX = imageView.getImage().getWidth() / imageView.getBoundsInParent().getWidth();
                double scaleY = imageView.getImage().getHeight() / imageView.getBoundsInParent().getHeight();
                
                double x = (selectionRect.getX() - imageView.getLayoutX()) * scaleX;
                double y = (selectionRect.getY() - imageView.getLayoutY()) * scaleY;
                double w = selectionRect.getWidth() * scaleX;
                double h = selectionRect.getHeight() * scaleY;

                // Ensure within bounds
                x = Math.max(0, x);
                y = Math.max(0, y);
                w = Math.min(imageView.getImage().getWidth() - x, w);
                h = Math.min(imageView.getImage().getHeight() - y, h);

                // We need to apply the color adjustment first if any
                WritableImage snapshot = imageView.snapshot(null, null);
                
                // Re-calculate scales for the snapshot
                double snapScaleX = snapshot.getWidth() / imageView.getBoundsInParent().getWidth();
                double snapScaleY = snapshot.getHeight() / imageView.getBoundsInParent().getHeight();

                double snapX = (selectionRect.getX() - imageView.getLayoutX()) * snapScaleX;
                double snapY = (selectionRect.getY() - imageView.getLayoutY()) * snapScaleY;
                double snapW = selectionRect.getWidth() * snapScaleX;
                double snapH = selectionRect.getHeight() * snapScaleY;

                // Crop from snapshot
                javafx.scene.image.PixelReader reader = snapshot.getPixelReader();
                imageToSave = new WritableImage(reader, (int)snapX, (int)snapY, (int)snapW, (int)snapH);
            } else {
                imageToSave = imageView.snapshot(null, null);
            }

            File tempFile = File.createTempFile("edited_oeuvre_" + UUID.randomUUID(), ".png");
            BufferedImage bImage = SwingFXUtils.fromFXImage(imageToSave, null);
            ImageIO.write(bImage, "png", tempFile);
            
            this.editedImagePath = tempFile.getAbsolutePath();
            stage.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String showAndWait() {
        stage.showAndWait();
        return editedImagePath;
    }
}
