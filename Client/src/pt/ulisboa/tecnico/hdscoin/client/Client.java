package pt.ulisboa.tecnico.hdscoin.client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pt.ulisboa.tecnico.hdscoin.Crypto.CipheredMessage;
import pt.ulisboa.tecnico.hdscoin.Crypto.CryptoManager;
import pt.ulisboa.tecnico.hdscoin.Crypto.Message;
import pt.ulisboa.tecnico.hdscoin.interfaces.RemoteServerInterface;
import pt.ulisboa.tecnico.hdscoin.interfaces.Transaction;

public class Client {

	String CLIENT_KEY = "30820122300d06092a864886f70d01010105000382010f003082010a02820101008c360d4883c690da318613a8ecadf70b93c2e1f823e87eb418b3a120cada12f87e0d43ab6d6484143815d94d5b7565b95531f0e5418746f8d40c219d98ad39529feccb98e8bd18c858668696cdcbf7eb72cfe18daf7abd592898d98757f9069ca0cafc3415bab87f6c338fbb4504fbcbaa7271cd9c3a9131d119aeb2b3f418d37cee4a859885cb08284144f2934e6a20d3ba3a2912b3c944c3b61c97f9a39e4395b0252f162c873077148f4bdcf562fb1a5f3e2395d8c0f1d0bbc9709d291b66427748709434a0cb66e702e9c77006d8ddd5a7461ee135e6a30bf26defc668dcef0fbf50517c625cc0b3e5cc659f8dd6c8a1f0eb6b49aa9d44268c973f79d43d0203010001";
    String SERVER_KEY = "30820122300d06092a864886f70d01010105000382010f003082010a0282010100c105187797a1ce79087657d825796562b2143fb7a4f8fd829996ede398f9f3c2103aaf4cba7d10e0322cbd938b8a07b8ac6978db1c23f7b1b609b3bdb41702633d97b064ba74b5498e3850ff01ef9b3b637d4af30ac579ea9f7123cb6e17c5c83751829617e7bbc7a1dc4400bb8d596524572ace113a49ba961bd749e5cb223dfe1a7c0e11799c0e38f59dff5b0e120c66672a079ae1c7c143f5c197d344f45d665dc744e119b837b4a7a10389dba9d7513dbc2e5115d99a5138947738a2895b3b87cb7b21d4637f61b5f0aeaaec7e8c15314e0d6c5d998ecd99bcb0562c1c94c0e956ca7466f9beaf0799bd108a3b468579ca40937747bc2e34a260774f32a50203010001";
    
    private List<Transaction> pendingTransaction;
    
