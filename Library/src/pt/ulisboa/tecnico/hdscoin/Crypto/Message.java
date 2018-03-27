package pt.ulisboa.tecnico.hdscoin.Crypto;

import java.io.Serializable;

public class Message implements Serializable{

    private String value;
    private String sender;


    public Message(String value, String sender){
        this.sender = sender;
        this.value = value;
    }

    public Message(String value, String sender, String ola){
        this.sender = sender;
        this.value = value;
    }

    @Override
    public String toString() {
        return value + " from " + sender;
    }

}
