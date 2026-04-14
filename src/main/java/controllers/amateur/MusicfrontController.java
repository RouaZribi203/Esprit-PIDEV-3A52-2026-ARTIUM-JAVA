package controllers.amateur;

import Services.PlaylistService;
import Services.MusiqueService;
import entities.Musique;
import entities.Playlist;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.sql.SQLDataException;
import java.util.Locale;

public class MusicfrontController {

	private static MusicfrontController activeController;

	@FXML
	private TextField searchField;

	@FXML
	private TilePane musicGrid;

	@FXML
	private Label emptyListLabel;

	@FXML
	private Label nowPlayingTitleLabel;

	@FXML
	private Label nowPlayingMetaLabel;

	@FXML
	private Label playerStatusLabel;

	@FXML
	private Button playPauseButton;

	@FXML
	private Button musiqueSectionButton;

	@FXML
	private Button playlistsSectionButton;

	@FXML
	private VBox musiqueSection;

	@FXML
	private VBox playlistsSection;

	@FXML
	private TextField playlistNameField;

	@FXML
	private TextArea playlistDescriptionArea;

	@FXML
	private TextField playlistImageField;

	@FXML
	private Button createPlaylistButton;

	@FXML
	private Button togglePlaylistFormButton;

	@FXML
	private VBox playlistFormSection;

	@FXML
	private Label playlistFeedbackLabel;

	@FXML
	private VBox playlistDetailSection;

	@FXML
	private Label playlistDetailTitleLabel;

	@FXML
	private Label playlistDetailMetaLabel;

	@FXML
	private Button closePlaylistDetailButton;

	@FXML
	private Label playlistSongsEmptyLabel;

	@FXML
	private VBox playlistSongsBox;

	@FXML
	private TilePane playlistGrid;

	private final MusiqueService musiqueService = new MusiqueService();
	private final PlaylistService playlistService = new PlaylistService();
	private final ObservableList<Musique> allTracks = FXCollections.observableArrayList();
	private final ObservableList<Musique> visibleTracks = FXCollections.observableArrayList();
	private final ObservableList<Playlist> playlists = FXCollections.observableArrayList();
	private Integer selectedPlaylistId;
	private Integer editingPlaylistId;
	private java.time.LocalDate editingPlaylistDateCreation;
	private byte[] editingPlaylistImageBytes;
	private static final java.util.Set<String> IMAGE_EXTENSIONS = new java.util.HashSet<>(java.util.Arrays.asList("png", "jpg", "jpeg"));
	private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;

	private MediaPlayer mediaPlayer;
	private int currentTrackIndex = -1;

	@FXML
	public void initialize() {
		activeController = this;
		searchField.textProperty().addListener((obs, oldValue, newValue) -> filterTracks(newValue));
		if (createPlaylistButton != null) {
			createPlaylistButton.setText("Créer la playlist");
		}
		if (closePlaylistDetailButton != null) {
			closePlaylistDetailButton.setText("Fermer");
		}
		setActiveSection(true);
		hidePlaylistForm();
		hidePlaylistDetails();
		setPlaylistFeedback("Créez une playlist puis cliquez sur + Playlist depuis une musique.", false);
		refreshTracks();
		refreshPlaylists();
	}

	@FXML
	private void handleSearch() {
		filterTracks(searchField.getText());
	}

	@FXML
	private void handleClearSearch() {
		searchField.clear();
		filterTracks(null);
	}

	@FXML
	private void handleShowMusiqueSection() {
		setActiveSection(true);
	}

	@FXML
	private void handleShowPlaylistsSection() {
		setActiveSection(false);
		if (playlistDetailSection != null && playlists.isEmpty()) {
			hidePlaylistDetails();
		}
	}

	@FXML
	private void handleClosePlaylistDetail() {
		hidePlaylistDetails();
	}

