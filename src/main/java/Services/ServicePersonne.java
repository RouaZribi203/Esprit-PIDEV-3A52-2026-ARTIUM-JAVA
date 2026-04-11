package Services;

import Models.Personne;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicePersonne implements Iservice <Personne> {
    private Connection connection;
    public ServicePersonne() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Personne personne) throws SQLDataException {
        String sql = "INSERT INTO personne (age, nom, prenom) VALUES ('"
                + personne.getAge() + "', '"
                + personne.getNom() + "', '"
                + personne.getPrenom() + "')";

        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void supprimer(Personne personne) throws SQLDataException {

    }

    @Override
    public void modifier(Personne personne) throws SQLDataException {
        String sql = "UPDATE personne SET nom = ?, prenom = ? , age = ? WHERE id = ?";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, personne.getNom());
            preparedStatement.setString(2, personne.getPrenom());
            preparedStatement.setInt(3, personne.getAge());
            preparedStatement.setInt(4, personne.getId());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e.getMessage());        }

    }


    @Override
    public List<Personne> recuperer() throws SQLDataException {
        String sql = "SELECT * FROM personne";
        List<Personne> personneList = null;
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            personneList = new ArrayList<>();
            while (rs.next()) {
                Personne p = new Personne();
                p.setId(rs.getInt(1));
                p.setAge(rs.getInt("age"));
                p.setNom(rs.getString("nom"));
                p.setPrenom(rs.getString("prenom"));
                personneList.add(p);

            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return personneList;
    }
}
