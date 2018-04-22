package pt.ulisboa.tecnico.hdscoin.interfaces;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReadWriteOperations {

	//TODO for check, it is not supposed received any parameters on read operation.
	public String read(String username, List<Transaction> pendingTransaction, Map<Integer, List<Transaction>> transactions) {
		Map<Integer, Double> amounts = new HashMap<Integer,Double>();
		Collection<Double> amount = amounts.values();
        if(Collections.frequency(amount, amount.iterator().next()) == amount.size())
        	System.out.println(username + "'s balance is: " + amount.iterator().next());
        //else{ implementar modelo de faltas
		//}
        if(transactions.size()==0)
    		System.out.println(username+" has no pending transfer...");
        else if(transactions.size()!=7)
        	//or all responded or none 
    		return null;
        else {
        	//Check if all are the same/modelo de faltas
            System.out.println(username+"'s pending transfer(s) are:");
            int id=0;
            for(Transaction t:pendingTransaction) {
            	id++;
            	System.out.println("id "+id+": \t"+t.toString());
            }
        }
		return null;
	}
	
	//TODO
	public void write(String value) {
		/*TODO 
			-read();
			-consensus;
			-write;
		*/
	}
}
