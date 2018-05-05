package pt.ulisboa.tecnico.hdscoin.interfaces;

import java.io.Serializable;
import java.security.PublicKey;

public class FunctionRegister implements Serializable{
	private String clientName;
	private PublicKey publicKey;
	private int rid;
	private int wts;
	private String serverOrigim;
	public FunctionRegister() {
		
	}
	public FunctionRegister(String clientName, PublicKey publicKey, int rid, int wts, String serverOrigim) {
		this.clientName=clientName;
		this.publicKey=publicKey;
		this.rid=rid;
		this.wts=wts;
		this.serverOrigim=serverOrigim;
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
	public int getWts() {
		return wts;
	}
	public void setWts(int wts) {
		this.wts = wts;
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
				wts==arg0.getWts()&&
				serverOrigim.equals(getServerOrigim())){
			return true;
		}
	    return false;
	}
	
}
