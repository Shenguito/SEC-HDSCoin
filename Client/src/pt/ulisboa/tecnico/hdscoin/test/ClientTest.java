package pt.ulisboa.tecnico.hdscoin.test;

import static org.junit.Assert.*;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import org.junit.Test;

import pt.ulisboa.tecnico.hdscoin.client.Client;

public class ClientTest {

	@Test
	public void register() {
		try {
			Client client=new Client("localhost", "alice");
			assertTrue(client.register());
		} catch (RemoteException e) {
			fail("Not yet implemented");
			e.printStackTrace();
		} catch (NotBoundException e) {
			fail("Not yet implemented");
			e.printStackTrace();
		}
	}
	@Test
	public void send() {
		try {
			Client alice=new Client("localhost", "alice");
			Client bob=new Client("localhost", "bob");
			assertTrue(alice.register());
			assertTrue(bob.register());
			assertTrue(alice.send("bob", "50"));
		} catch (RemoteException e) {
			fail("Not yet implemented");
			e.printStackTrace();
		} catch (NotBoundException e) {
			fail("Not yet implemented");
			e.printStackTrace();
		}
	}
	@Test
	public void check() {
		try {
			Client alice=new Client("localhost", "alice");
			Client bob=new Client("localhost", "bob");
			assertTrue(alice.register());
			assertTrue(bob.register());
			assertTrue(alice.send("bob", "10"));
			assertTrue(bob.send("alice", "10"));
			assertTrue(alice.check("bob"));
			assertTrue(bob.check("alice"));
			assertTrue(alice.check("alice"));
		} catch (RemoteException e) {
			fail("Not yet implemented");
			e.printStackTrace();
		} catch (NotBoundException e) {
			fail("Not yet implemented");
			e.printStackTrace();
		}
	}
	@Test
	public void receive() {
		try {
			Client alice=new Client("localhost", "alice");
			Client bob=new Client("localhost", "bob");
			assertTrue(alice.register());
			assertTrue(bob.register());
			assertTrue(alice.send("bob", "10"));
			assertTrue(alice.send("bob", "5"));
			assertTrue(alice.send("bob", "15"));
			assertTrue(alice.send("bob", "25"));
			assertTrue(alice.send("bob", "20"));
			assertTrue(bob.send("alice", "10"));
			assertTrue(alice.check("alice"));
			assertTrue(bob.check("bob"));
			assertTrue(alice.receive("1"));
			assertTrue(bob.receive("1 2 3 4 5"));
			
		} catch (RemoteException e) {
			fail("Not yet implemented");
			e.printStackTrace();
		} catch (NotBoundException e) {
			fail("Not yet implemented");
			e.printStackTrace();
		}
	}
	
	@Test
	public void audit() {
		try {
			Client alice=new Client("localhost", "alice");
			Client bob=new Client("localhost", "bob");
			assertTrue(alice.register());
			assertTrue(bob.register());
			assertTrue(alice.send("bob", "10"));
			assertTrue(alice.send("bob", "5"));
			assertTrue(alice.send("bob", "15"));
			assertTrue(alice.send("bob", "25"));
			assertTrue(alice.send("bob", "20"));
			assertTrue(bob.send("alice", "10"));
			assertTrue(alice.check("alice"));
			assertTrue(bob.check("bob"));
			assertTrue(alice.receive("1"));
			assertTrue(bob.receive("1 2 3 4 5"));
			assertTrue(alice.audit("alice"));
			assertTrue(alice.audit("bob"));
			
		} catch (RemoteException e) {
			fail("Not yet implemented");
			e.printStackTrace();
		} catch (NotBoundException e) {
			fail("Not yet implemented");
			e.printStackTrace();
		}
	}

}