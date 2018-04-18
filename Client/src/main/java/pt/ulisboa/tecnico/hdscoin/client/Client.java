package pt.ulisboa.tecnico.hdscoin.client;



import pt.ulisboa.tecnico.hdscoin.Crypto.CipheredMessage;
import pt.ulisboa.tecnico.hdscoin.Crypto.CryptoManager;
import pt.ulisboa.tecnico.hdscoin.Crypto.Message;
import pt.ulisboa.tecnico.hdscoin.interfaces.KeystoreManager;
import pt.ulisboa.tecnico.hdscoin.interfaces.RemoteServerInterface;
import pt.ulisboa.tecnico.hdscoin.interfaces.Transaction;

import java.io.File;
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
    private RemoteServerInterface serverInterfaces;
    
 	private KeystoreManager keyPairManager;
 	private KeyPair clientKeyPair;
 	private String clientName;
    private PublicKey serverPublicKey;
    
    private LastSentMessage lastSentMessage;
    
	private String host;
	public Client(String host, String clientName, String password) throws RemoteException, NotBoundException{
		this.host=host;
		this.clientName=clientName.toLowerCase().trim();
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
	
	private void connect() throws RemoteException, NotBoundException{
		registry = LocateRegistry.getRegistry(host);
		serverInterfaces = (RemoteServerInterface) registry.lookup("RemoteServerInterface");
	}

	public boolean register() {
		boolean success=false;
		int tentries=0;
		while(!success&&tentries<ATTEMPT){
			try {
				serverPublicKey=serverInterfaces.register(clientName, manager.getPublicKey());
				if(serverPublicKey==null)
	            	System.out.println("ServerKey is null");
	            System.out.println("You are registered!");
	            return true;
	        } catch (RemoteException e) {
	        	System.out.println("Connection fail...");
	        	tentries++;
	        }
		}
		return false;
		
	}
	public synchronized boolean reSend(){
		boolean success=false;
		int tentries=0;
		CipheredMessage cipheredMessage=lastSentMessage.readLastSentMessage(clientName);
		
		while(!success&&tentries<ATTEMPT){
        	try{
        		
	    		CipheredMessage response = serverInterfaces.send(cipheredMessage);
	
	            Message responseDeciphered = manager.decipherCipheredMessage(response);
	
	            System.out.println("Success: " + responseDeciphered.isConfirm());
	            lastSentMessage.removeLastSentMessage(clientName);
	            return true;
            } catch (RemoteException e) {
	        	System.out.println("Connection fail...");
	        	tentries++;
	        }catch(IllegalStateException e){
	        	System.out.println("The message is already sent");
	        	return true;
	        }
        }
        return false;
	}
	public synchronized boolean send(String sendDestination, String sendAmount) {
		//just in case
		if(lastSentMessage.checkFileExists(clientName)){
			return reSend();
		}
		boolean success=false;
		int tentries=0;
		
		try {
            Message msg = new Message(Double.parseDouble(sendAmount.trim()), manager.getPublicKey(), keyPairManager.getPublicKeyByName(sendDestination)); //SERVER_key represents sendDestination
            if(serverPublicKey==null)
            	System.out.println("ServerKey is null");
            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serverPublicKey);
            while(!success&&tentries<ATTEMPT){
            	try{
            		
	        		CipheredMessage response = serverInterfaces.send(cipheredMessage);
		
		            Message responseDeciphered = manager.decipherCipheredMessage(response);
		
		            System.out.println("Success: " + responseDeciphered.isConfirm());
		            return true;
	            } catch (RemoteException e) {
		        	System.out.println("Connection fail...");
		        	tentries++;
		        }catch(IllegalStateException e){
		        	System.out.println("Invalid signature");
		        	return true;
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
		boolean success=false;
		int tentries=0;
			try {
	            Message msg = new Message(manager.getPublicKey(), keyPairManager.getPublicKeyByName(sendDestination));
	            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serverPublicKey);
	            while(!success&&tentries<ATTEMPT){
	            	try{
	            		CipheredMessage response = serverInterfaces.check(cipheredMessage);
	            		Message responseDeciphered = manager.decipherCipheredMessage(response);
	            		if(responseDeciphered.getCheckedName().equals(null)){
	            			return false;
	            		}
	    	            System.out.println(responseDeciphered.getCheckedName() + "'s balance is: " + responseDeciphered.getAmount());
	    	            if(responseDeciphered.getTransactions()!=null) {
	    	            	if(responseDeciphered.getTransactions().size()==0) {
	    	            		System.out.println(responseDeciphered.getCheckedName()+" has no pending transfer...");
	    	            		return true;
	    	            	}
	    		            System.out.println(responseDeciphered.getCheckedName()+"'s pending transfer(s) are:");
	    		            pendingTransaction=new ArrayList<Transaction>();
	    		            int id=0;
	    		            for(Transaction t:responseDeciphered.getTransactions()) {
	    		            	pendingTransaction.add(t);
	    		            	id++;
	    		            	System.out.println("id "+id+": \t"+t.toString());
	    		            }
	    		            success=true;
	    		            return true;
	    	            }
	    	            success=true;
	    	            return true;
	            	} catch (RemoteException e) {
	    	        	System.out.println("Connection fail...");
	    	        	tentries++;
	    	        } catch(IllegalStateException e){
	    	        	System.out.println("Invalid signature");
	    	        	return true;
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
		
		boolean success=false;
		int tentries=0;
		
		try {
			int index=receivedPendingTransfers-1;
			Transaction receiveTransaction=pendingTransaction.get(index);
            
            Message msg = new Message(manager.getPublicKey(), receiveTransaction);
            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serverPublicKey);
            while(!success&&tentries<ATTEMPT){
            	try{
		            CipheredMessage response = serverInterfaces.receive(cipheredMessage);

		            Message responseDeciphered = manager.decipherCipheredMessage(response);
		            System.out.println("Success: " + responseDeciphered.isConfirm());
		            return true;
            	} catch (RemoteException e) {
    	        	System.out.println("Connection fail...");
    	        	tentries++;
    	        } catch(IllegalStateException e){
    	        	System.out.println("Illegal State Exception Invalid signature");
    	        	return true;
    	        }
            }
        } catch (Exception e) {
        	System.out.println("Invalid message");
        	return false;
        }
		return false;
	}
	public boolean audit(String sendDestination) {
		boolean success=false;
		int tentries=0;
		try {
            Message msg = new Message(manager.getPublicKey(), keyPairManager.getPublicKeyByName(sendDestination));
            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serverPublicKey);
            while(!success&&tentries<ATTEMPT){
            	try{
		            CipheredMessage response = serverInterfaces.audit(cipheredMessage);
		
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
		            return true;
            	} catch (RemoteException e) {
    	        	System.out.println("Connection fail...");
    	        	tentries++;
    	        } catch(IllegalStateException e){
    	        	System.out.println("Invalid signature");
    	        	return true;
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
