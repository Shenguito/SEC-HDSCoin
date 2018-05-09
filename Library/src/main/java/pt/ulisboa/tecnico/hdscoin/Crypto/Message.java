package pt.ulisboa.tecnico.hdscoin.Crypto;

import pt.ulisboa.tecnico.hdscoin.interfaces.BroadcastMessage;
import pt.ulisboa.tecnico.hdscoin.interfaces.Transaction;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.List;


public class Message implements Serializable{

    private byte[] IV;
    private IntegrityCheck originalSig;
    private PublicKey serverSender;

    public BroadcastMessage getBcm() {
		return bcm;
	}

	private double amount;
    private PublicKey sender;
    private PublicKey destination;
    private PublicKey checkedKey;
    private List<Transaction> transactions;
    private Transaction transaction;
    private boolean confirm;
    private String checkedName;
    private long timestamp;
    private boolean isAudit;
    private BroadcastMessage bcm;
    private Message original;

    //Client
    //receive
    public Message(PublicKey sender, Transaction transaction, long timestamp) {
        this.sender = sender;
        this.timestamp = timestamp;
        this.transaction = transaction;
    }
    //check && audit
    public Message(PublicKey sender, PublicKey destination, long timestamp){
        this.sender = sender;
        this.timestamp = timestamp;
        this.destination=destination;
    }
    //send
    public Message(double value, PublicKey sender, PublicKey destination, long timestamp){
        this.sender = sender;
        this.timestamp = timestamp;
        this.amount = value;
        this.destination = destination;
    }

    //Server
    //receive && send
    public Message(PublicKey sender, boolean confirm, long timestamp){
        this.confirm = confirm;
        this.timestamp = timestamp;
        this.sender = sender;
    }

   
	//check && audit
    public Message(PublicKey sender, double amount, List<Transaction> transactions, PublicKey checkedKey, String checkedName, long timestamp){
        this.sender = sender;
        this.timestamp = timestamp;
        this.amount=amount;
        this.transactions=transactions;
        this.checkedName=checkedName;
        this.checkedKey = checkedKey;
    }


    public Message(PublicKey sender, Message original, PublicKey checkedKey, String checkedName, boolean isAudit, IntegrityCheck check, byte[] IV){
        this.sender = sender;
        this.timestamp = timestamp;
        this.amount=amount;
        this.transactions=transactions;
        this.checkedKey = checkedKey;
        this.checkedName=checkedName;
        this.original = original;
        this.isAudit = isAudit;
        this.originalSig = check;
        this.IV = IV;
    }

    public byte[] getIV() {
        return IV;
    }

    public IntegrityCheck getOriginalSig() {
        return originalSig;
    }



    public Message getOriginal() {
        return original;
    }

    public Message(PublicKey sender, PublicKey destination, BroadcastMessage bcm) {
    	this.sender=sender;
    	this.destination=destination;
    	this.bcm=bcm;
    }

    public boolean isAudit() {
        return isAudit;
    }

    public PublicKey getServerSender() {
        return serverSender;
    }

    public PublicKey getCheckedKey() {
        return checkedKey;
    }

    public double getAmount() {
        return amount;
    }

    public PublicKey getSender() {
        return sender;
    }

    public PublicKey getDestination() {
		return destination;
	}
	public List<Transaction> getTransactions() {
		return transactions;
	}
	public boolean isConfirm() {
		return confirm;
	}
	public String getCheckedName(){
		return checkedName;
	}
	public Transaction getTransaction(){
		return transaction;
	}


    @Override
    public String toString() {
        return amount + " from " + sender;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
