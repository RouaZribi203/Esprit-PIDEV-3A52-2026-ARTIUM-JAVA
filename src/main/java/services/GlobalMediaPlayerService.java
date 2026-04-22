package services;

import entities.Musique;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import utils.ImageUrlUtils;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Locale;

public final class GlobalMediaPlayerService {
    private static final String XAMPP_IMAGE_DIR = "C:\\xampp\\htdocs\\img";
    private static final String DEFAULT_TITLE = "Aucune piste en lecture";
    private static final String DEFAULT_ARTIST = "Artiste inconnu";

    private static final GlobalMediaPlayerService INSTANCE = new GlobalMediaPlayerService();

    private final ReadOnlyObjectWrapper<Musique> currentTrack = new ReadOnlyObjectWrapper<>();
    private final StringProperty trackTitle = new SimpleStringProperty(DEFAULT_TITLE);
    private final StringProperty trackArtist = new SimpleStringProperty(DEFAULT_ARTIST);
    private final StringProperty trackMeta = new SimpleStringProperty("Genre: -");
    private final StringProperty statusText = new SimpleStringProperty("Pret");
    private final StringProperty timeText = new SimpleStringProperty("0:00 / 0:00");
    private final BooleanProperty playing = new SimpleBooleanProperty(false);
    private final ObjectProperty<Image> coverImage = new SimpleObjectProperty<>();

    private final ObservableList<Musique> queue = FXCollections.observableArrayList();
    private int queueIndex = -1;

    private MediaPlayer mediaPlayer;

    private GlobalMediaPlayerService() {
    }

    public static GlobalMediaPlayerService getInstance() {
        return INSTANCE;
    }

    public void playTrack(Musique track, List<Musique> queueTracks, int selectedIndex, String artistName) {
        if (track == null) {
            return;
        }

        queue.setAll(queueTracks != null ? queueTracks : List.of(track));
        if (queue.isEmpty()) {
            queue.add(track);
        }

        int resolvedIndex = selectedIndex;
        if (resolvedIndex < 0 || resolvedIndex >= queue.size()) {
            resolvedIndex = queue.indexOf(track);
        }
        queueIndex = resolvedIndex >= 0 ? resolvedIndex : 0;

        openTrack(track, artistName, true);
    }

    public void togglePlayPause() {
        if (mediaPlayer == null) {
            Musique fallback = currentTrack.get();
            if (fallback != null) {
                openTrack(fallback, trackArtist.get(), true);
            }
            return;
        }

        MediaPlayer.Status status = mediaPlayer.getStatus();
        if (status == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            playing.set(false);
            statusText.set("En pause");
            return;
        }

        mediaPlayer.play();
        playing.set(true);
        statusText.set("Lecture en cours");
    }

    public void playPrevious() {
        if (queue.isEmpty()) {
            return;
        }
        queueIndex = queueIndex <= 0 ? queue.size() - 1 : queueIndex - 1;
        Musique target = queue.get(queueIndex);
        openTrack(target, trackArtist.get(), true);
    }

    public void playNext() {
        if (queue.isEmpty()) {
            return;
        }
        queueIndex = queueIndex >= queue.size() - 1 ? 0 : queueIndex + 1;
        Musique target = queue.get(queueIndex);
        openTrack(target, trackArtist.get(), true);
    }

    public void seekToFraction(double fraction) {
        if (mediaPlayer == null) {
            return;
        }
        Duration total = mediaPlayer.getTotalDuration();
        if (total == null || total.isUnknown() || total.lessThanOrEqualTo(Duration.ZERO)) {
            return;
        }
        double clamped = Math.max(0.0, Math.min(1.0, fraction));
        mediaPlayer.seek(total.multiply(clamped));
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        playing.set(false);
        statusText.set("Pret");
    }

    public ReadOnlyObjectProperty<Musique> currentTrackProperty() {
        return currentTrack.getReadOnlyProperty();
    }

    public StringProperty trackTitleProperty() {
        return trackTitle;
    }

    public StringProperty trackArtistProperty() {
        return trackArtist;
    }

    public StringProperty trackMetaProperty() {
        return trackMeta;
    }

    public StringProperty statusTextProperty() {
        return statusText;
    }

    public StringProperty timeTextProperty() {
        return timeText;
    }

    public BooleanProperty playingProperty() {
        return playing;
    }

    public ObjectProperty<Image> coverImageProperty() {
        return coverImage;
    }

    public Musique getCurrentTrack() {
        return currentTrack.get();
    }

    public boolean isPlaying() {
        return playing.get();
    }

