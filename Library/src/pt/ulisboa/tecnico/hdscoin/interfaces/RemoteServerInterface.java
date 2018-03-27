package pt.ulisboa.tecnico.hdscoin.interfaces;

import pt.ulisboa.tecnico.hdscoin.Crypto.CipheredMessage;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.PublicKey;

public interface RemoteServerInterface extends Remote{
	CipheredMessage register(String pubkey, CipheredMessage msg) throws RemoteException;
    void send(PublicKey source, PublicKey destination, int amount) throws RemoteException;
    String check(PublicKey source) throws RemoteException;
    String receive(PublicKey source) throws RemoteException;
    String audit(PublicKey source) throws RemoteException;
}
