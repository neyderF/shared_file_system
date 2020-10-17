package model;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

public class Client implements Serializable {

    private static final String SERVER_INDEX_HOST = "localhost";
    private int port;
    private String host;


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


    }


    public void connectToIndexServer() {
        System.out.println("CLIENTE " + userName + " conectandose al servidor");

        try {

            clientSideSocket = new Socket(SERVER_INDEX_HOST, 5000);
            createStreams();
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

    private void executeIndexServerProtocol() throws IOException {


        while (!serverResponse.equals("EXIT")) {

            System.out.print(userName + "Ingrese un mensaje: ");
            String clientPrompt = new Scanner(System.in).nextLine();

            writer.println(clientPrompt);

            if (clientPrompt.contains(":")) {
                // con cada if se recive la respuesta del servidor de diferente manera
                if (clientPrompt.split(":")[0].equals("REGISTER")) {

                    serverResponse = reader.readLine();//el servidor me pide los archivos
                    if (serverResponse.contains("archivos")) {
                        System.out.println("Servidor: " + serverResponse);
                        clientPrompt = new Scanner(System.in).nextLine();// ingreso los archivos
                        writer.println(clientPrompt);

                        serverResponse = reader.readLine(); // me devuelve registro exitoso

                    }
                    System.out.println("Servidor: " + serverResponse); // imprimo el registro exitoso

                } else if (clientPrompt.split(":")[0].equals("SEARCH")) {// si se escribe search entonces el cliente espera un Arraylist

                    try {
                        ArrayList<String> resultList = (ArrayList<String>) objectReader.readObject();
                        if (!resultList.isEmpty()) {
                            System.out.println("Servidor: el archivo lo tiene:" + resultList.toString());

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
                        localFileServer = new ServerFileTransfer(host, port, rootDir,dataConnection[1]);
                        localFileServer.start();


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
        serverResponse = "";
        //elimina el servidor local del peer al desconectarse del indice
        localFileServer.setStop();
        localFileServer.interrupt();

    }


    public void connectToServerPeer(ArrayList<String> conexiones, String fileName) {

        ClientFileTransfer peerClient = new ClientFileTransfer(userName.substring(1,userName.length()-1));
        for (String connectionData :
                conexiones) {
            String data[] = connectionData.split("@");
            String connection[] = data[1].split(":");
            System.out.println("Conectando con " + connectionData);
            peerClient.startConnection(fileName, connection[0], Integer.parseInt(connection[1]), rootDir);
            if (!peerClient.connectionRefused) {
                System.out.println("EXITO el archivo ya se encuentra en su carpeta de descargas");
                break;
            } else {
                System.out.println("No se pudo conectar con " + connectionData);
            }
            peerClient=null;
        }


    }


    private void createStreams() throws IOException {

        writer = new PrintWriter(clientSideSocket.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(clientSideSocket.getInputStream()));
        objectWriter = new ObjectOutputStream(clientSideSocket.getOutputStream());
        objectReader = new ObjectInputStream(clientSideSocket.getInputStream());

    }

    private void send(Object o) throws IOException {
        objectWriter.writeObject(o);
        writer.flush();
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

    public static void main(String args[]) {
        Client cliente = new Client();
        cliente.connectToIndexServer();

    }

}
