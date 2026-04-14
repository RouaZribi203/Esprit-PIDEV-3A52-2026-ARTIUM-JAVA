package controllers.amateur;

import Services.CommentaireService;
import Services.OeuvreService;
import entities.Commentaire;
import entities.Oeuvre;
import entities.User;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FeedController {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRANCE);
	private static final DateTimeFormatter COMMENT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.FRANCE);
	private static final double POST_IMAGE_MAX_WIDTH = 760;
	private static final double POST_IMAGE_MAX_HEIGHT = 360;
	// TODO: remplacer par l'ID utilisateur de session.
	private static final int CURRENT_USER_ID = 4;

	@FXML
	private TextField searchField;

	@FXML
	private VBox oeuvresContainer;

	@FXML
	private Label emptyStateLabel;

	private final OeuvreService oeuvreService = new OeuvreService();
	private final CommentaireService commentaireService = new CommentaireService();
	private final List<Oeuvre> allOeuvres = new ArrayList<>();

	@FXML
	public void initialize() {
		searchField.textProperty().addListener((observable, oldValue, newValue) -> applySearch());
		loadOeuvres();
	}

	@FXML
	private void onSearchClick() {
		applySearch();
	}

	private void loadOeuvres() {
		try {
			allOeuvres.clear();
			allOeuvres.addAll(oeuvreService.getAll());
			renderOeuvres(allOeuvres);
		} catch (Exception e) {
			oeuvresContainer.getChildren().clear();
			emptyStateLabel.setText("Erreur chargement oeuvres: " + e.getMessage());
			emptyStateLabel.setVisible(true);
		}
	}

	private void renderOeuvres(List<Oeuvre> oeuvres) {
		oeuvresContainer.getChildren().clear();

		if (oeuvres.isEmpty()) {
			emptyStateLabel.setText("Aucune oeuvre trouvee.");
			emptyStateLabel.setVisible(true);
			return;
		}

		emptyStateLabel.setVisible(false);
		for (Oeuvre oeuvre : oeuvres) {
			oeuvresContainer.getChildren().add(buildPostCard(oeuvre));
		}
	}

	private VBox buildPostCard(Oeuvre oeuvre) {
		VBox card = new VBox(10);
		card.getStyleClass().add("oeuvre-post-card");

		HBox topRow = new HBox(10);
		topRow.setAlignment(Pos.CENTER_LEFT);

		StackPane avatar = new StackPane();
		avatar.getStyleClass().add("oeuvre-post-avatar");
		Label avatarLetter = new Label("A");
		avatarLetter.getStyleClass().add("oeuvre-post-avatar-text");
		avatar.getChildren().add(avatarLetter);

		VBox identityBox = new VBox(1);
		Label authorLabel = new Label("Artiste");
		authorLabel.getStyleClass().add("oeuvre-post-author");
		Label specialiteLabel = new Label("Createur");
		specialiteLabel.getStyleClass().add("oeuvre-post-specialite");
		identityBox.getChildren().addAll(authorLabel, specialiteLabel);

		Label dateLabel = new Label(formatDate(oeuvre.getDateCreation()));
		dateLabel.getStyleClass().add("oeuvre-post-date");

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		topRow.getChildren().addAll(avatar, identityBox, spacer, dateLabel);

		Label titleLabel = new Label(fallback(oeuvre.getTitre(), "Sans titre"));
		titleLabel.getStyleClass().add("oeuvre-post-title");

		Label descLabel = new Label(fallback(oeuvre.getDescription(), "Aucune description"));
		descLabel.getStyleClass().add("oeuvre-post-description");
		descLabel.setWrapText(true);

		Label tagsLabel = new Label(toHashtag(oeuvre.getType()));
		tagsLabel.getStyleClass().add("oeuvre-post-tags");

		StackPane imageWrapper = new StackPane();
		imageWrapper.getStyleClass().add("oeuvre-post-image-wrapper");
		imageWrapper.setMaxWidth(POST_IMAGE_MAX_WIDTH + 20);
		imageWrapper.setPrefHeight(POST_IMAGE_MAX_HEIGHT + 20);

		ImageView imageView = createImageViewFromBlob(oeuvre.getImage());
		if (imageView == null) {
			Label noImageLabel = new Label("Aucune image");
			noImageLabel.getStyleClass().add("oeuvre-post-image-placeholder");
			imageWrapper.getChildren().add(noImageLabel);
		} else {
			imageWrapper.getChildren().add(imageView);
		}

		List<Commentaire> comments = loadCommentsForOeuvre(oeuvre);

		HBox statsRow = new HBox(14);
		statsRow.getStyleClass().add("oeuvre-post-stats");
		statsRow.getChildren().addAll(
				buildStatChip("M12.1 18.55 10.55 17.14C5.4 12.47 2 9.39 2 5.6 2 2.52 4.42 0 7.5 0c1.74 0 3.41.81 4.5 2.09C13.09.81 14.76 0 16.5 0 19.58 0 22 2.52 22 5.6c0 3.79-3.4 6.87-8.55 11.55z", 0),
				buildStatChip("M20 2H4c-1.1 0-1.99.9-1.99 2L2 22l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z", comments.size()),
				buildStatChip("M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z", 0)
		);

		VBox commentsPreviewBox = buildCommentsPreview(oeuvre, comments);
		card.getChildren().addAll(topRow, titleLabel, descLabel, tagsLabel, imageWrapper, statsRow, commentsPreviewBox);
		return card;
	}

	private HBox buildStatChip(String iconPath, int count) {
		HBox statChip = new HBox(6);
		statChip.setAlignment(Pos.CENTER_LEFT);
		statChip.getStyleClass().add("oeuvre-post-stat-chip");

		SVGPath icon = createColoredIcon(iconPath, 0.55, "#6b7280");
		icon.getStyleClass().add("oeuvre-post-stat-icon");

		Label countLabel = new Label(String.valueOf(Math.max(0, count)));
		countLabel.getStyleClass().add("oeuvre-post-stat-count");

		statChip.getChildren().addAll(icon, countLabel);
		return statChip;
	}

	private List<Commentaire> loadCommentsForOeuvre(Oeuvre oeuvre) {
		if (oeuvre == null || oeuvre.getId() == null) {
			return new ArrayList<>();
		}

		try {
			List<Commentaire> comments = commentaireService.getCommentsByOeuvreId(oeuvre.getId());
			return comments == null ? new ArrayList<>() : comments;
		} catch (Exception ignored) {
			return new ArrayList<>();
		}
	}

	private VBox buildCommentsPreview(Oeuvre oeuvre, List<Commentaire> comments) {
		VBox commentsBox = new VBox(8);
		commentsBox.getStyleClass().add("oeuvre-post-comments-box");

		Label title = new Label("Commentaires");
		title.getStyleClass().add("oeuvre-post-comments-title");
		commentsBox.getChildren().add(title);

		if (comments == null || comments.isEmpty()) {
			Label emptyLabel = new Label("Aucun commentaire pour le moment.");
			emptyLabel.getStyleClass().add("oeuvre-post-comments-empty");
			commentsBox.getChildren().add(emptyLabel);
			commentsBox.getChildren().add(buildCommentComposer(oeuvre));
			return commentsBox;
		}

		int displayCount = Math.min(3, comments.size());
		for (int i = 0; i < displayCount; i++) {
			commentsBox.getChildren().add(buildCommentRow(comments.get(i)));
		}

		if (comments.size() > 3) {
			Label moreLabel = new Label("+" + (comments.size() - 3) + " autres commentaires");
			moreLabel.getStyleClass().add("oeuvre-post-comments-more");
			commentsBox.getChildren().add(moreLabel);
		}

		commentsBox.getChildren().add(buildCommentComposer(oeuvre));

		return commentsBox;
	}

	private VBox buildCommentComposer(Oeuvre oeuvre) {
		VBox composerBox = new VBox(6);
		composerBox.getStyleClass().add("oeuvre-post-comment-composer-box");

		HBox row = new HBox(8);
		row.setAlignment(Pos.CENTER_LEFT);

		TextField commentField = new TextField();
		commentField.setPromptText("Ecrire un commentaire...");
		commentField.getStyleClass().add("oeuvre-post-comment-input");
		HBox.setHgrow(commentField, Priority.ALWAYS);

		Button publishButton = new Button("Publier");
		publishButton.getStyleClass().add("oeuvre-post-comment-submit");
		publishButton.setDisable(true);

		Label errorLabel = new Label();
		errorLabel.getStyleClass().add("oeuvre-post-comment-error");
		errorLabel.setManaged(false);
		errorLabel.setVisible(false);

		commentField.textProperty().addListener((obs, oldValue, newValue) -> {
			String value = safeText(newValue);
			publishButton.setDisable(value.isEmpty());
			if (!value.isEmpty()) {
				errorLabel.setText("");
				errorLabel.setManaged(false);
				errorLabel.setVisible(false);
			}
		});

		publishButton.setOnAction(event -> submitComment(oeuvre, commentField, errorLabel));
		commentField.setOnAction(event -> submitComment(oeuvre, commentField, errorLabel));

		row.getChildren().addAll(commentField, publishButton);
		composerBox.getChildren().addAll(row, errorLabel);
		return composerBox;
	}

	private void submitComment(Oeuvre oeuvre, TextField commentField, Label errorLabel) {
		String text = safeText(commentField.getText());
		if (text.isEmpty()) {
			showCommentError(errorLabel, "Le commentaire est vide.");
			return;
		}

		if (oeuvre == null || oeuvre.getId() == null) {
			showCommentError(errorLabel, "Oeuvre invalide.");
			return;
		}

		try {
			Commentaire commentaire = new Commentaire();
			commentaire.setTexte(text);
			commentaire.setOeuvreId(oeuvre.getId());
			commentaire.setUserId(CURRENT_USER_ID);
			commentaire.setDateCommentaire(LocalDate.now());
			commentaireService.add(commentaire);

			commentField.clear();
			loadOeuvres();
		} catch (Exception e) {
			showCommentError(errorLabel, e.getMessage() == null ? "Publication impossible." : e.getMessage());
		}
	}

	private void showCommentError(Label errorLabel, String message) {
		errorLabel.setText(message);
		errorLabel.setManaged(true);
		errorLabel.setVisible(true);
	}

	private HBox buildCommentRow(Commentaire comment) {
		HBox row = new HBox(8);
		row.setAlignment(Pos.TOP_LEFT);
		row.getStyleClass().add("oeuvre-post-comment-row");

		StackPane avatar = new StackPane();
		avatar.getStyleClass().add("oeuvre-post-comment-avatar");

		User user = null;
		if (comment.getUserId() != null) {
			user = oeuvreService.getUserById(comment.getUserId());
		}

		String authorName = "Utilisateur";
		String authorPhoto = null;
		if (user != null) {
			String prenom = safeText(user.getPrenom());
			String nom = safeText(user.getNom());
			authorName = (prenom + " " + nom).trim();
			if (authorName.isEmpty()) {
				authorName = "Utilisateur";
			}
			authorPhoto = user.getPhotoProfil();
		}

		ImageView profileImage = createImageFromSource(authorPhoto);
		if (profileImage != null) {
			avatar.getChildren().add(profileImage);
		} else {
			Label initial = new Label(getInitialLetter(authorName));
			initial.getStyleClass().add("oeuvre-post-comment-avatar-text");
			avatar.getChildren().add(initial);
		}

		VBox body = new VBox(2);
		HBox.setHgrow(body, Priority.ALWAYS);

		HBox header = new HBox(8);
		header.setAlignment(Pos.CENTER_LEFT);

		Label authorLabel = new Label(authorName);
		authorLabel.getStyleClass().add("oeuvre-post-comment-author");

		Label dateLabel = new Label(formatCommentDate(comment.getDateCommentaire()));
		dateLabel.getStyleClass().add("oeuvre-post-comment-date");

		Label textLabel = new Label(fallback(comment.getTexte(), "..."));
		textLabel.getStyleClass().add("oeuvre-post-comment-text");
		textLabel.setWrapText(true);

		header.getChildren().addAll(authorLabel, dateLabel);
		body.getChildren().addAll(header, textLabel);
		row.getChildren().addAll(avatar, body);
		return row;
	}

	private SVGPath createColoredIcon(String path, double scale, String fillColor) {
		SVGPath icon = new SVGPath();
		icon.setContent(path);
		icon.setScaleX(scale);
		icon.setScaleY(scale);
		icon.setStyle("-fx-fill: " + fillColor + ";");
		return icon;
	}

	private ImageView createImageViewFromBlob(byte[] imageBytes) {
		if (imageBytes == null || imageBytes.length == 0) {
			return null;
		}

		try {
			Image image = new Image(new ByteArrayInputStream(imageBytes));
			if (image.isError()) {
				return null;
			}

			ImageView imageView = new ImageView(image);
			imageView.setFitWidth(POST_IMAGE_MAX_WIDTH);
			imageView.setFitHeight(POST_IMAGE_MAX_HEIGHT);
			imageView.setPreserveRatio(true);
			imageView.setSmooth(true);
			return imageView;
		} catch (Exception ignored) {
			return null;
		}
	}

	private ImageView createImageFromSource(String source) {
		String safeSource = safeText(source);
		if (safeSource.isEmpty()) {
			return null;
		}

		try {
			Image image;
			if (safeSource.startsWith("http://") || safeSource.startsWith("https://") || safeSource.startsWith("file:")) {
				image = new Image(safeSource, true);
			} else {
				image = new Image(new File(safeSource).toURI().toString(), true);
			}

			if (image.isError()) {
				return null;
			}

			ImageView imageView = new ImageView(image);
			imageView.setFitWidth(30);
			imageView.setFitHeight(30);
			imageView.setPreserveRatio(true);
			imageView.setSmooth(true);
			return imageView;
		} catch (Exception ignored) {
			return null;
		}
	}

	private String formatDate(LocalDate date) {
		if (date == null) {
			return "Date inconnue";
		}
		return DATE_FORMATTER.format(date);
	}

	private String formatCommentDate(LocalDate date) {
		if (date == null) {
			return "Date inconnue";
		}
		return COMMENT_DATE_FORMATTER.format(date.atStartOfDay());
	}

	private String toHashtag(String value) {
		String cleaned = safeText(value);
		if (cleaned.isEmpty()) {
			return "";
		}
		return "#" + cleaned.replaceAll("\\s+", "_");
	}

	private String fallback(String value, String fallback) {
		return safeText(value).isEmpty() ? fallback : value;
	}

	private String safeText(String value) {
		return value == null ? "" : value.trim();
	}

	private String getInitialLetter(String value) {
		String safe = safeText(value);
		if (safe.isEmpty()) {
			return "A";
		}
		return safe.substring(0, 1).toUpperCase(Locale.ROOT);
	}

	private void applySearch() {
		String keyword = safeText(searchField.getText()).toLowerCase(Locale.ROOT);
		List<Oeuvre> filtered = new ArrayList<>();

		for (Oeuvre oeuvre : allOeuvres) {
			String title = safeText(oeuvre.getTitre()).toLowerCase(Locale.ROOT);
			String desc = safeText(oeuvre.getDescription()).toLowerCase(Locale.ROOT);
			String type = safeText(oeuvre.getType()).toLowerCase(Locale.ROOT);

			boolean keywordOk = keyword.isEmpty()
					|| title.contains(keyword)
					|| desc.contains(keyword)
					|| type.contains(keyword);

			if (keywordOk) {
				filtered.add(oeuvre);
			}
		}

		renderOeuvres(filtered);
	}
}

