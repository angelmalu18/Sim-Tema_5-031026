package com.angelsystems.simulacionbloquera;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                MainApplication.class.getResource("main-view.fxml")
        );
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Simulacion | Industria Bloquera del Sureste");
        stage.setScene(scene);
        // Ventana normal con minimizar / maximizar / cerrar
        stage.setResizable(true);
        stage.setMaximized(true);
        stage.show();
    }
}
