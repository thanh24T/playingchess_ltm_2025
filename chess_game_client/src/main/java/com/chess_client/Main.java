package com.chess_client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import com.chess_client.controllers.LoginController;
import com.chess_client.services.AuthService;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Default single-window behavior preserved, but we also support
        // launching multiple windows programmatically via command line args.
        stage.getIcons().add(new Image(
                getClass().getResourceAsStream("/com/chess_client/images/logo.png")));

        // If args include -n <count>, open that many windows (one is the primary stage)
        int toOpen = 1;
        try {
            String[] args = getParameters().getRaw().toArray(new String[0]);
            for (int i = 0; i < args.length; i++) {
                if (("-n".equals(args[i]) || "--multi".equals(args[i])) && i + 1 < args.length) {
                    toOpen = Math.max(1, Integer.parseInt(args[i + 1]));
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        // Use WindowFactory to create stages
        if (toOpen <= 1) {
            Stage s = com.chess_client.ui.WindowFactory.createLoginStage();
            
            s.getIcons().add(new Image(getClass().getResourceAsStream("/com/chess_client/images/logo.png")));
            s.setOnCloseRequest(event -> {
                AuthService.signOutSync();
                Platform.exit();
                System.exit(0);
            });
            s.show();
        } else {
            // open 'toOpen' windows; primary stage is hidden
            for (int i = 0; i < toOpen; i++) {
                Stage s = com.chess_client.ui.WindowFactory.createLoginStage();
                s.getIcons().add(new Image(getClass().getResourceAsStream("/com/chess_client/images/logo.png")));
                // When the last window closes, exit app
                if (i == toOpen - 1) {
                    s.setOnCloseRequest(event -> {
                        AuthService.signOutSync();
                        Platform.exit();
                        System.exit(0);
                    });
                }
                s.show();
            }
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
