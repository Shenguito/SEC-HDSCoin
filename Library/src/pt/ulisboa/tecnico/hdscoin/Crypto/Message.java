package pt.ulisboa.tecnico.hdscoin.Crypto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Message implements Serializable{

    private double amount;
    private String sender;
    private String destination;
    private List<String> transactions;
    private boolean confirm;

    //Client
    //receive
    public Message(String sender, List<String> transactions) {
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
    public Message(String sender, double amount, List<String> transactions){
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
	public List<String> getTransactions() {
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
