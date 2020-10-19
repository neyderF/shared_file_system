package main;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerFileTransfer extends Thread {
    private Client client;
    private ServerSocket listener;
    private Socket serverSideSocket;
    private String peerHost;
    private int port;

    private BufferedReader reader;

    private String filename;
    private boolean stop;
    private String rootDir;

    private String userName;
    private PrintWriter writer;

    // flujo de entrada para leer un archivo
    private BufferedInputStream fromFile;
    // flujo de salida para escribir bytes de la red
    private BufferedOutputStream toNetwork;
    // Objeto para manejar el archivo a enviar
    private File localFile;
    private String actualClient;

    public ServerFileTransfer(String host, int port, String rooDir, String userName, Client client) {
        this.port = port;
        this.peerHost = host;
        this.stop = false;
        this.rootDir = rooDir;
        this.userName = userName;// es el peer dueño del servidor no el nombre del cliente que busca
        this.client = client;//este tambien es el dueño del servidor
        this.actualClient="";

    }

    /**
     * Metodo que ejcuta el protocolo de la transferencia de archivos del lado del servidor
     * Cada cliente tiene un servidor de archivos que se ejecuta cuando el cliente se autentifica con el servidor indice
     *
     * Una vez se acepta una conexión se ejecuta el protocolo:
     *  se recibe el nombre del archivo
     *  y posteriormente se le envia al cliente el tamaño del archivo,
     *  luego se procede a enviar el archivo en bloqyes de 1024 bytes
     *
     */

    @Override
    public void run() {


        System.out.println("Servidor FT corriendo en la direccion: " + peerHost + ":" + port);
        try {
            listener = new ServerSocket(port);

            while (!stop) {
                serverSideSocket = listener.accept();
                String clientHost = serverSideSocket.getRemoteSocketAddress().toString().split(":")[0].substring(1);


                try {

                    reader = new BufferedReader(new InputStreamReader(serverSideSocket.getInputStream()));
                    writer = new PrintWriter(serverSideSocket.getOutputStream(), true);
                    //llega el nombre del archivo que el cliente esta buscando y el nombre del cliente
                    String name = reader.readLine();
                    actualClient=name.split(",")[0];
                    System.out.println("El cliente " + actualClient +" esta solicitando el archivo "+name.split(",")[1] );
                    String filename = rootDir + userName + "/Compartida/" + name.split(",")[1];


                    localFile = new File(filename);


                    long size = localFile.length();
                    writer.println("Size:" + size);

                    fromFile = new BufferedInputStream(new FileInputStream(localFile));
                    toNetwork = new BufferedOutputStream(serverSideSocket.getOutputStream());

                    byte[] byteArray = new byte[1024];
                    int in;
                    while ((in = fromFile.read(byteArray)) != -1) {

                        toNetwork.write(byteArray, 0, in);
                    }

                    // se desocupa el socket
                    toNetwork.flush();
                    fromFile.close();


                    // se registra la solicitud al servidor
                    String fileSearch = name.split(",")[1];

                    if(!client.getStatistics().containsKey(actualClient)){
                        client.getStatistics().put(actualClient,new ArrayList<String>());
                    }
                    client.getStatistics().get(actualClient).add(fileSearch);




                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        } catch (Exception e) {
            //e.printStackTrace();

        }

    }


    public void setStop() {
        this.stop = true;
    }


}
