package pt.ulisboa.tecnico.hdscoin.server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;

public class Main {

	public static void main(String args[]) {

        try {
            ServerInterface obj = new ServerInterface();
            RemoteServer stub = (RemoteServer) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("ServerR", stub);

            System.err.println("ServerInterface ready");
        } catch (Exception e) {
            System.err.println("ServerInterface exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
