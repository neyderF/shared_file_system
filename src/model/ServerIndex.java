package model;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ServerIndex {

    public static final int PORT = 5000;
    private int maxClientsFile;
    private ServerSocket listener;
    private Socket serverSideSocket;

    private BufferedReader reader;
    private PrintWriter writer;

    private ObjectOutputStream objectWriter;
    private ObjectInputStream objectReader;


    private HashMap<String, String[]> peers;

    private boolean clientConnected;
    private String code = " ";
    private String value = "";

    private String peerHost;
    private int initPort;

    public ServerIndex(int maxClientsFile) {
        this.peers = new HashMap<>();
        this.initPort = 6000;
        this.maxClientsFile = maxClientsFile;
        this.clientConnected = false;
        this.peerHost = "";

        runServer();
    }

    public void runServer() {

        try {

            listener = new ServerSocket(PORT);
            System.out.println("SERVIDOR CONECTADO EN EL PUERTO : " + PORT);

            while (true) {
                System.out.println("El servidor esta esperando por un cliente...");
                serverSideSocket = listener.accept();
                peerHost = serverSideSocket.getRemoteSocketAddress().toString().split(":")[0].substring(1);
                System.out.println("Se ha conectado el cliente "+ peerHost);

                try {

                    createStreams();
                    executeProtocol();
                    System.out.println("Un cliente se ha desconectado");
                    writer.println("EXIT");


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {

                if (serverSideSocket != null)
                    serverSideSocket.close();
                if (listener != null)
                    listener.close();
                if (serverSideSocket != null)
                    serverSideSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void createStreams() throws IOException {

        writer = new PrintWriter(serverSideSocket.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(serverSideSocket.getInputStream()));
        objectWriter = new ObjectOutputStream(serverSideSocket.getOutputStream());
        objectReader = new ObjectInputStream(serverSideSocket.getInputStream());

    }

    private void executeProtocol() throws IOException {

        while (!code.equals("EXIT")) {

            String userMessage = reader.readLine();
            value = "";
            if (userMessage.contains(":")) {
                String userMessage2[] = userMessage.split(":");
                code = userMessage2[0];
                value = userMessage2[1];
            } else {
                code = userMessage;
            }

            if (code.equals("REGISTER")) {

                if (!searchClient(value)) {

                    String[] data = new String[2];
                    initPort++;
                    data[0] = peerHost + ":" + initPort;

                    writer.println("Ingrese la lista de archivos que desea compartir separados por comas:");

                    String files = reader.readLine();
                    data[1] = files;
                    peers.put(value, data);
                    writer.println("Registro exitoso, la conexion asignada fue: " + data[0]);
                    clientConnected = true;

                } else {
                    writer.println("Este cliente ya existe, pruebe con otro nombre");
                }

            } else if (code.equals("LOGIN")) {

                if (searchClient(value)) {
                    System.err.println(peers.get(value)[0]);
                    writer.println("Bienvenido " + value +" "+ peers.get(value)[0]);
                    clientConnected = true;

                } else {
                    writer.println("El cliente " + value + " no existe");
                }

            } else if (code.equals("SEARCH")) {

                if (clientConnected) {

                    //writer.println("Modo busqueda para cliente ");
                    System.out.println("BUSCAR: " + value);
                    ArrayList<String> result = searchFile(value);
                    objectWriter.writeObject(result);
                    objectWriter.flush();

                } else {
                    writer.println("Debe autentificarse primero");
                }
            } else if (!code.equals("EXIT")) {
                writer.println("Comando no reconocido");
            }
        }

        clientConnected = false;
        code = "";
    }


    public ArrayList<String> searchFile(String searchFileName) {
        ArrayList<String> searchResult = new ArrayList<>();
        if (!peers.isEmpty()) {
            for (Map.Entry<String, String[]> entry :
                    peers.entrySet()) {
                String files[] = entry.getValue()[1].split(",");

                for (String file :
                        files) {
                    if (file.equals(searchFileName) && searchResult.size() < maxClientsFile) {
                        searchResult.add(entry.getKey() + "@" + entry.getValue()[0]);
                    }
                }
            }

        }

        return searchResult;

    }

    public boolean searchClient(String name) {

        return peers.containsKey(name);

    }


    public static void main(String args[]) {
        new ServerIndex(3);
    }

}
