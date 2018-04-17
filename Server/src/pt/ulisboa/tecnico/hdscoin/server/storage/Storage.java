package pt.ulisboa.tecnico.hdscoin.server.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.DatatypeConverter;

import com.fasterxml.jackson.core	.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class Storage {
	private ObjectMapper objectMapper;
	
	public Storage() {
		//Remove clients' files
		//removeClients();
		objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	
	
	
	public Ledger readClient(String path) {
		File file = new File(getFile(path));
		Ledger ledger=null;
		try {
			ledger = objectMapper.readValue(file, Ledger.class);
		} catch (JsonParseException e) {
			
			e.printStackTrace();
		} catch (JsonMappingException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		return ledger;
	}
	
	public synchronized void writeClient(String path, Ledger ledger) {
		try {
			objectMapper.writeValue(new FileOutputStream(getFile(path)), ledger);
		} catch (JsonGenerationException e) {
			
			e.printStackTrace();
		} catch (JsonMappingException e) {
			
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}
	
	public synchronized void writeClientBackup(String path, Ledger ledger) {
		try {
			objectMapper.writeValue(new FileOutputStream(getBackupFile(path)), ledger);
		} catch (JsonGenerationException e) {
			
			e.printStackTrace();
		} catch (JsonMappingException e) {
			
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}
	
	public boolean checkFileExists(String name) {
		File f = new File(getFile(name));
		if(f.exists() && !f.isDirectory()) { 
			return true;
		}
		return false;
	}
	
	private String getFile(String name) {
		return "client"+File.separator+name+".json";
	}
	
	private String getBackupFile(String name) {
		return "backup"+File.separator+name+".json";
	}
	
	private void removeClients(){
		File file = new File("client");
		if(file.isDirectory()) {
			for(File f: file.listFiles())
				f.delete();
		}
	}

	public ConcurrentHashMap<PublicKey, String> getClients() throws JsonParseException, JsonMappingException, NoSuchAlgorithmException, InvalidKeySpecException, IOException {
		ConcurrentHashMap<PublicKey, String> allClients=new ConcurrentHashMap<PublicKey, String>();
		File file = new File("client");
		if(file.isDirectory()) {
			for(File f: file.listFiles()){
				allClients.put(getPubliKeyFromString(objectMapper.readValue(f, Ledger.class).getPublicKey()), f.getName().split(".json")[0].trim());
			}
				
		}
		return allClients;
	}
	
	public void backupCheck() {
		
		File file = new File("client");
		if(file.isDirectory()) {
			for(File f: file.listFiles()){
				try {
					Ledger ledger = objectMapper.readValue(f, Ledger.class);
					try {
						Ledger ledgerbackup = objectMapper.readValue(new File("backup"+File.separator+f.getName()), Ledger.class);
						if(ledgerbackup.myEquals(ledger)){
							continue;
						}else{
							writeClientBackup(f.getName(), ledger);
						}
					} catch (Exception e1) {
						writeClientBackup(f.getName(), ledger);
					}
				} catch (Exception e) {
					try {
						Ledger ledgerbackup = objectMapper.readValue(new File("backup"+File.separator+f.getName()), Ledger.class);
						writeClient(f.getName(), ledgerbackup);
					} catch (Exception e1) {
						System.out.println("Both files have errors!\n"+e1);
						continue;
					}
					
				}
			}
		}
	}
	
	private PublicKey getPubliKeyFromString(String publickey) throws NoSuchAlgorithmException, InvalidKeySpecException{
		byte[] bytes = DatatypeConverter.parseHexBinary(publickey);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(bytes);
        RSAPublicKey pubKey = (RSAPublicKey) kf.generatePublic(keySpecX509);
        return pubKey;
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
			
			e.printStackTrace();
		} catch (JsonMappingException e) {
			
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
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
			
			e.printStackTrace();
		} catch (JsonMappingException e) {
			
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		return senderTransactions;
	}
	*/
	
	
}
