package model;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerIndex {

    public static final int PORT = 5000;
    private int maxClientsFile;
    private ServerSocket listener;
    private Socket serverSideSocket;

    private BufferedReader reader;
    private PrintWriter writer;

    private ObjectOutputStream objectWriter;
    private ObjectInputStream objectReader;



    private ArrayList<Client> clients;

    private  boolean clientConnected;


    public ServerIndex(int maxClientsFile) {
        clients = new ArrayList<>();
        this.maxClientsFile = maxClientsFile;
        clientConnected =false;

        runServer();
    }

    public void runServer() {

        try {

            listener = new ServerSocket(PORT);
            System.out.println("SERVIDOR CONECTADO EN EL PUERTO : " + PORT);

            while (true) {
                System.out.println("El servidor esta esperando por un cliente...");
                serverSideSocket = listener.accept();
                System.out.println("Un cliente se ha conectado...");

                try {

                    createStreams();
                    clientConnected =true;
                    executeProtocol();
                    clientConnected =false;
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


        String code =" ";

        while (clientConnected&& !code.equals("EXIT")) {
            String userMessage[] = reader.readLine().split(" :");
            code = userMessage[0];
            String name = userMessage[1];

            if (code.equals("REGISTER")) {
                writer.println("OK");
                try {
                    if (!searchClient(name)) {
                        Client newClient = (Client) objectReader.readObject();
                        clients.add(newClient);
                        writer.println("Registro exitoso");
                        startDataSend();

                    } else {
                        writer.println("Este cliente ya existe, pruebe con otro nombre");
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            } else if (code.contains("LOGIN")) {


                if (searchClient(name)) {
                    writer.println("Bienvenido " + name);

                    startDataSend();

                } else {
                    writer.println("El Cliente" + name + " no exixte registrese por favor");
                }

            }
        }


    }

    private void startDataSend() throws IOException {

        String code = "";
        String searchFileName="";
        while (!code.equals("EXIT")){

            String userMessage[] = reader.readLine().split(" :");
            code=userMessage[0];
            searchFileName = userMessage[1];

            ArrayList<String>result=searchFile(searchFileName);
            objectWriter.writeObject(result);


        }

    }


    public ArrayList<String> searchFile(String searchFileName){
        int count=0;
        ArrayList<String>searchResult=new ArrayList<>();
        for (Client client: clients) {
            for (String file:
                    client.listSharedFiles()) {
                if(count!=maxClientsFile){
                    if (file.equals(searchFileName)){
                        searchResult.add(client.getHOST()+":"+client.getPort());
                        count++;
                    }
                }
            }
        }

        return  searchResult;

    }
    public boolean searchClient(String name){
        boolean exist=false;
        for (Client c :clients) {
            if(c.equals(name)){
                exist=true;
            }
        }

        return  exist;

    }


    public static void main(String args[]) {
        new ServerIndex(3);
    }

}