	@FXML
	private void handleEditSelectedPlaylist() {
		Playlist playlist = findPlaylistById(selectedPlaylistId);
		if (playlist == null) {
			setPlaylistFeedback("Sélectionnez d'abord une playlist à modifier.", false);
			return;
		}

		editingPlaylistId = playlist.getId();
		editingPlaylistDateCreation = playlist.getDateCreation();
		editingPlaylistImageBytes = playlist.getImage();

		if (playlistNameField != null) {
			playlistNameField.setText(playlist.getNom() != null ? playlist.getNom() : "");
		}
		if (playlistDescriptionArea != null) {
			playlistDescriptionArea.setText(playlist.getDescription() != null ? playlist.getDescription() : "");
		}
		if (playlistImageField != null) {
			playlistImageField.clear();
		}

		if (createPlaylistButton != null) {
			createPlaylistButton.setText("Enregistrer");
		}
		if (togglePlaylistFormButton != null) {
			togglePlaylistFormButton.setText("Fermer le formulaire");
		}
		if (playlistFormSection != null) {
			playlistFormSection.setVisible(true);
			playlistFormSection.setManaged(true);
		}
		setPlaylistFeedback("Mode modification de playlist activé.", true);
	}

	@FXML
	private void handleDeleteSelectedPlaylist() {
		Playlist playlist = findPlaylistById(selectedPlaylistId);
		if (playlist == null) {
			setPlaylistFeedback("Sélectionnez d'abord une playlist à supprimer.", false);
			return;
		}

		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
		confirm.setTitle("Supprimer la playlist");
		confirm.setHeaderText("Supprimer \"" + safePlaylistName(playlist) + "\" ?");
		confirm.setContentText("Cette action est définitive.");

		java.util.Optional<ButtonType> result = confirm.showAndWait();
		if (result.isEmpty() || result.get() != ButtonType.OK) {
			return;
		}

		try {
			playlistService.delete(playlist);
			editingPlaylistId = null;
			editingPlaylistDateCreation = null;
			editingPlaylistImageBytes = null;
			clearPlaylistForm();
			hidePlaylistForm();
			hidePlaylistDetails();
			refreshPlaylists();
			setPlaylistFeedback("Playlist supprimée avec succès.", true);
		} catch (SQLDataException e) {
			setPlaylistFeedback("Erreur lors de la suppression: " + e.getMessage(), false);
		}
	}

	@FXML
	private void handleChoosePlaylistImage() {
		javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
		chooser.setTitle("Choisir une image de playlist");
		chooser.getExtensionFilters().addAll(
				new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
		);

		File file = chooser.showOpenDialog(playlistImageField.getScene().getWindow());
		if (file != null) {
			String validationError = validateImagePath(file.getAbsolutePath());
			if (validationError != null) {
				playlistImageField.clear();
				setPlaylistFeedback(validationError, false);
				return;
			}
			playlistImageField.setText(file.getAbsolutePath());
		}
	}

	@FXML
	private void handleCreatePlaylist() {
		String nom = playlistNameField != null && playlistNameField.getText() != null ? playlistNameField.getText().trim() : "";
		String description = playlistDescriptionArea != null && playlistDescriptionArea.getText() != null ? playlistDescriptionArea.getText().trim() : "";
		String imagePath = playlistImageField != null && playlistImageField.getText() != null ? playlistImageField.getText().trim() : "";

		if (nom.length() < 3 || nom.length() > 255) {
			setPlaylistFeedback("Le nom de la playlist doit contenir entre 3 et 255 caractères.", false);
			return;
		}
		if (!description.isEmpty() && (description.length() < 10 || description.length() > 5000)) {
			setPlaylistFeedback("La description doit contenir entre 10 et 5000 caractères.", false);
			return;
		}

		String imageValidationError = validateImagePath(imagePath);
		if (imageValidationError != null) {
			setPlaylistFeedback(imageValidationError, false);
			return;
		}

		Playlist playlist = new Playlist();
		boolean editMode = editingPlaylistId != null;
		playlist.setId(editingPlaylistId);
		playlist.setNom(nom);
		playlist.setDescription(description.isEmpty() ? null : description);
		playlist.setDateCreation(editMode && editingPlaylistDateCreation != null ? editingPlaylistDateCreation : java.time.LocalDate.now());

		if (!imagePath.isEmpty()) {
			try {
				playlist.setImage(java.nio.file.Files.readAllBytes(new File(imagePath).toPath()));
			} catch (java.io.IOException e) {
				setPlaylistFeedback("Impossible de lire l'image selectionnee.", false);
				return;
			}
		} else if (editMode) {
			playlist.setImage(editingPlaylistImageBytes);
		}

		try {
			if (editMode) {
				playlistService.update(playlist);
				setPlaylistFeedback("Playlist modifiée avec succès.", true);
			} else {
				playlistService.add(playlist);
				setPlaylistFeedback("Playlist créée avec succès.", true);
			}
			clearPlaylistForm();
			editingPlaylistId = null;
			editingPlaylistDateCreation = null;
			editingPlaylistImageBytes = null;
			hidePlaylistForm();
			refreshPlaylists();
		} catch (SQLDataException e) {
			setPlaylistFeedback("Erreur lors de la création: " + e.getMessage(), false);
		}
	}

