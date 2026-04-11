package services;

import entities.User;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicePersonne implements Iservice <User> {
    private Connection connection;
    public ServicePersonne() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void add(User user) throws SQLDataException {
        String sql = "INSERT INTO personne (age, nom, prenom) VALUES ('"
                + user.getAge() + "', '"
                + user.getNom() + "', '"
                + user.getPrenom() + "')";

        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void delete(User user) throws SQLDataException {

    }

    @Override
    public void update(User user) throws SQLDataException {
        String sql = "UPDATE personne SET nom = ?, prenom = ? , age = ? WHERE id = ?";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, user.getNom());
            preparedStatement.setString(2, user.getPrenom());
            preparedStatement.setInt(3, user.getAge());
            preparedStatement.setInt(4, user.getId());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e.getMessage());        }

    }


    @Override
    public List<User> getAll() throws SQLDataException {
        String sql = "SELECT * FROM personne";
        List<User> userList = null;
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            userList = new ArrayList<>();
            while (rs.next()) {
                User p = new User();
                p.setId(rs.getInt(1));
                p.setAge(rs.getInt("age"));
                p.setNom(rs.getString("nom"));
                p.setPrenom(rs.getString("prenom"));
                userList.add(p);

            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return userList;
    }
}
