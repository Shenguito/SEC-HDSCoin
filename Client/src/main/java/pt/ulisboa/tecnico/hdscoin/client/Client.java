package pt.ulisboa.tecnico.hdscoin.client;



import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.ulisboa.tecnico.hdscoin.Crypto.CipheredMessage;
import pt.ulisboa.tecnico.hdscoin.Crypto.CryptoManager;
import pt.ulisboa.tecnico.hdscoin.Crypto.Message;
import pt.ulisboa.tecnico.hdscoin.interfaces.KeystoreManager;
import pt.ulisboa.tecnico.hdscoin.interfaces.RemoteServerInterface;
import pt.ulisboa.tecnico.hdscoin.interfaces.Transaction;




public class Client {

	
    private List<Transaction> pendingTransaction;
    
    private CryptoManager manager;
    
 	private KeystoreManager keyPairManager;
 	private KeyPair clientKeyPair;
 	private String clientName;
    private PublicKey serverPublicKey;
    
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


		for(int i = 0; i < numServers(); i++) {

			try {
				servers.get(i).register(clientName, manager.getPublicKey());
				//TODOSERVERKEY using only one key
				try {
					serverPublicKey=manager.getPublicKeyBy("server1");
				} catch (Exception e) {
					System.out.println("publickey error");
					e.printStackTrace();
				}
	            
				System.out.println("You are registered by server["+(i+1)+"]");
				
	        } catch (RemoteException e) {
	        	System.out.println("Connection fail...");
	        	System.out.println("Server["+(i+1)+"] connection failed");
	        }
			
		}
		return true;
	}
	
	public synchronized boolean send(String sendDestination, String sendAmount) {
		if(getClientName().toUpperCase().equals(sendDestination.toUpperCase())) {
			System.out.println("'"+sendDestination +"'? There is a bit probability being you, don't try to send money to yourself ;)");
			return true;
		}
		try {
            Message msg = new Message(Double.parseDouble(sendAmount.trim()), manager.getPublicKey(), keyPairManager.getPublicKeyByName(sendDestination)); //SERVER_key represents sendDestination
            if(serverPublicKey==null)
            	System.out.println("ServerKey is null");
            
            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serverPublicKey);
            List<Boolean> awnsers = new ArrayList<Boolean>();
            
            for(int i = 0; i < numServers(); i++) {
            	try{
	        		CipheredMessage response = servers.get(i).send(cipheredMessage);
		
		            Message responseDeciphered = manager.decipherCipheredMessage(response);
		            awnsers.add(responseDeciphered.isConfirm());
		            System.out.println("Success from server " + (i+1) + ": " + responseDeciphered.isConfirm());

	            } catch (RemoteException e) {
		        	System.out.println("Connection fail...");

		        }catch(IllegalStateException e){
		        	System.out.println("Invalid signature");

		        }
	            
            }
            Collection<Boolean> collection = awnsers;
            if(Collections.frequency(collection, collection.iterator().next()) != collection.size() && awnsers.size()==7)//implementar modelo de faltas
            	System.out.println("bad receive");
           
		} catch(Exception e){
        	System.out.println("Invalid message");
        	return false;
        }
		return true;
		
	}
	public boolean check(String sendDestination) {
		String checkedName="";
		Map<Integer, List<Transaction>> transactions = new HashMap<Integer,List<Transaction>>();
		Map<Integer, Double> amounts = new HashMap<Integer,Double>();
		try {
            Message msg = new Message(manager.getPublicKey(), keyPairManager.getPublicKeyByName(sendDestination));
            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serverPublicKey);
            for(int i = 0; i < numServers(); i++) {

            	try{
            		CipheredMessage response = servers.get(i).check(cipheredMessage);
            		Message responseDeciphered = manager.decipherCipheredMessage(response);
            		checkedName=responseDeciphered.getCheckedName();
            		if(checkedName==null) {		//no user exist
            			throw new Exception();
            		}
            		amounts.put(i, responseDeciphered.getAmount());
            		if(responseDeciphered.getTransactions()!=null&&clientName.equals(checkedName)) {

    		            pendingTransaction=new ArrayList<Transaction>();
    		            for(Transaction t:responseDeciphered.getTransactions()) 
    		            	pendingTransaction.add(t);
    		            if(pendingTransaction.size()!=0)
    		            	transactions.put(i, pendingTransaction);
    		            	
    	            }

            	} catch (RemoteException e) {
            		//TODO fix connection bug
    	        	System.out.println("Connection fail...");
    	        	e.printStackTrace();

    	        } catch(IllegalStateException e){
    	        	System.out.println("Invalid signature");

    	        }
	            
            }
            Collection<Double> amount = amounts.values();
            if(Collections.frequency(amount, amount.iterator().next()) == amount.size())
            	System.out.println(checkedName + "'s balance is: " + amount.iterator().next());
            //else{ implementar modelo de faltas
			//}
            if(transactions.size()==0&&clientName.equals(checkedName))
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
	return true;
}
	            		
            

	
	public boolean receive(int receivedPendingTransfers) {
		
		if(pendingTransaction.size()==0){
			System.out.println("You do not have any pending transaction. Make a check first...");
			return true;
		}
		
		List<Boolean> awnsers = new ArrayList<Boolean>();
		
		try {
			int index=receivedPendingTransfers-1;
			Transaction receiveTransaction=pendingTransaction.get(index);
            
            Message msg = new Message(manager.getPublicKey(), receiveTransaction);
            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serverPublicKey);
            
            for(int i = 0; i < numServers(); i++) {
        		
            	try{
		            CipheredMessage response = servers.get(i).receive(cipheredMessage);

		            Message responseDeciphered = manager.decipherCipheredMessage(response);
		            awnsers.add(responseDeciphered.isConfirm());
		            System.out.println("Success from server " + (i+1) + ": " + responseDeciphered.isConfirm());


            	} catch (RemoteException e) {
    	        	System.out.println("Connection fail...");

    	        } catch(IllegalStateException e){
    	        	System.out.println("Illegal State Exception Invalid signature");

    	        }
	            
            }
            
            Collection<Boolean> collection = awnsers;
            if(Collections.frequency(collection, collection.iterator().next()) != collection.size() && awnsers.size()==7)//implementar modelo de faltas
            	System.out.println("bad receive");
    		return true;
            
        } catch (Exception e) {
        	System.out.println("Invalid message");
        	return false;
        }
		
	}
	public boolean audit(String sendDestination) {
		String name ="";
		Map<Integer, List<Transaction>> transactions = new HashMap<Integer,List<Transaction>>();
		Map<Integer, Integer> transfers = new HashMap<Integer,Integer>();
		try {
            Message msg = new Message(manager.getPublicKey(), keyPairManager.getPublicKeyByName(sendDestination));
            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serverPublicKey);
            
            for(int i = 0; i < numServers(); i++) {
            
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
		            
		            continue;
            	} catch (RemoteException e) {
    	        	System.out.println("Connection fail...");

    	        } catch(IllegalStateException e){
    	        	System.out.println("Invalid signature");

    	        }
            
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
            
            
        } catch (Exception e) {
        	System.out.println("Invalid message");
        	return false;
        }
		return true;
	}
	
	public String getClientName() {
		return clientName;
	}
	
	
	public void removePendingTransaction(){
		pendingTransaction=new ArrayList<Transaction>();
	}
}
