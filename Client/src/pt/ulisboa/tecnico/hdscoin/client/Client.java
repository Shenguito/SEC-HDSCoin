package pt.ulisboa.tecnico.hdscoin.client;

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

import pt.ulisboa.tecnico.hdscoin.crypto.CipheredMessage;
import pt.ulisboa.tecnico.hdscoin.crypto.CryptoManager;
import pt.ulisboa.tecnico.hdscoin.crypto.Message;
import pt.ulisboa.tecnico.hdscoin.interfaces.KeystoreManager;
import pt.ulisboa.tecnico.hdscoin.interfaces.RemoteServerInterface;
import pt.ulisboa.tecnico.hdscoin.interfaces.Transaction;

public class Client {

	
    private List<Transaction> pendingTransaction;
    
    private CryptoManager manager;
    private Registry registry;
    private RemoteServerInterface serverInterfaces;
    
 	private KeystoreManager keyPairManager;
 	private KeyPair clientKeyPair;
 	private String clientName;
    private PublicKey serverPublicKey;
    
	private String host;
	public Client(String host, String clientName) throws RemoteException, NotBoundException{
		this.host=host;
		registry = LocateRegistry.getRegistry(host);
		serverInterfaces = (RemoteServerInterface) registry.lookup("RemoteServerInterface");
		this.clientName=clientName;
		try {
			keyPairManager=new KeystoreManager("KeyStore"+File.separator+clientName.trim().toLowerCase()+".jks", clientName.trim().toLowerCase()+"123");
			clientKeyPair=keyPairManager.getKeyPair(clientName.trim().toLowerCase(), clientName.trim().toLowerCase()+"123");
			manager = new CryptoManager(clientKeyPair.getPublic(), clientKeyPair.getPrivate(), keyPairManager);
		}catch(Exception e) {
			System.out.println("KeyPair Error");
			e.printStackTrace();
		}
		
		pendingTransaction=new ArrayList<Transaction>();
		System.out.println("Welcome "+clientName+"!");
	}

	public void register() {
		
		try {
			serverPublicKey=serverInterfaces.register(clientName, manager.getPublicKey());
			if(serverPublicKey==null)
            	System.out.println("ServerKey is null");
            System.out.println("You are registered!");
            keyPairManager.getPublicKeyByName("bob");
    		
        } catch (Exception e) {
        }
		
	}
	public void send(String sendDestination, String sendAmount) {
		try {
            Message msg = new Message(Double.parseDouble(sendAmount.trim()), manager.getPublicKey(), keyPairManager.getPublicKeyByName(sendDestination)); //SERVER_key represents sendDestination
            if(serverPublicKey==null)
            	System.out.println("ServerKey is null");
            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serverPublicKey);

            CipheredMessage response = serverInterfaces.send(cipheredMessage);

            Message responseDeciphered = manager.decipherCipheredMessage(response);

            System.out.println("Success: " + responseDeciphered.isConfirm());
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
            System.out.println("Unsuccess!");
        }
	}
	public void check() {
		
		try {
            
            Message msg = new Message(manager.getPublicKey());
            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serverPublicKey);

            CipheredMessage response = serverInterfaces.check(cipheredMessage);

            Message responseDeciphered = manager.decipherCipheredMessage(response);

            System.out.println("You balance is: " + responseDeciphered.getAmount());
            if(responseDeciphered.getTransactions()!=null) {
            	if(responseDeciphered.getTransactions().size()==0) {
            		System.out.println("You do not have any pending transfer...");
            		return;
            	}
	            System.out.println("Your pending transfer(s) are:");
	            pendingTransaction=new ArrayList<Transaction>();
	            for(Transaction t:responseDeciphered.getTransactions()) {
	            	pendingTransaction.add(t);
	            	System.out.println(t.toString());
	            }
            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
	}
	
	public void receive(String receivedPendingTransfers) {
		try {
            
            List <Transaction> pendingTransfers=new ArrayList<Transaction>();
            try {
            	if(receivedPendingTransfers.split(" ").length==0)
            		pendingTransfers.add(pendingTransaction.get(0));
        		else
		            for(String id:receivedPendingTransfers.split(" ")) {
		            	pendingTransfers.add(pendingTransaction.get(Integer.parseInt(id.trim())-1));
		            }
            }catch(Exception e) {
            	System.out.println("Input error... Try again! You input was '"+receivedPendingTransfers+"'!");
            	return;
            }
            
            Message msg = new Message(manager.getPublicKey(), pendingTransfers);
            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serverPublicKey);

            CipheredMessage response = serverInterfaces.receive(cipheredMessage);

            Message responseDeciphered = manager.decipherCipheredMessage(response);

            System.out.println("Success: " + responseDeciphered.isConfirm());
            pendingTransaction=new ArrayList<Transaction>();
            
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
	}
	public void audit() {
		try {
            
            Message msg = new Message(manager.getPublicKey());
            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serverPublicKey);

            CipheredMessage response = serverInterfaces.audit(cipheredMessage);

            Message responseDeciphered = manager.decipherCipheredMessage(response);
            if(responseDeciphered.getTransactions()!=null) {
            	if(responseDeciphered.getTransactions().size()==0) {
            		System.out.println("You do not have done any transfer...");
            	}
	            System.out.println("Your transfer history:");
	            for(Transaction s:responseDeciphered.getTransactions()) {
	            	System.out.println(s.toString());
	            }
            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
	}
	
	public String getClientName() {
		return clientName;
	}
}
