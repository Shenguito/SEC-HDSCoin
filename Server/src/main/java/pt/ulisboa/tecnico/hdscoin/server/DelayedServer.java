package pt.ulisboa.tecnico.hdscoin.server;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import pt.ulisboa.tecnico.hdscoin.Crypto.CipheredMessage;

public class DelayedServer extends Server{

	private int delay;
	public DelayedServer(int number, int totalServer, int byzantineServerNumber, int delay) throws RemoteException, AlreadyBoundException, MalformedURLException, NotBoundException {
        super(number, totalServer, byzantineServerNumber);
        this.delay=delay*1000;
    }

    @Override
    public CipheredMessage check(CipheredMessage msg) throws RemoteException {
        System.out.println("Byzantine received check");
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void register(PublicKey publickey) throws RemoteException {
        super.register(publickey);
        System.out.println("BYZANTINE SERVER STARTING");
    }

    @Override
    public synchronized CipheredMessage send(CipheredMessage msg) throws RemoteException {
        System.out.println("Byzantine received send");

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public synchronized CipheredMessage receive(CipheredMessage msg) throws RemoteException {
        System.out.println("Byzantine received receive");

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public CipheredMessage audit(CipheredMessage msg) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        System.out.println("Byzantine received msg");

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
