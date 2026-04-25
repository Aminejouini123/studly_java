module studly.java {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing;
    requires javafx.media;
    requires java.sql;
    requires mysql.connector.j;
    requires java.desktop;
    requires org.apache.pdfbox;
    requires java.xml;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;

    // Open packages for FXML injection
    opens controllers to javafx.fxml;
    opens controllers.activities to javafx.fxml;
    opens controllers.backend to javafx.fxml;
    opens controllers.courses to javafx.fxml;
    opens controllers.exams to javafx.fxml;
    opens controllers.gestiondetemps to javafx.fxml;
    opens controllers.group to javafx.fxml;
    opens controllers.user_controller to javafx.fxml;
    
    // Open models for TableView access
    opens models to javafx.base;
    opens models.chat to javafx.base;
    opens models.quiz to javafx.base;
    
    // Open main/test for graphics/fxml
    opens test to javafx.graphics, javafx.fxml;

    // Export services and other needed packages
    exports test;
    exports models;
    exports services;
    exports services.audio;
    exports services.chat;
    exports controllers;
    exports controllers.activities;
    exports controllers.courses;
    exports controllers.exams;
    exports utils;
}
