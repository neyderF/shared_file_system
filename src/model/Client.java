package model;

import java.io.*;

public class Client {

    private int port;
    private static final String HOST ="localhost";
    private String name;
    private String rootDir;
    private String fileSearch;
    private String addressSearch;

    private PrintWriter writer;
    private BufferedReader reader;

    private BufferedInputStream fromFile;
    private BufferedOutputStream toNetwork;

    private ObjectOutputStream objectWriter;
    private ObjectInputStream objectReader;

    private File localFile;

    //debo tener los arhivos a compartir





    public Client(int port, String name) {
        this.port = port;
        this.name = name;
        this.rootDir = rootDir;

    }

    public void connectToIndexServer(){

    }

    public void connectToClient(){

    }
    public String[] listSharedFiles(){
        //no he definido el root dir para cada cliente
        rootDir="src/model";
        File file = new File(rootDir);
        return file.list();

    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getHOST() {
        return HOST;
    }

    public static void main(String args[]) {
        new Client(3000,"Neyder");
    }

}
