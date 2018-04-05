package pt.ulisboa.tecnico.hdscoin.server;

import pt.ulisboa.tecnico.hdscoin.interfaces.RemoteServerInterface;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class ServerApplication {

	public static void main(String args[]) {
		

		try {
			Server server = new Server();
		} catch (RemoteException e) {
			System.out.println("Connection Problem");
		} catch (AlreadyBoundException e) {
			System.out.println("Already Bound");
		}

    }
}
