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

    }

    @Override
    public List<Galerie> getAll() throws SQLDataException {
        return List.of();
    }
}

