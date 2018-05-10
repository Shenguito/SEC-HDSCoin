package pt.ulisboa.tecnico.hdscoin.client;


import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pt.ulisboa.tecnico.hdscoin.Crypto.CipheredMessage;
import pt.ulisboa.tecnico.hdscoin.Crypto.CryptoManager;
import pt.ulisboa.tecnico.hdscoin.Crypto.Message;
import pt.ulisboa.tecnico.hdscoin.interfaces.KeystoreManager;
import pt.ulisboa.tecnico.hdscoin.interfaces.RemoteServerInterface;
import pt.ulisboa.tecnico.hdscoin.interfaces.Transaction;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;


public class Client {


    private List<Transaction> pendingTransaction;

    private CryptoManager manager;
    private KeystoreManager keyPairManager;
    private KeyPair clientKeyPair;
    private String clientName;
    private HashMap<String, PublicKey> serversPublicKey = new HashMap<String, PublicKey>();
    //private PublicKey serverPublicKey;
    private boolean isReading = false;
    private long readID = 0;
    private boolean test;
    private int testAttack;
    private CountDownLatch readyThreadCounter = new CountDownLatch(3);

    private long writeTimestamp = -1;

    ExecutorService service = Executors.newFixedThreadPool(7);

    private List<RemoteServerInterface> servers;
    //private HashMap<RemoteServerInterface, PublicKey> servers;

    private String host;

    public Client(String host, String clientName, String password, boolean testMode, int testAttack) throws RemoteException, NotBoundException, MalformedURLException {
        this.host = host;
        this.clientName = clientName.toLowerCase().trim();
        this.test=testMode;
        this.testAttack=testAttack;
        servers = new ArrayList<RemoteServerInterface>();
        //servers = new HashMap<RemoteServerInterface, PublicKey>();
        connect();
        try {
            keyPairManager = new KeystoreManager("/" + clientName.trim().toLowerCase() + ".jks", password);
            clientKeyPair = keyPairManager.getKeyPair(clientName.trim().toLowerCase(), password);
            manager = new CryptoManager(clientKeyPair.getPublic(), clientKeyPair.getPrivate(), keyPairManager, testMode,testAttack);
        } catch (Exception e) {
            System.out.println("KeyPair Error");
            e.printStackTrace();
        }
        pendingTransaction = new ArrayList<Transaction>();
        System.out.println("Welcome " + clientName + "!");
    }

    public int numServers() {
        return servers.size();
    }

    private void connect() throws RemoteException, NotBoundException, MalformedURLException {
        if (host == null) {
            int numS = LocateRegistry.getRegistry(8000).list().length;
            for (int i = 0; i < numS; i++)
                servers.add((RemoteServerInterface) Naming.lookup(new String("//localhost:8000/" + "RemoteServerInterface" + (i + 1))));
            //servers.put((RemoteServerInterface) Naming.lookup(new String ("//localhost:8000/"+"RemoteServerInterface" + (i + 1))), manager.getPublicKeyBy("Server"+(i + 1)));
        } else {
            int numS = LocateRegistry.getRegistry(8000).list().length;
            for (int i = 0; i < numS; i++)
                servers.add((RemoteServerInterface) Naming.lookup(new String("//" + host + ":8000/" + "RemoteServerInterface" + (i + 1))));
        }
    }

