module unoeste.fipp.photoshopfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires javafx.swing;
    requires java.rmi;
    requires ij;


    opens unoeste.fipp.photoshopfx to javafx.fxml;
    exports unoeste.fipp.photoshopfx;
}