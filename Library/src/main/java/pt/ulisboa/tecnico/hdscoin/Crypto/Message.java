package pt.ulisboa.tecnico.hdscoin.Crypto;

import pt.ulisboa.tecnico.hdscoin.interfaces.Transaction;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;


public class Message implements Serializable{

    private double amount;
    private PublicKey sender;
    private PublicKey destination;
    private List<Transaction> transactions;
    private Transaction transaction;
    private boolean confirm;
    private String checkedName;

    //Client
    //receive
    public Message(PublicKey sender, Transaction transaction) {
        this.sender = sender;
        this.transaction = transaction;
    }
    //check && audit
    public Message(PublicKey sender, PublicKey destination){
        this.sender = sender;
        this.destination=destination;
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
    public Message(PublicKey sender, double amount, List<Transaction> transactions, String checkedName){
        this.sender = sender;
        this.amount=amount;
        this.transactions=transactions;
        this.checkedName=checkedName;
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

}
