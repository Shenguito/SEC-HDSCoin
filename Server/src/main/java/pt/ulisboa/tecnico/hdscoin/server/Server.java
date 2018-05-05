package pt.ulisboa.tecnico.hdscoin.server;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import pt.ulisboa.tecnico.hdscoin.Crypto.CipheredMessage;
import pt.ulisboa.tecnico.hdscoin.Crypto.CryptoManager;
import pt.ulisboa.tecnico.hdscoin.Crypto.Message;
import pt.ulisboa.tecnico.hdscoin.interfaces.FunctionRegister;
import pt.ulisboa.tecnico.hdscoin.interfaces.KeystoreManager;
import pt.ulisboa.tecnico.hdscoin.interfaces.RemoteServerInterface;
import pt.ulisboa.tecnico.hdscoin.interfaces.Transaction;
import pt.ulisboa.tecnico.hdscoin.server.storage.Ledger;
import pt.ulisboa.tecnico.hdscoin.server.storage.Storage;
import pt.ulisboa.tecnico.hdscoin.server.storage.Tasks;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

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
	//CHANGED
	boolean sentEchoRegister=false;
	boolean sentReadyRegister=false;
	boolean deliveredRegister=false;
	HashMap<String, FunctionRegister> registerMessage=new HashMap<String, FunctionRegister>();
	
	
	
	
	

    private Storage storage;
    private KeystoreManager keyPairManager;
    private KeyPair serverKeyPair;
    private CryptoManager manager;
    private Tasks messageManager;
    private String nameServer;
    private int serverNumber;
    private int totalServerNumber;
    private int taskCounter;
    
    private List<RemoteServerInterface> servers;

    private HashMap<String, PublicKey> serversPublicKey;
    
    private boolean crashFailure;

    private CipheredMessage lastWrite = null;
    private long lastWriteTimestamp = -1;

    private ConcurrentHashMap<PublicKey, String> clients;

    public Server(int number, int totalServer) throws RemoteException, AlreadyBoundException, MalformedURLException, NotBoundException {
    	nameServer = "server" + number;
    	serverNumber=number;
    	totalServerNumber=totalServer;
    	servers = new ArrayList<RemoteServerInterface>();
    	serversPublicKey = new HashMap<String, PublicKey>();
        storage = new Storage(nameServer);
        taskCounter = 0;
        check();
        connect(number);
        try {
            keyPairManager = new KeystoreManager("/server.jks", "server123");
            serverKeyPair = keyPairManager.getKeyPair("server"+number, "server"+number+"123");
            manager = new CryptoManager(serverKeyPair.getPublic(), serverKeyPair.getPrivate(), keyPairManager);
            messageManager = new Tasks(nameServer);

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

    private void check() {
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

    public void register(String clientName, PublicKey publickey) throws RemoteException {
    	
    	//Authenticated Double-Echo Broadcast page 118
    	//CHANGED
    	FunctionRegister register=new FunctionRegister(clientName, publickey);
    	for (int i = 0; i < servers.size(); i++) {
    		if((i+1)==serverNumber) {
    			sentEchoRegister=true;
    			registerMessage.put(nameServer, register);
    			continue;
    		}
    		try {
    			//TODO should return echo value from others server?
                servers.get(i).sendEchoRegister(register);

            } catch (RemoteException e) {
                System.out.println("Connection fail...");
                System.out.println("Server[" + (i+1) + "] connection failed");
            }
    	}
    	
    	//TODO next lines below should continue with agreement above
    	
        if (isServerCrashed())
            throw new RemoteException();

        if (!storage.checkFileExists(clientName)) {
            try {
                Ledger ledger = new Ledger(publickey, 100, new ArrayList<Transaction>(), new ArrayList<Transaction>());
                storage.writeClient(clientName, ledger);
                storage.writeClientBackup(clientName, ledger);
            } catch (JsonGenerationException e) {
                e.printStackTrace();
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("User already registered!");
        }
        if (!clients.containsKey(clientName)) {
            clients.put(publickey, clientName);
            System.out.println("Test-> reading " + clientName + " file:\n" + storage.readClient(clientName).toString());
        }
    }

    //PublicKey source, PublicKey destination, int amount
    public synchronized CipheredMessage send(CipheredMessage msg) throws RemoteException {

        if (isServerCrashed())
            throw new RemoteException();

        System.out.println("Deciphering message");
        Message decipheredMessage = manager.decipherCipheredMessage(msg);

        taskCounter++;
        List<String> receivedTask = new ArrayList<String>();
        receivedTask.add("send");
        receivedTask.add(decipheredMessage.getCheckedName());
        receivedTask.add(Base64.getEncoder().encodeToString(decipheredMessage.getDestination().getEncoded()));
        receivedTask.add(String.valueOf(decipheredMessage.getAmount()));
        messageManager.addTask(taskCounter, receivedTask);


        Message message = new Message(serverKeyPair.getPublic(), false, lastWriteTimestamp); //case the client does not exist
        if (storage.checkFileExists(clients.get(decipheredMessage.getSender())) && lastWriteTimestamp < decipheredMessage.getTimestamp()) {
            lastWrite = msg;
            lastWriteTimestamp = decipheredMessage.getTimestamp();

            Ledger sender = storage.readClient(clients.get(decipheredMessage.getSender()));
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
                message = new Message(serverKeyPair.getPublic(), true, lastWriteTimestamp);
            }
        }

        CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, decipheredMessage.getSender());
        return cipheredMessage;

    }


    public CipheredMessage check(CipheredMessage msg) throws RemoteException {

        if (isServerCrashed())
            throw new RemoteException();

        Message decipheredMessage = manager.decipherCipheredMessage(msg);
        System.out.println(clients.get(decipheredMessage.getSender()) + ":\n" + decipheredMessage.getSender());
        System.out.println(clients.get(decipheredMessage.getDestination()) + ":\n" + decipheredMessage.getDestination());

        taskCounter++;
        List<String> receivedTask = new ArrayList<String>();
        receivedTask.add("check");
        receivedTask.add(decipheredMessage.getCheckedName());
        receivedTask.add(Base64.getEncoder().encodeToString(decipheredMessage.getDestination().getEncoded()));

        messageManager.addTask(taskCounter, receivedTask);

        Message message = new Message(manager.getPublicKey(), 0.0, new ArrayList<Transaction>(), clients.get(decipheredMessage.getDestination()), lastWriteTimestamp); //case the client does not exist
        if (storage.checkFileExists(clients.get(decipheredMessage.getDestination()))) {
            Ledger value = storage.readClient(clients.get(decipheredMessage.getDestination()));

            if (decipheredMessage.getDestination().equals(decipheredMessage.getSender()))
                message = new Message(manager.getPublicKey(), value.getBalance(), value.getPendingTransfers(), clients.get(decipheredMessage.getDestination()), lastWriteTimestamp);
            else
                message = new Message(manager.getPublicKey(), value.getBalance(), null, clients.get(decipheredMessage.getDestination()), lastWriteTimestamp);
        }
        CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, decipheredMessage.getSender());

        return cipheredMessage;
    }


    public synchronized CipheredMessage receive(CipheredMessage msg) throws RemoteException {

        if (isServerCrashed())
            throw new RemoteException();

        Message decipheredMessage = manager.decipherCipheredMessage(msg);

        taskCounter++;
        List<String> receivedTask = new ArrayList<String>();
        receivedTask.add("receive");
        receivedTask.add(decipheredMessage.getCheckedName());
//		receivedTask.add(Base64.getEncoder().encodeToString(decipheredMessage.getDestination().getEncoded()));
        receivedTask.add(String.valueOf(decipheredMessage.getAmount()));
        messageManager.addTask(taskCounter, receivedTask);

        Message message = new Message(serverKeyPair.getPublic(), false, lastWriteTimestamp);

        Ledger destiny = storage.readClient(clients.get(decipheredMessage.getSender()));

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
        lastWriteTimestamp = decipheredMessage.getTimestamp();
        message = new Message(serverKeyPair.getPublic(), true, lastWriteTimestamp);
        CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, decipheredMessage.getSender());
        return cipheredMessage;
    }

    public CipheredMessage audit(CipheredMessage msg) throws RemoteException {

        if (isServerCrashed())
            throw new RemoteException();

        Message decipheredMessage = manager.decipherCipheredMessage(msg);

        //check with other servers the tasks the received tasks
        //and if necessary change/update the receivedtasks file
        // only then insert the new tas

        //default values only to fill

        taskCounter++;
        List<String> receivedTask = new ArrayList<String>();
        receivedTask.add("audit");
        receivedTask.add(decipheredMessage.getCheckedName());
        receivedTask.add(Base64.getEncoder().encodeToString(decipheredMessage.getDestination().getEncoded()));

        messageManager.addTask(taskCounter, receivedTask);

        Ledger value = storage.readClient(clients.get(decipheredMessage.getDestination()));
        Message message = new Message(manager.getPublicKey(), value.getBalance(), value.getTransfers(), clients.get(decipheredMessage.getDestination()), lastWriteTimestamp);
        CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, decipheredMessage.getSender());
        return cipheredMessage;
    }

    @Override
    public CipheredMessage clientHasRead(CipheredMessage msg) throws RemoteException {
        Message decipheredMessage = manager.decipherCipheredMessage(msg);
        Ledger toBeUpdated = storage.readClient(clients.get(decipheredMessage.getCheckedKey()));
        if(decipheredMessage.isAudit())
            toBeUpdated.setPendingTransfers(decipheredMessage.getTransactions());
        else
            toBeUpdated.setTransfers(decipheredMessage.getTransactions());
        toBeUpdated.setBalance(decipheredMessage.getAmount());
        try {
            storage.writeClient(clients.get(decipheredMessage.getCheckedKey()), toBeUpdated);
            storage.writeClientBackup(clients.get(decipheredMessage.getCheckedKey()), toBeUpdated);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Message message = new Message(serverKeyPair.getPublic(), true, lastWriteTimestamp);
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
    
    
    
    
    
    
    public CipheredMessage readOperation(CipheredMessage msg) throws RemoteException {
    	return null;
    }
    public CipheredMessage readOperationConclusion(CipheredMessage msg) throws RemoteException {
    	return null;
    }
    public CipheredMessage writeOperation(CipheredMessage msg) throws RemoteException {
    	return null;
    }
    public void test(String test) {
    	System.out.println(nameServer+" receive: "+test);
    }
    
    
    
    
	//Authenticated Double-Echo Broadcast page 118 for register
	//CHANGED
	@Override
	public void sendEchoRegister(FunctionRegister register) throws RemoteException {
		if(!sentEchoRegister) {
			
			this.sentEchoRegister=true;
		}
		
	}
	@Override
	public void sendReadyRegister(FunctionRegister register) throws RemoteException {
		
		this.sentReadyRegister=true;
	}
	@Override
	public void sendDeliveryRegister(FunctionRegister register) throws RemoteException {
		
		this.sentReadyRegister=true;
	}
    


}
