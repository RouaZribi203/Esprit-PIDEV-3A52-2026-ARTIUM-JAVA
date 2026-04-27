package services;

import entities.ReclamationNotification;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ReclamationNotificationService {

    private final Connection connection;

    private static final String SELECT_LATEST_BY_USER_SQL =
            "SELECT r.id AS reponse_id, r.contenu AS reponse_contenu, r.date_reponse, " +
                    "r.reclamation_id, rec.type AS reclamation_type, rec.texte AS reclamation_texte " +
                    "FROM reponse r " +
                    "INNER JOIN reclamation rec ON rec.id = r.reclamation_id " +
                    "WHERE rec.user_id = ? " +
                    "ORDER BY r.date_reponse DESC, r.id DESC " +
                    "LIMIT ?";

    public ReclamationNotificationService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

     public List<ReclamationNotification> getLatestByUser(int userId, int limit) throws SQLDataException {
        List<ReclamationNotification> notifications = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(SELECT_LATEST_BY_USER_SQL)) {
            ps.setInt(1, userId);
            ps.setInt(2, Math.max(1, limit));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new SQLDataException("Erreur lors du chargement des notifications: " + e.getMessage());
        }

        return notifications;
    }

    private ReclamationNotification mapRow(ResultSet rs) throws SQLException {
        ReclamationNotification notification = new ReclamationNotification();

        int reponseId = rs.getInt("reponse_id");
        if (!rs.wasNull()) {
            notification.setReponseId(reponseId);
        }

        int reclamationId = rs.getInt("reclamation_id");
        if (!rs.wasNull()) {
            notification.setReclamationId(reclamationId);
        }

        notification.setReponseContenu(rs.getString("reponse_contenu"));
        notification.setReclamationType(rs.getString("reclamation_type"));
        notification.setReclamationTexte(rs.getString("reclamation_texte"));

        Timestamp dateReponse = rs.getTimestamp("date_reponse");
        if (dateReponse != null) {
            notification.setReponseDate(dateReponse.toLocalDateTime());
        }

        return notification;
    }
}

