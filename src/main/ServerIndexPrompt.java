package main;

import java.util.Scanner;

/**
 * Clase que representa el prompt del servidor,
 * la salida e ingreso  de datos de esta clase se verán en la consola del servidor
 * es decir que tanto la clase serverindex como esta comparten la misma consola
 *
 *
 */

public class ServerIndexPrompt extends Thread {

    private ServerIndex serverIndex;


    public ServerIndexPrompt(ServerIndex serverIndex) {
        this.serverIndex = serverIndex;
    }

    @Override
    public void run() {


        while (true) {

            System.out.print("Ingrese un mensaje: ");

            String userPrompt = new Scanner(System.in).nextLine();
            String response = "";
            String command[] = userPrompt.split(":");
            if (command.length > 1) {
                if (command[0].equals("EST1")) { // por nombre de cliente

                    try {
                        response = "El cliente " + command[1] + " ha realizado " + serverIndex.requestClientName(command[1]) + " solicitudes al servidor índice";
                    } catch (NullPointerException e) {
                        response = "el cliente no existe";

                    }
                } else if (command[0].equals("EST2")) {// por tipo de extension
                    response = "La cantidad de solicitudes que involucran esta extension ." + command[1] + " es: " + serverIndex.requestClientByFileType(command[1]);
                } else if (command[0].equals("MAX")) {//configura el maximo de resultados a las solicitudes
                    serverIndex.setMaxClientsFile(Integer.parseInt(command[1]));
                    response = "Ahora los clientes tendran maximo " + command[1] + " resultados de busqueda";

                }
            } else if (userPrompt.equals("EST3")) { //las solicitudes de archivos no encontrados
                response = "Hasta el momento hay " + serverIndex.clientSearchRefused() + " solicitudes de archivos que ningun cliente tiene";

            } else {
                response = "Comando no reconocido";
            }


            System.out.println("Servidor: " + response);


        }

    }

}
