package pt.ulisboa.tecnico.hdscoin.server;

import pt.ulisboa.tecnico.hdscoin.interfaces.RemoteServerInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Main {

	public static void main(String args[]) {

        try {
            System.setProperty("java.rmi.server.hostname","127.0.0.1");
            ServerInterface obj = new ServerInterface();
            Registry registry = LocateRegistry.createRegistry(1099);
            RemoteServerInterface stub = (RemoteServerInterface) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            registry.bind("RemoteServerInterface", stub);

            System.err.println("ServerInterface ready");
        } catch (Exception e) {
            System.err.println("ServerInterface exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