	@FXML
	private void handleTogglePlaylistForm() {
		if (playlistFormSection == null) {
			return;
		}

		boolean shouldShow = !playlistFormSection.isVisible();
		playlistFormSection.setVisible(shouldShow);
		playlistFormSection.setManaged(shouldShow);
		if (togglePlaylistFormButton != null) {
			togglePlaylistFormButton.setText(shouldShow ? "Fermer le formulaire" : "Nouvelle playlist");
		}
		if (!shouldShow) {
			editingPlaylistId = null;
			editingPlaylistDateCreation = null;
			editingPlaylistImageBytes = null;
			if (createPlaylistButton != null) {
				createPlaylistButton.setText("Créer la playlist");
			}
		}
	}

	@FXML
	private void handlePlayPause() {
		if (mediaPlayer == null) {
			int selectedIndex = currentTrackIndex;
			if (selectedIndex < 0 && !visibleTracks.isEmpty()) {
				selectedIndex = 0;
			}
			if (selectedIndex >= 0) {
				playTrackAtIndex(selectedIndex);
			}
			return;
		}

		MediaPlayer.Status status = mediaPlayer.getStatus();
		if (status == MediaPlayer.Status.PLAYING) {
			mediaPlayer.pause();
			setPlayPauseText("Play");
			setPlayerStatusText("En pause");
		} else {
			mediaPlayer.play();
			setPlayPauseText("Pause");
			setPlayerStatusText("Lecture en cours");
		}
	}

	@FXML
	private void handlePreviousTrack() {
		if (visibleTracks.isEmpty()) {
			return;
		}
		int targetIndex = currentTrackIndex <= 0 ? visibleTracks.size() - 1 : currentTrackIndex - 1;
		playTrackAtIndex(targetIndex);
	}

	@FXML
	private void handleNextTrack() {
		if (visibleTracks.isEmpty()) {
			return;
		}
		int targetIndex = currentTrackIndex >= visibleTracks.size() - 1 ? 0 : currentTrackIndex + 1;
		playTrackAtIndex(targetIndex);
	}

	private void refreshPlaylists() {
		try {
			playlists.setAll(playlistService.getAll());
			renderPlaylistGrid();
			if (playlistDetailSection != null && playlistDetailSection.isVisible()) {
				Playlist refreshed = findPlaylistById(selectedPlaylistId);
				if (refreshed != null) {
					showPlaylistDetails(refreshed);
				} else {
					hidePlaylistDetails();
				}
			}
		} catch (SQLDataException e) {
			playlists.clear();
			renderPlaylistGrid();
			hidePlaylistDetails();
			setPlaylistFeedback("Impossible de charger les playlists: " + e.getMessage(), false);
		}
	}

