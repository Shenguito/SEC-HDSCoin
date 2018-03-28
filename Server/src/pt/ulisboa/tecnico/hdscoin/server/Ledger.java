package pt.ulisboa.tecnico.hdscoin.server;

import java.util.List;

/* 
 * This class should have synchronization in consideration
*/

public class Ledger {
	private double balance;
	private List<String> transfers;
	private List<String> pendingTransfers;
	public Ledger(double balances, List<String> transfers, List<String> pendingTransfers) {
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
	public List<String> getTransfers() {
		return transfers;
	}
	public void addTransfers(String transfer) {
		transfers.add(transfer);
	}
	public List<String> getPendingTransfers() {
		return pendingTransfers;
	}
	public void addPendingTransfers(String pendingTransfer) {
		pendingTransfers.add(pendingTransfer);
	}
	public void removePendingTransfers(String pendingTransfer) {
		pendingTransfers.add(pendingTransfer);
	}
}
