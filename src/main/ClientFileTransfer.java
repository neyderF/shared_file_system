package main;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;

public class ClientFileTransfer {

    public boolean connectionRefused;
    private Socket clientSideSocket;
    private PrintWriter writer;
    private BufferedReader reader;

    private BufferedInputStream fromNetwork;
    private BufferedOutputStream toFile;
    private String userName;

    public ClientFileTransfer(String userName) {
        System.out.println("Cliente " + userName + " FT en linea");
        this.userName = userName;
    }

    /**
     * Metodo que ejecuta el protocolo para solicitar y recibir archivos del {@link ServerFileTransfer}
     * Cada cliente(peer) ejecuta este cliente de transferencia de archivos cuando se realiza una busqueda
     *
     * una vez se establece la conexion se ejecuta el protocolo
     * Protocolo:
     * se envia al servidor el nombre del cliente que busca el archivo y el nombre del archivo a buscar
     * el servidor contesta con el tamaño del archivo (no se tiene contemplado que el servidor de archivos no encuentre el archivo)
     * se inicia con la recepcion del archivo en bloques de 1024 bytes y se guarda en la carpeta descargas del
     *
     *
     *
     * @param filename
     * @param server
     * @param port
     * @param rooDir
     *
     */

    public void startConnection(String filename, String server, int port, String rooDir) {


        String absoluteFileName = rooDir + "/" + userName + "/Descargas/" + filename;

        try {
            //System.err.println( userName + server + port + rooDir);
            clientSideSocket = new Socket(server, port);


            reader = new BufferedReader(new InputStreamReader(clientSideSocket.getInputStream()));
            writer = new PrintWriter(clientSideSocket.getOutputStream(), true);

            fromNetwork = new BufferedInputStream(clientSideSocket.getInputStream());
            toFile = new BufferedOutputStream(new FileOutputStream(absoluteFileName));

            writer.println(userName + "," + filename);

            String sizeString = reader.readLine();
            long size = Long.parseLong(sizeString.split(":")[1]);

            //se cambia el nombre en caso de que ya exista el archivo en la carpeta de descargas
            //Files.exists(filename);

            // se recibe el archivo en bloques de 1024 bytes
            byte[] receivedData = new byte[1024];
            int in;
            long remainder = size;
            while ((in = fromNetwork.read(receivedData)) != -1) {

                toFile.write(receivedData, 0, in);
                remainder -= in;
                if (remainder == 0)
                    break;
            }

            reader.close();
            writer.close();
            toFile.close();
            fromNetwork.close();
            clientSideSocket.close();
            connectionRefused = false;

        } catch (Exception e) {

            connectionRefused = true;

        }


    }
}
