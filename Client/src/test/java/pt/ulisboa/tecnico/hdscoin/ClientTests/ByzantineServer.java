package pt.ulisboa.tecnico.hdscoin.ClientTests;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import org.junit.Test;

import pt.ulisboa.tecnico.hdscoin.client.Client;


public class ByzantineServer {

	static Client alice;
	static Client bob;
	
	@Test
	public void sendAttack() {
		try {
			alice=new Client("localhost", "alice", "alice123",false,0);
			assertTrue(alice.register());
			alice.setServerByzantine(true);
			
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertTrue(alice.check("alice"));
		assertTrue(alice.send("bob", "5"));
		alice.setServerByzantine(false);
		
	}
}
