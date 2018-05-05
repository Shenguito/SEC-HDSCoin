package pt.ulisboa.tecnico.hdscoin.interfaces;

import java.io.Serializable;
import java.security.PublicKey;

public class FunctionRegister implements Serializable{
	private String clientName;
	private PublicKey publicKey;
	public FunctionRegister() {
		
	}
	public FunctionRegister(String clientName, PublicKey publicKey) {
		this.clientName=clientName;
		this.publicKey=publicKey;
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
	
}
