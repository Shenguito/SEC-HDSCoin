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
	//Transaction with id for pending transaction!
	public Transaction(int id, String sender, String receiver, double amount) {
		this.id=id;
		this.sender = sender;
		this.receiver = receiver;
		this.amount = amount;
	}
	//Transaction without id for transaction done!
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
		if(id!=0){
			return "id: "+getId()+"\tSender:"+getSender()+
					"\tReceiver:"+getReceiver()+
					"\tAmount:"+getAmount()+".";
		}
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
