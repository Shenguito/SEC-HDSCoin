package pt.ulisboa.tecnico.hdscoin.crypto;

import java.io.Serializable;

/**
 * Object that represents a ciphered message in the format that we specified
 * The format is:
 * AES[Message, RSApriv[hash(Message, IV, timestamp)], IV, timestamp], RSApub[AESkey]
 * Actual implementation is slightly different because RSAPriv is a JAVA digital signature but it still uses RSA
 */
public class CipheredMessage implements Serializable{

    private byte[] content;
    private byte[] IV;
    private byte[] integrityCheck;
    private byte[] key;

    public CipheredMessage(byte[] content, byte[] IV, byte[] integrityCheck, byte[] key) {
        this.content = content;
        this.IV = IV;
        this.integrityCheck = integrityCheck;
        this.key = key;
    }

    public byte[] getKey() {
        return key;
    }

    public byte[] getIntegrityCheck() {
        return integrityCheck;
    }

    public byte[] getContent() {
        return content;
    }

    public byte[] getIV() {
        return IV;
    }

    @Override
    public String toString() {
        return new String(content) + new String(IV) ;
    }
}
