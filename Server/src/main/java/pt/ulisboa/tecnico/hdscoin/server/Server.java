package pt.ulisboa.tecnico.hdscoin.server;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import pt.ulisboa.tecnico.hdscoin.Crypto.CipheredMessage;
import pt.ulisboa.tecnico.hdscoin.Crypto.CryptoManager;
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
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

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
	private ArrayList<BroadcastMessage> broadcastMessageEcho=new ArrayList<BroadcastMessage>(); //receive
	private CountDownLatch echoCountDown = new CountDownLatch(3);
	private ArrayList<BroadcastMessage> broadcastMessageReady=new ArrayList<BroadcastMessage>(); //order
	private CountDownLatch readyCountDown = new CountDownLatch(3);
	private ArrayList<BroadcastMessage> broadcastMessageDelivery=new ArrayList<BroadcastMessage>(); //write
	private CountDownLatch deliveryCountDown = new CountDownLatch(3);
	
	/*TODO case above does not work
	List<BroadcastMessage> broadcastMessageEcho = Collections.synchronizedList(new ArrayList<BroadcastMessage>());
	List<BroadcastMessage> broadcastMessageReady = Collections.synchronizedList(new ArrayList<BroadcastMessage>());
	List<BroadcastMessage> broadcastMessageDelivery = Collections.synchronizedList(new ArrayList<BroadcastMessage>());
	*/
	
	

    private Storage storage;
    private KeystoreManager keyPairManager;
    private KeyPair serverKeyPair;
    private CryptoManager manager;
    private String nameServer;
    private int serverNumber;
    private int totalServerNumber;
    private int taskCounter;
    
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
            manager = new CryptoManager(serverKeyPair.getPublic(), serverKeyPair.getPrivate(), keyPairManager);

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


        
        
        //TODO
        final BroadcastMessage checkBroadcast=new BroadcastMessage(manager.getDigitalSign(msg), totalServerNumber);
        
        
        

        Message message = new Message(serverKeyPair.getPublic(), false, -1); //case the client does not exist
        if (storage.checkFileExists(clients.get(decipheredMessage.getSender()))) {


            Ledger sender = storage.readClient(clients.get(decipheredMessage.getSender()));
            if(sender.getLastWriteTimestamp() < decipheredMessage.getTimestamp()) {
                sender.setLastWriteTimestamp(decipheredMessage.getTimestamp());
                if (sender.sendBalance(decipheredMessage.getAmount())) {
                    Ledger destiny = storage.readClient(clients.get(decipheredMessage.getDestination())); //destiny public key, not name
                    destiny.addPendingTransfers(new Transaction(clients.get(decipheredMessage.getSender()),
                            clients.get(decipheredMessage.getDestination()), decipheredMessage.getAmount(), manager.getDigitalSign(msg)));

                    try {
                        storage.writeClient(clients.get(decipheredMessage.getDestination()), destiny);
                        storage.writeClient(clients.get(decipheredMessage.getSender()), sender);
                        //Write to backup file
                        storage.writeClientBackup(clients.get(decipheredMessage.getDestination()), destiny);
                        storage.writeClientBackup(clients.get(decipheredMessage.getSender()), sender);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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





        //TODO
        final BroadcastMessage checkBroadcast=new BroadcastMessage(manager.getDigitalSign(msg), totalServerNumber);
        //TODO Sheng
        echoSelf(checkBroadcast);
        
        
        
        
        
        try {
        	System.out.println("Waiting for echo...");
        	echoCountDown.await();
        	System.out.println("Echo worked...");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        //broadcastMessageEcho.add(checkBroadcast);
        
        
        
        Message message = new Message(manager.getPublicKey(), 0.0, new ArrayList<Transaction>(), clients.get(decipheredMessage.getDestination()), 0); //case the client does not exist
        if (storage.checkFileExists(clients.get(decipheredMessage.getDestination()))) {
            Ledger value = storage.readClient(clients.get(decipheredMessage.getDestination()));

            if (decipheredMessage.getDestination().equals(decipheredMessage.getSender()))
                message = new Message(manager.getPublicKey(), value.getBalance(), value.getPendingTransfers(), clients.get(decipheredMessage.getDestination()), value.getLastWriteTimestamp());
            else
                message = new Message(manager.getPublicKey(), value.getBalance(), null, clients.get(decipheredMessage.getDestination()), value.getLastWriteTimestamp());
        }
        CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, decipheredMessage.getSender());

        return cipheredMessage;
    }


    public synchronized CipheredMessage receive(CipheredMessage msg) throws RemoteException {

        if (isServerCrashed())
            throw new RemoteException();

        Message decipheredMessage = manager.decipherCipheredMessage(msg);


        
        
        
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

    public CipheredMessage audit(CipheredMessage msg) throws RemoteException {

        if (isServerCrashed())
            throw new RemoteException();

        Message decipheredMessage = manager.decipherCipheredMessage(msg);





        

        Ledger value = storage.readClient(clients.get(decipheredMessage.getDestination()));

        Message message = new Message(manager.getPublicKey(), value.getBalance(), value.getTransfers(), clients.get(decipheredMessage.getDestination()), value.getLastWriteTimestamp());

        CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, decipheredMessage.getSender());
        return cipheredMessage;
    }

    @Override
    public CipheredMessage clientHasRead(CipheredMessage msg) throws RemoteException {
        Message decipheredMessage = manager.decipherCipheredMessage(msg);

        Ledger toBeUpdated = storage.readClient(clients.get(decipheredMessage.getCheckedKey()));
        Message message = new Message(serverKeyPair.getPublic(), false, toBeUpdated.getLastWriteTimestamp());
        if(toBeUpdated.getLastWriteTimestamp() < decipheredMessage.getTimestamp()) {
            if (decipheredMessage.getTransactions() != null) {
                if (decipheredMessage.isAudit())
                    toBeUpdated.setPendingTransfers(decipheredMessage.getTransactions());
                else
                    toBeUpdated.setTransfers(decipheredMessage.getTransactions());
            }
            toBeUpdated.setBalance(decipheredMessage.getAmount());
            toBeUpdated.setLastWriteTimestamp(decipheredMessage.getTimestamp());
            try {
                storage.writeClient(clients.get(decipheredMessage.getCheckedKey()), toBeUpdated);
                storage.writeClientBackup(clients.get(decipheredMessage.getCheckedKey()), toBeUpdated);
            } catch (IOException e) {
                e.printStackTrace();
            }

            message = new Message(serverKeyPair.getPublic(), true, toBeUpdated.getLastWriteTimestamp());
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
    
    
    
    
    
    
    

	//Message broadcast
    //TODO sheng manager.decipherCipheredMessage(msg); error, cannot decipher the msg
    public void echoBroadcast(CipheredMessage msg) throws RemoteException {
    	Message decipheredMessage = manager.decipherCipheredMessage(msg);
    	BroadcastMessage bcm=decipheredMessage.getBcm();

    	//if there is no BroadcastMessage, then add and broadcast
    	if(!broadcastMessageEcho.stream().map(BroadcastMessage::getDigitalsign).filter(bcm.getDigitalsign()::equals).findFirst().isPresent()) {
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
	    		if(bcm.getDigitalsign().equals(broadcastMessageEcho.get(i).getDigitalsign())) {
	    			for(String s:serversPublicKey.keySet())
	    				if(serversPublicKey.get(s).equals(decipheredMessage.getSender())&&
	    						broadcastMessageEcho.get(i).serverEchoed(s)){
	    					broadcastMessageEcho.get(i).echoServer(s);
				    		echoCountDown.countDown();
						}
	    		}
    		//else nothing
    	}
    }
    public void readyBroadcast(CipheredMessage msg) throws RemoteException {
    	
    }
    public void deliveryBroadcast(CipheredMessage msg) throws RemoteException {
    	
    }
    
    
    private void echoSelf(BroadcastMessage checkBroadcast) throws RemoteException {
    	
    	if(!broadcastMessageEcho.stream().map(BroadcastMessage::getDigitalsign).filter(checkBroadcast.getDigitalsign()::equals).findFirst().isPresent()) {
        	
    		//TODO broadcast echoBroadcast(CipheredMessage)
    		
    		
    		for (int i = 0; i < servers.size(); i++) {
        		if((i+1)==serverNumber) {
        			checkBroadcast.echoServer(nameServer);
            		broadcastMessageEcho.add(checkBroadcast);
            		echoCountDown.countDown();
        			continue;
        		}
        		final int index=i;
        		service.execute(() -> {
        			
    	    		try {
    	    			//serversPublicKey.get("server"+(index))==serversPublicKey.get("server"+(index)) --> printed
    	    			Message msg=new Message(manager.getPublicKey(), manager.getPublicKeyBy("server"+(index+1)), checkBroadcast);
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
    private void readySelf(BroadcastMessage msg) throws RemoteException {
    	
    }
    private void deliverySelf(BroadcastMessage msg) throws RemoteException {
    	
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    private boolean registerEcho(FunctionRegister register) throws RemoteException{
    	//Authenticated Double-Echo Broadcast page 118 Echo
    	
    	final ConcurrentHashMap<String, FunctionRegister> acklist = new ConcurrentHashMap<>();
    	
    	//add myself that I'm receiving this message
    	if(registerEchoMessage.get(nameServer)==null){
    		ArrayList<FunctionRegister> tmpList=new ArrayList<FunctionRegister>();
    		tmpList.add(register);
    		registerEchoMessage.put(nameServer, tmpList);
    	}else{
    		ArrayList<FunctionRegister> tmpList=registerEchoMessage.get(nameServer);
    		tmpList.add(register);
    		registerEchoMessage.put(nameServer, tmpList);
    	}
    	System.out.println(nameServer+" : "+registerEchoMessage.get(nameServer).size()+" : Server size: "+servers.size());
    	int equal=0;
    	int different=0;
    	for (int i = 0; i < servers.size(); i++) {
    		if((i+1)==serverNumber) {
    			FunctionRegister registerReturn=sendEchoRegister(register);
    			acklist.put(nameServer, registerReturn);
    			continue;
    		}
    		final int index=i;
    		service.execute(() -> {
	    		try {
	    			FunctionRegister registerReturn=servers.get(index).sendEchoRegister(register);
	    			for(int j=0;registerReturn==null&&j<10;j++){
	    				//TODO  in case after 10 tries return null, BUG
	    				registerReturn=servers.get(index).sendEchoRegister(register);
	    				
	    			}
	    			acklist.put("server"+(index+1), registerReturn);
	
	            } catch (RemoteException e) {
	                System.out.println("Connection fail...");
	                System.out.println("Server[" + (index+1) + "] connection failed");
	            }
    		});
    	}
    	//Wait for values;
    	//TODO CountDownLatch???
    	while (!(acklist.keySet().size() > (totalServerNumber + 1) / 2)) { //(N+f)/2
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
		//Compare received Value;
		for(int i=0; i<acklist.size();i++) {
			try{
				System.out.println("Test1: "+acklist.get(nameServer).toString());
				System.out.println("Test2: "+acklist.get("server"+(i+1)).toString());
				if(acklist.get(nameServer).myEquals(acklist.get("server"+(i+1)))) {
					equal++;
				}else{
					different++;
				}
			}catch(Exception e){
				different++;
			}
			
	    }
		//if there is no more than 5 equals value, then operation is cancelled;
    	if(equal<=(totalServerNumber + 1) / 2) {
    		System.out.println("Equals values == "+equal);
    		System.out.println("There are "+different+" values");
			return false;
		}
    	
    	return true;
    }

    private boolean registerReady(FunctionRegister register) throws RemoteException{
    	//Authenticated Double-Echo Broadcast page 118 Ready
    	
    	//add myself that I'm ready for this message
    	if(registerReadyMessage.get(nameServer)==null){
    		ArrayList<FunctionRegister> tmpList=new ArrayList<FunctionRegister>();
    		tmpList.add(register);
    		registerReadyMessage.put(nameServer, tmpList);
    	}else{
    		ArrayList<FunctionRegister> tmpList=registerReadyMessage.get(nameServer);
    		tmpList.add(register);
    		registerReadyMessage.put(nameServer, tmpList);
    	}
    	
    	
    	final ConcurrentHashMap<String, ArrayList<FunctionRegister>> acklist = new ConcurrentHashMap<>();
    	int equal=0;
    	int different=0;
    	for (int i = 0; i < servers.size(); i++) {
    		if((i+1)==serverNumber) {
    			ArrayList<FunctionRegister> registerReturn=sendReadyRegister(register);
    			acklist.put(nameServer, registerReturn);
    			continue;
    		}
    		final int index=i;
    		service.execute(() -> {
	    		try {
	    			ArrayList<FunctionRegister> registerReturn=servers.get(index).sendReadyRegister(register);
	    			while(registerReturn.size()<registerReadyMessage.get(nameServer).size())
	    				registerReturn=servers.get(index).sendReadyRegister(register);
	    			acklist.put("server"+(index+1), registerReturn);
	
	            } catch (RemoteException e) {
	                System.out.println("Connection fail...");
	                System.out.println("Server[" + (index+1) + "] connection failed");
	            }
    		});
    	}
    	//Wait for values;
    	while (!(acklist.keySet().size() > (totalServerNumber + 1) / 2)) { //(N+f)/2
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
		//Compare received Value;
    	int listEqual=0;
		for(int i=0; i<acklist.size();i++) {
			System.out.println(nameServer+" check: "+acklist.get(nameServer).size()+" vs "+acklist.get("server"+(i+1)).size());
			if(acklist.get(nameServer).size()!=1){
				for(int j=0; j<acklist.get(nameServer).size();j++){
					try{
						if(acklist.get(nameServer).get(j).myEquals(acklist.get("server"+(i+1)).get(j))) {
							listEqual++;
						}
					}catch(Exception e){
						// case others server don't have same list size, same server will get delay
						e.printStackTrace();
					}
					if(j==(acklist.get(nameServer).size()-1)){
						System.out.println(nameServer+" test: "+listEqual+" : "+acklist.get(nameServer).size());
						if(listEqual==acklist.get(nameServer).size())
							equal++;
						else
							different++;
					}
				}
			}else{
				if(acklist.get(nameServer).get(0).myEquals(acklist.get("server"+(i+1)).get(0))) 
					equal++;
				else
					different++;
			}
				
	    }
		//if there is no more than 5 equals value, then operation is cancelled;
		//TODO maybe we can put it with timeout instead of 1 try
    	if(equal<=(totalServerNumber + 1) / 2) {
    		System.out.println("Equals values == "+equal);
    		System.out.println("There are "+different+" values");
			return false;
		}
    	return true;
    }
    
    /*TODO not necessary part1?
    private boolean registerDelivery(FunctionRegister register) throws RemoteException{
    	//Authenticated Double-Echo Broadcast page 118 Delievery

    	final ConcurrentHashMap<String, FunctionRegister> acklist = new ConcurrentHashMap<>();
    	int equal=0;
    	int different=0;
    	for (int i = 0; i < servers.size(); i++) {
    		if((i+1)==serverNumber) {
    			FunctionRegister registerReturn=sendEchoRegister(register);
    			acklist.put(nameServer, registerReturn);
    			continue;
    		}
    		final int index=i;
    		service.execute(() -> {
	    		try {
	    			
	    			FunctionRegister registerReturn=servers.get(index).sendEchoRegister(register);
	    			acklist.put("server"+(index+1), registerReturn);
	
	            } catch (RemoteException e) {
	                System.out.println("Connection fail...");
	                System.out.println("Server[" + (index+1) + "] connection failed");
	            }
    		});
    	}
    	//Wait for values;
    	while (!(acklist.keySet().size() > (totalServerNumber + 1) / 2)) { //(N+f)/2
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
		//Compare received Value;
		for(int i=0; i<acklist.size();i++) {
			if(acklist.get(nameServer).myEquals(acklist.get("server"+(i+1)))) {
				equal++;
			}else{
				different++;
			}
			
	    }
		//if there is no more than 5 equals value, then operation is cancelled;
    	if(equal<=(totalServerNumber + 1) / 2) {
    		System.out.println("Equals values == "+equal);
    		System.out.println("There are "+different+" values");
			return false;
		}
    	return true;
    }
    */
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
	//Authenticated Double-Echo Broadcast page 118 for register
	@Override
	public FunctionRegister sendEchoRegister(FunctionRegister register) throws RemoteException {
	
		
		//this.sentEchoRegister=true;
		if(registerEchoMessage.get(register.getServerOrigin())==null){
    		ArrayList<FunctionRegister> tmpList=new ArrayList<FunctionRegister>();
    		tmpList.add(register);
    		registerEchoMessage.put(register.getServerOrigin(), tmpList);
    	}else{
    		ArrayList<FunctionRegister> tmpList=registerEchoMessage.get(register.getServerOrigin());
    		if(!tmpList.contains(register)){
    			tmpList.add(register);
    			registerEchoMessage.put(register.getServerOrigin(), tmpList);
    		}
    	}
		
		ArrayList<FunctionRegister> tmpList=registerEchoMessage.get(nameServer);
		if(tmpList!=null){
			for(FunctionRegister reg:tmpList){
				if(reg.myEquals(register)){
					System.out.println(reg.getServerOrigin()+" comparing "+register.getServerOrigin());
					return reg;
				}
			}
		}
		return null;
	}
	@Override
	public ArrayList<FunctionRegister> sendReadyRegister(FunctionRegister register) throws RemoteException {
		//this.sentReadyRegister=true;
		if(registerReadyMessage.get(register.getServerOrigin())==null){
    		ArrayList<FunctionRegister> tmpList=new ArrayList<FunctionRegister>();
    		tmpList.add(register);
    		registerReadyMessage.put(register.getServerOrigin(), tmpList);
    	}else{
    		ArrayList<FunctionRegister> tmpList=registerReadyMessage.get(register.getServerOrigin());
			if(!tmpList.contains(register)){
				tmpList.add(register);
				registerReadyMessage.put(register.getServerOrigin(), tmpList);
			}
    	}
		return registerReadyMessage.get(nameServer);
	}
	
	//TODO not necessary part3?
	@Override
	public FunctionRegister sendDeliveryRegister(FunctionRegister register) throws RemoteException {
		
		//this.sentReadyRegister=true;
		return register;
	}
    

}