	private String host;
	public Client(String host) {
		this.host=host;
		pendingTransaction=new ArrayList<Transaction>();
	}
	public void sample() {
		try {

            Registry registry = LocateRegistry.getRegistry(host);
            RemoteServerInterface stub = (RemoteServerInterface) registry.lookup("RemoteServerInterface");
            
            CryptoManager manager = new CryptoManager(CLIENT_KEY, "passwd");
            Message msg = new Message(CLIENT_KEY);
            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, SERVER_KEY);

            CipheredMessage response = stub.sample(CLIENT_KEY, cipheredMessage);

            Message responseDeciphered = manager.decipherCipheredMessage(response);

            System.out.println("response: " + responseDeciphered);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
            System.out.println("Sample test failed!");
        }
		System.out.println("Sample test successed!");
	}
	public void register() {
		try {

            Registry registry = LocateRegistry.getRegistry(host);
            RemoteServerInterface serverInterface = (RemoteServerInterface) registry.lookup("RemoteServerInterface");
            
            CryptoManager manager = new CryptoManager(CLIENT_KEY, "passwd");
            Message msg = new Message(CLIENT_KEY);

            SERVER_KEY=serverInterface.register(CLIENT_KEY);
            
            System.out.println("You are registered!");
        } catch (Exception e) {
            System.out.println("You are already registered!");
        }
		
	}
	public void send(String sendDestination, String sendAmount) {
		try {

            Registry registry = LocateRegistry.getRegistry(host);
            RemoteServerInterface serverInterfaces = (RemoteServerInterface) registry.lookup("RemoteServerInterface");
            
            CryptoManager manager = new CryptoManager(CLIENT_KEY, "passwd");
            Message msg = new Message(Double.parseDouble(sendAmount.trim()), CLIENT_KEY, SERVER_KEY); //SERVER_key represents sendDestination
            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, SERVER_KEY);

            CipheredMessage response = serverInterfaces.send(cipheredMessage);

            Message responseDeciphered = manager.decipherCipheredMessage(response);

            System.out.println("Success: " + responseDeciphered.isConfirm());
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
            System.out.println("Unsuccess!");
        }
	}
	public void check() {
		
		try {

            Registry registry = LocateRegistry.getRegistry(host);
            RemoteServerInterface serverInterfaces = (RemoteServerInterface) registry.lookup("RemoteServerInterface");
            
            CryptoManager manager = new CryptoManager(CLIENT_KEY, "passwd");
            Message msg = new Message(CLIENT_KEY);
            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, SERVER_KEY);

            CipheredMessage response = serverInterfaces.check(cipheredMessage);

            Message responseDeciphered = manager.decipherCipheredMessage(response);

            System.out.println("You balance is: " + responseDeciphered.getAmount());
            if(responseDeciphered.getTransactions()!=null) {
            	if(responseDeciphered.getTransactions().size()==0) {
            		System.out.println("You do not have any pending transfer...");
            		return;
            	}
	            System.out.println("Your pending transfer(s) are:");
	            pendingTransaction=new ArrayList<Transaction>();
	            for(Transaction t:responseDeciphered.getTransactions()) {
	            	pendingTransaction.add(t);
	            	System.out.println(t.toString());
	            }
            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
		
	}
	
	public void receive(String receivedPendingTransfers) {
		try {
			
            Registry registry = LocateRegistry.getRegistry(host);
            RemoteServerInterface serverInterfaces = (RemoteServerInterface) registry.lookup("RemoteServerInterface");
            
            CryptoManager manager = new CryptoManager(CLIENT_KEY, "passwd");
            
            List <Transaction> pendingTransfers=new ArrayList<Transaction>();
            try {
	            for(String id:receivedPendingTransfers.split(" ")) {
	            	pendingTransfers.add(pendingTransaction.get(Integer.parseInt(id.trim())-1));
	            }
            }catch(Exception e) {
            	System.out.println("Input error... Try again! You input was '"+receivedPendingTransfers+"'!");
            }
            
            Message msg = new Message(CLIENT_KEY, pendingTransfers);
            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, SERVER_KEY);

            CipheredMessage response = serverInterfaces.receive(cipheredMessage);

            Message responseDeciphered = manager.decipherCipheredMessage(response);

            System.out.println("Success: " + responseDeciphered.isConfirm());
            for(Transaction transactionDone:pendingTransfers)
            	pendingTransaction.remove(transactionDone);
            
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
	}
	public void audit() {
		try {

            Registry registry = LocateRegistry.getRegistry(host);
            RemoteServerInterface serverInterfaces = (RemoteServerInterface) registry.lookup("RemoteServerInterface");
            
            CryptoManager manager = new CryptoManager(CLIENT_KEY, "passwd");
            Message msg = new Message(CLIENT_KEY);
            CipheredMessage cipheredMessage = manager.makeCipheredMessage(msg, SERVER_KEY);

            CipheredMessage response = serverInterfaces.audit(cipheredMessage);

            Message responseDeciphered = manager.decipherCipheredMessage(response);
            if(responseDeciphered.getTransactions()!=null) {
            	if(responseDeciphered.getTransactions().size()==0) {
            		System.out.println("You do not have done any transfer...");
            	}
	            System.out.println("Your transfer history:");
	            for(Transaction s:responseDeciphered.getTransactions()) {
	            	System.out.println(s.toString());
	            }
            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
		System.out.println("Your transfers history:");
	}
}
