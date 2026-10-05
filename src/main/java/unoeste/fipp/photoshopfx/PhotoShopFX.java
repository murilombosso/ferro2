package unoeste.fipp.photoshopfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class PhotoShopFX extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(PhotoShopFX.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        MainController controller = fxmlLoader.getController();
        stage.setTitle("PhotoPaintFX");
        stage.setMaximized(true);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> { if (!controller.fecharTodas()) event.consume(); });
        stage.show();
    }
}
