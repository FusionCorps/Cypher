package com.scout.ui;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

public class PlusMinusBox extends HBox {
    LimitedTextField value = new LimitedTextField();
    Button minus = new Button("-");
    Button plus = new Button("+");


    public PlusMinusBox() {
        super();
        value.setText("0");
        minus.setPrefSize(50, 50);
        plus.setPrefSize(50, 50);
        value.setPrefSize(50,50);

        minus.setStyle("""
                -fx-font-size: 24px;
                -fx-font-weight: bold;
                -fx-text-fill: black;
                -fx-background-radius: 5px;
                -fx-padding: 5px;
                -fx-alignment: center;""");

        plus.setStyle("""
                -fx-font-size: 24px;
                -fx-font-weight: bold;
                -fx-text-fill: black;
                -fx-background-radius: 5px;
                -fx-padding: 5px;
                -fx-alignment: center;""");

        value.setStyle("""
                -fx-font-size: 24px;
                    -fx-font-weight: bold;
                    -fx-text-fill: white;
                    -fx-background-color: black;
                    -fx-background-radius: 5px;
                    -fx-padding: 5px;
                    -fx-alignment: center;""");

        this.getChildren().addAll(minus, value, plus);
        plus.setOnAction(e -> value.setText(String.valueOf(Integer.parseInt(value.getText()) + 1)));
        minus.setOnAction(e -> {
            if (!value.getText().equals("0")) value.setText(String.valueOf(Integer.parseInt(value.getText()) - 1));
        });
    }

    public LimitedTextField getValueElement() {
        return value;
    }
}
