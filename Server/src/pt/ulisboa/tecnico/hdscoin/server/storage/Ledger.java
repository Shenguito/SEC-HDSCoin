package pt.ulisboa.tecnico.hdscoin.server.storage;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import pt.ulisboa.tecnico.hdscoin.interfaces.Transaction;

/* 
 * This class should have synchronization in consideration
*/

public class Ledger implements Serializable{
	
	//TODO JACKSON BUG
	//private byte[] publickey;
	private PublicKey publickey;
	private double balance;
	private List<Transaction> transfers;
	private List<Transaction> pendingTransfers;
	public Ledger() {
		
	}
	public Ledger(PublicKey publickey, double balances, List<Transaction> transfers, List<Transaction> pendingTransfers) {
		//TODO JACKSON BUG
		//this.publickey=publickey.getEncoded();
		this.publickey=publickey;
		this.balance = balances;
		this.transfers = transfers;
		this.pendingTransfers = pendingTransfers;
	}
	/* not needed yet
	public PublicKey getPublicKey() {
		return publickey;
	}
	*/
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
		return "Balance: "+balance+"\nPending transfer size: "+pendingTransfers.size()+"\nTransfer size: "+transfers.size()+".";
	}
}
