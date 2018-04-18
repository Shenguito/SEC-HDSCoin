package pt.ulisboa.tecnico.hdscoin.client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;



public class ClientApplication {

    public static void main(String[] args) {
    	String host = (args.length < 1) ? null : args[0];
        Client client;
    	boolean exit=false;
    	boolean initing=false;
    	Scanner reader = new Scanner(System.in);
    	
    	while(!initing) {
    		System.out.println("Select a number to init:");
    		System.out.println("1-Alice");
    		System.out.println("2-Bob.");
    		System.out.println("3-Charlie.");
    		String userNumer = reader.nextLine();
    		try {
	            switch (userNumer.trim()) {
	                case "1":  client=new Client(host, "alice", "alice123");
	                         break;
	                case "2":  client=new Client(host, "bob", "bob123");
	                         break;
	                case "3":  client=new Client(host, "charlie", "charlie123");
	                		break;
	                default: System.out.println("\nThe '"+userNumer+ "' is not a valid number!");
	                		continue;
	            }
    		}catch(RemoteException e) {
    			System.out.println("\nServer not started.");
    			initing=true;
    			continue;
    		}catch(NotBoundException e) {
    			System.out.println("\nServer not bound.");
    			initing=true;
    			continue;
    		}
    		exit=false;
	    	while(!exit) {
	    		
	    		System.out.println("\nChoose below options by its number...");
	    		System.out.println("1-Register.");
	    		System.out.println("0-Exit.");
	    		String registerOption = reader.nextLine();
	    		
	    		if(registerOption.equals("0")) {
	        		exit=true;
	        	}else if(registerOption.equals("1")) {
	        		boolean registered=client.register();
	        		while(registered){
	        			System.out.println("\nChoose below options by its number...");
		        		System.out.println("1-Send.");
			    		System.out.println("2-Check.");
			    		System.out.println("3-Receive.");
			    		System.out.println("4-Audit.");
			    		System.out.println("0-Exit.");
			    		String option = reader.nextLine();
			    		if(option.equals("0")) {
			    			registered=false;
			        	}else if(option.equals("1")) {
			        		if(client.clientHasMessageNotSent()){
			        			System.out.println("Resending last message...");
			        			client.reSend();
			        		}
			        		else{
				        		System.out.println("Amount:");
				        		String sendAmount = reader.nextLine();
				        		System.out.println("Destination (Available: Alice Bob Charlie; you are "+client.getClientName().toUpperCase()+"):");
				        		String sendDestination = reader.nextLine();
				        		client.send(sendDestination, sendAmount);
			        		}
		    			}else if(option.equals("2")) {
			    			System.out.println("Destination (Available: Alice Bob Charly; you are "+client.getClientName().toUpperCase()+"):");
			    			String destination = reader.nextLine();
			    			client.check(destination.toLowerCase().trim());
			    		}else if(option.equals("3")) {
			    			boolean receiveExit=false;
			    			List<Integer> chosenNumbers=new ArrayList<Integer>();
			    			while(!receiveExit){
			    				System.out.println("Choose pending transfer:\n0-back.");
				    			String pendingTransfer = reader.nextLine();
				    			int chosenNumber=-1;
				    			try{
				    				chosenNumber=Integer.parseInt(pendingTransfer.trim());
				    			}catch(Exception e){
				    				System.out.println("'"+pendingTransfer.trim()+"'"+" is not correct!");
				    				continue;
				    			}
				    			if(chosenNumber==0){
				    				receiveExit=true;
				    				continue;
				    			}
				    			if(chosenNumbers.contains(chosenNumber)){
				    				System.out.println("Number '"+chosenNumber+"' is already chosen");
				    				continue;
				    			}
				    			chosenNumbers.add(chosenNumber);
				    			client.receive(chosenNumber);
			    			}
			    			client.removePendingTransaction();
			    		}else if(option.equals("4")) {
			    			System.out.println("Destination (Available: Alice Bob Charly; you are "+client.getClientName().toUpperCase()+"):");
			    			String destination = reader.nextLine();
			        		client.audit(destination.toLowerCase().trim());
			    		}else {
			    			System.out.println("The '"+option+ "' is not valid!");
			    		}
	        		}
	        	}else {
	    			System.out.println("The '"+registerOption+ "' is not valid!");
	    		}
    		}
	    	System.out.println("\nGoodBye! See you next time!");
    	}
    }
}
