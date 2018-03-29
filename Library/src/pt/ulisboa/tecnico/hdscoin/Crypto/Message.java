package pt.ulisboa.tecnico.hdscoin.Crypto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.hdscoin.interfaces.Transaction;

public class Message implements Serializable{

    private double amount;
    private String sender;
    private String destination;
    private List<Transaction> transactions;
    private boolean confirm;

    //Client
    //receive
    public Message(String sender, List<Transaction> transactions) {
        this.sender = sender;
        this.transactions = transactions;
    }
    //check && audit
    public Message(String sender){
        this.sender = sender;
    }
    //send
    public Message(double value, String sender, String destination){
        this.sender = sender;
        this.amount = value;
        this.destination = destination;
    }

    //Server
    //receive && send
    public Message(boolean confirm){
        this.confirm = confirm;
    }

   
	//check && audit
    public Message(String sender, double amount, List<Transaction> transactions){
        this.sender = sender;
        this.amount=amount;
        this.transactions=transactions;
    }

    public double getAmount() {
        return amount;
    }

    public String getSender() {
        return sender;
    }

    public String getDestination() {
		return destination;
	}
	public List<Transaction> getTransactions() {
		return transactions;
	}
	public boolean isConfirm() {
		return confirm;
	}


    @Override
    public String toString() {
        return amount + " from " + sender;
    }

}
