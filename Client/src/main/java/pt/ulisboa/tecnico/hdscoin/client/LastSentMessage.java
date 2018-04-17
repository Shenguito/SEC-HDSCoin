package pt.ulisboa.tecnico.hdscoin.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ulisboa.tecnico.hdscoin.Crypto.CipheredMessage;


public class LastSentMessage {
	private ObjectMapper objectMapper;
	public LastSentMessage(){
		objectMapper = new ObjectMapper();
	}
	public CipheredMessage readLastSentMessage(String client) {
		File file = new File(getFile(client));
		CipheredMessage message=null;
		try {
			message = objectMapper.readValue(file, CipheredMessage.class);
		} catch (JsonParseException e) {
			
			e.printStackTrace();
		} catch (JsonMappingException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		return message;
	}
	
	public synchronized void writeLastSentMessage(String client, CipheredMessage message) {
		try {
			objectMapper.writeValue(new FileOutputStream(getFile(client)), message);
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
	public boolean checkFileExists(String client) {
		try{
			File f = new File(getFile(client));
			if(f.exists() && !f.isDirectory()) {
				return true;
			}
		}catch(Exception e){
			
		}
		return false;
	}
	
	public void removeLastSentMessage(String client){
		File file = new File(getFile(client));
		if(!file.isDirectory()) {
			file.delete();
		}
	}
	
	private String getFile(String name) {
		return "lastMessage"+File.separator+name+".json";
	}
}
