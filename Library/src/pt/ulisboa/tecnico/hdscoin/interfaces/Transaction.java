package pt.ulisboa.tecnico.hdscoin.interfaces;

import java.io.Serializable;
import java.security.PublicKey;

public class Transaction implements Serializable{
	private int id=0;
	private String sender;
	private String receiver;
	private double amount;
	public Transaction() {
		
	}

	//Transaction without id for transaction done!
	//TODO HASH (sender || receiver || amount) encrypted with client private key.
	public Transaction(String sender, String receiver, double amount) {
		this.sender = sender;
		this.receiver = receiver;
		this.amount = amount;
	}
	
	public int getId() {
		return id;
	}
	public String getSender() {
		return sender;
	}
	public String getReceiver() {
		return receiver;
	}
	public double getAmount() {
		return amount;
	}
	@Override
	public String toString() {
		
		return "Sender:"+getSender()+
				"\tReceiver:"+getReceiver()+
				"\tAmount:"+getAmount()+".";
	}
	

	public boolean myEquals(Transaction obj) {
		if(sender.equals(obj.getSender())&&
				receiver.equals(obj.getReceiver())&&
				amount==obj.getAmount())
			return true;
	    return false;
	}
}
