package pt.ulisboa.tecnico.hdscoin.server.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pt.ulisboa.tecnico.hdscoin.interfaces.Transaction;


public class Storage {
	private ObjectMapper objectMapper;
	//private final String Historyfilename="history"+File.separator+"transactions.json";
	
	public Storage() {
		
		objectMapper = new ObjectMapper();
	}
	
	public Ledger readClient(String path) {
		File file = new File(getFile(path));
		Ledger ledger=null;
		try {
			ledger = objectMapper.readValue(file, Ledger.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ledger;
	}
	
	public void writeClient(String path, Ledger ledger) {
		try {
			objectMapper.writeValue(new FileOutputStream(getFile(path)), ledger);
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean checkFileExists(String pubkey) {
		File f = new File(getFile(pubkey));
		if(f.exists() && !f.isDirectory()) { 
			return true;
		}
		return false;
	}
	
	private String getFile(String name) {
		return "client"+File.separator+name+".json";
	}
	
	
	/*

	public void writeTransaction(String sender, String receiver, double amount) {
		File f = new File(Historyfilename);
		
		try {
			if(f.exists() && !f.isDirectory()) {
				List<Transaction> transactions = objectMapper.readValue(Historyfilename, new TypeReference<List<Transaction>>(){});
				transactions.add(new Transaction(transactions.size()+1, sender, receiver, amount));
				objectMapper.writeValue(new FileOutputStream(Historyfilename), transactions);
			}else {
				List<Transaction> transactions=new ArrayList<Transaction>();
				transactions.add(new Transaction(1, sender, receiver, amount));
				objectMapper.writeValue(new FileOutputStream(Historyfilename), transactions);
			}
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public List<Transaction> getTransactionBySender(String sender){
		File f = new File(Historyfilename);
		List<Transaction> senderTransactions=new ArrayList<Transaction>();
		try {
			if(f.exists() && !f.isDirectory()) {
				List<Transaction> transactions = objectMapper.readValue(Historyfilename, new TypeReference<List<Transaction>>(){});
				for(Transaction t:transactions) {
					if(t.getSender().equals(sender)||t.getReceiver().equals(sender)) {
						senderTransactions.add(t);
					}
				}
				objectMapper.writeValue(new FileOutputStream(Historyfilename), transactions);
			}
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return senderTransactions;
	}
	*/
}
