package Services;

import entities.Oeuvre;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLDataException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OeuvreService implements services.Iservice<Oeuvre> {

    private final Connection connection;

    public OeuvreService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void add(Oeuvre t) throws SQLDataException {
        String titre = t.getTitre() == null ? "" : t.getTitre().trim();
        String description = t.getDescription() == null ? "" : t.getDescription().trim();
        Integer collectionId = t.getCollectionId();

        if (titre.isEmpty()) {
            throw new SQLDataException("Le titre de l'oeuvre est obligatoire.");
        }
        if (description.isEmpty()) {
            throw new SQLDataException("La description est obligatoire.");
        }
        if (collectionId == null) {
            throw new SQLDataException("La collection est obligatoire.");
        }
        if (t.getImage() == null || t.getImage().length == 0) {
            throw new SQLDataException("L'image est obligatoire.");
        }

        String type = t.getType() == null || t.getType().trim().isEmpty() ? "Oeuvre" : t.getType().trim();
        LocalDate dateCreation = t.getDateCreation() == null ? LocalDate.now() : t.getDateCreation();

        String sql = "INSERT INTO oeuvre (titre, description, date_creation, image, type, collection_id, classe) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, titre);
            preparedStatement.setString(2, description);
            preparedStatement.setDate(3, Date.valueOf(dateCreation));
            preparedStatement.setBytes(4, t.getImage());
            preparedStatement.setString(5, type);
            preparedStatement.setInt(6, collectionId);
            preparedStatement.setString(7, "oeuvre");
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public void delete(Oeuvre t) throws SQLDataException {
        if (t == null || t.getId() == null) {
            throw new SQLDataException("L'identifiant de l'oeuvre est obligatoire.");
        }

        try {
            // Cascade delete: supprimer les commentaires associés
            CommentaireService commentService = new CommentaireService();
            commentService.deleteByOeuvreId(t.getId());

            // Puis supprimer l'oeuvre elle-même
            String sql = "DELETE FROM oeuvre WHERE id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, t.getId());
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    /**
     * Supprime toutes les oeuvres d'une collection (cascade delete).
     * Supprime aussi les commentaires associés à chaque oeuvre.
     * Utilisé lors de la suppression d'une collection.
     */
    public void deleteByCollectionId(int collectionId) throws SQLDataException {
        try {
            CommentaireService commentService = new CommentaireService();

            // Récupérer toutes les oeuvres de la collection
            String selectSql = "SELECT id FROM oeuvre WHERE collection_id = ?";
            List<Integer> oeuvreIds = new ArrayList<>();

            try (PreparedStatement selectStatement = connection.prepareStatement(selectSql)) {
                selectStatement.setInt(1, collectionId);
                try (ResultSet resultSet = selectStatement.executeQuery()) {
                    while (resultSet.next()) {
                        oeuvreIds.add(resultSet.getInt("id"));
                    }
                }
            }

            // Pour chaque oeuvre, supprimer les commentaires puis l'oeuvre
            for (Integer oeuvreId : oeuvreIds) {
                commentService.deleteByOeuvreId(oeuvreId);

                String deleteSql = "DELETE FROM oeuvre WHERE id = ?";
                try (PreparedStatement deleteStatement = connection.prepareStatement(deleteSql)) {
                    deleteStatement.setInt(1, oeuvreId);
                    deleteStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public void update(Oeuvre t) throws SQLDataException {
        if (t == null || t.getId() == null) {
            throw new SQLDataException("L'identifiant de l'oeuvre est obligatoire.");
        }

        String titre = t.getTitre() == null ? "" : t.getTitre().trim();
        String description = t.getDescription() == null ? "" : t.getDescription().trim();
        Integer collectionId = t.getCollectionId();

        if (titre.isEmpty()) {
            throw new SQLDataException("Le titre de l'oeuvre est obligatoire.");
        }
        if (description.isEmpty()) {
            throw new SQLDataException("La description est obligatoire.");
        }
        if (collectionId == null) {
            throw new SQLDataException("La collection est obligatoire.");
        }
        if (t.getImage() == null || t.getImage().length == 0) {
            throw new SQLDataException("L'image est obligatoire.");
        }

        String type = t.getType() == null || t.getType().trim().isEmpty() ? "Oeuvre" : t.getType().trim();
        LocalDate dateCreation = t.getDateCreation() == null ? LocalDate.now() : t.getDateCreation();

        String sql = "UPDATE oeuvre SET titre = ?, description = ?, date_creation = ?, image = ?, type = ?, collection_id = ?, classe = ? WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, titre);
            preparedStatement.setString(2, description);
            preparedStatement.setDate(3, Date.valueOf(dateCreation));
            preparedStatement.setBytes(4, t.getImage());
            preparedStatement.setString(5, type);
            preparedStatement.setInt(6, collectionId);
            preparedStatement.setString(7, "oeuvre");
            preparedStatement.setInt(8, t.getId());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    @Override
    public List<Oeuvre> getAll() throws SQLDataException {
        String sql = "SELECT id, titre, description, date_creation, image, type, collection_id "
                + "FROM oeuvre ORDER BY date_creation DESC, id DESC";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            List<Oeuvre> oeuvres = new ArrayList<>();
            while (resultSet.next()) {
                oeuvres.add(mapOeuvre(resultSet));
            }
            return oeuvres;
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }
   ///remove to artist service
    public ArtistIdentity getArtisteIdentityById(int artisteId) {
        String[] tables = new String[] {"user", "users", "personne", "personnes"};
        String[] specialiteColumns = new String[] {"specialite", "speciality"};

        for (String table : tables) {
            for (String specialiteColumn : specialiteColumns) {
                String sql = "SELECT nom, prenom, " + specialiteColumn + " AS specialite FROM `" + table + "` WHERE id = ? LIMIT 1";
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    preparedStatement.setInt(1, artisteId);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        if (resultSet.next()) {
                            String nom = trimOrEmpty(resultSet.getString("nom"));
                            String prenom = trimOrEmpty(resultSet.getString("prenom"));
                            String specialite = trimOrEmpty(resultSet.getString("specialite"));

                            String fullName = (prenom + " " + nom).trim();
                            if (fullName.isEmpty()) {
                                fullName = "Artiste";
                            }
                            if (specialite.isEmpty()) {
                                specialite = "Specialite inconnue";
                            }

                            return new ArtistIdentity(fullName, specialite);
                        }
                    }
                } catch (SQLException ignored) {
                    // Try next table/column candidate.
                }
            }
        }

        return new ArtistIdentity("Artiste", "Specialite inconnue");
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public List<OeuvreFeedItem> getFeedByArtisteId(int artisteId) throws SQLDataException {
        String sql = "SELECT o.id, o.titre, o.description, o.date_creation, o.image, o.type, o.collection_id, c.titre AS collection_titre "
                + "FROM oeuvre o "
                + "INNER JOIN collections c ON c.id = o.collection_id "
                + "WHERE c.artiste_id = ? "
                + "ORDER BY o.date_creation DESC, o.id DESC";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, artisteId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                List<OeuvreFeedItem> feedItems = new ArrayList<>();
                while (resultSet.next()) {
                    Oeuvre oeuvre = mapOeuvre(resultSet);
                    String collectionTitre = resultSet.getString("collection_titre");
                    feedItems.add(new OeuvreFeedItem(oeuvre, collectionTitre, 0));
                }

                applyEngagementData(feedItems);
                return feedItems;
            }
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    private void applyEngagementData(List<OeuvreFeedItem> feedItems) {
        Map<Integer, Integer> likeCountByOeuvreId = loadLikeCounts(feedItems);
        Map<Integer, Integer> favoriteCountByOeuvreId = loadFavoriteCounts(feedItems);

        // Load comments data via CommentaireService
        CommentaireService commentService = new CommentaireService();
        List<Integer> oeuvreIds = new ArrayList<>();
        for (OeuvreFeedItem item : feedItems) {
            oeuvreIds.add(item.getOeuvre().getId());
        }
        Map<Integer, Integer> commentCountByOeuvreId = commentService.getCommentCounts(oeuvreIds);
        Map<Integer, List<CommentaireService.CommentPreview>> commentsPreviewByOeuvreId = commentService.getCommentsPreview(oeuvreIds, 3);

        for (int i = 0; i < feedItems.size(); i++) {
            OeuvreFeedItem item = feedItems.get(i);
            Integer oeuvreId = item.getOeuvre().getId();
            int likeCount = likeCountByOeuvreId.getOrDefault(oeuvreId, 0);
            int commentCount = commentCountByOeuvreId.getOrDefault(oeuvreId, 0);
            int favoriteCount = favoriteCountByOeuvreId.getOrDefault(oeuvreId, 0);
            List<CommentaireService.CommentPreview> commentsPreview = commentsPreviewByOeuvreId.getOrDefault(oeuvreId, Collections.emptyList());

            feedItems.set(
                    i,
                    new OeuvreFeedItem(
                            item.getOeuvre(),
                            item.getCollectionTitre(),
                            likeCount,
                            commentCount,
                            favoriteCount,
                            commentsPreview
                    )
            );
        }
    }

    private Map<Integer, Integer> loadLikeCounts(List<OeuvreFeedItem> feedItems) {
        Map<Integer, Integer> counts = new HashMap<>();
        if (feedItems.isEmpty()) {
            return counts;
        }

        String placeholders = buildPlaceholders(feedItems.size());
        if (placeholders.isEmpty()) {
            return counts;
        }

        String[] tableCandidates = new String[] {"likes", "like_entity", "likeentity"};
        String[] countExprCandidates = new String[] {
                "COALESCE(SUM(CASE WHEN liked = 1 THEN 1 ELSE 0 END), 0)",
                "COUNT(*)"
        };

        for (String table : tableCandidates) {
            for (String countExpr : countExprCandidates) {
                String sql = "SELECT oeuvre_id, " + countExpr + " AS total FROM " + table
                        + " WHERE oeuvre_id IN (" + placeholders + ") GROUP BY oeuvre_id";
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    bindOeuvreIds(preparedStatement, feedItems);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        while (resultSet.next()) {
                            counts.put(resultSet.getInt("oeuvre_id"), resultSet.getInt("total"));
                        }
                    }
                    return counts;
                } catch (SQLException ignored) {
                    // Try next possible table/expression.
                }
            }
        }

        return counts;
    }

    private Map<Integer, Integer> loadFavoriteCounts(List<OeuvreFeedItem> feedItems) {
        Map<Integer, Integer> counts = new HashMap<>();
        if (feedItems.isEmpty()) {
            return counts;
        }

        String placeholders = buildPlaceholders(feedItems.size());
        if (placeholders.isEmpty()) {
            return counts;
        }

        String[] tableCandidates = new String[] {"oeuvre_user", "favoris", "favorites"};
        String[] oeuvreColumnCandidates = new String[] {"oeuvre_id", "oeuvreId"};

        for (String table : tableCandidates) {
            for (String oeuvreColumn : oeuvreColumnCandidates) {
                String sql = "SELECT " + oeuvreColumn + " AS oeuvre_id, COUNT(*) AS total FROM " + table
                        + " WHERE " + oeuvreColumn + " IN (" + placeholders + ") GROUP BY " + oeuvreColumn;
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    bindOeuvreIds(preparedStatement, feedItems);
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

    private void bindOeuvreIds(PreparedStatement preparedStatement, List<OeuvreFeedItem> feedItems) throws SQLException {
        int index = 1;
        for (OeuvreFeedItem item : feedItems) {
            preparedStatement.setInt(index++, item.getOeuvre().getId());
        }
    }

    private Oeuvre mapOeuvre(ResultSet resultSet) throws SQLException {
        Oeuvre oeuvre = new Oeuvre();
        oeuvre.setId(resultSet.getInt("id"));
        oeuvre.setTitre(resultSet.getString("titre"));
        oeuvre.setDescription(resultSet.getString("description"));

        Date sqlDate = resultSet.getDate("date_creation");
        if (sqlDate != null) {
            oeuvre.setDateCreation(sqlDate.toLocalDate());
        }

        oeuvre.setImage(resultSet.getBytes("image"));
        oeuvre.setType(resultSet.getString("type"));

        int collectionId = resultSet.getInt("collection_id");
        if (!resultSet.wasNull()) {
            oeuvre.setCollectionId(collectionId);
        }

        return oeuvre;
    }

    public static class OeuvreFeedItem {
        private final Oeuvre oeuvre;
        private final String collectionTitre;
        private final int likeCount;
        private final int commentCount;
        private final int favoriteCount;
        private final List<CommentaireService.CommentPreview> commentsPreview;

        public OeuvreFeedItem(Oeuvre oeuvre, String collectionTitre, int likeCount) {
            this(oeuvre, collectionTitre, likeCount, 0, 0, new ArrayList<>());
        }

        public OeuvreFeedItem(
                Oeuvre oeuvre,
                String collectionTitre,
                int likeCount,
                int commentCount,
                int favoriteCount,
                List<CommentaireService.CommentPreview> commentsPreview
        ) {
            this.oeuvre = oeuvre;
            this.collectionTitre = collectionTitre;
            this.likeCount = likeCount;
            this.commentCount = commentCount;
            this.favoriteCount = favoriteCount;
            this.commentsPreview = commentsPreview == null ? new ArrayList<>() : commentsPreview;
        }

        public Oeuvre getOeuvre() {
            return oeuvre;
        }

        public String getCollectionTitre() {
            return collectionTitre;
        }

        public int getLikeCount() {
            return likeCount;
        }

        public int getCommentCount() {
            return commentCount;
        }

        public int getFavoriteCount() {
            return favoriteCount;
        }

        public List<CommentaireService.CommentPreview> getCommentsPreview() {
            return commentsPreview;
        }
    }

    ////remove to artist service
    public static class ArtistIdentity {
        private final String fullName;
        private final String specialite;

        public ArtistIdentity(String fullName, String specialite) {
            this.fullName = fullName;
            this.specialite = specialite;
        }

        public String getFullName() {
            return fullName;
        }

        public String getSpecialite() {
            return specialite;
        }
    }
}
