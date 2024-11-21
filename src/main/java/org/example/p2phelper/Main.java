package org.example.p2phelper;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.p2phelper.controller.WriteNumbersToFile;


import java.io.IOException;

public class Main extends Application {
    private static HelloApplication helloApp;
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 679, 375);
        stage.setTitle("P2PHelper");
        stage.setScene(scene);
        stage.show();
        stage.setOnHidden(e -> System.exit(0));
        helloApp = fxmlLoader.getController();
        Thread writerThread = new Thread(() -> {
            try {
                WriteNumbersToFile.writeNumbers(Main.getHelloApp());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        writerThread.setDaemon(true); // Делаем поток демоном, чтобы он не блокировал завершение приложения
        writerThread.start();
       
    }
    public static HelloApplication getHelloApp() { // Метод для доступа к экземпляру HelloApplication
        return helloApp;
    }
    public static void main(String[] args) {
        launch();
    }
}