package pt.ulisboa.tecnico.hdscoin.server;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.File;
import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import pt.ulisboa.tecnico.hdscoin.crypto.CipheredMessage;
import pt.ulisboa.tecnico.hdscoin.crypto.CryptoManager;
import pt.ulisboa.tecnico.hdscoin.crypto.Message;
import pt.ulisboa.tecnico.hdscoin.interfaces.*;
import pt.ulisboa.tecnico.hdscoin.server.storage.*;



public class Server implements RemoteServerInterface{

	
	private Storage storage;
 	private KeystoreManager keyPairManager;
 	private KeyPair serverKeyPair;
 	private CryptoManager manager;
 	
 	private boolean crashFailure;
 	
 	private ConcurrentHashMap<PublicKey, String> clients;
 	
	public Server() throws RemoteException, AlreadyBoundException {
		
		connect();
		
		storage=new Storage();
		try {
			clients=storage.getClients();
			for(String s:clients.values())
				System.out.println("Client already registered: "+s);
		} catch (JsonParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (JsonMappingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InvalidKeySpecException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			keyPairManager=new KeystoreManager("KeyStore"+File.separator+"server.jks", "server123");
			serverKeyPair=keyPairManager.getKeyPair("server", "server123");
			manager = new CryptoManager(serverKeyPair.getPublic(), serverKeyPair.getPrivate(), keyPairManager);
		}catch(Exception e) {
			System.out.println("KeyPair Error");
			e.printStackTrace();
		}
		crashFailure=false;
		
	}
	private void connect() throws RemoteException, AlreadyBoundException{
		System.setProperty("java.rmi.server.hostname","127.0.0.1");
        
        
        RemoteServerInterface stub = (RemoteServerInterface) UnicastRemoteObject.exportObject(this, 0);
        Registry registry = LocateRegistry.createRegistry(1099);
        // Bind the remote object's stub in the registry
        registry.bind("RemoteServerInterface", stub);

        System.out.println("ServerInterface ready");
        
	}
	
	public PublicKey register(String clientName, PublicKey publickey) throws RemoteException {
		
		if(isServerCrashed())
			throw new RemoteException();
		
		if(!storage.checkFileExists(clientName)){
			storage.writeClient(clientName, new Ledger(publickey, 100, new ArrayList<Transaction>(), new ArrayList<Transaction>()));
		}else {
			System.out.println("User already registered!");
		}
		if(!clients.containsKey(clientName)){
			clients.put(publickey, clientName);
			System.out.println("Test-> reading "+clientName+" file:\n"+storage.readClient(clientName).toString());
		}
		
		
		return manager.getPublicKey();
	}
	
	//PublicKey source, PublicKey destination, int amount
	public synchronized CipheredMessage send(CipheredMessage msg) throws RemoteException {
		
		if(isServerCrashed())
			throw new RemoteException();

		System.out.println("Deciphering message");
		Message decipheredMessage = manager.decipherCipheredMessage(msg);
		
		Message message = new Message(serverKeyPair.getPublic(), false);//case the client does not exist
		if(storage.checkFileExists(clients.get(decipheredMessage.getSender()))){
			Ledger sender = storage.readClient(clients.get(decipheredMessage.getSender()));
			if(sender.sendBalance(decipheredMessage.getAmount())) {
				Ledger destiny = storage.readClient(clients.get(decipheredMessage.getDestination()));
				//Transaction with id for pending
				destiny.addPendingTransfers(new Transaction(clients.get(decipheredMessage.getSender()),clients.get(decipheredMessage.getDestination()),decipheredMessage.getAmount()));
				storage.writeClient(clients.get(decipheredMessage.getDestination()), destiny);
				storage.writeClient(clients.get(decipheredMessage.getSender()), sender);
				message = new Message(serverKeyPair.getPublic(), true);
			}
		}
		
		CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, decipheredMessage.getSender());
		return cipheredMessage;
		
	}


	public CipheredMessage check(CipheredMessage msg) throws RemoteException {

		if(isServerCrashed())
			throw new RemoteException();
		
		Message decipheredMessage = manager.decipherCipheredMessage(msg);
		
		Message message = new Message(manager.getPublicKey(), 0.0, new ArrayList<Transaction>(), clients.get(decipheredMessage.getDestination())); //case the client does not exist
		if(storage.checkFileExists(clients.get(decipheredMessage.getDestination()))){
			Ledger value = storage.readClient(clients.get(decipheredMessage.getDestination()));
			if(decipheredMessage.getDestination().equals(decipheredMessage.getSender()))
				message = new Message(manager.getPublicKey(), value.getBalance(), value.getPendingTransfers(), clients.get(decipheredMessage.getDestination()));
			else
				message = new Message(manager.getPublicKey(), value.getBalance(), null, clients.get(decipheredMessage.getDestination()));
		}
		CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, decipheredMessage.getSender());
		return cipheredMessage;
	}


	public synchronized CipheredMessage receive(CipheredMessage msg) throws RemoteException {

		if(isServerCrashed())
			throw new RemoteException();
		
		Message decipheredMessage = manager.decipherCipheredMessage(msg);
		
		Message message = new Message(serverKeyPair.getPublic(), false);
		
		//decipheredMessage.getDestination()==null
		//System.out.println("Test1: "+clients.get(decipheredMessage.getSender()));
		Ledger destiny = storage.readClient(clients.get(decipheredMessage.getSender()));
		
		
		Iterator<Transaction> i = destiny.getPendingTransfers().iterator();
		while (i.hasNext()) {
			Transaction t=i.next();
			if(decipheredMessage.getTransaction().myEquals(t)){
				Ledger sender = storage.readClient(t.getSender());
				destiny.receiveBalance(t.getAmount());
				destiny.addTransfers(new Transaction(t.getSender(), t.getReceiver(), t.getAmount()));
				sender.addTransfers(new Transaction(t.getSender(), t.getReceiver(), t.getAmount()));
				
				//Write to file BUG
				i.remove();
				storage.writeClient(t.getSender(), sender);
				storage.writeClient(t.getReceiver(), destiny);
				break;
			}
		}
		
		message = new Message(serverKeyPair.getPublic(), true);
		CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, decipheredMessage.getSender());
		return cipheredMessage;
	}

	public CipheredMessage audit(CipheredMessage msg) throws RemoteException {

		if(isServerCrashed())
			throw new RemoteException();
		
		Message decipheredMessage = manager.decipherCipheredMessage(msg);
		
		
		Ledger value = storage.readClient(clients.get(decipheredMessage.getDestination()));
		Message message = new Message(manager.getPublicKey(), value.getBalance(), value.getTransfers(), clients.get(decipheredMessage.getDestination()));
		CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, decipheredMessage.getSender());
		return cipheredMessage;
	}
	
	public void setServerFault(boolean crash){
		crashFailure=crash;
	}
	
	private boolean isServerCrashed(){
		return crashFailure;
	}
	
	private boolean discardMessage(){
		if(1==(new Random().nextInt(1 - 0 + 1) + 0)){
			return false;
		}
		return true;
	}
	    
	
}
