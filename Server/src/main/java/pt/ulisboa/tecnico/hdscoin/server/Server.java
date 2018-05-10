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



	private ExecutorService service = Executors.newFixedThreadPool(7);
	



	private List<BroadcastMessage> broadcastMessage = Collections.synchronizedList(new ArrayList<BroadcastMessage>());
	private List<Message> broadcastMessageDelivery = Collections.synchronizedList(new ArrayList<Message>());


	//private HashMap<String, CountDownLatch> clientCountDown=new HashMap<String, CountDownLatch>();
	//private CountDownLatch echoCountDown = new CountDownLatch(3); 		//[(N+f)/2]+1
	//private CountDownLatch deliveryCountDown = new CountDownLatch(3);	//2f+1



    private Storage storage;
    private KeystoreManager keyPairManager;
    private KeyPair serverKeyPair;
    private CryptoManager manager;
    private String myServerName;
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
    	myServerName = "server" + number;
    	serverNumber=number;
    	totalServerNumber=totalServer;
    	servers = new ArrayList<RemoteServerInterface>();
    	serversPublicKey = new HashMap<String, PublicKey>();
        storage = new Storage(myServerName);
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


    
    public void register(PublicKey publickey) throws RemoteException {
    	
    	if (isServerCrashed())
            throw new RemoteException();
    	String clientName=manager.getNamebyAlias(publickey);


        if (!storage.checkFileExists(clientName)) {
            try {
                Ledger ledger = new Ledger(publickey, 100, new ArrayList<Transaction>(), new ArrayList<Transaction>());
                storage.writeClient(clientName, ledger);
                storage.writeClientBackup(clientName, ledger);
                //Confirm write and read

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

        //Broadcast
        broadcastEcho(manager.getDigitalSign(msg), decipheredMessage);


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
                    System.out.println(serverNumber + " is byzantine:    " + byzantine);
                    if(!byzantine)
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
        broadcastEcho(manager.getDigitalSign(msg), decipheredMessage);




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
        broadcastEcho(manager.getDigitalSign(msg), decipheredMessage);



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
        broadcastEcho(manager.getDigitalSign(msg), decipheredMessage);








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

    private void broadcastEcho(IntegrityCheck integrityCheck, Message msg) {
        final BroadcastMessage checkBroadcast=new BroadcastMessage(integrityCheck, totalServerNumber);
        //System.out.println("Digital signature stored: "+integrityCheck.getStringDigitalSignature());
        echoSelf(checkBroadcast, msg);
        //msg is not comparable

        boolean exist=false;
        while(!exist) {
			for(Message bcmDelivery : broadcastMessageDelivery) {
				if(bcmDelivery!=null && checkBroadcast!=null &&
						bcmDelivery.getBcm().getStringDigitalsign().equals(checkBroadcast.getStringDigitalsign())) {
					exist=true;
				}
			}
			try {
				Thread.sleep(1000);
				//System.out.println("test: "+broadcastMessageDelivery.size());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
    }





    private void echoSelf(BroadcastMessage checkEchoBroadcast, Message message){
    	if(!broadcastMessage.stream().map(BroadcastMessage::getDigitalsign).filter(checkEchoBroadcast.getDigitalsign()::equals).findFirst().isPresent()) {
			checkEchoBroadcast.echoServer(myServerName);
			broadcastMessage.add(checkEchoBroadcast);
			for (int i = 0; i < servers.size(); i++) {
	    		if((i+1)==serverNumber) { // it does not self send, above 4 lines already did it
	    			continue;
	    		}
	    		final int index=i;
	    		service.execute(() -> {
		    		try {
		    			//serversPublicKey.get("server"+(index))==serversPublicKey.get("server"+(index)) --> printed
		    			Message msg=new Message(message, manager.getPublicKey(), manager.getPublicKeyBy("server"+(index+1)), checkEchoBroadcast);
	        			final CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, manager.getPublicKeyBy("server"+(index+1)));

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






	//Message broadcast
    public void echoBroadcast(CipheredMessage msg) throws RemoteException {
    	Message decipheredMessage = manager.decipherCipheredMessage(msg);
    	BroadcastMessage bcm=decipheredMessage.getBcm();
    	//if there is no BroadcastMessage, then add and broadcast

    	if(!broadcastMessage.stream().map(BroadcastMessage::getStringDigitalsign).filter(bcm.getStringDigitalsign()::equals).findFirst().isPresent()) {
    		//ATTENTION, server x does not store his publickey in his map
    		System.out.println("enter if "+myServerName+"\n"+manager.getPublicKey());
    		if(decipheredMessage.getDestination().equals(manager.getPublicKey())){
				BroadcastMessage tmp=new BroadcastMessage(bcm.getDigitalsign(), totalServerNumber);
				for(String s:serversPublicKey.keySet())
					if(serversPublicKey.get(s).equals(decipheredMessage.getSender())){
						tmp.echoServer(s);
						broadcastMessage.add(tmp);
						// here server won't never broadcast ready since it is first message
					}

			}
    	//else it only turn it true
    	}else {
    		//Compare boolean,
    		//if there is a server that becomes true and in my list it isn't
    		System.out.println("enter else1 "+myServerName+"\n"+manager.getPublicKey());
    		for(int i=0;i<broadcastMessage.size();i++)
	    		if(bcm.getStringDigitalsign().equals(broadcastMessage.get(i).getStringDigitalsign())) {
	    			System.out.println("enter else2 "+myServerName+"\n"+manager.getPublicKey());
	    			for(String s:serversPublicKey.keySet()) { // echoServer, which means server echo received
	    				if(serversPublicKey.get(s).equals(decipheredMessage.getSender())&&
	    						!broadcastMessage.get(i).serverEchoed(s)){
	    					System.out.println("enter else3 "+myServerName+"\n"+manager.getPublicKey());
	    					broadcastMessage.get(i).echoServer(s);

	    					//if this message broadcastMessageEcho.get(i).echoServerReceived()>(n+f) / 2;
	    					//and if this server is really delivered message
	    					if(broadcastMessage.get(i).echoServerReceived()>((totalServerNumber+1)/2)) {
	    						//TODO readyself
	    						//readySelf(broadcastMessage.get(i), requestMessageEcho.get(broadcastMessage.get(i).getStringDigitalsign()));
	    						System.out.println("enter else4 "+myServerName+"\n"+manager.getPublicKey());
	    						broadcastMessage.get(i).readyServer(myServerName);

	    						BroadcastMessage checkbcm=broadcastMessage.get(i);

	    						for (int j = 0; j < servers.size(); j++) {
	    				    		if((i+1)==serverNumber) { // it does not self send, above 4 lines already did it
	    				    			continue;
	    				    		}
	    				    		final int index=j;
	    				    		service.execute(() -> {
	    					    		try {
	    					    			//serversPublicKey.get("server"+(index))==serversPublicKey.get("server"+(index)) --> printed
	    					    			Message tmpmsg=new Message(decipheredMessage.getMessage(), manager.getPublicKey(), manager.getPublicKeyBy("server"+(index+1)), checkbcm);
	    				        			CipheredMessage cipheredMessage = manager.makeCipheredMessage(tmpmsg, manager.getPublicKeyBy("server"+(index+1)));
	    				        			//System.out.println("server"+(index+1)+"\n"+serversPublicKey.get("server"+(index+1)));
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
	    			}
	    		}
    	}

    }
    public void readyBroadcast(CipheredMessage msg) throws RemoteException {

    	Message decipheredMessage = manager.decipherCipheredMessage(msg);
    	BroadcastMessage bcm=decipheredMessage.getBcm();
    	//if there is no BroadcastMessage, then add and broadcast
    	if(!broadcastMessage.stream().map(BroadcastMessage::getStringDigitalsign).filter(bcm.getStringDigitalsign()::equals).findFirst().isPresent()) {
    		//ATTENTION, server x does not store his publickey in his map (HashMap<String, PublicKey> serversPublicKey)
    		if(decipheredMessage.getDestination().equals(manager.getPublicKey())){
				BroadcastMessage tmp=new BroadcastMessage(bcm.getDigitalsign(), totalServerNumber);
				for(String s:serversPublicKey.keySet())
					if(serversPublicKey.get(s).equals(decipheredMessage.getSender())){
						tmp.readyServer(s);
						broadcastMessage.add(tmp);
						// do nothing, this is first message
					}

			}
    	//else it only turn it true
    	}else {
    		//Compare boolean,
    		//if there is a server that becomes true and in my list it isn't
    		for(int i=0;i<broadcastMessage.size();i++)
	    		if(bcm.getStringDigitalsign().equals(broadcastMessage.get(i).getStringDigitalsign())) {
	    			for(String s:serversPublicKey.keySet())
	    				if(serversPublicKey.get(s).equals(decipheredMessage.getSender())&&
	    						!broadcastMessage.get(i).serverReadied(s)){
	    					broadcastMessage.get(i).readyServer(s);
						}

	    			// delivery in case >2f
	    			if(broadcastMessage.get(i).readyServerReceived()>2&&
	    					broadcastMessage.get(i).serverReadied(myServerName)) {
	    				if(broadcastMessageDelivery.size()==0) {
	    					broadcastMessageDelivery.add(decipheredMessage);
	    					System.out.println("Primeira entrega no server: "+myServerName);
	    					continue;
	    				}
	    				boolean exist=false;
	    				for(Message bcmDelivery : broadcastMessageDelivery) {
	    					if(bcmDelivery.getBcm().getStringDigitalsign().equals(broadcastMessage.get(i).getStringDigitalsign())) {
	    						exist=true;
	    					}
	    				}
	    				if(!exist) {
	    					broadcastMessageDelivery.add(decipheredMessage);
	    					System.out.println("Entregue no server: "+myServerName);
	    				}

	    			// broadcast ready in case >f
	    			}else if(broadcastMessage.get(i).readyServerReceived()>1&&
	    					!broadcastMessage.get(i).serverReadied(myServerName)) {
	    				broadcastMessage.get(i).readyServer(myServerName);
	    				BroadcastMessage tmp=new BroadcastMessage(bcm.getDigitalsign(), totalServerNumber);
	    				tmp.readyServer(myServerName);
	    				for (int j = 0; j < servers.size(); j++) {
	    		    		if((j+1)==serverNumber) {
	    		    			continue;
	    		    		}
	    		    		final int index=j;
	    					service.execute(() -> {
	    						try {
	    			    			Message msg2=new Message(decipheredMessage.getMessage(), manager.getPublicKey(), manager.getPublicKeyBy("server"+(index+1)), tmp);
	    		        			CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg2, manager.getPublicKeyBy("server"+(index+1)));

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

    	}
    }

	public void setByzantine(boolean mode) {
		byzantine = mode;
		
	}
    

}
