package pt.ulisboa.tecnico.hdscoin.interfaces;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;

import pt.ulisboa.tecnico.hdscoin.Crypto.IntegrityCheck;
import pt.ulisboa.tecnico.hdscoin.Crypto.Message;

public class BroadcastMessage implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8110083639783388238L;

	private IntegrityCheck digitalsign;

	private ConcurrentHashMap<String, Boolean> echo=new ConcurrentHashMap<String, Boolean>(); //hash
	private ConcurrentHashMap<String, Boolean> ready=new ConcurrentHashMap<String, Boolean>(); //hash
	private ConcurrentHashMap<String, Boolean> delivery=new ConcurrentHashMap<String, Boolean>(); //hash
	public BroadcastMessage() {
		
	}
	public BroadcastMessage(IntegrityCheck digitalsign, int totalServerNumber) {
		this.digitalsign=digitalsign;
		for(int i=1; i<=totalServerNumber;i++){
			this.echo.putIfAbsent("server"+i, false);
			this.ready.putIfAbsent("server"+i, false);
			this.delivery.putIfAbsent("server"+i, false);
		}
		
	}
	public IntegrityCheck getDigitalsign() {
		return digitalsign;
	}
	public String getStringDigitalsign() {
		// TODO Auto-generated method stub
		return digitalsign.getStringDigitalSignature();
	}
	
	
	
	public void echoServer(String server) {
		if(!echo.get(server)) {
			echo.put(server, true);
		}
	}
	public int echoServerReceived() {
		int v=0;
		for(boolean tmp: echo.values())
			if(tmp)
				v++;
		return v;
	}
	public boolean serverEchoed(String server) {
		return echo.get(server);
	}
	
	
	
	public void readyServer(String server) {
		if(!ready.get(server)) {
			ready.put(server, true);
		}
	}
	public int readyServerReceived() {
		int v=0;
		for(boolean tmp: ready.values())
			if(tmp)
				v++;
		return v;
	}
	public boolean serverReadied(String server) {
		return ready.get(server);
	}
	
	
	
	public void deliveryServer(String server) {
		if(!delivery.get(server)) {
			delivery.put(server, true);
		}
	}
	public int deliveryServerReceived() {
		int v=0;
		for(boolean tmp: delivery.values())
			if(tmp)
				v++;
		return v;
	}
	public boolean serverDelivery(String server) {
		return delivery.get(server);
	}
	
	
	
	public int echoServerReceivedSize() {
		return echo.size();
	}
	public int readyServerReceivedSize() {
		return ready.size();
	}
	public int deliveryServerReceivedSize() {
		return delivery.size();
	}
	
	
	
}