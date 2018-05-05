package pt.ulisboa.tecnico.hdscoin.interfaces;


import pt.ulisboa.tecnico.hdscoin.Crypto.CipheredMessage;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.PublicKey;


public interface RemoteServerInterface extends Remote{
	void register(String clientName, PublicKey pubkey) throws RemoteException;
	CipheredMessage send(CipheredMessage msg) throws RemoteException;
    CipheredMessage check(CipheredMessage msg) throws RemoteException;
    CipheredMessage receive(CipheredMessage msg) throws RemoteException;
    CipheredMessage audit(CipheredMessage msg) throws RemoteException;
    CipheredMessage clientHasRead(CipheredMessage msg) throws RemoteException;
    
    
    
	//Authenticated Double-Echo Broadcast page 118 for register
	void sendEchoRegister(FunctionRegister register) throws RemoteException;
	void sendReadyRegister(FunctionRegister register) throws RemoteException;
	void sendDeliveryRegister(FunctionRegister register) throws RemoteException;
    
    
    
    
    
    
    CipheredMessage readOperation(CipheredMessage msg) throws RemoteException;
    CipheredMessage readOperationConclusion(CipheredMessage msg) throws RemoteException;
    
    CipheredMessage writeOperation(CipheredMessage msg) throws RemoteException;
    
    void test(String text)throws RemoteException;
}
