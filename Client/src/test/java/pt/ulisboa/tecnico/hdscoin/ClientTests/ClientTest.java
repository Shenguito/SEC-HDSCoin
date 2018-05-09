package pt.ulisboa.tecnico.hdscoin.ClientTests;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;


import org.junit.Test;
import pt.ulisboa.tecnico.hdscoin.client.Client;

public class ClientTest {

	static Client alice;
	static Client bob;

	@Test
	public void register() {
		try {
			Client client=new Client("localhost", "alice", "alice123");
			assertTrue(client.register());
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Test
	public void send() {
		try {
			alice=new Client("localhost", "alice", "alice123");
			bob=new Client("localhost", "bob", "bob123");
			assertTrue(alice.register());
			assertTrue(bob.register());
			System.out.println("BLABA");
			alice.check("alice");
			bob.check("bob");
			assertTrue(alice.send("bob", "5.0"));
		} catch (RemoteException e) {
			fail("Not yet implemented");
			e.printStackTrace();
		} catch (NotBoundException e) {
			fail("Not yet implemented");
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Test
	public void check() {
		try {
			alice=new Client("localhost", "alice", "alice123");
			bob=new Client("localhost", "bob", "bob123");
			assertTrue(alice.register());
			assertTrue(bob.register());
			alice.check("alice");
			bob.check("bob");
			assertTrue(alice.send("bob", "1"));
			assertTrue(bob.send("alice", "1"));
			assertTrue(alice.check("bob"));
			assertTrue(bob.check("alice"));
			assertTrue(alice.check("alice"));
		} catch (RemoteException e) {
			fail("Not yet implemented");
			e.printStackTrace();
		} catch (NotBoundException e) {
			fail("Not yet implemented");
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Test
	public void receive() {
		try {
			alice=new Client("localhost", "alice", "alice123");
			bob=new Client("localhost", "bob", "bob123");
			assertTrue(alice.register());
			assertTrue(bob.register());
			alice.check("alice");
			bob.check("bob");
			assertTrue(alice.send("bob", "1.0"));
			assertTrue(alice.send("bob", "0.5"));
			assertTrue(alice.send("bob", "1.5"));
			assertTrue(alice.send("bob", "2.5"));
			assertTrue(alice.send("bob", "2.0"));
			assertTrue(bob.send("alice", "1.0"));
			assertTrue(alice.check("alice"));
			assertTrue(bob.check("bob"));
			assertTrue(alice.receive(1));
			assertTrue(bob.receive(2));
			
		} catch (RemoteException e) {
			fail("Not yet implemented");
			e.printStackTrace();
		} catch (NotBoundException e) {
			fail("Not yet implemented");
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void audit() {
		try {
			alice=new Client("localhost", "alice", "alice123");
			bob=new Client("localhost", "bob", "bob123");
			assertTrue(alice.register());
			assertTrue(bob.register());
			alice.check("alice");
			bob.check("bob");
			assertTrue(alice.send("bob", "1"));
			assertTrue(alice.send("bob", "0.5"));
			assertTrue(alice.send("bob", "1.5"));
			assertTrue(alice.send("bob", "2.5"));
			assertTrue(alice.send("bob", "2.0"));
			assertTrue(bob.send("alice", "1.0"));
			assertTrue(alice.check("alice"));
			assertTrue(bob.check("bob"));
			assertTrue(alice.receive(1));
			assertTrue(bob.receive(3));
			assertTrue(alice.audit("alice"));
			assertTrue(alice.audit("bob"));
			
		} catch (RemoteException e) {
			fail("Not yet implemented");
			e.printStackTrace();
		} catch (NotBoundException e) {
			fail("Not yet implemented");
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
