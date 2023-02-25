package com.scout;

/**
  FusionCorps 6672 Scouting App
  Built with JavaFX framework
  @author FusionCorps-Rishabh Rengarajan
  @version 1.6.9

  each page is a separate scene, whose layout is defined in a separate FXML file

  UI Utility classes (in package com.scout.ui):
 * AlertBox.java - pop-up box for error messages
 * TimerText.java - timer for balance timing
 * LimitedTextField.java - restrictive text fields for user input

 Other Utility classes (in package com.scout.util):
 * QRFuncs.java - specific implementations of the ZXing library for generating, displaying, and writing QR codes
 * CopyImageToClipboard.java - copies output QR code/raw text data to clipboard for debugging/extreme circumstances

 AppMain.java - main class, launches app
 AppRun.java - main class for running app as an executable JAR
 FXMLController.java is the main controller that controls all scenes. It handles all logic, such as user input and data storage.

 Layout of scenes is in package resources.com.scout.scenes
 CSS is located in package resources.com.scout.css
 Images are located in package resources.com.scout.images
 **/

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class AppMain extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLController.setPage(primaryStage, 0);
    }

    public static void main(String[] args) {
        launch();
    }
}

