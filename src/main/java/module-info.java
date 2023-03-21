module com.scout {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.datatransfer;
    requires java.desktop;
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires org.controlsfx.controls;
    requires java.net.http;

    opens com.scout to javafx.fxml;
    exports com.scout;
    exports com.scout.util;
    exports com.scout.ui;

}