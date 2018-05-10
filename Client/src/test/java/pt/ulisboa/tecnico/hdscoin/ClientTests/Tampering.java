package pt.ulisboa.tecnico.hdscoin.ClientTests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import org.junit.Test;

import pt.ulisboa.tecnico.hdscoin.Crypto.CipheredMessage;
import pt.ulisboa.tecnico.hdscoin.Crypto.Message;
import pt.ulisboa.tecnico.hdscoin.client.Client;

import javax.xml.bind.DatatypeConverter;

public class Tampering {
	
	static Client alice;
	static Client bob;
	static Client charlie;
	
	@Test
	public void TamperingAttack() {
		try {
			alice=new Client("localhost", "alice", "alice123",false,0);
			bob = new Client("localhost", "bob", "bob123", false, 0);
			charlie = new Client("localhost", "charlie", "charlie123", false, 0);

			CipheredMessage caughtMsg = alice.createTestMsg("charlie");
			byte[] content  = caughtMsg.getContent();
			content[0] = 1;
			caughtMsg.setContent(DatatypeConverter.printHexBinary(content));
			assertFalse(charlie.decipherCaughtMsg(caughtMsg));
		} catch (RemoteException | MalformedURLException | NotBoundException e) {
			e.printStackTrace();
		}

	}
}
