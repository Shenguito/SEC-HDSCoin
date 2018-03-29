package pt.ulisboa.tecnico.hdscoin.server.storage;

import java.io.Serializable;

public class Transaction implements Serializable{
	private int id;
	private String sender;
	private String receiver;
	private double amount;
	public Transaction() {
		
	}
	public Transaction(int id, String sender, String receiver, double amount) {
		this.id=id;
		this.sender = sender;
		this.receiver = receiver;
		this.amount = amount;
	}
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
	
}
