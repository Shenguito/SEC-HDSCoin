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
import java.util.List;

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
		for(int i = 0; i < numServers(); i++) {
			boolean success=false;
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
		            System.out.println("You are registered!");
		            success= true;
		            continue;
		        } catch (RemoteException e) {
		        	System.out.println("Connection fail...");
		        	tentries++;
		        }
			}
		}
		return true;
		
	}
	public synchronized boolean reSend(){
		
		int tentries=0;
		CipheredMessage cipheredMessage=lastSentMessage.readLastSentMessage(clientName);
		for(int i = 0; i < numServers(); i++) {
			boolean success=false;
			while(!success&&tentries<ATTEMPT){
	        	try{
	        		
		    		CipheredMessage response = servers.get(i).send(cipheredMessage);
		
		            Message responseDeciphered = manager.decipherCipheredMessage(response);
		
		            System.out.println("Success: " + responseDeciphered.isConfirm());
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
        return false;
	}
	public synchronized boolean send(String sendDestination, String sendAmount) {
		//just in case
		if(lastSentMessage.checkFileExists(clientName)){
			return reSend();
		}
		
		int tentries=0;
		try {
            Message msg = new Message(Double.parseDouble(sendAmount.trim()), manager.getPublicKey(), keyPairManager.getPublicKeyByName(sendDestination)); //SERVER_key represents sendDestination
            if(serverPublicKey==null)
            	System.out.println("ServerKey is null");
            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serverPublicKey);
            
            for(int i = 0; i < numServers()&&tentries<ATTEMPT; i++) {
            	boolean success=false;
	            while(!success&&tentries<ATTEMPT){
	            	try{
	            		
		        		CipheredMessage response = servers.get(i).send(cipheredMessage);
			
			            Message responseDeciphered = manager.decipherCipheredMessage(response);
			
			            System.out.println("Success: " + responseDeciphered.isConfirm());
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
            lastSentMessage.writeLastSentMessage(clientName, cipheredMessage);
            return false;
		} catch(Exception e){
        	System.out.println("Invalid message");
        	return false;
        }
		
		
	}
	public boolean check(String sendDestination) {
		
		int tentries=0;
			try {
	            Message msg = new Message(manager.getPublicKey(), keyPairManager.getPublicKeyByName(sendDestination));
	            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serverPublicKey);
	            
	            for(int i = 0; i < numServers()&&tentries<ATTEMPT; i++) {
	            	boolean success=false;
		            while(!success&&tentries<ATTEMPT){
		            	try{
		            		CipheredMessage response = servers.get(i).check(cipheredMessage);
		            		Message responseDeciphered = manager.decipherCipheredMessage(response);
		            		if(responseDeciphered.getCheckedName().equals(null)){
		            			//TODO case user does not exists
		            			System.out.println("User does not exists....");
		            			success=true;
		            			continue;
		            		}
		    	            System.out.println(responseDeciphered.getCheckedName() + "'s balance is: " + responseDeciphered.getAmount());
		    	            if(responseDeciphered.getTransactions()!=null) {
		    	            	if(responseDeciphered.getTransactions().size()==0) {
		    	            		System.out.println(responseDeciphered.getCheckedName()+" has no pending transfer...");
		    	            		success = true;
		    	            		continue;
		    	            	}
		    		            System.out.println(responseDeciphered.getCheckedName()+"'s pending transfer(s) are:");
		    		            pendingTransaction=new ArrayList<Transaction>();
		    		            int id=0;
		    		            for(Transaction t:responseDeciphered.getTransactions()) {
		    		            	pendingTransaction.add(t);
		    		            	id++;
		    		            	System.out.println("id "+id+": \t"+t.toString());
		    		            }
		    	            }
		    	            success= true;
				            continue;
		            	} catch (RemoteException e) {
		    	        	System.out.println("Connection fail...");
		    	        	tentries++;
		    	        } catch(IllegalStateException e){
		    	        	System.out.println("Invalid signature");
		    	        	success= true;
				            continue;
		    	        }
		            }
	            }
	        } catch(Exception e){
	        	System.out.println("Invalid message");
	        	return false;
	        }
		return false;
	}
	
	public boolean receive(int receivedPendingTransfers) {
		
		if(pendingTransaction.size()==0){
			System.out.println("You do not have any pending transaction. Make a check first...");
			return true;
		}
		
		
		int tentries=0;
		
		try {
			int index=receivedPendingTransfers-1;
			Transaction receiveTransaction=pendingTransaction.get(index);
            
            Message msg = new Message(manager.getPublicKey(), receiveTransaction);
            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serverPublicKey);
            
            for(int i = 0; i < numServers(); i++) {
            	boolean success=false;
	            while(!success&&tentries<ATTEMPT){
	            	try{
			            CipheredMessage response = servers.get(i).receive(cipheredMessage);
	
			            Message responseDeciphered = manager.decipherCipheredMessage(response);
			            System.out.println("Success: " + responseDeciphered.isConfirm());
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
		return false;
	}
	public boolean audit(String sendDestination) {
		int tentries=0;
		try {
            Message msg = new Message(manager.getPublicKey(), keyPairManager.getPublicKeyByName(sendDestination));
            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serverPublicKey);
            
            for(int i = 0; i < numServers(); i++) {
            	boolean success=false;
	            while(!success&&tentries<ATTEMPT){
	            	try{
			            CipheredMessage response = servers.get(i).audit(cipheredMessage);
			
			            Message responseDeciphered = manager.decipherCipheredMessage(response);
			            if(responseDeciphered.getTransactions()!=null) {
			            	if(responseDeciphered.getTransactions().size()==0) {
			            		System.out.println(responseDeciphered.getCheckedName()+" does not have done any transfer...");
			            	}
				            System.out.println(responseDeciphered.getCheckedName()+"'s transfer history:");
				            for(Transaction s:responseDeciphered.getTransactions()) {
				            	System.out.println(s.toString());
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
		return false;
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
