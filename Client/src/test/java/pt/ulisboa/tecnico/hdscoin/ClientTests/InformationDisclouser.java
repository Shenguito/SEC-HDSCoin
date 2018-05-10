package pt.ulisboa.tecnico.hdscoin.ClientTests;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import org.junit.Test;

import pt.ulisboa.tecnico.hdscoin.client.Client;

public class InformationDisclouser {
	
	static Client alice;
	static Client bob;
	
	@Test
	public void informationDisclouser() {
		try {
			alice=new Client("localhost", "alice", "alice123",true,3);
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
		System.out.println("Sending suposed message of server1 to the other server");
		assertTrue(alice.check("alice"));
		System.out.println("1 correct");
		System.out.println("3 exceptions");
	}
}
