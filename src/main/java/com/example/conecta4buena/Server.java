package com.example.conecta4buena;
import java.io.*;
import java.net.*;
import java.util.Random;

public class Server {
    private static final int GRID_SIZE = 5;
    private static final int NUM_SHIPS = 3;
    private static final int SERVER_PORT_TCP = 12345; // Puerto TCP
    private static final int SERVER_PORT_UDP = 12346; // Puerto UDP

    private static int[][] tablero = new int[GRID_SIZE][GRID_SIZE];
    private static boolean gameActive = true;

    public static void main(String[] args) {
        try {
            ServerSocket serverSocketTCP = new ServerSocket(SERVER_PORT_TCP);
            DatagramSocket serverSocketUDP = new DatagramSocket(SERVER_PORT_UDP);

            System.out.println("Esperando a que el jugador se conecte al servidor TCP...");

            while (true) {
                // Espera la conexión TCP de un jugador
                Socket clientSocketTCP = serverSocketTCP.accept();
                System.out.println("¡Jugador conectado al servidor TCP!");

                // Procesa la conexión TCP del cliente en un hilo separado
                new Thread(() -> handleTCPConnection(clientSocketTCP)).start();

                // Procesa la conexión UDP del cliente en un hilo separado
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocketUDP.receive(receivePacket);

                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());

                // Procesa el mensaje UDP
                System.out.println("Mensaje UDP recibido: " + message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleTCPConnection(Socket clientSocketTCP) {
        try {
            // Configura streams de entrada y salida para la conexión TCP
            DataInputStream inputStream = new DataInputStream(clientSocketTCP.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(clientSocketTCP.getOutputStream());

            while (gameActive) {
                // Lee las coordenadas del disparo o el mensaje desde el cliente
                String clientMessage = inputStream.readUTF();

                if ("RESET".equals(clientMessage)) {
                    // Reinicia el juego si el cliente solicita un reinicio
                    reiniciarJuego();
                    colocarBarcosAleatorios(tablero);
                    enviarMensaje(outputStream, "RESET:Barcos reubicados. ¡Que comience el juego!");
                } else if ("ADD_SHIPS".equals(clientMessage)) {
                    // Coloca barcos aleatorios si el cliente solicita agregar barcos
                    reiniciarJuego(); // Asegúrate de reiniciar antes de colocar los nuevos barcos
                    colocarBarcosAleatorios(tablero);
                    enviarMensaje(outputStream, "RESET:Barcos colocados. ¡Que comience el juego!");
                } else {
                    // Convierte el mensaje del cliente en coordenadas de disparo
                    String[] coordinates = clientMessage.split(",");
                    if (coordinates.length == 2) {
                        int row = Integer.parseInt(coordinates[0]);
                        int col = Integer.parseInt(coordinates[1]);

                        // Procesa el disparo y envía el resultado al cliente
                        if (esDisparoValido(row, col)) {
                            if (tablero[row][col] == 1) {
                                enviarMensaje(outputStream, "IMPACTO:" + row + "," + col);
                            } else {
                                enviarMensaje(outputStream, "AGUA:" + row + "," + col);
                            }
                            checkGameOver(outputStream);
                        } else {
                            // Notifica al cliente si el disparo es inválido
                            enviarMensaje(outputStream, "INVALIDO:" + row + "," + col);
                        }
                    }

                    // Imprime el tablero del juego en el servidor
                    printBoard(tablero);
                }
            }

            // Cierra la conexión TCP
            clientSocketTCP.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private static void colocarBarcosAleatorios(int[][] board) {
        Random random = new Random();

        // Coloca barcos aleatorios en el tablero
        for (int i = 0; i < NUM_SHIPS; i++) {
            int row = random.nextInt(GRID_SIZE);
            int col = random.nextInt(GRID_SIZE);

            while (board[row][col] == 1) {
                row = random.nextInt(GRID_SIZE);
                col = random.nextInt(GRID_SIZE);
            }

            board[row][col] = 1;
        }
    }

    private static boolean esDisparoValido(int row, int col) {
        // Verifica si las coordenadas del disparo están dentro del tablero
        return row >= 0 && row < GRID_SIZE && col >= 0 && col < GRID_SIZE;
    }

    private static void printBoard(int[][] board) {
        // Imprime el estado actual del tablero en el servidor
        System.out.println("Tablero del juego:");
        for (int[] row : board) {
            for (int cell : row) {
                System.out.print(cell + " ");
            }
            System.out.println();
        }
        System.out.println();
    }

    private static void enviarMensaje(DataOutputStream outputStream, String message) {
        try {
            // Envía un mensaje al cliente a través del flujo de salida TCP
            outputStream.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void checkGameOver(DataOutputStream outputStream) throws IOException {
        // Verifica si todos los barcos han sido impactados y finaliza el juego
        if (todosBarcosImpactados()) {
            enviarMensaje(outputStream, "HUNDIDA:Toda la flota ha sido hundida. ¡Enhorabuena!");
            gameActive = false;
        }
    }

    private static boolean todosBarcosImpactados() {
        // Verifica si todos los barcos en el tablero han sido impactados
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (tablero[i][j] == 1) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void reiniciarJuego() {
        // Reinicia el estado del juego, incluido el tablero
        tablero = new int[GRID_SIZE][GRID_SIZE];
        gameActive = true;
    }


}
