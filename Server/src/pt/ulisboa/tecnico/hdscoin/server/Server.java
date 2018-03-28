package pt.ulisboa.tecnico.hdscoin.server;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.rmi.RemoteException;

import pt.ulisboa.tecnico.hdscoin.Crypto.CipheredMessage;
import pt.ulisboa.tecnico.hdscoin.Crypto.CryptoManager;
import pt.ulisboa.tecnico.hdscoin.Crypto.Message;
import pt.ulisboa.tecnico.hdscoin.interfaces.*;


public class Server implements RemoteServerInterface{

	public static final String SERVER_KEY = "30820122300d06092a864886f70d01010105000382010f003082010a0282010100c105187797a1ce79087657d825796562b2143fb7a4f8fd829996ede398f9f3c2103aaf4cba7d10e0322cbd938b8a07b8ac6978db1c23f7b1b609b3bdb41702633d97b064ba74b5498e3850ff01ef9b3b637d4af30ac579ea9f7123cb6e17c5c83751829617e7bbc7a1dc4400bb8d596524572ace113a49ba961bd749e5cb223dfe1a7c0e11799c0e38f59dff5b0e120c66672a079ae1c7c143f5c197d344f45d665dc744e119b837b4a7a10389dba9d7513dbc2e5115d99a5138947738a2895b3b87cb7b21d4637f61b5f0aeaaec7e8c15314e0d6c5d998ecd99bcb0562c1c94c0e956ca7466f9beaf0799bd108a3b468579ca40937747bc2e34a260774f32a50203010001";
	private HashMap<String, Ledger> ledgers;

	public Server() {
		ledgers=new HashMap<String, Ledger>();
	}
	
	public CipheredMessage sample(String pubkey, CipheredMessage msg) throws RemoteException{
		CryptoManager manager = new CryptoManager(SERVER_KEY, "passwd");
		//decipher
		Message decipheredMessage = manager.decipherCipheredMessage(msg);

		System.out.println("Sample starting\n"+msg+"\nSample ending");

		Message message = new Message(SERVER_KEY);
		CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, pubkey);
		return cipheredMessage;
	}

	public String register(String pubkey)
			throws RemoteException {
		
		if(ledgers.containsKey(pubkey)) {
			System.out.println("User already registered!");
			throw new RemoteException();
		}
		ledgers.put(pubkey, new Ledger(100, new ArrayList<String>(), new ArrayList<String>()));
		
		return SERVER_KEY;
	}

	//PublicKey source, PublicKey destination, int amount
	public CipheredMessage send(CipheredMessage msg)
			throws RemoteException {
		CryptoManager manager = new CryptoManager(SERVER_KEY, "passwd");
		//decipher
		Message decipheredMessage = manager.decipherCipheredMessage(msg);
		
		//TODO AES? Compare client keys?
		
		Ledger sender = (Ledger)ledgers.get(decipheredMessage.getSender());
		Message message = new Message(false);
		if(sender.sendBalance(decipheredMessage.getAmount())) {
			Ledger destiny = (Ledger)ledgers.get(decipheredMessage.getDestination());
			destiny.addPendingTransfers("Pending transfer:"+decipheredMessage.getAmount());
			message = new Message(true);
		}
		CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, decipheredMessage.getSender());
		return cipheredMessage;
		
	}


	public CipheredMessage check(CipheredMessage msg) throws RemoteException {
		CryptoManager manager = new CryptoManager(SERVER_KEY, "passwd");
		//decipher
		Message decipheredMessage = manager.decipherCipheredMessage(msg);
		
		//TODO AES? Compare client keys?
		
		Ledger value = (Ledger)ledgers.get(decipheredMessage.getSender());
		Message message = new Message(SERVER_KEY, value.getBalance(), value.getPendingTransfers());
		CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, decipheredMessage.getSender());
		return cipheredMessage;
	}


	public CipheredMessage receive(CipheredMessage msg) throws RemoteException {
		CryptoManager manager = new CryptoManager(SERVER_KEY, "passwd");
		//decipher
		Message decipheredMessage = manager.decipherCipheredMessage(msg);
		
		//TODO AES? Compare client keys?
		
		
		//TODO pending tranfers transaction
		Message message = new Message(false);
		if(true) {
			Ledger destiny = (Ledger)ledgers.get(decipheredMessage.getDestination());
			destiny.addTransfers("Transfer:"+decipheredMessage.getAmount());
			destiny.receiveBalance(decipheredMessage.getAmount());
			Ledger sender = (Ledger)ledgers.get(decipheredMessage.getSender());
			sender.addTransfers("Transfer:-"+decipheredMessage.getAmount());
			message = new Message(true);
		}
		CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, decipheredMessage.getSender());
		return cipheredMessage;
	}


	public CipheredMessage audit(CipheredMessage msg) throws RemoteException {
		CryptoManager manager = new CryptoManager(SERVER_KEY, "passwd");
		//decipher
		Message decipheredMessage = manager.decipherCipheredMessage(msg);
		
		//TODO AES? Compare client keys?
		
		Ledger value = (Ledger)ledgers.get(decipheredMessage.getSender());
		Message message = new Message(SERVER_KEY, value.getBalance(), value.getPendingTransfers());
		CipheredMessage cipheredMessage = manager.makeCipheredMessage(message, decipheredMessage.getSender());
		return cipheredMessage;
	}
	    

	
	
}
