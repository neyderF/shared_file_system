package main;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Client implements Serializable {

    private static final String SERVER_INDEX_HOST = "localhost";
    private int port;
    private String host;

    private HashMap<String, ArrayList<String>> statistics;

    private String userName;
    private String rootDir;
    private String fileSearch;

    private Socket clientSideSocket;


    private PrintWriter writer;
    private BufferedReader reader;

    private BufferedInputStream fromFile;
    private BufferedOutputStream toNetwork;
    private File localFile;

    private ObjectOutputStream objectWriter;
    private ObjectInputStream objectReader;

    private String serverResponse;

    private ServerFileTransfer localFileServer;


    public Client() {

        serverResponse = "";
        userName = "";
        this.rootDir = "./rootDir/" + userName;
        this.statistics = new HashMap<>();


    }


    public void launchPrompt() {


        try {

            //clientSideSocket = new Socket(SERVER_INDEX_HOST, 5000);
            //createStreams();
            executeIndexServerProtocol();

        }
        // Puede lanzar una excepcion de host desconocido.
        catch (UnknownHostException e) {
            e.printStackTrace();
        }
        // Puede lanzar una excepcion de entrada y salida.
        catch (IOException e) {
            e.printStackTrace();
        }

        // Finalmente se cierran los flujos y el socket.
        finally {
            try {
                if (reader != null)
                    reader.close();
                if (writer != null)
                    writer.close();
                if (clientSideSocket != null)
                    clientSideSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**Metodo que contiene el protocolo para la conexion con el servidor índice
     * contempla tanto comandos locales como comandos de peticion al servidor
     *
     * @throws IOException
     */
    private void executeIndexServerProtocol() throws IOException, SocketException {

        String clientPrompt="";
        String clientLocalPrompt="";
        serverResponse="EXIT";
        while (!clientLocalPrompt.equals("BYE")) {

            while (!serverResponse.equals("EXIT")) {

                System.out.print(userName + "Ingrese un mensaje para el servidor: ");
                clientPrompt = new Scanner(System.in).nextLine();

                if (clientPrompt.contains("EST")) { // entra si esta autentificado y quiere conocer las estadisticas
                    if (userName.length() > 1) {

                        evaluateLocalCommands(clientPrompt);

                    } else {
                        System.out.println("Servidor Local: debe autentificarse con el servidor índice al menos una vez");
                    }
                } else {
                    writer.println(clientPrompt);
                    if (clientPrompt.contains(":")) {
                        // con cada if se recive la respuesta del servidor de diferente manera
                        if (clientPrompt.split(":")[0].equals("REGISTER")) {

                            //File temp=new File(rootDir+clientPrompt.split(":")[0]);


                            serverResponse = reader.readLine();//el servidor me pide los archivos
                            if (serverResponse.contains("archivos")) {



                                System.out.println("Servidor: " + serverResponse);
                                clientPrompt = new Scanner(System.in).nextLine();// ingreso los archivos
                                writer.println(clientPrompt);

                                serverResponse = reader.readLine(); // me devuelve registro exitoso

                            }
                            System.out.println("Servidor: " + serverResponse); // imprimo el registro exitoso

                        } else if (clientPrompt.split(":")[0].equals("SEARCH") && userName.length() > 1) {// si se escribe search entonces el cliente espera un Arraylist

                            try {
                                ArrayList<String> resultList = (ArrayList<String>) objectReader.readObject();

                                if (!resultList.isEmpty()) {
                                    System.out.println("Servidor: el archivo lo tiene:" + resultList.toString());
                                    //se lanza el cliente de transferencia de archivos en PRIMEER PLANO
                                    connectToServerPeer(resultList, clientPrompt.split(":")[1]);
                                } else {
                                    System.out.println("Servidor: ninguno de los peers tiene el archivo:");
                                }
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }

                        } else {
                            serverResponse = reader.readLine();
                            System.out.println("Servidor: " + serverResponse);
                            String dataConnection[] = serverResponse.split(" ");

                            if (serverResponse.contains("Bienvenido")) {

                                this.host = dataConnection[2].split(":")[0];
                                this.port = Integer.parseInt(dataConnection[2].split(":")[1]);

                                userName = "(" + dataConnection[1] + ")";

                                //inicia el servidor del peer en segundo plano
                                if(localFileServer==null) {
                                    localFileServer = new ServerFileTransfer(host, port, rootDir, dataConnection[1], this);
                                    localFileServer.start();
                                }


                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } else {

                        serverResponse = reader.readLine();
                        System.out.println("Servidor: " + serverResponse);
                    }

                }
            }
            System.out.print(userName + "Ingrese un mensaje local: ");
            clientLocalPrompt = new Scanner(System.in).nextLine();
            if(clientLocalPrompt.contains("EST")){// entra si NO esta autentificado y quiere conocer las estadisticas o conectarse al servidor
                if (userName.length() > 1) {

                    evaluateLocalCommands(clientLocalPrompt);

                }else {
                    System.out.println("Servidor Local: debe autentificarse con el servidor índice al menos una vez");
                }
            }else if(clientLocalPrompt.equals("CONNECT")){
                serverResponse="";
                //intenta conectarse al servidor

                clientSideSocket = new Socket(SERVER_INDEX_HOST, 5000);
                createStreams();
                System.out.println("Se ha conectado al servidor índice");
                userName="";
            }else{
                System.out.println("Comando local no reconocido");
            }

        }
        serverResponse = "";

    }

    /**Metodo que evalua si los comandos ingresados son locales,
     *  es decir que no involucran una peticion a ningun servidor
     *
     * @param clientPrompt
     */
    private void evaluateLocalCommands(String clientPrompt) {

        String response = "";
        String command[] = clientPrompt.split(":");
        if (command.length > 1) {
            if (command[0].equals("EST1")) { // por nombre de archivo

                try {
                    response = "El cliente " + command[1] + " ha realizado " + requestClientName(command[1]) + " solicitudes al servidor índice";
                } catch (NullPointerException e) {
                    response = "el cliente nunca se ha conectado a este servidor";

                }
            } else if (command[0].equals("EST2")) {// por tipo de extension
                response = "La cantidad de solicitudes que involucran esta extension ." + command[1] + " es: " + requestClientByFileType(command[1]);
            }

            //es posible que el archivo que busque un cliente  dentro de un servidor de archivisos no esté disponible?
            //teniendo en cuenta que el servidor indice si lo encontró

        } else if (clientPrompt.equals("EST3")) { //las solicitudes de archivos no encontrados
            response = "Hasta el momento hay " + clientSearchRefused() + " solicitudes de archivos que ningun cliente tiene";

        } else {
            response = "Comando no reconocido";
        }


        System.out.println("Servidor Local dice: " + response);


    }

    /**
     * metodo que crea el cliente de transferencia de archivos y
     * le envia los datos de conexion para que se pueda conectar a un servidor de archivos
     *
     * @param conexiones
     * @param fileName
     */

    public void connectToServerPeer(ArrayList<String> conexiones, String fileName) {

        ClientFileTransfer peerClient = new ClientFileTransfer(userName.substring(1, userName.length() - 1));
        for (String connectionData :
                conexiones) {
            String data[] = connectionData.split("@");
            String connection[] = data[1].split(":");
            System.out.println("Conectando con " + connectionData);
            peerClient.startConnection(fileName, connection[0], Integer.parseInt(connection[1]), rootDir);
            if (!peerClient.connectionRefused) {
                System.out.println("EXITO el archivo se transfirio correctamente, revise su carpeta de descargas");
                break;
            } else {
                System.out.println("No se pudo conectar con " + connectionData);


            }

        }



    }


    private void createStreams() throws IOException {

        writer = new PrintWriter(clientSideSocket.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(clientSideSocket.getInputStream()));
        objectWriter = new ObjectOutputStream(clientSideSocket.getOutputStream());
        objectReader = new ObjectInputStream(clientSideSocket.getInputStream());

    }


    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getRootDir() {
        return rootDir;
    }

    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

    public String getFileSearch() {
        return fileSearch;
    }

    public void setFileSearch(String fileSearch) {
        this.fileSearch = fileSearch;
    }


    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getHost() {
        return this.host;
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

    public HashMap<String, ArrayList<String>> getStatistics() {
        return statistics;
    }

    public void setStatistics(HashMap<String, ArrayList<String>> statistics) {
        this.statistics = statistics;
    }


    public static void main(String args[]) {
        Client cliente = new Client();
        cliente.launchPrompt();

    }

}
