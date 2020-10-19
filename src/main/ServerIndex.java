package main;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
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
    private HashMap<String, ArrayList<String>> statistics;

    private boolean clientConnected;
    private String actualClient;
    private String code = " ";
    private String value = "";

    private String peerHost;
    private int initPort;

    public ServerIndex(int maxClientsFile) {
        this.peers = new HashMap<>();
        this.statistics = new HashMap<>();
        this.initPort = 6000;
        this.maxClientsFile = maxClientsFile;
        this.clientConnected = false;
        this.peerHost = "";
        this.actualClient = "";

        runServer();
    }

    public void runServer() {

        try {

            listener = new ServerSocket(PORT);
            System.out.println("SERVIDOR CORRIENDO EN EL PUERTO : " + PORT);
            ServerIndexPrompt prompt = new ServerIndexPrompt(this);
            prompt.start();

            while (true) {
                System.out.println("El servidor esta esperando por un cliente...");
                serverSideSocket = listener.accept();
                peerHost = serverSideSocket.getRemoteSocketAddress().toString().split(":")[0].substring(1);
                System.out.println("Se ha conectado el cliente " + peerHost);

                try {

                    createStreams();
                    executeProtocol();
                    System.out.println("El cliente " + peerHost + " se ha desconectado");
                    writer.println("EXIT");


                } catch (IOException e) {
                    //e.printStackTrace();
                    System.out.println("El cliente se ha desconectado repentinamente");
                }
            }
        } catch (IOException e) {
            System.err.println("ERROR en el servidor indice");
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

    /**Metodo que ejecuta el protocolo en el lado del servidor
     * los comandos locales son capturados desde la clase serverIndexPrompt
     *
     * @throws IOException
     */
    private void executeProtocol() throws IOException, SocketException {

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
                    //clientConnected = true;
                    statistics.put(value, new ArrayList<String>());

                } else {
                    writer.println("Este cliente ya existe, pruebe con otro nombre");
                }

            } else if (code.equals("LOGIN")) {

                if (searchClient(value)) {
                    System.err.println(peers.get(value)[0]);
                    writer.println("Bienvenido " + value + " " + peers.get(value)[0]);
                    actualClient = value;
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

                    String fileSearch = value + "," + !result.isEmpty();
                    System.err.println(statistics.size());
                    statistics.get(actualClient).add(fileSearch);

                } else {
                    writer.println("Debe autentificarse primero");
                }
            } else if (!code.equals("EXIT")) {
                writer.println("Comando no reconocido");
            } else if (!code.equals("EXIT")) {
                writer.println("Comando no reconocido");
            }
        }

        clientConnected = false;
        actualClient = "";
        code = "";
    }

    /**Metodo que busca entre todos los clientes un archivo,
     * la busqueda se hace por el nomre
     *
     * @param searchFileName
     * @return  searchResult lista de clientes que tienen el archivo solicitado
     * el maximo numero de clientes estad definido por la variable maxClientsFile
     */

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

    /**metodo que indica si un cliente exite o no
     *
     * @param name
     * @return
     */
    public boolean searchClient(String name) {

        return peers.containsKey(name);

    }

    /**
     * retorna la cantidad de peticiones de un usuario
     *
     * @param name
     * @return
     */
    public int requestClientName(String name) {

        return statistics.get(name).size();

    }

    /**
     * metodo que dada una extension de archivo devuelve el número de consultas
     * que se hicieron a este tipo de archivos
     *
     * @param fileExt extension del archivo  sin el punto ej jpg,pdf
     * @return
     */

    public int requestClientByFileType(String fileExt) {

        int result = 0;

        if (!statistics.isEmpty()) {
            for (Map.Entry<String, ArrayList<String>> entry :
                    statistics.entrySet()) {
                ArrayList<String> requests = entry.getValue();

                for (String req :
                        requests) {
                    String ext = (req.replace(".", ",")).split(",")[1];
                    if (ext.equals(fileExt)) {
                        result++;
                    }
                }
            }

        }
        return result;
    }

    /**
     * Metodo que retorna la cantidad de solicitudes de archivos, que no fueron encontrados
     *
     * @return
     */
    public int clientSearchRefused() {

        int result = 0;

        if (!statistics.isEmpty()) {
            for (Map.Entry<String, ArrayList<String>> entry :
                    statistics.entrySet()) {
                ArrayList<String> requests = entry.getValue();

                for (String req :
                        requests) {
                    boolean ext = Boolean.parseBoolean(req.split(",")[1]);
                    if (!ext) {
                        result++;
                    }
                }
            }

        }
        return result;
    }

    public void setMaxClientsFile(int maxClientsFile) {
        this.maxClientsFile = maxClientsFile;
    }


    public static void main(String args[]) {
        new ServerIndex(3);
    }

}
