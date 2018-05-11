package pt.ulisboa.tecnico.hdscoin.server;

import pt.ulisboa.tecnico.hdscoin.interfaces.RemoteServerInterface;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ServerApplication {

	public static void main(String args[]) {
		
		try {
			int serversize = 4;
			int byzantineServerNumber=1;
			
			Scanner reader = new Scanner(System.in);
			/*
			System.out.println("Write number of servers you want to run:");
			String number=reader.nextLine();
			try{
				serversize=Integer.parseInt(number.trim());
			}catch(Exception e){
				System.out.println("'"+number.trim()+"'"+" is not correct!");
			}
			*/

			System.out.println("Do you want any of these servers?");
			System.out.println("1 - No Reply Server");
			String input = reader.nextLine();


			Server[] servers=new Server[serversize];
			int byzantineIndex = 0;
			if(Integer.parseInt(input) == 1){
				try {
					servers[0] = new NoReplyServer(byzantineIndex+1, serversize, byzantineServerNumber);
				} catch (MalformedURLException | NotBoundException e) {
					e.printStackTrace();
				}
				byzantineIndex= 1;
			}
			for(; byzantineIndex<servers.length; byzantineIndex++){
				try {
					System.out.println("ABC");
					servers[byzantineIndex]=new Server(byzantineIndex+1, serversize, byzantineServerNumber);
				} catch (MalformedURLException | NotBoundException e) {
					
					e.printStackTrace();
				}
			}

			while(true){
				System.out.println("\n"+serversize+" servers are running. Do you confirm?");
				System.out.println("1-Confirm.");
				System.out.println("0-Exit.");
				String confirm=reader.nextLine();
				try {
					Integer.parseInt(confirm.trim());
				}catch(Exception e) {
					System.out.println("\nThe '"+confirm+ "' is not a valid number!");
					continue;
				}
				if(Integer.parseInt(confirm.trim())==1) {
					for(int i=0; i<servers.length; i++){
						try {
							servers[i].connectServer();
						} catch (MalformedURLException | NotBoundException e) {
							e.printStackTrace();
						}
					}
				}else if(Integer.parseInt(confirm.trim())==0) {
					break;
				}else {
					System.out.println("\nThe '"+confirm+ "' is not a valid number!");
					continue;
				}
				boolean makecommand=true;
				while(makecommand) {
					System.out.println("\nChose a number to make command to");
					for(int s=1;s<=serversize;s++)
						System.out.println(s+"-server"+s);
					System.out.println("0-exit");
					
					String chosenServer=reader.nextLine();
					int chosenServerInt=0;
					try{
						chosenServerInt=Integer.parseInt(chosenServer.trim());
					}catch(Exception e){
						System.out.println("'"+chosenServer.trim()+"'"+" is not correct!");
						continue;
					}
					if(chosenServerInt==0) {
						makecommand=false;
						continue;
					}else if((chosenServerInt>serversize||chosenServerInt<1)) {
						System.out.println("\nThe '"+chosenServerInt+ "' is not a valid number!");
						continue;
					}
					boolean manageServer=true;
					while(manageServer){
						System.out.println("1-crash");
						System.out.println("2-recover");
						System.out.println("0-exit");
						String option=reader.nextLine();
						if(Integer.parseInt(option.trim())==1){
							servers[chosenServerInt].setServerFault(true);
						}else if(Integer.parseInt(option.trim())==2){
							servers[chosenServerInt].setServerFault(false);
						}else if(Integer.parseInt(option.trim())==0){
							manageServer=false;
							break;
						}else{
							System.out.println("\nThe '"+option+ "' is not a valid number!");
						}
					}
				}
			}
			
		} catch (RemoteException e) {
			System.out.println("Connection Problem");
		} catch (AlreadyBoundException e) {
			System.out.println("Already Bound");
		}
		System.out.println("GoodBye!!!");
    }
}
