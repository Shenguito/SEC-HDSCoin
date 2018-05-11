package pt.ulisboa.tecnico.hdscoin.interfaces;


import pt.ulisboa.tecnico.hdscoin.Crypto.CipheredMessage;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;


public interface RemoteServerInterface extends Remote{
	void register(PublicKey pubkey) throws RemoteException;
	CipheredMessage send(CipheredMessage msg) throws RemoteException;
    CipheredMessage check(CipheredMessage msg) throws RemoteException;
    CipheredMessage receive(CipheredMessage msg) throws RemoteException;
    CipheredMessage audit(CipheredMessage msg) throws RemoteException, IOException, NoSuchAlgorithmException, InvalidKeySpecException;
    CipheredMessage clientHasRead(CipheredMessage msg) throws IOException, NoSuchPaddingException, ClassNotFoundException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException;
    
    
    
	//Authenticated Double-Echo Broadcast page 118 for register
    void echoBroadcast(CipheredMessage msg) throws RemoteException;
    void readyBroadcast(CipheredMessage msg) throws RemoteException;
	void setByzantine(boolean mode, int attack) throws RemoteException;
    
}
