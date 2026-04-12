package services;

import entities.Galerie;
import utils.MyDatabase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLDataException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class GalerieService implements Iservice<Galerie> {

    private static final String INSERT_SQL = "INSERT INTO galerie (nom, adresse, localisation, description, capacite_max) VALUES (?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL = "UPDATE galerie SET nom = ?, adresse = ?, localisation = ?, description = ?, capacite_max = ? WHERE id = ?";

    @Override
    public void add(Galerie galerie) throws SQLDataException {
        try (PreparedStatement statement = MyDatabase.getInstance().getConnection().prepareStatement(INSERT_SQL)) {
            statement.setString(1, galerie.getNom());
            statement.setString(2, galerie.getAdresse());
            statement.setString(3, galerie.getLocalisation());
            statement.setString(4, galerie.getDescription());
            statement.setInt(5, galerie.getCapaciteMax());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException("Erreur lors de l'ajout de la galerie: " + e.getMessage());
        }
    }

    @Override
    public void delete(Galerie galerie) throws SQLDataException {

    }

    @Override
    public void update(Galerie galerie) throws SQLDataException {
        if (galerie == null || galerie.getId() == null) {
            throw new SQLDataException("Impossible de modifier une galerie sans ID");
        }

        try (PreparedStatement statement = MyDatabase.getInstance().getConnection().prepareStatement(UPDATE_SQL)) {
            statement.setString(1, galerie.getNom());
            statement.setString(2, galerie.getAdresse());
            statement.setString(3, galerie.getLocalisation());
            statement.setString(4, galerie.getDescription());
            statement.setInt(5, galerie.getCapaciteMax());
            statement.setInt(6, galerie.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException("Erreur lors de la modification de la galerie: " + e.getMessage());
        }
    }

    @Override
    public List<Galerie> getAll() throws SQLDataException {
        return List.of();
    }

}

