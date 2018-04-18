package pt.ulisboa.tecnico.hdscoin.server;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import pt.ulisboa.tecnico.hdscoin.Crypto.CipheredMessage;
import pt.ulisboa.tecnico.hdscoin.Crypto.CryptoManager;
import pt.ulisboa.tecnico.hdscoin.Crypto.Message;
import pt.ulisboa.tecnico.hdscoin.interfaces.KeystoreManager;
import pt.ulisboa.tecnico.hdscoin.interfaces.RemoteServerInterface;
import pt.ulisboa.tecnico.hdscoin.interfaces.Transaction;
import pt.ulisboa.tecnico.hdscoin.server.storage.Ledger;
import pt.ulisboa.tecnico.hdscoin.server.storage.Storage;

import java.net.URISyntaxException;
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




public class Server implements RemoteServerInterface{

	
	private Storage storage;
 	private KeystoreManager keyPairManager;
 	private KeyPair serverKeyPair;
 	private CryptoManager manager;
 	private int numInterface;
 	
 	private boolean crashFailure;
 	
 	private ConcurrentHashMap<PublicKey, String> clients;
 	
	public Server() throws RemoteException, AlreadyBoundException {
		check();
		connect();
		

		try {
			keyPairManager=new KeystoreManager("/server.jks", "server123");
			serverKeyPair=keyPairManager.getKeyPair("server", "server123");
			manager = new CryptoManager(serverKeyPair.getPublic(), serverKeyPair.getPrivate(), keyPairManager);
		}catch(Exception e) {
			System.out.println("KeyPair Error");
			e.printStackTrace();
		}
		crashFailure=false;

	}
	private void check(){
		storage=new Storage();
		storage.backupCheck();
		try {
			clients=storage.getClients();
			for(String s:clients.values())
				System.out.println("Client already registered: "+s);
		} catch (JsonParseException e1) {

			e1.printStackTrace();
		} catch (JsonMappingException e1) {

			e1.printStackTrace();
		} catch (NoSuchAlgorithmException e1) {

			e1.printStackTrace();
		} catch (InvalidKeySpecException e1) {

			e1.printStackTrace();
		} catch (IOException e1) {

			e1.printStackTrace();
		}
		
	}

	private void connect() throws RemoteException, AlreadyBoundException{
		System.setProperty("java.rmi.server.hostname","127.0.0.1");
		RemoteServerInterface stub;
		Registry registry;
		int RealNumS = 0;
        try{
        	RealNumS = LocateRegistry.getRegistry(8000).list().length;
        }catch(RemoteException e){
        	stub = (RemoteServerInterface) UnicastRemoteObject.exportObject(this, 0);
    	    registry = LocateRegistry.createRegistry(8000);
    	        
        	registry.bind("RemoteServerInterface1", stub);
        	numInterface = 1;
        	System.out.println("ServerInterface ready");
        	return;
        }
        System.out.println(RealNumS);
        	stub = (RemoteServerInterface) UnicastRemoteObject.exportObject(this, 0);
    	    registry = LocateRegistry.getRegistry(8000);
    	        
        	registry.bind(new String("RemoteServerInterface" + (RealNumS + 1)), stub);
        	numInterface=RealNumS +1;
        	System.out.println("ServerInterface" + (RealNumS + 1) + " ready");
        
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
				destiny.addPendingTransfers(new Transaction(clients.get(decipheredMessage.getSender()),clients.get(decipheredMessage.getDestination()),decipheredMessage.getAmount(), manager.getDigitalSign(msg)));

				//Write to file
				storage.writeClient(clients.get(decipheredMessage.getDestination()), destiny);
				storage.writeClient(clients.get(decipheredMessage.getSender()), sender);
				//Write to backup file
				storage.writeClientBackup(clients.get(decipheredMessage.getDestination()), destiny);
				storage.writeClientBackup(clients.get(decipheredMessage.getSender()), sender);

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

				destiny.addTransfers(t);
				sender.addTransfers(t);
				
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
