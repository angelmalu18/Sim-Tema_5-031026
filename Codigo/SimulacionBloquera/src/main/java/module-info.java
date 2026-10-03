module com.angelsystems.simulacionbloquera {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;

    opens com.angelsystems.simulacionbloquera to javafx.fxml;
    opens com.angelsystems.simulacionbloquera.controller to javafx.fxml;

    exports com.angelsystems.simulacionbloquera;
    exports com.angelsystems.simulacionbloquera.controller;
    exports com.angelsystems.simulacionbloquera.model;
}
