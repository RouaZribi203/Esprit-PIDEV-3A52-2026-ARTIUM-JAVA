package Services;

import entities.Musique;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MusiqueService implements Iservice<Musique> {

    private final Connection connection;

    public MusiqueService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void add(Musique musique) throws SQLDataException {
        if (connection == null) {
            throw new SQLDataException("Connexion a la base de donnees indisponible.");
        }

        String oeuvreInsert = "INSERT INTO oeuvre (titre, description, date_creation, image, type, collection_id, classe) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String musiqueInsert = "INSERT INTO musique (id, genre, audio, updated_at) VALUES (?, ?, ?, ?)";

        try {
            connection.setAutoCommit(false);
            Integer effectiveCollectionId = resolveCollectionId(musique.getCollectionId());

            int oeuvreId;
            try (PreparedStatement oeuvreStatement = connection.prepareStatement(oeuvreInsert, Statement.RETURN_GENERATED_KEYS)) {
                oeuvreStatement.setString(1, musique.getTitre());
                oeuvreStatement.setString(2, musique.getDescription());
                oeuvreStatement.setDate(3, Date.valueOf(musique.getDateCreation()));
                oeuvreStatement.setBytes(4, musique.getImage());
                oeuvreStatement.setString(5, musique.getType() != null ? musique.getType() : "musique");
                oeuvreStatement.setInt(6, effectiveCollectionId);
                oeuvreStatement.setString(7, musique.getClasse() != null ? musique.getClasse() : "musique");
                oeuvreStatement.executeUpdate();

                try (var keys = oeuvreStatement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLDataException("Impossible de recuperer l'identifiant de l'oeuvre creee.");
                    }
                    oeuvreId = keys.getInt(1);
                }
            }

            try (PreparedStatement musiqueStatement = connection.prepareStatement(musiqueInsert)) {
                musiqueStatement.setInt(1, oeuvreId);
                musiqueStatement.setString(2, musique.getGenre());
                musiqueStatement.setString(3, musique.getAudio());
                if (musique.getUpdatedAt() != null) {
                    musiqueStatement.setTimestamp(4, Timestamp.valueOf(musique.getUpdatedAt()));
                } else {
                    musiqueStatement.setTimestamp(4, null);
                }
                musiqueStatement.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                throw new SQLDataException("Echec lors du rollback apres erreur d'ajout musique: " + rollbackException.getMessage());
            }
            throw new SQLDataException("Impossible d'ajouter la musique: " + e.getMessage());
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
                // Restore auto-commit best effort for next service calls.
            }
        }
    }

    private Integer resolveCollectionId(Integer requestedCollectionId) throws SQLException {
        if (requestedCollectionId != null) {
            return requestedCollectionId;
        }

        String randomCollectionQuery = "SELECT id FROM collections ORDER BY RAND() LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(randomCollectionQuery);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt("id");
            }
        }

        throw new SQLException("Aucune collection disponible pour ajouter une musique.");
    }

    @Override
    public void delete(Musique musique) throws SQLDataException {
        throw new SQLDataException("Suppression non implementee pour le moment.");
    }

    @Override
    public void update(Musique musique) throws SQLDataException {
        throw new SQLDataException("Mise a jour non implementee pour le moment.");
    }

    @Override
    public List<Musique> getAll() throws SQLDataException {
        if (connection == null) {
            throw new SQLDataException("Connexion a la base de donnees indisponible.");
        }

        String query = "SELECT o.titre, o.description, o.date_creation, o.collection_id, o.type, o.classe, o.image, m.genre, m.audio, m.updated_at "
                + "FROM musique m INNER JOIN oeuvre o ON o.id = m.id ORDER BY o.id DESC";

        List<Musique> musiques = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Musique musique = new Musique();
                musique.setTitre(resultSet.getString("titre"));
                musique.setDescription(resultSet.getString("description"));
                Date dateCreation = resultSet.getDate("date_creation");
                if (dateCreation != null) {
                    musique.setDateCreation(dateCreation.toLocalDate());
                }
                musique.setCollectionId(resultSet.getInt("collection_id"));
                musique.setType(resultSet.getString("type"));
                musique.setClasse(resultSet.getString("classe"));
                musique.setImage(resultSet.getBytes("image"));
                musique.setGenre(resultSet.getString("genre"));
                musique.setAudio(resultSet.getString("audio"));
                Timestamp updatedAt = resultSet.getTimestamp("updated_at");
                if (updatedAt != null) {
                    musique.setUpdatedAt(updatedAt.toLocalDateTime());
                }

                musiques.add(musique);
            }
        } catch (SQLException e) {
            throw new SQLDataException("Impossible de charger les musiques: " + e.getMessage());
        }

        return musiques;
    }
}


