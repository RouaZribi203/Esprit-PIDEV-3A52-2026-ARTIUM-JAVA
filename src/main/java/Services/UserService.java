package Services;

import entities.User;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class UserService implements Iservice<User> {

    private static final String HASH_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int HASH_ITERATIONS = 65536;
    private static final int HASH_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final String[] SPECIALITE_COLUMNS = {"specialite", "specailite"};
    private static final String[] CENTRE_INTERET_COLUMNS = {"centre_interet", "centre_iteret"};

    private final Connection connection;
    private final String specialiteColumn;
    private final String centreInteretColumn;

    public UserService() {
        this.connection = MyDatabase.getInstance().getConnection();
        this.specialiteColumn = resolveExistingColumn(SPECIALITE_COLUMNS);
        this.centreInteretColumn = resolveExistingColumn(CENTRE_INTERET_COLUMNS);
    }

    @Override
    public void add(User user) throws SQLDataException {
        if (user == null) {
            throw new SQLDataException("Utilisateur invalide.");
        }
        if (connection == null) {
            throw new SQLDataException("Connexion base de donnees indisponible.");
        }

        String normalizedRole = normalizeRole(user.getRole());
        user.setRole(normalizedRole);
        applyRoleRules(user, normalizedRole);
        validateRequiredFields(user);
        validateSchemaColumns();

        user.setMdp(hashPassword(user.getMdp()));

        String insertUserSql = getInsertUserSql();
        try (PreparedStatement ps = connection.prepareStatement(insertUserSql)) {
            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setDate(3, user.getDateNaissance() == null ? null : Date.valueOf(user.getDateNaissance()));
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getMdp());
            ps.setString(6, user.getRole());
            ps.setString(7, user.getStatut());
            ps.setDate(8, user.getDateInscription() == null ? null : Date.valueOf(user.getDateInscription()));
            ps.setString(9, user.getNumTel());
            ps.setString(10, user.getVille());
            ps.setString(11, user.getBiographie());
            ps.setString(12, user.getSpecialite());
            ps.setString(13, user.getCentreInteret());
            ps.setString(14, user.getPhotoReferencePath());
            ps.setString(15, user.getPhotoProfil());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException("Insertion utilisateur impossible: " + e.getMessage());
        }
    }

    private String getInsertUserSql() {
        return "INSERT INTO `user` " +
                "(`nom`, `prenom`, `date_naissance`, `email`, `mdp`, `role`, `statut`, `date_inscription`, " +
                "`num_tel`, `ville`, `biographie`, `" + specialiteColumn + "`, `" + centreInteretColumn + "`, `photo_reference_path`, `photo_profil`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    private String normalizeRole(String role) throws SQLDataException {
        if (isBlank(role)) {
            throw new SQLDataException("Role invalide.");
        }

        if ("admin".equalsIgnoreCase(role.trim())) {
            return "Admin";
        }
        if ("amateur".equalsIgnoreCase(role.trim())) {
            return "Amateur";
        }
        if ("artiste".equalsIgnoreCase(role.trim())) {
            return "Artiste";
        }
        throw new SQLDataException("Role non supporte: " + role);
    }

    private void applyRoleRules(User user, String role) throws SQLDataException {
        if ("Admin".equals(role)) {
            user.setSpecialite(null);
            user.setCentreInteret(null);
            return;
        }

        if ("Amateur".equals(role)) {
            user.setSpecialite(null);
            if (isBlank(user.getCentreInteret())) {
                throw new SQLDataException("Un amateur doit choisir un centre d'interet.");
            }
            return;
        }

        if ("Artiste".equals(role)) {
            user.setCentreInteret(null);
            if (isBlank(user.getSpecialite())) {
                throw new SQLDataException("Un artiste doit choisir une specialite.");
            }
        }
    }

    private void validateSchemaColumns() throws SQLDataException {
        List<String> missingSchemaColumns = new ArrayList<>();
        if (specialiteColumn == null) missingSchemaColumns.add("specialite/specailite");
        if (centreInteretColumn == null) missingSchemaColumns.add("centre_interet/centre_iteret");

        if (!missingSchemaColumns.isEmpty()) {
            throw new SQLDataException("Colonnes introuvables dans table user: " + String.join(", ", missingSchemaColumns));
        }
    }

    private String resolveExistingColumn(String[] candidates) {
        if (connection == null) {
            return null;
        }

        try {
            DatabaseMetaData metaData = connection.getMetaData();
            for (String candidate : candidates) {
                if (columnExists(metaData, "user", candidate)) {
                    return candidate;
                }
            }
        } catch (SQLException ignored) {
            return null;
        }
        return null;
    }

    private boolean columnExists(DatabaseMetaData metaData, String tableName, String columnName) throws SQLException {
        try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }

    private void validateRequiredFields(User user) throws SQLDataException {
        List<String> missingFields = new ArrayList<>();

        if (isBlank(user.getNom())) missingFields.add("nom");
        if (isBlank(user.getPrenom())) missingFields.add("prenom");
        if (user.getDateNaissance() == null) missingFields.add("date_naissance");
        if (isBlank(user.getEmail())) missingFields.add("email");
        if (isBlank(user.getMdp())) missingFields.add("mdp");
        if (isBlank(user.getRole())) missingFields.add("role");
        if (isBlank(user.getStatut())) missingFields.add("statut");
        if (user.getDateInscription() == null) missingFields.add("date_inscription");
        if (isBlank(user.getNumTel())) missingFields.add("num_tel");
        if (isBlank(user.getVille())) missingFields.add("ville");

        if (!missingFields.isEmpty()) {
            throw new SQLDataException("Champs obligatoires manquants: " + String.join(", ", missingFields));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String hashPassword(String rawPassword) throws SQLDataException {
        try {
            byte[] salt = new byte[SALT_LENGTH];
            new SecureRandom().nextBytes(salt);
            PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, HASH_ITERATIONS, HASH_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(HASH_ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return "pbkdf2_sha256$" + HASH_ITERATIONS + "$" +
                    Base64.getEncoder().encodeToString(salt) + "$" +
                    Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SQLDataException("Hachage du mot de passe impossible: " + e.getMessage());
        }
    }

    @Override
    public void delete(User user) throws SQLDataException {
        throw new SQLDataException("Delete non implemente.");
    }

    @Override
    public void update(User user) throws SQLDataException {
        throw new SQLDataException("Update non implemente.");
    }

    @Override
    public List<User> getAll() throws SQLDataException {
        if (connection == null) {
            throw new SQLDataException("Connexion base de donnees indisponible.");
        }

        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM `user`");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User user = new User();
                user.setNom(rs.getString("nom"));
                user.setPrenom(rs.getString("prenom"));
                user.setEmail(rs.getString("email"));
                user.setRole(rs.getString("role"));
                users.add(user);
            }
        } catch (SQLException e) {
            throw new SQLDataException("Lecture utilisateurs impossible: " + e.getMessage());
        }
        return users;
    }
}







