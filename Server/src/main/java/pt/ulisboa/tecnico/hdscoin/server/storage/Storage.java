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
import pt.ulisboa.tecnico.hdscoin.server.storage.Ledger;


public class Storage {
	private ObjectMapper objectMapper;
	private String serverStorage;
	public Storage(String server) {
		//Remove clients' files
		//removeClients();
		File file = new File(server);
		if (!file.exists()) {
		      file.mkdirs();
	    }
		serverStorage=server;
		objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
		try {
		if(ledger.getPendingTransfers().size()!=0)
			ledger.getPendingTransfers().get(0).toString();
		}catch(Exception e) {
			e.printStackTrace();
		}
		return ledger;
	}
	
	public synchronized void writeClient(String path, Ledger ledger) throws JsonGenerationException, JsonMappingException, FileNotFoundException, IOException {
		File yourFile = new File(getFile(path));
		yourFile.createNewFile();
		objectMapper.writeValue(new FileOutputStream(getFile(path)), ledger);
		
	}
	
	public synchronized void writeClientBackup(String path, Ledger ledger) throws JsonGenerationException, JsonMappingException, FileNotFoundException, IOException {
		File yourFile = new File(getBackupFile(path));
		yourFile.createNewFile();
		objectMapper.writeValue(new FileOutputStream(getBackupFile(path)), ledger);
		
	}
	
	public boolean checkFileExists(String name) {
		File f = new File(getFile(name));
		if(f.exists() && !f.isDirectory()) { 
			return true;
		}
		return false;
	}
	
	private String getDirectory() {
		return serverStorage+File.separator+"client";
	}
	private String getBackupDirectory() {
		return serverStorage+File.separator+"backup";
	}
	
	private String getFile(String name) {
		File file = new File(serverStorage+File.separator+"client");
		if (!file.exists()) {
		      file.mkdirs();
	    }
		return serverStorage+File.separator+"client"+File.separator+name;
	}
	
	private String getBackupFile(String name) {
		File file = new File(serverStorage+File.separator+"backup");
		if (!file.exists()) {
		      file.mkdirs();
	    }
		return serverStorage+File.separator+"backup"+File.separator+name;
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
		File file = new File(getDirectory());
		if(file.isDirectory()) {
			for(File f: file.listFiles()){
				allClients.put(getPubliKeyFromString(objectMapper.readValue(f, Ledger.class).getPublicKey()), f.getName().split(".json")[0].trim());
			}
				
		}
		return allClients;
	}
	
	public void backupCheck() {
		
		File file = new File(getDirectory());
		if(file.isDirectory()) {
			for(File f: file.listFiles()){
				try {
					Ledger ledger = objectMapper.readValue(f, Ledger.class);
					try {
						Ledger ledgerbackup = objectMapper.readValue(new File(getBackupDirectory() + File.separator + f.getName()), Ledger.class);
						if (ledgerbackup.myEquals(ledger)) {
							continue;
						} else {
							System.out.println("Backup File and Original File are differents. Backup File is rewrited ...");
							writeClientBackup(f.getName(), ledger);
						}
					} catch (Exception e1) {
						System.out.println("Backup File has error. It is rewrited\n" + e1);
						writeClientBackup(f.getName(), ledger);
					}
				} catch (Exception e) {
					System.out.println("Original File has error. It is rewrited\n" + e);
					try {
						Ledger ledgerbackup = objectMapper.readValue(new File(getBackupDirectory()  + File.separator + f.getName()), Ledger.class);
						writeClient(f.getName(), ledgerbackup);
					} catch (Exception e1) {
						System.out.println("Both files have errors!\n" + e1);
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
	
}
