package pt.ulisboa.tecnico.hdscoin.interfaces;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;

import pt.ulisboa.tecnico.hdscoin.Crypto.Message;

public class FunctionRegister implements Serializable{
	
	private String clientName;
	private PublicKey publicKey;
	private Message message;
	//TODO commented below
	private int rid; //digitalsign
	private String serverOrigin;
	private ConcurrentHashMap<String, Boolean> echo=new ConcurrentHashMap<String, Boolean>(); //hash
	private ConcurrentHashMap<String, Boolean> ready=new ConcurrentHashMap<String, Boolean>(); //hash
	private ConcurrentHashMap<String, Boolean> delivery=new ConcurrentHashMap<String, Boolean>(); //hash
	public FunctionRegister() {
		
	}
	public FunctionRegister(String clientName, PublicKey publicKey, int rid, int totalServerNumber) {
		this.clientName=clientName;
		this.publicKey=publicKey;
		this.rid=rid;
		for(int i=1; i<=totalServerNumber;i++){
			this.echo.put("server"+i, false);
			this.ready.put("server"+i, false);
			this.delivery.put("server"+i, false);
		}
		
	}
	public String getServerOrigin() {
		return serverOrigin;
	}
	public void setServerOrigin(String serverOrigin) {
		this.serverOrigin = serverOrigin;
	}
	public int getRid() {
		return rid;
	}
	public void setRid(int rid) {
		this.rid = rid;
	}
	public String getClientName() {
		return clientName;
	}
	public void setClientName(String clientName) {
		this.clientName = clientName;
	}
	public PublicKey getPublicKey() {
		return publicKey;
	}
	public void setPublicKey(PublicKey publicKey) {
		this.publicKey = publicKey;
	}
	
	public boolean myEquals(FunctionRegister arg0) {
		if(publicKey.equals(arg0.getPublicKey())&&
				clientName.equals(arg0.getClientName())&&
				serverOrigin.equals(getServerOrigin())){
			return true;
		}
	    return false;
	}
	public String toString(){
		return "ClientName: "+clientName+"\tByServer: "+serverOrigin;
	}
}