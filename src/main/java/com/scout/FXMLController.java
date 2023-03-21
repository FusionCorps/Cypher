//TODO (long-term restructuring) EFFICIENCY: how to encapsulate/declare data fields in more efficient way (e.g. maybe hashmap for each field, like [Object:fx_id]?)
//TODO tidy up writeToCSV() and outputAll() method
package com.scout;

import com.scout.ui.AlertBox;
import com.scout.ui.LimitedTextField;
import com.scout.util.CopyImageToClipBoard;
import com.scout.util.QRFuncs;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.controlsfx.control.Rating;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

public class FXMLController {
    /**
     * scene0:begin
     * scene1:pregame
     * scene2:auton
     * scene3:teleop
     * scene4:endgame
     * scene5:qualitative notes
     * scene6:QR CODE
     */

    private static LinkedHashMap<String, String> info = new LinkedHashMap<>(); //stores user input data
    private static HashMap<String, Integer> toggleMap = new HashMap<>() {{
        putIfAbsent("alliance", null);
        putIfAbsent("startLocation", null);
        putIfAbsent("preload", null);
        putIfAbsent("autoBalance", null);
        putIfAbsent("teleopBalance", null);
        putIfAbsent("drivetrainType", null);
    }}; //stores toggle group values

    private static int sceneIndex = 0;  //used for changing pages
    private static StringBuilder data = new StringBuilder(); //used to build data output string in sendInfo()
    private static boolean isNextPageClicked = false; //used in initialize() to prevent data from being sent to info HashMap before user clicks next page
    private static String autonColor = "R"; //for changing color in auton pickup grid
    private static boolean PNGflipped = false; //for flipping starting location image

    //data for each page, variables are named the same as corresponding fx:ids in fxml files for consistency

    //page 1 - pregame
    @FXML private LimitedTextField teamNum; //team number
    @FXML private LimitedTextField matchNum; //match number
    @FXML private ToggleGroup alliance; //robot alliance
    @FXML private ComboBox<String> driveStation;
    @FXML private ToggleGroup startLocation; //starting location

    @FXML private ImageView startLocationPNG; //starting location image
    @FXML private Text teamNameText;
    //page 2 - auton
    @FXML private ToggleGroup preload; // GP type preload
    @FXML private CheckBox mobility; //mobility
    private static final ArrayList<Integer> autoPickups = new ArrayList<>(); //GP intaked at community
    private static final ArrayList<Integer> autoCones = new ArrayList<>(); //cones placed
    private static final ArrayList<Integer> autoCubes = new ArrayList<>(); //cubes placed
    @FXML private ToggleGroup autoBalance; //auton balance status

    @FXML private GridPane a_grid; //GP grid
    @FXML private GridPane a_preGrid; //preload GP grid
    @FXML private ImageView gpAutonPNG;
    //page 3 - teleop
    @FXML private LimitedTextField communityPickups; //community GP intaked
    @FXML private LimitedTextField neutralPickups; //neutral zone GP intaked
    @FXML private LimitedTextField singlePickups; //singlesub GP intaked
    @FXML private LimitedTextField doublePickups; //doublesub GP intaked
    private static final ArrayList<Integer> teleopCones = new ArrayList<>(); //cones intaked
    private static final ArrayList<Integer> teleopCubes = new ArrayList<>(); //cubes intaked

    @FXML private GridPane t_grid; //GP grid
    //page 4 - endgame
    @FXML private CheckBox shuttle; //shuttlebot
    @FXML private ToggleGroup teleopBalance; //endgame balance status
    @FXML private CheckBox buddyClimb; //buddy climb
    //page5 - qualitative notes
    @FXML private Rating driver; //driver rating
    @FXML private LimitedTextField scoutName; //scouter name`
    @FXML private TextArea comments; //general comments
    //page6 - data output
    @FXML private Text f_reminderBox; //You scouted, "[insert team #]"
    @FXML private Text f_dataStr; //data string for QR code
    @FXML private ImageView f_imageBox; //QR code image display box
    private static BufferedImage bufferedImage; //QR code image

