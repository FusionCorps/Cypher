//TODO (long-term restructuring) EFFICIENCY/READABILITY/EASIER TO MODIFY (for new games): how to encapsulate/declare data fields in more efficient way (e.g. maybe hashmap for each field, like [Object:fx_id]?)

package com.scout;

import com.scout.ui.AlertBox;
import com.scout.ui.LimitedTextField;
import com.scout.ui.PlusMinusBox;
import com.scout.util.CopyImageToClipBoard;
import com.scout.util.QRFuncs;
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
        putIfAbsent("driveStation", null);
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
    private static String prevMatchNum = "1"; //stores current matchNum, increments on reset

    //======================FXML DATA FIELDS======================
    //data for each page, variables are named the same as corresponding fx:ids in fxml files for consistency

    //page 1 - pregame
    @FXML private LimitedTextField teamNum; //team number
    @FXML private LimitedTextField matchNum; //match number
    @FXML private ToggleGroup driveStation;
    @FXML private ToggleGroup startLocation; //starting location
    @FXML private ToggleGroup preload; // GP type preload

    @FXML private ImageView startLocationPNG; //starting location image
    @FXML private Text teamNameText;
    //page 2 - auton
    @FXML private CheckBox mobility; //mobility
    private static final ArrayList<Integer> autoPickups = new ArrayList<>(); //GP intaked during auton
    private static final ArrayList<Integer> autoFailedPickups = new ArrayList<>(); //GP failed to intake during auton

    private static final ArrayList<Integer> autoCones = new ArrayList<>(); //cones placed
    private static final ArrayList<Integer> autoCubes = new ArrayList<>(); //cubes placed
    @FXML private ToggleGroup autoBalance; //auton balance status

    @FXML private GridPane a_grid; //GP grid
    @FXML private GridPane a_preGrid; //preload GP grid
    @FXML private ImageView gpAutonPNG;
    //page 3 - teleop
    @FXML private PlusMinusBox communityPickups; //community GP intaked
    @FXML private PlusMinusBox neutralPickups; //neutral zone GP intaked
    @FXML private PlusMinusBox singlePickups; //singlesub GP intaked
    @FXML private PlusMinusBox doublePickups; //doublesub GP intaked
    @FXML private PlusMinusBox superChargeScored; //supercharged pieces scored by a team
    @FXML private PlusMinusBox ferryPieces; //number of ferried pieces by a team
    private static final ArrayList<Integer> teleopCones = new ArrayList<>(); //cones intaked
    private static final ArrayList<Integer> teleopCubes = new ArrayList<>(); //cubes intaked

    @FXML private GridPane t_grid; //GP grid
    //page 4 - endgame
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

    //=============================METHODS FOR CONTROLLING APP LOGIC=============================
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

              //displays starting location image according to whether it was previously flipped
              if (PNGflipped)  startLocationPNG.setImage(new Image(getClass().getResource(
                      "images/start_locs_flipped.png").toString()));
              else startLocationPNG.setImage(new Image(getClass().getResource(
                      "images/start_locs.png").toString()));
          }
        //setting defaults for certain nullable fields
        if (isNextPageClicked) {
            if (sceneIndex == 1) {
                if (matchNum.getText().isEmpty()) matchNum.setText(prevMatchNum);
            }
            if (sceneIndex == 2) {
                //sets default color for autoPickup picture depending on alliance color chosen on pregame
                autonColor = info.get("driveStation").substring(0,1);
                Image fieldRed = new Image(getClass().getResource("images/GPstart_red.png").toString());
                Image fieldBlue = new Image(getClass().getResource("images/GPstart_blue.png").toString());
                if (autonColor.equals("R"))
                    gpAutonPNG.setImage(fieldRed);
                else gpAutonPNG.setImage(fieldBlue);
            }
            if (sceneIndex == 3) {
                //sets defaults for community, neutral, single, and double GP intakes to 0
                communityPickups.getValueElement().setText("0");
                neutralPickups.getValueElement().setText("0");
                singlePickups.getValueElement().setText("0");
                doublePickups.getValueElement().setText("0");
                superChargeScored.getValueElement().setText("0");
            }
        }
        reloadData();
    }

    /**
     * <p> {@code resetAll} - resets all forms of data storage and goes to first page
     * <p> {@code nextPage} - goes to next page
     * <p> {@code prevPage} - goes to previous page
     * <p> {@code setPage} - general function for setting page number
     */
    //implementations of setPage() for going to next and previous pages
    @FXML private void resetAll(ActionEvent event) throws IOException {
        //sets new default match number
        prevMatchNum = String.valueOf(Integer.parseInt(prevMatchNum) + Integer.parseInt(info.get("matchNum")));
        //resets all data storing elements
        data = new StringBuilder();
        info = new LinkedHashMap<>();
        toggleMap = new HashMap<>();
        autoPickups.clear();
        autoFailedPickups.clear();
        autoCones.clear();
        autoCubes.clear();
        teleopCones.clear();
        teleopCubes.clear();
        // resets UI to scene1
        sceneIndex = 0;
        nextPage(event);
    }
    @FXML private void nextPage(ActionEvent event) throws IOException {
            collectData();
            sceneIndex++;
            isNextPageClicked = true;
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            setPage(stage, sceneIndex);
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
        stage.setTitle("6672 Cypher Page " + (sceneIndex));
        stage.setScene(scene);

        //adjusts viewing screen to full screen, will look best on the particular surface pro display used because AnchorPane layout is used
        Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
        stage.setWidth(size.getWidth()); //2736 px
        stage.setHeight(size.getHeight()); //1824 px
        stage.setMaximized(true);
        stage.show();
    }

    /**
     * <p> {@code sendInfo} - formats and creates QR code for data, calls outputAll
     * <p> {@code collectData} - saves data entered on a page
     * <p> {@code reloadData} - reloads data previously entered when reentering a page
     * <p> {@code validateInput} - checks for certain data fields (types of characters allowed)
     * <p> {@code checkRequiredFields} - displays alert boxes for unfulfilled required fields
     */

    //sends data to QR code creator and displays it on screen
    @FXML private void sendInfo() throws Exception {
        if(checkRequiredFields()) {
            data = new StringBuilder();

            //run certain checks to correctly format data; boolean checks, and remove pieces scored in auton from being recorded in teleop
            for (String keyName : info.keySet()) {
                if (info.get(keyName).equals("true")) info.put(keyName, "TRUE");
                else if (info.get(keyName).equals("false")) info.put(keyName, "FALSE");
                else if (info.get(keyName).equals("N/A") || info.get(keyName).equals("N/A or Failed"))
                    info.put(keyName, "NA");
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
            for (String keyName : info.keySet()) {
                //get embedded alliance value from driveStation
                if (keyName.equals("driveStation"))
                    data.append("alliance" + "=" + info.get("driveStation").substring(0, 1) + "|");
                data.append(keyName + "=" + info.get(keyName) + "|");
            }

            data = data.delete(data.lastIndexOf("|"), data.length());

            //creates QR code and displays it on screen, runs outputAll() to save all data
            bufferedImage = QRFuncs.generateQRCode(data.toString(), "qrcode.png");
            File file = new File("qrcode.png");
            Image img = new Image(file.getAbsolutePath());
            f_imageBox.setImage(img);
            f_dataStr.setText(data.toString());
            outputAll();
        }
    }

    //IMPORTANT: ALL collected data elements must be added to the info HashMap in this method, with a SPECIFIC ORDER so that Kraken can correctly parse them
    private void collectData() {
        switch (sceneIndex) {
            case 1 -> {
                collectDataTextField(teamNum, "teamNum");
                collectDataTextField(matchNum, "matchNum");
                collectDataToggleGroup(driveStation, "driveStation");
                collectDataToggleGroup(startLocation, "startLocation");
                collectDataToggleGroup(preload, "preload");
            }
            case 2 -> {
                collectDataCheckBox(mobility, "mobility");
                collectDataArray(autoPickups, "autoPickups");
                collectDataArray(autoFailedPickups, "autoFailedPickups");
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
                collectDataTextField(communityPickups.getValueElement(), "communityPickups");
                collectDataTextField(neutralPickups.getValueElement(), "neutralPickups");
                collectDataTextField(singlePickups.getValueElement(), "singlePickups");
                collectDataTextField(doublePickups.getValueElement(), "doublePickups");
                collectDataTextField(superChargeScored.getValueElement(), "superChargeScored");
                collectDataTextField(ferryPieces.getValueElement(), "ferryPieces");
                collectDataArray(teleopCones, "teleopCones");
                collectDataArray(teleopCubes, "teleopCubes");
            }
            case 4 -> {
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
                reloadDataToggleGroup(driveStation, "driveStation");
                reloadDataToggleGroup(startLocation, "startLocation");
                reloadDataToggleGroup(preload, "preload");
            }
            case 2 -> {
                reloadDataCheckBox(mobility, "mobility");
                reloadDataToggleGroup(autoBalance, "autoBalance");
                reloadDataGridFieldGP(a_grid, autoCones, autoCubes);
                reloadDataGridFieldPickup(a_preGrid);
            }
            case 3 -> {
                reloadDataTextField(communityPickups.getValueElement(), "communityPickups");
                reloadDataTextField(neutralPickups.getValueElement(), "neutralPickups");
                reloadDataTextField(singlePickups.getValueElement(), "singlePickups");
                reloadDataTextField(doublePickups.getValueElement(), "doublePickups");
                reloadDataTextField(superChargeScored.getValueElement(), "superChargeScored");
                reloadDataTextField(ferryPieces.getValueElement(), "ferryPieces");
                reloadDataGridFieldGP(t_grid, teleopCones, teleopCubes);
            }
            case 4 -> {
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

    //puts restrictions on certain LimitedTextFields
    @FXML private void validateInput(KeyEvent keyEvent) {
        //create src variable to make code more readable which could be either a LimitedTextField or a TextArea, it is the source of the event
        Object src = keyEvent.getSource();

        if (src instanceof LimitedTextField ltf) {
            //if src is teamNum, restrict to Integers, and set max length to 4
            if (ltf.equals(teamNum)) {
                ltf.setIntegerField();
                ltf.setMaxLength(4);
            }
            //if src is matchNum, restrict to Integers, and set max length to 3
            if (ltf.equals(matchNum)) {
                ltf.setIntegerField();
                ltf.setMaxLength(3);
            }
            //if src is scoutName, restrict to letters and spaces, and set max length to 30
            if (ltf.equals(scoutName)) {
                ltf.setRestrict("[a-zA-Z ]");
                ltf.setMaxLength(30);
            }
        }
        else if (src instanceof TextArea ta) {
            if (ta.equals(comments)) {
                //set max length to 2471
                ta.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue.length() > 2471) ta.setText(oldValue);
                });
                //restrict to all characters but |
                ta.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue.contains("|")) ta.setText(oldValue);
                });
            }
        }
    }

    //validation for required fields
    private boolean checkRequiredFields() {
        String warnings = "";
        if (info.get("teamNum").isBlank() || info.get("teamNum").matches("0+"))
            warnings += "Fix the team number (cannot contain only 0s or be blank). ";
        if (info.get("matchNum").isBlank() || info.get("matchNum").matches("0+"))
            warnings += "Fix the match number (cannot contain only 0s or be blank). ";
        if (Integer.parseInt(info.get("communityPickups")) > 99)
            warnings += "Community Pickups cannot be greater than 99. ";
        if (Integer.parseInt(info.get("neutralPickups")) > 99)
            warnings += "Neutral Zone Pickups cannot be greater than 99. ";
        if (Integer.parseInt(info.get("singlePickups")) > 99)
            warnings += "Single Pickups cannot be greater than 99. ";
        if (Integer.parseInt(info.get("doublePickups")) > 99)
            warnings += "Double Pickups cannot be greater than 99. ";
        if (Integer.parseInt(info.get("superChargeScored")) > 99)
            warnings += "Super Charge Scored cannot be greater than 99. ";
        if (Integer.parseInt(info.get("ferryPieces")) > 99)
            warnings += "Ferried Pieces cannot be greater than 99. ";
        if (info.get("scoutName").isBlank())
            warnings += "Fix the scouter name (cannot be blank). ";

        System.out.println(warnings);
        if (warnings.isBlank()) return true;
        else {
            AlertBox.display("Incorrect inputs", warnings);
            return false;
        }
    }

    /**
     * <p> {@code outputAll) - central function for outputting and saving data
     * <p> {@code writeToCSV} - writes data to CSV on computer
     * <p> {@code copyToClipBoard} - copies data to clipboard, mainly for debugging
     */

    //saves output to QR Codes and text files on computer, copies in Desktop/Scouting and Documents/backupScouting of active user
    private void outputAll() {
        //output paths
        String outputPath = "C:\\Users\\" + System.getProperty("user.name") + "\\Desktop\\Scouting";
        String backupPath = "C:\\Users\\" + System.getProperty("user.name") + "\\Documents\\backupScouting";

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


            //writes text file/qr code/CSV in Documents backup directory
            FileWriter backupWriter = new FileWriter(backupPath + "\\texts\\" + dataName + ".txt");
            backupWriter.write(data.toString());
            backupWriter.close();
            ImageIO.write(bufferedImage, "png", new File(backupPath + "\\qrcodes\\" + dataName + ".png")); //backup qrcode
            writeToCSV(backupPath + "\\matchData.csv");

            //writes text file/qr code/CSV to central Scouting directory in Desktop
            FileWriter writer = new FileWriter(outputPath + "\\texts\\" + dataName + ".txt");
            writer.write(data.toString());
            writer.close();
            ImageIO.write(bufferedImage, "png", new File(outputPath + "\\qrcodes\\" + dataName + ".png")); //qr code
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

    /**
     * manipGPStart - responds to user input on auton Pickups grid
     * manipCones - responds to user input on hardcoded cone nodes
     * manipCubes - affects hardcoded cube nodes
     * manipVar - affects hybrid nodes
     */
    //grid field/GP pickup field functions
    @FXML private void manipGPStart(ActionEvent event) {
        Button btn = (Button) event.getSource();
        if (btn.getStyle().contains("-fx-background-color: white;")) {
            btn.setStyle("-fx-background-color: green; -fx-border-color: black;");
            autoPickups.add(Integer.valueOf(btn.getUserData().toString()));
        } else if (btn.getStyle().contains("-fx-background-color: green;")) {
            btn.setStyle("-fx-background-color: red; -fx-border-color: black;");
            autoPickups.remove(Integer.valueOf(btn.getUserData().toString()));
            autoFailedPickups.add(Integer.valueOf(btn.getUserData().toString()));
        }
        else if (btn.getStyle().contains("-fx-background-color: red;")) {
            btn.setStyle("-fx-background-color: white; -fx-border-color: black;");
            autoFailedPickups.remove(Integer.valueOf(btn.getUserData().toString()));
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
            if (FXMLController.autoFailedPickups.contains(Integer.valueOf(btn.getUserData().toString())))
                btn.setStyle("-fx-background-color: red; -fx-border-color: black;");
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
            startLocationPNG.setImage(new Image(getClass().getResource("images/start_locs.png").toString()));
            PNGflipped = false;
        } else {
            PNGflipped = true;
            startLocationPNG.setImage(new Image(getClass().getResource("images/start_locs_flipped.png").toString()));
        }
    }
}