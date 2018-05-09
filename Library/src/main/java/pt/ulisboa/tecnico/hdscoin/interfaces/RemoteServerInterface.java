package pt.ulisboa.tecnico.hdscoin.interfaces;


import pt.ulisboa.tecnico.hdscoin.Crypto.CipheredMessage;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.PublicKey;
import java.util.ArrayList;


public interface RemoteServerInterface extends Remote{
	boolean register(String clientName, PublicKey pubkey) throws RemoteException;
	CipheredMessage send(CipheredMessage msg) throws RemoteException;
    CipheredMessage check(CipheredMessage msg) throws RemoteException;
    CipheredMessage receive(CipheredMessage msg) throws RemoteException;
    CipheredMessage audit(CipheredMessage msg) throws RemoteException;
    CipheredMessage clientHasRead(CipheredMessage msg) throws RemoteException;
    
    
    
	//Authenticated Double-Echo Broadcast page 118 for register
    FunctionRegister sendEchoRegister(FunctionRegister register) throws RemoteException;
    ArrayList<FunctionRegister> sendReadyRegister(FunctionRegister register) throws RemoteException;
    FunctionRegister sendDeliveryRegister(FunctionRegister register) throws RemoteException;
    
    
    
    
    
    
    void echoBroadcast(CipheredMessage msg) throws RemoteException;
    void readyBroadcast(CipheredMessage msg) throws RemoteException;
    void deliveryBroadcast(CipheredMessage msg) throws RemoteException;
    
}
