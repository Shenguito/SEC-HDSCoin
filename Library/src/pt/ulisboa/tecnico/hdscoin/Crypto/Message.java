package pt.ulisboa.tecnico.hdscoin.crypto;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.hdscoin.interfaces.Transaction;

public class Message implements Serializable{

    private double amount;
    private PublicKey sender;
    private PublicKey destination;
    private List<Transaction> transactions;
    private boolean confirm;

    //Client
    //receive
    public Message(PublicKey sender, List<Transaction> transactions) {
        this.sender = sender;
        this.transactions = transactions;
    }
    //check && audit
    public Message(PublicKey sender){
        this.sender = sender;
    }
    //send
    public Message(double value, PublicKey sender, PublicKey destination){
        this.sender = sender;
        this.amount = value;
        this.destination = destination;
    }

    //Server
    //receive && send
    public Message(PublicKey sender, boolean confirm){
        this.confirm = confirm;
        this.sender = sender;
    }

   
	//check && audit
    public Message(PublicKey sender, double amount, List<Transaction> transactions){
        this.sender = sender;
        this.amount=amount;
        this.transactions=transactions;
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


    @Override
    public String toString() {
        return amount + " from " + sender;
    }

}
