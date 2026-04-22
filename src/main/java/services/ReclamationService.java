package services;

import entities.Reclamation;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReclamationService implements Iservice<Reclamation> {

    Connection connection;

    private static final String INSERT_SQL = "INSERT INTO reclamation(texte,date_creation,statut,type,file_name,updated_at,user_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL = "UPDATE reclamation SET texte = ?, date_creation = ?, statut = ?, type = ?, file_name = ? ,updated_at = ?,user_id=?  WHERE id = ?";
    private static final String SELECT_ALL_SQL = "SELECT id, texte, date_creation, statut, type,file_name, updated_at, user_id FROM reclamation";
    private static final String DELETE_SQL = "DELETE FROM reclamation WHERE id = ?";
    private static final String UPDATE_STATUT_SQL = "UPDATE reclamation SET statut = ? WHERE id = ?";

    public ReclamationService(){
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void add(Reclamation reclamation) throws SQLDataException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, reclamation.getTexte());
            statement.setTimestamp(2,java.sql.Timestamp.valueOf(reclamation.getDateCreation()));
            statement.setString(3, reclamation.getStatut());
            statement.setString(4, reclamation.getType());
            statement.setString(5, reclamation.getFileName());
            statement.setTimestamp(6, java.sql.Timestamp.valueOf(reclamation.getUpdatedAt()));
            statement.setInt(7, reclamation.getUserId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException("Erreur lors de l'ajout de reclamation: " + e.getMessage());
        }
    }

    @Override
    public void update(Reclamation reclamation) throws SQLDataException {
        if (reclamation == null || reclamation.getId() == null) {
            throw new SQLDataException("Impossible de modifier une reclamation sans ID");
        }

        try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, reclamation.getTexte());
            // LocalDateTime.toString() produit un format ISO (ex: 2026-04-14T12:34:56)
            // Timestamp.valueOf attend "yyyy-[m]m-[d]d hh:mm:ss[.f...]".
            statement.setTimestamp(2, java.sql.Timestamp.valueOf(reclamation.getDateCreation()));
            statement.setString(3, reclamation.getStatut());
            statement.setString(4, reclamation.getType());
            statement.setString(5, reclamation.getFileName());
            statement.setTimestamp(6, java.sql.Timestamp.valueOf(reclamation.getUpdatedAt()));
            statement.setInt(7, reclamation.getUserId());
            statement.setInt(8, reclamation.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException("Erreur lors de la modification de la reclamation: " + e.getMessage());
        }
    }

    @Override
    public void delete(Reclamation reclamation) throws SQLDataException {
        if (reclamation == null || reclamation.getId() == null) {
            throw new SQLDataException("Impossible de supprimer une reclamation sans ID");
        }

        try (PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setInt(1, reclamation.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException("Erreur lors de la suppression de la reclamation: " + e.getMessage());
        }
    }

    @Override
    public List<Reclamation> getAll() throws SQLDataException {
        List<Reclamation> reclamations = new ArrayList<>();

        try (Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(SELECT_ALL_SQL)) {

            while (resultSet.next()) {
                Reclamation reclamation = new Reclamation();
                reclamation.setId(resultSet.getInt("id"));
                reclamation.setTexte(resultSet.getString("texte"));
                reclamation.setDateCreation(resultSet.getTimestamp("date_creation").toLocalDateTime());
                reclamation.setStatut(resultSet.getString("statut"));
                reclamation.setType(resultSet.getString("type"));
                reclamation.setFileName(resultSet.getString("file_name"));
                reclamation.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
                reclamation.setUserId(resultSet.getInt("user_id"));
                reclamations.add(reclamation);
            }
        } catch (SQLException e) {
            throw new SQLDataException("Erreur lors de la recuperation des reclamation: " + e.getMessage());
        }

        return reclamations;
    }

    @Override
    public Reclamation getById(int id) throws SQLDataException {

        String query = "SELECT * FROM reclamation WHERE id = " + id;
        Reclamation reclamation = new Reclamation();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            if (resultSet.next()) {
                reclamation.setId(resultSet.getInt("id"));
                reclamation.setTexte(resultSet.getString("texte"));
                reclamation.setDateCreation(resultSet.getTimestamp("date_creation").toLocalDateTime());
                reclamation.setStatut(resultSet.getString("statut"));
                reclamation.setType(resultSet.getString("type"));
                reclamation.setFileName(resultSet.getString("file_name"));
                reclamation.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
                reclamation.setUserId(resultSet.getInt("user_id"));
            }
        } catch (SQLException e) {
            throw new SQLDataException("Erreur lors de la recuperation de la reclamation: " + e.getMessage());
        }

        return reclamation;
    }

    public void updateStatutById(int id, String statut) throws SQLDataException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_STATUT_SQL)) {
            statement.setString(1, statut);
            statement.setInt(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException("Erreur lors de la modification du statut: " + e.getMessage());
        }
    }

    public int getTotalReclamationsForUser(int userId) throws SQLDataException {
        String sql = "SELECT COUNT(*) FROM reclamation WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new SQLDataException("Erreur lors du comptage des reclamations: " + e.getMessage());
        }
        return 0;
    }
}
