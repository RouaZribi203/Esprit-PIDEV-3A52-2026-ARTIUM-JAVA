package services;

import entities.User;
import entities.Oeuvre;
import utils.ImageUrlUtils;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLDataException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
        String imageUrl = ImageUrlUtils.persistToWebImageDirectoryAndNormalize(t.getImage());
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new SQLDataException("L'image est obligatoire.");
        }

        String type = t.getType() == null || t.getType().trim().isEmpty() ? "Oeuvre" : t.getType().trim();
        LocalDate dateCreation = t.getDateCreation() == null ? LocalDate.now() : t.getDateCreation();

        String sql = "INSERT INTO oeuvre (titre, description, date_creation, image, type, collection_id, classe) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, titre);
            preparedStatement.setString(2, description);
            preparedStatement.setDate(3, Date.valueOf(dateCreation));
            preparedStatement.setString(4, imageUrl);
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
        String imageUrl = ImageUrlUtils.persistToWebImageDirectoryAndNormalize(t.getImage());
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new SQLDataException("L'image est obligatoire.");
        }

        String type = t.getType() == null || t.getType().trim().isEmpty() ? "Oeuvre" : t.getType().trim();
        LocalDate dateCreation = t.getDateCreation() == null ? LocalDate.now() : t.getDateCreation();

        String sql = "UPDATE oeuvre SET titre = ?, description = ?, date_creation = ?, image = ?, type = ?, collection_id = ?, classe = ? WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, titre);
            preparedStatement.setString(2, description);
            preparedStatement.setDate(3, Date.valueOf(dateCreation));
            preparedStatement.setString(4, imageUrl);
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
                + "FROM oeuvre "
                + "WHERE classe NOT IN ('livre', 'musique') "
                + "ORDER BY date_creation DESC, id DESC";

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

    @Override
    public Oeuvre getById(int id) throws SQLDataException {
        return null;
    }

    /**
     * Retourne la liste des oeuvres d'un artiste (simple, sans engagement data).
     */
    public List<Oeuvre> getOeuvresByArtisteId(int artisteId) throws SQLDataException {
        String sql = "SELECT o.id, o.titre, o.description, o.date_creation, o.image, o.type, o.collection_id "
                + "FROM oeuvre o "
                + "INNER JOIN collections c ON c.id = o.collection_id "
                + "WHERE c.artiste_id = ? "
                + "ORDER BY o.date_creation DESC, o.id DESC";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, artisteId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                List<Oeuvre> oeuvres = new ArrayList<>();
                while (resultSet.next()) {
                    oeuvres.add(mapOeuvre(resultSet));
                }
                return oeuvres;
            }
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    public List<Oeuvre> getOeuvresByCollectionId(int collectionId) throws SQLDataException {
        String sql = "SELECT id, titre, description, date_creation, image, type, collection_id "
                + "FROM oeuvre "
                + "WHERE collection_id = ? "
                + "ORDER BY id DESC";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, collectionId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                List<Oeuvre> oeuvres = new ArrayList<>();
                while (resultSet.next()) {
                    oeuvres.add(mapOeuvre(resultSet));
                }
                return oeuvres;
            }
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }

    public Oeuvre getOeuvreById(int oeuvreId) throws SQLDataException {
        String sql = "SELECT id, titre, description, date_creation, image, type, collection_id "
                + "FROM oeuvre WHERE id = ? LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, oeuvreId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapOeuvre(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
    }


    /**
     * Retourne un user par son ID (pour affichage auteur commentaire, etc.).
     */
    public User getUserById(int userId) {
        String sql = "SELECT id, nom, prenom, photo_profil AS photoProfil, specialite FROM `user` WHERE id = ? LIMIT 1";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, userId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    User user = new User();
                    user.setNom(trimOrEmpty(resultSet.getString("nom")));
                    user.setPrenom(trimOrEmpty(resultSet.getString("prenom")));
                    user.setPhotoProfil(ImageUrlUtils.normalizeForDatabase(resultSet.getString("photoProfil")));
                    user.setSpecialite(trimOrEmpty(resultSet.getString("specialite")));
                    return user;
                }
            }
        } catch (SQLException ignored) {
            // Keep null fallback for caller-side graceful handling.
        }

        return null;
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
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

        oeuvre.setImage(ImageUrlUtils.normalizeForDatabase(resultSet.getString("image")));
        oeuvre.setType(resultSet.getString("type"));

        int collectionId = resultSet.getInt("collection_id");
        if (!resultSet.wasNull()) {
            oeuvre.setCollectionId(collectionId);
        }

        return oeuvre;
    }
}
