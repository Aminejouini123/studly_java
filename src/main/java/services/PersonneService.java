package services;

import models.Personne;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PersonneService implements IService<Personne> {

    private Connection connection;

    public PersonneService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Personne personne) throws SQLException {
        String sql = "insert into personne (nom, prenom,age) " +
                "values('" + personne.getNom() + "','" + personne.getPrenom() + "'" +
                "," + personne.getAge() + ")";
        Statement statement = connection.createStatement();
        statement.executeUpdate(sql);
    }

    @Override
    public void modifier(Personne personne) throws SQLException {
        String sql = "update personne set nom = ?, prenom = ?, age = ? where id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, personne.getNom());
        preparedStatement.setString(2, personne.getPrenom());
        preparedStatement.setInt(3, personne.getAge());
        preparedStatement.setInt(4, personne.getId());
        preparedStatement.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "delete from personne where id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setInt(1, id);
        preparedStatement.executeUpdate();
    }

    @Override
    public List<Personne> recuperer() throws SQLException {
        String sql = "select * from personne";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        List<Personne> personnes = new ArrayList<>();
        while (rs.next()) {
            Personne p = new Personne();
            p.setId(rs.getInt("id"));
            p.setAge(rs.getInt("age"));
            p.setNom(rs.getString("nom"));
            p.setPrenom(rs.getString("prenom"));

            personnes.add(p);
        }
        return personnes;
    }
}
