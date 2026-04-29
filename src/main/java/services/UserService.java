package services;

import entities.User;
import utils.ImageUrlUtils;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.regex.Pattern;

public class UserService implements Iservice<User> {

    private static final String HASH_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int HASH_ITERATIONS = 65536;
    private static final int HASH_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final String[] ID_COLUMNS = { "id", "id_user" };
    private static final String[] SPECIALITE_COLUMNS = { "specialite", "specailite" };
    private static final String[] CENTRE_INTERET_COLUMNS = { "centre_interet", "centre_iteret" };
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final Connection connection;
    private final String idColumn;
    private final String specialiteColumn;
    private final String centreInteretColumn;

    public UserService() {
        this.connection = MyDatabase.getInstance().getConnection();
        this.idColumn = resolveExistingColumn(ID_COLUMNS);
        this.specialiteColumn = resolveExistingColumn(SPECIALITE_COLUMNS);
        this.centreInteretColumn = resolveExistingColumn(CENTRE_INTERET_COLUMNS);
    }

    @Override
    public void add(User user) throws SQLDataException {
        if (user == null) {
            throw new SQLDataException("Utilisateur invalide.");
        }
        ensureConnection();

        String normalizedRole = normalizeRole(user.getRole());
        user.setRole(normalizedRole);
        normalizeImageFields(user);
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

    public User authenticate(String email, String rawPassword) throws SQLDataException {
        if (isBlank(email) || isBlank(rawPassword)) {
            throw new SQLDataException("Veuillez saisir votre e-mail et votre mot de passe.");
        }
        ensureConnection();

        String sql = "SELECT * FROM `user` WHERE LOWER(TRIM(`email`)) = LOWER(TRIM(?)) LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLDataException("Adresse e-mail ou mot de passe incorrect.");
                }

                String storedPassword = rs.getString("mdp");
                if (!matchesPassword(rawPassword, storedPassword)) {
                    throw new SQLDataException("Adresse e-mail ou mot de passe incorrect.");
                }

                return mapUser(rs);
            }
        } catch (SQLDataException e) {
            throw e;
        } catch (SQLException e) {
            throw new SQLDataException("Authentification impossible: " + e.getMessage());
        }
    }

    public void resetPasswordByEmail(String email, String newPassword) throws SQLDataException {
        if (isBlank(email) || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new SQLDataException("Veuillez saisir une adresse e-mail valide.");
        }
        if (isBlank(newPassword) || newPassword.length() < 8) {
            throw new SQLDataException("Le mot de passe doit contenir au moins 8 caractères.");
        }
        ensureConnection();

        String updateSql = "UPDATE `user` SET `mdp` = ? WHERE LOWER(TRIM(`email`)) = LOWER(TRIM(?))";
        try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
            ps.setString(1, hashPassword(newPassword));
            ps.setString(2, email.trim());
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLDataException("Aucun compte trouvé avec cette adresse e-mail.");
            }
        } catch (SQLDataException e) {
            throw e;
        } catch (SQLException e) {
            throw new SQLDataException("Réinitialisation impossible: " + e.getMessage());
        }
    }

    private String getInsertUserSql() {
        return "INSERT INTO `user` " +
                "(`nom`, `prenom`, `date_naissance`, `email`, `mdp`, `role`, `statut`, `date_inscription`, " +
                "`num_tel`, `ville`, `biographie`, `" + specialiteColumn + "`, `" + centreInteretColumn
                + "`, `photo_reference_path`, `photo_profil`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        if (idColumn != null) {
            int rawId = rs.getInt(idColumn);
            if (!rs.wasNull()) {
                user.setId(rawId);
            }
        }
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setDateNaissance(rs.getDate("date_naissance") == null ? null : rs.getDate("date_naissance").toLocalDate());
        user.setEmail(rs.getString("email"));
        user.setMdp(rs.getString("mdp"));
        user.setRole(rs.getString("role"));
        user.setStatut(rs.getString("statut"));
        user.setDateInscription(
                rs.getDate("date_inscription") == null ? null : rs.getDate("date_inscription").toLocalDate());
        user.setNumTel(rs.getString("num_tel"));
        user.setVille(rs.getString("ville"));
        user.setBiographie(rs.getString("biographie"));
        user.setSpecialite(safeGetString(rs, specialiteColumn));
        user.setCentreInteret(safeGetString(rs, centreInteretColumn));
        user.setPhotoReferencePath(ImageUrlUtils.normalizeForDatabase(rs.getString("photo_reference_path")));
        user.setPhotoProfil(ImageUrlUtils.normalizeForDatabase(rs.getString("photo_profil")));
        return user;
    }

    private void normalizeImageFields(User user) throws SQLDataException {
        user.setPhotoReferencePath(ImageUrlUtils.persistToWebImageDirectoryAndNormalize(user.getPhotoReferencePath()));
        user.setPhotoProfil(ImageUrlUtils.persistToWebImageDirectoryAndNormalize(user.getPhotoProfil()));
    }

    private String safeGetString(ResultSet rs, String column) throws SQLException {
        return column == null ? null : rs.getString(column);
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
        if (specialiteColumn == null)
            missingSchemaColumns.add("specialite/specailite");
        if (centreInteretColumn == null)
            missingSchemaColumns.add("centre_interet/centre_iteret");

        if (!missingSchemaColumns.isEmpty()) {
            throw new SQLDataException(
                    "Colonnes introuvables dans table user: " + String.join(", ", missingSchemaColumns));
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

        if (isBlank(user.getNom()))
            missingFields.add("nom");
        if (isBlank(user.getPrenom()))
            missingFields.add("prenom");
        if (user.getDateNaissance() == null)
            missingFields.add("date_naissance");
        if (isBlank(user.getEmail()))
            missingFields.add("email");
        if (isBlank(user.getMdp()))
            missingFields.add("mdp");
        if (isBlank(user.getRole()))
            missingFields.add("role");
        if (isBlank(user.getStatut()))
            missingFields.add("statut");
        if (user.getDateInscription() == null)
            missingFields.add("date_inscription");
        if (isBlank(user.getNumTel()))
            missingFields.add("num_tel");
        if (isBlank(user.getVille()))
            missingFields.add("ville");

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

    private boolean matchesPassword(String rawPassword, String storedPassword) throws SQLDataException {
        if (isBlank(storedPassword)) {
            return false;
        }

        if (!storedPassword.startsWith("pbkdf2_sha256$")) {
            return rawPassword.equals(storedPassword);
        }

        String[] parts = storedPassword.split("\\$");
        if (parts.length != 4) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);

            PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterations, expectedHash.length * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(HASH_ALGORITHM);
            byte[] computedHash = factory.generateSecret(spec).getEncoded();
            return MessageDigest.isEqual(expectedHash, computedHash);
        } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SQLDataException("Verification du mot de passe impossible: " + e.getMessage());
        }
    }

    @Override
    public void delete(User user) throws SQLDataException {
        if (user == null || user.getId() == null) {
            throw new SQLDataException("Suppression impossible: identifiant utilisateur manquant.");
        }
        ensureConnection();
        ensureIdColumn();

        String sql = "DELETE FROM `user` WHERE `" + idColumn + "` = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, user.getId());
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLDataException("Aucun utilisateur supprime (id introuvable).");
            }
        } catch (SQLDataException e) {
            throw e;
        } catch (SQLException e) {
            throw new SQLDataException("Suppression utilisateur impossible: " + e.getMessage());
        }
    }

    @Override
    public void update(User user) throws SQLDataException {
        if (user == null || user.getId() == null) {
            throw new SQLDataException("Mise a jour impossible: identifiant utilisateur manquant.");
        }

        ensureConnection();
        ensureIdColumn();

        String normalizedRole = normalizeRole(user.getRole());
        user.setRole(normalizedRole);
        normalizeImageFields(user);
        applyRoleRules(user, normalizedRole);
        validateSchemaColumns();
        validateRequiredFieldsForUpdate(user);

        String sql = buildUpdateSql(!isBlank(user.getMdp()));
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, user.getNom());
            ps.setString(idx++, user.getPrenom());
            ps.setDate(idx++, user.getDateNaissance() == null ? null : Date.valueOf(user.getDateNaissance()));
            ps.setString(idx++, user.getEmail());
            ps.setString(idx++, user.getRole());
            ps.setString(idx++, user.getStatut());
            ps.setDate(idx++, user.getDateInscription() == null ? null : Date.valueOf(user.getDateInscription()));
            ps.setString(idx++, user.getNumTel());
            ps.setString(idx++, user.getVille());
            ps.setString(idx++, user.getBiographie());
            ps.setString(idx++, user.getSpecialite());
            ps.setString(idx++, user.getCentreInteret());
            ps.setString(idx++, user.getPhotoReferencePath());
            ps.setString(idx++, user.getPhotoProfil());

            if (!isBlank(user.getMdp())) {
                ps.setString(idx++, hashPassword(user.getMdp()));
            }
            ps.setInt(idx, user.getId());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLDataException("Aucun utilisateur mis a jour (id introuvable).");
            }
        } catch (SQLDataException e) {
            throw e;
        } catch (SQLException e) {
            throw new SQLDataException("Mise a jour utilisateur impossible: " + e.getMessage());
        }
    }

    public List<User> getByRole(String role) throws SQLDataException {
        ensureConnection();
        String normalizedRole = normalizeRole(role);

        String orderBy = idColumn != null ? ("`" + idColumn + "`") : "`date_inscription`";
        String sql = "SELECT * FROM `user` WHERE LOWER(TRIM(`role`)) = LOWER(TRIM(?)) ORDER BY " + orderBy + " DESC";
        List<User> users = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, normalizedRole);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new SQLDataException("Lecture utilisateurs par role impossible: " + e.getMessage());
        }
        return users;
    }

    @Override
    public List<User> getAll() throws SQLDataException {
        ensureConnection();

        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = connection
                .prepareStatement("SELECT * FROM `user` ORDER BY `date_inscription` DESC");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            throw new SQLDataException("Lecture utilisateurs impossible: " + e.getMessage());
        }
        return users;
    }

    @Override
    public User getById(int id) throws SQLDataException {
        return null;
    }

    private String buildUpdateSql(boolean withPasswordUpdate) {
        String sql = "UPDATE `user` SET " +
                "`nom` = ?, `prenom` = ?, `date_naissance` = ?, `email` = ?, `role` = ?, `statut` = ?, " +
                "`date_inscription` = ?, `num_tel` = ?, `ville` = ?, `biographie` = ?, " +
                "`" + specialiteColumn + "` = ?, `" + centreInteretColumn + "` = ?, " +
                "`photo_reference_path` = ?, `photo_profil` = ?";
        if (withPasswordUpdate) {
            sql += ", `mdp` = ?";
        }
        sql += " WHERE `" + idColumn + "` = ?";
        return sql;
    }

    private void validateRequiredFieldsForUpdate(User user) throws SQLDataException {
        List<String> missingFields = new ArrayList<>();
        if (isBlank(user.getNom()))
            missingFields.add("nom");
        if (isBlank(user.getPrenom()))
            missingFields.add("prenom");
        if (user.getDateNaissance() == null)
            missingFields.add("date_naissance");
        if (isBlank(user.getEmail()))
            missingFields.add("email");
        if (isBlank(user.getRole()))
            missingFields.add("role");
        if (isBlank(user.getStatut()))
            missingFields.add("statut");
        if (user.getDateInscription() == null)
            missingFields.add("date_inscription");
        if (isBlank(user.getNumTel()))
            missingFields.add("num_tel");
        if (isBlank(user.getVille()))
            missingFields.add("ville");

        if (!missingFields.isEmpty()) {
            throw new SQLDataException("Champs obligatoires manquants: " + String.join(", ", missingFields));
        }
    }

    private void ensureConnection() throws SQLDataException {
        if (connection == null) {
            throw new SQLDataException("Connexion base de donnees indisponible.");
        }
    }

    private void ensureIdColumn() throws SQLDataException {
        if (idColumn == null) {
            throw new SQLDataException("Colonne identifiant utilisateur introuvable (id/id_user).");
        }
    }
}
