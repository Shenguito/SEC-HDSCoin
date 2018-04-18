package pt.ulisboa.tecnico.hdscoin.interfaces;

import pt.ulisboa.tecnico.hdscoin.Crypto.IntegrityCheck;

import java.io.Serializable;
import java.security.PublicKey;


public class Transaction implements Serializable{
	private String sender;
	private String receiver;
	private double amount;
	//TODO save integrityCheck
	private IntegrityCheck digitalSign;
	
	public Transaction() {
		
	}

	//TODO HASH (sender || receiver || amount) encrypted with client private key.
	public Transaction(String sender, String receiver, double amount, IntegrityCheck digitalSign) {
		this.sender = sender;
		this.receiver = receiver;
		this.amount = amount;
		this.digitalSign=digitalSign;
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
	public IntegrityCheck getIntegrityCheck() {
		return digitalSign;
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