	private void refreshTracks() {
		try {
			allTracks.setAll(musiqueService.getAll());
			filterTracks(searchField != null ? searchField.getText() : null);
		} catch (SQLDataException e) {
			allTracks.clear();
			visibleTracks.clear();
			renderGrid();
			emptyListLabel.setText("Impossible de charger les musiques: " + e.getMessage());
			emptyListLabel.setVisible(true);
			emptyListLabel.setManaged(true);
			stopPlayer();
		}
	}

	private void filterTracks(String query) {
		String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

		visibleTracks.setAll(allTracks.stream()
				.filter(track -> normalizedQuery.isEmpty() || matchesQuery(track, normalizedQuery))
				.toList());

		if (visibleTracks.isEmpty()) {
			currentTrackIndex = -1;
			stopPlayer();
			setNowPlayingTitle("Selectionnez une musique");
			setNowPlayingMeta("Genre: -");
			setPlayerStatusText("Pret");
			setPlayPauseText("Play");
		} else if (currentTrackIndex >= visibleTracks.size()) {
			currentTrackIndex = -1;
		}

		renderGrid();
		emptyListLabel.setVisible(visibleTracks.isEmpty());
		emptyListLabel.setManaged(visibleTracks.isEmpty());

		if (!visibleTracks.isEmpty() && currentTrackIndex < 0) {
			updateNowPlayingLabels(visibleTracks.get(0));
		}
	}

	private boolean matchesQuery(Musique musique, String query) {
		String titre = musique.getTitre() != null ? musique.getTitre().toLowerCase(Locale.ROOT) : "";
		String description = musique.getDescription() != null ? musique.getDescription().toLowerCase(Locale.ROOT) : "";
		String genre = musique.getGenre() != null ? musique.getGenre().toLowerCase(Locale.ROOT) : "";
		return titre.contains(query) || description.contains(query) || genre.contains(query);
	}

	private void playTrackAtIndex(int index) {
		if (index < 0 || index >= visibleTracks.size()) {
			return;
		}

		Musique selectedTrack = visibleTracks.get(index);
		String source = toMediaSource(selectedTrack.getAudio());
		if (source == null) {
			setPlayerStatusText("Fichier audio introuvable");
			return;
		}

		stopPlayer();
		try {
			Media media = new Media(source);
			mediaPlayer = new MediaPlayer(media);
			currentTrackIndex = index;
			renderGrid();
			updateNowPlayingLabels(selectedTrack);

			mediaPlayer.setOnReady(() -> {
				mediaPlayer.play();
				setPlayPauseText("Pause");
				setPlayerStatusText("Lecture en cours");
			});
			mediaPlayer.setOnEndOfMedia(this::handleNextTrack);
			mediaPlayer.setOnError(() -> setPlayerStatusText(describeMediaError(mediaPlayer.getError())));
		} catch (MediaException mediaException) {
			setPlayPauseText("Play");
			setPlayerStatusText(describeMediaError(mediaException));
		} catch (RuntimeException runtimeException) {
			setPlayPauseText("Play");
			setPlayerStatusText("Impossible de lire ce fichier audio");
		}
	}

	private String describeMediaError(Throwable throwable) {
		if (throwable instanceof MediaException mediaException) {
			if (mediaException.getType() == MediaException.Type.MEDIA_UNSUPPORTED) {
				return "Format non pris en charge par JavaFX (ex: OPUS)";
			}
			String message = mediaException.getMessage();
			if (message != null && message.toLowerCase(Locale.ROOT).contains("unrecognized file signature")) {
				return "Codec audio non pris en charge (essayez MP3/M4A/WAV)";
			}
		}
		return "Impossible de lire ce fichier audio";
	}

	private String validateImagePath(String imagePath) {
		if (imagePath == null || imagePath.isBlank()) {
			return null;
		}

		File imageFile = new File(imagePath.trim());
		if (!imageFile.exists() || !imageFile.isFile()) {
			return "Image introuvable. Veuillez choisir un fichier valide.";
		}
		if (!imageFile.canRead()) {
			return "Image non lisible. Verifiez les permissions du fichier.";
		}

		String fileName = imageFile.getName();
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
			return "Format image non supporté. Utilisez JPEG ou PNG.";
		}

