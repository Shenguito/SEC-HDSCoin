package pt.ulisboa.tecnico.hdscoin.client;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.PublicKey;

public interface RemoteServer extends Remote{
	String register(PublicKey source) throws RemoteException;
    void send(PublicKey source, PublicKey destination, int amount) throws RemoteException;
    String check(PublicKey source) throws RemoteException;
    String receive(PublicKey source) throws RemoteException;
    String audit(PublicKey source) throws RemoteException;
}
