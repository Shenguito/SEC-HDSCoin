package pt.ulisboa.tecnico.hdscoin.server;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.io.File;
import java.rmi.RemoteException;

import pt.ulisboa.tecnico.hdscoin.Crypto.CipheredMessage;
import pt.ulisboa.tecnico.hdscoin.Crypto.CryptoManager;
import pt.ulisboa.tecnico.hdscoin.Crypto.Message;
import pt.ulisboa.tecnico.hdscoin.interfaces.*;
import pt.ulisboa.tecnico.hdscoin.server.storage.*;



public class Server implements RemoteServerInterface{

	public static final String SERVER_KEY = "30820122300d06092a864886f70d01010105000382010f003082010a0282010100c105187797a1ce79087657d825796562b2143fb7a4f8fd829996ede398f9f3c2103aaf4cba7d10e0322cbd938b8a07b8ac6978db1c23f7b1b609b3bdb41702633d97b064ba74b5498e3850ff01ef9b3b637d4af30ac579ea9f7123cb6e17c5c83751829617e7bbc7a1dc4400bb8d596524572ace113a49ba961bd749e5cb223dfe1a7c0e11799c0e38f59dff5b0e120c66672a079ae1c7c143f5c197d344f45d665dc744e119b837b4a7a10389dba9d7513dbc2e5115d99a5138947738a2895b3b87cb7b21d4637f61b5f0aeaaec7e8c15314e0d6c5d998ecd99bcb0562c1c94c0e956ca7466f9beaf0799bd108a3b468579ca40937747bc2e34a260774f32a50203010001";
	
	private Storage storage;
 	private KeystoreManager keyPairManager;
 	private KeyPair serverKeyPair;
 	private CryptoManager manager;
 	
 	private HashMap<PublicKey, String> clients=new HashMap<PublicKey, String>();
 	
	public Server() {
		storage=new Storage();
		try {
			keyPairManager=new KeystoreManager("KeyStore"+File.separator+"server.ks", "server123");
			serverKeyPair=keyPairManager.getKeyPair("server", "server123");
			manager = new CryptoManager(serverKeyPair.getPublic(), serverKeyPair.getPrivate(), keyPairManager);
		}catch(Exception e) {
			System.out.println("KeyPair Error");
			e.printStackTrace();
		}
		
	}
	// TODO jackson can not save byte[] nor public key
	public PublicKey register(String clientName, PublicKey publickey) throws RemoteException {
		
		//System.out.println("Received client: "+clientName);
		if(!storage.checkFileExists(clientName)){
			storage.writeClient(clientName, new Ledger(100, new ArrayList<Transaction>(), new ArrayList<Transaction>()));
		}else {
			System.out.println("User already registered!");
		}
		if(clients.containsKey(clientName));
		clients.put(publickey, clientName);
		//System.out.println("Test reading "+clientName+" file:\n"+storage.readClient(clientName).toString());
		
		return manager.getPublicKey();
	}
	
	//PublicKey source, PublicKey destination, int amount
	public CipheredMessage send(CipheredMessage msg) throws RemoteException {
		
		//decipher
		System.out.println("Deciphering message");
		Message decipheredMessage = manager.decipherCipheredMessage(msg);
		
		
		Message message = new Message(serverKeyPair.getPublic(), false);//case the client does not exist
		if(storage.checkFileExists(clients.get(decipheredMessage.getSender()))){
			Ledger sender = storage.readClient(clients.get(decipheredMessage.getSender()));
			if(sender.sendBalance(decipheredMessage.getAmount())) {
				Ledger destiny = storage.readClient(clients.get(decipheredMessage.getDestination()));
				//Transaction with id for pending
				destiny.addPendingTransfers(new Transaction(destiny.getPendingTransfers().size()+1,clients.get(decipheredMessage.getSender()),clients.get(decipheredMessage.getDestination()),decipheredMessage.getAmount()));
				storage.writeClient(clients.get(decipheredMessage.getDestination()), destiny);
				storage.writeClient(clients.get(decipheredMessage.getSender()), sender);
				message = new Message(serverKeyPair.getPublic(), true);
			}
		}
		
		CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, decipheredMessage.getSender());
		return cipheredMessage;
		
	}


	public CipheredMessage check(CipheredMessage msg) throws RemoteException {
		//decipher
		Message decipheredMessage = manager.decipherCipheredMessage(msg);
		
		//TODO AES? Compare client keys?
		
		Message message = new Message(manager.getPublicKey(), 0.0, new ArrayList<Transaction>()); //case the client does not exist
		if(storage.checkFileExists(clients.get(decipheredMessage.getSender()))){
			Ledger value = storage.readClient(clients.get(decipheredMessage.getSender()));
			message = new Message(manager.getPublicKey(), value.getBalance(), value.getPendingTransfers());
		}
		CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, decipheredMessage.getSender());
		return cipheredMessage;
	}


	public CipheredMessage receive(CipheredMessage msg) throws RemoteException {
		//decipher
		Message decipheredMessage = manager.decipherCipheredMessage(msg);
		
		//TODO AES? Compare client keys?
		
		Message message = new Message(serverKeyPair.getPublic(), false);
		if(true) {//TODO pending tranfers transaction
			//decipheredMessage.getDestination()==null
			Ledger destiny = storage.readClient(clients.get(decipheredMessage.getSender()));
			List<Transaction> acceptedTransactions=decipheredMessage.getTransactions();
			//System.out.println("Ledger: "+destiny.toString());
			//System.out.println("Sender: "+decipheredMessage.getSender());
			
			Iterator<Transaction> i = destiny.getPendingTransfers().iterator();
			while (i.hasNext()) {
				Transaction t=i.next();
				for(Transaction accepted:decipheredMessage.getTransactions())
					if(accepted.myEquals(t)) {
						Ledger sender = storage.readClient(t.getSender());
						destiny.receiveBalance(t.getAmount());
						destiny.addTransfers(new Transaction(t.getSender(), t.getReceiver(), t.getAmount()));
						sender.addTransfers(new Transaction(t.getSender(), t.getReceiver(), t.getAmount()));
						//Write to file BUG
						i.remove();
						storage.writeClient(t.getSender(), sender);
						storage.writeClient(t.getReceiver(), destiny);
					}
			}
			
			message = new Message(serverKeyPair.getPublic(), true);
		}
		CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, decipheredMessage.getSender());
		return cipheredMessage;
	}


	public CipheredMessage audit(CipheredMessage msg) throws RemoteException {
		//decipher
		Message decipheredMessage = manager.decipherCipheredMessage(msg);
		
		//TODO AES? Compare client keys?
		
		Ledger value = storage.readClient(clients.get(decipheredMessage.getSender()));
		Message message = new Message(manager.getPublicKey(), value.getBalance(), value.getTransfers());
		CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, decipheredMessage.getSender());
		return cipheredMessage;
	}
	    
	
}