    //runs at loading of any scene, defaults null values and reloads previously entered data
    public void initialize() {
          if (sceneIndex == 1) {
              //handles team name display
              teamNum.setOnKeyTyped(event -> {
                  try {
                      BufferedReader csvReader = new BufferedReader(new InputStreamReader(
                              this.getClass().getResourceAsStream("teamList.csv")));
                      String line;
                      while ((line = csvReader.readLine()) != null) {
                          String[] pair = line.split(",");
                          if (pair[0].equals(teamNum.getText())) {
                              teamNameText.setText("You are scouting: " + pair[1]);
                              break;
                          }
                          else teamNameText.setText("You are scouting: null");
                      }
                  } catch (Exception e) {
                      throw new RuntimeException(e);
                  }
              });

              //automatically selects alliance color based on selected drive-station
              driveStation.setOnAction(event -> {
                  for (Toggle toggle: alliance.getToggles()) {
                      if (toggle.getUserData().equals("R") && driveStation.getValue().contains("Red")) alliance.selectToggle(toggle);
                      else if (toggle.getUserData().equals("B") && driveStation.getValue().contains("B")) alliance.selectToggle(toggle);
                  }});
          }
        //setting defaults for certain nullable fields
        if (isNextPageClicked) {
            if (sceneIndex == 2) {
                //sets default color for autoPickup picture depending on alliance color chosen on pregame
                autonColor = info.get("alliance");
                Image fieldRed = new Image(getClass().getResource("images/GPstart_red.png").toString());
                Image fieldBlue = new Image(getClass().getResource("images/GPstart_blue.png").toString());
                if (info.get("alliance").equals("R"))
                    gpAutonPNG.setImage(fieldRed);
                else gpAutonPNG.setImage(fieldBlue);

                if (autonColor.equals("R"))
                    gpAutonPNG.setImage(fieldRed);
                else gpAutonPNG.setImage(fieldBlue);
            }
            if (sceneIndex == 3) {
                //sets defaults for community, neutral, single, and double GP intakes to 0
                communityPickups.setText("0");
                neutralPickups.setText("0");
                singlePickups.setText("0");
                doublePickups.setText("0");
            }
        }
        reloadData();
    }

    //implementations of setPage() for going to next and previous pages
    @FXML private void resetAll(ActionEvent event) throws IOException {
        //resets all data storing elements
        data = new StringBuilder();
        info = new LinkedHashMap<>();
        toggleMap = new HashMap<>();
        autoPickups.clear();
        autoCones.clear();
        autoCubes.clear();
        teleopCones.clear();
        teleopCubes.clear();
        // resets UI to scene0
        sceneIndex = 0;
        nextPage(event);
    }
    @FXML private void nextPage(ActionEvent event) throws IOException {
        //checks if all required fields are filled, then changes page
        if (checkRequiredFields()) {
            collectData();
            sceneIndex++;
            isNextPageClicked = true;
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            setPage(stage, sceneIndex);
        }
    }
    @FXML private void prevPage(ActionEvent event) throws IOException {
        //collects data from current page and goes to previous page
        collectData();
        if (sceneIndex > 0) sceneIndex--;
        isNextPageClicked = false;
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        setPage(stage, sceneIndex);
    }

    //changes page to the scene specified by sceneIndex
    static void setPage(Stage stage, int page) throws IOException {
        sceneIndex = page;
        //if next line causes errors, check syntax in all fxml files
        Parent root = FXMLLoader.load(FXMLController.class.getResource("scenes/scene" + (sceneIndex) + ".fxml"));
        Scene scene = new Scene(root);
        stage.setTitle("6672 Scouting App Page " + (sceneIndex));
        stage.setScene(scene);

        //adjusts viewing screen to full screen, will look best on the particular surface pro display used because AnchorPane layout is used
        Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
        stage.setWidth(size.getWidth()); //2736 px
        stage.setHeight(size.getHeight()); //1824 px
        stage.setMaximized(true);
        stage.show();
    }

