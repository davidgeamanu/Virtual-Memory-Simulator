module ro.tuc.virtualmemorysimulator {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens ro.tuc.virtualmemorysimulator to javafx.fxml;
    exports ro.tuc.virtualmemorysimulator.PresentationLayer;
    opens ro.tuc.virtualmemorysimulator.PresentationLayer to javafx.fxml;
}