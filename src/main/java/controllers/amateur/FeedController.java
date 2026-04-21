package controllers.amateur;

import services.CommentaireService;
import services.LikeService;
import services.OeuvreRecommendationService;
import services.OeuvreCollectionService;
import services.OeuvreService;
import entities.CollectionOeuvre;
import entities.Commentaire;
import entities.Oeuvre;
import entities.User;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import utils.UserSession;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class FeedController {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRANCE);
	private static final DateTimeFormatter COMMENT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRANCE);
	private static final double POST_IMAGE_MAX_WIDTH = 700;
	private static final double POST_IMAGE_MAX_HEIGHT = 360;
	private Integer currentUserId;

	@FXML
	private VBox oeuvresContainer;

	@FXML
	private Label emptyStateLabel;

	private final OeuvreService oeuvreService = new OeuvreService();
	private final OeuvreCollectionService oeuvreCollectionService = new OeuvreCollectionService();
	private final CommentaireService commentaireService = new CommentaireService();
	private final LikeService likeService = new LikeService();
	private final OeuvreRecommendationService oeuvreRecommendationService = new OeuvreRecommendationService();
	private final List<Oeuvre> allOeuvres = new ArrayList<>();
	private final List<Oeuvre> recommendedOeuvres = new ArrayList<>();
	private final Map<Integer, String> collectionHashtagById = new HashMap<>();
	private final Map<Integer, CollectionOeuvre> collectionById = new HashMap<>();
	private final Map<Integer, User> artistById = new HashMap<>();
	private String currentRouteFilter = "feed";

	@FXML
	public void initialize() {
		currentUserId = UserSession.getCurrentUserId();
		loadOeuvres();
	}

	public void setRouteFilter(String route) {
		currentUserId = UserSession.getCurrentUserId();
		currentRouteFilter = route == null ? "feed" : route;
		loadOeuvres();
	}

	private void loadOeuvres() {
		try {
			allOeuvres.clear();
			allOeuvres.addAll(oeuvreService.getAll());
			recommendedOeuvres.clear();
			if (isRecommendationRoute(currentRouteFilter)) {
				User currentUser = UserSession.getCurrentUser();
				recommendedOeuvres.addAll(oeuvreRecommendationService.getRecommendedOeuvresByImage(currentUser, 12));
			}
			applySearch();
		} catch (Exception e) {
			oeuvresContainer.getChildren().clear();
			emptyStateLabel.setText("Erreur chargement oeuvres: " + e.getMessage());
			emptyStateLabel.setVisible(true);
		}
	}

	private void renderOeuvres(List<Oeuvre> oeuvres) {
		oeuvresContainer.getChildren().clear();

		if (oeuvres.isEmpty()) {
			if ("favoris".equals(currentRouteFilter)) {
				emptyStateLabel.setText("Aucune oeuvre en favoris.");
			} else {
				emptyStateLabel.setText("Aucune oeuvre trouvee.");
			}
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
		card.setMaxWidth(760);
		card.setPrefWidth(760);

		User artist = loadArtistForOeuvre(oeuvre);
		String artistName = buildArtistName(artist);
		String artistSpecialite = buildArtistSpecialite(artist);

		HBox topRow = new HBox(10);
		topRow.setAlignment(Pos.CENTER_LEFT);

		StackPane avatar = new StackPane();
		avatar.getStyleClass().add("oeuvre-post-avatar");
		Label avatarLetter = new Label(getInitialLetter(artistName));
		avatarLetter.getStyleClass().add("oeuvre-post-avatar-text");
		avatar.getChildren().add(avatarLetter);

		VBox identityBox = new VBox(1);
		Label authorLabel = new Label(artistName);
		authorLabel.getStyleClass().add("oeuvre-post-author");
		Label specialiteLabel = new Label(artistSpecialite);
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

		HBox tagsRow = new HBox(8);
		String typeTag = toHashtag(oeuvre.getType());
		if (!typeTag.isEmpty()) {
			Label typeTagLabel = new Label(typeTag);
			typeTagLabel.getStyleClass().addAll("oeuvre-post-tag", "oeuvre-post-tag-type");
			tagsRow.getChildren().add(typeTagLabel);
		}

		String collectionTag = getCollectionHashtag(oeuvre);
		if (!collectionTag.isEmpty()) {
			Label collectionTagLabel = new Label(collectionTag);
			collectionTagLabel.getStyleClass().addAll("oeuvre-post-tag", "oeuvre-post-tag-collection");
			tagsRow.getChildren().add(collectionTagLabel);
		}

		if (tagsRow.getChildren().isEmpty()) {
			Label fallbackTag = new Label("#Oeuvre");
			fallbackTag.getStyleClass().addAll("oeuvre-post-tag", "oeuvre-post-tag-type");
			tagsRow.getChildren().add(fallbackTag);
		}

		StackPane imageWrapper = new StackPane();
		imageWrapper.getStyleClass().add("oeuvre-post-image-wrapper");
		imageWrapper.setMaxWidth(POST_IMAGE_MAX_WIDTH + 20);
		imageWrapper.setPrefHeight(POST_IMAGE_MAX_HEIGHT + 20);

		ImageView imageView = createImageViewFromSource(oeuvre.getImage());
		if (imageView == null) {
			Label noImageLabel = new Label("Aucune image");
			noImageLabel.getStyleClass().add("oeuvre-post-image-placeholder");
			imageWrapper.getChildren().add(noImageLabel);
		} else {
			imageWrapper.getChildren().add(imageView);
		}

		List<Commentaire> comments = loadCommentsForOeuvre(oeuvre);
		int likesCount = getLikesCount(oeuvre);
		int favorisCount = getFavorisCount(oeuvre);
		boolean likedByCurrentUser = isLikedByCurrentUser(oeuvre);
		boolean favoriByCurrentUser = isFavoriByCurrentUser(oeuvre);

		HBox statsRow = new HBox(14);
		statsRow.getStyleClass().add("oeuvre-post-stats");
		statsRow.getChildren().addAll(
				buildReactionButton(
						"M12.1 18.55 10.55 17.14C5.4 12.47 2 9.39 2 5.6 2 2.52 4.42 0 7.5 0c1.74 0 3.41.81 4.5 2.09C13.09.81 14.76 0 16.5 0 19.58 0 22 2.52 22 5.6c0 3.79-3.4 6.87-8.55 11.55z",
						likesCount,
						likedByCurrentUser,
						() -> onToggleLike(oeuvre),
						"oeuvre-post-like-active",
						"#dc2626"
				),
				buildStatChip("M20 2H4c-1.1 0-1.99.9-1.99 2L2 22l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z", comments.size()),
				buildReactionButton(
						"M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z",
						favorisCount,
						favoriByCurrentUser,
						() -> onToggleFavori(oeuvre),
						"oeuvre-post-favori-active",
						"#eab308"
				)
		);

		VBox commentsPreviewBox = buildCommentsPreview(oeuvre, comments);
		card.getChildren().addAll(topRow, titleLabel, descLabel, tagsRow, imageWrapper, statsRow, commentsPreviewBox);
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

	private Button buildReactionButton(String iconPath,
									 int count,
									 boolean active,
									 Runnable action,
									 String activeStyleClass,
									 String activeColor) {
		Button button = new Button();
		button.getStyleClass().addAll("oeuvre-post-stat-chip", "oeuvre-post-stat-button");
		if (active) {
			button.getStyleClass().add(activeStyleClass);
		}

		String iconColor = active ? activeColor : "#6b7280";
		SVGPath icon = createColoredIcon(iconPath, 0.55, iconColor);
		icon.getStyleClass().add("oeuvre-post-stat-icon");

		Label countLabel = new Label(String.valueOf(Math.max(0, count)));
		countLabel.getStyleClass().add("oeuvre-post-stat-count");
		if (active) {
			countLabel.setStyle("-fx-text-fill: " + iconColor + ";");
		}

		HBox content = new HBox(6, icon, countLabel);
		content.setAlignment(Pos.CENTER_LEFT);
		button.setGraphic(content);
		button.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
		button.setOnAction(event -> action.run());

		if (!isCurrentUserAmateur()) {
			button.setDisable(true);
		}

		return button;
	}

	private boolean isCurrentUserAmateur() {
		User current = UserSession.getCurrentUser();
		if (current == null || current.getRole() == null) {
			return false;
		}
		return "amateur".equalsIgnoreCase(current.getRole().trim());
	}

	private int getLikesCount(Oeuvre oeuvre) {
		if (oeuvre == null || oeuvre.getId() == null) {
			return 0;
		}
		return likeService.countLikesByOeuvre(oeuvre.getId());
	}

	private int getFavorisCount(Oeuvre oeuvre) {
		if (oeuvre == null || oeuvre.getId() == null) {
			return 0;
		}
		return likeService.countFavorisByOeuvre(oeuvre.getId());
	}

	private boolean isLikedByCurrentUser(Oeuvre oeuvre) {
		if (oeuvre == null || oeuvre.getId() == null || currentUserId == null) {
			return false;
		}
		return likeService.isLiked(currentUserId, oeuvre.getId());
	}

	private boolean isFavoriByCurrentUser(Oeuvre oeuvre) {
		if (oeuvre == null || oeuvre.getId() == null || currentUserId == null) {
			return false;
		}
		return likeService.isFavori(currentUserId, oeuvre.getId());
	}

	private void onToggleLike(Oeuvre oeuvre) {
		if (!canReact(oeuvre)) {
			return;
		}
		try {
			likeService.toggleLike(currentUserId, oeuvre.getId());
			loadOeuvres();
		} catch (Exception e) {
			showReactionError(e);
		}
	}

	private void onToggleFavori(Oeuvre oeuvre) {
		if (!canReact(oeuvre)) {
			return;
		}
		try {
			likeService.toggleFavori(currentUserId, oeuvre.getId());
			loadOeuvres();
		} catch (Exception e) {
			showReactionError(e);
		}
	}

	private boolean canReact(Oeuvre oeuvre) {
		currentUserId = UserSession.getCurrentUserId();
		if (currentUserId == null || oeuvre == null || oeuvre.getId() == null) {
			showReactionMessage("Veuillez vous connecter pour interagir.");
			return false;
		}
		if (!isCurrentUserAmateur()) {
			showReactionMessage("Cette action est reservee au profil amateur.");
			return false;
		}
		return true;
	}

	private void showReactionError(Exception e) {
		String message = e == null || e.getMessage() == null || e.getMessage().isBlank()
				? "Action impossible pour le moment."
				: e.getMessage();
		showReactionMessage(message);
	}

	private void showReactionMessage(String message) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Information");
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
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
		currentUserId = UserSession.getCurrentUserId();
		if (currentUserId == null) {
			showCommentError(errorLabel, "Veuillez vous connecter pour commenter.");
			return;
		}

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
			commentaire.setUserId(currentUserId);
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

		if (comment.getUserId() != null && comment.getUserId().equals(currentUserId)) {
			Button menuTrigger = new Button("...");
			menuTrigger.getStyleClass().add("oeuvre-post-comment-menu-trigger");
			menuTrigger.setFocusTraversable(false);

			ContextMenu menu = buildCommentActionsMenu(comment, textLabel, body, dateLabel);
			menuTrigger.setOnAction(event -> {
				if (menu.isShowing()) {
					menu.hide();
				} else {
					menu.show(menuTrigger, Side.BOTTOM, 0, 4);
				}
			});

			header.getChildren().add(menuTrigger);
		}

		body.getChildren().addAll(header, textLabel);
		row.getChildren().addAll(avatar, body);
		return row;
	}

	private ContextMenu buildCommentActionsMenu(Commentaire comment, Label textLabel, VBox body, Label dateLabel) {
		MenuItem editItem = new MenuItem("Modifier");
		editItem.getStyleClass().add("comment-menu-edit");
		editItem.setGraphic(createColoredIcon("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25z", 0.48, "#6b7280"));
		editItem.setOnAction(event -> activateInlineEdit(comment, textLabel, body, dateLabel));

		MenuItem deleteItem = new MenuItem("Supprimer");
		deleteItem.getStyleClass().add("comment-menu-delete");
		deleteItem.setGraphic(createColoredIcon("M6 7h12v2H6V7zm2 3h8v10H8V10zm3-6h2l1 1h4v2H6V5h4l1-1z", 0.48, "#dc2626"));
		deleteItem.setOnAction(event -> onDeleteComment(comment));

		ContextMenu menu = new ContextMenu(editItem, deleteItem);
		menu.getStyleClass().add("comment-menu");
		return menu;
	}

	private void activateInlineEdit(Commentaire comment, Label textLabel, VBox body, Label dateLabel) {
		TextField editField = new TextField(fallback(comment.getTexte(), ""));
		editField.getStyleClass().add("oeuvre-post-comment-edit-input");
		HBox.setHgrow(editField, Priority.ALWAYS);

		Button saveButton = new Button("Enregistrer");
		saveButton.getStyleClass().addAll("oeuvre-post-comment-action", "oeuvre-post-comment-action-save");

		Button cancelButton = new Button("Annuler");
		cancelButton.getStyleClass().addAll("oeuvre-post-comment-action", "oeuvre-post-comment-action-cancel");

		HBox editRow = new HBox(8, editField, saveButton, cancelButton);
		editRow.setAlignment(Pos.CENTER_LEFT);
		editRow.getStyleClass().add("oeuvre-post-comment-edit-row");

		int textIndex = body.getChildren().indexOf(textLabel);
		if (textIndex >= 0) {
			body.getChildren().set(textIndex, editRow);
		}

		Runnable cancelEdit = () -> {
			int editIndex = body.getChildren().indexOf(editRow);
			if (editIndex >= 0) {
				body.getChildren().set(editIndex, textLabel);
			}
		};

		Runnable saveEdit = () -> {
			String updatedText = safeText(editField.getText());
			if (updatedText.isEmpty()) {
				cancelEdit.run();
				return;
			}

			try {
				comment.setTexte(updatedText);
				comment.setDateCommentaire(LocalDate.now());
				commentaireService.update(comment);
				textLabel.setText(updatedText);
				dateLabel.setText(formatCommentDate(comment.getDateCommentaire()));
				cancelEdit.run();
			} catch (Exception e) {
				Alert error = new Alert(Alert.AlertType.ERROR);
				error.setTitle("Erreur");
				error.setHeaderText("Modification impossible");
				error.setContentText(e.getMessage() == null ? "Une erreur est survenue." : e.getMessage());
				error.showAndWait();
			}
		};

		saveButton.setOnAction(event -> saveEdit.run());
		cancelButton.setOnAction(event -> cancelEdit.run());
		editField.setOnAction(event -> saveEdit.run());
	}

	private void onDeleteComment(Commentaire comment) {
		if (comment == null || comment.getId() == null) {
			return;
		}

		Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
		confirmation.setTitle("Supprimer le commentaire");
		confirmation.setHeaderText(null);
		confirmation.setContentText("Voulez-vous supprimer ce commentaire ?");

		Optional<ButtonType> result = confirmation.showAndWait();
		if (result.isEmpty() || result.get() != ButtonType.OK) {
			return;
		}

		try {
			commentaireService.delete(comment);
			loadOeuvres();
		} catch (Exception e) {
			Alert error = new Alert(Alert.AlertType.ERROR);
			error.setTitle("Erreur");
			error.setHeaderText("Suppression impossible");
			error.setContentText(e.getMessage() == null ? "Une erreur est survenue." : e.getMessage());
			error.showAndWait();
		}
	}

	private SVGPath createColoredIcon(String path, double scale, String fillColor) {
		SVGPath icon = new SVGPath();
		icon.setContent(path);
		icon.setScaleX(scale);
		icon.setScaleY(scale);
		icon.setStyle("-fx-fill: " + fillColor + ";");
		return icon;
	}

	private ImageView createImageViewFromSource(String imageSource) {
		String safeSource = safeText(imageSource);
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

	private String getCollectionHashtag(Oeuvre oeuvre) {
		if (oeuvre == null || oeuvre.getCollectionId() == null) {
			return "";
		}

		Integer collectionId = oeuvre.getCollectionId();
		if (collectionHashtagById.containsKey(collectionId)) {
			return collectionHashtagById.get(collectionId);
		}

		String hashtag = "";
		try {
			CollectionOeuvre collection = getCollectionById(collectionId);
			if (collection != null) {
				hashtag = toHashtag(collection.getTitre());
			}
		} catch (Exception ignored) {
			hashtag = "";
		}

		collectionHashtagById.put(collectionId, hashtag);
		return hashtag;
	}

	private CollectionOeuvre getCollectionById(Integer collectionId) throws Exception {
		if (collectionId == null) {
			return null;
		}
		if (collectionById.containsKey(collectionId)) {
			return collectionById.get(collectionId);
		}
		CollectionOeuvre collection = oeuvreCollectionService.getCollectionById(collectionId);
		collectionById.put(collectionId, collection);
		return collection;
	}

	private User loadArtistForOeuvre(Oeuvre oeuvre) {
		if (oeuvre == null || oeuvre.getCollectionId() == null) {
			return null;
		}

		try {
			CollectionOeuvre collection = getCollectionById(oeuvre.getCollectionId());
			if (collection == null || collection.getArtisteId() == null) {
				return null;
			}

			Integer artisteId = collection.getArtisteId();
			if (artistById.containsKey(artisteId)) {
				return artistById.get(artisteId);
			}

			User artist = oeuvreService.getUserById(artisteId);
			artistById.put(artisteId, artist);
			return artist;
		} catch (Exception ignored) {
			return null;
		}
	}

	private String buildArtistName(User artist) {
		if (artist == null) {
			return "Artiste";
		}
		String prenom = safeText(artist.getPrenom());
		String nom = safeText(artist.getNom());
		String fullName = (prenom + " " + nom).trim();
		return fullName.isEmpty() ? "Artiste" : fullName;
	}

	private String buildArtistSpecialite(User artist) {
		if (artist == null) {
			return "Createur";
		}
		String specialite = safeText(artist.getSpecialite());
		return specialite.isEmpty() ? "Createur" : specialite;
	}

	private void applySearch() {
		List<Oeuvre> source = isRecommendationRoute(currentRouteFilter) ? recommendedOeuvres : allOeuvres;
		List<Oeuvre> filtered = new ArrayList<>();

		for (Oeuvre oeuvre : source) {
			boolean keywordOk = true;
			boolean routeOk = matchesRouteFilter(oeuvre);

			if (keywordOk && routeOk) {
				filtered.add(oeuvre);
			}
		}

		renderOeuvres(filtered);
	}

	private boolean matchesRouteFilter(Oeuvre oeuvre) {
		String type = safeText(oeuvre == null ? "" : oeuvre.getType()).toLowerCase(Locale.ROOT);
		return switch (currentRouteFilter) {
			case "favoris" -> isFavoriByCurrentUser(oeuvre);
			case "favoris-peintures" -> isFavoriByCurrentUser(oeuvre) && type.contains("peint");
			case "favoris-sculptures" -> isFavoriByCurrentUser(oeuvre) && type.contains("sculpt");
			case "favoris-photos" -> isFavoriByCurrentUser(oeuvre) && type.contains("photo");
			case "feed-peintures" -> type.contains("peint");
			case "feed-sculptures" -> type.contains("sculpt");
			case "feed-photos" -> type.contains("photo");
			case "feed-recommandations", "favoris-recommandations" -> true;
			case "feed" -> true;
			default -> true;
		};
	}

	private boolean isRecommendationRoute(String route) {
		return "feed-recommandations".equals(route) || "favoris-recommandations".equals(route);
	}
}

