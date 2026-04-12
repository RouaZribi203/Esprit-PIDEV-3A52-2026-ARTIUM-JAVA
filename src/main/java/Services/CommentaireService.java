package Services;

import entities.Commentaire;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLDataException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentaireService implements services.Iservice<Commentaire> {

    private final Connection connection;

    public CommentaireService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void add(Commentaire commentaire) throws SQLDataException {
        // TODO: implement
    }

    @Override
    public void delete(Commentaire commentaire) throws SQLDataException {
        // TODO: implement
    }

    @Override
    public void update(Commentaire commentaire) throws SQLDataException {
        // TODO: implement
    }

    @Override
    public java.util.List<entities.Commentaire> getAll() throws java.sql.SQLDataException {
        // TODO: implement
        return new ArrayList<>();
    }

    /**
     * Supprime tous les commentaires associés à une oeuvre.
     * Utilisé lors de la suppression d'une oeuvre pour cascade delete.
     */
    public void deleteByOeuvreId(int oeuvreId) throws java.sql.SQLDataException {
        String[] tableCandidates = new String[] {"commentaire", "commentaires", "comments"};
        String[] oeuvreColumnCandidates = new String[] {"oeuvre_id", "oeuvreId"};

        for (String table : tableCandidates) {
            for (String oeuvreColumn : oeuvreColumnCandidates) {
                String sql = "DELETE FROM " + table + " WHERE " + oeuvreColumn + " = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    preparedStatement.setInt(1, oeuvreId);
                    preparedStatement.executeUpdate();
                    return; // Succès sur la première tentative
                } catch (java.sql.SQLException ignored) {
                    // Essayer la prochaine combinaison table/colonne
                }
            }
        }
    }

    /**
     * Charge les compteurs de commentaires par oeuvre.
     */
    public Map<Integer, Integer> getCommentCounts(List<Integer> oeuvreIds) {
        Map<Integer, Integer> counts = new HashMap<>();
        if (oeuvreIds == null || oeuvreIds.isEmpty()) {
            return counts;
        }

        String placeholders = buildPlaceholders(oeuvreIds.size());
        if (placeholders.isEmpty()) {
            return counts;
        }

        String[] tableCandidates = new String[] {"commentaire", "commentaires", "comments"};
        String[] oeuvreColumnCandidates = new String[] {"oeuvre_id", "oeuvreId"};

        for (String table : tableCandidates) {
            for (String oeuvreColumn : oeuvreColumnCandidates) {
                String sql = "SELECT " + oeuvreColumn + " AS oeuvre_id, COUNT(*) AS total FROM " + table
                        + " WHERE " + oeuvreColumn + " IN (" + placeholders + ") GROUP BY " + oeuvreColumn;
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    bindOeuvreIds(preparedStatement, oeuvreIds);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        while (resultSet.next()) {
                            counts.put(resultSet.getInt("oeuvre_id"), resultSet.getInt("total"));
                        }
                    }
                    return counts;
                } catch (SQLException ignored) {
                    // Try next possible table/column.
                }
            }
        }

        return counts;
    }

    /**
     * Charge un aperçu des commentaires récents pour chaque oeuvre.
     */
    public Map<Integer, List<CommentPreview>> getCommentsPreview(List<Integer> oeuvreIds, int limitPerOeuvre) {
        Map<Integer, List<CommentPreview>> commentsByOeuvreId = new HashMap<>();
        if (oeuvreIds == null || oeuvreIds.isEmpty() || limitPerOeuvre <= 0) {
            return commentsByOeuvreId;
        }

        String placeholders = buildPlaceholders(oeuvreIds.size());
        if (placeholders.isEmpty()) {
            return commentsByOeuvreId;
        }

        String[] commentTableCandidates = new String[] {"commentaire", "commentaires", "comments"};
        String[] oeuvreColumnCandidates = new String[] {"oeuvre_id", "oeuvreId"};
        String[] userColumnCandidates = new String[] {"user_id", "userId", "auteur_id"};
        String[] textColumnCandidates = new String[] {"texte", "contenu", "content", "commentaire"};
        String[] dateColumnCandidates = new String[] {"date_commentaire", "date_creation", "created_at", "createdAt"};
        String[] userTableCandidates = new String[] {"user", "users", "personne", "personnes"};
        String[] userPhotoColumnCandidates = new String[] {"photo_profil", "photoProfil", "avatar", "photo"};

        for (String commentTable : commentTableCandidates) {
            for (String oeuvreColumn : oeuvreColumnCandidates) {
                for (String userColumn : userColumnCandidates) {
                    for (String textColumn : textColumnCandidates) {
                        for (String dateColumn : dateColumnCandidates) {
                            for (String userTable : userTableCandidates) {
                                for (String userPhotoColumn : userPhotoColumnCandidates) {
                                    String sql = "SELECT c." + oeuvreColumn + " AS oeuvre_id, c." + textColumn + " AS comment_text, "
                                            + "c." + dateColumn + " AS comment_date, u.prenom AS user_prenom, u.nom AS user_nom, "
                                            + "u." + userPhotoColumn + " AS user_photo "
                                            + "FROM " + commentTable + " c "
                                            + "LEFT JOIN `" + userTable + "` u ON u.id = c." + userColumn + " "
                                            + "WHERE c." + oeuvreColumn + " IN (" + placeholders + ") "
                                            + "ORDER BY c." + dateColumn + " DESC, c.id DESC";

                                    try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                                        bindOeuvreIds(preparedStatement, oeuvreIds);
                                        try (ResultSet resultSet = preparedStatement.executeQuery()) {
                                            while (resultSet.next()) {
                                                int oeuvreId = resultSet.getInt("oeuvre_id");
                                                List<CommentPreview> comments = commentsByOeuvreId.computeIfAbsent(oeuvreId, ignored -> new ArrayList<>());
                                                if (comments.size() >= limitPerOeuvre) {
                                                    continue;
                                                }

                                                String prenom = trimOrEmpty(resultSet.getString("user_prenom"));
                                                String nom = trimOrEmpty(resultSet.getString("user_nom"));
                                                String fullName = (prenom + " " + nom).trim();
                                                if (fullName.isEmpty()) {
                                                    fullName = "Utilisateur";
                                                }

                                                comments.add(
                                                        new CommentPreview(
                                                                trimOrEmpty(resultSet.getString("comment_text")),
                                                                toLocalDateTime(resultSet, "comment_date"),
                                                                fullName,
                                                                trimOrEmpty(resultSet.getString("user_photo"))
                                                        )
                                                );
                                            }
                                        }
                                        return commentsByOeuvreId;
                                    } catch (SQLException ignored) {
                                        // Try next possible schema variant.
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return commentsByOeuvreId;
    }

    // Helper methods

    private String buildPlaceholders(int size) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('?');
        }
        return builder.toString();
    }

    private void bindOeuvreIds(PreparedStatement preparedStatement, List<Integer> oeuvreIds) throws SQLException {
        int index = 1;
        for (Integer oeuvreId : oeuvreIds) {
            preparedStatement.setInt(index++, oeuvreId);
        }
    }

    private LocalDateTime toLocalDateTime(ResultSet resultSet, String columnLabel) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnLabel);
        if (timestamp != null) {
            return timestamp.toLocalDateTime();
        }

        Date sqlDate = resultSet.getDate(columnLabel);
        if (sqlDate != null) {
            return sqlDate.toLocalDate().atStartOfDay();
        }

        return null;
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Aperçu d'un commentaire avec métadonnées (auteur, date, photo).
     */
    public static class CommentPreview {
        private final String text;
        private final LocalDateTime postedAt;
        private final String authorName;
        private final String authorPhoto;

        public CommentPreview(String text, LocalDateTime postedAt, String authorName, String authorPhoto) {
            this.text = text;
            this.postedAt = postedAt;
            this.authorName = authorName;
            this.authorPhoto = authorPhoto;
        }

        public String getText() {
            return text;
        }

        public LocalDateTime getPostedAt() {
            return postedAt;
        }

        public String getAuthorName() {
            return authorName;
        }

        public String getAuthorPhoto() {
            return authorPhoto;
        }
    }
}
