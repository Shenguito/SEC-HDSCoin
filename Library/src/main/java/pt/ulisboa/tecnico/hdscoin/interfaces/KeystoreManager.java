package pt.ulisboa.tecnico.hdscoin.interfaces;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.crypto.*;
import java.util.*;



public class KeystoreManager {


	private KeyStore ks;

	public KeyStore getKeyStore(){

		return this.ks;
	}

	public KeystoreManager(String name, String pass) throws Exception{
		
		ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(getClass().getResourceAsStream(name), pass.toCharArray());
	}

	public KeyPair getKeyPair(String name, String pass) throws Exception{

		Key key = getKeyStore().getKey(name.toLowerCase(), pass.toCharArray());
	    if (key instanceof PrivateKey) {
	      // Get certificate of public key
	      Certificate cert = getKeyStore().getCertificate(name);

	      // Get public key
	      PublicKey publicKey = cert.getPublicKey();

	      // Return a key pair
	      return new KeyPair(publicKey, (PrivateKey) key);
	      
	    }

	    return null;
	}
	
	public PublicKey getPublicKeyByName(String name) throws Exception{

		Certificate certificate = getKeyStore().getCertificate(name);
		PublicKey publicKey = certificate.getPublicKey();
	    return publicKey;
	}
	
	
	public static void main(String[] argv) throws Exception {
		String SIGNATURE_ALGO = "SHA256withRSA";
		String ASYM_CIPHER = "RSA/ECB/PKCS1Padding";
	 	KeystoreManager k = new KeystoreManager("keystore.ks", "cofre123");
	 	KeyPair p = k.getKeyPair("alice", "alice123");
	 	if(p != null){
		 	String a = "aaa";

		 	Cipher cipher = Cipher.getInstance(ASYM_CIPHER);
			cipher.init(Cipher.ENCRYPT_MODE, p.getPublic());
			byte[] cipherBytes = cipher.doFinal(a.getBytes());

			Cipher cipher2 = Cipher.getInstance(ASYM_CIPHER);
			cipher2.init(Cipher.DECRYPT_MODE, p.getPrivate());
			byte[] plainBytes = cipher2.doFinal(cipherBytes);
			
			String b = new String(plainBytes);

			System.out.println(a + "   " + b);
		}
		else {
			System.out.println("erro");
		}

	}
}