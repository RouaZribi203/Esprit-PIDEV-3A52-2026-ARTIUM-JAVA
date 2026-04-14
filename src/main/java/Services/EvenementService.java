package services;

import entities.Evenement;
import utils.MyDatabase;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLDataException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class EvenementService implements Iservice<Evenement> {

    private static final String SELECT_ALL_SQL = "SELECT id, titre, description, date_debut, date_fin, date_creation, type, image_couverture, statut, capacite_max, prix_ticket, galerie_id, artiste_id FROM evenement ORDER BY id DESC";
    private static final String DELETE_SQL = "DELETE FROM evenement WHERE id = ?";

    @Override
    public void add(Evenement evenement) throws SQLDataException {
        throw new SQLDataException("Ajout des evenements non supporte pour le moment");
    }

    @Override
    public void delete(Evenement evenement) throws SQLDataException {
        if (evenement == null || evenement.getId() == null) {
            throw new SQLDataException("Impossible de supprimer un evenement sans ID");
        }

        try (PreparedStatement statement = MyDatabase.getInstance().getConnection().prepareStatement(DELETE_SQL)) {
            statement.setInt(1, evenement.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException("Erreur lors de la suppression de l'evenement: " + e.getMessage());
        }
    }

    @Override
    public void update(Evenement evenement) throws SQLDataException {
        throw new SQLDataException("Modification des evenements non supportee pour le moment");
    }

    @Override
    public List<Evenement> getAll() throws SQLDataException {
        List<Evenement> evenements = new ArrayList<>();

        try (Statement statement = MyDatabase.getInstance().getConnection().createStatement();
             ResultSet resultSet = statement.executeQuery(SELECT_ALL_SQL)) {

            while (resultSet.next()) {
                Evenement evenement = new Evenement();
                evenement.setId(resultSet.getInt("id"));
                evenement.setTitre(resultSet.getString("titre"));
                evenement.setDescription(resultSet.getString("description"));

                Timestamp dateDebut = resultSet.getTimestamp("date_debut");
                evenement.setDateDebut(dateDebut == null ? null : dateDebut.toLocalDateTime());

                Timestamp dateFin = resultSet.getTimestamp("date_fin");
                evenement.setDateFin(dateFin == null ? null : dateFin.toLocalDateTime());

                Date dateCreation = resultSet.getDate("date_creation");
                evenement.setDateCreation(dateCreation == null ? null : dateCreation.toLocalDate());

                evenement.setType(resultSet.getString("type"));
                evenement.setImageCouverture(resultSet.getBytes("image_couverture"));
                evenement.setStatut(resultSet.getString("statut"));

                Number capaciteMax = (Number) resultSet.getObject("capacite_max");
                evenement.setCapaciteMax(capaciteMax == null ? null : capaciteMax.intValue());

                Number prixTicket = (Number) resultSet.getObject("prix_ticket");
                evenement.setPrixTicket(prixTicket == null ? null : prixTicket.doubleValue());

                Number galerieId = (Number) resultSet.getObject("galerie_id");
                evenement.setGalerieId(galerieId == null ? null : galerieId.intValue());

                Number artisteId = (Number) resultSet.getObject("artiste_id");
                evenement.setArtisteId(artisteId == null ? null : artisteId.intValue());
                evenements.add(evenement);
            }
        } catch (SQLException e) {
            throw new SQLDataException("Erreur lors de la recuperation des evenements: " + e.getMessage());
        }

        return evenements;
    }
}