    public boolean register() {
    	int registerValue=0;

        for (int i = 0; i < numServers(); i++) {
        	final int index=i;
        	service.execute(() -> {
            try {
                if(servers.get(index).register(clientName, manager.getPublicKey())){
                	readyThreadCounter.countDown();
                	System.out.println("countdown");
                }
                try {
                	serversPublicKey.put("server"+(index+1), manager.getPublicKeyBy("server"+(index+1)));
                } catch (Exception e) {
                    System.out.println("publickey error");
                    e.printStackTrace();
                }

                System.out.println("You are registered by server[" + (index+1) + "]");

            } catch (RemoteException e) {
                System.out.println("Connection fail...");
                System.out.println("Server[" + (index+1) + "] connection failed");
            } catch(Exception e1){
                e1.printStackTrace();
            	System.out.println("Exception1: "+e1);
            }
        	});
        }
        try {
        	System.out.println("Waiting");
			readyThreadCounter.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

        return true;
    }

    public synchronized boolean send(String sendDestination, String sendAmount) {
        System.out.println("BLA");
        if (getClientName().toUpperCase().equals(sendDestination.toUpperCase())) {
            System.out.println("'" + sendDestination + "'? There is a bit probability being you, don't try to send money to yourself ;)");
            return true;
        }
        try {

            if(test && testAttack==4) {
                System.out.println("Setting timestamp to 0");
                writeTimestamp = 0;
            }


            final ConcurrentHashMap<String, Message> acklist = new ConcurrentHashMap<>();
            final ConcurrentHashMap<String, Message> failedacklist = new ConcurrentHashMap<>();
            writeTimestamp++;
            Message msg = new Message(Double.parseDouble(sendAmount.trim()), manager.getPublicKey(), keyPairManager.getPublicKeyByName(sendDestination), writeTimestamp); //SERVER_key represents sendDestination
            if (serversPublicKey.size() > numServers())
                System.out.println("I didn't received publickey for all server");

            for (int i = 0; i < numServers(); i++) {
            	final CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serversPublicKey.get("server"+(i+1)));
                final int index = i;
                service.execute(() -> {
                            try {
                                CipheredMessage response = servers.get(index).send(cipheredMessage);

                                Message responseDeciphered = manager.decipherCipheredMessage(response);
                                if (responseDeciphered.isConfirm()) acklist.putIfAbsent("" + index, responseDeciphered);
                                else failedacklist.putIfAbsent("" + index, responseDeciphered);
                                System.out.println("Success from server " + (index + 1) + ": " + responseDeciphered.isConfirm());
                            } catch (RemoteException e) {
                                System.out.println("Connection fail...");

                            } catch (IllegalStateException e) {
                                System.out.println("Invalid signature");

                            }
                        }
                );
            }
            while (!(acklist.keySet().size() > (numServers() + 2) / 2) && !(failedacklist.keySet().size() > (numServers() + 2) / 2)) {
            }
            if(acklist.keySet().size() > (numServers() + 2) / 2) {
                System.out.println("SUCCESS");
                return true;
            } else {
                System.out.println("FAILURE");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Invalid message");
            return false;
        }
    }

    public boolean check(String sendDestination) {
        final StringBuilder checkedName = new StringBuilder();
        final Map<Integer, List<Transaction>> transactions = new HashMap<Integer, List<Transaction>>();
        final ConcurrentHashMap<String, Message> readList = new ConcurrentHashMap<>();
        final ConcurrentHashMap<String, CipheredMessage> readListCiphers = new ConcurrentHashMap<>();
        readID++;
        

        try {
            Message msg = new Message(manager.getPublicKey(), keyPairManager.getPublicKeyByName(sendDestination), readID);
            for (int i = 0; i < numServers(); i++) {
            	final CipheredMessage cipheredMessage;
            	if(test && testAttack==3) {
            		cipheredMessage = manager.makeCipheredMessage(msg, serversPublicKey.get("server"+1));
            	}
            	else
            		cipheredMessage = manager.makeCipheredMessage(msg, serversPublicKey.get("server"+(i+1)));
                final int index = i;
                service.execute(() -> {
                    try {
                        CipheredMessage response = servers.get(index).check(cipheredMessage);
                        Message responseDeciphered = manager.decipherCipheredMessage(response);
                        //TODO Last message? or a array of message?
                        readList.putIfAbsent("" + index, responseDeciphered);
                        readListCiphers.putIfAbsent("" + index, response);
                        checkedName.replace(0, responseDeciphered.getCheckedName().length(), responseDeciphered.getCheckedName());
                        if (checkedName.toString().equals("")) {        //no user exist
                            throw new Exception();
                        }
                        if (responseDeciphered.getTransactions() != null && clientName.equals(checkedName.toString())) {
                            pendingTransaction = new ArrayList<Transaction>();
                            pendingTransaction.addAll(responseDeciphered.getTransactions());
                            if (pendingTransaction.size() != 0)
                                transactions.put(index, pendingTransaction);
                        }

                    } catch (RemoteException e) {
                        //TODO fix connection bug
                        System.out.println("Connection fail...");
                        e.printStackTrace();

                    } catch (IllegalStateException e) {
                        System.out.println("Invalid signature");

                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                });


            }
            while (!(readList.keySet().size() > (numServers() + 2) / 2)) {
            }
            return enforceCheck(checkedName, readList, readListCiphers, msg, false);
        } catch (Exception e) {
            System.out.println("Invalid message");
            return false;
        }
    }


    private boolean enforceCheck(StringBuilder checkedName, ConcurrentHashMap<String, Message> readList, ConcurrentHashMap<String, CipheredMessage> readListCiphers, Message msg, boolean isAudit) {
        System.out.println("Enforcing Read");
        String highestValKey = readList.entrySet().stream().max(Comparator.comparing(x -> x.getValue().getTimestamp())).get().getKey();
        Message highestVal = readList.get(highestValKey);
        CipheredMessage highestValCipher = readListCiphers.get(highestValKey);
        if(highestVal.getCheckedName().equals("")) return false;
        if(highestVal.getCheckedName().equals(clientName)) writeTimestamp = highestVal.getTimestamp();
        final ConcurrentHashMap<String, Message> acklist = new ConcurrentHashMap<>();
        Message newMsg = null;
        try {
            newMsg = new Message(clientKeyPair.getPublic(), highestVal, highestVal.getSender(), checkedName.toString(), isAudit, manager.decipherIntegrityCheck(highestValCipher), highestValCipher.getIV());
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (int i = 0; i < numServers(); i++) {
        	final CipheredMessage newCipheredMessage = manager.makeCipheredMessage(newMsg, serversPublicKey.get("server"+(i+1)));
            final int index = i;
            service.execute(() ->
            {
                try {
                    CipheredMessage response = servers.get(index).clientHasRead(newCipheredMessage);
                    Message responseDeciphered = manager.decipherCipheredMessage(response);
                    System.out.println("Server was outdated? " + (index + 1) + ": " + responseDeciphered.isConfirm());
                    acklist.put("" + index, responseDeciphered);
                } catch (ClassNotFoundException | BadPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | IOException | NoSuchPaddingException | IllegalBlockSizeException e) {
                    e.printStackTrace();
                }
            });
        }
        //not necessary... since you obtained always a response by RMI
        //if you don't get a response by server you get a exception.
        while (!(acklist.keySet().size() > (numServers() + 2) / 2)) {
        }
        if (!isAudit) {
            System.out.println(checkedName + "'s balance is: " + highestVal.getAmount());
            if (pendingTransaction.size() == 0 && clientName.equals(checkedName))
                System.out.println(checkedName + " has no pending transfer...");
            else {
                System.out.println(checkedName + "'s pending transfer(s) are:");
                int id = 0;
                for (Transaction t : pendingTransaction) {
                    id++;
                    System.out.println("id " + id + ": \t" + t.toString());
                }
            }
        } else {
            if(highestVal.getTransactions().size() > 0){
                System.out.println(checkedName + "'s transactions: ");
                int id = 0;
                for (Transaction t : highestVal.getTransactions()) {
                    id++;
                    System.out.println("id " + id + ": \t" + t.toString());
                }
            } else {
                System.out.println("No previous transactions");
            }
        }
        return true;
    }

    public boolean receive(int receivedPendingTransfers) {

        if (pendingTransaction.size() == 0) {
            System.out.println("You do not have any pending transaction. Make a check first...");
            return false;
        }

        final ConcurrentHashMap<String, Message> acklist = new ConcurrentHashMap<>();
        writeTimestamp++;
        try {
            int index = receivedPendingTransfers - 1;
            Transaction receiveTransaction = pendingTransaction.get(index);

            Message msg = new Message(manager.getPublicKey(), receiveTransaction, writeTimestamp);

            for (int i = 0; i < numServers(); i++) {

            	final CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serversPublicKey.get("server"+(i+1)));
                final int for_index = i;
                service.execute(() -> {
                    try {
                        CipheredMessage response = servers.get(for_index).receive(cipheredMessage);
                        Message responseDeciphered = manager.decipherCipheredMessage(response);
                        if (responseDeciphered.isConfirm()) acklist.putIfAbsent("" + for_index, responseDeciphered);
                        System.out.println("Success from server " + (for_index + 1) + ": " + responseDeciphered.isConfirm());
                    } catch (RemoteException e) {
                        System.out.println("Connection fail...");
                    } catch (IllegalStateException e) {
                        System.out.println("Illegal State Exception Invalid signature");

                    }
                });
            }
            while (!(acklist.keySet().size() > (numServers() + 2) / 2)) {
            }
            System.out.println("SUCCESS");
            return true;
        } catch (Exception e) {
            System.out.println("Invalid message");
            return false;
        }
    }

    public boolean audit(String sendDestination) {
        final StringBuilder name = new StringBuilder();
        Map<Integer, List<Transaction>> transactions = new HashMap<Integer, List<Transaction>>();
        final ConcurrentHashMap<String, Message> readList = new ConcurrentHashMap<>();
        final ConcurrentHashMap<String, CipheredMessage> readListCiphers = new ConcurrentHashMap<>();
        readID++;
        try {
            Message msg = new Message(manager.getPublicKey(), keyPairManager.getPublicKeyByName(sendDestination), readID);
            for (int i = 0; i < numServers(); i++) {
            	final CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, serversPublicKey.get("server"+(i+1)));
                final int index = i;
                service.execute(() -> {
                    try {
                        CipheredMessage response = servers.get(index).audit(cipheredMessage);
                        Message responseDeciphered = manager.decipherCipheredMessage(response);
                        name.replace(0, responseDeciphered.getCheckedName().length(), responseDeciphered.getCheckedName());
                        readList.putIfAbsent("" + index, responseDeciphered);
                        readListCiphers.putIfAbsent("" + index, response);
                        if (responseDeciphered.getTransactions() != null) {
                            if (responseDeciphered.getTransactions().size() != 0) {
                                transactions.put(index, responseDeciphered.getTransactions());
                            }
                        }
                    } catch (RemoteException e) {
                        System.out.println("Connection fail...");

                    } catch (IllegalStateException e) {
                        System.out.println("Invalid signature");

                    } catch (NoSuchAlgorithmException | IOException | InvalidKeySpecException e) {
                        e.printStackTrace();
                    }
                });
            }

            while (!(readList.keySet().size() > (numServers() + 2) / 2)) {
            }
            enforceCheck(name, readList, readListCiphers, msg, true);


        } catch (Exception e) {
            System.out.println("Invalid message");
            return false;
        }
        return true;
    }

    public String getClientName() {
        return clientName;
    }


    public void removePendingTransaction() {
        pendingTransaction = new ArrayList<Transaction>();
    }
    
    /*public void setServerByzantine(boolean mode) {
    	servers.get(0).setByzantine(mode);
    }*/



    public boolean decipherCaughtMsg(CipheredMessage msg){
        Message responseDeciphered = manager.decipherCipheredMessage(msg);
        if(responseDeciphered == null) return false;
        return true;
    }

    public CipheredMessage createTestMsg(String to){
        try {
            Message msg = new Message(Double.parseDouble("1"), manager.getPublicKey(), keyPairManager.getPublicKeyByName(to), writeTimestamp);
            return manager.makeCipheredMessage(msg, keyPairManager.getPublicKeyByName(to));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
