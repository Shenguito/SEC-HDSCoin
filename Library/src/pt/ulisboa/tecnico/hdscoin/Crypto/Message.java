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
    //check && audit && register
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
    //register && receive && send
    public Message(boolean confirm){
        this.confirm = confirm;
    }

    //check && audit
    public Message(double amount, List<String> transactions){
        this.amount=amount;
        this.transactions=transactions;
    }






    @Override
    public String toString() {
        return amount + " from " + sender;
    }

}
