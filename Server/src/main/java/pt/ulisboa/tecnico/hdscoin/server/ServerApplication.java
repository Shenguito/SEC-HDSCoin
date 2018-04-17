package pt.ulisboa.tecnico.hdscoin.server;

import pt.ulisboa.tecnico.hdscoin.interfaces.RemoteServerInterface;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;

public class ServerApplication {

	public static void main(String args[]) {
		

		try {
			Server server = new Server();
			Scanner reader = new Scanner(System.in);
			while(true){
				System.out.println("1-crash");
				System.out.println("2-recover");
				String option=reader.nextLine();
				if(Integer.parseInt(option.trim())==1){
					server.setServerFault(true);
				}else if(Integer.parseInt(option.trim())==2){
					server.setServerFault(false);
				}else{
					System.out.println("\nThe '"+option+ "' is not a valid number!");
				}
			}
		} catch (RemoteException e) {
			System.out.println("Connection Problem");
		} catch (AlreadyBoundException e) {
			System.out.println("Already Bound");
		}

    }
}
