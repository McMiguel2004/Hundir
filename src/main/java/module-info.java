module com.example.conecta4buena {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.conecta4buena to javafx.fxml;
    exports com.example.conecta4buena;
}