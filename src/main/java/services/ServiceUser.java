package services;

import entities.User;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Service for user registration and lookup.
 */
public class ServiceUser {

    private final Connection connection = MyDatabase.getInstance().getConnection();

    /**
     * Inserts a new user into the database.
     *
     * @param user the user to persist; must have prenom, nom, email, mdp, role set.
     * @throws SQLException on database error
     */
    public void add(User user) throws SQLException {
        String sql = "INSERT INTO user (nom, prenom, date_naissance, email, mdp, role, statut, date_inscription)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setDate(3, user.getDateNaissance() != null ? Date.valueOf(user.getDateNaissance()) : null);
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getMdp());
            ps.setString(6, user.getRole());
            ps.setString(7, user.getStatut() != null ? user.getStatut() : "active");
            ps.setDate(8, Date.valueOf(LocalDate.now()));
            ps.executeUpdate();
        }
    }

    /**
     * Returns true if an account with the given email already exists.
     *
     * @param email email address to check
     * @throws SQLException on database error
     */
    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}
