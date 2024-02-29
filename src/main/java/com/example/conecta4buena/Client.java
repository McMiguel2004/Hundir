package com.example.conecta4buena;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class Client extends Application {
    private static final int GRID_SIZE = 5;
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT_TCP = 12345; // Puerto TCP
    private static final int SERVER_PORT_UDP = 12346; // Puerto UDP

    private Socket socket; // Socket TCP
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    private Button[][] buttons;
    private Label messageLabel;
    private boolean gameActive = true;
    private int barcosRestantes = 3;

    private UDPClient udpClient;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Hundir la Flota");

        GridPane grid = new GridPane();
        buttons = new Button[GRID_SIZE][GRID_SIZE];
        messageLabel = new Label("Bienvenido. Realiza tu primer disparo.");

        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                buttons[i][j] = createGridButton(i, j);
                grid.add(buttons[i][j], j, i);
            }
        }

        Button resetButton = new Button("Reiniciar Juego");
        resetButton.setOnAction(e -> handleResetButton());

        VBox root = new VBox(grid, messageLabel, resetButton);
        Scene scene = new Scene(root, 250, 400);
        primaryStage.setScene(scene);

        try {
            // Establece la conexión TCP con el servidor
            socket = new Socket(SERVER_IP, SERVER_PORT_TCP);
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());

            // Inicia un hilo para recibir mensajes del servidor a través de TCP
            new Thread(this::receiveMessages).start();

            // Crea una instancia de UDPClient
            udpClient = new UDPClient();
            // Inicia la comunicación UDP
            udpClient.startUDPClient();

        } catch (IOException e) {
            e.printStackTrace();
        }

        primaryStage.show();
    }

    private Button createGridButton(int row, int col) {
        Button button = new Button(" ");
        button.setMinSize(50, 50);
        button.setOnAction(e -> handleButtonClick(row, col));
        return button;
    }

    private void handleButtonClick(int row, int col) {
        if (gameActive) {
            try {
                outputStream.writeInt(row);
                outputStream.writeInt(col);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void receiveMessages() {
        try {
            while (gameActive) {
                String message = inputStream.readUTF();
                processMessage(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processMessage(String message) {
        String[] parts = message.split(":");
        if (parts.length < 2) {
            System.err.println("Mensaje recibido con un formato incorrecto: " + message);
            return;
        }

        String messageType = parts[0];
        String messageContent = parts[1];

        Platform.runLater(() -> {
            switch (messageType) {
                case "INICIO":
                    // Puedes realizar alguna acción inicial si es necesario
                    break;
                case "IMPACTO":
                    handleImpact(messageContent);
                    break;
                case "AGUA":
                    handleWater(messageContent);
                    break;
                case "INVALIDO":
                    handleInvalid(messageContent);
                    break;
                case "HUNDIDA":
                    handleWin(messageContent);
                    break;
                default:
                    System.out.println("Mensaje informativo: " + message);
                    break;
            }
        });
    }

    private void handleImpact(String coordinates) {
        String[] parts = coordinates.split(",");
        int row = Integer.parseInt(parts[0]);
        int col = Integer.parseInt(parts[1]);

        buttons[row][col].setText("X");
        messageLabel.setText("¡Impacto! Barco alcanzado en (" + row + ", " + col + ")");

        barcosRestantes--;

        if (barcosRestantes == 0) {
            Platform.runLater(() -> {
                gameActive = false;
                messageLabel.setText("Toda la flota ha sido hundida. ¡Enhorabuena!");
                disableButtons();
            });
        }
    }

    private void handleWater(String coordinates) {
        String[] parts = coordinates.split(",");
        int row = Integer.parseInt(parts[0]);
        int col = Integer.parseInt(parts[1]);

        buttons[row][col].setText("O");
        messageLabel.setText("Agua. Disparo en (" + row + ", " + col + ")");
    }

    private void handleInvalid(String coordinates) {
        messageLabel.setText("Disparo inválido en " + coordinates);
    }

    private void handleWin(String message) {
        Platform.runLater(() -> {
            gameActive = false;
            messageLabel.setText(message);
            disableButtons();
        });
    }

    private void handleResetButton() {
        Platform.runLater(() -> {
            gameActive = true;
            barcosRestantes = 3;
            messageLabel.setText("Bienvenido. Realiza tu primer disparo.");
            String barcosUbicados = resetButtons();
            colocarBarcosAleatorios(barcosUbicados);
        });
    }

    private void disableButtons() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                buttons[i][j].setOnAction(null);
            }
        }
    }

    private String resetButtons() {
        StringBuilder barcosUbicados = new StringBuilder();

        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                final int finalI = i;
                final int finalJ = j;

                buttons[i][j].setText(" ");
                buttons[i][j].setOnAction(e -> handleButtonClick(finalI, finalJ));

                if (buttons[i][j].getText().equals("B")) {
                    barcosUbicados.append(i).append(",").append(j).append(":");
                }
            }
        }

        return barcosUbicados.toString();
    }

    private void colocarBarcosAleatorios(String barcosUbicados) {
        String[] ubicaciones = barcosUbicados.split(":");
        for (String ubicacion : ubicaciones) {
            String[] parts = ubicacion.split(",");
            int row = Integer.parseInt(parts[0]);
            int col = Integer.parseInt(parts[1]);
            buttons[row][col].setText("B");
        }
    }



    // Clase interna para manejar la comunicación UDP
    public static class UDPClient {
        private static final int PUERTO_UDP = 12346; // Puerto UDP

        public void startUDPClient() {
            try {
                DatagramSocket socketUDP = new DatagramSocket();
                InetAddress direccionServidor = InetAddress.getByName("localhost");

                String mensajeUDP = "Hello from UDP";
                byte[] datosEnviar = mensajeUDP.getBytes();

                DatagramPacket paqueteEnviar = new DatagramPacket(datosEnviar, datosEnviar.length, direccionServidor, PUERTO_UDP);
                socketUDP.send(paqueteEnviar);

                socketUDP.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
