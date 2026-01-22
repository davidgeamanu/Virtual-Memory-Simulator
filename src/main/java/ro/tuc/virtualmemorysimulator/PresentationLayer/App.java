package ro.tuc.virtualmemorysimulator.PresentationLayer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/ro/tuc/virtualmemorysimulator/SimulatorViewModern.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1520, 820);
        stage.setTitle("Virtual Memory Simulator");
        stage.setMinWidth(1200);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }


    public static void main(String[] args) {
        launch();
    }
}