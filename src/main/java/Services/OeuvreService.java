package Services;

import entities.Oeuvre;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLDataException;
import java.util.ArrayList;
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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void delete(Oeuvre t) throws SQLDataException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void update(Oeuvre t) throws SQLDataException {
        throw new UnsupportedOperationException("Not supported yet.");
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

                applyLikeCounts(feedItems);
                return feedItems;
            }
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    private void applyLikeCounts(List<OeuvreFeedItem> feedItems) {
        Map<Integer, Integer> likeCountByOeuvreId = loadLikeCounts(feedItems);
        for (int i = 0; i < feedItems.size(); i++) {
            OeuvreFeedItem item = feedItems.get(i);
            int count = likeCountByOeuvreId.getOrDefault(item.getOeuvre().getId(), 0);
            feedItems.set(i, new OeuvreFeedItem(item.getOeuvre(), item.getCollectionTitre(), count));
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

        public OeuvreFeedItem(Oeuvre oeuvre, String collectionTitre, int likeCount) {
            this.oeuvre = oeuvre;
            this.collectionTitre = collectionTitre;
            this.likeCount = likeCount;
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
    }
}
