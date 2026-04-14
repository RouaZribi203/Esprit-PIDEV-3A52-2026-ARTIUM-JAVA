package controllers.amateur;

import Services.MusiqueService;
import entities.Musique;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.sql.SQLDataException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MusicfrontController {

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

	private final MusiqueService musiqueService = new MusiqueService();
	private final ObservableList<Musique> allTracks = FXCollections.observableArrayList();
	private final ObservableList<Musique> visibleTracks = FXCollections.observableArrayList();

	private MediaPlayer mediaPlayer;
	private int currentTrackIndex = -1;

	@FXML
	public void initialize() {
		searchField.textProperty().addListener((obs, oldValue, newValue) -> filterTracks(newValue));
		refreshTracks();
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
	private void handlePlayPause() {
		if (mediaPlayer == null) {
			int selectedIndex = currentTrackIndex;
			if (selectedIndex < 0 && !visibleTracks.isEmpty()) {
				selectedIndex = 0;
			}
			if (selectedIndex >= 0) {
				playTrackAtIndex(selectedIndex, true);
			}
			return;
		}

		MediaPlayer.Status status = mediaPlayer.getStatus();
		if (status == MediaPlayer.Status.PLAYING) {
			mediaPlayer.pause();
			playPauseButton.setText("Play");
			playerStatusLabel.setText("En pause");
		} else {
			mediaPlayer.play();
			playPauseButton.setText("Pause");
			playerStatusLabel.setText("Lecture en cours");
		}
	}

	@FXML
	private void handlePreviousTrack() {
		if (visibleTracks.isEmpty()) {
			return;
		}
		int targetIndex = currentTrackIndex <= 0 ? visibleTracks.size() - 1 : currentTrackIndex - 1;
		playTrackAtIndex(targetIndex, true);
	}

	@FXML
	private void handleNextTrack() {
		if (visibleTracks.isEmpty()) {
			return;
		}
		int targetIndex = currentTrackIndex >= visibleTracks.size() - 1 ? 0 : currentTrackIndex + 1;
		playTrackAtIndex(targetIndex, true);
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
			nowPlayingTitleLabel.setText("Selectionnez une musique");
			nowPlayingMetaLabel.setText("Genre: -");
			playerStatusLabel.setText("Pret");
			playPauseButton.setText("Play");
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

	private void playTrackAtIndex(int index, boolean autoPlay) {
		if (index < 0 || index >= visibleTracks.size()) {
			return;
		}

		Musique selectedTrack = visibleTracks.get(index);
		String source = toMediaSource(selectedTrack.getAudio());
		if (source == null) {
			playerStatusLabel.setText("Fichier audio introuvable");
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
				if (autoPlay) {
					mediaPlayer.play();
					playPauseButton.setText("Pause");
					playerStatusLabel.setText("Lecture en cours");
				} else {
					playPauseButton.setText("Play");
					playerStatusLabel.setText("Pret");
				}
			});
			mediaPlayer.setOnEndOfMedia(this::handleNextTrack);
			mediaPlayer.setOnError(() -> playerStatusLabel.setText(describeMediaError(mediaPlayer.getError())));
		} catch (MediaException mediaException) {
			playPauseButton.setText("Play");
			playerStatusLabel.setText(describeMediaError(mediaException));
		} catch (RuntimeException runtimeException) {
			playPauseButton.setText("Play");
			playerStatusLabel.setText("Impossible de lire ce fichier audio");
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

	private void updateNowPlayingLabels(Musique musique) {
		String titre = musique.getTitre() != null ? musique.getTitre() : "Sans titre";
		String genre = musique.getGenre() != null ? musique.getGenre() : "-";
		nowPlayingTitleLabel.setText(titre);
		nowPlayingMetaLabel.setText("Genre: " + genre);
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

		card.getChildren().addAll(coverNode, titleLabel, metaLabel);
		card.setOnMouseClicked(event -> playTrackAtIndex(index, true));
		return card;
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

