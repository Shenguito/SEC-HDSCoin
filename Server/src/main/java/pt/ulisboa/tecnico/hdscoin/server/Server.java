package pt.ulisboa.tecnico.hdscoin.server;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import pt.ulisboa.tecnico.hdscoin.Crypto.CipheredMessage;
import pt.ulisboa.tecnico.hdscoin.Crypto.CryptoManager;
import pt.ulisboa.tecnico.hdscoin.Crypto.IntegrityCheck;
import pt.ulisboa.tecnico.hdscoin.Crypto.Message;
import pt.ulisboa.tecnico.hdscoin.interfaces.BroadcastMessage;
import pt.ulisboa.tecnico.hdscoin.interfaces.FunctionRegister;
import pt.ulisboa.tecnico.hdscoin.interfaces.KeystoreManager;
import pt.ulisboa.tecnico.hdscoin.interfaces.RemoteServerInterface;
import pt.ulisboa.tecnico.hdscoin.interfaces.Transaction;
import pt.ulisboa.tecnico.hdscoin.server.storage.Ledger;
import pt.ulisboa.tecnico.hdscoin.server.storage.Storage;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


public class Server implements RemoteServerInterface {

	
	//Authenticated Double-Echo Broadcast page 118

	private ConcurrentHashMap<String, ArrayList<FunctionRegister>> registerEchoMessage=new ConcurrentHashMap<String, ArrayList<FunctionRegister>>();
	private ConcurrentHashMap<String, ArrayList<FunctionRegister>> registerReadyMessage=new ConcurrentHashMap<String, ArrayList<FunctionRegister>>();
	private int rid=0;
	private int wts=0;
	private ExecutorService service = Executors.newFixedThreadPool(7);
	
	
	
	//Authenticated Double-Echo Broadcast based for message exchange
	/*
	private ArrayList<BroadcastMessage> broadcastMessageEcho=new ArrayList<BroadcastMessage>(); //receive
	private ArrayList<BroadcastMessage> broadcastMessageReady=new ArrayList<BroadcastMessage>(); //order
	private ArrayList<BroadcastMessage> broadcastMessageDelivery=new ArrayList<BroadcastMessage>(); //write
	*/


	List<BroadcastMessage> broadcastMessageEcho = Collections.synchronizedList(new ArrayList<BroadcastMessage>());
	List<BroadcastMessage> broadcastMessageReady = Collections.synchronizedList(new ArrayList<BroadcastMessage>());
	List<BroadcastMessage> broadcastMessageDelivery = Collections.synchronizedList(new ArrayList<BroadcastMessage>());

	
	private CountDownLatch echoCountDown = new CountDownLatch(3);
	private CountDownLatch readyCountDown = new CountDownLatch(3);
	private CountDownLatch deliveryCountDown = new CountDownLatch(3);
	


    private Storage storage;
    private KeystoreManager keyPairManager;
    private KeyPair serverKeyPair;
    private CryptoManager manager;
    private String nameServer;
    private int serverNumber;
    private int totalServerNumber;
    private int taskCounter;
    private boolean byzantine=false;
    
    private List<RemoteServerInterface> servers;

    private HashMap<String, PublicKey> serversPublicKey;
    
    private boolean crashFailure;

    private CipheredMessage lastWrite = null;
    //private long lastWriteTimestamp = -1;

    private ConcurrentHashMap<PublicKey, String> clients;

