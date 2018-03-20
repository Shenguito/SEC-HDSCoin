package cs;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.PublicKey;

public class Server implements ServerR {

    public Server() {}
    
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

    public static void main(String args[]) {

        try {
            Server obj = new Server();
            ServerR stub = (ServerR) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("ServerR", stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

	
}