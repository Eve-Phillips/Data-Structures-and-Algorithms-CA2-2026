package ie.setu.gallery.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GalleryApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/gallery-view.fxml")
        );

        Scene scene = new Scene(loader.load(), 1000, 700);
        stage.setTitle("National Gallery Route Finder");
        stage.setScene(scene);
        stage.show();
    }
}