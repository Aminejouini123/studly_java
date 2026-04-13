package test;

import models.Personne;
import services.PersonneService;

import java.sql.SQLException;

public class Main {


    public static void main(String[] args) {
        PersonneService ps = new PersonneService();

        try {
            ps.ajouter(new Personne(24, "Ben Foulen", "Foulen"));
            System.out.println("Personne ajouté");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        try {
            ps.modifier(new Personne(1, 24, "nsit", "Billal"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
//

        try {
            System.out.println(ps.recuperer());
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

    }
}
