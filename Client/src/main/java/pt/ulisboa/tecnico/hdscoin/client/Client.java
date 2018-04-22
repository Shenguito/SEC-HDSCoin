package pt.ulisboa.tecnico.hdscoin.client;



import pt.ulisboa.tecnico.hdscoin.Crypto.CipheredMessage;
import pt.ulisboa.tecnico.hdscoin.Crypto.CryptoManager;
import pt.ulisboa.tecnico.hdscoin.Crypto.Message;
import pt.ulisboa.tecnico.hdscoin.interfaces.KeystoreManager;
import pt.ulisboa.tecnico.hdscoin.interfaces.RemoteServerInterface;
import pt.ulisboa.tecnico.hdscoin.interfaces.Transaction;

import java.io.File;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;




public class Client {

	private static final int ATTEMPT=1;
    private List<Transaction> pendingTransaction;
    
    private CryptoManager manager;
    private Registry registry;
    
 	private KeystoreManager keyPairManager;
 	private KeyPair clientKeyPair;
 	private String clientName;
    private PublicKey serverPublicKey;
    
    private LastSentMessage lastSentMessage;
    private List<RemoteServerInterface> servers;
    //private HashMap<RemoteServerInterface, PublicKey> servers;
    
	private String host;
	public Client(String host, String clientName, String password) throws RemoteException, NotBoundException, MalformedURLException{
		this.host=host;
		this.clientName=clientName.toLowerCase().trim();
		servers = new ArrayList<RemoteServerInterface>();
		//servers = new HashMap<RemoteServerInterface, PublicKey>();
		connect();
		try {
			keyPairManager=new KeystoreManager("/"+clientName.trim().toLowerCase()+".jks", password);
			clientKeyPair=keyPairManager.getKeyPair(clientName.trim().toLowerCase(), password);
			manager = new CryptoManager(clientKeyPair.getPublic(), clientKeyPair.getPrivate(), keyPairManager);
		}catch(Exception e) {
			System.out.println("KeyPair Error");
			e.printStackTrace();
		}
		lastSentMessage=new LastSentMessage();
		pendingTransaction=new ArrayList<Transaction>();
		System.out.println("Welcome "+clientName+"!");
	}
	
	public int numServers(){
		return servers.size();
	}
	
	private void connect() throws RemoteException, NotBoundException, MalformedURLException{
		if(host==null){
			int numS = LocateRegistry.getRegistry(8000).list().length;
			for(int i = 0; i < numS; i++)
				servers.add((RemoteServerInterface) Naming.lookup(new String ("//localhost:8000/"+"RemoteServerInterface" + (i + 1))));
				//servers.put((RemoteServerInterface) Naming.lookup(new String ("//localhost:8000/"+"RemoteServerInterface" + (i + 1))), manager.getPublicKeyBy("Server"+(i + 1)));
		}
		else{
			int numS = LocateRegistry.getRegistry(8000).list().length;
			for(int i = 0; i < numS; i++)
				servers.add((RemoteServerInterface) Naming.lookup(new String ("//" + host + ":8000/"+"RemoteServerInterface" + (i + 1))));
		}
	}

