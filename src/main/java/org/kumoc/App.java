package org.kumoc;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void init() throws Exception {
        AppContext.inicializar();
    }

    @Override
    public void start(Stage stage) throws Exception {

        Parent raiz = FXMLLoader.load(App.class.getResource("/view/MainView.fxml"));

        Scene escena = new Scene(raiz, 1280, 760);
        escena.getStylesheets().add(App.class.getResource("/css/kumoc.css").toExternalForm());

        stage.setTitle("KUMOC");
        stage.getIcons().add(new Image(App.class.getResourceAsStream("/img/logo-cuadrado.png")));
        stage.setMinWidth(1000);
        stage.setMinHeight(620);
        stage.setScene(escena);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        AppContext.cerrar();
    }
}
