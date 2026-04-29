package ie.setu.gallery.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class GalleryApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        URL fxmlUrl = getClass().getResource("/ie/setu/gallery/gallery-view.fxml");

        if (fxmlUrl == null) {
            throw new IllegalStateException("Could not find gallery-view.fxml in /ie/setu/gallery/");
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);

        Scene scene = new Scene(loader.load(), 1100, 650);
        stage.setTitle("National Gallery Route Finder");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}