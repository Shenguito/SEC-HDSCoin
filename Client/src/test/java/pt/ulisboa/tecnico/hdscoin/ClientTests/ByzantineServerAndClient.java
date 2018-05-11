package pt.ulisboa.tecnico.hdscoin.ClientTests;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import org.junit.Test;

import pt.ulisboa.tecnico.hdscoin.client.Client;
import pt.ulisboa.tecnico.hdscoin.server.NoReplyServer;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import org.junit.Test;

import pt.ulisboa.tecnico.hdscoin.client.Client;
public class ByzantineServerAndClient {

	static Client alice;
	static Client bob;
	
	@Test
	public void sendAttack() {
		try {
			alice=new Client("localhost", "alice", "alice123",false,0);
			assertTrue(alice.register());
			assertTrue(alice.check("alice"));
			assertTrue(alice.send("bob", "1"));
			alice.setServerByzantine(true,2);
			
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
		alice.setServerByzantine(false,0);
		
	}
}
