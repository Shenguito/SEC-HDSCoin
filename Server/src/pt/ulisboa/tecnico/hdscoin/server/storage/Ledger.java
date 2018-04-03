package pt.ulisboa.tecnico.hdscoin.server.storage;

import java.io.Serializable;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import pt.ulisboa.tecnico.hdscoin.interfaces.Transaction;

/* 
 * This class should have synchronization in consideration
*/

public class Ledger implements Serializable{
	
	//TODO JACKSON BUG
	//private byte[] publickey;
	//private PublicKey publickey;
	
	@JsonProperty("publicKey")
	private String publickey;
	private double balance;
	private List<Transaction> transfers;
	private List<Transaction> pendingTransfers;
	public Ledger() {
		
	}
	/*
	public Ledger(double balances, List<Transaction> transfers, List<Transaction> pendingTransfers) {
		this.publickey="/r+0";
		this.balance = balances;
		this.transfers = transfers;
		this.pendingTransfers = pendingTransfers;
	}
	*/
	
	public Ledger(PublicKey publickey, double balances, List<Transaction> transfers, List<Transaction> pendingTransfers) {
		//TODO JACKSON BUG
		StringBuffer toReturn = new StringBuffer();
		byte[] keyBytes=publickey.getEncoded();
        for (int i = 0; i < keyBytes.length; ++i) {
            toReturn.append(Integer.toHexString(0x0100 + (keyBytes[i] & 0x00FF)).substring(1));
        }
		this.publickey=toReturn.toString();
		this.balance = balances;
		this.transfers = transfers;
		this.pendingTransfers = pendingTransfers;
	}
	
	@JsonIgnore
	public PublicKey getPubliKeyFromString() throws NoSuchAlgorithmException, InvalidKeySpecException{
		byte[] bytes = DatatypeConverter.parseHexBinary(publickey);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(bytes);
        RSAPublicKey pubKey = (RSAPublicKey) kf.generatePublic(keySpecX509);
        return pubKey;
	}
	
	
	
	public String getPublicKey(){
		return publickey;
	}
	
	public double getBalance() {
		return balance;
	}
	public boolean sendBalance(double send) {
		if(balance-send>=0) {
			balance-=send;
			return true;
		}
		return false;
	}
	public void receiveBalance(double receive) {
		balance+=receive;
	}
	public void setBalance(double balance) {
		this.balance = balance;
	}
	public List<Transaction> getTransfers() {
		return transfers;
	}
	public void addTransfers(Transaction transfer) {
		transfers.add(transfer);
	}
	public List<Transaction> getPendingTransfers() {
		return pendingTransfers;
	}
	public void addPendingTransfers(Transaction pendingTransfer) {
		pendingTransfers.add(pendingTransfer);
	}
	public void removePendingTransfers(Transaction pendingTransfer) {
		pendingTransfers.add(pendingTransfer);
	}

	@Override
	public String toString()
	{
		return "Balance: "+balance+"\nPending transfer size: "+pendingTransfers.size()+"\nTransfer size: "+transfers.size()+"\nPublicKey:"+publickey+".";
	}
}