    //sends data to QR code creator and displays it on screen
    @FXML private void sendInfo() throws Exception {
        data = new StringBuilder();

        //run certain checks to correctly format data; boolean checks, and remove pieces scored in auton from being recorded in teleop
        for (String keyName : info.keySet()) {
            if (info.get(keyName).equals("true")) info.put(keyName, "TRUE");
            else if (info.get(keyName).equals("false")) info.put(keyName, "FALSE");
            else if (info.get(keyName).equals("N/A") || info.get(keyName).equals("N/A or Failed")) info.put(keyName, "NA");
        }

        for (int i = 0; i < teleopCones.size(); i++)
            if (autoCones.contains(teleopCones.get(i)))
                teleopCones.remove(i);
        collectDataArray(teleopCones, "teleopCones");

        for (int i = 0; i < teleopCubes.size(); i++)
            if (autoCubes.contains(teleopCubes.get(i)))
                teleopCubes.remove(i);
        collectDataArray(teleopCubes, "teleopCubes");

        //output string appended to data StringBuilder
        for (String keyName : info.keySet())
            data.append(keyName + "=" + info.get(keyName) + "|");

        data = data.delete(data.lastIndexOf("|"), data.length());

        //creates QR code and displays it on screen, runs outputAll() to save all data
        bufferedImage = QRFuncs.generateQRCode(data.toString(), "qrcode.png");
        File file = new File("qrcode.png");
        Image img = new Image(file.getAbsolutePath());
        f_imageBox.setImage(img);
        f_dataStr.setText(data.toString());
        outputAll();
    }

    //IMPORTANT: ALL collected data elements must be added to the info HashMap in this method, with a SPECIFIC ORDER so that Kraken can correctly parse them
    private void collectData() {
        switch (sceneIndex) {
            case 1 -> {
                collectDataTextField(teamNum, "teamNum");
                collectDataTextField(matchNum, "matchNum");
                collectDataToggleGroup(alliance, "alliance");
                collectDataComboBox(driveStation, "driveStation");
                collectDataToggleGroup(startLocation, "startLocation");
            }
            case 2 -> {
                collectDataToggleGroup(preload, "preload");
                collectDataCheckBox(mobility, "mobility");
                collectDataArray(autoPickups, "autoPickups");
                collectDataArray(autoCones, "autoCones");
                for (Integer i : autoCones) {
                    if (!teleopCones.contains(i)) teleopCones.add(i);
                }
                collectDataArray(autoCubes, "autoCubes");
                for (Integer i : autoCubes) {
                    if (!teleopCubes.contains(i)) teleopCubes.add(i);
                }
                collectDataToggleGroup(autoBalance, "autoBalance");
            }
            case 3 -> {
                collectDataTextField(communityPickups, "communityPickups");
                collectDataTextField(neutralPickups, "neutralPickups");
                collectDataTextField(singlePickups, "singlePickups");
                collectDataTextField(doublePickups, "doublePickups");
                collectDataArray(teleopCones, "teleopCones");
                collectDataArray(teleopCubes, "teleopCubes");
            }
            case 4 -> {
                collectDataCheckBox(shuttle, "shuttle");
                collectDataToggleGroup(teleopBalance, "teleopBalance");
                collectDataCheckBox(buddyClimb, "buddyClimb");
            }
            case 5 -> {
                collectDataRating(driver, "driver");
                collectDataTextField(scoutName, "scoutName");
                collectDataTextArea(comments, "comments");
            }
        }
    }

    //reloads data for a scene, called when loading scene in initialize() method
    private void reloadData() {
        switch (sceneIndex) {
            case 1 -> {
                reloadDataTextField(teamNum, "teamNum");
                reloadDataTextField(matchNum, "matchNum");
                reloadDataToggleGroup(alliance, "alliance");
                reloadDataComboBox(driveStation, "driveStation");
                reloadDataToggleGroup(startLocation, "startLocation");
            }
            case 2 -> {
                reloadDataCheckBox(mobility, "mobility");
                reloadDataToggleGroup(preload, "preload");
                reloadDataToggleGroup(autoBalance, "autoBalance");
                reloadDataGridFieldGP(a_grid, autoCones, autoCubes);
                reloadDataGridFieldPickup(a_preGrid);
            }
            case 3 -> {
                reloadDataTextField(communityPickups, "communityPickups");
                reloadDataTextField(neutralPickups, "neutralPickups");
                reloadDataTextField(singlePickups, "singlePickups");
                reloadDataTextField(doublePickups, "doublePickups");
                reloadDataGridFieldGP(t_grid, teleopCones, teleopCubes);
            }
            case 4 -> {
                reloadDataCheckBox(shuttle, "shuttle");
                reloadDataToggleGroup(teleopBalance, "teleopBalance");
                reloadDataCheckBox(buddyClimb, "buddyClimb");
            }
            case 5 -> {
                reloadDataRating(driver, "driver");
                reloadDataTextField(scoutName, "scoutName");
                reloadDataTextArea(comments, "comments");
            }
            case 6 -> {
                if (info.get("teamNum") != null)
                    f_reminderBox.setText(info.get("scoutName") + " Scouted Team #" + info.get("teamNum") + ".");
            }
        }
    }