		String extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
		if (!IMAGE_EXTENSIONS.contains(extension)) {
			return "Format image non supporté. Utilisez JPEG ou PNG.";
		}

		if (imageFile.length() > MAX_IMAGE_SIZE_BYTES) {
			return "Image trop volumineuse. Taille max: 5 MB.";
		}

		return null;
	}

	private void updateNowPlayingLabels(Musique musique) {
		String titre = musique.getTitre() != null ? musique.getTitre() : "Sans titre";
		String genre = musique.getGenre() != null ? musique.getGenre() : "-";
		setNowPlayingTitle(titre);
		setNowPlayingMeta("Genre: " + genre);
	}

	private String toMediaSource(String audioPath) {
		if (audioPath == null || audioPath.isBlank()) {
			return null;
		}

		String trimmed = audioPath.trim();
		if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("file:/")) {
			return trimmed;
		}

		File file = new File(trimmed);
		if (!file.exists()) {
			return null;
		}
		return file.toURI().toString();
	}

	private void stopPlayer() {
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.dispose();
			mediaPlayer = null;
		}
	}

	private void renderGrid() {
		musicGrid.getChildren().clear();
		for (int i = 0; i < visibleTracks.size(); i++) {
			Musique musique = visibleTracks.get(i);
			musicGrid.getChildren().add(createTrackCard(musique, i));
		}
	}

	private void renderPlaylistGrid() {
		playlistGrid.getChildren().clear();
		for (Playlist playlist : playlists) {
			playlistGrid.getChildren().add(createPlaylistCard(playlist));
		}
	}

	private void clearPlaylistForm() {
		if (playlistNameField != null) {
			playlistNameField.clear();
		}
		if (playlistDescriptionArea != null) {
			playlistDescriptionArea.clear();
		}
		if (playlistImageField != null) {
			playlistImageField.clear();
		}
	}

	private void hidePlaylistForm() {
		if (playlistFormSection != null) {
			playlistFormSection.setVisible(false);
			playlistFormSection.setManaged(false);
		}
		if (togglePlaylistFormButton != null) {
			togglePlaylistFormButton.setText("Nouvelle playlist");
		}
		if (createPlaylistButton != null) {
			createPlaylistButton.setText(editingPlaylistId != null ? "Enregistrer" : "Créer la playlist");
		}
	}

	private void setActiveSection(boolean showMusique) {
		if (musiqueSection != null) {
			musiqueSection.setVisible(showMusique);
			musiqueSection.setManaged(showMusique);
		}
		if (playlistsSection != null) {
			playlistsSection.setVisible(!showMusique);
			playlistsSection.setManaged(!showMusique);
		}

		if (musiqueSectionButton != null) {
			musiqueSectionButton.setStyle(showMusique
					? "-fx-background-color: #198754; -fx-text-fill: white;"
					: "-fx-background-color: #4b5563; -fx-text-fill: white;");
		}
		if (playlistsSectionButton != null) {
			playlistsSectionButton.setStyle(!showMusique
					? "-fx-background-color: #198754; -fx-text-fill: white;"
					: "-fx-background-color: #4b5563; -fx-text-fill: white;");
		}
	}

	private void setPlaylistFeedback(String message, boolean success) {
		if (playlistFeedbackLabel == null) {
			return;
		}
		playlistFeedbackLabel.setText(message);
		playlistFeedbackLabel.setStyle(success ? "-fx-text-fill: #198754;" : "-fx-text-fill: #b00020;");
	}

	private void showPlaylistDetails(Playlist playlist) {
		if (playlist == null) {
			hidePlaylistDetails();
			return;
		}

		selectedPlaylistId = playlist.getId();

		if (playlistDetailSection != null) {
			playlistDetailSection.setVisible(true);
			playlistDetailSection.setManaged(true);
		}
		if (playlistDetailTitleLabel != null) {
			playlistDetailTitleLabel.setText(safePlaylistName(playlist));
		}
		if (playlistDetailMetaLabel != null) {
			playlistDetailMetaLabel.setText((playlist.getDescription() != null && !playlist.getDescription().isBlank() ? playlist.getDescription() + " · " : "")
					+ (playlist.getMusiques() != null ? playlist.getMusiques().size() : 0) + " musique(s)");
		}

		if (playlistSongsBox != null) {
			playlistSongsBox.getChildren().clear();
			java.util.List<Musique> musiques = fetchPlaylistTracks(playlist);
			boolean empty = musiques.isEmpty();
			if (playlistSongsEmptyLabel != null) {
				playlistSongsEmptyLabel.setVisible(empty);
				playlistSongsEmptyLabel.setManaged(empty);
			}
			for (Musique track : musiques) {
				HBox row = new HBox(8);
				Label name = new Label(track.getTitre() != null ? track.getTitre() : "Sans titre");
				name.setStyle("-fx-text-fill: white; -fx-font-weight: 700;");
				Button playBtn = new Button("Play");
				playBtn.setOnAction(event -> playTrackFromPlaylist(track));
				row.getChildren().addAll(name, playBtn);
				playlistSongsBox.getChildren().add(row);
			}
		}
	}

	private java.util.List<Musique> fetchPlaylistTracks(Playlist playlist) {
		try {
			if (playlist != null && playlist.getId() != null) {
				java.util.List<Musique> musiques = playlistService.getMusiquesForPlaylist(playlist.getId());
				playlist.setMusiques(musiques);
				return musiques;
			}
		} catch (SQLDataException e) {
			setPlaylistFeedback("Impossible de charger les musiques de la playlist: " + e.getMessage(), false);
		}
		return java.util.List.of();
	}

	private Playlist findPlaylistById(Integer playlistId) {
		if (playlistId == null) {
			return null;
		}
		for (Playlist playlist : playlists) {
			if (playlist.getId() != null && playlist.getId().equals(playlistId)) {
				return playlist;
			}
		}
		return null;
	}

	private void hidePlaylistDetails() {
		selectedPlaylistId = null;
		editingPlaylistId = null;
		editingPlaylistDateCreation = null;
		editingPlaylistImageBytes = null;
		if (playlistDetailSection != null) {
			playlistDetailSection.setVisible(false);
			playlistDetailSection.setManaged(false);
		}
		if (playlistSongsBox != null) {
			playlistSongsBox.getChildren().clear();
		}
		if (playlistSongsEmptyLabel != null) {
			playlistSongsEmptyLabel.setVisible(true);
			playlistSongsEmptyLabel.setManaged(true);
		}
		if (playlistDetailTitleLabel != null) {
			playlistDetailTitleLabel.setText("Aucune playlist sélectionnée");
		}
		if (playlistDetailMetaLabel != null) {
			playlistDetailMetaLabel.setText("Cliquez sur une playlist pour voir ses musiques.");
		}
	}

	private void setNowPlayingTitle(String text) {
		if (nowPlayingTitleLabel != null) {
			nowPlayingTitleLabel.setText(text);
		}
	}

	private void setNowPlayingMeta(String text) {
		if (nowPlayingMetaLabel != null) {
			nowPlayingMetaLabel.setText(text);
		}
	}

	private void setPlayerStatusText(String text) {
		if (playerStatusLabel != null) {
			playerStatusLabel.setText(text);
		}
	}

	private void setPlayPauseText(String text) {
		if (playPauseButton != null) {
			playPauseButton.setText(text);
		}
	}

	private void addMusicToPlaylistWithChoice(Musique musique) {
		if (playlists.isEmpty()) {
			setPlaylistFeedback("Créez d'abord une playlist.", false);
			return;
		}
		if (musique == null || musique.getId() == null) {
			setPlaylistFeedback("Musique invalide.", false);
			return;
		}

		java.util.List<PlaylistChoice> choices = playlists.stream()
				.filter(playlist -> playlist.getId() != null)
				.map(playlist -> new PlaylistChoice(playlist.getId(), safePlaylistName(playlist)))
				.toList();

		if (choices.isEmpty()) {
			setPlaylistFeedback("Aucune playlist valide disponible.", false);
			return;
		}

		ChoiceDialog<PlaylistChoice> dialog = new ChoiceDialog<>(choices.get(0), choices);
		dialog.setTitle("Ajouter à une playlist");
		dialog.setHeaderText("Choisissez une playlist");
		dialog.setContentText("Playlist:");

		java.util.Optional<PlaylistChoice> selected = dialog.showAndWait();
		if (selected.isEmpty()) {
			return;
		}

		try {
			playlistService.addMusiqueToPlaylist(selected.get().id(), musique.getId());
			refreshPlaylists();
			setPlaylistFeedback("Musique ajoutée à \"" + selected.get().name() + "\" avec succès.", true);
		} catch (SQLDataException e) {
			setPlaylistFeedback("Erreur lors de l'ajout à la playlist: " + e.getMessage(), false);
		}
	}

	private VBox createPlaylistCard(Playlist playlist) {
		VBox card = new VBox(6);
		card.setPrefWidth(180);
		card.setMaxWidth(180);
		card.setStyle("-fx-background-color: #212529; -fx-background-radius: 8; -fx-padding: 8;");

		Node coverNode = buildPlaylistCoverNode(playlist.getImage());
		Label nameLabel = new Label(safePlaylistName(playlist));
		nameLabel.setWrapText(true);
		nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

		Label descriptionLabel = new Label(playlist.getDescription() != null && !playlist.getDescription().isBlank()
				? playlist.getDescription()
				: "Aucune description");
		descriptionLabel.setWrapText(true);
		descriptionLabel.setStyle("-fx-text-fill: #cbd5e1;");

		Label countLabel = new Label((playlist.getMusiques() != null ? playlist.getMusiques().size() : 0) + " musique(s)");
		countLabel.setStyle("-fx-text-fill: #9ca3af;");

		Button openButton = new Button("Ouvrir");
		openButton.setOnAction(event -> {
			event.consume();
			showPlaylistDetails(playlist);
		});

		card.setOnMouseClicked(event -> showPlaylistDetails(playlist));
		card.getChildren().addAll(coverNode, nameLabel, descriptionLabel, countLabel, openButton);
		return card;
	}


	private void playTrackFromPlaylist(Musique track) {
		if (track == null) {
			return;
		}

		int index = -1;
		for (int i = 0; i < visibleTracks.size(); i++) {
			Musique candidate = visibleTracks.get(i);
			if (candidate.getId() != null && candidate.getId().equals(track.getId())) {
				index = i;
				break;
			}
		}

		if (index >= 0) {
			playTrackAtIndex(index);
			return;
		}

		String source = toMediaSource(track.getAudio());
		if (source == null) {
			setPlayerStatusText("Fichier audio introuvable");
			return;
		}

		stopPlayer();
		try {
			Media media = new Media(source);
			mediaPlayer = new MediaPlayer(media);
			currentTrackIndex = -1;
			renderGrid();
			updateNowPlayingLabels(track);
			mediaPlayer.setOnReady(() -> {
				mediaPlayer.play();
				setPlayPauseText("Pause");
				setPlayerStatusText("Lecture en cours");
			});
			mediaPlayer.setOnError(() -> setPlayerStatusText(describeMediaError(mediaPlayer.getError())));
		} catch (MediaException mediaException) {
			setPlayPauseText("Play");
			setPlayerStatusText(describeMediaError(mediaException));
		}
	}

	private Node buildPlaylistCoverNode(byte[] imageBytes) {
		StackPane placeholder = new StackPane();
		placeholder.setPrefSize(160, 110);
		placeholder.setStyle("-fx-background-color: #2d333b; -fx-background-radius: 6;");

		if (imageBytes == null || imageBytes.length == 0) {
			Label noImageLabel = new Label("Playlist");
			noImageLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-weight: bold;");
			placeholder.getChildren().add(noImageLabel);
			return placeholder;
		}

		try {
			Image image = new Image(new ByteArrayInputStream(imageBytes));
			if (image.isError()) {
				Label noImageLabel = new Label("Playlist");
				noImageLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-weight: bold;");
				placeholder.getChildren().add(noImageLabel);
				return placeholder;
			}

			ImageView imageView = new ImageView(image);
			imageView.setFitWidth(160);
			imageView.setFitHeight(110);
			imageView.setPreserveRatio(false);
			return imageView;
		} catch (Exception ex) {
			Label noImageLabel = new Label("Playlist");
			noImageLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-weight: bold;");
			placeholder.getChildren().add(noImageLabel);
			return placeholder;
		}
	}

	private String safePlaylistName(Playlist playlist) {
		if (playlist == null || playlist.getNom() == null || playlist.getNom().isBlank()) {
			return "Playlist sans nom";
		}
		return playlist.getNom();
	}

	private VBox createTrackCard(Musique musique, int index) {
		VBox card = new VBox(6);
		card.setPrefWidth(170);
		card.setMaxWidth(170);
		card.setStyle(index == currentTrackIndex
				? "-fx-background-color: #212529; -fx-background-radius: 8; -fx-border-color: #198754; -fx-border-radius: 8; -fx-padding: 8;"
				: "-fx-background-color: #212529; -fx-background-radius: 8; -fx-padding: 8;");

		Node coverNode = buildCoverNode(musique.getImage());

		String titre = musique.getTitre() != null ? musique.getTitre() : "Sans titre";
		Label titleLabel = new Label(titre);
		titleLabel.setWrapText(true);
		titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

		String genre = musique.getGenre() != null ? musique.getGenre() : "-";
		Label metaLabel = new Label("Genre: " + genre);
		metaLabel.setStyle("-fx-text-fill: #cbd5e1;");

		Button addToPlaylistButton = new Button("+ Playlist");
		addToPlaylistButton.setOnAction(event -> {
			event.consume();
			addMusicToPlaylistWithChoice(musique);
		});

		HBox actionsRow = new HBox(addToPlaylistButton);

		card.getChildren().addAll(coverNode, titleLabel, metaLabel, actionsRow);
		card.setOnMouseClicked(event -> playTrackAtIndex(index));
		return card;
	}

	private record PlaylistChoice(Integer id, String name) {
		@Override
		public String toString() {
			return name;
		}
	}

	public static MusicfrontController getActiveController() {
		return activeController;
	}

	public void togglePlayPauseFromMini() {
		handlePlayPause();
	}

	public void playPreviousFromMini() {
		handlePreviousTrack();
	}

	public void playNextFromMini() {
		handleNextTrack();
	}

	public boolean isCurrentlyPlaying() {
		return mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING;
	}

	private Node buildCoverNode(byte[] imageBytes) {
		StackPane placeholder = new StackPane();
		placeholder.setPrefSize(150, 150);
		placeholder.setStyle("-fx-background-color: #2d333b; -fx-background-radius: 6;");

		if (imageBytes == null || imageBytes.length == 0) {
			Label noImageLabel = new Label("No cover");
			noImageLabel.setStyle("-fx-text-fill: #9ca3af;");
			placeholder.getChildren().add(noImageLabel);
			return placeholder;
		}

		try {
			Image image = new Image(new ByteArrayInputStream(imageBytes));
			if (image.isError()) {
				Label noImageLabel = new Label("No cover");
				noImageLabel.setStyle("-fx-text-fill: #9ca3af;");
				placeholder.getChildren().add(noImageLabel);
				return placeholder;
			}

			ImageView imageView = new ImageView(image);
			imageView.setFitWidth(150);
			imageView.setFitHeight(150);
			imageView.setPreserveRatio(false);
			return imageView;
		} catch (Exception ex) {
			Label noImageLabel = new Label("No cover");
			noImageLabel.setStyle("-fx-text-fill: #9ca3af;");
			placeholder.getChildren().add(noImageLabel);
			return placeholder;
		}
	}
}

