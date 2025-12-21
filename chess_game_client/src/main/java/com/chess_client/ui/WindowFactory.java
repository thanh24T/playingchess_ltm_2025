package com.chess_client.ui;

import com.chess_client.controllers.LoginController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class WindowFactory {

    /**
     * Create a new Stage showing the login screen.
     * Caller must run this on the JavaFX Application Thread (or via Platform.runLater).
     */
    public static Stage createLoginStage() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(WindowFactory.class.getResource("/com/chess_client/fxml/login.fxml"));
        Parent root = fxmlLoader.load();

        Stage stage = new Stage();
        stage.setScene(new Scene(root, 930, 740));
        stage.setResizable(false);
        stage.setTitle("Chess Game");

        LoginController controller = fxmlLoader.getController();
        controller.setStage(stage);

        return stage;
    }
}