    //copies either data text or QR code based on button source that was clicked, mainly emergency/debug methods
    @FXML private void copyToClipboard(ActionEvent event) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (event.getSource().getClass().equals(javafx.scene.control.Button.class)) {
            if (((javafx.scene.control.Button) event.getSource()).getText().contains("Text"))
                clipboard.setContents(new StringSelection(data.toString()), null);
             else if (((javafx.scene.control.Button) event.getSource()).getText().contains("QR Code"))
                new CopyImageToClipBoard().copyImage(bufferedImage);
        }
    }

    //saves output to QR Codes and text files on computer, copies in Desktop/Scouting and Documents/backupScouting
    @FXML private void outputAll() {
        //output paths
        String outputPath = "C:\\Users\\robotics\\Desktop\\Scouting";
        String backupPath = "C:\\Users\\robotics\\Documents\\backupScouting";

        //m~-#~-name~
        String dataName = "m"  + info.get("matchNum") + "-" + "#" + info.get("teamNum") + "-" + "name" + info.get("scoutName");

        try {
            //creates Desktop directories (if they don't exist) to store text and QR code files
            new File(outputPath+"\\qrcodes").mkdirs();
            new File(outputPath+"\\texts").mkdirs();
            new File(outputPath + "\\matchData.csv").createNewFile();
            //creates backup (in Documents) directories
            new File(backupPath+"\\qrcodes").mkdirs();
            new File(backupPath+"\\texts").mkdirs();
            new File(backupPath + "\\matchData.csv").createNewFile();


            //writes text file and qr code in Documents backup directory
            FileWriter backupWriter = new FileWriter(backupPath + "\\texts\\" + dataName + ".txt");
            backupWriter.write(data.toString());
            backupWriter.close();
            ImageIO.write(bufferedImage, "png", new File(backupPath + "\\qrcodes\\" + dataName + ".png")); //backup qrcode

            //writes text file and qr code to central Scouting directory in Desktop
            FileWriter writer = new FileWriter(outputPath + "\\texts\\" + dataName + ".txt");
            writer.write(data.toString());
            writer.close();
            ImageIO.write(bufferedImage, "png", new File(outputPath + "\\qrcodes\\" + dataName + ".png")); //qr code

            //writes CSV to Scouting directory and backup directory
            writeToCSV(backupPath + "\\matchData.csv");
            writeToCSV(outputPath + "\\matchData.csv");
        }
        catch (Exception e) {
            System.out.println("file not found");
        }

    }

    //helper function for outputAll() method, writes data to CSV file
    private void writeToCSV(String outputCSVPath) throws IOException {
        File file = new File(outputCSVPath);
        FileWriter writer = new FileWriter(file, true);
        Scanner reader = new Scanner(file);

        if (!reader.hasNext()) {
            StringBuilder headers = new StringBuilder();
            for (int i = 0; i < info.keySet().size(); i++)
                headers.append((info.keySet().toArray())[i] + ",");
            headers.delete(headers.lastIndexOf(","), headers.length());
            writer.write(headers + "\n");
        }

        StringBuilder values = new StringBuilder();
        for (int j = 0; j < info.keySet().size(); j++)
            values.append(info.get((info.keySet().toArray())[j]) + ",");
        values.delete(values.lastIndexOf(","), values.length());

        boolean found = false;
        while (reader.hasNext())
            if (reader.nextLine().contains(values.toString()))
                found = true;

        if (!found) writer.write(values + "\n");

        writer.close();
        reader.close();
        }

    //puts restrictions on certain LimitedTextFields
    @FXML private void validateInput(KeyEvent keyEvent) {
        LimitedTextField src = (LimitedTextField) keyEvent.getSource();
        if (src.equals(teamNum)) { //team number
            src.setIntegerField();
            src.setMaxLength(4);
        } else if (src.equals(matchNum)) { //match number
            src.setIntegerField();
            src.setMaxLength(3);
        } else if (src.equals(scoutName)) { //scouter name
            src.setRestrict("[A-Za-z ]"); //letters + spaces only
            src.setMaxLength(30);
        }
    }

    //validation for required fields
    private boolean checkRequiredFields() {
        switch (sceneIndex) {
            case 1 -> {
                if (teamNum.getText().isEmpty() || matchNum.getText().isEmpty() || alliance.getSelectedToggle() == null || driveStation.getValue().isBlank() || startLocation.getSelectedToggle() == null) {
                    AlertBox.display("", "Before proceeding, please fill out ALL FIELDS.");
                    return false;
                }
                if (teamNum.getText().equals("0000") || matchNum.getText().equals("000") || matchNum.getText().equals("00") || matchNum.getText().equals("0")) {
                    AlertBox.display("", "Please enter a valid team number and match number.");
                    return false;
                }
            }
            case 2 -> {
                if (preload.getSelectedToggle() == null || autoBalance.getSelectedToggle() == null) {
                    AlertBox.display("", "Before proceeding, please select one of the GP preloads and balance status buttons.");
                    return false;
                }
            }
            case 3 ->  {
                if (Integer.parseInt(communityPickups.getText()) > 99) {
                    AlertBox.display("", "Community Pickups cannot be greater than 99.");
                    return false;
                }
                if (Integer.parseInt(neutralPickups.getText()) > 99) {
                    AlertBox.display("", "Neutral Zone Pickups cannot be greater than 99.");
                    return false;
                }
                if (Integer.parseInt(singlePickups.getText()) > 99) {
                    AlertBox.display("", "Single Pickups cannot be greater than 99.");
                    return false;
                }
                if (Integer.parseInt(doublePickups.getText()) > 99) {
                    AlertBox.display("", "Double Pickups cannot be greater than 99.");
                }

            }
            case 4 -> {
                if (teleopBalance.getSelectedToggle() == null) {
                    AlertBox.display("", "Before proceeding, please select a balance status button.");
                    return false;
                }
            }
            case 5 -> {
                if (scoutName.getText().isEmpty()) {
                    AlertBox.display("", "Before proceeding, please fill out your name and the drivetrain type button. PLEASE INCLUDE COMMENTS!!!");
                    return false;
                }
            }
        }
        return true;
    }

    //grid field/GP pickup field functions
    @FXML private void manipGPStart(ActionEvent event) {
        Button btn = (Button) event.getSource();
        System.out.println(btn.getUserData().toString());
        if (btn.getStyle().contains("-fx-background-color: white;")) {
            btn.setStyle("-fx-background-color: green; -fx-border-color: black;");
            autoPickups.add(Integer.valueOf(btn.getUserData().toString()));
        } else if (btn.getStyle().contains("-fx-background-color: green;")) {
            btn.setStyle("-fx-background-color: white; -fx-border-color: black;");
            autoPickups.remove(Integer.valueOf(btn.getUserData().toString()));
        }
    }
    @FXML private void manipCones(ActionEvent event) {
        Button btn = (Button) event.getSource();
        int btnVal = Integer.parseInt(btn.getUserData().toString());
        //if button is white, make it yellow; add to autoCones/teleopCones
        if (btn.getStyle().contains("-fx-background-color: white;")) {
            btn.setStyle("-fx-background-color: yellow; -fx-border-color: black;");
            if (sceneIndex == 2) autoCones.add(btnVal);
            else if (sceneIndex == 3) teleopCones.add(btnVal);
        }
        //if button is yellow, make it white; remove from autoCones/teleopCones
        else if (btn.getStyle().contains("-fx-background-color: yellow;")) {
            btn.setStyle("-fx-background-color: white; -fx-border-color: black;");
            if (sceneIndex == 2) autoCones.remove((Integer) btnVal);
            else if (sceneIndex == 3) teleopCones.remove((Integer) btnVal);
        }
    }
    @FXML private void manipCubes(ActionEvent event) {
        Button btn = (Button) event.getSource();
        int btnVal = Integer.parseInt(btn.getUserData().toString());
        //if button is white, make it purple; add to autoCubes/teleopCubes
        if (btn.getStyle().contains("-fx-background-color: white;")) {
            btn.setStyle("-fx-background-color: purple; -fx-border-color: black;");
            if (sceneIndex == 2) autoCubes.add(btnVal);
            else if (sceneIndex == 3) teleopCubes.add(btnVal);
        }
        //if button is purple, make it white; remove from autoCubes/teleopCubes
        else if (btn.getStyle().contains("-fx-background-color: purple;")) {
            btn.setStyle("-fx-background-color: white; -fx-border-color: black;");
            if (sceneIndex == 2) autoCubes.remove((Integer) btnVal);
            else if (sceneIndex == 3) teleopCubes.remove((Integer) btnVal);
        }
    }
    @FXML private void manipVar(ActionEvent event) {
        Button btn = (Button) event.getSource();
        int btnVal = Integer.parseInt(btn.getUserData().toString());
        //if button is white, make it yellow; add to autoCones/teleopCones
        if (btn.getStyle().contains("-fx-background-color: white;")) {
            btn.setStyle("-fx-background-color: yellow; -fx-border-color: black;");
            if (sceneIndex == 2) autoCones.add(btnVal);
            else if (sceneIndex == 3) teleopCones.add(btnVal);
        }
        //if button is yellow, make it purple; remove from autoCones/teleopCones, add to autoCubes/teleopCubes
        else if (btn.getStyle().contains("-fx-background-color: yellow;")) {
            btn.setStyle("-fx-background-color: purple; -fx-border-color: black;");
            if (sceneIndex == 2) {
                autoCones.remove((Integer) btnVal);
                autoCubes.add(btnVal);
            } else if (sceneIndex == 3) {
                teleopCones.remove((Integer) btnVal);
                teleopCubes.add(btnVal);
            }
        }
        //if button is purple, make it white; remove from autoCubes/teleopCubes
        else if (btn.getStyle().contains("-fx-background-color: purple;")) {
            btn.setStyle("-fx-background-color: white; -fx-border-color: black;");
            if (sceneIndex == 2) autoCubes.remove((Integer) btnVal);
            else if (sceneIndex == 3) teleopCubes.remove((Integer) btnVal);
        }
    }

    //template incrementer functions, used by +/- buttons
    private void increment(LimitedTextField txtfield) {
        txtfield.setText(String.valueOf(Integer.parseInt(txtfield.getText()) + 1));
    }
    private void decrement(LimitedTextField txtfield) {
        if (!txtfield.getText().equals("0")) txtfield.setText(String.valueOf(Integer.parseInt(txtfield.getText()) - 1));
    }

    //general methods for +/- buttons affecting corresponding LimitedTextFields
    @FXML private void incrementT_cmty(ActionEvent ignoredEvent) {
        increment(communityPickups);
    }
    @FXML private void decrementT_cmty(ActionEvent ignoredEvent) {
        decrement(communityPickups);
    }
    @FXML private void incrementT_neutzone(ActionEvent ignoredEvent) {
        increment(neutralPickups);
    }
    @FXML private void decrementT_neutzone(ActionEvent ignoredEvent) {
        decrement(neutralPickups);
    }
    @FXML private void incrementT_singlesub(ActionEvent ignoredEvent) {
        increment(singlePickups);
    }
    @FXML private void decrementT_singlesub(ActionEvent ignoredEvent) {
        decrement(singlePickups);
    }
    @FXML private void incrementT_doublesub(ActionEvent ignoredEvent) {
        increment(doublePickups);
    }
    @FXML private void decrementT_doublesub(ActionEvent ignoredEvent) {
        decrement(doublePickups);
    }

    //used in collectData() for specific types of data
    private void collectDataCheckBox(CheckBox checkBox, String key) {
        info.put(key, String.valueOf(checkBox.isSelected()));
    }
    private void collectDataTextField(LimitedTextField textField, String key) {info.put(key, textField.getText());}
    private void collectDataArray(ArrayList<Integer> array, String key) {
        info.put(key, array.toString());
    }
    private void collectDataRating(Rating rating, String key) {
        info.put(key, String.valueOf((int) rating.getRating()));
    }
    private void collectDataTextArea(TextArea textArea, String key) {
        info.put(key, textArea.getText());
    }
    private void collectDataToggleGroup(ToggleGroup toggleGroup, String key) {
        if (toggleGroup.getSelectedToggle() == null) return;
        Toggle selectedToggle = toggleGroup.getSelectedToggle();
        int index = toggleGroup.getToggles().indexOf(selectedToggle);
        String value = selectedToggle.getUserData().toString();
        info.put(key, value);
        toggleMap.put(key, index);
    }
    private void collectDataComboBox(ComboBox<String> comboBox, String key) {
        info.put(key, comboBox.getValue());
    }

    //used in reloadData() for specific types of data
    private void reloadDataCheckBox(CheckBox checkBox, String key) {
        checkBox.setSelected(Boolean.parseBoolean(info.get(key)));
    }
    private void reloadDataTextField(LimitedTextField textField, String key) {
        if (info.get(key) != null) textField.setText(info.get(key));
    }
    private void reloadDataGridFieldGP(GridPane grid, ArrayList<Integer> coneArray, ArrayList<Integer> cubeArray) {
        int gridLength = grid.getChildren().size();
        for (int i = 0; i < gridLength; i++) {
            Button btn = (Button) grid.getChildren().get(i);
            if (coneArray.contains(Integer.valueOf(btn.getUserData().toString())))
                btn.setStyle("-fx-background-color: yellow; -fx-border-color: black;");
            else if (cubeArray.contains(Integer.valueOf(btn.getUserData().toString())))
                btn.setStyle("-fx-background-color: purple; -fx-border-color: black;");
        }
    }
    private void reloadDataGridFieldPickup(GridPane grid) {
        int gridLength = grid.getChildren().size();
        for (int i = 0; i < gridLength; i++) {
            Button btn = (Button) grid.getChildren().get(i);
            if (FXMLController.autoPickups.contains(Integer.valueOf(btn.getUserData().toString())))
                btn.setStyle("-fx-background-color: green; -fx-border-color: black;");
        }
    }
    private void reloadDataRating(Rating rating, String key) {
        if (info.get(key) != null) rating.setRating(Double.parseDouble(info.get(key)));
    }
    private void reloadDataTextArea(TextArea textArea, String key) {
        textArea.setText(info.get(key));
    }
    private void reloadDataToggleGroup(ToggleGroup toggleGroup, String key) {
        if (toggleMap.get(key) != null) toggleGroup.selectToggle(toggleGroup.getToggles().get(toggleMap.get(key)));
    }
    private void reloadDataComboBox(ComboBox<String> comboBox, String key) {
        comboBox.setValue(info.get(key));
    }

    //used to change auton pickup image to red/blue
    @FXML private void changeGPAutonPNG(ActionEvent ignoredEvent) {
        if (autonColor.equals("R")) {
            autonColor = "B";
            gpAutonPNG.setImage(new Image(getClass().getResource("images/GPstart_blue.png").toString()));
        } else if (autonColor.equals("B")) {
            autonColor = "R";
            gpAutonPNG.setImage(new Image(getClass().getResource("images/GPstart_red.png").toString()));
        }
    }

    //displays confirmation popup before resetting app
    @FXML private void confirmReset(ActionEvent event) throws IOException {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reset");
        alert.setHeaderText("Are you sure you want to reset the app?");
        alert.setContentText("This will clear all data and return to the start page. This cannot be undone.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) resetAll(event);
    }

    //flips pregame start location image
    @FXML private void flipImage(ActionEvent ignoredEvent) {
        if (PNGflipped) {
            PNGflipped = false;
            startLocationPNG.setImage(new Image(getClass().getResource("images/start_locs.png").toString()));
        } else {
            PNGflipped = true;
            startLocationPNG.setImage(new Image(getClass().getResource("images/start_locs_reversed.png").toString()));
        }
    }
}