package pt.ulisboa.tecnico.hdscoin.ClientTests;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import org.junit.Test;

import pt.ulisboa.tecnico.hdscoin.client.Client;


public class replayAttack {
	
	static Client alice;
	static Client bob;
	
	@Test
	public void replayAttack() {
		try {
			alice=new Client("localhost", "alice", "alice123",true,4);
			assertTrue(alice.register());
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
		System.out.println("sending first check");
		assertTrue(alice.check("alice"));
		System.out.println("ok");
		System.out.println("sending second check with exactly same message");
		assertTrue(alice.check("alice"));
	}
}
