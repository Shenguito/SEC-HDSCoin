package pt.ulisboa.tecnico.hdscoin.interfaces;

import java.io.Serializable;
import java.security.PublicKey;

public class FunctionRegister implements Serializable{
	private String clientName;
	private PublicKey publicKey;
	private int rid;
	private String serverOrigim;
	private boolean echo;
	private boolean ready;
	private boolean delivery;
	public FunctionRegister() {
		
	}
	public FunctionRegister(String clientName, PublicKey publicKey, int rid, String serverOrigim) {
		this.clientName=clientName;
		this.publicKey=publicKey;
		this.rid=rid;
		this.serverOrigim=serverOrigim;
		this.echo=false;
		this.ready=false;
		this.delivery=false;
	}
	public boolean isEcho() {
		return echo;
	}
	public void setEcho(boolean echo) {
		this.echo = echo;
	}
	public boolean isReady() {
		return ready;
	}
	public void setReady(boolean ready) {
		this.ready = ready;
	}
	public boolean isDelivery() {
		return delivery;
	}
	public void setDelivery(boolean delivery) {
		this.delivery = delivery;
	}
	public String getServerOrigim() {
		return serverOrigim;
	}
	public void setServerOrigim(String serverOrigim) {
		this.serverOrigim = serverOrigim;
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
				rid==arg0.getRid()&&
				serverOrigim.equals(getServerOrigim())){
			return true;
		}
	    return false;
	}
	
}