    public Server(int number, int totalServer) throws RemoteException, AlreadyBoundException, MalformedURLException, NotBoundException {
    	nameServer = "server" + number;
    	serverNumber=number;
    	totalServerNumber=totalServer;
    	servers = new ArrayList<RemoteServerInterface>();
    	serversPublicKey = new HashMap<String, PublicKey>();
        storage = new Storage(nameServer);
        taskCounter = 0;
        backupFileCheck();
        connect(number);
        try {
            keyPairManager = new KeystoreManager("/server.jks", "server123");
            serverKeyPair = keyPairManager.getKeyPair("server"+number, "server"+number+"123");
            manager = new CryptoManager(serverKeyPair.getPublic(), serverKeyPair.getPrivate(), keyPairManager, false, 0);

        } catch (Exception e) {
            System.out.println("KeyPair Error");
            e.printStackTrace();
        }
        crashFailure = false;
    }
    public void connectServer() throws RemoteException, MalformedURLException, NotBoundException {
        for (int i = 0; i < totalServerNumber; i++) {
        	servers.add((RemoteServerInterface) Naming.lookup(new String("//localhost:8000/" + "RemoteServerInterface" + (i + 1))));
        	try {
        		if((i+1)==serverNumber) //Does not save publickey of himself
        			continue;
				serversPublicKey.put("server"+(i+1), manager.getPublicKeyBy("server"+(i+1)));
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
    }

    private void backupFileCheck() {
        storage.backupCheck();
        try {
            clients = storage.getClients();
            for (String s : clients.values())
                System.out.println("Client already registered: " + s);
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

    private void connect(int serverNumber) throws RemoteException, AlreadyBoundException {
        System.setProperty("java.rmi.server.hostname", "127.0.0.1");
        RemoteServerInterface stub;
        Registry registry;
        int RealNumS = 0;
        try {
            RealNumS = LocateRegistry.getRegistry(8000).list().length;
        } catch (RemoteException e) {
            stub = (RemoteServerInterface) UnicastRemoteObject.exportObject(this, 0);
            registry = LocateRegistry.createRegistry(8000);

            registry.bind("RemoteServerInterface1", stub);
            System.out.println("ServerInterface1 ready");
            System.out.println("ServerInterface" + (RealNumS + 1) + " ready"+"\t Server number: "+serverNumber);
            return;
        }
        System.out.println(RealNumS);
        stub = (RemoteServerInterface) UnicastRemoteObject.exportObject(this, 0);
        registry = LocateRegistry.getRegistry(8000);

        registry.bind(new String("RemoteServerInterface" + (RealNumS + 1)), stub);
        System.out.println("ServerInterface" + (RealNumS + 1) + " ready"+"\t Server number: "+serverNumber);
    }


    
    public boolean register(String clientName, PublicKey publickey) throws RemoteException {
    	
    	if (isServerCrashed())
            throw new RemoteException();
    	
    	int readID=rid+1;
    	
    	final FunctionRegister register=new FunctionRegister(clientName, publickey, readID, totalServerNumber);
    	/*
    	//ECHO
    	final FunctionRegister register=new FunctionRegister(clientName, publickey, readID, nameServer);

    	//ECHO
    	try{
	    	if(!(registerEcho(register))){
	    		System.out.println("Error with message and echo message!");
	    		return false;
	    	}
    	}catch(RemoteException e){
    		System.out.println("RemoteException error... Error with message and echo message!");
    		return false;
    	}
    	
    	//READY
    	try{
	    	if(!(registerReady(register))){
	    		System.out.println("Error with ready and delivery message!");
	    		return false;
	    	}
    	}catch(RemoteException e){
    		System.out.println("RemoteException error... Error with ready and delivery message!");
    		return false;
    	}
*/
        if (!storage.checkFileExists(clientName)) {
            try {
                Ledger ledger = new Ledger(publickey, 100, new ArrayList<Transaction>(), new ArrayList<Transaction>());
                storage.writeClient(clientName, ledger);
                storage.writeClientBackup(clientName, ledger);
                //Confirm write and read
                rid=readID;
            } catch (JsonGenerationException e) {
                e.printStackTrace();
                return false;
            } catch (JsonMappingException e) {
                e.printStackTrace();
                return false;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            System.out.println("User already registered!");
            
        }
        if (!clients.containsKey(clientName)) {
            clients.put(publickey, clientName);
            System.out.println("Test-> reading " + clientName + " file:\n" + storage.readClient(clientName).toString());
        }
    	
    	
    	/*TODO not necessary part2?
        try{
	    	if(!registerDelivery(register)){
	    		System.out.println("Error with delivery message!");
	    		return;
	    	}
    	}catch(RemoteException e){
    		System.out.println("RemoteException error... Error with delivery message!");
    		return;
    	}
    	*/
    	return true;
    }

    //PublicKey source, PublicKey destination, int amount
    public synchronized CipheredMessage send(CipheredMessage msg) throws RemoteException {

        if (isServerCrashed())
            throw new RemoteException();

        System.out.println("Deciphering message");
        Message decipheredMessage = manager.decipherCipheredMessage(msg);

        //Broadcast
        broadcastEcho(manager.getDigitalSign(msg));



        Message message = new Message(serverKeyPair.getPublic(), false, -1); //case the client does not exist
        if (storage.checkFileExists(clients.get(decipheredMessage.getSender()))) {


            Ledger sender = storage.readClient(clients.get(decipheredMessage.getSender()));
            if(sender.getLastWriteTimestamp() < decipheredMessage.getTimestamp()) {
                sender.setLastWriteTimestamp(decipheredMessage.getTimestamp());
                if (sender.sendBalance(decipheredMessage.getAmount())) {
                    Ledger destiny = storage.readClient(clients.get(decipheredMessage.getDestination())); //destiny public key, not name
                    destiny.addPendingTransfers(new Transaction(clients.get(decipheredMessage.getSender()),
                            clients.get(decipheredMessage.getDestination()), decipheredMessage.getAmount(), manager.getDigitalSign(msg), decipheredMessage, msg.getIV()));

                    try {
                        storage.writeClient(clients.get(decipheredMessage.getDestination()), destiny);
                        storage.writeClient(clients.get(decipheredMessage.getSender()), sender);
                        //Write to backup file
                        storage.writeClientBackup(clients.get(decipheredMessage.getDestination()), destiny);
                        storage.writeClientBackup(clients.get(decipheredMessage.getSender()), sender);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(byzantine)
                    	message = new Message(serverKeyPair.getPublic(), false, sender.getLastWriteTimestamp());
                    else
                    	message = new Message(serverKeyPair.getPublic(), true, sender.getLastWriteTimestamp());
                }
            } else System.out.println("Message out of date - MSG: " + decipheredMessage.getTimestamp() + " TIME: " + sender.getLastWriteTimestamp());

        }

        CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, decipheredMessage.getSender());
        return cipheredMessage;

    }


    public CipheredMessage check(CipheredMessage msg) throws RemoteException {

        if (isServerCrashed())
            throw new RemoteException();

        Message decipheredMessage = manager.decipherCipheredMessage(msg);
        System.out.println("Sender is "+clients.get(decipheredMessage.getSender()) + ":\n" + decipheredMessage.getSender());
        System.out.println("Target is "+clients.get(decipheredMessage.getDestination()) + ":\n" + decipheredMessage.getDestination());


        //Broadcast
        broadcastEcho(manager.getDigitalSign(msg));


        Message message = new Message(manager.getPublicKey(), 0.0, new ArrayList<Transaction>(), decipheredMessage.getDestination(), clients.get(decipheredMessage.getDestination()), 0); //case the client does not exist
        if (storage.checkFileExists(clients.get(decipheredMessage.getDestination()))) {
            Ledger value = storage.readClient(clients.get(decipheredMessage.getDestination()));

            if (decipheredMessage.getDestination().equals(decipheredMessage.getSender()))
                message = new Message(manager.getPublicKey(), value.getBalance(), value.getPendingTransfers(), decipheredMessage.getDestination(), clients.get(decipheredMessage.getDestination()), value.getLastWriteTimestamp());
            else //NOT GOOD
                message = new Message(manager.getPublicKey(), value.getBalance(), null,  decipheredMessage.getDestination(), clients.get(decipheredMessage.getDestination()), value.getLastWriteTimestamp());
        }
        CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, decipheredMessage.getSender());
        System.out.println("Deliveried");
        return cipheredMessage;
    }
    



    public synchronized CipheredMessage receive(CipheredMessage msg) throws RemoteException {

        if (isServerCrashed())
            throw new RemoteException();

        Message decipheredMessage = manager.decipherCipheredMessage(msg);

        //Broadcast
        broadcastEcho(manager.getDigitalSign(msg));



        Message message = new Message(serverKeyPair.getPublic(), false, 0);

        Ledger destiny = storage.readClient(clients.get(decipheredMessage.getSender()));

        if(destiny.getLastWriteTimestamp() < decipheredMessage.getTimestamp()) {
            Iterator<Transaction> i = destiny.getPendingTransfers().iterator();
            while (i.hasNext()) {
                Transaction t = i.next();
                if (decipheredMessage.getTransaction().myEquals(t)) {
                    Ledger sender = storage.readClient(t.getSender());
                    destiny.receiveBalance(t.getAmount());
                    if (t.getIntegrityCheck() != null)
                        System.out.println("Test Ttransaction:\n" + t.getIntegrityCheck().getDigitalSignature());
                    else
                        System.out.println("NULLLLLLLLLLLLLLL");
                    destiny.addTransfers(t);
                    sender.addTransfers(t);

                    //Write to file BUG
                    i.remove();
                    try {
                        storage.writeClient(t.getSender(), sender);
                        storage.writeClient(t.getReceiver(), destiny);
                        break;
                    } catch (JsonGenerationException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (JsonMappingException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            lastWrite = msg;
            destiny.setLastWriteTimestamp(decipheredMessage.getTimestamp());
            message = new Message(serverKeyPair.getPublic(), true, destiny.getLastWriteTimestamp());
        }
        CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, decipheredMessage.getSender());
        return cipheredMessage;
    }

    public CipheredMessage audit(CipheredMessage msg) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

        if (isServerCrashed())
            throw new RemoteException();

        Message decipheredMessage = manager.decipherCipheredMessage(msg);

        //Broadcast
        broadcastEcho(manager.getDigitalSign(msg));
        
        
        Ledger value = storage.readClient(clients.get(decipheredMessage.getDestination()));
        String name = storage.getClients().get(decipheredMessage.getDestination());

        Message message = new Message(manager.getPublicKey(), value.getBalance(), value.getTransfers(), decipheredMessage.getDestination() ,name, value.getLastWriteTimestamp());

        CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, decipheredMessage.getSender());
        return cipheredMessage;
    }

    @Override
    public CipheredMessage clientHasRead(CipheredMessage msg) throws IOException, NoSuchPaddingException, ClassNotFoundException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        Message decipheredMessage = manager.decipherCipheredMessage(msg);
        Message innerMessage = decipheredMessage.getOriginal();
        boolean verified = manager.verifyIntegrity(innerMessage, decipheredMessage.getOriginalSig(), innerMessage.getSender());
        System.out.println("VERiFiED? " + verified);
        Ledger toBeUpdated = storage.readClient(clients.get(innerMessage.getCheckedKey()));
        Message message = new Message(serverKeyPair.getPublic(), false, toBeUpdated.getLastWriteTimestamp());
        //verify here if n+f/2 have toBeUpdated.getLastWriteTimestamp() as last time stamp to avoid wrong timestamp from client?
        if(verified) {
            if (toBeUpdated.getLastWriteTimestamp() < innerMessage.getTimestamp()) {
                if (innerMessage.getTransactions() != null) {
                    if (decipheredMessage.isAudit())
                        toBeUpdated.setPendingTransfers(innerMessage.getTransactions());
                    else
                        toBeUpdated.setTransfers(innerMessage.getTransactions());
                }
                toBeUpdated.setBalance(innerMessage.getAmount());
                toBeUpdated.setLastWriteTimestamp(innerMessage.getTimestamp());
                try {
                    storage.writeClient(clients.get(innerMessage.getCheckedKey()), toBeUpdated);
                    storage.writeClientBackup(clients.get(innerMessage.getCheckedKey()), toBeUpdated);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                message = new Message(serverKeyPair.getPublic(), true, toBeUpdated.getLastWriteTimestamp());
            }
        }
        CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, decipheredMessage.getSender());
        return cipheredMessage;
    }



    public void setServerFault(boolean crash) {
        crashFailure = crash;
    }

    private boolean isServerCrashed() {
        return crashFailure;
    }

    private boolean discardMessage() {
        if (1 == (new Random().nextInt(1 - 0 + 1) + 0)) {
            return false;
        }
        return true;
    }
    
    
    
    
    
    
    
    
    //Broadcast echo and ready
    
    private void broadcastEcho(IntegrityCheck integrityCheck) {
        final BroadcastMessage checkBroadcast=new BroadcastMessage(integrityCheck, totalServerNumber);
        
        //System.out.println("Digital signature stored: "+integrityCheck.getStringDigitalSignature());
        
        echoSelf(checkBroadcast);
        try {
        	//System.out.println("Waiting for echo...");
        	echoCountDown.await();
        	//System.out.println("Echo worked...");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        readySelf(checkBroadcast);
        try {
        	//System.out.println("Waiting for ready...");
        	readyCountDown.await();
        	//System.out.println("Ready worked...");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }
    
    

    

    private void echoSelf(BroadcastMessage checkEchoBroadcast){
    	if(!broadcastMessageEcho.stream().map(BroadcastMessage::getDigitalsign).filter(checkEchoBroadcast.getDigitalsign()::equals).findFirst().isPresent()) {
			checkEchoBroadcast.echoServer(nameServer);
			broadcastMessageEcho.add(checkEchoBroadcast);
			echoCountDown.countDown();
			for (int i = 0; i < servers.size(); i++) {
	    		if((i+1)==serverNumber) {
	    			continue;
	    		}
	    		final int index=i;
	    		service.execute(() -> {
		    		try {
		    			//serversPublicKey.get("server"+(index))==serversPublicKey.get("server"+(index)) --> printed
		    			Message msg=new Message(manager.getPublicKey(), manager.getPublicKeyBy("server"+(index+1)), checkEchoBroadcast);
	        			final CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serversPublicKey.get("server"+(index+1)));
	
	    				servers.get(index).echoBroadcast(cipheredMessage);
	
		            } catch (RemoteException e) {
		                System.out.println("Connection fail...");
		                System.out.println("Server[" + (index+1) + "] connection failed");
		            } catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	    		});
	    	}
    	}
    	
    }
    private void readySelf(BroadcastMessage checkReadyBroadcast){
    	
    	/* 
    	 * in case broadcastMessageReady has not this BroadcastMessage yet
    	 * 
    	 * in case broadcastMessageEcho has it && more than (n+f)/2 of servers are true
    	 * then it means that it can do ready
    	 */
    	if(!broadcastMessageReady.stream().map(BroadcastMessage::getDigitalsign).filter(checkReadyBroadcast.getDigitalsign()::equals).findFirst().isPresent()) {
			checkReadyBroadcast.readyServer(nameServer);
			broadcastMessageReady.add(checkReadyBroadcast);
			readyCountDown.countDown();
			for (int i = 0; i < servers.size(); i++) {
	    		if((i+1)==serverNumber) {
	    			continue;
	    		}
	    		final int index=i;
	    		service.execute(() -> {
		    		try {
		    			//serversPublicKey.get("server"+(index))==serversPublicKey.get("server"+(index)) --> printed
		    			Message msg=new Message(manager.getPublicKey(), manager.getPublicKeyBy("server"+(index+1)), checkReadyBroadcast);
	        			final CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serversPublicKey.get("server"+(index+1)));
	
	    				servers.get(index).readyBroadcast(cipheredMessage);
	
		            } catch (RemoteException e) {
		                System.out.println("Connection fail...");
		                System.out.println("Server[" + (index+1) + "] connection failed");
		            } catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	    		});
	    	}
    	}
    	
    }



	//Message broadcast
    public void echoBroadcast(CipheredMessage msg) throws RemoteException {
    	Message decipheredMessage = manager.decipherCipheredMessage(msg);
    	BroadcastMessage bcm=decipheredMessage.getBcm();

    	
    	//if there is no BroadcastMessage, then add and broadcast
    	
    	if(!broadcastMessageEcho.stream().map(BroadcastMessage::getStringDigitalsign).filter(bcm.getStringDigitalsign()::equals).findFirst().isPresent()) {
    		//ATTENTION, server x does not store his publickey in his map
    		if(decipheredMessage.getDestination().equals(manager.getPublicKey())){
				BroadcastMessage tmp=new BroadcastMessage(bcm.getDigitalsign(), totalServerNumber);
				for(String s:serversPublicKey.keySet())
					if(serversPublicKey.get(s).equals(decipheredMessage.getSender())){
						tmp.echoServer(s);
						broadcastMessageEcho.add(tmp);
			    		echoCountDown.countDown();
					}

			}
    	//else it only turn it true
    	}else {
    		//Compare boolean,
    		//if there is a server that becomes true and in my list it isn't
    		for(int i=0;i<broadcastMessageEcho.size();i++)
	    		if(bcm.getStringDigitalsign().equals(broadcastMessageEcho.get(i).getStringDigitalsign())) {
	    			for(String s:serversPublicKey.keySet()) { // echoServer, which means server echo received
	    				if(serversPublicKey.get(s).equals(decipheredMessage.getSender())&&
	    						!broadcastMessageEcho.get(i).serverEchoed(s)){
	    					broadcastMessageEcho.get(i).echoServer(s);
				    		echoCountDown.countDown();
						}
	    			}
	    		}
    		//else nothing
    	}
    }
    public void readyBroadcast(CipheredMessage msg) throws RemoteException {
    	Message decipheredMessage = manager.decipherCipheredMessage(msg);
    	BroadcastMessage bcm=decipheredMessage.getBcm();

    	//if there is no BroadcastMessage, then add and broadcast
    	if(!broadcastMessageReady.stream().map(BroadcastMessage::getStringDigitalsign).filter(bcm.getStringDigitalsign()::equals).findFirst().isPresent()) {
    		//ATTENTION, server x does not store his publickey in his map (HashMap<String, PublicKey> serversPublicKey)
    		if(decipheredMessage.getDestination().equals(manager.getPublicKey())){
				BroadcastMessage tmp=new BroadcastMessage(bcm.getDigitalsign(), totalServerNumber);
				for(String s:serversPublicKey.keySet())
					if(serversPublicKey.get(s).equals(decipheredMessage.getSender())){
						tmp.echoServer(s);
						broadcastMessageReady.add(tmp);
						readyCountDown.countDown();
					}

			}
    	//else it only turn it true
    	}else {
    		//Compare boolean,
    		//if there is a server that becomes true and in my list it isn't
    		for(int i=0;i<broadcastMessageReady.size();i++)
	    		if(bcm.getStringDigitalsign().equals(broadcastMessageReady.get(i).getStringDigitalsign())) {
	    			for(String s:serversPublicKey.keySet())
	    				if(serversPublicKey.get(s).equals(decipheredMessage.getSender())&&
	    						!broadcastMessageReady.get(i).serverReadied(s)){
	    					broadcastMessageReady.get(i).readyServer(s);
				    		readyCountDown.countDown();
						}
	    		}
    		//else nothing
    	}
    	
    }

















	/*@Override
	public void setByzantine(boolean mode) {
		byzantine = mode;
		
	}*/
    

}
