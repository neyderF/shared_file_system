package model;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerFileTransfer extends Thread {
    private ServerSocket listener;
    private Socket serverSideSocket;
    private String peerHost;
    private int port;

    private BufferedReader reader;
    private BufferedInputStream fromNetwork;
    private BufferedOutputStream toFile;
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

    public ServerFileTransfer(String host, int port, String rooDir, String userName) {
        this.port = port;
        this.peerHost = host;
        this.stop = false;
        this.rootDir = rooDir;
        this.userName = userName;


    }

    @Override
    public void run() {


        System.out.println("Servidor FT corriendo en la direccion: " + peerHost + ":" + port);
        try {
            listener = new ServerSocket(port);

            while (!stop) {
                serverSideSocket = listener.accept();
                String clientHost = serverSideSocket.getRemoteSocketAddress().toString().split(":")[0].substring(1);
                System.out.println("Se ha conectado el cliente " + clientHost);

                try {

                    reader = new BufferedReader(new InputStreamReader(serverSideSocket.getInputStream()));
                    writer = new PrintWriter(serverSideSocket.getOutputStream(), true);
                    //llega el nombre del archivo que el cliente esta buscando
                    String filename = rootDir + userName + "/Compartida/" + reader.readLine();


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


/**
 filename = "src/destino/" + filename.split("/")[2];

 fromNetwork = new BufferedInputStream(serverSideSocket.getInputStream());
 toFile = new BufferedOutputStream(new FileOutputStream(filename));

 String sizeString = reader.readLine();
 long size = Long.parseLong(sizeString.split(":")[1]);

 System.out.println(size);
 // se recibe el archivo en bloques de 512 bytes
 byte[] receivedData = new byte[512];
 int in;
 long remainder = size;
 while ((in = fromNetwork.read(receivedData)) != -1) {

 toFile.write(receivedData, 0, in);
 remainder -= in;
 if (remainder == 0)
 break;
 }
 **/


                    //reader.close();
                    //toFile.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setStop() {
        this.stop = true;
    }
}
