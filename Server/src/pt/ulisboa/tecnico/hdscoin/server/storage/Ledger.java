package pt.ulisboa.tecnico.hdscoin.server.storage;

import java.io.Serializable;
import java.util.List;

import pt.ulisboa.tecnico.hdscoin.interfaces.Transaction;

/* 
 * This class should have synchronization in consideration
*/

public class Ledger implements Serializable{
	private double balance;
	private List<Transaction> transfers;
	private List<Transaction> pendingTransfers;
	public Ledger() {
		
	}
	public Ledger(double balances, List<Transaction> transfers, List<Transaction> pendingTransfers) {
		this.balance = balances;
		this.transfers = transfers;
		this.pendingTransfers = pendingTransfers;
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
}
