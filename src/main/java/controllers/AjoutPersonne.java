package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import models.Personne;
import services.PersonneService;

import java.sql.SQLException;

public class AjoutPersonne {

    @FXML
    private Button buttonID;

    @FXML
    private TextField nomID;

    @FXML
    private TextField prenomID;

    @FXML
    void ajouterPersonne(ActionEvent event) throws SQLException {

        if (nomID.getText().isEmpty() || prenomID.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Yahdiik 3ammer les champs!");
            alert.show();

        }

        PersonneService personneService = new PersonneService();
        personneService.ajouter(new Personne(nomID.getText(),nomID.getText()));

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

    }

}