	public boolean register() {
		int tentries=0;
		boolean success=false;
		for(int i = 0; i < numServers()&&tentries<ATTEMPT; i++) {
			success=false;
			tentries=0;
			while(!success&&tentries<ATTEMPT){
				try {
					servers.get(i).register(clientName, manager.getPublicKey());
					//TODOSERVERKEY using only one key
					try {
						serverPublicKey=manager.getPublicKeyBy("server1");
					} catch (Exception e) {
						System.out.println("publickey error");
						e.printStackTrace();
					}
		            
		            success= true;
		            continue;
		        } catch (RemoteException e) {
		        	System.out.println("Connection fail...");
		        	tentries++;
		        }
			}
		}
		if(success)
			System.out.println("You are registered!");
		return success;
		
		
	}
	public synchronized boolean reSend(){
		
		int tentries=0;
		boolean success=false;
		CipheredMessage cipheredMessage=lastSentMessage.readLastSentMessage(clientName);
		for(int i = 0; i < numServers(); i++) {
			tentries=0;
			success=false;
			while(!success&&tentries<ATTEMPT){
	        	try{
	        		
		    		CipheredMessage response = servers.get(i).send(cipheredMessage);
		
		            Message responseDeciphered = manager.decipherCipheredMessage(response);
		
		            System.out.println("Success from server " + (i+1) + ": " + responseDeciphered.isConfirm());
		            lastSentMessage.removeLastSentMessage(clientName);
		            success= true;
		            continue;
	            } catch (RemoteException e) {
		        	System.out.println("Connection fail...");
		        	tentries++;
		        }catch(IllegalStateException e){
		        	System.out.println("The message is already sent");
		        	success= true;
		            continue;
		        }
			}
        }
        return success;
	}
	public synchronized boolean send(String sendDestination, String sendAmount) {
		//just in case
		if(lastSentMessage.checkFileExists(clientName)){
			return reSend();
		}
		
		int tentries=0;
		boolean success=false;
		try {
            Message msg = new Message(Double.parseDouble(sendAmount.trim()), manager.getPublicKey(), keyPairManager.getPublicKeyByName(sendDestination)); //SERVER_key represents sendDestination
            if(serverPublicKey==null)
            	System.out.println("ServerKey is null");
            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serverPublicKey);
            List<Boolean> awnsers = new ArrayList<Boolean>();
            
            for(int i = 0; i < numServers()&&tentries<ATTEMPT; i++) {
            	
            	tentries=0;
        		success=false;
	            while(!success&&tentries<ATTEMPT){
	            	try{
	            		
		        		CipheredMessage response = servers.get(i).send(cipheredMessage);
			
			            Message responseDeciphered = manager.decipherCipheredMessage(response);
			            awnsers.add(responseDeciphered.isConfirm());
			            System.out.println("Success from server " + (i+1) + ": " + responseDeciphered.isConfirm());
			            success= true;
			            continue;
		            } catch (RemoteException e) {
			        	System.out.println("Connection fail...");
			        	tentries++;
			        }catch(IllegalStateException e){
			        	System.out.println("Invalid signature");
			        	success= true;
			            continue;
			        }
	            }
            }
            Collection<Boolean> collection = awnsers;
            if(Collections.frequency(collection, collection.iterator().next()) != collection.size() && awnsers.size()==7)//implementar modelo de faltas
            	lastSentMessage.writeLastSentMessage(clientName, cipheredMessage);
            return success;
		} catch(Exception e){
        	System.out.println("Invalid message");
        	return false;
        }
		
		
	}
	public boolean check(String sendDestination) {
		boolean success=false;
		int tentries=0;
		String checkedName="";
		Map<Integer, List<Transaction>> transactions = new HashMap<Integer,List<Transaction>>();
		Map<Integer, Double> amounts = new HashMap<Integer,Double>();
		try {
            Message msg = new Message(manager.getPublicKey(), keyPairManager.getPublicKeyByName(sendDestination));
            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serverPublicKey);
            for(int i = 0; i < numServers()&&tentries<ATTEMPT; i++) {
            	success=false;
        		tentries=0;
	            while(!success&&tentries<ATTEMPT){
	            	try{
	            		CipheredMessage response = servers.get(i).check(cipheredMessage);
	            		Message responseDeciphered = manager.decipherCipheredMessage(response);
	            		checkedName=responseDeciphered.getCheckedName();
	            		
	            		amounts.put(i, responseDeciphered.getAmount());
	            		if(responseDeciphered.getTransactions()!=null) {
	    		            success=true;
	    		            pendingTransaction=new ArrayList<Transaction>();
	    		            for(Transaction t:responseDeciphered.getTransactions()) 
	    		            	pendingTransaction.add(t);
	    		            if(pendingTransaction.size()!=0)
	    		            	transactions.put(i, pendingTransaction);
	    		            	
	    	            }
	    	            success=true;
	            	} catch (RemoteException e) {
	    	        	System.out.println("Connection fail...");
	    	        	tentries++;
	    	        } catch(IllegalStateException e){
	    	        	System.out.println("Invalid signature");
	    	        	success=true;
	    	        }
	            }
            }
            Collection<Double> amount = amounts.values();
            if(Collections.frequency(amount, amount.iterator().next()) == amount.size())
            	System.out.println(checkedName + "'s balance is: " + amount.iterator().next());
            //else{ implementar modelo de faltas
			//}
            if(transactions.size()==0)
        		System.out.println(checkedName+" has no pending transfer...");
            else if(transactions.size()!=7)
            	//or all responded or none 
        		return false;
            else {
            	//Check if all are the same/modelo de faltas
	            System.out.println(checkedName+"'s pending transfer(s) are:");
	            int id=0;
	            for(Transaction t:pendingTransaction) {
	            	id++;
	            	System.out.println("id "+id+": \t"+t.toString());
	            }
            }
        } catch(Exception e){
        	System.out.println("Invalid message");
        	return false;
        }
	
	return success;
}
	            		
            

	
	public boolean receive(int receivedPendingTransfers) {
		
		if(pendingTransaction.size()==0){
			System.out.println("You do not have any pending transaction. Make a check first...");
			return true;
		}
		
		List<Boolean> awnsers = new ArrayList<Boolean>();
		int tentries=0;
		boolean success=false;
		
		try {
			int index=receivedPendingTransfers-1;
			Transaction receiveTransaction=pendingTransaction.get(index);
            
            Message msg = new Message(manager.getPublicKey(), receiveTransaction);
            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serverPublicKey);
            
            for(int i = 0; i < numServers()&&tentries<ATTEMPT; i++) {
            	tentries=0;
        		success=false;
	            while(!success&&tentries<ATTEMPT){
	            	try{
			            CipheredMessage response = servers.get(i).receive(cipheredMessage);
	
			            Message responseDeciphered = manager.decipherCipheredMessage(response);
			            awnsers.add(responseDeciphered.isConfirm());
			            System.out.println("Success from server " + (i+1) + ": " + responseDeciphered.isConfirm());
			            success= true;
			            continue;
	            	} catch (RemoteException e) {
	    	        	System.out.println("Connection fail...");
	    	        	tentries++;
	    	        } catch(IllegalStateException e){
	    	        	System.out.println("Illegal State Exception Invalid signature");
	    	        	success= true;
			            continue;
	    	        }
	            }
            }
        } catch (Exception e) {
        	System.out.println("Invalid message");
        	return false;
        }
		Collection<Boolean> collection = awnsers;
        if(Collections.frequency(collection, collection.iterator().next()) != collection.size() && awnsers.size()==7)//implementar modelo de faltas
        	System.out.println("bad receive");
		return success;
	}
	public boolean audit(String sendDestination) {
		int tentries=0;
		boolean success=false;
		String name ="";
		Map<Integer, List<Transaction>> transactions = new HashMap<Integer,List<Transaction>>();
		Map<Integer, Integer> transfers = new HashMap<Integer,Integer>();
		try {
            Message msg = new Message(manager.getPublicKey(), keyPairManager.getPublicKeyByName(sendDestination));
            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serverPublicKey);
            
            for(int i = 0; i < numServers()&&tentries<ATTEMPT; i++) {
            	tentries=0;
        		success=false;
	            while(!success&&tentries<ATTEMPT){
	            	try{
			            CipheredMessage response = servers.get(i).audit(cipheredMessage);
			
			            Message responseDeciphered = manager.decipherCipheredMessage(response);
			            name=responseDeciphered.getCheckedName();
			            if(responseDeciphered.getTransactions()!=null) {
			            	transfers.put(i, responseDeciphered.getTransactions().size());
			            	if(responseDeciphered.getTransactions().size()!=0) {
			            		transactions.put(i, responseDeciphered.getTransactions());	
			            	}
				            
			            }
			            success=true;
			            continue;
	            	} catch (RemoteException e) {
	    	        	System.out.println("Connection fail...");
	    	        	tentries++;
	    	        } catch(IllegalStateException e){
	    	        	System.out.println("Invalid signature");
	    	        	success=true;
			            continue;
	    	        }
	            }
            }
        } catch (Exception e) {
        	System.out.println("Invalid message");
        	return false;
        }
		Collection<Integer> transfer = transfers.values();
        if(Collections.frequency(transfer, transfer.iterator().next()) == transfer.size() && transfer.size()==7) {//implementar modelo de faltas
        	if(transfers.get(0)==0)
        		System.out.println(name+" does not have done any transfer...");
        	else {
        		System.out.println(name+"'s transfer history:");
                for(Transaction s: transactions.get(0)) { //implementar modelo de faltas
                	System.out.println(s.toString());
                }
        	}
        }
		
		return success;
	}
	
	public String getClientName() {
		return clientName;
	}
	
	public boolean clientHasMessageNotSent(){
		if(lastSentMessage.checkFileExists(clientName)){
			return true;
		}
		return false;
	}
	
	public void removePendingTransaction(){
		pendingTransaction=new ArrayList<Transaction>();
	}
}
