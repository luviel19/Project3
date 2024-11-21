module org.example.p2phelper {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.desktop;
    requires tess4j;
    requires com.github.kwhat.jnativehook;
    requires org.jfree.jfreechart;
    requires jcommon;
    requires org.bytedeco.opencv;
    requires com.sun.jna;
requires com.sun.jna.platform;
requires org.bytedeco.opencv.platform;
requires org.bytedeco.javacv;
requires jocl;

    opens org.example.p2phelper to javafx.fxml;
    exports org.example.p2phelper;
}