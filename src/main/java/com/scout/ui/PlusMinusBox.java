package com.scout.ui;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.w3c.dom.Text;

public class PlusMinusBox extends HBox {
    LimitedTextField value = new LimitedTextField();
    Button minus = new Button("-");
    Button plus = new Button("+");


    public PlusMinusBox() {
        super();
        value.setText("0");
        plus.setPrefSize(50, 45);
        minus.setPrefSize(50, 45);
        value.setPrefSize(60,45);

        plus.setStyle("-fx-font-size: 24px; " +
                "-fx-font-weight: bold;" +
                " fx-text-fill: white;" +
                "-fx-background-radius: 5px;" +
                "-fx-padding: 5px;" +
                "-fx-alignment: center;");
        minus.setStyle("-fx-font-size: 24px; " +
                "-fx-font-weight: bold;" +
                " fx-text-fill: white;" +
                "-fx-background-radius: 5px;" +
                "-fx-padding: 5px;" +
                "-fx-alignment: center;");
        value.setStyle("-fx-font-size: 24px;\n" +
                "    -fx-font-weight: bold;\n" +
                "    -fx-text-fill: white;\n" +
                "    -fx-background-color: black;\n" +
                "    -fx-background-radius: 5px;\n" +
                "    -fx-padding: 5px;\n" +
                "    -fx-alignment: center;");

        this.getChildren().addAll(minus, value, plus);
        plus.setOnAction(e -> value.setText(String.valueOf(Integer.parseInt(value.getText()) + 1)));
        minus.setOnAction(e -> value.setText(String.valueOf(Integer.parseInt(value.getText()) - 1)));
    }

    public LimitedTextField getValueElement() {
        return value;
    }
}
