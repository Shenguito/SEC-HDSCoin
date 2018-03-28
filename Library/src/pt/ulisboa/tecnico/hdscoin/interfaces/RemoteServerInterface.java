package pt.ulisboa.tecnico.hdscoin.interfaces;

import pt.ulisboa.tecnico.hdscoin.Crypto.CipheredMessage;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.PublicKey;

public interface RemoteServerInterface extends Remote{
	CipheredMessage sample(String pubkey, CipheredMessage msg) throws RemoteException;
	String register(String pubkey) throws RemoteException;
	CipheredMessage send(CipheredMessage msg) throws RemoteException;
    CipheredMessage check(CipheredMessage msg) throws RemoteException;
    CipheredMessage receive(CipheredMessage msg) throws RemoteException;
    CipheredMessage audit(CipheredMessage msg) throws RemoteException;
}
