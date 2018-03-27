package pt.ulisboa.tecnico.hdscoin.server;

import java.security.PublicKey;
import java.rmi.RemoteException;

import pt.ulisboa.tecnico.hdscoin.client.*;


public class ServerInterface implements RemoteServerInterface {

	 public ServerInterface() {}
	    
	    @Override
		public String register(PublicKey source) throws RemoteException {
	    	return "Hello, world!";
		}

		@Override
		public void send(PublicKey source, PublicKey destination, int amount) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public String check(PublicKey source) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String receive(PublicKey source) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String audit(PublicKey source) {
			// TODO Auto-generated method stub
			return null;
		}
	
}