    private void openTrack(Musique track, String artistName, boolean autoPlay) {
        String source = toMediaSource(track.getAudio());
        if (source == null) {
            statusText.set("Fichier audio introuvable");
            return;
        }

        stop();

        try {
            Media media = new Media(source);
            mediaPlayer = new MediaPlayer(media);
            currentTrack.set(track);
            trackTitle.set(track.getTitre() != null && !track.getTitre().isBlank() ? track.getTitre() : "Sans titre");
            trackArtist.set(normalizeArtist(artistName));
            trackMeta.set("Genre: " + (track.getGenre() != null && !track.getGenre().isBlank() ? track.getGenre() : "-"));
            coverImage.set(loadImageSafely(track.getImage()));
            statusText.set("Chargement...");
            timeText.set("0:00 / 0:00");

            mediaPlayer.setOnReady(() -> {
                Duration total = mediaPlayer.getTotalDuration();
                timeText.set(formatDuration(Duration.ZERO) + " / " + formatDuration(total));
                if (autoPlay) {
                    mediaPlayer.play();
                    playing.set(true);
                    statusText.set("Lecture en cours");
                } else {
                    playing.set(false);
                    statusText.set("Pret");
                }
            });

            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                Duration total = mediaPlayer.getTotalDuration();
                timeText.set(formatDuration(newTime) + " / " + formatDuration(total));
            });

            mediaPlayer.setOnPaused(() -> {
                playing.set(false);
                statusText.set("En pause");
            });
            mediaPlayer.setOnPlaying(() -> {
                playing.set(true);
                statusText.set("Lecture en cours");
            });
            mediaPlayer.setOnEndOfMedia(this::playNext);
            mediaPlayer.setOnError(() -> {
                playing.set(false);
                statusText.set(describeMediaError(mediaPlayer.getError()));
            });
        } catch (MediaException mediaException) {
            statusText.set(describeMediaError(mediaException));
        } catch (RuntimeException runtimeException) {
            statusText.set("Impossible de lire ce fichier audio");
        }
    }

    private String normalizeArtist(String artistName) {
        if (artistName == null || artistName.isBlank()) {
            return DEFAULT_ARTIST;
        }
        return artistName;
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

    private String describeMediaError(Throwable throwable) {
        if (throwable instanceof MediaException mediaException) {
            if (mediaException.getType() == MediaException.Type.MEDIA_UNSUPPORTED) {
                return "Format non pris en charge par JavaFX";
            }
            String message = mediaException.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("unrecognized file signature")) {
                return "Codec audio non pris en charge (essayez MP3/M4A/WAV)";
            }
        }
        return "Impossible de lire ce fichier audio";
    }

    private String formatDuration(Duration duration) {
        if (duration == null || duration.isUnknown() || duration.lessThanOrEqualTo(Duration.ZERO)) {
            return "0:00";
        }

        int totalSeconds = (int) Math.floor(duration.toSeconds());
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
    }

    private Image loadImageSafely(String imageSource) {
        if (imageSource == null || imageSource.isBlank()) {
            return null;
        }

        try {
            String trimmed = imageSource.trim();

            File localImage = resolveLocalImageFile(trimmed);
            if (localImage != null && localImage.exists() && localImage.isFile()) {
                Image local = new Image(localImage.toURI().toString(), false);
                if (!local.isError()) {
                    return local;
                }
            }

            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                Image remote = new Image(trimmed, true);
                return remote.isError() ? null : remote;
            }
        } catch (RuntimeException ignored) {
            return null;
        }

        return null;
    }

    private File resolveLocalImageFile(String source) {
        String trimmed = source;
        if (trimmed.length() > 1 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        if (trimmed.startsWith("/") && trimmed.length() > 2 && trimmed.charAt(2) == ':') {
            trimmed = trimmed.substring(1);
        }

        if (trimmed.startsWith(ImageUrlUtils.IMAGE_BASE_URL)) {
            String fileName = trimmed.substring(ImageUrlUtils.IMAGE_BASE_URL.length()).trim();
            return fileName.isEmpty() ? null : new File(XAMPP_IMAGE_DIR, fileName);
        }

        if (trimmed.startsWith("/img/") || trimmed.startsWith("/htdocs/img/")) {
            String fileName = extractFileName(trimmed);
            return fileName.isEmpty() ? null : new File(XAMPP_IMAGE_DIR, fileName);
        }

        if (trimmed.startsWith("file:")) {
            try {
                return new File(new URI(trimmed));
            } catch (Exception ignored) {
                String rawPath = trimmed.substring("file:".length());
                if (rawPath.startsWith("//")) {
                    rawPath = rawPath.substring(2);
                }
                if (rawPath.startsWith("/") && rawPath.length() > 2 && rawPath.charAt(2) == ':') {
                    rawPath = rawPath.substring(1);
                }
                return new File(rawPath);
            }
        }

        return new File(trimmed);
    }

    private String extractFileName(String value) {
        String normalized = value.replace('\\', '/');
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        int fragmentIndex = normalized.indexOf('#');
        if (fragmentIndex >= 0) {
            normalized = normalized.substring(0, fragmentIndex);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        int lastSlash = normalized.lastIndexOf('/');
        return (lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized).trim();
    }
}

