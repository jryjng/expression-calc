module application.calcfx {
    requires javafx.controls;
    requires javafx.fxml;


    opens application.calcfx to javafx.fxml;
    exports application;
}