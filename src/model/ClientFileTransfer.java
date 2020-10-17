package model;

import java.io.*;
import java.net.Socket;

public class ClientFileTransfer {

    public boolean connectionRefused;
    private Socket clientSideSocket;
    private PrintWriter writer;
    private BufferedReader reader;

    private BufferedInputStream fromNetwork;
    private BufferedOutputStream toFile;
    private String userName;

    public ClientFileTransfer(String userName){
        System.out.println("Cliente "+userName+" FT en linea");
        this.userName=userName;
    }

    public void startConnection(String filename,String server, int port, String rooDir){


        String absoluteFileName = rooDir+"/"+userName+"/Descargas/"+filename;

        try {
            System.err.println( filename+server+port+rooDir);
            clientSideSocket = new Socket(server, port);


            reader = new BufferedReader(new InputStreamReader(clientSideSocket.getInputStream()));
            writer = new PrintWriter(clientSideSocket.getOutputStream(), true);

            fromNetwork = new BufferedInputStream(clientSideSocket.getInputStream());
            toFile = new BufferedOutputStream(new FileOutputStream(absoluteFileName));

            writer.println(filename);

            String sizeString = reader.readLine();
            long size = Long.parseLong(sizeString.split(":")[1]);

            // se recibe el archivo en bloques de 512 bytes
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
            toFile.close();

            connectionRefused=false;
        } catch (Exception e) {

            connectionRefused=true;

            e.printStackTrace();

        }


    }
}
